package com.dudu.monitor.tirepressure;

import com.dudu.monitor.active.ActiveDeviceManage;
import com.dudu.monitor.tirepressure.model.TirePressureFactory;
import com.dudu.monitor.tirepressure.model.TirePressureUpload;
import com.dudu.network.NetworkManage;
import com.dudu.network.message.TirePressureDataUpload;
import com.dudu.persistence.realm.RealmCallBack;
import com.dudu.persistence.realmmodel.tirepressure.TirePressureDataRealm;
import com.dudu.workflow.tpms.TPMSFlow;
import com.dudu.workflow.tpms.TPMSInfo;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Subscription;
import rx.schedulers.Schedulers;

/**
 * Created by dengjun on 2016/4/18.
 * Description :
 */
public class TirePressureManage {
    private static TirePressureManage mInstance = new TirePressureManage();
    private static Logger log = LoggerFactory.getLogger("monitor.TirePressure");
    private Subscription tirePressureSubscription;

    private TirePressureManage() {

    }

    public static TirePressureManage getInstance() {
        return mInstance;
    }

    public static void uploadTirePressureData(TirePressureUpload tirePressureUpload) {
        log.debug("胎压数据：{}", new Gson().toJson(tirePressureUpload));

        //设备已经激活,并且对应轮胎已经对码成功,则上传胎压信息
        if (ActiveDeviceManage.getInstance().isDeviceActived() && TirePairSp.isTpmsPair(tirePressureUpload.a)) {
            NetworkManage.getInstance().sendMessage(new TirePressureDataUpload(new Gson().toJson(tirePressureUpload)));
        } else {
            //未对码, 发送空数据
            uploadEmptyTirePressureData(tirePressureUpload.a);
        }
    }

    /**
     * 上传对应轮胎位置的空数据
     */
    public static void uploadEmptyTirePressureData(int position) {
        log.debug("上传服务器,置空 {} 胎压数据.", TirePressureManager.getTireChinese(position));

        TPMSInfo info = new TPMSInfo();
        info.position = position;
        info.pressure = 0f;
        info.noData = false;
        info.temperature = 0;
        info.gasLeaks = 0;
        NetworkManage.getInstance().sendMessage(
                new TirePressureDataUpload(new Gson().toJson(new TirePressureUpload(info))));
    }

    public void init() {
        log.info("初始化TirePressureManage");
        if (TPMSFlow.TPMSWarnInfoStream() == null) {
            return;
        }
        tirePressureSubscription = TPMSFlow
                .TPMSWarnInfoStream()
                .subscribeOn(Schedulers.io())
                .doOnNext(tpmsWarnInfo1 -> {
                    saveTirePressureData(tpmsWarnInfo1);
                })
                .map(tpmsWarnInfo -> new TirePressureUpload(tpmsWarnInfo))
                .subscribe(tirePressureUpload -> {
                    try {
                        uploadTirePressureData(tirePressureUpload);
                    } catch (Exception e) {
                        log.error("异常", e);
                    }
                }, throwable -> {
                    log.error("异常", throwable);
                });

       /* Observable
                .interval(5, 30, TimeUnit.SECONDS)
                .subscribe(l->{
                    test();
                },throwable -> {
                    log.error("异常", throwable);
                });*/
    }

    public void release() {
        if (tirePressureSubscription != null && !tirePressureSubscription.isUnsubscribed()) {
            tirePressureSubscription.unsubscribe();
        }
    }

    public void saveTirePressureData(TPMSInfo tpmsWarnInfo) {
        TirePressureFactory.saveTirePressureData(tpmsWarnInfo, new RealmCallBack<TirePressureDataRealm, Exception>() {
            @Override
            public void onRealm(TirePressureDataRealm result) {
                log.debug("保存的胎压数据：位置：{}", result.getPostion());

                /*在内存中处理胎压数据*/
                TirePressureManager.instance().setTPMSInfo(result);
            }

            @Override
            public void onError(Exception error) {

            }
        });
    }

    private void test() {
        uploadTirePressureData(new TirePressureUpload());
        TPMSInfo tpmsWarnInfo = new TPMSInfo();
        saveTirePressureData(tpmsWarnInfo);
    }
}
