package com.dudu.aios.ui.fragment.tire;

import com.dudu.monitor.tirepressure.TirePairSp;
import com.dudu.monitor.tirepressure.TirePressureManager;
import com.dudu.workflow.tpms.TPMSInfo;

/**
 * Created by robi on 2016-06-29 18:03.
 * <p>
 * <p>
 * <p>
 * 设置轮胎信息
 * mTPMSInfo1;//右前轮   轮胎信息
 * mTPMSInfo2;//  左前轮 轮胎信息
 * mTPMSInfo3;//右后轮   轮胎信息
 * mTPMSInfo4;//  左后轮 轮胎信息
 */
public class TireExceptionHelper {

    /**
     * 左前轮异常信息
     */
    TireException mL1TireException;

    /**
     * 左后轮异常信息
     */
    TireException mL2TireException;
    /**
     * 右前轮异常信息
     */
    TireException mR1TireException;
    /**
     * 右后轮异常信息
     */
    TireException mR2TireException;

    /**
     * 最后一次通知的时间
     */
//    private long lastShowTime = 0;
    public TireExceptionHelper() {
        mL1TireException = new TireException();
        mL2TireException = new TireException();
        mR1TireException = new TireException();
        mR2TireException = new TireException();
    }

//    public static TireExceptionHelper instance() {
//        return Holder.helper;
//    }

    /**
     * 比对之前的异常信息, 判断异常是否更改
     *
     * @return 返回true, 表示异常改变了
     */
    private static boolean isExceptionChange(TireException exception, final TPMSInfo info) {
        final boolean gasExp = TirePressureManager.isGasLeaksException(info.getGasLeaks());
//        final boolean preExp = (info.getPosition() == TirePressureManager.L_1 || info.getPosition() == TirePressureManager.R_1) ?
//                TirePressureManager.isFrontTpmsException(info.getPressure()) :
//                TirePressureManager.isBackTpmsException(info.getPressure());
//        final boolean tempExp = TirePressureManager.isTempException(info.getTemperature());
        final boolean preExp = TirePressureManager.isTpmsException(info);
        final boolean tempExp = TirePressureManager.isTempException(info);

        boolean result = false;

        /*漏气异常是否改变*/
        if (gasExp && gasExp != exception.isGasLeaksException) {
            result = true;
        }

        /*胎压异常是否改变*/
        if (preExp && preExp != exception.isPressureException) {
            result = true;
        }

        /*温度异常是否改变*/
        if (tempExp && tempExp != exception.isTempException) {
            result = true;
        }

        exception.isGasLeaksException = gasExp;
        exception.isPressureException = preExp;
        exception.isTempException = tempExp;

        return result;
    }

    /**
     * 异常检测
     *
     * @return 返回true, 表示需要进行异常提示(弹出界面, 播放语音).
     */
    public boolean setTPMSInfo(final TPMSInfo info) {
        boolean result = false;
        if (info == null || info.noData || !TirePairSp.isTpmsPair(info.getPosition()) /*轮胎对码过*/) {
            result = false;
        } else {

//            //如果发现异常, 并且没有显示过 , 则不考虑异常是否改变过
//            if (TirePressureManager.isTpmsInfoException(info) && !isExceptionShow) {
//                result = true;
//            }

            //如果异常改变了
            final boolean change = handleTpmsInfo(info);
//            if (change && !isExceptionShow) {
            if (change) {
                result = true;
            }
        }

//        if (result) {
//            //几秒之内,只提醒一次
//            final long nowTime = System.currentTimeMillis();
//            if (nowTime - lastShowTime > 3 * 1000) {
//                result = true;
//            } else {
//                result = false;
//            }
//            lastShowTime = nowTime;
//        }

        return result;
    }

    /**
     * 检查胎压信息,分配给对应轮胎
     *
     * @return 返回true, 表示4个轮胎中有异常改变
     */
    private boolean handleTpmsInfo(final TPMSInfo info) {
        boolean result = false;
        switch (info.getPosition()) {
            case TirePressureManager.R_1:
                result = isExceptionChange(mR1TireException, info);
                break;
            case TirePressureManager.R_2:
                result = isExceptionChange(mR2TireException, info);
                break;
            case TirePressureManager.L_1:
                result = isExceptionChange(mL1TireException, info);
                break;
            case TirePressureManager.L_2:
                result = isExceptionChange(mL2TireException, info);
                break;
        }

        return result;
    }

    /**
     * 清理异常标识
     */
    public void cleanTpmsException(int position) {
        switch (position) {
            case TirePressureManager.R_1:
                mR1TireException.isGasLeaksException = false;
                mR1TireException.isPressureException = false;
                mR1TireException.isTempException = false;
                break;
            case TirePressureManager.R_2:
                mR2TireException.isGasLeaksException = false;
                mR2TireException.isPressureException = false;
                mR2TireException.isTempException = false;
                break;
            case TirePressureManager.L_1:
                mL1TireException.isGasLeaksException = false;
                mL1TireException.isPressureException = false;
                mL1TireException.isTempException = false;
                break;
            case TirePressureManager.L_2:
                mL2TireException.isGasLeaksException = false;
                mL2TireException.isPressureException = false;
                mL2TireException.isTempException = false;
                break;
        }
    }

//    static class Holder {
//        static final TireExceptionHelper helper = new TireExceptionHelper();
//    }

    class TireException {

        /**
         * 是否是急漏气异常
         */
        public boolean isGasLeaksException = false;

        /**
         * 是否是温度异常
         */
        public boolean isTempException = false;

        /**
         * 是否是胎压异常
         */
        public boolean isPressureException = false;

    }
}
