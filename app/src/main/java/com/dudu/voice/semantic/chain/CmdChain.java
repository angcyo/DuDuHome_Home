package com.dudu.voice.semantic.chain;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;

import com.dudu.aios.ui.activity.MainRecordActivity;
import com.dudu.aios.ui.activity.weather.WeatherActivity;
import com.dudu.aios.ui.base.BaseFragmentManagerActivity;
import com.dudu.aios.ui.map.AddressSearchActivity;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.broadcast.ACCReceiver;
import com.dudu.android.launcher.utils.ActivitiesManager;
import com.dudu.android.launcher.utils.BtPhoneUtils;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.android.launcher.utils.Contacts;
import com.dudu.android.launcher.utils.WifiApAdmin;
import com.dudu.commonlib.event.Events;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.commonlib.utils.ModelUtil;
import com.dudu.drivevideo.frontcamera.FrontCameraManage;
import com.dudu.drivevideo.rearcamera.RearCameraManage;
import com.dudu.event.DeviceEvent;
import com.dudu.map.NavigationProxy;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.bean.CmdBean;
import com.dudu.voice.semantic.bean.SemanticBean;
import com.dudu.voice.semantic.constant.SemanticConstant;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.voice.semantic.engine.SemanticEngine;
import com.dudu.workflow.obd.VehicleConstants;
import com.umeng.analytics.MobclickAgent;

import de.greenrobot.event.EventBus;

/**
 * Created by 赵圣琪 on 2015/10/29.
 */
public class CmdChain extends SemanticChain {

    @Override
    public boolean matchSemantic(String service) {
        return SemanticConstant.SERVICE_CMD.equals(service);
    }

    @Override
    public boolean doSemantic(SemanticBean semantic) {
        return handleCmd((CmdBean) semantic);
    }

    private boolean handleCmd(CmdBean bean) {

        String action = bean.getAction();
        String target = bean.getTarget();
        if (target == null) {

        } else {
            if (target.contains(Constants.NAVIGATION)) {
                return handleNavigationCmd(action);
            } else if (target.contains(SemanticConstant.RECORD_CN)) {
                return handleVideoCmd(action);
            } else if (target.equals(Constants.WIFI) || target.equals(Constants.WIFI_CN)) {
                handleWifi(action);
                return true;
            } else if (target.contains(Constants.SPEECH)) {
                handleExitCmd();
                return true;
            } else if (target.contains(Constants.EXIT)) {
                handleExitCmd();
                return true;
            } else if (target.contains(Constants.BACK)) {
                handleBackCmd();
                return true;
            } else if (target.contains(Constants.MAP)) {
                return handleMapCmd(action);
            } else if (target.contains(Constants.SELF_CHECKING)) {
                return handleSelfChecking(action);
            } else if (target.contains(Constants.ROBBERY)) {
                handleRobbery();
                return true;
            } else if (target.contains(Constants.GUARD)) {
                handleGuard();
                return true;
            } else if (target.contains(Constants.FLOWPAY)) {
                handleFlowPay();
                return true;
            } else if (target.contains(Constants.GUARDUNLOCK)) {
                handleUnlockGuard();
                return true;
            } else if (target.contains(Constants.OPEN_ROBBERY)) {
                handleOpenRobbery();
                return true;
            } else if (target.contains(Constants.CONTACT)) {
                openContact();
                return true;
            } else if (target.equals(Constants.VIPSERVICE)) {
                return handleVipService();
            } else if (target.equals(Constants.BT_CALL)) {
                openBtCall();
                return true;
            } else if (target.equals(Constants.SCREEN)) {
                handleScreen(action);
                return true;
            } else if (target.equals(Constants.REAR_RECOED)) {
                handleRearRecord();
                return true;
            } else if (target.equals(Constants.TYRE)) {
                return handleTyre();
            } else if (target.equals(Constants.TAKE_PICTURE)) {
                return takePicture();
            } else if (target.equals(Constants.LOOK_PICTURE)) {
                return handleLookPicture();
            }
        }

        return false;
    }

    private boolean handleNavigationCmd(String option) {
        switch (option) {
            case Constants.OPEN:
            case Constants.START:
                return NavigationProxy.getInstance().openNavi(NavigationProxy.OPEN_VOICE);
            case Constants.CLOSE:
            case Constants.EXIT:
                floatWindowUtils.removeFloatWindow();
                NavigationProxy.getInstance().existNavi();
                toMainActivity();
                if (LauncherApplication.getContext().getInstance() != null) {
                    LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
                }
                MobclickAgent.onEvent(mContext, ClickEvent.voice13.getEventId());
                break;
        }
        return true;
    }

    private boolean handleVideoCmd(String option) {
        switch (option) {
            case Constants.OPEN:
            case Constants.QIDONG:
            case Constants.KAIQI:
                floatWindowUtils.removeFloatWindow();
                toMainActivity();
                LauncherApplication.startRecord = true;
                if (LauncherApplication.getContext().getInstance() != null) {
                    LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_DRIVING_RECORD);
                }
                MobclickAgent.onEvent(mContext, ClickEvent.voice8.getEventId());

                return true;
            case Constants.CLOSE:
            case Constants.EXIT:
            case Constants.GUANDIAO:
                floatWindowUtils.removeFloatWindow();

                try {
                    if (LauncherApplication.getContext().getInstance() != null) {
                        LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
                    }
//                    LauncherApplication.getContext().getInstance().setBlur();
                    FrontCameraManage.getInstance().setPreviewBlur(true);
                } catch (Exception e) {

                }
                RearCameraManage.getInstance().stopPreview();
                MobclickAgent.onEvent(mContext, ClickEvent.voice9.getEventId());

                return true;
            case Constants.PLAY:
                floatWindowUtils.removeFloatWindow();
                toMainActivity();
                if (LauncherApplication.getContext().getInstance() != null) {
                    LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_VIDEO_LIST);
                }
                return true;
            case Constants.CHANGE_SHOT:
                floatWindowUtils.removeFloatWindow();
                toMainActivity();
                Bundle bundle = new Bundle();
                bundle.putString(FragmentConstants.OPEN_VIDEO_TYPE, FragmentConstants.CHANGE_SHOT);
                FragmentConstants.TEMP_ARGS = bundle;
                LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_DRIVING_RECORD);
                return true;
            default:
                return false;
        }
    }


    private void handleBackCmd() {

        floatWindowUtils.removeFloatWindow();
        Activity topActivity = ActivitiesManager.getInstance().getTopActivity();
        if (topActivity instanceof AddressSearchActivity) {
            ActivitiesManager.getInstance().closeTargetActivity(topActivity.getClass());
        } else {
            toMainActivity();
            if (LauncherApplication.getContext().getInstance() != null) {
                LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
            }
            ActivitiesManager.getInstance().closeTargetActivity(WeatherActivity.class);
            MobclickAgent.onEvent(mContext, ClickEvent.voice65.getEventId());
        }

    }

    private void handleExitCmd() {
        floatWindowUtils.removeFloatWindow();
        SemanticEngine.getProcessor().clearSemanticStack();
        ActivitiesManager.getInstance().closeTargetActivity(AddressSearchActivity.class);
//        toMainActivity();
        if (isCarChecking() && LauncherApplication.getContext().getInstance() != null
                || ActivitiesManager.getInstance().getTopActivity() instanceof WeatherActivity) {
            ActivitiesManager.getInstance().closeTargetActivity(WeatherActivity.class);
            LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
        }
        MobclickAgent.onEvent(mContext, ClickEvent.voice64.getEventId());

    }

    private boolean handleMapCmd(String option) {
        floatWindowUtils.removeFloatWindow();
        switch (option) {
            case Constants.OPEN:
            case Constants.START:
            case Constants.KAIQI:
                if (NavigationProxy.getInstance().openNavi(NavigationProxy.OPEN_MAP)) {
                    floatWindowUtils.removeFloatWindow();
                    return true;
                }
                return false;
            case Constants.CLOSE:
            case Constants.EXIT:
                NavigationProxy.getInstance().closeMap();
                break;
        }
        return true;
    }

    private boolean handleSelfChecking(String action) {
        switch (action) {
            case Constants.OPEN:
            case Constants.QIDONG:
            case Constants.KAIQI:
            case Constants.START:
                floatWindowUtils.removeFloatWindow();
                toMainActivity();
                Bundle bundle = new Bundle();
                bundle.putBoolean(VehicleConstants.START_CHECKING, true);
                FragmentConstants.TEMP_ARGS = bundle;
                EventBus.getDefault().post(new Events.OpenSafeCenterEvent(Events.OPEN_VEHICLE_INSPECTION));
                MobclickAgent.onEvent(mContext, ClickEvent.voice1.getEventId());
                break;
            case Constants.CLOSE:
            case Constants.EXIT:
            case Constants.GUANDIAO:
                floatWindowUtils.removeFloatWindow();
                if (LauncherApplication.getContext().getInstance() != null) {
                    LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
                }
                MobclickAgent.onEvent(mContext, ClickEvent.voice2.getEventId());

                break;
            default:
                return false;
        }

        return true;
    }

    private void handleWifi(String option) {
        switch (option) {
            case Constants.OPEN:
                WifiApAdmin.startWifiAp(mContext);
                mVoiceManager.startSpeaking("Wifi热点已打开", TTSType.TTS_START_UNDERSTANDING, true);
                MobclickAgent.onEvent(mContext, ClickEvent.voice56.getEventId());
                break;
            case Constants.CLOSE:
                WifiApAdmin.closeWifiAp(mContext);
                mVoiceManager.startSpeaking("Wifi热点已关闭", TTSType.TTS_START_UNDERSTANDING, true);
                MobclickAgent.onEvent(mContext, ClickEvent.voice57.getEventId());
                break;

            default:
                handleFlowPay();
                break;

        }
    }


    private void handleRobbery() {
        floatWindowUtils.removeFloatWindow();
        toMainActivity();
        Bundle bundle = new Bundle();
        bundle.putInt(VehicleConstants.SHOW_GUARD_OR_ROBBERY, Contacts.SHOW_ROBBERY_FRAGMENT);
        FragmentConstants.TEMP_ARGS = bundle;
        EventBus.getDefault().post(new Events.OpenSafeCenterEvent(Events.OPEN_SAFETY_CENTER));
        MobclickAgent.onEvent(mContext, ClickEvent.voice52.getEventId());

    }

    private void handleGuard() {
        floatWindowUtils.removeFloatWindow();
        toMainActivity();
        Bundle bundle = new Bundle();
        bundle.putInt(VehicleConstants.SHOW_GUARD_OR_ROBBERY, Contacts.SHOW_GUARD_FRAGMENT);
        FragmentConstants.TEMP_ARGS = bundle;
        EventBus.getDefault().post(new Events.OpenSafeCenterEvent(Events.OPEN_SAFETY_CENTER));
        MobclickAgent.onEvent(mContext, ClickEvent.voice53.getEventId());

    }

    private void toMainActivity() {

        if (!(ActivitiesManager.getInstance().getTopActivity() instanceof MainRecordActivity)
                || LauncherApplication.getContext().isReceivingOrder()
                || !ActivitiesManager.getInstance().isTopActivity(mContext, "com.dudu.android.launcher")) {
            LauncherApplication.getContext().setReceivingOrder(false);
            Intent intent = new Intent();
            intent.setClass(mContext, MainRecordActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }
    }

    private void handleFlowPay() {
        floatWindowUtils.removeFloatWindow();
        toMainActivity();
        if (LauncherApplication.getContext().getInstance() != null) {
            LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_FLOW);
        }
        MobclickAgent.onEvent(mContext, ClickEvent.voice58.getEventId());

    }

    private void handleUnlockGuard() {
        floatWindowUtils.removeFloatWindow();
        toMainActivity();
        Bundle bundle = new Bundle();
        bundle.putInt(VehicleConstants.SHOW_GUARD_OR_ROBBERY, Contacts.SHOW_GUARD_FRAGMENT);
        bundle.putBoolean(VehicleConstants.UNLOCK_GUARD, true);
        FragmentConstants.TEMP_ARGS = bundle;
        EventBus.getDefault().post(new Events.OpenSafeCenterEvent(Events.OPEN_SAFETY_CENTER));
        MobclickAgent.onEvent(mContext, ClickEvent.voice55.getEventId());

    }

    private void handleOpenRobbery() {
        floatWindowUtils.removeFloatWindow();
        toMainActivity();
        Bundle bundle = new Bundle();
        bundle.putInt(VehicleConstants.SHOW_GUARD_OR_ROBBERY, Contacts.SHOW_ROBBERY_FRAGMENT);
        bundle.putBoolean(VehicleConstants.OPEN_ROBBERY, true);
        FragmentConstants.TEMP_ARGS = bundle;
        EventBus.getDefault().post(new Events.OpenSafeCenterEvent(Events.OPEN_SAFETY_CENTER));
        MobclickAgent.onEvent(mContext, ClickEvent.voice54.getEventId());

    }

    private void openContact() {
        floatWindowUtils.removeFloatWindow();
        toMainActivity();
        if (LauncherApplication.getContext().getInstance() != null) {
            LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.BT_CONTACTS);
        }
        MobclickAgent.onEvent(mContext, ClickEvent.voice50.getEventId());

    }

    private boolean handleVipService() {
        if (ModelUtil.needVip()) {
            toMainActivity();
            Bundle voipBundle = new Bundle();
            voipBundle.putBoolean("call", true);
            FragmentConstants.TEMP_ARGS = voipBundle;
            if (LauncherApplication.getContext().getInstance() != null) {
                LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.VOIP_CALLING_FRAGMENT);
            }
            MobclickAgent.onEvent(mContext, ClickEvent.voice61.getEventId());
            return true;
        }
        return false;
    }

    private void openBtCall() {
        toMainActivity();
        //拨号前先判断蓝牙是否处于连接状态
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (null == adapter) {
            VoiceManagerProxy.getInstance().startSpeaking(
                    mContext.getString(R.string.bt_noti_disenable), TTSType.TTS_START_UNDERSTANDING, true);
            return;
        }
        if (!adapter.isEnabled() || BtPhoneUtils.connectionState != BtPhoneUtils.STATE_CONNECTED) {
            adapter.enable();
            VoiceManagerProxy.getInstance().startSpeaking(
                    mContext.getString(R.string.bt_noti_connect_waiting), TTSType.TTS_START_UNDERSTANDING, true);
            return;
        }

        floatWindowUtils.removeFloatWindow();
        if (LauncherApplication.getContext().getInstance() != null) {
            LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.BT_DIAL);
        }
        MobclickAgent.onEvent(mContext, ClickEvent.voice49.getEventId());

    }

    private void handleScreen(String action) {
        if (action.equals(Constants.CLOSE)) {
            floatWindowUtils.removeFloatWindow();
            EventBus.getDefault().post(new DeviceEvent.Screen(DeviceEvent.OFF));
            MobclickAgent.onEvent(mContext, ClickEvent.voice62.getEventId());
        }
    }

    private void handleRearRecord() {
        floatWindowUtils.removeFloatWindow();
        ACCReceiver.startBackCarPreview();
        MobclickAgent.onEvent(mContext, ClickEvent.voice11.getEventId());

    }

    private boolean isCarChecking() {
        if (ActivitiesManager.getInstance().getTopActivity() instanceof MainRecordActivity) {
            BaseFragmentManagerActivity baseFragmentManagerActivity = (BaseFragmentManagerActivity) ActivitiesManager.getInstance().getTopActivity();
            String currentStackTag = baseFragmentManagerActivity.getCurrentStackTag();
            return (FragmentConstants.CAR_CHECKING.equals(currentStackTag)
                    || FragmentConstants.REPAIR_FAULT_CODE_FRAGMENT.equals(currentStackTag)
                    || FragmentConstants.VEHICLE_ANIMATION_FRAGMENT.equals(currentStackTag));
        }
        return false;
    }

    private boolean handleTyre() {
        floatWindowUtils.removeFloatWindow();
        toMainActivity();
        EventBus.getDefault().post(new Events.OpenSafeCenterEvent(Events.OPEN_TYRE));
        LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.TIRE_FRAGMENT);
        return true;
    }

    private boolean takePicture() {
        floatWindowUtils.removeFloatWindow();
        toMainActivity();
        Bundle bundle = new Bundle();
        bundle.putString(FragmentConstants.OPEN_VIDEO_TYPE, FragmentConstants.TAKE_PICTURE);
        FragmentConstants.TEMP_ARGS = bundle;
        LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_DRIVING_RECORD);
        return true;
    }

    private boolean handleLookPicture() {
        floatWindowUtils.removeFloatWindow();
        toMainActivity();
        LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_PHOTO_LIST);
        return true;
    }
}
