package com.dudu.aios.ui.activity;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import com.dudu.aios.ui.base.BaseFragmentManagerActivity;
import com.dudu.aios.ui.base.ObservableFactory;
import com.dudu.aios.ui.fragment.AccelerationTestFragment;
import com.dudu.aios.ui.fragment.BtCallingFragment;
import com.dudu.aios.ui.fragment.BtContactsFragment;
import com.dudu.aios.ui.fragment.BtDialFragment;
import com.dudu.aios.ui.fragment.BtDialSelectNumberFragment;
import com.dudu.aios.ui.fragment.BtInCallFragment;
import com.dudu.aios.ui.fragment.BtOutCallFragment;
import com.dudu.aios.ui.fragment.CarCheckingFragment;
import com.dudu.aios.ui.fragment.Coolant.WaterWarningFragment;
import com.dudu.aios.ui.fragment.DeviceBindFragment;
import com.dudu.aios.ui.fragment.FlowFragment;
import com.dudu.aios.ui.fragment.LicenseUploadFragment;
import com.dudu.aios.ui.fragment.MainFragment;
import com.dudu.aios.ui.fragment.PhotoFragment;
import com.dudu.aios.ui.fragment.PhotoListFragment2;
import com.dudu.aios.ui.fragment.PhotoShowFragment;
import com.dudu.aios.ui.fragment.RepairFaultCodeFragment;
import com.dudu.aios.ui.fragment.RequestNetworkFragment;
import com.dudu.aios.ui.fragment.SafetyMainFragment;
import com.dudu.aios.ui.fragment.VehicleAnimationFragment;
import com.dudu.aios.ui.fragment.VideoFragment;
import com.dudu.aios.ui.fragment.VideoListFragment;
import com.dudu.aios.ui.fragment.VideoPlayFragment;
import com.dudu.aios.ui.fragment.VoipCallingFragment;
import com.dudu.aios.ui.fragment.base.BaseManagerFragment;
import com.dudu.aios.ui.fragment.tire.TireExceptionHelper;
import com.dudu.aios.ui.fragment.tire.TireFragment;
import com.dudu.aios.ui.fragment.video.DrivingRecordFragment;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.aios.ui.voice.VoiceEvent;
import com.dudu.aios.ui.voice.VoiceFragment;
import com.dudu.android.hideapi.SystemPropertiesProxy;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.broadcast.ACCReceiver;
import com.dudu.android.launcher.broadcast.ScreenReceiver;
import com.dudu.android.launcher.broadcast.TFlashCardReceiver;
import com.dudu.android.launcher.utils.AdminReceiver;
import com.dudu.android.launcher.utils.DialogUtils;
import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.event.DownEvent;
import com.dudu.commonlib.event.Events;
import com.dudu.commonlib.utils.time.DateTimeUtils;
import com.dudu.drivevideo.frontcamera.FrontCameraManage;
import com.dudu.drivevideo.frontcamera.event.StreamEvent;
import com.dudu.drivevideo.rearcamera.RearCameraManage;
import com.dudu.drivevideo.spaceguard.event.VideoSpaceEvent;
import com.dudu.event.DeviceEvent;
import com.dudu.event.WaterWarningDisplayEvent;
import com.dudu.init.CarFireManager;
import com.dudu.monitor.Monitor;
import com.dudu.monitor.event.TireExceptionEvent;
import com.dudu.monitor.event.WaterWarningEvent;
import com.dudu.monitor.obd.CoolantTemperatureManager;
import com.dudu.monitor.obdUpdate.ObdUpdateService;
import com.dudu.monitor.tirepressure.TirePressureManager;
import com.dudu.monitor.valueobject.LocationInfo;
import com.dudu.navi.event.NaviEvent;
import com.dudu.rest.model.common.RequestResponse;
import com.dudu.voice.FloatWindowUtils;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.voip.VoipSDKCoreHelper;
import com.dudu.workflow.HandlerPushData;
import com.dudu.workflow.common.DataFlowFactory;
import com.dudu.workflow.common.RequestFactory;
import com.dudu.workflow.obd.CarLock;
import com.dudu.workflow.obd.ObdFlow;
import com.dudu.workflow.push.model.PushParams;
import com.dudu.workflow.push.model.ReceiverPushData;
import com.dudu.workflow.robbery.RobberyFlow;
import com.dudu.workflow.robbery.RobberyStateModel;
import com.umeng.analytics.MobclickAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


public class MainRecordActivity extends BaseFragmentManagerActivity {
    private static final int MY_REQUEST_CODE = 9999;

    /**
     * 该页面显示透明还是黑色背景<br/>
     * true:透明<br/>
     * false:黑色
     */
    protected boolean titleColorIsTransparent = true;
    protected boolean showAppDownloadIcon = false;
    private TFlashCardReceiver mTFlashCardReceiver;
    private ScreenReceiver mScreenReceiver;
    private Logger log_init;
    private Logger log_web;
    private DevicePolicyManager mPolicyManager;
    private ComponentName componentName;
    private Logger log = LoggerFactory.getLogger("ui.MainRecordActivity");

    /**
     * 异常最后一次报警的时间
     */
    private long lastExceptionShowTime = 0;
    /**
     * 异常判断助手,判断异常是否改变
     */
    private TireExceptionHelper mTireExceptionHelper;

    @Override
    public int fragmentViewId() {
        return R.id.container;
    }

    public Fragment getFragment(String key) {
        Fragment fragment = null;
        if (fragmentMap != null) {
            List<BaseManagerFragment> fragmentList = fragmentMap.get(key);
            if (fragmentList != null && fragmentList.size() > 0) {
                fragment = fragmentList.get(0);
            }
        }
        return fragment;
    }

    @Override
    public Map<String, Class<? extends BaseManagerFragment>> baseFragmentWithTag() {
        Map<String, Class<? extends BaseManagerFragment>> fragmentMap = new HashMap<>();
        fragmentMap.put(FragmentConstants.FRAGMENT_MAIN_PAGE, MainFragment.class);
        fragmentMap.put(FragmentConstants.FRAGMENT_SAFETY_CENTER, SafetyMainFragment.class);//防劫防盗
        fragmentMap.put(FragmentConstants.FRAGMENT_DRIVING_RECORD, DrivingRecordFragment.class);//行车记录
        fragmentMap.put(FragmentConstants.FRAGMENT_PHOTO, PhotoFragment.class);
        fragmentMap.put(FragmentConstants.FRAGMENT_PHOTO_LIST, PhotoListFragment2.class);//图片列表
        fragmentMap.put(FragmentConstants.FRAGMENT_VIDEO, VideoFragment.class);
        fragmentMap.put(FragmentConstants.FRAGMENT_FLOW, FlowFragment.class);//移动热点
        fragmentMap.put(FragmentConstants.VOICE_FRAGMENT, VoiceFragment.class);
        fragmentMap.put(FragmentConstants.FRAGMENT_VIDEO_LIST, VideoListFragment.class);
        fragmentMap.put(FragmentConstants.FRAGMENT_DEVICE_BINDING, DeviceBindFragment.class);//展示绑定设备的二维码
        fragmentMap.put(FragmentConstants.CAR_CHECKING, CarCheckingFragment.class);//行车自检
        fragmentMap.put(FragmentConstants.BT_DIAL, BtDialFragment.class);//蓝牙电话
        fragmentMap.put(FragmentConstants.BT_CONTACTS, BtContactsFragment.class);//联系人界面
        fragmentMap.put(FragmentConstants.BT_IN_CALL, BtInCallFragment.class);//来电界面
        fragmentMap.put(FragmentConstants.BT_OUT_CALL, BtOutCallFragment.class);//去电界面
        fragmentMap.put(FragmentConstants.BT_CALLING, BtCallingFragment.class);//通话中
        fragmentMap.put(FragmentConstants.BT_DIAL_SELECT_NUMBER, BtDialSelectNumberFragment.class);//多个电话号码选择
        fragmentMap.put(FragmentConstants.VEHICLE_ANIMATION_FRAGMENT, VehicleAnimationFragment.class);//故障清除
        fragmentMap.put(FragmentConstants.REPAIR_FAULT_CODE_FRAGMENT, RepairFaultCodeFragment.class);//汽车修理
        fragmentMap.put(FragmentConstants.PHOTO_SHOW_FRAGMENT, PhotoShowFragment.class);//图片显示
        fragmentMap.put(FragmentConstants.VIDEO_PLAY_FRAGMENT, VideoPlayFragment.class);//视频播放
        fragmentMap.put(FragmentConstants.ACCELERATION_TEST_FRAGMENT, AccelerationTestFragment.class);//加速测试
        fragmentMap.put(FragmentConstants.LICENSE_UPLOAD_UPLOAD_FRAGMENT, LicenseUploadFragment.class);//证件上传
        fragmentMap.put(FragmentConstants.REQUEST_NETWORK_FRAGMENT, RequestNetworkFragment.class);//请求网络等待的界面
        fragmentMap.put(FragmentConstants.VOIP_CALLING_FRAGMENT, VoipCallingFragment.class);//VOIP呼叫 VIP Service
        fragmentMap.put(FragmentConstants.TIRE_FRAGMENT, TireFragment.class);//轮胎信息界面
        fragmentMap.put(FragmentConstants.WATER_WARNING_FRAGMENT, WaterWarningFragment.class);//水温告警界面
        return fragmentMap;
    }

    @Override
    public void showDefaultFragment() {
        replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LauncherApplication.getContext().setInstance(this);
        super.onCreate(null);

//        MobclickAgent.setDebugMode(true);
        MobclickAgent.openActivityDurationTrack(false);

        log.debug("onCreate()");
        EventBus.getDefault().unregister(this);
        EventBus.getDefault().register(this);

        initData();

        FrontCameraManage.getInstance().setBlurGLSurfaceView(baseBinding.frontCameraPreview);
//        FrontCameraInstance.getInstance().setPreviewView(baseBinding.frontCameraPreview);
        RearCameraManage.getInstance().setRearPreviewViewGroup(baseBinding.rearPreviewContainer);
        replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);


        if (ACCReceiver.isBackCarIng == false) {
            replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
        } else {
            ACCReceiver.log.debug("正在倒车，打开行车记录界面");
          /*  Bundle bundle = new Bundle();
            bundle.putBoolean(FragmentConstants.IS_BACK_CARING, true);
            FragmentConstants.TEMP_ARGS = bundle;*/
            replaceFragment(FragmentConstants.FRAGMENT_DRIVING_RECORD);
        }

        mTireExceptionHelper = new TireExceptionHelper();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    public void showMain() {
        if (!FloatWindowUtils.getInstance().isShowWindow()) {
            log_init.debug("MainRecordActivity -> showMainFragment");
            EventBus.getDefault().post(new NaviEvent.ToMainFragmentEvent());
            replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
        }
    }

    @Override
    protected View getChildView() {
        return LayoutInflater.from(this).inflate(R.layout.activity_record, null);
    }

    @Override
    public void showFragment(String tagKey, boolean append) {
        super.showFragment(tagKey, append);
    }

    private void initData() {
        log_init = LoggerFactory.getLogger("init.start");
        log_web = LoggerFactory.getLogger("workFlow.webSocket");

        log_init.debug("MainActivity 调用onCreate方法初始化...");

        registerTFlashCardReceiver();

        registerScreenReceiver();

        // 获取设备管理服务
        mPolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // 自己的AdminReceiver 继承自 DeviceAdminReceiver
        componentName = new ComponentName(this, AdminReceiver.class);

    }

    public void replaceFragment(String name, Bundle arg) {
        fragmentArg = arg;
        replaceFragment(name);
    }

    public void showTitleColorTransparent() {
        if (titleColorIsTransparent) {
            if (baseBinding.commonTitleLayout.getBackground() instanceof ColorDrawable) {
                if (((ColorDrawable) baseBinding.commonTitleLayout.getBackground()).getColor() == getResources().getColor(R.color.transparent1)) {
                    return;
                }
            }
            baseBinding.commonTitleLayout.setBackgroundResource(R.color.transparent1);
        } else {
            if (baseBinding.commonTitleLayout.getBackground() instanceof ColorDrawable) {
                if (((ColorDrawable) baseBinding.commonTitleLayout.getBackground()).getColor() == getResources().getColor(R.color.black)) {
                    return;
                }
            }
            baseBinding.commonTitleLayout.setBackgroundResource(R.color.black);
        }
    }

    public void showAppDownloadIcon() {
        if (showAppDownloadIcon) {
            ObservableFactory.getInstance().getTitleObservable().showAppDownload.set(true);
        } else {
            ObservableFactory.getInstance().getTitleObservable().showAppDownload.set(false);
        }
    }

    public void showTitle(boolean isShow) {
        ObservableFactory.getInstance().getCommonObservable().hasTitle.set(isShow);
        ObservableFactory.getInstance().getCommonObservable().hasBackground.set(false);
    }

    @DebugLog
    public void replaceFragment(String name) {
        switch (name) {

            //视频播放
            case FragmentConstants.VIDEO_PLAY_FRAGMENT:
                switchToStackByTag(FragmentConstants.VIDEO_PLAY_FRAGMENT);
                break;

            //图片显示
            case FragmentConstants.PHOTO_SHOW_FRAGMENT:
                switchToStackByTag(FragmentConstants.PHOTO_SHOW_FRAGMENT);
                break;

            //汽车修理
            case FragmentConstants.REPAIR_FAULT_CODE_FRAGMENT:
                switchToStackByTag(FragmentConstants.REPAIR_FAULT_CODE_FRAGMENT);
                break;

            //故障清除
            case FragmentConstants.VEHICLE_ANIMATION_FRAGMENT:
                switchToStackByTag(FragmentConstants.VEHICLE_ANIMATION_FRAGMENT);
                break;

            //蓝牙电话打进界面
            case FragmentConstants.BT_IN_CALL:
                switchToStackByTag(FragmentConstants.BT_IN_CALL);
                break;
            //蓝牙电话拨出界面
            case FragmentConstants.BT_OUT_CALL:
                switchToStackByTag(FragmentConstants.BT_OUT_CALL);
                break;

            //蓝牙电话联系人界面
            case FragmentConstants.BT_CONTACTS:
                switchToStackByTag(FragmentConstants.BT_CONTACTS);
                break;

            //蓝牙电话拨号界面
            case FragmentConstants.BT_DIAL:
                switchToStackByTag(FragmentConstants.BT_DIAL);
                break;
            //蓝牙电话通话中界面
            case FragmentConstants.BT_CALLING:
                switchToStackByTag(FragmentConstants.BT_CALLING);
                break;
            //多个号码选择界面
            case FragmentConstants.BT_DIAL_SELECT_NUMBER:
                switchToStackByTag(FragmentConstants.BT_DIAL_SELECT_NUMBER);
                break;

            //行车自检
            case FragmentConstants.CAR_CHECKING:
                switchToStackByTag(FragmentConstants.CAR_CHECKING);
                break;

            case FragmentConstants.FRAGMENT_MAIN_PAGE:
                switchToStackByTag(FragmentConstants.FRAGMENT_MAIN_PAGE);
                break;

            case FragmentConstants.FRAGMENT_SAFETY_CENTER:
                switchToStackByTag(FragmentConstants.FRAGMENT_SAFETY_CENTER);
                break;

            //行车记录界面,取消模糊
            case FragmentConstants.FRAGMENT_DRIVING_RECORD:
                switchToStackByTag(FragmentConstants.FRAGMENT_DRIVING_RECORD);
                break;

            case FragmentConstants.FRAGMENT_VIDEO_LIST:
                switchToStackByTag(FragmentConstants.FRAGMENT_VIDEO_LIST);
                break;

            case FragmentConstants.FRAGMENT_VIDEO:
                switchToStackByTag(FragmentConstants.FRAGMENT_VIDEO);
                break;

            case FragmentConstants.FRAGMENT_PHOTO_LIST:
                switchToStackByTag(FragmentConstants.FRAGMENT_PHOTO_LIST);
                break;

            case FragmentConstants.FRAGMENT_PHOTO:
                switchToStackByTag(FragmentConstants.FRAGMENT_PHOTO);
                break;

            case FragmentConstants.FRAGMENT_FLOW:
                switchToStackByTag(FragmentConstants.FRAGMENT_FLOW);
                break;
            case FragmentConstants.VOICE_FRAGMENT:
                switchToStackByTag(FragmentConstants.VOICE_FRAGMENT);
                break;
            case FragmentConstants.FRAGMENT_DEVICE_BINDING:
                switchToStackByTag(FragmentConstants.FRAGMENT_DEVICE_BINDING);
                break;
            case FragmentConstants.ACCELERATION_TEST_FRAGMENT:
                switchToStackByTag(FragmentConstants.ACCELERATION_TEST_FRAGMENT);
                break;
            case FragmentConstants.LICENSE_UPLOAD_UPLOAD_FRAGMENT:
                switchToStackByTag(FragmentConstants.LICENSE_UPLOAD_UPLOAD_FRAGMENT);
                break;
            case FragmentConstants.VOIP_CALLING_FRAGMENT:
                switchToStackByTag(FragmentConstants.VOIP_CALLING_FRAGMENT);
                break;
            case FragmentConstants.REQUEST_NETWORK_FRAGMENT:
                switchToStackByTag(FragmentConstants.REQUEST_NETWORK_FRAGMENT);
                break;
            //轮胎信息
            case FragmentConstants.TIRE_FRAGMENT:
                switchToStackByTag(FragmentConstants.TIRE_FRAGMENT);
                break;
            //水温告警弹窗
            case FragmentConstants.WATER_WARNING_FRAGMENT:
                switchToStackByTag(FragmentConstants.WATER_WARNING_FRAGMENT);
                break;
            default:
                switchToStackByTag(FragmentConstants.FRAGMENT_MAIN_PAGE);
                break;
        }

        if (!(name.equals(FragmentConstants.VOICE_FRAGMENT)  ||
                name.equals(FragmentConstants.BT_OUT_CALL)||
                name.equals(FragmentConstants.BT_IN_CALL) ||
                name.equals(FragmentConstants.BT_CALLING) ||
                name.equals(FragmentConstants.VOIP_CALLING_FRAGMENT))) {
            lastThirdFragment = lastSecondFragment;
                    lastSecondFragment = lastFragment;
            lastFragment = name;
        }
    }

    @Override
    public void switchToStackByTag(String tag) {
        changeTitleColor(tag);
        RearCameraManage.log.debug("界面切换至-->{}", tag);
        super.switchToStackByTag(tag);
    }

    private void changeTitleColor(String tag) {
        titleColorIsTransparent = true;
        showAppDownloadIcon = false;
        switch (tag) {
            case FragmentConstants.CAR_CHECKING:
            case FragmentConstants.TIRE_FRAGMENT:
            case FragmentConstants.VEHICLE_ANIMATION_FRAGMENT:
            case FragmentConstants.REPAIR_FAULT_CODE_FRAGMENT:
                titleColorIsTransparent = false;
                break;
            case FragmentConstants.FRAGMENT_MAIN_PAGE:
                showAppDownloadIcon = true;
                break;
        }
    }

    private void registerTFlashCardReceiver() {
        mTFlashCardReceiver = new TFlashCardReceiver();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addDataScheme("file");
        registerReceiver(mTFlashCardReceiver, intentFilter);
    }

    private void registerScreenReceiver() {
        mScreenReceiver = new ScreenReceiver();

        IntentFilter filter = new IntentFilter();
        // 屏幕灭屏广播
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        // 屏幕亮屏广播
        filter.addAction(Intent.ACTION_SCREEN_ON);
        // 屏幕解锁广播
        filter.addAction(Intent.ACTION_USER_PRESENT);
        // 当长按电源键弹出“关机”对话或者锁屏时系统会发出这个广播
        // example：有时候会用到系统对话框，权限可能很高，会覆盖在锁屏界面或者“关机”对话框之上，
        // 所以监听这个广播，当收到时就隐藏自己的对话，如点击pad右下角部分弹出的对话框
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        registerReceiver(mScreenReceiver, filter);
    }


    public void onEventMainThread(DeviceEvent.Screen event) {
        log_init.debug("DeviceEvent.Screen {}", event.getState());
        if (event.getState() == DeviceEvent.OFF) {
            if (mPolicyManager.isAdminActive(componentName)) {
                mPolicyManager.lockNow();// 锁屏
            } else {
                activeManage(); //获取权限
            }
        } else {
            CarFireManager.getInstance().wakeScreen();
        }
    }

    private void activeManage() {
        // 启动设备管理(隐式Intent) - 在AndroidManifest.xml中设定相应过滤器
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);

        // 权限列表
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);

        // 描述(additional explanation) 在申请权限时出现的提示语句
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "激活后就能一键锁屏了");

        startActivityForResult(intent, MY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 获取权限成功，立即锁屏并finish自己，否则继续获取权限
        if (requestCode == MY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            mPolicyManager.lockNow();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        log.debug("MainRecordActivity onResume");

        observableFactory.getCommonObservable(baseBinding).hasTitle.set(true);
        observableFactory.getCommonObservable().hasBackground.set(false);

        LauncherApplication.startRecord = false;
        EventBus.getDefault().post(NaviEvent.FloatButtonEvent.HIDE);
        isSaveInstanceState = false;
        LoggerFactory.getLogger("video1.frontdrivevideo").debug("MainRecordActivity onResume");
        //FrontCameraManage.getInstance().startForegroundRecord();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }

    public void onDialButtonClick(View view) {
        if (TextUtils.equals(FragmentConstants.BT_DIAL, currentStackTag)) {
            List<BaseManagerFragment> fragmentList = fragmentMap.get(FragmentConstants.BT_DIAL);
            ((BtDialFragment) fragmentList.get(fragmentList.size() - 1)).onDialButtonClick(view);
        } else if (TextUtils.equals(FragmentConstants.BT_CALLING, currentStackTag)) {
            List<BaseManagerFragment> fragmentList = fragmentMap.get(FragmentConstants.BT_CALLING);
            ((BtCallingFragment) fragmentList.get(fragmentList.size() - 1)).onDialButtonClick(view);
        } else if (TextUtils.equals(FragmentConstants.VOIP_CALLING_FRAGMENT, currentStackTag)) {
            List<BaseManagerFragment> fragmentList = fragmentMap.get(FragmentConstants.VOIP_CALLING_FRAGMENT);
            ((VoipCallingFragment) fragmentList.get(fragmentList.size() - 1)).onDialButtonClick(view);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        log.debug("MainRecordActivity onPause");
        MobclickAgent.onPause(this);
        LoggerFactory.getLogger("video1.frontdrivevideo").debug("MainRecordActivity onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        CameraInstance.getInstance().stopCamera();

        log.debug("MainRecordActivity 调用onDestroy释放资源...");
        unregisterReceiver(mTFlashCardReceiver);
        unregisterReceiver(mScreenReceiver);

        //释放Voip资源
        VoipSDKCoreHelper.getInstance().release();

        EventBus.getDefault().unregister(this);
        LauncherApplication.getContext().setInstance(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LoggerFactory.getLogger("ui.MainRecordActivity").debug("onSaveInstanceState method called!");
    }

    public void onEventBackgroundThread(ReceiverPushData data) {
        log_web.debug("收到推送数据");

        HandlerPushData.getInstance(this).handlerData(data);
        setCarTypeIfGetted(data);

        if (data != null && data.resultCode == 0 && data.result != null) {
            if (data.result.method != null && PushParams.LAUNCHER_UPGRADE.equals(data.result.method)) {
                LoggerFactory.getLogger("car.obdUpdate").info("收到推送升级消息----");
                ObdUpdateService.getInstance().delayQueryServerVersion(0);
            }
        }
    }

    public void onEventBackgroundThread(VideoSpaceEvent videoSpaceEvent) {
        VoiceManagerProxy.getInstance().startSpeaking(videoSpaceEvent.getMesageToSpeak(), TTSType.TTS_DO_NOTHING, false);
    }

    public void setCarTypeIfGetted(ReceiverPushData data) {
        Observable.just(data)
                .filter(data1 -> data1 != null)
                .filter(data2 -> data2.resultCode == 0)
                .filter(data3 -> data3.result != null)
                .map(data4 -> data4.result)
                .filter(result -> result.method != null)
                .map(result1 -> result1.method)
                .filter(method -> method == PushParams.THEFT_APPROVAL)
                .subscribe(method1 -> ObdFlow.setCarTypeAndGetResult(data.result.obd_car_no)
                        , throwable -> log_init.error("setCarTypeIfGetted", throwable));


    }


    public void onEventMainThread(Events.DeviceEvent event) {
        if (event.getEvent() == Events.REBOOT) {
            VoiceManagerProxy.getInstance().startSpeaking(
                    getString(R.string.obd_update_success_reboot), TTSType.TTS_DO_NOTHING, false);
            Observable.timer(10, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aLong -> SystemPropertiesProxy.getInstance().set(this, "persist.sys.boot", "reboot"), throwable -> log_init.error("onEventMainThread", throwable));
        }
    }

    public void onEventMainThread(Events.TestSpeedEvent data) {
        int testSpeedStatus = data.getEvent();
        switch (testSpeedStatus) {
            case Events.TEST_SPEED_START:
                replaceFragment(FragmentConstants.ACCELERATION_TEST_FRAGMENT);
                break;
            case Events.TEST_SPEED_ZERO:
                log_init.debug("请停止车速后再开始测速。。");
                VoiceManagerProxy.getInstance().startSpeaking(
                        CommonLib.getInstance().getContext().getString(R.string.test_speed_after_stop), TTSType.TTS_DO_NOTHING, false);
                break;
        }
    }

    public void onEventMainThread(Events.RobberyEvent data) {
        try {
            LoggerFactory.getLogger("workFlow.Robbery").info("收到RobberyEvent，开启防劫（踩油门检测）");
            com.dudu.workflow.common.ObservableFactory.robberyFlow().startAccelerometersMonitoring(data.getRevolutions(), data.getNumberOfOperations(), data.getCompleteTime());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onEventMainThread(VoiceEvent event) {
        switch (event) {
            case SHOW_ANIM:
                log_init.debug("voice onEvent show anim");
                replaceFragment(FragmentConstants.VOICE_FRAGMENT);
                break;
            case DISMISS_WINDOW:
                if (currentStackTag.equals(FragmentConstants.VOIP_CALLING_FRAGMENT)
                        ||currentStackTag.equals(FragmentConstants.BT_OUT_CALL)
                        ||currentStackTag.equals(FragmentConstants.BT_IN_CALL)
                        ||currentStackTag.equals(FragmentConstants.BT_CALLING)
                        )
                    return;
                replaceFragment(lastFragment);
                break;
        }

    }

    /**
     * 实时视频监控事件
     */
    public void onEventMainThread(StreamEvent event) {
        if (event.getState() == StreamEvent.START) {
            mLogoImageView.startAnimation(getRotateAnimation());
        } else {
            mLogoImageView.clearAnimation();
        }
    }

    /**
     * Launcher下载事件
     */
    public void onEventMainThread(DownEvent event) {
        mDuduDownView.setState(event.getState());
        mDuduDownView.setVisibility(View.GONE);
    }

    /**
     * 轮胎异常事件
     */
    public void onEventMainThread(TireExceptionEvent event) {
        final String tireChinese = TirePressureManager.getTireChinese(event.mInfo.getPosition());
        log.info("MainRecordActivity正在处理 {} 异常.", tireChinese);

        //几秒之内,只提醒一次
        final long nowTime = System.currentTimeMillis();
        if (event.isException && mTireExceptionHelper.setTPMSInfo(event.mInfo) && (nowTime - lastExceptionShowTime > 1 * 1000) /*&& !TireFragment.isExceptionShow()*/) {
            final String string = "轮胎异常,请检查!";
            log.info(string);
            final Fragment tireFragment = getFragment(FragmentConstants.TIRE_FRAGMENT);
            if (tireFragment != null && tireFragment.isVisible()) {
                //正在胎压界面
                log.info("正在胎压界面.");
            } else {
                VoiceManagerProxy.getInstance().startSpeaking(string, TTSType.TTS_DO_NOTHING, false);
                replaceFragment(FragmentConstants.TIRE_FRAGMENT);
            }

            lastExceptionShowTime = nowTime;
        } else {
            if (!event.isException) {
                mTireExceptionHelper.cleanTpmsException(event.mInfo.getPosition());
            }
            log.info("{}, 无异常需要警报.", tireChinese);
        }
    }

    /**
     * 水温告警事件
     */
    public void onEventMainThread(WaterWarningEvent event) {

        float currentTemperature = event.mInfo.getEngCoolant();

        if (!(CoolantTemperatureManager.TEMPERATURE_THRESHOLD > currentTemperature)) //水温超标
        {
            if (isWaterWarningShown()) {
                //已经处于告警弹窗状态，不再重复弹窗
//                log.info("WaterWarning 已经处于告警弹窗状态，不再重复弹窗.");
            } else {
                //当前未处于告警弹窗状态，需要进一步判断
                long lastTime = CoolantTemperatureManager.getInstance().getLastHighTime();
                final long currentTime = System.currentTimeMillis();
//                log.info("{}, WaterWarning currentTime .", currentTime);
//                log.info("{}, WaterWarning lastTime .", lastTime);

                if (CoolantTemperatureManager.MIN_TIME_INTERVAL < (currentTime - lastTime)) {
                    log.info("WaterWarning 警告,水温过高,请检查.", currentTemperature);

                    //距离上次告警已经超过5分钟，需要进行告警弹窗和语音提示
                    poupWaterWarning();

                } else {
                    //距离上次未超过5分钟，不再进行提示
//                    log.info(" WaterWarning 距离上次未超过5分钟，不再进行提示.");
                }
            }
        } else //水温未超标
        {

            if (isWaterWarningShown()) {
                //正在进行告警，并退出水温告警弹窗
                closeWaterWarning();


                //记录告警弹窗时间
                final long currentTime = System.currentTimeMillis();
                CoolantTemperatureManager.getInstance().setLastHighTime(currentTime);

                log.info("WaterWarning 水温未超标，正在进行告警，退出水温告警弹窗.");
            } else {
                //不做任何处理
//                log.info(",WaterWarning 水温未超标，且当前未进行告警，所以不做任何处理.");
            }
        }
    }

    private void poupWaterWarning() {
        //弹出水温告警窗口
        CoolantTemperatureManager.getInstance().setDisplayWarning(true);
        startActivity(new Intent(this, WaterWarningActivity.class));
    }

    private void closeWaterWarning() {
        //通知水温告警窗口退出
        EventBus.getDefault().post(new WaterWarningDisplayEvent(true));

    }

    private boolean isWaterWarningShown() {
//        log.info("WaterWarning 是否在前台 -> {}", CoolantTemperatureManager.getInstance().isForeground(this, "com.dudu.aios.ui.activity.WaterWarningActivity"));
        return CoolantTemperatureManager.getInstance().isDisplayWarning();

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        log.debug("keyEvent keyCode:" + event.getKeyCode());

        if (TextUtils.equals(FragmentConstants.BT_IN_CALL, currentStackTag)) {
//            List<BaseManagerFragment> fragmentList = fragmentMap.get(FragmentConstants.BT_IN_CALL);
//            ((BtInCallFragment) fragmentList.get(fragmentList.size() - 1)).dispatchKeyEvent(event);
        } else if (TextUtils.equals(FragmentConstants.BT_CALLING, currentStackTag)) {
//            List<BaseManagerFragment> fragmentList = fragmentMap.get(FragmentConstants.BT_CALLING);
//            ((BtCallingFragment) fragmentList.get(fragmentList.size() - 1)).dispatchKeyEvent(event);
        } else if (TextUtils.equals(FragmentConstants.BT_CONTACTS, currentStackTag)) {
            List<BaseManagerFragment> fragmentList = fragmentMap.get(FragmentConstants.BT_CONTACTS);
            ((BtContactsFragment) fragmentList.get(fragmentList.size() - 1)).dispatchKeyEvent(event);
        } else if (TextUtils.equals(FragmentConstants.BT_DIAL, currentStackTag)) {
            List<BaseManagerFragment> fragmentList = fragmentMap.get(FragmentConstants.BT_DIAL);
            ((BtDialFragment) fragmentList.get(fragmentList.size() - 1)).dispatchKeyEvent(event);

        }
        return super.dispatchKeyEvent(event);
    }

    public void onEventMainThread(RobberyStateModel event) {
        log_init.debug("收到防劫模式触发事件:" + event.getRobberyState());
        switch (event.getRobberyState()) {
            case RobberyStateModel.ROBBERY_IS_TRIGGERED:
            DataFlowFactory.getRobberyMessageFlow().obtainRobberyMessage()
                    .map(robberyMessage -> robberyMessage.isRobberySwitch())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(switchIsOn -> {
                        log_init.debug("查询本地是否打开了防3次踩油门:" + switchIsOn);
                        if (switchIsOn) {
                            DataFlowFactory.getRobberyMessageFlow().saveRobberyTriggerStatus(true);
                            checkCarlock(true);
                            requestRobberyTrigger();

                        }
                    }, (error) -> {
                        log_init.error("收到防劫模式触发事件:" + event.getRobberyState(), error);
                    });
                break;
            case RobberyStateModel.START_ROBBERY:
                RobberyFlow.getInstance().checkGunSwitch();
                break;

        }
    }

    public void checkCarlock(boolean lock) {
        if (lock) {
            CarLock.robberyLockCar();
        } else {
            CarLock.robbertUnlockCar();
        }
    }

    private void requestRobberyTrigger() {
        log_init.debug("请求网络防劫触发了、、");
        LocationInfo locationInfo = Monitor.getInstance().getCurLocation();
        double lon = -1;
        double lat = -1;
        if (locationInfo != null) {
            lon = locationInfo.getLon();
            lat = locationInfo.getLat();
        }
        String datetime = DateTimeUtils.obtainCurrentTime();
        log_init.debug("现在的经度：" + lon + "  纬度:" + lat + "  datetime:" + datetime);
        RequestFactory.getRobberyRequest().robberyTrigger(String.valueOf(lon), String.valueOf(lat), datetime).subscribe(new Action1<RequestResponse>() {
            @Override
            public void call(RequestResponse requestResponse) {
                log_init.debug("requestResponse:  code:" + requestResponse.resultCode + "  msg:" + requestResponse.resultMsg);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                log_init.debug("防劫触发：" + throwable);
            }
        });
    }

    public void onAppDownloadEntrance(View view) {
        log.debug("点击了下载的图标。。。");
        DialogUtils.showAppDownloadDialog(this);
        EventBus.getDefault().post(new Events.AppDownloadIconEvent(true));
    }
}
