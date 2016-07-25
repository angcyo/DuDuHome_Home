package com.dudu.monitor.tirepressure;

import com.dudu.commonlib.CommonLib;

/**
 * Created by dudusmart on 16/7/14.
 */
public class TirePairSp {
    public static final String LF = "lf";
    public static final String LB = "lb";
    public static final String RF = "rf";
    public static final String RB = "rb";

    //判断对应轮胎是否对码成功过
    public static boolean isTpmsPair(final int position) {
        boolean result = false;
        if (position == TirePressureManager.R_1) {
            result = TirePairSp.isRFPair();
        } else if (position == TirePressureManager.L_1) {
            result = TirePairSp.isLFPair();
        } else if (position == TirePressureManager.R_2) {
            result = TirePairSp.isRBPair();
        } else if (position == TirePressureManager.L_2) {
            result = TirePairSp.isLBPair();
        }
        return result;
    }

    //设置对应轮胎对码标识
    public static void setTpmsPair(final int position, final boolean pair) {
        if (position == TirePressureManager.R_1) {
            TirePairSp.setRFPair(pair);
        } else if (position == TirePressureManager.L_1) {
            TirePairSp.setLFPair(pair);
        } else if (position == TirePressureManager.R_2) {
            TirePairSp.setRBPair(pair);
        } else if (position == TirePressureManager.L_2) {
            TirePairSp.setLBPair(pair);
        }
    }


    /**
     * 左前轮胎是否对码成功过
     */
    public static boolean isLFPair() {
        return SharedPreferencesUtils.getBooleanValue(CommonLib.getInstance().getContext(), LF, false);
    }

    public static void setLFPair(boolean pair) {
        SharedPreferencesUtils.putBooleanValue(CommonLib.getInstance().getContext(), LF, pair);
    }

    /**
     * 左后轮胎是否对码成功过
     */
    public static boolean isLBPair() {
        return SharedPreferencesUtils.getBooleanValue(CommonLib.getInstance().getContext(), LB, false);
    }

    public static void setLBPair(boolean pair) {
        SharedPreferencesUtils.putBooleanValue(CommonLib.getInstance().getContext(), LB, pair);
    }

    /**
     * 右前轮胎是否对码成功过
     */
    public static boolean isRFPair() {
        return SharedPreferencesUtils.getBooleanValue(CommonLib.getInstance().getContext(), RF, false);
    }

    public static void setRFPair(boolean pair) {
        SharedPreferencesUtils.putBooleanValue(CommonLib.getInstance().getContext(), RF, pair);
    }

    /**
     * 右后轮胎是否对码成功过
     */
    public static boolean isRBPair() {
        return SharedPreferencesUtils.getBooleanValue(CommonLib.getInstance().getContext(), RB, false);
    }

    public static void setRBPair(boolean pair) {
        SharedPreferencesUtils.putBooleanValue(CommonLib.getInstance().getContext(), RB, pair);
    }
}
