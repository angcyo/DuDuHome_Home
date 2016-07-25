package com.dudu.commonlib.utils;

import ch.qos.logback.core.android.SystemPropertiesProxy;

/**
 * Created by lxh on 2016-05-15 15:18.
 */
public class ModelUtil {
    public static final String MODEL_D02 = "d02";

    public static final String MODEL_D03 = "d03";

    public static String getModel() {
        //ROM中不设置persist.sys.model的值,该值只留做后门
        //已ro.build.display.id开头的型号判断
        String debug_model = SystemPropertiesProxy.getInstance().get("persist.sys.model", "");
        if (debug_model.isEmpty()) {
            String sys_model = SystemPropertiesProxy.getInstance().get("ro.build.display.id", "");
            if (!sys_model.isEmpty()) {
                if (sys_model.startsWith("D02")) return MODEL_D02;
            }
        } else {
            return debug_model;
        }
        return MODEL_D03;
    }


    public static boolean needVip() {
        if (getModel().equals(MODEL_D02))
            return true;
        return false;
    }

}
