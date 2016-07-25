package com.dudu.aios.ui.fragment.tire;

import android.os.Looper;
import android.support.annotation.Nullable;

import com.dudu.commonlib.utils.ThreadExecutor;
import com.dudu.monitor.tirepressure.TirePressureManager;
import com.dudu.persistence.factory.RealmCallFactory;
import com.dudu.persistence.realmmodel.tirepressure.TireInfoSetDataRealm;
import com.dudu.rest.model.common.RequestResponse;
import com.dudu.workflow.common.RequestFactory;
import com.dudu.workflow.tpms.TPMSFlow;
import com.dudu.workflow.tpms.TPMSInfo;
import com.dudu.workflow.tpms.TPMSParamBean;
import com.dudu.workflow.tpms.TireInfoSetManager;

import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

import io.realm.RealmResults;
import rx.Observable;
import rx.functions.Action1;

/**
 * Created by robi on 2016-06-30 16:27.
 */
public class TireDataPull implements TireInfoSetManager.OnTireInfoUpdateListener, TirePressureManager.ITireExceptionListener, TirePressureManager.ITireDataListener {
    public static Logger log = TirePressureManager.log;
    TireExceptionHelper mTireExceptionHelper;
    private Set<ITireDataChangeListener> mTireDataChangeListeners;

    private TireDataPull() {
        mTireDataChangeListeners = new HashSet<>();
        mTireExceptionHelper = new TireExceptionHelper();
    }

    public static TireDataPull instance() {
        return Holder.pull;
    }

    public static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 使用内存中默认的值设置模组预警
     */
    public static void notifyTPMS() {
        notifyTPMS(TirePressureManager.maxTemp,
                TirePressureManager.frontMax, TirePressureManager.frontMin,
                TirePressureManager.backMax, TirePressureManager.backMin,
                null, null);
    }

    /**
     * 写入预警值到模组
     */
    public static void notifyTPMS(int celsius, float fBarometerHigh, float fBarometerLow,
                                  float bBarometerHigh, float bBarometerLow,
                                  @Nullable Runnable okRun, @Nullable Runnable failRun) {
        new Thread() {
            @Override
            public void run() {
                int shaft, barometerHigh, barometerLow;

                //设置预警温度
                TPMSFlow.TPMSSetTempParam(celsius);
                log.info("设置模组温度预警值:{}", celsius);
//        sleep();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                shaft = 1;
                barometerHigh = (int) (fBarometerHigh * 10);
                barometerLow = (int) (fBarometerLow * 10);
                //设置 前轴 胎压报警值
                TPMSFlow.TPMSSetBarParam(shaft, barometerHigh, barometerLow);
                log.info("设置模组前轴胎压预警值:{} {}", barometerHigh, barometerLow);
//        sleep();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                shaft = 2;
                barometerHigh = (int) (bBarometerHigh * 10);
                barometerLow = (int) (bBarometerLow * 10);
                //设置 后轴 胎压报警值
                TPMSFlow.TPMSSetBarParam(shaft, barometerHigh, barometerLow);
                log.info("设置模组后轴胎压预警值:{} {}", barometerHigh, barometerLow);
//        sleep();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //发送 TPMS 命令, 发送请求预警参数值
                TPMSFlow.TPMSGetTempParam();
                //读取返回值
                final Observable<TPMSParamBean> paramBeanObservable = TPMSFlow.TPMSGetParamStream();
                if (paramBeanObservable != null) {
                    paramBeanObservable.subscribe(tpmsParamBean -> {
                        log.info("胎压配置返回:{}", tpmsParamBean.toString());
                        if (TireValueHelper.isTPMSParamRight(tpmsParamBean, celsius, fBarometerHigh,
                                fBarometerLow, bBarometerHigh, bBarometerLow)) {
                            log.info("胎压配置设置成功");
                            if (okRun != null) {
                                okRun.run();
                            }
                        } else {
                            log.info("胎压配置设置失败");
                            if (failRun != null) {
                                failRun.run();
                            }
                        }
                    }, throwable -> {
                        log.error("胎压配置失败 {} ", throwable.getMessage());
                        if (failRun != null) {
                            failRun.run();
                        }
                    });
                } else {
                    if (failRun != null) {
                        failRun.run();
                    }
                }
            }
        }.start();
    }

    public void init() {
        addListener();
    }

    /**
     * 注册数据更新事件
     */
    private void addListener() {
        TireInfoSetManager.getInstance().setOnTireInfoUpdateListener(this);
        TirePressureManager.instance().addTireExceptionListener(this);
        TirePressureManager.instance().addTireDataListener(this);

        pullTireData();
    }

    public void addTireDataChangeListener(ITireDataChangeListener listener) {
        mTireDataChangeListeners.add(listener);
    }

    public void removeTireDataChangeListener(ITireDataChangeListener listener) {
        mTireDataChangeListeners.remove(listener);
    }

    /**
     * 查询数据库,获取最后一次的信息
     */
    public void pullTireData() {
        RealmCallFactory.tran(realm -> {
            log.info("从数据库查询轮胎范围配置.");
            final RealmResults<TireInfoSetDataRealm> dataRealms = realm.where(TireInfoSetDataRealm.class).findAll();
//                    .equalTo("obied", CommonLib.getInstance().getObeId()).findFirst();
            final int size = dataRealms.size();
            checkTireInfo(size == 0 ? null : dataRealms.get(size - 1), false);
        });
    }

    /**
     * @param confirmServer 配置成功后,是否告诉服务器
     */
    private void checkTireInfo(final TireInfoSetDataRealm tireInfoSetDataRealm, boolean confirmServer) {
        if (tireInfoSetDataRealm != null) {
            try {
                TirePressureManager.frontMin = Float.valueOf(tireInfoSetDataRealm.getFrontAxleTirePressureRangeLowest());
                TirePressureManager.frontMax = Float.valueOf(tireInfoSetDataRealm.getFrontAxleTirePressureRangeHighest());
                TirePressureManager.backMin = Float.valueOf(tireInfoSetDataRealm.getRearAxleTirePressureRangeLowest());
                TirePressureManager.backMax = Float.valueOf(tireInfoSetDataRealm.getRearAxleTirePressureRangeHighest());
                TirePressureManager.maxTemp = Integer.valueOf(tireInfoSetDataRealm.getTireHighestTemperatureValue());

                log.info("预警范围更新: 最高温:{} 最高前胎压:{} 最低前胎压:{} 最高后胎压:{} 最低后胎压:{} ",
                        TirePressureManager.maxTemp,
                        TirePressureManager.frontMax, TirePressureManager.frontMin,
                        TirePressureManager.backMax, TirePressureManager.backMin);

                // TODO: 2016-07-06
                notifyTPMSInternal(tireInfoSetDataRealm, new Runnable() {
                    @Override
                    public void run() {
                        notifyTireDataChange();
                        TireFragment.setExceptionShow(false);
                        if (confirmServer) {
                            RequestFactory.getPushCallBackRequest()
                                    .pushCallBack(TireInfoSetManager.getInstance().getMessageId(),
                                            TireInfoSetManager.getInstance().getMethod())
                                    .subscribe(new Action1<RequestResponse>() {
                                        @Override
                                        public void call(RequestResponse requestResponse) {
                                            log.debug("预警值配置确定成功：" + "code:" + requestResponse.resultCode + "  msg:" + requestResponse.resultMsg);
                                        }
                                    }, new Action1<Throwable>() {
                                        @Override
                                        public void call(Throwable throwable) {
                                            log.debug("预警值配置确定error:" + throwable);
                                        }
                                    });
                        }
                    }
                }, null);

            } catch (Exception e) {
                e.printStackTrace();
                log.error("轮胎范围配置异常:{}", e);
            }
        } else {
            log.debug("轮胎范围配置为空.");
        }
    }

    /**
     * 通知数据更改
     */
    private void notifyTireDataChange() {
        for (ITireDataChangeListener listener : mTireDataChangeListeners) {
            if (Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId()) {
                listener.onTireDataChange();
            } else {
                ThreadExecutor.instance().onMain(listener::onTireDataChange);
            }
        }
    }

    @Override
    public void onUpdate(TireInfoSetDataRealm tireInfoSet) {
        //预警范围更新
        if (tireInfoSet != null) {
            log.info("预警范围更新: 最高温:{} 最高前胎压:{} 最低前胎压:{} 最高后胎压:{} 最低后胎压:{} ",
                    tireInfoSet.getTireHighestTemperatureValue(),
                    tireInfoSet.getFrontAxleTirePressureRangeHighest(), tireInfoSet.getFrontAxleTirePressureRangeLowest(),
                    tireInfoSet.getFrontAxleTirePressureRangeHighest(), tireInfoSet.getRearAxleTirePressureRangeLowest());

//            notifyTPMSInternal(tireInfoSet, new Runnable() {
//                @Override
//                public void run() {
//                    log.info("预警值配置成功.");
//                    RequestFactory.getPushCallBackRequest()
//                            .pushCallBack(TireInfoSetManager.getInstance().getMessageId(),
//                                    TireInfoSetManager.getInstance().getMethod())
//                            .subscribe(new Action1<RequestResponse>() {
//                        @Override
//                        public void call(RequestResponse requestResponse) {
//                            log.debug("预警值配置确定成功：" + "code:" + requestResponse.resultCode + "  msg:" + requestResponse.resultMsg);
//                        }
//                    }, new Action1<Throwable>() {
//                        @Override
//                        public void call(Throwable throwable) {
//                            log.debug("预警值配置确定error:" + throwable);
//                        }
//                    });
//                }
//            }, new Runnable() {
//                @Override
//                public void run() {
//                    log.info("预警值配置失败.");
//                }
//            });
        }
        checkTireInfo(tireInfoSet, true);
    }

    @Override
    public void onTireException(int position, TPMSInfo info) {
//        //胎压异常回调
//        if (TireValueHelper.isTpmsInfoException(info)) {
//            log.info("{} 异常,准备上报.", TireValueHelper.getTireChinese(info.getPosition()));
//            RequestFactory.getTpmsRequest().notifyTireInfo(TirePressureManager.createTirePressureData(info));
//        }
    }

    @Override
    public void onTireData(boolean change1, boolean change2, boolean change3, boolean change4, TPMSInfo info1, TPMSInfo info2, TPMSInfo info3, TPMSInfo info4) {
        checkChange(change1, info1);
        checkChange(change2, info2);
        checkChange(change3, info3);
        checkChange(change4, info4);
    }

    private void checkChange(boolean change, TPMSInfo info) {
        if (change) {
            if (info != null && mTireExceptionHelper.setTPMSInfo(info)) {
                log.info("{} 异常改变, 准备上报.", TirePressureManager.getTireChinese(info.getPosition()));
                notifyTireInfo(info);
            }
        }
    }

    private void notifyTireInfo(TPMSInfo info) {
        RequestFactory.getTpmsRequest().notifyTireInfo(TirePressureManager.createTirePressureData(info)).subscribe(requestResponse -> {
            if (requestResponse.resultCode == 0) {
                log.info("{} 信息,上报成功.", TirePressureManager.getTireChinese(info.getPosition()));
            } else {
                log.info("{} 信息,上报失败.", TirePressureManager.getTireChinese(info.getPosition()));
            }
        }, throwable -> {
            log.error("{} 信息,上报异常:{}", TirePressureManager.getTireChinese(info.getPosition()), throwable);
        });
    }

    /**
     * 写入预警值到模组
     */
    private void notifyTPMSInternal(final TireInfoSetDataRealm tireInfoSet,
                                    @Nullable Runnable okRun, @Nullable Runnable failRun) {
//        int celsius, shaft, barometerHigh, barometerLow;
//
//        celsius = Integer.valueOf(tireInfoSet.getTireHighestTemperatureValue());
//
//        //设置预警温度
//        TPMSFlow.TPMSSetTempParam(celsius);
//        log.info("设置模组温度预警值:{}", celsius);
//        sleep();
//
//        shaft = 1;
//        barometerHigh = (int) (Float.valueOf(tireInfoSet.getFrontAxleTirePressureRangeHighest()) * 10);
//        barometerLow = (int) (Float.valueOf(tireInfoSet.getFrontAxleTirePressureRangeLowest()) * 10);
//        //设置 前轴 胎压报警值
//        TPMSFlow.TPMSSetBarParam(shaft, barometerHigh, barometerLow);
//        log.info("设置模组前轴胎压预警值:{} {}", barometerHigh, barometerLow);
//        sleep();
//
//        shaft = 2;
//        barometerHigh = (int) (Float.valueOf(tireInfoSet.getRearAxleTirePressureRangeHighest()) * 10);
//        barometerLow = (int) (Float.valueOf(tireInfoSet.getRearAxleTirePressureRangeLowest()) * 10);
//        //设置 后轴 胎压报警值
//        TPMSFlow.TPMSSetBarParam(shaft, barometerHigh, barometerLow);
//        log.info("设置模组后轴胎压预警值:{} {}", barometerHigh, barometerLow);
//        new Thread() {
//            @Override
//            public void run() {
        notifyTPMS(Integer.valueOf(tireInfoSet.getTireHighestTemperatureValue()),
                Float.valueOf(tireInfoSet.getFrontAxleTirePressureRangeHighest()), Float.valueOf(tireInfoSet.getFrontAxleTirePressureRangeLowest()),
                Float.valueOf(tireInfoSet.getRearAxleTirePressureRangeHighest()), Float.valueOf(tireInfoSet.getRearAxleTirePressureRangeLowest()),
                okRun, failRun);
//            }
//        }.start();
    }

    public interface ITireDataChangeListener {
        void onTireDataChange();
    }

    static class Holder {
        static final TireDataPull pull = new TireDataPull();
    }
}
