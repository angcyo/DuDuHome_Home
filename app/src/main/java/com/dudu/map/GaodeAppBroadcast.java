package com.dudu.map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.dudu.aios.ui.map.GaodeMapActivity;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.utils.BtPhoneUtils;
import com.dudu.android.launcher.utils.TimeUtils;
import com.dudu.navi.NavigationManager;
import com.dudu.navi.vauleObject.NavigationType;
import com.dudu.voice.FloatWindowUtils;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.TTSType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by lxh on 2016/3/1.
 */
public class GaodeAppBroadcast extends BroadcastReceiver {


    private final static String SEND_ACTION = "com.autonavi.minimap.carmode.send";
    private final static String SEND_BUSINESS_ACTION = "send_business_action";
    private final static String SEND_BUSINESS_DATA = "send_business_data";
    public final static String SEND_LAUNCH_APP = "LAUNCH_APP"; // 程序启动
    public final static String SEND_EXIT_APP = "EXIT_APP"; // 程序退出
    public final static String SEND_OPEN_NAVI = "OPEN_NAVI"; //打开导航
    public final static String SEND_CLOSE_NAVI = "CLOSE_NAVI"; // 关闭导航
    public final static String SEND_NAVI_END = "NAVI_END"; //导航到终点正常结束
    public final static String SEND_PATH_FAIL = "PATH_FAIL"; // 路径规划失败
    public final static String SEND_APP_FOREGROUND = "APP_FOREGROUND"; // 程序切到前台
    public final static String SEND_APP_BACKGROUND = "APP_BACKGROUND"; // 程序隐藏到后台
    public final static String SEND_NAVI_INFO = "NAVI_INFO"; // 导航信息
    // 显示路口放大图
    public final static String SEND_DISP_ROAD_ENLARGE_PIC = "DIS_ROAD_ENLARGE_PIC";
    // 隐藏路口放大图
    public final static String SEND_HIDE_ROAD_ENLARGE_PIC = "HIDE_ROAD_ENLARGE_PIC";
    public final static String SEND_NAVI_STR = "NAVI_STR"; // 导航文字信息

    private boolean isForeground = false;
    private Logger logger = LoggerFactory.getLogger("naviInfo");

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (SEND_ACTION.equals(action)) {
            String businessAct = intent.getStringExtra(SEND_BUSINESS_ACTION);
            switch (businessAct) {
                case SEND_LAUNCH_APP:
                    handleOpenApp();
                    break;
                case SEND_EXIT_APP:
                    handleExitApp(context);
                    break;
                case SEND_OPEN_NAVI:
                    handleOpenNavi(context);
                    break;
                case SEND_CLOSE_NAVI:
                    handleExitNavi();
                    break;
                case SEND_PATH_FAIL:
                    handleAppPathFail();
                    break;
                case SEND_APP_FOREGROUND:
                    handleAppForeground();
                    break;
                case SEND_APP_BACKGROUND:
                    handleAppBackground();
                    break;
                case SEND_NAVI_END:
                    handleNaviEnd(context);
                    break;
                case SEND_NAVI_STR:
                    handleNaviStr(intent, context);
                    break;
            }

        }
    }

    private void handleOpenApp() {
        logger.debug("onReceive  程序启动");
        LauncherApplication.getContext().setReceivingOrder(true);
    }

    private void handleExitApp(Context context) {
        logger.debug("onReceive  程序退出");
        isForeground = true;

    }

    private void handleExitNavi() {
        if (!NavigationProxy.getInstance().isStartNewNavi()) {
            logger.debug("不是开始新的导航，退出高德APP");
            GaodeMapAppUtil.closeNaviVoice();
            GaodeMapAppUtil.exitGapdeApp();
        }
        NavigationProxy.getInstance().setStartNewNavi(false);
    }

    private void handleOpenNavi(Context context) {
        logger.debug("onReceive  打开导航");
        GaodeMapAppUtil.closeNaviVoice();
        NavigationManager.getInstance(context).setIsNavigatining(true);

        int time = Integer.parseInt(TimeUtils.format(TimeUtils.format6));

        if (time > 18 || time < 5) {
            GaodeMapAppUtil.startNaviNightMode();
        } else {
            GaodeMapAppUtil.startNaviDayMode();
        }
    }

    private void handleNaviStr(Intent intent, Context context) {
        String strInfo = intent.getStringExtra(SEND_BUSINESS_DATA);

        try {

            if (!TextUtils.isEmpty(strInfo) && !FloatWindowUtils.getInstance().isShowWindow()
                    && BtPhoneUtils.btCallState != BtPhoneUtils.CALL_STATE_ACTIVE
                    && !NavigationProxy.getInstance().isShowList()
                    && NavigationManager.getInstance(context).isNavigatining()) {

                if (strInfo.contains("(")) {
                    strInfo = strInfo.replace("(", "");
                }
                if (strInfo.contains(")")) {
                    strInfo = strInfo.replace(")", "");
                }
                logger.debug("onReceive  导航播报文字 {}", strInfo);
                VoiceManagerProxy.getInstance().stopSpeaking();
                VoiceManagerProxy.getInstance().clearMisUnderstandCount();
                VoiceManagerProxy.getInstance().startSpeaking(strInfo, TTSType.TTS_DO_NOTHING, false);
            }
        } catch (Exception e) {

        }
    }

    private void handleNaviEnd(Context context) {
        logger.debug("onReceive  导航结束");
        Observable.timer(4, TimeUnit.SECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    if (isForeground) {

                        GaodeMapAppUtil.exitGapdeApp();
                        Intent i = new Intent(context, GaodeMapActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(i);
                    }
                }, throwable -> logger.error("SEND_NAVI_END", throwable));
    }

    private void handleAppForeground() {
        logger.debug("onReceive  程序切换到前台");
        LauncherApplication.getContext().setReceivingOrder(true);
        isForeground = true;
    }

    private void handleAppBackground() {
        logger.debug("onReceive  程序切换到后台");
        isForeground = false;
        LauncherApplication.getContext().setReceivingOrder(false);
    }

    private void handleAppPathFail() {
        EventBus.getDefault().post(NavigationType.CALCULATEERROR);

    }
}
