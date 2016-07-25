package com.dudu.voice.semantic.chain;

import android.app.Activity;
import android.provider.Settings;
import android.view.WindowManager;

import com.dudu.aios.ui.base.VolBrightnessSetting;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.utils.ActivitiesManager;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.voice.semantic.bean.BrightnessBean;
import com.dudu.voice.semantic.bean.SemanticBean;
import com.dudu.voice.semantic.constant.SemanticConstant;
import com.umeng.analytics.MobclickAgent;

/**
 * Created by lxh on 2016-04-20 16:04.
 */
public class BrightnessChain extends SemanticChain {

    public static final String DOWN = "down";
    public static final String UP = "up";
    public static final String MAX = "max";
    public static final String MIN = "min";
    public static final String MAX_CN = "最亮";
    public static final String MIN_CN = "最暗";
    public static final int BRIGHTNESS_STEP = 60;
    public static int staticBrightness = 255;

    private int currentBrightness;

    @Override
    public boolean matchSemantic(String service) {
        return SemanticConstant.SERVICE_BRIGHTNESS.equalsIgnoreCase(service);
    }

    @Override
    public boolean doSemantic(SemanticBean bean) {

        if (bean != null) {

            String action = ((BrightnessBean) bean).getAction();
            if(bean.getText().equals(MAX_CN))
                action = MAX;
            if(bean.getText().equals(MIN_CN))
                action = MIN;
            switch (action) {
                case DOWN:
                case "-":
                    down();
                    break;
                case UP:
                case "+":
                    up();
                    break;
                case MAX:
                    max();
                    break;
                case MIN:
                    min();
                    break;
            }
            synchroBrightness(currentBrightness);
            mVoiceManager.startUnderstanding();
            return true;
        }
        return false;
    }

    private void down() {
        currentBrightness = Settings.System.getInt(ActivitiesManager.getInstance().getTopActivity().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, -1);
        if (currentBrightness >= 17) {
            currentBrightness = currentBrightness - BRIGHTNESS_STEP;
            if (currentBrightness < 17) {
                currentBrightness = 0;
            }
        }
        MobclickAgent.onEvent(mContext, ClickEvent.voice60.getEventId());

    }

    private void up() {
        currentBrightness = Settings.System.getInt(ActivitiesManager.getInstance().getTopActivity().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, -1);
        currentBrightness = currentBrightness + BRIGHTNESS_STEP;
        if (currentBrightness > staticBrightness) {
            currentBrightness = staticBrightness;
        }
        MobclickAgent.onEvent(mContext, ClickEvent.voice59.getEventId());

    }

    private void max() {
        currentBrightness = 255;
    }

    private void min() {
        currentBrightness = 20;
    }

    public void synchroBrightness(int brightness) {
        Activity activity = ActivitiesManager.getInstance().getTopActivity();
        VolBrightnessSetting.setScreenMode(activity, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
        currentBrightness = Settings.System.getInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, -1);
        if (LauncherApplication.getContext().getInstance() != null) {
            LauncherApplication.getContext().getInstance().showBrightnessView(currentBrightness);
        }

//        staticBrightness = currentBrightness;

        WindowManager.LayoutParams wl = activity.getWindow().getAttributes();
        float tmpFloat = (float) brightness / 255;
        if (tmpFloat > 0 && tmpFloat <= 1) {
            wl.screenBrightness = tmpFloat;
        }
        activity.getWindow().setAttributes(wl);
    }
}
