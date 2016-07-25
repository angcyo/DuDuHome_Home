package com.dudu.android.launcher.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.dudu.aios.ui.activity.MainRecordActivity;
import com.dudu.aios.ui.fragment.video.DrivingRecordFragment;
import com.dudu.aios.ui.map.AddressSearchActivity;
import com.dudu.aios.ui.map.GaodeMapActivity;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.utils.ActivitiesManager;
import com.dudu.android.launcher.utils.BtPhoneUtils;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.android.launcher.utils.Utils;
import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.utils.File.KeyConstants;
import com.dudu.drivevideo.rearcamera.RearCameraManage;
import com.dudu.init.CarFireManager;
import com.dudu.map.NavigationProxy;
import com.dudu.navi.NavigationManager;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.TTSType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.schedulers.Schedulers;

public class ACCReceiver extends BroadcastReceiver {
    private final static String ACTION_ACC_BL_CHANGED = "android.intent.action.ACC_BL";
    private final static String ACTION_ACC_ON_CHANGED = "android.intent.action.ACC_ON";

    /* 标记是否正在倒车*/
    public static boolean isBackCarIng = false;
    public static boolean isFactroyIng = false;

    public static boolean previewIngFlag = false;

    public static Logger log = LoggerFactory.getLogger("video.reardrivevideo");

    public ACCReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (isFactroyIng) return;
        if(!KeyConstants.is_agree_disclaimer)
            return;

        if (intent.getAction().equals(ACTION_ACC_BL_CHANGED)) {
            boolean backed = intent.getBooleanExtra("backed", false);
            log.debug("收到倒车广播 acc_bl:{}", backed);
            isBackCarIng = backed;
            if (backed == true) {
                if (Utils.isDemoVersion(context)) {
                    VoiceManagerProxy.getInstance().startSpeaking("正在倒车", TTSType.TTS_DO_NOTHING, false);
                }
                /*if (!(BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ACTIVE
                        || BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_INCOMING
                        || BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_DIALING)) {
                    startBackCarPreview();
                } else {
                    log.info("来电中，不开启倒车预览 蓝牙电话状态：{}", BtPhoneUtils.btCallState);
                }*/
                startBackCarPreview();
                delayStartRearCameraRecord(5);
            } else {
                if (Utils.isDemoVersion(context)) {
                    VoiceManagerProxy.getInstance().startSpeaking("停止倒车", TTSType.TTS_DO_NOTHING, false);
                }

                exitBackCarPreview();
            }
        } else if (intent.getAction().equals(ACTION_ACC_ON_CHANGED)) {
            boolean fired = intent.getBooleanExtra("fired", false);
            log.debug("ACCReceiver acc_on:{}", fired);
            if (fired) {
                log.debug("[init][{}] 接收到点火通知");
//                CommonLib.getInstance().getContext().startService(new Intent(context, MainService.class));
                CarFireManager.getInstance().fireControl();
            } else {
                log.debug("[init][{}] 收到熄火广播");
//                CommonLib.getInstance().getContext().stopService(new Intent(context, MainService.class));
                CarFireManager.getInstance().flamoutControl();
            }
        }
    }

    private void delayStartRearCameraRecord(int seconds) {
        Observable
                .timer(seconds, TimeUnit.SECONDS, Schedulers.newThread())
                .subscribe(l -> {
                    log.info("延时{}秒开启后置录像", seconds);
                    RearCameraManage.getInstance().startRecord();
                }, throwable -> {
                    log.error("异常", throwable);
                });
    }


    public static boolean proDrivingView(){
        if (LauncherApplication.getContext().getInstance() != null) {
            Fragment fragment = LauncherApplication.getContext().getInstance().getFragment(FragmentConstants.FRAGMENT_DRIVING_RECORD);
            if (fragment != null && fragment.isVisible()) {
                LoggerFactory.getLogger("video.reardrivevideo").info("倒车时已经在行车记录界面");
                if (!RearCameraManage.getInstance().isPreviewIng()){
                    RearCameraManage.getInstance().startPreview();
                }
                DrivingRecordFragment drivingRecordFragment = (DrivingRecordFragment)fragment;
                drivingRecordFragment.delayHideVideoButton(0);
                DrivingRecordFragment.isFrontCameraPreView = false;
                return true;
            }else {
                return false;
            }
        }
        return false;
    }


    public static void startBackCarPreview() {
        if (proDrivingView()) {
            return;
        }

        RearCameraManage.getInstance().stopPreview();

        if (NavigationManager.getInstance(CommonLib.getInstance().getContext()).isNavigatining()
                || ActivitiesManager.getInstance().getTopActivity()instanceof GaodeMapActivity
                || ActivitiesManager.getInstance().getTopActivity()instanceof AddressSearchActivity){
            log.info("正在导航，切换到主活动");
            ActivitiesManager.toMainActivity();
        }

        if (null != LauncherApplication.getContext().getInstance()) {
            LoggerFactory.getLogger("video.reardrivevideo").debug("LauncherApplication.getContext().getInstance() " + LauncherApplication.getContext().getInstance());
            LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_DRIVING_RECORD);
        }
    }

    public static void exitBackCarPreview() {
//        ObservableFactory.getInstance().getCommonObservable().changeToFrontCameraPreview();

        if (null != LauncherApplication.getContext().getInstance()) {
            Fragment fragment = LauncherApplication.getContext().getInstance().getFragment(FragmentConstants.FRAGMENT_DRIVING_RECORD);
            if (fragment != null && !fragment.isVisible()) {
                log.info("停止倒车时没在行车记录界面，不做处理");
                return;
            }
        }

        if (null != LauncherApplication.getContext().getInstance()) {
            log.debug("LauncherApplication.getContext().getInstance() " + LauncherApplication.getContext().getInstance());
            checkBtCallState();
        }
        log.debug("导航状态：{}", NavigationManager.getInstance(CommonLib.getInstance().getContext()).isNavigatining());
        if (NavigationManager.getInstance(CommonLib.getInstance().getContext()).isNavigatining()) {
            NavigationProxy.getInstance().openNavi(NavigationProxy.OPEN_MANUAL);
        }
    }

    /**
     * 倒车结束后判断蓝牙电话状态做界面跳转
     */
    private static void checkBtCallState(){
        String number = "";
        if(null!=FragmentConstants.TEMP_ARGS){
            number = FragmentConstants.TEMP_ARGS.getString(Constants.EXTRA_PHONE_NUMBER);
        }
        log.debug("bluetooth call number " + number + ", btCallState " + BtPhoneUtils.btCallState);
        //判断蓝牙电话状态
        if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_DIALING &&!TextUtils.isEmpty(number)) {
            //拨号中
            LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.BT_OUT_CALL);
        } else if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_INCOMING &&!TextUtils.isEmpty(number)) {
            //来电中
            LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.BT_IN_CALL);
        } else if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ACTIVE &&!TextUtils.isEmpty(number)) {
            //通话中
            LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.BT_CALLING);
        } else if (!FragmentConstants.FRAGMENT_DRIVING_RECORD.equals(MainRecordActivity.lastFragment)) {
            //返回上第一个界面
            LauncherApplication.getContext().getInstance().replaceFragment(MainRecordActivity.lastFragment);
        } else if (!FragmentConstants.FRAGMENT_DRIVING_RECORD.equals(MainRecordActivity.lastSecondFragment)) {
            //返回上第二个界面
            LauncherApplication.getContext().getInstance().replaceFragment(MainRecordActivity.lastSecondFragment);
        } else if (!FragmentConstants.FRAGMENT_DRIVING_RECORD.equals(MainRecordActivity.lastThirdFragment)) {
            //返回上第三个界面
            LauncherApplication.getContext().getInstance().replaceFragment(MainRecordActivity.lastThirdFragment);
        } else {
            //返回主界面
            LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
        }

    }
}
