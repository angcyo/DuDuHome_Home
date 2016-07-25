package com.dudu.aios.ui.fragment.tire;

import android.util.Log;
import android.view.View;

import com.dudu.init.CarFireManager;
import com.dudu.monitor.tirepressure.TirePressureManage;
import com.dudu.monitor.tirepressure.TirePressureManager;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.workflow.driving.CarStatusUtils;
import com.dudu.workflow.tpms.TPMSFlow;
import com.dudu.workflow.tpms.TPMSInfo;
import com.dudu.workflow.tpms.TPMSParamBean;

import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by robi on 2016-06-28 14:59.
 */
public class TireValueHelper {
    public static Logger log = TirePressureManager.log;

    private static boolean isFire;
    private Subscription mMTpmsPairSub;
    private Subscription obdWorkStartSubscription;
    private Subscription tpmsSub = null;
    private Subscription tpmsPairSub = null;

    public static TPMSInfo.POSITION getTPMSInfoPOSITION(int position) {
        switch (position) {
            case TirePressureManager.R_1:
                return TPMSInfo.POSITION.RIGHT_FRONT;
            case TirePressureManager.R_2:
                return TPMSInfo.POSITION.LEFT_FRONT;
            case TirePressureManager.L_1:
                return TPMSInfo.POSITION.RIGHT_BACK;
            case TirePressureManager.L_2:
                return TPMSInfo.POSITION.LEFT_BACK;
        }
        return TPMSInfo.POSITION.UNKNOW;
    }

    public static boolean isTPMSParamRight(final TPMSParamBean bean, int celsius,
                                           float fBarometerHigh, float fBarometerLow,
                                           float bBarometerHigh, float bBarometerLow) {
        if (bean.getTemperature() == celsius &&
                bean.getFrontPressureHigh() == fBarometerHigh && bean.getFrontPressureLow() == fBarometerLow &&
                bean.getBackPressureHigh() == bBarometerHigh && bean.getBackPressureLow() == bBarometerLow) {
            return true;
        }
        return false;
    }

    /**
     * 保存点火状态
     */
    public static void setIsFire(boolean isFire) {
        TireValueHelper.isFire = isFire;
    }


    public String getMaxTempString() {
        return String.format("%2d°C", TirePressureManager.maxTemp);
    }

    public String getFrontTpmsRange() {
        return getTpmsRange(TirePressureManager.frontMin, TirePressureManager.frontMax);
    }

    public String getBackTpmsRange() {
        return getTpmsRange(TirePressureManager.backMin, TirePressureManager.backMax);
    }

    private String getTpmsRange(float min, float max) {
        return String.format("%.1f-%.1fBar", min, max);
    }

    public String getTpmsTextView(float pressure) {
        String p = String.valueOf(pressure);
        StringBuilder builder = new StringBuilder("胎压: ");
        builder.append(p.substring(0, p.indexOf(".") + 2));
        builder.append("Bar");
        return builder.toString();
//        return String.format("胎压: %.1fBar", pressure);
    }

    public String getTempTextView(int temp) {
        return String.format("温度: %2d°C", temp);
    }

    /**
     * 右前轮对码
     */
    public void tpms6601RF(Runnable timeoutRunnable, Runnable pairOkRunnable) {
        pairStart(TirePressureManager.R_1, timeoutRunnable, pairOkRunnable);
        TPMSFlow.TPMSPairStart(TPMSInfo.POSITION.RIGHT_FRONT);
    }

    /**
     * 左前轮对码
     */
    public void tpms6602LF(Runnable timeoutRunnable, Runnable pairOkRunnable) {
        pairStart(TirePressureManager.L_1, timeoutRunnable, pairOkRunnable);
        TPMSFlow.TPMSPairStart(TPMSInfo.POSITION.LEFT_FRONT);
    }

    /**
     * 右后轮对码
     */
    public void tpms6603RB(Runnable timeoutRunnable, Runnable pairOkRunnable) {
        pairStart(TirePressureManager.R_2, timeoutRunnable, pairOkRunnable);
        TPMSFlow.TPMSPairStart(TPMSInfo.POSITION.RIGHT_BACK);
    }

    /**
     * 左后轮对码
     */
    public void tpms6604LB(Runnable timeoutRunnable, Runnable pairOkRunnable) {
        pairStart(TirePressureManager.L_2, timeoutRunnable, pairOkRunnable);
        TPMSFlow.TPMSPairStart(TPMSInfo.POSITION.LEFT_BACK);
    }

    /**
     * 是否可以对码, 一次只允许一个轮胎对码
     */
    public boolean canPairStart() {
        return mMTpmsPairSub == null;
    }

    /**
     * 车辆是否点火
     */
    public boolean isCarFire() {
        return isFire;
    }

    public void checkFireStart(Runnable fireRunnable) {
        if (obdWorkStartSubscription != null) {
            obdWorkStartSubscription.unsubscribe();
        }
        obdWorkStartSubscription = CarStatusUtils.isFired()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fired -> {
                    isFire = fired;
                    if (!fired) {
//                        VoiceManagerProxy.getInstance().startSpeaking(
//                                CommonLib.getInstance().getContext().getString(R.string.fire_before_tirecheck), TTSType.TTS_DO_NOTHING, false);
                        //没有点火,打开胎压串口读取数据
                        CarFireManager.getInstance().initTpms();
                    }
                    fireRunnable.run();
                }, throwable -> log.error("checkFireStart", throwable));
    }

    /**
     * 对码需要,点火
     */
    public void pairStart(int pos, Runnable timeoutRunnable, Runnable pairOkRunnable) {
        if (mMTpmsPairSub != null && !mMTpmsPairSub.isUnsubscribed()) {
            return;
        }

        if (TPMSFlow.TPMSWarnInfoStream() == null || TPMSFlow.TPMSPairStream() == null) {
            return;
        }

//        final Observable<TPMSInfo> tpmsInfoObservable = TPMSFlow.TPMSPairStream();
        final Observable<TPMSInfo> tpmsInfoObservable = TPMSFlow.TPMSWarnInfoStream();
        if (tpmsInfoObservable != null) {
//            mMTpmsPairSub = tpmsInfoObservable
//                    .timeout(180, TimeUnit.SECONDS)
//                    .filter(info -> {
//                        boolean result = (TPMSInfo.POSITION.valueOf(info.position) == getTPMSInfoPOSITION(pos));
//                        log.debug("对码轮胎: 位置:{} 请求:{} ", info.position, pos);
//                        return result;
//                    })
//                    .subscribeOn(Schedulers.newThread())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(tpmsinfo -> {
//                        log.debug("对码成功: TPMS pair: {}", tpmsinfo.toString());
//                        mMTpmsPairSub = null;
//                    }, throwable -> {
//                        log.error("对码超时: mTpmsPairSub: {}", throwable);
//                        mMTpmsPairSub = null;
//                    });

            log.debug("准备对码轮胎位置:{}", pos);
            mMTpmsPairSub = tpmsInfoObservable.subscribeOn(Schedulers.newThread())
                    .filter(info -> pos == info.getPosition())
                    .timeout(180, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(tpmsWarnInfo -> {
                        log.debug("对码结果-->轮胎位置:{} 信息:{}", pos, tpmsWarnInfo);
                        pairOkRunnable.run();
                        TirePressureManage.getInstance().saveTirePressureData(tpmsWarnInfo);
                    }, throwable -> {
                        if (throwable instanceof TimeoutException) {
                            log.error("对码结果-->轮胎位置:{} 超时:{}", pos, throwable);
                            timeoutRunnable.run();
                            VoiceManagerProxy.getInstance().startSpeaking(TirePressureManager.getTireChinese(pos) + "对码超时,请重新对码.", TTSType.TTS_DO_NOTHING, false);
                        } else {
                            log.error("对码结果-->轮胎位置:{} 出错:{}", pos, throwable);
                            timeoutRunnable.run();
                            VoiceManagerProxy.getInstance().startSpeaking(TirePressureManager.getTireChinese(pos) + "对码出错,请重试.", TTSType.TTS_DO_NOTHING, false);
                        }
                    });

            final Observable<TPMSInfo> tpmsInfoObservable1 = TPMSFlow.TPMSPairStream();
            if (tpmsInfoObservable1 != null) {
                tpmsPairSub = tpmsInfoObservable1
                        .filter(info -> pos == info.getPosition())
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(pair -> {
                            log.debug("对码成功:{}", pair);
                            pairOkRunnable.run();
                            TirePressureManage.getInstance().saveTirePressureData(pair);
                        }, throwable -> {
                            log.error("对码失败:{}", throwable);
                            timeoutRunnable.run();
                        });
            }
            VoiceManagerProxy.getInstance().startSpeaking(TirePressureManager.getTireChinese(pos) + "开始对码,请在3分钟之内完成", TTSType.TTS_DO_NOTHING, false);
        } else {
            log.debug("tpmsInfoObservable is Null 终止对码.");
        }
    }

    /**
     * 取消对码订阅
     */
    public void pairCancel() {
        if (mMTpmsPairSub != null) {
            mMTpmsPairSub.unsubscribe();
            mMTpmsPairSub = null;
        }

        if (tpmsPairSub != null) {
            tpmsPairSub.unsubscribe();
            tpmsPairSub = null;
        }
    }

    public void tpmsSub(View view) {
        if (TPMSFlow.TPMSWarnInfoStream() == null || TPMSFlow.TPMSPairStream() == null) {
            return;
        }

        tpmsSub = TPMSFlow.TPMSWarnInfoStream()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tpmsWarnInfo -> {
                    Log.d("TPMS", "info: " + tpmsWarnInfo);
                }, throwable -> Log.e("DebugActivity", "tpmsSub: ", throwable));

        tpmsPairSub = TPMSFlow.TPMSPairStream()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> {
                    Log.d("TPMS", "pair: " + pair);
                }, throwable -> Log.e("DebugActivity", "tpmsSub: ", throwable));

    }
}
