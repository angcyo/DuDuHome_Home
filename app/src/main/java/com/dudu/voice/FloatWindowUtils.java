package com.dudu.voice;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.AdapterView.OnItemClickListener;

import com.dudu.aios.ui.activity.MainRecordActivity;
import com.dudu.aios.ui.activity.weather.WeatherActivity;
import com.dudu.aios.ui.base.BaseFragmentManagerActivity;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.aios.ui.voice.VoiceEvent;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.model.WindowMessageEntity;
import com.dudu.android.launcher.utils.ActivitiesManager;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.voice.semantic.constant.SceneType;
import com.dudu.voice.semantic.engine.SemanticEngine;
import com.dudu.voice.window.BaseWindowManager;
import com.dudu.voice.window.BlueWindowManager;
import com.dudu.voice.window.FloatWindowManager;
import com.dudu.voice.window.MessageType;

import de.greenrobot.event.EventBus;

public class FloatWindowUtils {

    private static final int FLOAT_SHOW_MESSAGE = 0;
    private static final int FLOAT_SHOW_ADDRESS = 1;
    private static final int FLOAT_SHOW_STRATEGY = 2;
    private static final int FLOAT_REMOVE_WINDOW = 3;
    private static final int FLOAT_NEXT_PAGE = 4;
    private static final int FLOAT_PREVIOUS_PAGE = 5;
    private static final int FLOAT_CHOOSE_PAGE = 6;
    private static final int FLOAT_VOLUME_CHANGED = 7;
    private static final int FLOAT_REMOVE_WINDOW_WITH_BLUR = 8;
    private static FloatWindowUtils floatWindowUtils;
    private FloatWindowManager sManager;
    private FloatWindowHandler sHandler = new FloatWindowHandler();
    public static boolean needShowMessage = true;

    public static FloatWindowUtils getInstance() {
        if (floatWindowUtils == null) {
            floatWindowUtils = new FloatWindowUtils();
        }
        return floatWindowUtils;
    }

    public FloatWindowUtils() {
        sManager = new BlueWindowManager();
    }

    public void showMessage(String message, MessageType type) {

        if (ActivitiesManager.getInstance().getTopActivity() instanceof MainRecordActivity) {
            BaseFragmentManagerActivity baseFragmentManagerActivity = (BaseFragmentManagerActivity) ActivitiesManager.getInstance().getTopActivity();
            String currentStackTag = baseFragmentManagerActivity.getCurrentStackTag();
            if (FragmentConstants.CAR_CHECKING.equals(currentStackTag)
                    || FragmentConstants.REPAIR_FAULT_CODE_FRAGMENT.equals(currentStackTag)
                    || FragmentConstants.VEHICLE_ANIMATION_FRAGMENT.equals(currentStackTag)) {
                return;
            }
        }
        if (!needShowMessage) {
            return;
        }
        sHandler.sendMessage(sHandler.obtainMessage(FLOAT_SHOW_MESSAGE,
                new WindowMessageEntity(message, type)));

    }

    public void showAddress(OnItemClickListener listener) {
        sHandler.sendMessage(sHandler.obtainMessage(FLOAT_SHOW_ADDRESS, listener));
    }

    public void showStrategy(OnItemClickListener listener) {
        sHandler.sendMessage(sHandler.obtainMessage(FLOAT_SHOW_STRATEGY, listener));
    }

    public void removeFloatWindow() {
        sHandler.sendMessage(sHandler.obtainMessage(FLOAT_REMOVE_WINDOW));
    }

    public void onNextPage() {
        sHandler.sendMessage(sHandler.obtainMessage(FLOAT_NEXT_PAGE));
    }

    public void onPreviousPage() {
        sHandler.sendMessage(sHandler.obtainMessage(FLOAT_PREVIOUS_PAGE));
    }

    public void onChoosePage(int page) {
        sHandler.sendMessage(sHandler.obtainMessage(FLOAT_CHOOSE_PAGE, page, 0));
    }

    public void onVolumeChanged(int volume) {
        sHandler.sendMessage(sHandler.obtainMessage(FLOAT_VOLUME_CHANGED, volume, 0));
    }

    public void showAnimWindow() {

        if (ActivitiesManager.getInstance().getTopActivity() instanceof MainRecordActivity) {

            BaseFragmentManagerActivity baseFragmentManagerActivity = (BaseFragmentManagerActivity) ActivitiesManager.getInstance().getTopActivity();
            String currentStackTag = baseFragmentManagerActivity.getCurrentStackTag();
            if (!LauncherApplication.getContext().isReceivingOrder()
                    && (FragmentConstants.CAR_CHECKING.equals(currentStackTag)
                    || FragmentConstants.REPAIR_FAULT_CODE_FRAGMENT.equals(currentStackTag)
                    || FragmentConstants.VEHICLE_ANIMATION_FRAGMENT.equals(currentStackTag))) {
                SemanticEngine.getProcessor().switchSemanticType(SceneType.CAR_CHECKING);
                return;
            }
            showAnim();
        } else {
            if (ActivitiesManager.getInstance().getTopActivity() instanceof WeatherActivity) {
                ActivitiesManager.getInstance().closeTargetActivity(WeatherActivity.class);
                showAnim();
                return;
            }
            sHandler.sendMessage(sHandler.obtainMessage(FLOAT_SHOW_MESSAGE,
                    new WindowMessageEntity(Constants.WAKEUP_WORDS, MessageType.MESSAGE_INPUT)));
        }

    }

    private void showAnim() {
        EventBus.getDefault().post(VoiceEvent.SHOW_ANIM);
        sHandler.postDelayed(() -> ((BlueWindowManager) sManager).showAnimWindow(), 200);
    }

    public void removeWithBlur() {
        sHandler.sendEmptyMessage(FLOAT_REMOVE_WINDOW_WITH_BLUR);
    }

    public boolean isShowWindow() {
        return ((BaseWindowManager) sManager).isShowFloatWindow();
    }

    private class FloatWindowHandler extends Handler {

        public FloatWindowHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case FLOAT_SHOW_MESSAGE:
                    sManager.showMessage((WindowMessageEntity) msg.obj);
                    break;
                case FLOAT_SHOW_ADDRESS:
                    sManager.setItemClickListener((OnItemClickListener) msg.obj);
                    sManager.showAddress();
                    break;
                case FLOAT_SHOW_STRATEGY:
                    sManager.setItemClickListener((OnItemClickListener) msg.obj);
                    sManager.showStrategy();
                    break;
                case FLOAT_REMOVE_WINDOW:
                    sManager.removeFloatWindow();
                    break;
                case FLOAT_NEXT_PAGE:
                    sManager.onNextPage();
                    break;
                case FLOAT_PREVIOUS_PAGE:
                    sManager.onPreviousPage();
                    break;
                case FLOAT_CHOOSE_PAGE:
                    sManager.onChoosePage(msg.arg1);
                    break;
                case FLOAT_VOLUME_CHANGED:
                    sManager.onVolumeChanged(msg.arg1);
                    break;


            }
        }

    }

}
