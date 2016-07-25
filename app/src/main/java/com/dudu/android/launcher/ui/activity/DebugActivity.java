package com.dudu.android.launcher.ui.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.blur.CameraControl;
import com.blur.SurfaceWindow;
import com.dudu.aios.ui.activity.MainRecordActivity;
import com.dudu.aios.ui.activity.OtherApkActivity;
import com.dudu.aios.ui.fragment.tire.TireDataPull;
import com.dudu.aios.ui.fragment.tire.TireValueHelper;
import com.dudu.aios.ui.utils.InstallerUtils;
import com.dudu.aios.ui.utils.Rx;
import com.dudu.aios.ui.view.SpinnerItem;
import com.dudu.android.hideapi.SystemPropertiesProxy;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.broadcast.ACCReceiver;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.android.launcher.utils.WifiApAdmin;
import com.dudu.android.libserial.SerialManager;
import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.utils.IPConfig;
import com.dudu.commonlib.utils.ToastUtils;
import com.dudu.commonlib.utils.process.ProcessUtils;
import com.dudu.commonlib.utils.shell.ShellExe;
import com.dudu.commonlib.utils.shell.ShellExeTest;
import com.dudu.drivevideo.frontcamera.FrontCameraInstance;
import com.dudu.drivevideo.frontcamera.FrontCameraManage;
import com.dudu.drivevideo.rearcamera.RearCameraManage;
import com.dudu.drivevideo.service.FrontDriveVideoService;
import com.dudu.drivevideo.spaceguard.StorageSpaceService;
import com.dudu.drivevideo.utils.FileUtil;
import com.dudu.drivevideo.utils.UsbControl;
import com.dudu.monitor.obd.ObdManage;
import com.dudu.monitor.obdUpdate.ObdUpdateService;
import com.dudu.monitor.obdUpdate.config.ObdUpdateCmd;
import com.dudu.monitor.repo.SensorManage;
import com.dudu.monitor.tirepressure.TirePairSp;
import com.dudu.monitor.tirepressure.TirePressureManage;
import com.dudu.navi.event.NaviEvent;
import com.dudu.network.NetworkManage;
import com.dudu.rest.model.driving.response.GetCarBrandResponse;
import com.dudu.service.GpsNmeaManager;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.workflow.common.DataFlowFactory;
import com.dudu.workflow.common.RequestFactory;
import com.dudu.workflow.driving.CarStatusUtils;
import com.dudu.workflow.obd.CarLock;
import tm.dudu.ext.GpioControl;
import com.dudu.workflow.obd.OBDStream;
import com.dudu.workflow.obd.ObdUpdateFlow;
import com.dudu.workflow.push.ReceiverDataFlow;
import com.dudu.workflow.push.model.PushParams;
import com.dudu.workflow.push.model.ReceiverPushData;
import com.dudu.workflow.tpms.TPMSFlow;
import com.dudu.workflow.tpms.TPMSInfo;
import com.dudu.workflow.tpms.TpmsStream;
import com.dudu.workflow.upgrade.LauncherUpgrade;

import org.slf4j.LoggerFactory;
import org.wysaid.camera.CameraInstance;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import tm.dudu.ext.GPSCtl;
import tm.dudu.ext.PortalCtl;

/**
 * Created by lxh on 2016/1/2.
 */
public class DebugActivity extends Activity implements View.OnClickListener {

    // added by huxuemin 2016-07-05 for amapauto copy begin
    private final static String AmapAutoSrcPath = "/storage/sdcard1/amapauto";
    private final static String AmapAutoDestPath = "/storage/sdcard0/amapauto";
    private TextView textView_ip;
    private TextView textView_socket;
    private TextView textView_obd;
    private TextView textView_stream;
    private TextView tpmsPrint;
    private Button btn_reset;
    private Button btnBack;
    private Button btnOpenGsp;
    private Button btnAmapautoCopy;    // added by huxuemin 2016-07-05 for amapauto copy
    private Button btnOpenMap;
    private Button btnOpenCamera;
    private Button btnOpenMusic;
    private Button btnOpenAdb;
    private Button btnCloseWifi;
    private Button btnCloseVideo;
    private Button btnStartVideo;
    private Button btnCloseVoice;
    private Button btnOpenFactoryTest;
    private Button btnOpenUCBrower;
    private Button btnOPenRecord;
    private Button btnOpenDial;
    private Button btnOpenOtherApk;
    private Button btnMobileData;
    private Spinner carTypeCodeSpinner;
    private RadioGroup radioGroup;
    private RadioButton radioBtnFormal, radioBtnTest;
    private IPConfig ipConfig;
    private boolean isTest = true;
    private String ip, testIP;
    private int port, testPort;
    private HashMap<Integer, String> obdCarMap;
    /* @Override
     protected View getChildView() {
         return LayoutInflater.from(this).inflate(R.layout.ip_congfig_layout, null);
     }*/
    private Subscription FDTestSub = null;
    private Subscription tpmsSub = null;
    private Subscription tpmsPairSub = null;
    private Subscription setCarTypeSubscription;
    private Subscription obdCheckVersionSubscription;
    private Subscription testDestoryVoiceSub;
    private Subscription testInitVoiceSub;
    private Subscription obdQureyAdcVolSubscription;
    private Subscription obdQureyStartVolSubscription;

    // added by huxuemin 2016-07-05 for amapauto copy end
    public static boolean isMultiMic() {
        boolean ret = false;
        String mic = com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().get("persist.sys.mic.multi", "0");
        if (TextUtils.equals(mic, "0")) {
            ret = false;
        } else if (TextUtils.equals(mic, "1")) {
            ret = true;
        }

        return ret;
    }

    public static boolean isGPSNmeaStart() {
        boolean ret = false;
        String enable = com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().get("persist.sys.gps.nmea", "0");
        if (TextUtils.equals(enable, "0")) {
            ret = false;
        } else if (TextUtils.equals(enable, "1")) {
            ret = true;
        }

        return ret;
    }

    public static boolean isLogcatEnable() {
        boolean ret = false;
        String enable = com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().get("persist.sys.logcat.enable", "0");
        if (TextUtils.equals(enable, "0")) {
            ret = false;
        } else if (TextUtils.equals(enable, "1")) {
            ret = true;
        }

        return ret;
    }

    public void setLogcatEnable(View view) {
        if (isLogcatEnable()) {
            com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().set(this, "persist.sys.logcat.enable", "0");
            ((TextView) view).setText("开启Logcat");
        } else {
            com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().set(this, "persist.sys.logcat.enable", "1");
            ((TextView) view).setText("关闭Logcat");
        }
    }

    public void onGPSNmea(View view) {
        if (isGPSNmeaStart()) {
            GpsNmeaManager.getInstance().removeGpsNmeaListener();
            com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().set(this, "persist.sys.gps.nmea", "0");
            ((TextView) view).setText("GPS NMEA开始保存");
        } else {
            GpsNmeaManager.getInstance().addGpsNmeaListener();
            com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().set(this, "persist.sys.gps.nmea", "1");
            ((TextView) view).setText("GPS NMEA停止保存");
        }
    }

    public boolean isFDStart() {
        return FDTestSub != null;
    }

    public void initFDTest(TextView view) {
        if (isFDStart()) {
            view.setText("停止防盗测试");
        } else {
            view.setText("开始防盗测试");
        }
    }

    public void onFDTest(View view) {
        if (isFDStart()) {
            if (FDTestSub != null) {
                FDTestSub.unsubscribe();
                FDTestSub = null;
            }
            ((TextView) view).setText("开始防盗测试");
        } else {
            FDTestSub = Observable.interval(2, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aLong -> {
                                if (aLong % 2 == 0) {
                                    CarLock.lockCar();
                                } else {
                                    CarLock.unlockCar();
                                }
                                ((TextView) view).setText("停止防盗测试,已测:" + aLong);
                            },
                            throwable -> {
                            });
            ((TextView) view).setText("停止防盗测试");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_layout);
        initView(savedInstanceState);
        initListener();
    }

    public void initView(Bundle savedInstanceState) {
        ipConfig = IPConfig.getInstance();

        textView_ip = (TextView) findViewById(R.id.ip_textview);

        textView_socket = (TextView) findViewById(R.id.websocket_textview);

        textView_obd = (TextView) findViewById(R.id.obd_textview);

        textView_stream = (TextView) findViewById(R.id.stream_textview);

        btn_reset = (Button) findViewById(R.id.btn_ip_reset);

        btnOpenGsp = (Button) findViewById(R.id.openGps);

        // added by huxuemin 2016-07-05 for amapauto copy begin
        btnAmapautoCopy = (Button) findViewById(R.id.amapautoCopy);
        boolean exists = fileIsExists(AmapAutoDestPath);
        long freeSpace = getUsableSpace(AmapAutoDestPath);
        Log.d("AmapautoCopy", "exists:" + exists + " free:" + freeSpace);
        if (exists && freeSpace < 7 * 1024 * 1024 * 1024L) {
            btnAmapautoCopy.setVisibility(View.VISIBLE);
            btnAmapautoCopy.setText("离线导航拷贝完成");
            btnAmapautoCopy.setTextColor(Color.GRAY);
            btnAmapautoCopy.setClickable(false);
        } else if (fileIsExists(AmapAutoSrcPath)) {
            btnAmapautoCopy.setVisibility(View.VISIBLE);
            btnAmapautoCopy.setText("离线导航拷贝");
            btnAmapautoCopy.setTextColor(Color.WHITE);
        }
        // added by huxuemin 2016-07-05 for amapauto copy end

        btnOpenMap = (Button) findViewById(R.id.openRMap);

        radioGroup = (RadioGroup) findViewById(R.id.ip_radioGroup);

        radioBtnFormal = (RadioButton) findViewById(R.id.radioBtnFormal);

        radioBtnTest = (RadioButton) findViewById(R.id.radioBtnTest);

        btnBack = (Button) findViewById(R.id.back_button);

        btnOpenCamera = (Button) findViewById(R.id.button_open_camera);

        btnOpenMusic = (Button) findViewById(R.id.button_open_music);

        btnOpenAdb = (Button) findViewById(R.id.openAdb);

        btnCloseVideo = (Button) findViewById(R.id.closeVideo);
        btnStartVideo = (Button) findViewById(R.id.startVideo);

        btnCloseVoice = (Button) findViewById(R.id.closeVoice);

        btnCloseWifi = (Button) findViewById(R.id.closeWifi);

        btnOpenFactoryTest = (Button) findViewById(R.id.openFactoryTest);

        btnOpenUCBrower = (Button) findViewById(R.id.open_UC_browser);

        btnOPenRecord = (Button) findViewById(R.id.open_record);

        btnOpenDial = (Button) findViewById(R.id.open_dial);

        btnOpenOtherApk = (Button) findViewById(R.id.openOtherApk);

        btnMobileData = (Button) findViewById(R.id.mobileData);

        initMicView((TextView) findViewById(R.id.buttonMic));
        initLogcatEnable((TextView) findViewById(R.id.buttonLogcat));
        initGPSNmea((TextView) findViewById(R.id.btn_gpsNmea));
        initFDTest((TextView) findViewById(R.id.buttonFDTest));
        initStreamView((TextView) findViewById(R.id.buttonLibStream));
        initRecordView((TextView) findViewById(R.id.buttonStartRecorder));

        carTypeCodeSpinner = (Spinner) findViewById(R.id.cartypecode);
        List<SpinnerItem> spinnerItems = new ArrayList<SpinnerItem>();
        obdCarMap = new HashMap<>();

        obdCarMap.put(0, "标准");
        obdCarMap.put(1, "路虎");
        obdCarMap.put(2, "丰田");
        obdCarMap.put(3, "奔驰");
        obdCarMap.put(4, "宝马");
        obdCarMap.put(5, "福特");
        obdCarMap.put(6, "通用");
        obdCarMap.put(7, "本田");
        obdCarMap.put(8, "起亚");
        obdCarMap.put(9, "大众");
        obdCarMap.put(10, "现代");
        obdCarMap.put(11, "马自达");
        obdCarMap.put(12, "日产");
        obdCarMap.put(13, "沃尔沃");
        obdCarMap.put(14, "吉普");
        obdCarMap.put(15, "长城");
        obdCarMap.put(16, "海马");
        obdCarMap.put(17, "标致");

        for (Object o : obdCarMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            Integer key = (Integer) entry.getKey();
            String val = String.valueOf(entry.getValue());
            spinnerItems.add(new SpinnerItem(key, val));
        }

        ArrayAdapter<SpinnerItem> adapter = new ArrayAdapter<SpinnerItem>(this,
                android.R.layout.simple_spinner_item, spinnerItems);

        carTypeCodeSpinner.setAdapter(adapter);

        isTest = ipConfig.isTest_Server();
        if (isTest) {
            radioBtnTest.setChecked(true);
        } else {
            radioBtnFormal.setChecked(true);
        }

        changeIpConfigViews();

        tpmsPrint = (TextView) findViewById(R.id.tpms_print);

        btnMobileData.setText(getMobileDataState() ? "关闭移动网络" : "开启移动网络");
    }

    private void changeIpConfigViews() {
        textView_ip.setText(getString(R.string.ip_server) + ipConfig.getSERVER_ADDRESS());
        textView_obd.setText(getString(R.string.obd_server) + ipConfig.getServerIP() + ":" + ipConfig.getServerPort());
        textView_socket.setText(getString(R.string.socket_server) + ipConfig.getSOCKET_ADDRESS() + ":" + ipConfig.getSOCKET_PORT());
        textView_stream.setText(getString(R.string.stream_server) + ipConfig.getmTcpIp() + ":" + ipConfig.getmTcpPort());
    }

    public void initListener() {

        btn_reset.setOnClickListener(this);

        btnBack.setOnClickListener(this);

        btnOpenGsp.setOnClickListener(this);

        btnOpenMap.setOnClickListener(this);

        btnOpenMusic.setOnClickListener(this);

        btnOpenCamera.setOnClickListener(this);

        btnOpenAdb.setOnClickListener(this);

        btnCloseVoice.setOnClickListener(this);

        btnCloseWifi.setOnClickListener(this);


        btnOpenFactoryTest.setOnClickListener(this);

        btnOpenUCBrower.setOnClickListener(this);

        btnOPenRecord.setOnClickListener(this);

        btnOpenDial.setOnClickListener(this);

        btnOpenOtherApk.setOnClickListener(this);

        btnMobileData.setOnClickListener(this);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioBtnFormal) {
                    isTest = false;
                } else if (checkedId == R.id.radioBtnTest) {
                    isTest = true;
                }
                ipConfig.setTest_Server(isTest);
                changeIpConfigViews();
                ReceiverDataFlow.getInstance().init();
                NetworkManage.getInstance().init();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_ip_reset:
                reset();
                break;
            case R.id.back_button:
                startActivity(new Intent(DebugActivity.this, MainRecordActivity.class));
                finish();
                break;
            case R.id.openRMap:
                openMap();
                break;
            case R.id.openGps:
                openGps();
                break;
            case R.id.button_open_camera:
                openCamera();
                break;
            case R.id.button_open_music:
                openMusic();
                break;
            case R.id.openAdb:
                openAdb();
                break;
            case R.id.closeVoice:
                closeVoice();
                break;
            case R.id.closeWifi:
                closeWifi();
                break;
            case R.id.openFactoryTest:
                openFactoryTest();
                break;
            case R.id.open_UC_browser:
                oepnBrower();
                break;
            case R.id.open_record:
                openRecord();
                break;
            case R.id.open_dial:
                openDial();
                break;
            case R.id.openOtherApk:
                openOtherApk();
                break;
            case R.id.mobileData:
                mobileData();
                break;
        }
    }

    public void openVoice(View v) {
        VoiceManagerProxy.getInstance().onInit();
    }

    private void openOtherApk() {
        startActivity(new Intent(this, OtherApkActivity.class));
    }

    public void mobileData() {
        boolean pBoolean = getMobileDataState();

        try {

            ConnectivityManager mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Class ownerClass = mConnectivityManager.getClass();

            Class[] argsClass = new Class[1];
            argsClass[0] = boolean.class;

            Method method = ownerClass.getMethod("setMobileDataEnabled", argsClass);

            method.invoke(mConnectivityManager, !pBoolean);
            btnMobileData.setText(pBoolean ? "开启移动网络" : "关闭移动网络");
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("poecao", "移动数据设置错误: " + e.toString());
        }
    }

    public boolean getMobileDataState() {
        try {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Class ownerClass = mConnectivityManager.getClass();

            Class[] argsClass = null;
            Object[] arg = null;

            Method method = ownerClass.getMethod("getMobileDataEnabled", argsClass);

            Boolean isOpen = (Boolean) method.invoke(mConnectivityManager, arg);

            return isOpen;

        } catch (Exception e) {
            Log.d("poecao", "得到移动数据状态出错");
            return false;
        }
    }

    public void obdGeneral(View view) {
        try {
            SpinnerItem spinnerItem = (SpinnerItem) carTypeCodeSpinner.getSelectedItem();
            setCarTypeSubscription = OBDStream.getInstance().OBDSetCarType()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            result -> {
                                setCarTypeSubscription.unsubscribe();
                                ToastUtils.showTip("设置车型为" + spinnerItem.getValue() + "成功");
                            },
                            throwable -> {
                                setCarTypeSubscription.unsubscribe();
                                ToastUtils.showTip("设置车型为" + spinnerItem.getValue() + "失败，请重试");
                            });
            OBDStream.getInstance().exec("ATSETVEHICLE=" + spinnerItem.getID());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openDial() {
        InstallerUtils.openApp(this, "com.android.dialer");
        EventBus.getDefault().post(NaviEvent.FloatButtonEvent.SHOW);
    }

    private void oepnBrower() {
        InstallerUtils.openApp(this, "com.UCMobile");
        EventBus.getDefault().post(NaviEvent.FloatButtonEvent.SHOW);
    }

    private void openRecord() {
        InstallerUtils.openApp(this, "com.android.soundrecorder");
        EventBus.getDefault().post(NaviEvent.FloatButtonEvent.SHOW);
    }

    private void openFactoryTest() {
        PackageManager packageManager = getPackageManager();
        startActivity(new Intent(packageManager.getLaunchIntentForPackage("com.qualcomm.factory")));
        EventBus.getDefault().post(NaviEvent.FloatButtonEvent.SHOW);
    }

    private void closeWifi() {
        WifiApAdmin.closeWifiAp(this);
        btnCloseWifi.setText("热点已关闭");
    }

    private void closeVoice() {
        VoiceManagerProxy.getInstance().onDestroy();
        btnCloseVoice.setText("语音已关闭");
    }

    private void openAdb() {
        UsbControl.setToClient();
        Observable.timer(5, TimeUnit.SECONDS, Schedulers.io())
                .subscribe(l ->
                                com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
                                        set(this, "persist.sys.usb.config", "diag,serial_smd,rmnet_bam,adb")
                        , throwable -> {
                        });
    }

    private void openMusic() {
        InstallerUtils.openApp(this, "com.tencent.qqmusic");
        EventBus.getDefault().post(NaviEvent.FloatButtonEvent.SHOW);
    }

    private void openCamera() {
        FrontDriveVideoService.getInstance().release();
        CameraInstance.getInstance().stopCamera();
        InstallerUtils.openApp(this, "com.android.camera2");
        EventBus.getDefault().post(NaviEvent.FloatButtonEvent.SHOW);
    }

    private void openGps() {
        InstallerUtils.openApp(this, "com.chartcross.gpstestplus");
        EventBus.getDefault().post(NaviEvent.FloatButtonEvent.SHOW);
    }

    private void openMap() {
        InstallerUtils.openApp(this, "org.gyh.rmaps");
        EventBus.getDefault().post(NaviEvent.FloatButtonEvent.SHOW);
    }

    private void reset() {
        radioBtnFormal.setChecked(true);
//        changeIpConfigViews();
//        ipConfig.setTest_Server(true);
    }

    public void enterSetting(View view) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        EventBus.getDefault().post(NaviEvent.FloatButtonEvent.SHOW);
    }

    public void startFactory(View view) {
        Observable
                .just(stopFunctions())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(l -> {
                    PackageManager packageManager = getPackageManager();
                    startActivity(new Intent(packageManager.getLaunchIntentForPackage("com.qualcomm.factory")));
                }, throwable -> {
                });
    }

    private boolean stopFunctions() {
        ACCReceiver.isFactroyIng = true;

        if (getMobileDataState()) {
            mobileData();
        }
        //关闭语音
        VoiceManagerProxy.getInstance().onDestroy();

        //关闭Portal
        if (CommonLib.getInstance().getVersionManage().isPortalNeeded()) {
            PortalCtl.stop();
        }

        onStopFrontVideo(null);

        SurfaceWindow.hideWindowDialog();

        //关闭热点
        WifiApAdmin.closeWifiAp(this);

        //stop bluetooth
        ObdManage.getInstance().release();
        OBDStream.getInstance().obdStreamClose();
        TirePressureManage.getInstance().release();
        TpmsStream.getInstance().tpmsStreamClose();

        FrontCameraManage.getInstance().release();
        RearCameraManage.getInstance().release();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().post(NaviEvent.FloatButtonEvent.HIDE);
    }

    public void switchUSBCamera(View view) {
        UsbControl.setToHost();
//        ObservableFactory.getInstance().getCommonObservable().startRearPreview();
//        startActivity(new Intent(DebugActivity.this, MainRecordActivity.class));
//        finish();
    }

    public void switchADB(View view) {
        UsbControl.setToClient();
//        ObservableFactory.getInstance().getCommonObservable().stopRearPreview();
//        startActivity(new Intent(DebugActivity.this, MainRecordActivity.class));
//        finish();
    }

    public void onStopFrontVideo(View view) {
//        btnCloseVideo.setText("录像已关闭");
        LoggerFactory.getLogger("video1.frontdrivevideo").info("关闭前置录像");
        FrontCameraManage.getInstance().stopRecord();
        FrontCameraManage.getInstance().setRecordEnable(false);
    }

    public void onStartFrontVideo(View view) {
//        btnStartVideo.setText("录像已开启");
        LoggerFactory.getLogger("video1.frontdrivevideo").info("开启前置录像");
        FrontCameraManage.getInstance().setRecordEnable(true);
        FrontCameraManage.getInstance().startRecord();
    }

    public void onInitFontCamera(View view) {
        FrontCameraManage.getInstance().init();
    }

    public void onReleaseFontCamera(View view) {
        FrontCameraManage.getInstance().release();
    }

    public void startUSBCameraRecord(View view) {
        //后门开启后置摄像头
        ToastUtils.showToast("开启后置录像");
        RearCameraManage.getInstance().setRecordEnable(true);
        RearCameraManage.getInstance().startRecord();
    }

    public void stopUSBCameraRecord(View view) {
        if (view != null) {
            ToastUtils.showToast("停止后置录像");
        }
        RearCameraManage.getInstance().stopRecord();
        RearCameraManage.getInstance().setRecordEnable(false);
    }

    /**
     * 清理realm数据库
     */
    public void cleanRealm(View view) {
        cleanVideo(view);
        cleanPhoto(view);
    }

    public void testActivity(View view) {
//        Context context = view.getContext();
//        context.startActivity(new Intent(context, MonitorActivity.class));

        new Thread() {
            @Override
            public void run() {
                TireDataPull.notifyTPMS(20, 4.f, 3.f, 6.f, 2.f, null, null);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                TPMSFlow.TPMSGetTempParam();
                TPMSFlow.TPMSGetParamStream()
                        .subscribe(tpmsParamBean -> {
                                    if (TireValueHelper.isTPMSParamRight(tpmsParamBean, 20, 4f, 3f, 6f, 2f)) {
                                        Log.e("angcyo", "配置设置成功");
                                    } else {
                                        Log.e("angcyo", "配置设置失败");
                                    }
                                }, throwable -> Log.e("angcyo", "失败:" + throwable.getMessage()),
                                () -> Log.e("angcyo", "调用结束."));
            }
        }.start();
    }

    /**
     * 清理前后置视频,包括缩略图
     */
    public void cleanVideo(View view) {
        Rx.base("", s -> {
            FileUtil.delectAllFiles(new File("/storage/sdcard1/dudu/video"));
            FileUtil.delectAllFiles(new File("/storage/sdcard1/dudu/thumbnail"));
            FileUtil.delectAllFiles(new File("/storage/sdcard1/dudu/frontVideo"));
            FileUtil.delectAllFiles(new File("/storage/sdcard1/dudu/frontVideoThumbnail"));

            FileUtil.delectAllFiles(new File("/storage/sdcard1/dudu/rearVideo"));
            FileUtil.delectAllFiles(new File("/storage/sdcard1/dudu/rearVideoThumbnail"));
            return "";
        });
    }

    /**
     * 清理前后置图片
     */
    public void cleanPhoto(View view) {
        Rx.base("", s -> {
            FileUtil.delectAllFiles(new File("/storage/sdcard1/dudu/photos"));
            FileUtil.delectAllFiles(new File("/storage/sdcard1/dudu/frontPicture"));
            return "";
        });
    }

    @Override
    protected void onDestroy() {
        if (tpmsSub != null) tpmsSub.unsubscribe();
        if (tpmsPairSub != null) tpmsPairSub.unsubscribe();
        if (FDTestSub != null) FDTestSub.unsubscribe();
        super.onDestroy();
    }

    public void tpms6601(View view) {
        TPMSFlow.TPMSPairStart(TPMSInfo.POSITION.RIGHT_FRONT);
    }

    public void tpms6602(View view) {
        TPMSFlow.TPMSPairStart(TPMSInfo.POSITION.LEFT_FRONT);
    }

    public void tpms6603(View view) {
        TPMSFlow.TPMSPairStart(TPMSInfo.POSITION.RIGHT_BACK);
    }

    public void tpms6604(View view) {
        TPMSFlow.TPMSPairStart(TPMSInfo.POSITION.LEFT_BACK);
    }

    public void tpms11(View view) {
        byte[] cmd = {(byte) 0xAA, 0x41, (byte) 0xA1, 0x07, 0x11, 0x00};
        TpmsStream.getInstance().write(cmd);
    }

    public void tpmsSub(View view) {
        if (TPMSFlow.TPMSWarnInfoStream() == null || TPMSFlow.TPMSPairStream() == null) {
            return;
        }

        tpmsSub = TPMSFlow.TPMSWarnInfoStream()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tpmsWarnInfo -> {
                    Log.d("TPMS", "info: " + tpmsWarnInfo);
                    tpmsPrint.append(tpmsWarnInfo.toString());
                }, throwable -> Log.e("DebugActivity", "tpmsSub: ", throwable));

        tpmsPairSub = TPMSFlow.TPMSPairStream()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> {
                    Log.d("TPMS", "pair: " + pair);
                    tpmsPrint.append(pair + "对码成功\n");
                }, throwable -> Log.e("DebugActivity", "tpmsSub: ", throwable));

    }

    public void tpmsUnSub(View view) {
        if (tpmsSub != null) tpmsSub.unsubscribe();
        if (tpmsPairSub != null) tpmsPairSub.unsubscribe();
    }

    public void cancelPair(View view) {
        TirePairSp.setLBPair(false);
        TirePairSp.setLFPair(false);
        TirePairSp.setRFPair(false);
        TirePairSp.setRBPair(false);

        TirePressureManage.uploadEmptyTirePressureData(1);
        TirePressureManage.uploadEmptyTirePressureData(2);
        TirePressureManage.uploadEmptyTirePressureData(3);
        TirePressureManage.uploadEmptyTirePressureData(4);
    }

    public void switchDSDS(View view) {
        SystemPropertiesProxy.getInstance().set(this, "persist.radio.multisim.config", "dsds");
    }

    public void switchSSSS(View view) {
        SystemPropertiesProxy.getInstance().set(this, "persist.radio.multisim.config", "ssss");
    }

    public void obdfwVersion(View view) {
        try {
            if (obdCheckVersionSubscription != null) {
                obdCheckVersionSubscription.unsubscribe();
            }
            obdCheckVersionSubscription = ObdUpdateFlow.getObdVersion()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(obdVersion -> {
                        ToastUtils.showTip("读取obd版本号：" + obdVersion);
                        obdCheckVersionSubscription.unsubscribe();
                    }, Throwable::printStackTrace);
            ObdUpdateFlow.checkVersion();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void obdReset(View view) {
        OBDStream.getInstance().obdStreamClose();
        SerialManager.getInstance().closeSerialPortOBD();
        ObdUpdateCmd.resetObdChip();
        Observable
                .timer(6, TimeUnit.SECONDS, Schedulers.io())
                .subscribe(l -> {
                    GpioControl.wakeObd();
                    OBDStream.getInstance().init();
                    ObdManage.getInstance().init();
                    try {
                        OBDStream.getInstance().exec("ATRON");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, throwable -> Log.e("DebugActivity", "obdReset:", throwable));
    }

    public void fdOpen(View view) {
        CarLock.lockCar();
    }

    public void fdClose(View view) {
        CarLock.unlockCar();
    }

    public void onReboot(View view) {
        Log.i("dudu_aios", "click onReboot");
        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().set(this, "persist.sys.boot", "reboot");
    }

    public void onCrash(View view) {
        int a = 10;
        int b = a / 0;
    }

    public void onFireOn(View view) {
        CarStatusUtils.isDemo = true;
        CarStatusUtils.isDemoFired = true;

        Intent intent = new Intent("android.intent.action.ACC_ON");
        intent.putExtra("fired", true);
        sendBroadcast(intent);
//        finish();
    }

    public void onFireOff(View view) {
        CarStatusUtils.isDemo = true;
        CarStatusUtils.isDemoFired = false;

        Intent intent = new Intent("android.intent.action.ACC_ON");
        intent.putExtra("fired", false);
        sendBroadcast(intent);
//        finish();
    }

    public void closeFireMoni(View view) {
        CarStatusUtils.isDemo = false;
        CarStatusUtils.isDemoFired = false;
    }

    public void onQueryADCVol(View view) {

        if (obdQureyAdcVolSubscription != null) {
            obdQureyAdcVolSubscription.unsubscribe();
        }

        obdQureyAdcVolSubscription = OBDStream.getInstance().accVoltageStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                            ToastUtils.showTip("ACC电压:" + s);
                        }
                        , Throwable::printStackTrace);
        try {
            OBDStream.getInstance().exec("ATGETVOL");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onQueryStartVol(View view) {
        if (obdQureyStartVolSubscription != null) {
            obdQureyStartVolSubscription.unsubscribe();
        }
        obdQureyStartVolSubscription = OBDStream.getInstance().startVolStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                            obdQureyStartVolSubscription.unsubscribe();
                            ToastUtils.showTip("点火电压:" + s);
                        }
                        , Throwable::printStackTrace);
        try {
            OBDStream.getInstance().exec("ATGETSTARTVOL");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onQueryOBDCarModel(View view) {
        OBDStream.getInstance().OBDGetCarType()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                            ToastUtils.showTip("OBD已设置车型:" + obdCarMap.get(Integer.parseInt(s)));
                        }
                        , Throwable::printStackTrace);
        try {
            OBDStream.getInstance().exec("ATGETVEHICLE");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onQueryActive(View view) {
        RequestFactory
                .getActiveRequest()
                .checkDeviceActive(new com.dudu.rest.model.active.CheckDeviceActive())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(requestResponse -> {
                    if (requestResponse != null) {
                        if (requestResponse.resultCode == 40019) {
                            ToastUtils.showToast("设备已经激活了");
                        } else {
                            ToastUtils.showTip(requestResponse.resultMsg);
                        }
                    }
                }, throwable -> {
                    ToastUtils.showTip("检查出错，请查看网络");
                });
    }

    public void onQueryCarType(View view) {
        RequestFactory.getDrivingRequest()
                .getCarBrand()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getCarBrandResponse -> {
                    if (getCarBrandResponse != null) {
                        switch (getCarBrandResponse.result.audit_state) {
                            case GetCarBrandResponse.AUDIT_STATE_UNAUDITED:
                                ToastUtils.showToast(R.string.fault_code_clear_fail_upload_license);
                                break;
                            case GetCarBrandResponse.AUDIT_STATE_AUDITING:
                                ToastUtils.showToast(R.string.fault_code_clear_fail_wait_checking);
                                break;
                            case GetCarBrandResponse.AUDIT_STATE_AUDITED:
                                ToastUtils.showToast(getCarBrandResponse.result.brand);
                                DataFlowFactory
                                        .getUserMessageFlow()
                                        .saveCarType(getCarBrandResponse.result);
                                break;
                            case GetCarBrandResponse.AUDIT_STATE_REJECT:
                                ToastUtils.showToast(R.string.fault_code_clear_fail_reject);
                                break;
                        }
                    }
                }, throwable -> {
                    ToastUtils.showToast("检查出错，请查看网络");
                });
    }

    public void onQueryBLValue(View view) {
        ToastUtils.showToast("背光值:" + Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0));
    }

    public void onQueryLightValue(View view) {
        SensorManage.getInstance(this).initLightSensorManage();
        ToastUtils.showToast("光感强度:" + SensorManage.getInstance(this).getLux());
    }

    public void onBluetoothPhonePullBook(View view) {
        //广播给蓝牙服务获取通讯录
        Intent intent = new Intent(Constants.BLUETOOTH_PULL_PHONE_BOOK_BEGIN);
        sendBroadcast(intent);
    }

    public void testBackCarPreview(View view) {
        test(0, true);
        test(30, false);
    }

    private void test(int timeDelaySeconds, boolean backFlag) {
        Observable
                .timer(timeDelaySeconds, TimeUnit.SECONDS, Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(l -> {
                    sendBackCarBroadcast(CommonLib.getInstance().getContext(), backFlag);
                }, throwable -> Log.e("DebugActivity", "test:", throwable));
    }

    private void sendBackCarBroadcast(Context context, boolean backFlag) {
        Intent intent = new Intent("android.intent.action.ACC_BL");
        intent.putExtra("backed", backFlag);
        context.sendBroadcast(intent);
    }

    public void obdRTClose(View view) {
        try {
            OBDStream.getInstance().exec("ATROFF");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void obdRTOpen(View view) {
        try {
            OBDStream.getInstance().exec("ATRON");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveVoice(View view) {
        LauncherApplication.getContext().setNeedSaveVoice(true);
        VoiceManagerProxy.getInstance().onDestroy();
        VoiceManagerProxy.getInstance().onInit();
    }

    public void queryObdVersionInfo(View view) {
        ObdUpdateService.getInstance().delayQueryServerVersion(0);
    }

    public void obdDownload(View view) {
        if (ObdUpdateService.getInstance().isUpdateIng()) {
            ToastUtils.showToast("obdBin正在升级，请等待升级完成");
        }
        ObdUpdateService.getInstance().updateObdBin();
    }

    public void launcherUpgrade(View view) {
        LauncherUpgrade.queryVersionInfo();
    }

    public void hardUpdateObdBin(View view) {
        ObdUpdateService.getInstance().hardUpdateObdbin();
    }

    public void sendPushMessage(View view) {
        ReceiverPushData receiverPushData = new ReceiverPushData();
        receiverPushData.resultCode = 0;
        ReceiverPushData.ReceivedDataResult receivedDataResult = new ReceiverPushData.ReceivedDataResult();
        receivedDataResult.method = PushParams.LAUNCHER_UPGRADE;
        receiverPushData.result = receivedDataResult;

        EventBus.getDefault().post(receiverPushData);
    }

    public void onSetD02(View view) {
        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().set(this, "persist.sys.model", "d02");
        onCrash(null);
    }

    public void onSetD03(View view) {
        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().set(this, "persist.sys.model", "d03");
        onCrash(null);
    }

    public void setGaodeminiMap(View view) {
        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().set(this, "persist.sys.amap.ver", "amap");
    }

    public void initMicView(TextView view) {
        if (isMultiMic()) {
            view.setText("关闭Mic复用(重启生效)");
        } else {
            view.setText("启用Mic复用(重启生效)");
        }
    }

    public void initLogcatEnable(TextView view) {
        if (isLogcatEnable()) {
            view.setText("关闭Logcat");
        } else {
            view.setText("开启Logcat");
        }
    }


    public void clearLog(View view) {
        new Thread(() -> {
            FrontCameraManage.getInstance().stopRecord();
            RearCameraManage.getInstance().release();
            UsbControl.setToClient();

            ShellExe.execShellCmd("root dudu");
            ShellExe.execShellCmd("rm -rf /sdcard/logback");
            ShellExe.execShellCmd("rm -rf /sdcard/dudu/log*");
            ShellExe.execShellCmd("rm -rf /sdcard/dudu/crash/*");
            Observable
                    .timer(4, TimeUnit.SECONDS, Schedulers.newThread())
                    .subscribe(l -> {
                        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().set(this, "persist.sys.boot", "reboot");
                    }, throwable -> {
                    });
        }).start();
    }

    private long getUsableSpace(String path) {
        File tfCard = new File(path);
        return tfCard.getUsableSpace();
    }

    // added by huxuemin 2016-07-05 for amapauto copy begin
    private boolean fileIsExists(String path) {
        if (null == path) {
            return false;
        }
        try {
            File f = new File(path);
            if (!f.exists()) {
                Log.i("hxm_debug", path + " not exists!");
                return false;
            }

        } catch (Exception e) {
            // TODO: handle exception
            Log.i("hxm_debug", path + " not exists exception");
            return false;
        }
        Log.i("hxm_debug", path + " exists");
        return true;
    }

    public void startAmapAuto(View view) {
        PackageManager packageManager = getPackageManager();
        startActivity(new Intent(packageManager.getLaunchIntentForPackage("com.autonavi.amapauto")));
        EventBus.getDefault().post(NaviEvent.FloatButtonEvent.SHOW);
    }

    public void startAmapAutoCopy(View view) {
        if (fileIsExists(AmapAutoSrcPath)) {
            btnAmapautoCopy.setText("正在拷贝...");
            btnAmapautoCopy.setTextColor(Color.GRAY);
            btnAmapautoCopy.setClickable(false);
            btnBack.setTextColor(Color.GRAY);
            btnBack.setClickable(false);
            new Thread(() -> {
                Log.i("hxm_debug", "startAmapAutoCopy");
                ShellExe.execShellCmd("root dudu");
                ShellExe.execShellCmd("adb shell");
                ShellExe.execShellCmd("cp -r storage/sdcard1/amapauto storage/sdcard0");
                btnAmapautoCopy.post(new Runnable() {
                    @Override
                    public void run() {
                        if (fileIsExists(AmapAutoDestPath)) {
                            btnAmapautoCopy.setText("离线导航拷贝完成");
                        } else {
                            btnAmapautoCopy.setText("离线导航拷贝");
                            btnAmapautoCopy.setTextColor(Color.WHITE);
                            btnAmapautoCopy.setClickable(true);
                        }
                        btnBack.setTextColor(Color.WHITE);
                        btnBack.setClickable(true);
                    }
                });
            }).start();
        }
    }
    // added by huxuemin 2016-07-05 for amapauto copy end


    public void initGPSNmea(TextView view) {
        if (isGPSNmeaStart()) {
            view.setText("GPS NMEA停止保存");
        } else {
            view.setText("GPS NMEA开始保存");
        }
    }

    public void initStreamView(TextView view) {
        if (CameraControl.isStream()) {
            view.setText("关闭推流");
        } else {
            view.setText("开启推流");
        }
    }

    public void initRecordView(TextView view) {
        if (FrontCameraManage.getInstance().isRecording()/* || FrontCameraInstance.getInstance().isRecording()*/) {
            view.setText("关闭前置录像");
        } else {
            view.setText("开启前置录像");
        }
    }

    public void setMicMulti(View view) {
        if (isMultiMic()) {
            com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().set(this, "persist.sys.mic.multi", "0");
            ((TextView) view).setText("开启Mic复用(重启生效)");
        } else {
            com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().set(this, "persist.sys.mic.multi", "1");
            ((TextView) view).setText("关闭Mic复用(重启生效)");
        }
    }

    public void setLibStream(View view) {
//        if (CameraControl.isStream()) {
//            CameraControl.instance().setStreamState(false);
//            ((TextView) view).setText("开启推流");
//        } else {
//            CameraControl.instance().setStreamState(true);
//            ((TextView) view).setText("关闭推流");
//        }
        StreamActivity.launch(this);
    }

    public void setStartRecorder(View view) {
        if (FrontCameraManage.getInstance().isRecording() && FrontCameraInstance.getInstance().isRecording()) {
            FrontCameraManage.getInstance().stopRecord();
//            FrontCameraInstance.getInstance().releaseRecord(false);
            ((TextView) view).setText("开启前置录像");
        } else {
//            FrontCameraInstance.getInstance().startRecord();
            FrontCameraManage.getInstance().startRecord();
            ((TextView) view).setText("关闭前置录像");
        }
    }

    public void setGaodeAuto(View view) {
        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().set(this, "persist.sys.amap.ver", "auto");
    }

    public void obdSleep(View view) {
        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
                setCmd(SystemPropertiesProxy.SYS_OBD_SLEEP, SystemPropertiesProxy.high);
//        try {
//            OBDStream.getInstance().exec("ATENTERSLEEP");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void openWebSocket(View view) {
        ReceiverDataFlow.getInstance().init();
    }

    public void closeWebSocket(View view) {
        ReceiverDataFlow.getInstance().release();
    }

    public void initNetwork(View view) {
        NetworkManage.getInstance().init();
    }

    public void releaseNetwork(View view) {
        NetworkManage.getInstance().release();
    }

    public void obdWake(View view) {
        GpioControl.wakeObd();
    }

    public void obdPowerOn(View view) {
        GpioControl.powerOnObd();
    }

    public void obdPowerOff(View view) {
        GpioControl.powerOffObd();
    }


    public void testStorageSpace(View view) {
        StorageSpaceService.getInstance().testStorageSpaceService();
    }

    public void start_test_shell_exe(View view) {
        ShellExeTest.startTest();
    }

    public void start_test_shell_exe1(View view) {
        ShellExeTest.startTest1();
    }

    public void stop_test_shell_exe(View view) {
        ShellExeTest.stopTest();
    }

    public void onKillZombieProcess(View view) {
        ProcessUtils.killZombieProcess(CommonLib.getInstance().getContext(), "com.dudu.android.launcher");
    }

    public void destoryVoice(View view) {
        LoggerFactory.getILoggerFactory().getLogger("voice").debug("语音销毁/初始化压测");
        if (testDestoryVoiceSub != null) {
            testDestoryVoiceSub.unsubscribe();
            testDestoryVoiceSub = null;
            if (testInitVoiceSub != null) {
                testInitVoiceSub.unsubscribe();
                testInitVoiceSub = null;
            }
            testInitVoiceSub = Observable.timer(5, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aLong -> {
                        ((TextView) view).setText("销毁语音");
                    }, throwable -> {
                    });
            VoiceManagerProxy.getInstance().onInit();

        } else {
            VoiceManagerProxy.getInstance().onDestroy();
            testDestoryVoiceSub = Observable.timer(1, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aLong -> {
                        ((TextView) view).setText("初始化语音");
                    }, throwable -> {
                    });
            if (testInitVoiceSub != null) {
                testInitVoiceSub.unsubscribe();
                testInitVoiceSub = null;
            }
        }
    }


    public void onKillLauncher(View view) {
        ProcessUtils.killLauncherProcess();
    }

    public void onStartLocationServer(View view) {
        GPSCtl.startFixService();
    }

    public void onStoptLocationServer(View view) {
        GPSCtl.stopFixService();
    }

    public void tpmsPowerOn(View view) {
        GpioControl.powerOnTPMS();
    }

    public void tpmsPowerOff(View view) {
        GpioControl.powerOffTPMS();
    }
}
