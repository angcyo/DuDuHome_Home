package com.dudu.aios.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dudu.aios.ui.activity.MainRecordActivity;
import com.dudu.aios.ui.base.ObservableFactory;
import com.dudu.aios.ui.fragment.base.BaseFragment;
import com.dudu.aios.ui.utils.Debug;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.BtPhoneUtils;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.android.launcher.utils.Contacts;
import com.dudu.android.launcher.utils.DialogUtils;
import com.dudu.android.launcher.utils.ViewAnimation;
import com.dudu.android.launcher.utils.WeatherUtils;
import com.dudu.commonlib.event.Events;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.commonlib.utils.ModelUtil;
import com.dudu.commonlib.utils.TextVerify;
import com.dudu.drivevideo.frontcamera.FrontCameraManage;
import com.dudu.drivevideo.rearcamera.BlurControl;
import com.dudu.drivevideo.rearcamera.RearCameraManage;
import com.dudu.map.NavigationProxy;
import com.dudu.monitor.utils.SharedPreferencesUtil;
import com.dudu.persistence.RobberyMessage.RobberyMessage;
import com.dudu.rest.model.GetGuardStatusResponse;
import com.dudu.rest.model.GetRobberyStatusResponse;
import com.dudu.rest.model.common.RequestResponse;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voip.VoipSDKCoreHelper;
import com.dudu.weather.WeatherFlow;
import com.dudu.weather.WeatherInfo;
import com.dudu.workflow.common.DataFlowFactory;
import com.dudu.workflow.common.RequestFactory;
import com.dudu.workflow.obd.VehicleConstants;
import com.umeng.analytics.MobclickAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainFragment extends BaseFragment implements View.OnClickListener {

    private static final String TAG = "ui.MainFragment";
    private static final int STANDBY_INTERVAL = 15000;
    private Logger logger;
    private LinearLayout vehicleInspection, drivingRecord, navigation, bluetoothPhone, flow, preventRob;
    private RelativeLayout mDateWeatherContainer, mScreenContainer;
    private TextView mWeatherView, mTemperatureView;
    private ImageView mWeatherImage;
    private ImageButton voice_imageBtn;

    private LinearLayout mMenuButtonContainer, vipServerContainer;

    private Handler animHandler = new ViewDisappearHandler();
    private boolean isStartAnimation = false;

    private Subscription guardSubscription;

    private Subscription robberySubscription;

    @Override
    public View getView() {

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_main_layout, null);

        initFragmentView(view);

        initOnClickListener(view);

        initData();

        EventBus.getDefault().unregister(this);

        EventBus.getDefault().register(this);

        return view;
    }

    private void initData() {

        logger = LoggerFactory.getLogger("ui.MainFragment");

        initModel();

        getWeather();
    }

    private void initModel() {
        if (ModelUtil.needVip()) {
            vipServerContainer.setVisibility(View.VISIBLE);
        } else {
            vipServerContainer.setVisibility(View.INVISIBLE);
        }
    }

    private void getWeather() {
        weatherSubscriber(WeatherFlow.getInstance().requestWeather());
    }

    private void weatherSubscriber(Observable<WeatherInfo> observable) {
        observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(weatherInfo -> {
                            updateWeatherInfo(weatherInfo.getWeather(), weatherInfo.getTemperature());
                        },
                        throwable -> {
                        });
    }

    private void initOnClickListener(View view) {
        vehicleInspection.setOnClickListener(this);
        drivingRecord.setOnClickListener(this);
        navigation.setOnClickListener(this);
        bluetoothPhone.setOnClickListener(this);
        flow.setOnClickListener(this);
        preventRob.setOnClickListener(this);
        voice_imageBtn.setOnClickListener(this);
        vipServerContainer.setOnClickListener(this);
        mScreenContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    isStartAnimation = false;
                    starDisappearTask();
                    if (mDateWeatherContainer.getVisibility() == View.GONE) {
                        toggleAnimation();
                    }
//                    RearCameraManage.getInstance().stopPreview();
                    Debug.debug("MainFragment 设置模糊状态：true onTouch");
                    BlurControl.instance().setBlur(true);//
                }
                return false;
            }
        });

        mWeatherImage.setOnLongClickListener(v -> {
            //  EventBus.getDefault().post(new RobberyStateModel(true));
//            ACCReceiver.log.debug("正在倒车，打开行车记录界面");
//            Bundle bundle = new Bundle();
//            bundle.putBoolean(FragmentConstants.IS_BACK_CARING, true);
//            FragmentConstants.TEMP_ARGS = bundle;
//            replaceFragment(FragmentConstants.FRAGMENT_DRIVING_RECORD);
            return true;
        });
    }

    private void initFragmentView(View view) {

        vipServerContainer = (LinearLayout) view.findViewById(R.id.linearLayout_voip_service);
        mMenuButtonContainer = (LinearLayout) view.findViewById(R.id.button_menu_container);
        mDateWeatherContainer = (RelativeLayout) view.findViewById(R.id.date_weather_container);
        mScreenContainer = (RelativeLayout) view.findViewById(R.id.screen_container);

        vehicleInspection = (LinearLayout) view.findViewById(R.id.vehicle_inspection);
        drivingRecord = (LinearLayout) view.findViewById(R.id.driving_record_button);
        navigation = (LinearLayout) view.findViewById(R.id.navigation_button);
        bluetoothPhone = (LinearLayout) view.findViewById(R.id.bluetooth_phone_button);
        flow = (LinearLayout) view.findViewById(R.id.flow_button);
        preventRob = (LinearLayout) view.findViewById(R.id.prevent_rob);
        mTemperatureView = (TextView) view.findViewById(R.id.text_temperature);
        mWeatherView = (TextView) view.findViewById(R.id.text_weather);
        mWeatherImage = (ImageView) view.findViewById(R.id.weather_icon);
        voice_imageBtn = (ImageButton) view.findViewById(R.id.voice_imageBtn);
    }

    @DebugLog
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.screen_container:
                if (isStartAnimation) {
                    isStartAnimation = false;
                    starDisappearTask();
                    if (mDateWeatherContainer.getVisibility() == View.GONE) {
                        toggleAnimation();
                    }
                }
                break;
            case R.id.vehicle_inspection:
            case R.id.vehicle_inspection_icon:
//                vehicleInspection.setEnabled(false);
//                iconVehicleInspection.setEnabled(false);
                //行车自检
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_CARCHECKING.getEventId());
                replaceFragment(FragmentConstants.REQUEST_NETWORK_FRAGMENT);
                requestObtainGuardMessage(FragmentConstants.CAR_CHECKING);
                break;

            case R.id.driving_record_button:
            case R.id.driving_record_icon:
//                drivingRecord.setEnabled(false);
//                iconDrivingRecord.setEnabled(false);
                //行车记录
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_DRIVING_RECORD.getEventId());
                replaceFragment(FragmentConstants.FRAGMENT_DRIVING_RECORD);
                break;

            case R.id.navigation_button:
            case R.id.navigation_icon:
//                navigation.setEnabled(false);
//                iconNavigation.setEnabled(false);
                //导航
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_MAP.getEventId());
                NavigationProxy.getInstance().openNavi(NavigationProxy.OPEN_MANUAL);
                break;

            case R.id.bluetooth_phone_button:
            case R.id.bluetooth_phone_icon:
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_BLUETOOTH_CALL.getEventId());
                //蓝牙电话
                checkStateReplaceFragment();
                break;

            case R.id.flow_button:
            case R.id.flow_icon:
//                flow.setEnabled(false);
//                iconFlow.setEnabled(false);
                //wifi热点
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_FLOW.getEventId());
                replaceFragment(FragmentConstants.FRAGMENT_FLOW);
                break;

            case R.id.prevent_rob:
            case R.id.prevent_rob_icon:
//                preventRob.setEnabled(false);
//                iconRob.setEnabled(false);
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_ROBBERY_GUARD.getEventId());
                replaceFragment(FragmentConstants.REQUEST_NETWORK_FRAGMENT);
                requestObtainGuardMessage(FragmentConstants.FRAGMENT_SAFETY_CENTER);
                requestServerRobberyMessage();
                break;

            case R.id.voice_imageBtn:
//                voice_imageBtn.setEnabled(false);
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_VOICE.getEventId());
                VoiceManagerProxy.getInstance().stopWakeup();
                VoiceManagerProxy.getInstance().startVoiceService();
                break;
            case R.id.linearLayout_voip_service:
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_VIP.getEventId());
                Bundle voipBundle = new Bundle();
                voipBundle.putBoolean(VoipCallingFragment.VOIP_CALL_BUNDLE_KEY, true);
                FragmentConstants.TEMP_ARGS = voipBundle;
                replaceFragment(FragmentConstants.VOIP_CALLING_FRAGMENT);
                break;

        }
    }

    private void starDisappearTask() {
        animHandler.removeCallbacksAndMessages(null);
        animHandler.sendEmptyMessageDelayed(0, STANDBY_INTERVAL);

        Debug.debug("15秒后,切换模糊界面.");
    }


    private void updateWeatherInfo(String weather, String temperature) {

        if (!TextUtils.isEmpty(weather) && !TextUtils.isEmpty(temperature)) {
            if (weather.contains("-")) {
                weather = weather
                        .replace("-", getString(R.string.weather_turn));
            }

            mTemperatureView.setTextSize(sp2px(getContext(), 22));

            mTemperatureView.setText(temperature + getString(R.string.temperature_degree));

            mWeatherView.setText(weather);
            mWeatherImage.setImageResource(WeatherUtils
                    .getWeatherIcon(WeatherUtils.getWeatherType(weather)));
        } else {
            //获取天气失败
            mWeatherView.setGravity(Gravity.CENTER);
            mWeatherView.setText(R.string.unkown_weather_info);
            mTemperatureView.setText("");
        }
    }

    public int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    @Override
    public void onResume() {
        super.onResume();
        logger.debug("fragment is onResume()");
        DialogUtils.dismissAppDownloadDialog();
        RearCameraManage.log.debug("fragment is onResume()");
        Debug.debug("MainFragment onResume isVisible:" + isVisible());

        showMainFragmentAnim();

        vehicleInspection.setEnabled(true);
        drivingRecord.setEnabled(true);
        navigation.setEnabled(true);
        bluetoothPhone.setEnabled(true);
        flow.setEnabled(true);
        preventRob.setEnabled(true);

        voice_imageBtn.setEnabled(true);

        toggleBlur(true);
//        FrontCameraManage.getInstance().setPreviewBlur(true);
        ObservableFactory.getInstance().getCommonObservable().hasBackground.set(false);

    }

    @Override
    public void onPause() {
        super.onPause();
        animHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onHide() {
        super.onHide();
        logger.debug("fragment is onHide()");
        RearCameraManage.log.debug("fragment is onHide()");
        animHandler.removeCallbacksAndMessages(null);
        DialogUtils.dismissAppDownloadDialog();

//        if (RearCameraManage.getInstance().isPreviewIng()) {
//            RearCameraManage.getInstance().stopPreview();
//        }
        Debug.debug("MainFragment 设置模糊状态：true onHide");
        BlurControl.instance().setBlur(true);//取消模糊
        LoggerFactory.getLogger("video1.frontdrivevideo").debug("设置模糊状态：{}", true);
        FrontCameraManage.getInstance().setPreviewBlur(true);
//        FrontCameraInstance.getInstance().switchBlur(true);
    }

    @Override
    public void onShow() {
        super.onShow();
        logger.debug("fragment is onShow()");
        RearCameraManage.log.debug("fragment is onShow()");
        if (LauncherApplication.getContext().getInstance() != null) {
            LauncherApplication.getContext().getInstance().showTitleColorTransparent();
            LauncherApplication.getContext().getInstance().showAppDownloadIcon();
        }

        showMainFragmentAnim();
        ObservableFactory.getInstance().getCommonObservable().hasBackground.set(false);
        //FrontCameraManage.getInstance().setPreviewBlur(true);
        LoggerFactory.getLogger("video1.frontdrivevideo").debug("设置模糊状态：{}", true);

        /*if (ACCReceiver.isBackCarIng == true) {
            ACCReceiver.log.debug("正在倒车，打开行车记录界面");
            replaceFragment(FragmentConstants.FRAGMENT_DRIVING_RECORD);
        }*/

        FrontCameraManage.getInstance().setPreviewBlur(true);
//        FrontCameraInstance.getInstance().switchBlur(true);
        if(!ObservableFactory.getInstance().getTitleObservable().bluetooth.get()){
            //检查蓝牙连接状态
            BtPhoneUtils.checkUpdateBluetoothConnectState();
        }

    }

    private void showMainFragmentAnim() {
        DialogUtils.dismissAppDownloadDialog();
        mDateWeatherContainer.clearAnimation();
        mMenuButtonContainer.clearAnimation();
        mMenuButtonContainer.setVisibility(View.VISIBLE);
        mDateWeatherContainer.setVisibility(View.VISIBLE);
        isStartAnimation = false;
        animHandler.removeCallbacksAndMessages(null);
        animHandler.sendEmptyMessageDelayed(0, STANDBY_INTERVAL);
        if (LauncherApplication.getContext().getInstance() != null) {
            LauncherApplication.getContext().getInstance().showTitle(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(getActivity());
    }

    private void toggleAnimation() {
        //关闭模糊,启动录像
        toggleBlur(!mDateWeatherContainer.isShown());

        MainRecordActivity mainRecordActivity = (MainRecordActivity) getActivity();
        if (mainRecordActivity == null) {
            return;
        }
        Fragment mainFragment = mainRecordActivity.getFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
        if (mainFragment != null && mainFragment.isVisible()) {
            //当前在主界面
            toggleMenuButtonAnimation();
            toggleDateWeatherAnimation();

//            if (!RearCameraManage.getInstance().isPreviewIng()) {
//                RearCameraManage.getInstance().startPreview();
//            } else
            Debug.debug("MainFragment 设置模糊状态：false ..toggleAnimation");
            BlurControl.instance().setBlur(false);//取消模糊
            {
                LoggerFactory.getLogger("video.reardrivevideo").debug("正在预览，无需再次开启预览");
            }
        }
    }

    private void toggleBlur(boolean blur) {
        Debug.debug("MainFragment toggleBlur isVisible ：" + isVisible());

        if (!isVisible()) {
            return;
        }

        Debug.debug("MainFragment 设置模糊状态 toggleBlur ：" + blur);
        BlurControl.instance().setBlur(blur);

        MainRecordActivity mainRecordActivity = (MainRecordActivity) getActivity();
        if (mainRecordActivity == null) {
            return;
        }
        Fragment fragment = mainRecordActivity.getFragment(FragmentConstants.FRAGMENT_DRIVING_RECORD);
        if (fragment != null && fragment.isVisible()) {
            //行车记录界面
        } else {
//            ((BaseActivity) getActivity()).setBlur(blur);
            //FrontCameraManage.getInstance().setPreviewBlur(blur);
            LoggerFactory.getLogger("video1.frontdrivevideo").debug("设置模糊状态：{}", blur);
            FrontCameraManage.getInstance().setPreviewBlur(blur);
//            FrontCameraInstance.getInstance().switchBlur(blur);
        }
    }


    private void toggleDateWeatherAnimation() {
        ViewAnimation.startAnimation(mDateWeatherContainer, mDateWeatherContainer.getVisibility() == View.VISIBLE ? R.anim.date_weather_disappear : R.anim.date_weather_appear, getActivity());
        ViewAnimation.onAnimPlayListener(isPlay -> isStartAnimation = true);
    }

    private void toggleMenuButtonAnimation() {
        ViewAnimation.startAnimation(mMenuButtonContainer, mMenuButtonContainer.getVisibility() == View.VISIBLE ? R.anim.menu_button_disappear : R.anim.menu_button_appear, getActivity());
    }

    /**
     * 根据蓝牙电话状态显示界面
     */
    private void checkStateReplaceFragment(){
        Log.d("phone", "BtPhoneUtils.btCallState:" + BtPhoneUtils.btCallState +
                "VoipSDKCoreHelper.getInstance().eccall_state:" + VoipSDKCoreHelper.getInstance().eccall_state);
        if (!(VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_PROCEEDING ||
                VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_ANSWERED ||
                VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_ALERTING)) {
            //网络电话不在工作中
            String number = "";
            if (null != FragmentConstants.TEMP_ARGS) {
                number = FragmentConstants.TEMP_ARGS.getString(Constants.EXTRA_PHONE_NUMBER);
            }
            if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ACTIVE &&
                    !TextUtils.isEmpty(number)) {
                //通话中
                replaceFragment(FragmentConstants.BT_CALLING);
            } else if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_INCOMING &&
                    !TextUtils.isEmpty(number)) {
                //来电中
                replaceFragment(FragmentConstants.BT_IN_CALL);
            } else if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_DIALING &&
                    !TextUtils.isEmpty(number)) {
                //拨号中
                replaceFragment(FragmentConstants.BT_OUT_CALL);
            }else if(BtPhoneUtils.getAudioState()==2 && !TextUtils.isEmpty(number)){
                //蓝牙语音在连接中
                replaceFragment(FragmentConstants.BT_CALLING);
            }else {
                MobclickAgent.onEvent(getActivity(), ClickEvent.click45.getEventId());
                replaceFragment(FragmentConstants.BT_DIAL);
            }
        }
    }

    private void requestObtainGuardMessage(String fragmentName) {
        guardSubscription = RequestFactory.getGuardRequest().getLockStatus().subscribe(statusResponse -> {
                    logger.debug("请求获取防盗的网络的状态:" + statusResponse.result + "resultCode:" + statusResponse.resultCode);
                    guardSubscription.unsubscribe();
                    if (statusResponse != null) {
                        if (statusResponse.resultCode == 0) {
                            SharedPreferencesUtil.putBooleanValue(Contacts.BINDING_STATE, true);
                            GetGuardStatusResponse.GetGuardStatusResult result = statusResponse.result;
                            if (result != null) {
                                int auditStatus = result.audit_state;
                                logger.debug("开始保存从服务器请求下来的审核状态：" + auditStatus);
                                DataFlowFactory.getUserMessageFlow().saveAuditState(auditStatus);
                                if (auditStatus == Contacts.AUDIT_STATE_PASS) {
                                    String gesturePassword = result.protect_thief_signal_password;
                                    logger.debug("开始保存从服务器请求下来的手势密码：" + gesturePassword);
                                    int gesturePasswordSwitchState = result.protect_thief_signal_state;
                                    logger.debug("开始保存从服务器请求下来的手势密码的开关状态：" + (gesturePasswordSwitchState == 1 ? "开启" : "关闭"));
                                    String digitPassword = result.protect_thief_password;
                                    logger.debug("开始保存从服务器请求下来的数字密码：" + digitPassword);
                                    int digitPasswordSwitchState = result.protect_thief_state;
                                    logger.debug("开始保存从服务器请求下来的数字  密码的开关状态：" + (digitPasswordSwitchState == 1 ? "开启" : "关闭"));
                                    int guardSwitchStatus = statusResponse.result.thief_switch_state;
                                    logger.debug("开始保存从服务器请求下来的防盗的开关状态  ：" + guardSwitchStatus);
                                    DataFlowFactory.getUserMessageFlow().saveGuardStatus(gesturePassword, gesturePasswordSwitchState == 1 ? true : false, digitPassword, digitPasswordSwitchState == 1 ? true : false);
                                    checkGuardSwitchFromDB(guardSwitchStatus == 1);
                                    if (fragmentName.equals(FragmentConstants.CAR_CHECKING)) {
                                        if(null==FragmentConstants.TEMP_ARGS ){
                                            FragmentConstants.TEMP_ARGS = new Bundle();
                                        }
                                        FragmentConstants.TEMP_ARGS.putBoolean(VehicleConstants.START_CHECKING, true);
                                    } else {
//                                        FragmentConstants.TEMP_ARGS = null;
                                    }
                                    replaceFragment(fragmentName);
                                } else {
                                    if (fragmentName.equals(FragmentConstants.CAR_CHECKING)) {
                                        showLicensePromptFragment();
                                    } else {
                                        replaceFragment(fragmentName);
                                    }

                                }
                            }

                        } else if (statusResponse.resultCode == 40200) {
                            showBindingFragments();
                        }
                    }
                }
                , throwable -> {
                    logger.error("getLockStatus onError:" + throwable);
                    guardSubscription.unsubscribe();
                    queryAuditStateDB(fragmentName);
                }

        );
    }

    private void checkGuardSwitchFromDB(boolean isOpen) {
        DataFlowFactory.getSwitchDataFlow().getGuardSwitch().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                logger.debug("查看本地与服务器是否相同：" + ((isOpen == aBoolean) ? "相同" : "不相同"));
                if (!isOpen == aBoolean) {
                    syncServerGuardSwitch(aBoolean);
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                logger.debug("throwable:" + throwable);
                DataFlowFactory.getSwitchDataFlow().saveGuardSwitch(isOpen);
            }
        });
    }

    private void showLicensePromptFragment() {
        SharedPreferencesUtil.putLongValue(Contacts.LICENSE_TYPE, Contacts.DRIVING_TYPE);
        replaceFragment(FragmentConstants.LICENSE_UPLOAD_UPLOAD_FRAGMENT);
    }

    protected void showBindingFragments() {
        replaceFragment(FragmentConstants.FRAGMENT_DEVICE_BINDING);
    }

    private void queryAuditStateDB(String fragmentName) {
        boolean hasBinded = SharedPreferencesUtil.getBooleanValue(Contacts.BINDING_STATE, false);
        if (hasBinded) {
            DataFlowFactory.getUserMessageFlow().obtainUserMessage()
                    .map(userMessage -> userMessage.getAudit_state())
                    .subscribe(auditState -> {
                        logger.debug("查询数据库的审核状态：" + auditState);
                        if (auditState == Contacts.AUDIT_STATE_PASS) {
                            if (fragmentName.equals(FragmentConstants.CAR_CHECKING)) {
                                if(null==FragmentConstants.TEMP_ARGS){
                                    FragmentConstants.TEMP_ARGS = new Bundle();
                                }
                                FragmentConstants.TEMP_ARGS.putBoolean(VehicleConstants.START_CHECKING, true);
                            }
                            replaceFragment(fragmentName);
                        } else {
                            if (fragmentName.equals(FragmentConstants.CAR_CHECKING)) {
                                showLicensePromptFragment();
                            } else {
                                replaceFragment(fragmentName);
                            }
                        }

                    }, throwable -> logger.error("queryAuditStateDB", throwable));

        } else {
            showBindingFragments();
        }
    }

    private void requestServerRobberyMessage() {
        robberySubscription = RequestFactory.getRobberyRequest()
                .getRobberyState()
                .subscribe(requestResponse -> {
                    logger.debug("获取服务器的防劫的信息  resultCode：{}{}", requestResponse.resultCode, requestResponse.result);
                    robberySubscription.unsubscribe();
                    if (requestResponse != null && requestResponse.result != null) {
                        String auditStatus = requestResponse.result.audit_state;
                        SharedPreferencesUtil.putLongValue(Contacts.AUDIT_STATE, Long.parseLong(auditStatus));
                        transFormRobberyMessage(requestResponse.result);
                    }
                }, throwable -> {
                    logger.error("getRobberyState", throwable);
                    robberySubscription.unsubscribe();
                });
    }

    private void transFormRobberyMessage(GetRobberyStatusResponse.Result result) {
        if (!TextVerify.isEmpty(result.protect_rob_state) && !TextVerify.isEmpty(result.revolutions) && !TextVerify.isEmpty(result.numberOfOperations) && !TextVerify.isEmpty(result.completeTime)) {
            DataFlowFactory.getRobberyMessageFlow().changeRobberyMessage("1".equals(result.protect_rob_state), result.revolutions, result.numberOfOperations, result.completeTime);
        }
        String robberyTriggerStatus = result.protectRobTriggerSwitchState;
        if (!TextVerify.isEmpty(robberyTriggerStatus)) {
            checkRobberyTriggerSwitchFromDB("1".equals(robberyTriggerStatus));
        }
    }

    private void checkRobberyTriggerSwitchFromDB(boolean isOpen) {
        DataFlowFactory.getRobberyMessageFlow().getRobberyTriggerSwitch().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                logger.debug("获取防劫触发的值:" + (aBoolean == isOpen ? "相同" : "不相同"));
                if (isOpen != aBoolean) {
                    requestRobberySwitchStatus(aBoolean);
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                logger.debug("throwable:" + throwable);
            }
        });
    }

    private void syncServerGuardSwitch(boolean isOpen) {
        logger.debug(isOpen ? "lockGuard" : "unlockGuard");
        RequestFactory.getGuardRequest()
                .checkLockCar(isOpen)
                .subscribe(setGuardStateResponse -> {
                    if (setGuardStateResponse.resultCode == 0) {
                        logger.debug(isOpen ? "加锁成功" : "解锁成功");
                    } else {
                        logger.debug(setGuardStateResponse.resultMsg);
                    }

                }, throwable -> {
                    logger.error(isOpen ? "lockGuard" : "unlockGuard" + " getError", throwable);
                });
    }

    public void onEventMainThread(Events.OpenSafeCenterEvent event) {
        logger.debug("收到语音打开的事件：" + event.getOpenType());
        switch (event.getOpenType()) {
            case Events.OPEN_SAFETY_CENTER:
                replaceFragment(FragmentConstants.REQUEST_NETWORK_FRAGMENT);
                requestObtainGuardMessage(FragmentConstants.FRAGMENT_SAFETY_CENTER);
                requestServerRobberyMessage();
                break;
            case Events.OPEN_VEHICLE_INSPECTION:
                replaceFragment(FragmentConstants.REQUEST_NETWORK_FRAGMENT);
                requestObtainGuardMessage(FragmentConstants.CAR_CHECKING);
                break;
            case Events.OPEN_TYRE:
                replaceFragment(FragmentConstants.REQUEST_NETWORK_FRAGMENT);
                requestObtainGuardMessage(FragmentConstants.TIRE_FRAGMENT);
                break;
        }

    }

    public void onEventMainThread(Events.RequestNetworkBackEvent event) {
        if (robberySubscription != null && !robberySubscription.isUnsubscribed()) {
            logger.debug("取消防劫请求网络的订阅");
            robberySubscription.unsubscribe();
        }
        if (guardSubscription != null && !guardSubscription.isUnsubscribed()) {
            logger.debug("取消防盗请求网络的订阅");
            guardSubscription.unsubscribe();
        }
    }

    public void onEventMainThread(Events.AppDownloadIconEvent event) {
        if (event.isExit()) {
            logger.debug("点击了app下载的图标");
            if (mDateWeatherContainer.isShown()) {
                toggleDateWeatherAnimation();
                toggleMenuButtonAnimation();
                animHandler.removeCallbacksAndMessages(null);
            }
            FrontCameraManage.getInstance().setPreviewBlur(true);
        } else {
            logger.debug("取消点击了app下载的图标");
            FrontCameraManage.getInstance().setPreviewBlur(false);
        }
    }

    public void requestRobberySwitchStatus(boolean isOpen) {
        DataFlowFactory.getRobberyMessageFlow().obtainRobberyMessage().subscribe(new Action1<RobberyMessage>() {
            @Override
            public void call(RobberyMessage robberyMessage) {
                robberyMessage.setRobberTrigger(isOpen);
                RequestFactory.getRobberyRequest().settingAntiRobberyMode(robberyMessage).subscribe(new Action1<RequestResponse>() {
                    @Override
                    public void call(RequestResponse requestResponse) {
                        if (requestResponse.resultCode == 0) {
                            logger.debug("请求网络：" + requestResponse.resultMsg);
                        } else {
                            logger.debug("请求网络：" + requestResponse.resultMsg + "   " + requestResponse.resultCode);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.debug("请求网络失败：" + throwable);
                    }
                });
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {

            }
        });


    }

    private class ViewDisappearHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            toggleAnimation();
        }
    }
}
