package com.dudu.init;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.text.TextUtils;

import com.blur.SoundPlayManager;
import com.dudu.aios.ui.fragment.tire.TireDataPull;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.broadcast.ACCReceiver;
import com.dudu.android.launcher.exception.CrashHandler;
import com.dudu.android.launcher.service.BluetoothService;
import com.dudu.android.launcher.service.FloatBackButtonService;
import com.dudu.android.launcher.utils.BtPhoneUtils;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.android.launcher.utils.StatusBarManager;
import com.dudu.android.launcher.utils.Utils;
import com.dudu.android.launcher.utils.WifiApAdmin;
import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.utils.IPConfig;
import com.dudu.commonlib.utils.VersionTools;
import com.dudu.drivevideo.frontcamera.FrontCameraManage;
import com.dudu.drivevideo.rearcamera.RearCameraManage;
import com.dudu.drivevideo.utils.UsbControl;
import com.dudu.event.DeviceEvent;
import com.dudu.map.GaodeMapAppUtil;
import com.dudu.map.NavigationProxy;
import com.dudu.monitor.repo.location.LocationManage;
import com.dudu.navi.NavigationManager;
import com.dudu.service.GpsNmeaManager;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voip.VoipSDKCoreHelper;
import com.dudu.weather.WeatherStream;
import com.dudu.workflow.common.CommonParams;
import com.dudu.workflow.common.DataFlowFactory;
import com.dudu.workflow.common.ObservableFactory;
import com.dudu.workflow.driving.CarStatusUtils;
import com.dudu.workflow.push.ReceiverDataFlow;
import com.networkbench.agent.impl.NBSAppAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import ch.qos.logback.core.android.SystemPropertiesProxy;
import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by 赵圣琪 on 2015/11/24.
 */
public class InitManager {

    private static InitManager mInstance;

    private static Logger logger = LoggerFactory.getLogger("init.manager");
    private Subscription releaseCameraSubscription;

    private int log_step = 0;

    private Context mContext;

    private HandlerThread mInitThread;

    private Handler mInitHandler;
    private boolean entered;

    private InitManager() {

        mContext = LauncherApplication.getContext();

        entered = false;

        mInitThread = new HandlerThread("init thread");
        mInitThread.start();

        mInitHandler = new Handler(mInitThread.getLooper());
    }

    public static InitManager getInstance() {
        if (mInstance == null) {
            mInstance = new InitManager();
        }

        return mInstance;
    }

    public synchronized boolean init() {
        logger.debug("[init][{}] init call:{}", log_step++, entered);
        if (entered) return true;
        logger.debug("[init][{}]初始化", log_step++);
        entered = true;
        NBSAppAgent.setLicenseKey("820f83526db145d6974a3fe22ae7ac7a").withLocationServiceEnabled(true).start(mContext);

        mInitHandler.post(new Runnable() {
            @Override
            public void run() {
                appInit(LauncherApplication.getContext());
                initOthers();
            }
        });
        //延时启动蓝牙服务
        mInitHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                logger.debug("[init][{}]打开蓝牙", log_step++);
                openBlueTooth();
                int count = 0;//检查蓝牙状态计数
                //循环检查蓝牙状态
                while (true) {
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (bluetoothAdapter != null) {
                        logger.debug("[init][{}]检查蓝牙状态 state:" + bluetoothAdapter.getState(), log_step++);
                        if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {

                            logger.debug("[init][{}]初始化蓝牙设备名称", log_step++);
                            BtPhoneUtils.initBluetoothDeviceName();
                            logger.debug("[init][{}]设置蓝牙可见性时间为永不超时", log_step++);
                            int time = 0;//0为永不超时
                            BtPhoneUtils.setDiscoverableTimeout(time);
                            logger.debug("[init][{}]开启蓝牙电话服务", log_step++);
                            startBluetoothService();
                            break;
                        }
                    }
                    try {
                        logger.debug("[init][{}]等待5秒再去检查蓝牙状态 count:" + count, log_step++);
                        Thread.sleep(5000);
                        count++;
                        if (count == 10) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 5000);

        GaodeMapAppUtil.killAmapAuto();
        return true;
    }

    public void unInit() {
        logger.debug("程序崩溃，释放语音资源");
        VoiceManagerProxy.getInstance().stopSpeaking();
        VoiceManagerProxy.getInstance().stopUnderstanding();
        VoiceManagerProxy.getInstance().onDestroy();
        logger.debug("程序崩溃，释放录像资源");
        RearCameraManage.getInstance().release();
        logger.debug("程序崩溃，释放voip资源");
        VoipSDKCoreHelper.getInstance().uninit();
        //FrontCameraManage.getInstance().release();

        //chad add
//        stopBluetoothService();//不主动停止蓝牙服务，保持后台长期运行
        mInitThread.quitSafely();

        logger.debug("程序崩溃，结束导航");
        NavigationProxy.getInstance().existNavi();
    }

    /**
     * 开启悬浮按钮服务
     */
    private void startFloatButtonService() {
        Intent i = new Intent(mContext, FloatBackButtonService.class);
        mContext.startService(i);
    }

    /**
     * 开启蓝牙电话服务
     */
    private void startBluetoothService() {
//        stopBluetoothService();
        Intent intent = new Intent(mContext, BluetoothService.class);
        mContext.startService(intent);
    }

    /**
     * 停止蓝牙电话服务
     * chad add
     */
    private void stopBluetoothService() {
        Intent intent = new Intent(mContext, BluetoothService.class);
        mContext.stopService(intent);
    }

    /**
     * 打开蓝牙
     */
    private void openBlueTooth() {
//        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(
//                Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//bluetoothManager.getAdapter();
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
        }
    }

    /**
     * 工厂检测,暂不做检测
     */
    private boolean checkBTFT() {
        SystemPropertiesProxy sps = SystemPropertiesProxy.getInstance();
        boolean need_bt = !"1".equals(sps.get("persist.sys.bt", "0"));
        boolean need_ft = !"1".equals(sps.get("persist.sys.ft", "0"));
        Intent intent;
        PackageManager packageManager = mContext.getPackageManager();
        intent = packageManager.getLaunchIntentForPackage("com.qualcomm.factory");
        if (intent != null) {
            //close wifi ap for ft test
            WifiApAdmin.closeWifiAp(mContext);

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            return false;
        } else {
            initOthers();
            return true;
        }
    }

    private void initOthers() {
        logger.info("launcher版本名：{}", VersionTools.getAppVersion(CommonLib.getInstance().getContext()));
        logger.info("launcher版本号：{}", VersionTools.getAppVersionCode(CommonLib.getInstance().getContext()));

        // 正式版本:切换usb为Host模式,取消adb端口
        if (!Utils.isDemoVersion(mContext)) {
            logger.debug("usbHostState:{}", UsbControl.usbHostState);
            RearCameraManage.getInstance().resetUsbMode();
            com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().set(mContext,
                    "persist.sys.usb.config", "charging");
        } else {
            //没写IMEI号,切换usb为Client,保持屏幕常亮
            UsbControl.setToClient();
            CarFireManager.getInstance().acquireLock();
        }

        String enable = com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().get("persist.sys.gps.nmea", "0");
        if (TextUtils.equals(enable, "1")) {
            GpsNmeaManager.getInstance();

            rx.Observable.timer(1, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aLong -> {
                        logger.debug("[init][{}]启动GPS Nmea采集");

                        GpsNmeaManager.getInstance().addGpsNmeaListener();
                    }, throwable -> logger.error("GpsNmeaManager", throwable));
        }


        rx.Observable.timer(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {

                }, throwable -> logger.error("initOthers", throwable));

        logger.debug("[init][{}]启动悬浮返回按钮服务", log_step++);
        startFloatButtonService();

        NavigationManager.getInstance(LauncherApplication.getContext()).initNaviManager();
        VoiceManagerProxy.getInstance().onInit();

        //轮胎预警范围配置初始化
        TireDataPull.instance().init();
    }


    private void initBluetooth() {

    }

    private void screenOnOrOff() {
        EventBus.getDefault().post(new DeviceEvent.Screen(DeviceEvent.ON));
    }

    public void bootInit(Context context) {
        laucherApplicationInit(context);
    }

    public void appInit(Context context) {
        laucherApplicationInit(context);
    }

    public void laucherApplicationInit(Context context) {

        if (Constants.DEBUG_STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyDialog()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build());
        }

        CrashHandler crashHandler = CrashHandler.getInstance();

        IPConfig.getInstance().init();

        // 注册crashHandler
        crashHandler.init(context);

        DataFlowFactory.init();

        CommonParams.getInstance().init();
        ObservableFactory.init();

        ReceiverDataFlow.getInstance().init();

        SoundPlayManager.init(context, R.raw.take_photo2);//初始化拍照音效

        StatusBarManager.getInstance().initBarStatus();

        screenOnControl(true);


        CarFireManager.getInstance().doIfFired();

        cancelServiceIfNotFired();
    }

    public void screenOnControl(boolean initFlag) {
        LoggerFactory.getLogger("video.ScreenReceiver").debug("screenOnControl");
        if (!initFlag){
            if (!UsbControl.usbHostState && !Utils.isDemoVersion(mContext)) {
                UsbControl.setToHost();
            }
        }

        if (releaseCameraSubscription != null) {
            releaseCameraSubscription.unsubscribe();
            releaseCameraSubscription = null;
        }
        CarFireManager.getInstance().wakeScreen();
        FrontCameraManage.getInstance().init();
        FrontCameraManage.getInstance().startPreview();
//        FrontCameraInstance.getInstance().startPreview();
        ACCReceiver.proDrivingView();

        WeatherStream.getInstance().startService();
        LocationManage.getInstance().init();
    }

    public void screenOffControl() {
        LoggerFactory.getLogger("video.ScreenReceiver").debug("screenOffControl");
        RearCameraManage.getInstance().stopPreview();

        if (releaseCameraSubscription != null) {
            releaseCameraSubscription.unsubscribe();
            releaseCameraSubscription = null;
        }

        releaseCameraSubscription = CarStatusUtils.isFired()
                .filter(fired -> !fired)
                .delay(1, TimeUnit.MINUTES)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        notFired -> {
                            LoggerFactory.getLogger("video.ScreenReceiver").debug("即将释放Camera和MediaRecorder资源");
                            LocationManage.getInstance().release();
                            RearCameraManage.getInstance().closeCamera();
                            RearCameraManage.getInstance().stopRecord();
                            RearCameraManage.getInstance().UsbToClient();

                            FrontCameraManage.getInstance().release();

                            com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
                                    setCmd(com.dudu.android.hideapi.SystemPropertiesProxy.SYS_OBD_SLEEP, com.dudu.android.hideapi.SystemPropertiesProxy.high);

                            CarFireManager.getInstance().startFlamoutKillGaodeTask();

                            WeatherStream.getInstance().stopService();
                        }
                        , throwable -> logger.error("obdWorkStart", throwable));
    }


    /**
     * 关闭掉开机启动时开启的服务（语音唤醒、GPS、天气）
     */
    private void cancelServiceStartedAtApplicationInit() {
        logger.debug("cancelServiceStartedAtApplicationInit");
        //停止天气获取
        WeatherStream.getInstance().stopService();
        //停止定位
        LocationManage.getInstance().release();
        //关闭语音
        CarStatusUtils.setIsSpeechSleeped(true);
        VoiceManagerProxy.getInstance().cancelVoice();

        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
                setCmd(com.dudu.android.hideapi.SystemPropertiesProxy.SYS_OBD_SLEEP, com.dudu.android.hideapi.SystemPropertiesProxy.high);

        CarFireManager.getInstance().startFlamoutKillGaodeTask();
    }

    public void cancelServiceIfNotFired() {
        logger.debug("cancelServiceIfNotFired");
        int time_cancel = 5;
        if (Utils.isDemoVersion(mContext)) {
            time_cancel = 1;
        }
        Observable.timer(time_cancel, TimeUnit.MINUTES)
                .map(aLong -> CarStatusUtils.isCarFiredByFile())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fired -> {
                            if (fired) {
                                logger.info("开机5分钟后，系统处于点火状态，不关闭服务");
                                CarStatusUtils.saveCarIsFire(true);
                            } else {
                                logger.info("开机5分钟后，系统未点火，关闭语音、定位、天气服务");
                                CarStatusUtils.saveCarIsFire(false);
                                cancelServiceStartedAtApplicationInit();
                            }
                        }
                        , throwable -> logger.error("doIfFired", throwable));
    }

}
