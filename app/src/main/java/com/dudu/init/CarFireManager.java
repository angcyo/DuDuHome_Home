package com.dudu.init;

import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;

import com.dudu.aios.ui.base.VolBrightnessSetting;
import com.dudu.aios.ui.fragment.tire.TireFragment;
import com.dudu.android.launcher.utils.Contacts;
import com.dudu.android.launcher.utils.Utils;
import com.dudu.android.launcher.utils.WifiApAdmin;
import com.dudu.carChecking.CarCheckingProxy;
import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.event.Events;
import com.dudu.commonlib.utils.File.SharedPreferencesUtil;
import com.dudu.commonlib.utils.thread.ThreadPoolManager;
import com.dudu.drivevideo.frontcamera.FrontCameraManage;
import com.dudu.drivevideo.rearcamera.RearCameraManage;
import com.dudu.drivevideo.spaceguard.StorageSpaceService;
import com.dudu.map.GaodeMapAppUtil;
import com.dudu.map.NavigationProxy;
import com.dudu.monitor.active.ActiveDeviceManage;
import com.dudu.monitor.flow.FlowManage;
import com.dudu.monitor.obd.ObdManage;
import com.dudu.monitor.obdUpdate.ObdUpdateService;
import com.dudu.monitor.portal.PortalManage;
import com.dudu.monitor.repo.location.LocationManage;
import com.dudu.monitor.tirepressure.TirePressureManage;
import com.dudu.network.NetworkManage;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.weather.WeatherStream;
import com.dudu.workflow.common.ObservableFactory;
import com.dudu.workflow.driving.CarStatusUtils;
import com.dudu.workflow.obd.OBDStream;
import com.dudu.workflow.robbery.RobberyFlow;
import com.dudu.workflow.tpms.TpmsStream;
import com.dudu.workflow.upgrade.LauncherUpgrade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import tm.dudu.ext.GPSCtl;
import tm.dudu.ext.GpioControl;

/**
 * Created by Administrator on 2016/5/19.
 */
public class CarFireManager {
    private static final String TAG = "CarFireManager";
    private static Logger logger = LoggerFactory.getLogger("init.CarFireManager");
    private static CarFireManager mInstance = new CarFireManager();

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;

    private Subscription requestVersionInfoSubsCription;
    private Subscription flamoutControlSubscription;
    private Subscription obdInitSubsCription;
    private Subscription carCheckingSubsCription;
    private Subscription tpmsInitSubsCription;
    private Subscription releaseWakeLockSubscription;
    private Subscription updateObdSubscription;
    //    private Subscription obdWakeupSubsCription;
    private int wakeObdTimes;

    private Timer killGaodeTimer = null;
    private Timer uploadVoltageTimer = null;
    private Timer doUploadVoltageTimer = null;
    private boolean isFlamoutStatus = false;

    private Thread wakeUpObdThread = new Thread() {

        @Override
        public void run() {
            logger.debug("wakeUpObdThread.run");
            if (wakeObdTimes > 2) {
                ThreadPoolManager.getInstance(TAG).cancelTaskThreads(TAG, true);
                wakeObdTimes = 0;
                return;
            }
            wakeObdTimes++;
            GpioControl.wakeObd();
        }
    };

    private CarFireManager() {
        mPowerManager = (PowerManager) CommonLib.getInstance().getContext().getSystemService(Context.POWER_SERVICE);
    }

    public static CarFireManager getInstance() {
        return mInstance;
    }

    public void doIfFired() {
        carCheckingSubsCription = CarStatusUtils.isFired()
                .subscribeOn(Schedulers.newThread())
                .subscribe(fired -> {
                            carCheckingSubsCription.unsubscribe();
                            if (fired) {
                                logger.info("点火，启动各种服务");
                                CarFireManager.getInstance().fireControl();
                            } else {
                                logger.info("未点火，不启动各种服务");
                                GpioControl.powerOffObd();
                                GpioControl.powerOffTPMS();
                                ObdManage.getInstance().makeSureOBDSleep(); //兼容V3.0主板
                            }
                        }
                        , throwable -> {
                            carCheckingSubsCription.unsubscribe();
                            logger.error("doIfFired", throwable);
                        });
    }

    /**
     * 点火操作
     */
    public void fireControl() {
        logger.debug("fireControl");
        isFlamoutStatus = false; //点火状态

        cancelAllSubscription();
        CarStatusUtils.saveCarIsFire(true);

        GPSCtl.startFixService();

        //开启语音唤醒
        CarStatusUtils.setIsSpeechSleeped(false);
        VoiceManagerProxy.getInstance().startWakeup();

        initObd();//初始化obd
        initTpms();//初始化胎压

        ActiveDeviceManage.getInstance().init();//设备激活服务
        WeatherStream.getInstance().startService();//开启天气任务
        LocationManage.getInstance().startSendLocation();//开始发送位置信息
        PortalManage.getInstance().init();
        FlowManage.getInstance().init();
        requestVersionInfo();//版本检测
        NetworkManage.getInstance().init();
        StorageSpaceService.getInstance().init();

//        RearCameraManage.getInstance().openCamera();
        RearCameraManage.getInstance().setRecordEnable(true);
        RearCameraManage.getInstance().startRecord();

        //直接可以在主线程中初始化，摄像头操控都在handlerThread 中完成，不会阻塞主线程
        FrontCameraManage.getInstance().setRecordEnable(true);//点火才使能录像
        FrontCameraManage.getInstance().init();
        FrontCameraManage.getInstance().startRecord();//防止已经初始化了，导致不能开启录像

        /*FrontCameraInstance.getInstance().setRecorderEnable(true);
        FrontCameraInstance.getInstance().startRecord();*/

        //点火切换为自动屏幕亮度
        initScreenBrightnessAutoMode();

        new Thread() {
            public void run() {
                logger.debug("fireControl:Thread.run");
                if (CarStatusUtils.isWifiAvailable()) {
                    WifiApAdmin.startWifiAp(CommonLib.getInstance().getContext());
                }
            }
        }.start();

        acquireLock();
    }

    /**
     * 熄火操作
     */
    public void flamoutControl() {
        logger.debug("flamoutControl");
        isFlamoutStatus = true; //点火状态

        cancelAllSubscription();
        CarStatusUtils.saveCarIsFire(false);

        ActiveDeviceManage.getInstance().release();
        EventBus.getDefault().post(new Events.CancelRobberyEvent());//取消防劫
        LocationManage.getInstance().cancerSendLocation();//停止发送gps位置
        PortalManage.getInstance().release();
        ObservableFactory.getDrivingFlow().stopGetAdcVoltage();//停止定时获取电压任务
        stopUploadVoltageTask(); //停止点火后的电压上传任务.
//        startFlamoutUploadVoltageTask();// 开启熄火后的电压上传任务。
        startFlamoutKillGaodeTask();//熄火后定时关闭高德
        flamoutDelayControl();
        NavigationProxy.getInstance().existNavi();
        releaseWakeLock();
    }

    public void acquireLock() {
        logger.debug("启动wakelock");
        if (mWakeLock == null) {
            mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "screenswakelock");
        }
        mWakeLock.setReferenceCounted(false);
        mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        logger.debug("释放wakelock");
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    public void wakeScreen() {
        CarFireManager.getInstance().acquireLock();
        CarFireManager.getInstance().releaseWakeLockIfNotFired();
    }

    public void releaseWakeLockIfNotFired() {
        logger.debug("releaseWakeLockIfNotFired");
        if (releaseWakeLockSubscription != null) {
            releaseWakeLockSubscription.unsubscribe();
        }
        releaseWakeLockSubscription = Observable.timer(2, TimeUnit.SECONDS)
                .zipWith(CarStatusUtils.isFired(), (aLong, isFired) -> isFired)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fired -> {
                    if (!fired) {
                        releaseWakeLock();
                    }
                }, throwable -> logger.error("obdWorkStart", throwable));
    }

    /**
     * 版本检测
     */
    private void requestVersionInfo() {
        logger.debug("requestVersionInfo");
        requestVersionInfoSubsCription = Observable
                .timer(30, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .subscribe(l -> {
                    logger.debug("requestVersionInfo:LauncherUpgrade.queryVersionInfo");
                    LauncherUpgrade.queryVersionInfo();
                }, throwable -> {
                    logger.error("requestVersionInfo", throwable);
                });
    }

    /**
     * 点火后的obd操作
     */
    private void initObd() {
        logger.debug("initObd");
        //上传数据流、检测电压数据、开始车辆自检、开始防劫逻辑检测
        obdInitSubsCription = Observable.just(1)
                .doOnNext(aInteger -> GpioControl.powerOnObd())
                .delay(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    logger.debug("initObd:ObdManage.init");
                    OBDStream.getInstance().init();
                    ObdManage.getInstance().init();

                    ObdUpdateService.getInstance().init();

                    //开始循环检测电压并上传
                    startUploadVoltageTask();

                    try {
                        ObservableFactory.getDrivingFlow().checkShouldMonitorAccVoltage();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    carCheckingWhenFire();
                    RobberyFlow.getInstance().checkGunSwitch();
                }, throwable -> logger.error("initObd:ObdManage.init", throwable));

        wakeObdTimes = 0;
        ThreadPoolManager.getInstance(TAG).cancelTaskThreads(TAG, true);
        ThreadPoolManager.getInstance(TAG).scheduleAtFixedRate(wakeUpObdThread, 5, 1, TimeUnit.SECONDS);
    }

    /**
     * 点火后的胎压操作(打开胎压串口)
     */
    public void initTpms() {
        if (tpmsInitSubsCription != null && !tpmsInitSubsCription.isUnsubscribed()) {
            return;
        }
        logger.debug("initTpms");
        GpioControl.powerOnTPMS();
        TpmsStream.getInstance().init();
        tpmsInitSubsCription = Observable.timer(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe(aLong -> {
                    logger.debug("initTpms:TirePressureManage.init");
                    TirePressureManage.getInstance().init();
                }, throwable -> logger.error("initTpms", throwable));
    }

    /**
     * 熄火后的胎压操作(关闭胎压串口,下电)
     */
    public void releaseTpms() {
        TpmsStream.getInstance().tpmsStreamClose();
        GpioControl.powerOffTPMS();
    }

    /**
     * 熄火延时操作（停止语音唤醒、obd流停止下电、断开长链接、停止录像、释放摄像头）
     */
    private void flamoutDelayControl() {
        logger.debug("flamoutDelayControl");
        int time_obd = 0;
        int time_sleep = 5 * 60;
        if (Utils.isDemoVersion(CommonLib.getInstance().getContext())) {
            time_obd = 0;
            time_sleep = 120;
        }

        updateObdSubscription = Observable.timer(time_obd, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .subscribe(aLong1 -> ObdUpdateService.getInstance().updateObdBin(),
                        throwable1 -> logger.debug("flamoutDelayControl", throwable1));
        flamoutControlSubscription = Observable.timer(time_sleep, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .subscribe(aLong -> flamoutControlBeforeSleep(),
                        throwable -> logger.error("flamoutControl", throwable));
    }

    public void flamoutControlBeforeSleep() {
        logger.debug("flamoutControlBeforeSleep:");

        GPSCtl.stopFixService();

        //关闭语音
        CarStatusUtils.setIsSpeechSleeped(true);
        VoiceManagerProxy.getInstance().cancelVoice();

        //停止天气获取
        WeatherStream.getInstance().stopService();
        //停止定位
        LocationManage.getInstance().release();

        //停止上传Obd实时数据、熄火数据的服务
        ObdManage.getInstance().release();

        //关闭热点
        WifiApAdmin.closeWifiAp(CommonLib.getInstance().getContext());

        //停止流量同步的服务
        FlowManage.getInstance().release();
        //停止长链接的服务
        NetworkManage.getInstance().release();

        //停止存储空间管理服务
        StorageSpaceService.getInstance().release();

        //停止胎压服务,关闭串口,模组下电
        TirePressureManage.getInstance().release();
        releaseTpms();

        //重置胎压报警状态
        TireFragment.setExceptionShow(false);

        //停止ADC电压获取
        ObservableFactory.getDrivingFlow().stopVoltageStream();

        //关闭obd串口，obd下电
        OBDStream.getInstance().obdStreamClose();
        GpioControl.powerOffObd();

        //安装新的Launcher应用
        LauncherUpgrade.installLauncherApk();

        //停止前置录像
        logger.debug("即将释放Camera和MediaRecorder资源");
        FrontCameraManage.getInstance().setRecordEnable(false);
        FrontCameraManage.getInstance().stopRecord();
      /*  FrontCameraInstance.getInstance().setRecorderEnable(false);
        FrontCameraInstance.getInstance().releaseRecord(false);*/

        //停止后置录像
        /*RearCameraManage.getInstance().stopPreview();
        RearCameraManage.getInstance().closeCamera();*/
        RearCameraManage.getInstance().setRecordEnable(false);
        RearCameraManage.getInstance().stopRecord();
    }

    private void cancelAllSubscription() {
        logger.debug("cancelAllSubscription");
        if (flamoutControlSubscription != null) {
            flamoutControlSubscription.unsubscribe();
        }
        if (requestVersionInfoSubsCription != null) {
            requestVersionInfoSubsCription.unsubscribe();
        }
        if (obdInitSubsCription != null) {
            obdInitSubsCription.unsubscribe();
        }

        if (tpmsInitSubsCription != null) {
            tpmsInitSubsCription.unsubscribe();
            tpmsInitSubsCription = null;
        }
        if (releaseWakeLockSubscription != null) {
            releaseWakeLockSubscription.unsubscribe();
        }
        if (updateObdSubscription != null) {
            updateObdSubscription.unsubscribe();
        }
        if (carCheckingSubsCription != null) {
            carCheckingSubsCription.unsubscribe();
        }
//        if (obdWakeupSubsCription != null) {
//            obdWakeupSubsCription.unsubscribe();
//        }
        ThreadPoolManager.getInstance(TAG).cancelTaskThreads(TAG, true);
        if (killGaodeTimer != null) {
            killGaodeTimer.cancel();
            killGaodeTimer = null;
        }
    }


    private void carCheckingWhenFire() {
        logger.debug("carCheckingWhenFire");
        long milliseconds1 = SharedPreferencesUtil.getLongValue(CommonLib.getInstance().getContext(), Contacts.CAR_CHECKING_TIME, 0);
        long milliseconds2 = System.currentTimeMillis();
        long diff = milliseconds2 - milliseconds1;
        long diffDays = diff / (24 * 60 * 60 * 1000);
        logger.debug("milliseconds1:" + milliseconds1);
        logger.debug("milliseconds2:" + milliseconds2);
        logger.debug("diffDays:" + diffDays);
        if (diffDays > 7) {
            CarCheckingProxy.getInstance().requestCarTypeAndStartCarchecking(true);
        }
    }


    private void openObd() {
        logger.debug("initObd");
        //启用obd
        GpioControl.powerOnObd();

        wakeObdTimes = 0;
        ThreadPoolManager.getInstance(TAG).cancelTaskThreads(TAG, true);
        ThreadPoolManager.getInstance(TAG).scheduleAtFixedRate(wakeUpObdThread, 6, 1, TimeUnit.SECONDS);

        OBDStream.getInstance().init();
    }

    private void closeObd() {
        //关闭obd串口，obd下电
        try {
            OBDStream.getInstance().exec("ATENTERSLEEP");
        } catch (IOException e) {
            e.printStackTrace();
        }
        OBDStream.getInstance().obdStreamClose();
        GpioControl.powerOffObd();
    }

    private void startUploadVoltageTask() {
        logger.debug(" startUploadVoltageTask ");
        if (uploadVoltageTimer == null) {
            uploadVoltageTimer = new Timer();
        }

        try {
            uploadVoltageTimer.schedule(new TimerTask() {
                @Override
                public void run() {

                    logger.debug(" fireOn uploadVoltageTimer loop ");
                    if (doUploadVoltageTimer == null) {
                        doUploadVoltageTimer = new Timer();
                    }
                    doUploadVoltageTimer.schedule(new VoltageCollectionTask(5), 10000, 2 * 1000);
                }
            }, 15 * 60 * 1000, 10 * 60 * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void stopUploadVoltageTask() {
        logger.debug(" stopUploadVoltageTask ");
        try {
            if (uploadVoltageTimer != null) {
                uploadVoltageTimer.cancel();
                uploadVoltageTimer = null;
            }

            if (doUploadVoltageTimer != null) {
                doUploadVoltageTimer.cancel();
                doUploadVoltageTimer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 延时25分钟开启熄火后的电压上传任务。
     */
    private void startFlamoutUploadVoltageTask() {
        logger.debug(" startFlamoutUploadVoltageTask ");
        if (uploadVoltageTimer == null) {
            uploadVoltageTimer = new Timer();
        }

        try {
            uploadVoltageTimer.schedule(new TimerTask() {
                @Override
                public void run() {

//                    logger.debug(" fireOut uploadVoltageTimer loop ");
                    if (isFlamoutStatus) {
                        openObd(); //先开启OBD
                    }
                    if (doUploadVoltageTimer == null) {
                        doUploadVoltageTimer = new Timer();
                    }
                    doUploadVoltageTimer.schedule(new VoltageCollectionTask(5), 0, 2 * 1000);
                }
            }, 25 * 60 * 1000, 10 * 60 * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 开启熄火后的关闭高德地图任务。
     */
    public void startFlamoutKillGaodeTask() {
        logger.debug(" startFlamoutKillGaodeTask");
        if (killGaodeTimer != null) {
            return;
        }

        killGaodeTimer = new Timer();

        try {
            killGaodeTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    logger.debug("kill amap.auto");
                    GPSCtl.off();
                    GaodeMapAppUtil.killAmapAuto();
                }
            }, 5 * 60 * 1000, 5 * 60 * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class VoltageCollectionTask extends TimerTask {

        private int count = 1;

        public VoltageCollectionTask(int times) {
            count = times;
        }

        @Override
        public void run() {
            if (count > 0) {
                count--;
                try {
//                    logger.debug("get voltage left count = " + count);
                    ObdManage.getInstance().procAccVol();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                this.cancel();
                if (isFlamoutStatus) //如果是熄火状态因为会重复开启，所以需要重复关闭obd。
                {
                    closeObd(); //关闭OBD
                }
//                logger.debug("once voltage task is over");
            }
        }
    }

    /**
     * 点火之后，切换为自动屏幕亮度调节
     */
    private void initScreenBrightnessAutoMode() {
        VolBrightnessSetting.setScreenMode(CommonLib.getInstance().getContext(), Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }
}
