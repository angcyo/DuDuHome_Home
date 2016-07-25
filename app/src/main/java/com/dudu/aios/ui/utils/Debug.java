package com.dudu.aios.ui.utils;

import com.dudu.android.launcher.BuildConfig;
import com.dudu.drivevideo.rearcamera.BlurControl;

/**
 * Created by robi on 2016-07-07 15:22.
 */
public class Debug implements BlurControl.IBlurListener {

    static {
        BlurControl.instance().addBlurListener(new Debug());
    }

    public static void debug(String log) {
        if (BuildConfig.DEBUG) {
//            DebugWindow.instance().addText(log);
        }
    }

    @Override
    public void onBlurChange(boolean isBlur) {
        debug("监听到模糊状态改变为:" + isBlur);
    }
}
