package com.dudu.monitor.tirepressure;

import com.dudu.commonlib.utils.ThreadExecutor;
import com.dudu.monitor.event.TireExceptionEvent;
import com.dudu.persistence.realmmodel.tirepressure.TirePressureDataRealm;
import com.dudu.workflow.tpms.TPMSInfo;
import com.dudu.workflow.tpms.TirePressureData;
import com.dudu.workflow.tpms.TpmsDataCallBack;
import com.dudu.workflow.tpms.TpmsDatasFlow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * Created by robi on 2016-06-16 15:07.
 */
public class TirePressureManager {
    public static final int R_1 = 1;//右 上
    public static final int L_1 = 2;//左 上
    public static final int R_2 = 3;//右 下
    public static final int L_2 = 4;//左 下 轮胎
    public static Logger log = LoggerFactory.getLogger("monitor.TirePressure");
    public static int maxTemp = 70;//最高温度
    public static float frontMin = 2.0f;
    public static float frontMax = 3.0f;
    public static float backMin = 2.0f;
    public static float backMax = 3.0f;
    private TPMSInfo mTPMSInfo1;//右前轮   轮胎信息
    private TPMSInfo mTPMSInfo2;//  左前轮 轮胎信息
    private TPMSInfo mTPMSInfo3;//右后轮   轮胎信息
    private TPMSInfo mTPMSInfo4;//  左后轮 轮胎信息
    private Set<ITireExceptionListener> mTireExceptionListenerList;
    private Set<ITireDataListener> mTireDataListenerList;

    private TirePressureManager() {
        mTireDataListenerList = new HashSet<>();
        mTireExceptionListenerList = new HashSet<>();
    }

    /**
     * 判断轮胎信息是否有异常
     */
    public static boolean isTpmsInfoException(TPMSInfo info) {
        boolean result = false;
        if (!isTpmsEmpty(info) && !info.noData) {
//            result = (isTempException(info.getTemperature()) ||
//                    isFrontTpmsException(info.getPressure()) || isBackTpmsException(info.getPressure())
//                    || isGasLeaksException(info.gasLeaks)
            result = (isTempException(info) ||
                    isTpmsException(info) || isGasLeaksException(info.gasLeaks)
            );
        }
        return result;
    }

    public static String getTireChinese(int position) {
        String msg = "未知轮胎";
        switch (position) {
            case TirePressureManager.R_1:
                msg = "右前轮胎";
                break;
            case TirePressureManager.R_2:
                msg = "右后轮胎";
                break;
            case TirePressureManager.L_1:
                msg = "左前轮胎";
                break;
            case TirePressureManager.L_2:
                msg = "左后轮胎";
                break;
        }
        return msg;
    }

    public static boolean isTpmsEmpty(TPMSInfo info) {
        if (info == null) {
            return true;
        }

        return info.temperature == 0 && info.pressure == 0;
    }

    /**
     * 判断温度是否异常
     */
    public static boolean isTempException(int temp) {
        return temp > maxTemp;
    }

    public static boolean isTempException(final TPMSInfo info) {
        return info.isTemperatureHigh();
    }

    public static boolean isTpmsException(final TPMSInfo info) {
        return info.barometerHigh || info.barometerLow;
    }

    /**
     * 判断前轮胎压是否异常
     */
    public static boolean isFrontTpmsException(float pressure) {
        return pressure < frontMin || pressure > frontMax;
    }

    /**
     * 是否是急漏气异常
     */
    public static boolean isGasLeaksException(int gasLeaks) {
        return gasLeaks == 1;
    }

    /**
     * 判断后轮胎压是否异常
     */
    public static boolean isBackTpmsException(float pressure) {
        return pressure < backMin || pressure > backMax;
    }

    public static TirePressureManager instance() {
        return Holder.sTirePressureManager;
    }

    public static TirePressureDataRealm createTirePressureDataRealm(TPMSInfo tpmsWarnInfo) {
        TirePressureDataRealm tirePressureDataRealm = new TirePressureDataRealm();
        tirePressureDataRealm.setPostion(tpmsWarnInfo.getPosition());
        tirePressureDataRealm.setSensorID(tpmsWarnInfo.getSensorID());
        tirePressureDataRealm.setPressure(tpmsWarnInfo.getPressure());
        tirePressureDataRealm.setTemperature(tpmsWarnInfo.getTemperature());
        tirePressureDataRealm.setGasLeaks(tpmsWarnInfo.getGasLeaks());
        tirePressureDataRealm.setBattery(tpmsWarnInfo.isBattery());
        tirePressureDataRealm.setNoData(tpmsWarnInfo.isNoData());
        tirePressureDataRealm.setBarometerHigh(tpmsWarnInfo.isBarometerHigh());
        tirePressureDataRealm.setBarometerLow(tpmsWarnInfo.isBarometerLow());
        tirePressureDataRealm.setTemperatureHigh(tpmsWarnInfo.isTemperatureHigh());
        return tirePressureDataRealm;
    }

    public static TPMSInfo createTPMSInfoWidthRealm(TirePressureDataRealm dataRealm) {
        TPMSInfo info = new TPMSInfo();
        info.position = dataRealm.getPostion();
        info.sensorID = dataRealm.getSensorID();
        info.pressure = dataRealm.getPressure();
        info.temperature = dataRealm.getTemperature();
        info.gasLeaks = dataRealm.getGasLeaks();
        info.battery = dataRealm.isBattery();
        info.noData = dataRealm.isNoData();
        info.barometerHigh = dataRealm.isBarometerHigh();
        info.barometerLow = dataRealm.isBarometerLow();
        info.temperatureHigh = dataRealm.isTemperatureHigh();
        return info;
    }

    public static TPMSInfo createTPMSInfoWidthRealm(TirePressureData data) {
        TPMSInfo info = new TPMSInfo();
        info.position = data.getPostion();
        info.sensorID = data.getSensorID();
        info.pressure = data.getPressure();
        info.temperature = data.getTemperature();
        info.gasLeaks = data.getGasLeaks();
        info.battery = data.isBattery();
        info.noData = data.isNoData();
        info.barometerHigh = data.isBarometerHigh();
        info.barometerLow = data.isBarometerLow();
        info.temperatureHigh = data.isTemperatureHigh();
        return info;
    }

    public static TirePressureData createTirePressureData(TPMSInfo data) {
        TirePressureData info = new TirePressureData(data.position);
        info.setSensorID(data.getSensorID());
        info.setPressure(data.getPressure());
        info.setTemperature(data.getTemperature());
        info.setGasLeaks(data.getGasLeaks());
        info.setBattery(data.isBattery());
        info.setNoData(data.isNoData());
        info.setBarometerHigh(data.isBarometerHigh());
        info.setBarometerLow(data.isBarometerLow());
        info.setTemperatureHigh(data.isTemperatureHigh());
        return info;
    }

    /**
     * 返回胎压信息是否属于正常情况
     *
     * @return 是否正常
     */
    private static boolean checkTirePressureDataIsException(TirePressureData tirePressureDataRealm) {
        log.debug("checkTirePressureDataRealmIsRight {}", tirePressureDataRealm.toString());
        boolean isRight = tirePressureDataRealm != null
                && (tirePressureDataRealm.getGasLeaks() == 0 || tirePressureDataRealm.getGasLeaks() == 2)
                && !tirePressureDataRealm.isBattery()
                && !tirePressureDataRealm.isNoData()
                && !tirePressureDataRealm.isBarometerHigh()
                && !tirePressureDataRealm.isBarometerLow()
                && !tirePressureDataRealm.isTemperatureHigh();
        return isRight;
    }

    /**
     * 返回胎压信息是否属于正常情况
     *
     * @return 是否正常
     */
    public static boolean checkTirePressureDataIsException(TPMSInfo tpmsInfo) {
        log.debug("checkTirePressureDataIsException {}", tpmsInfo.toString());
        boolean isException = isTpmsInfoException(tpmsInfo);
//                tpmsInfo != null
//                && (tpmsInfo.getGasLeaks() == 0 || tpmsInfo.getGasLeaks() == 2)
//                && !tpmsInfo.isBattery()
//                && !tpmsInfo.isNoData()
//                && !tpmsInfo.isBarometerHigh()
//                && !tpmsInfo.isBarometerLow()
//                && !tpmsInfo.isTemperatureHigh();
        return isException;
    }

    /**
     * 设置轮胎信息,并处理数据,回调给监听者
     */
    public synchronized void setTPMSInfo(final TPMSInfo info) {
        handleTireData(info, true);
    }

    /**
     * 设置轮胎信息,并处理数据,回调给监听者
     */
    public synchronized void setTPMSInfo(final TirePressureDataRealm info) {
        handleTireData(createTPMSInfoWidthRealm(info), true);
    }

    /**
     * 主动查询胎压数据
     */
    public synchronized void queryTireDatas() {
        TpmsDatasFlow.findAllTirePressureDatas(new TpmsDataCallBack() {
            @Override
            public void onDatas(List<TirePressureData> result) {
                if (result.size() != 4) {
                    notifyTireNoData();
                }
                for (TirePressureData data : result) {
                    handleTireData(createTPMSInfoWidthRealm(data), false);
                }
                log.info("主动查询到胎压数据:{}(条)", result.size());
            }

            @Override
            public void onError(Exception error) {
                log.error("主动查询胎压数据失败:{}", error);
            }
        });
    }

    /**
     * 主动查询胎压数据
     */
    public synchronized void queryTireDatas(boolean notify) {
        TpmsDatasFlow.findAllTirePressureDatas(new TpmsDataCallBack() {
            @Override
            public void onDatas(List<TirePressureData> result) {
                if (result.size() != 4) {
                    notifyTireNoData();
                }
                for (TirePressureData data : result) {
                    handleTireData(createTPMSInfoWidthRealm(data), notify);
                }
                log.info("主动查询到胎压数据{}:{}(条)", notify, result.size());
            }

            @Override
            public void onError(Exception error) {
                log.error("主动查询胎压数据失败:{}", error);
            }
        });
    }

    /**
     * 通知胎压无数据,需要对码
     */
    private void notifyTireNoData() {
        log.info("无数据,对码通知.");
//        mTPMSInfo1 = null;
//        mTPMSInfo2 = null;
//        mTPMSInfo3 = null;
//        mTPMSInfo4 = null;
        notifyTireDataChangeInternal(true, true, true, true);
    }

    /**
     * 解析轮胎数据,检测是否更改,是否异常
     *
     * @param info   轮胎信息
     * @param notify 是否发送通知消息
     */
    private void handleTireData(final TPMSInfo info, boolean notify) {
        if (info != null) {
            log.info("处理 {} 的数据:{}", getTireChinese(info.getPosition()), info.toString());
            boolean change1 = false;
            boolean change2 = false;
            boolean change3 = false;
            boolean change4 = false;
            final int position = info.getPosition();
            if (position == R_1) {
                if (!info.equals(mTPMSInfo1)) {
                    mTPMSInfo1 = info;
                    change1 = true;

                    //右前轮有异常
//                    if (checkTirePressureDataIsException(mTPMSInfo1)) {
//                        if (notify) {
//                            notifyTireExceptionChange(R_1, mTPMSInfo1);
//                        }
//                        notifyTireException(mTPMSInfo1);
//                    }

                    notifyTireException(mTPMSInfo1, checkTirePressureDataIsException(mTPMSInfo1));
                }
            } else if (position == L_1) {
                if (!info.equals(mTPMSInfo2)) {
                    mTPMSInfo2 = info;
                    change2 = true;

                    //右后轮有异常
//                    if (checkTirePressureDataIsException(mTPMSInfo2)) {
//                        if (notify) {
//                            notifyTireExceptionChange(L_1, mTPMSInfo2);
//                        }
//                        notifyTireException(mTPMSInfo2);
//                    }
                    notifyTireException(mTPMSInfo2, checkTirePressureDataIsException(mTPMSInfo2));
                }
            } else if (position == R_2) {
                if (!info.equals(mTPMSInfo3)) {
                    mTPMSInfo3 = info;
                    change3 = true;

                    //左前轮有异常
//                    if (checkTirePressureDataIsException(mTPMSInfo3)) {
//                        if (notify) {
//                            notifyTireExceptionChange(R_2, mTPMSInfo3);
//                        }
//                        notifyTireException(mTPMSInfo3);
//                    }

                    notifyTireException(mTPMSInfo3, checkTirePressureDataIsException(mTPMSInfo3));
                }
            } else if (position == L_2) {
                if (!info.equals(mTPMSInfo4)) {
                    mTPMSInfo4 = info;
                    change4 = true;

                    //左后轮有异常
//                    if (checkTirePressureDataIsException(mTPMSInfo4)) {
//                        if (notify) {
//                            notifyTireExceptionChange(L_2, mTPMSInfo4);
//                        }
//                        notifyTireException(mTPMSInfo4);
//                    }

                    notifyTireException(mTPMSInfo4, checkTirePressureDataIsException(mTPMSInfo4));
                }
            }

            if (notify) {
                //通知轮胎数据有异常
                notifyTireDataChangeInternal(change1, change2, change3, change4);
            }
        }
    }

    /**
     * 通知轮胎异常
     */
    private void notifyTireException(TPMSInfo info, boolean isException) {
        EventBus.getDefault().post(new TireExceptionEvent(info, isException));
    }

    private void notifyTireDataChangeInternal(boolean change1, boolean change2, boolean change3, boolean change4) {
//        log.info("通知胎压数据改变:\n右前轮-->{}左前轮-->{}右后轮-->{}左后轮-->{}",
//                mTPMSInfo1 == null ? "暂无数据\n" : mTPMSInfo1.toString(),
//                mTPMSInfo2 == null ? "暂无数据\n" : mTPMSInfo2.toString(),
//                mTPMSInfo3 == null ? "暂无数据\n" : mTPMSInfo3.toString(),
//                mTPMSInfo4 == null ? "暂无数据\n" : mTPMSInfo4.toString()
//        );
        if (change1) {
            log.info("通知胎压数据改变:\n右前轮-->{}",
                    mTPMSInfo1 == null ? "暂无数据\n" : mTPMSInfo1.toString()
            );
        }
        if (change2) {
            log.info("通知胎压数据改变:\n左前轮-->{}",
                    mTPMSInfo2 == null ? "暂无数据\n" : mTPMSInfo2.toString()
            );
        }
        if (change3) {
            log.info("通知胎压数据改变:\n右后轮-->{}",
                    mTPMSInfo3 == null ? "暂无数据\n" : mTPMSInfo3.toString()
            );
        }
        if (change4) {
            log.info("通知胎压数据改变:\n左后轮-->{}",
                    mTPMSInfo4 == null ? "暂无数据\n" : mTPMSInfo4.toString()
            );
        }

        notifyTireDataChange(change1, change2, change3, change4);
    }

    /**
     * 可以强制更新界面
     */
    public void notifyTireDataChange(boolean change1, boolean change2, boolean change3, boolean change4) {
        for (ITireDataListener listener : mTireDataListenerList) {
            ThreadExecutor.instance().onMain(() -> listener.onTireData(change1, change2, change3, change4, mTPMSInfo1, mTPMSInfo2, mTPMSInfo3, mTPMSInfo4));
        }
    }


    private void notifyTireExceptionChange(int position, TPMSInfo info) {
        String tire = "";
        if (position == R_1) {
            tire = "右前轮";
        } else if (position == L_1) {
            tire = "左前轮";
        } else if (position == R_2) {
            tire = "右后轮";
        } else if (position == L_2) {
            tire = "左后轮";
        }
        log.info("通知轮胎异常:\n{}-->{}", tire, info.toString());
        for (ITireExceptionListener listener : mTireExceptionListenerList) {
            ThreadExecutor.instance().onMain(() -> listener.onTireException(position, info));
        }
    }

    /**
     * 添加监听胎压信息改变
     */
    public void addTireDataListener(ITireDataListener tireDataListener) {
        mTireDataListenerList.add(tireDataListener);
    }

    /**
     * 添加监听胎压异常
     */
    public void addTireExceptionListener(ITireExceptionListener tireExceptionListener) {
        mTireExceptionListenerList.add(tireExceptionListener);
    }

    /**
     * 移出胎压信息改变监听
     */
    public void removeTireDataListener(ITireDataListener tireDataListener) {
        mTireDataListenerList.remove(tireDataListener);
    }

    /**
     * 移出胎压异常监听
     */
    public void removeTireExceptionListener(ITireExceptionListener tireExceptionListener) {
        mTireExceptionListenerList.remove(tireExceptionListener);
    }

    public interface ITireExceptionListener {
        /**
         * 胎压异常监听回调,主线程.
         *
         * @param position 轮胎位置
         * @param info     轮胎信息
         */
        void onTireException(final int position, final TPMSInfo info);
    }

    public interface ITireDataListener {
        /**
         * 收到胎压数据之后的回调,主线程.
         *
         * @param change1 表示对应轮胎的数据,是否有改变
         * @param change2 表示对应轮胎的数据,是否有改变
         * @param change3 表示对应轮胎的数据,是否有改变
         * @param change4 表示对应轮胎的数据,是否有改变
         * @param info1   右前轮   轮胎信息
         * @param info2   左前轮   轮胎信息
         * @param info3   右后轮   轮胎信息
         * @param info4   左后轮   轮胎信息
         */
        void onTireData(final boolean change1, final boolean change2, final boolean change3, final boolean change4, final TPMSInfo info1, final TPMSInfo info2, final TPMSInfo info3, final TPMSInfo info4);
    }

    static class Holder {
        static final TirePressureManager sTirePressureManager = new TirePressureManager();
    }
}
