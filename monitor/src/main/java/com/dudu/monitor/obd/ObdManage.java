package com.dudu.monitor.obd;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.dudu.android.hideapi.SystemPropertiesProxy;
import com.dudu.monitor.active.ActiveDeviceManage;
import com.dudu.monitor.event.CarStatus;
import com.dudu.monitor.obd.modol.AdcVolDataUpload;
import com.dudu.monitor.obd.modol.FlamoutData;
import com.dudu.monitor.obd.modol.FlamoutDataUpload;
import com.dudu.monitor.obd.modol.ObdRTData;
import com.dudu.monitor.obd.modol.ObdRTDataUpload;
import com.dudu.network.NetworkManage;
import com.dudu.network.message.DriveHabitsDataUpload;
import com.dudu.network.message.ObdRtDataUpload;
import com.dudu.network.message.VoltageDataUpload;
import tm.dudu.ext.GpioControl;
import com.dudu.workflow.obd.OBDStream;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;


/**
 * Created by dengjun on 2016/3/8.
 * Description :
 */
public class ObdManage {
    private static ObdManage instance = null;
    private int curSpeed;//当前车速
    private float curRpm;//当前转速
    public static final int READ_FAULT_TIME = 30;
    /* 车辆状态*/
    private CarStatus carStatus = CarStatus.OFFLINE;
    private float cur_batteryV;

    private Subscription realTimeObdDataSubscription;
    private Subscription flamoutDataSubscription;
    private Subscription voltageDataSubscription;
    private Subscription startDetectSpdDataSubscription;
    private Subscription startVoltageDataSubscription;
    private Subscription totalDistanceAndRemainLSubscription;
    private Subscription sleepModeSubscription;

    private Gson gson;
    private ObdManageHandler handler;

    private static final int MSG_UPLOAD_RT_OBD = 1;
    private static final int MSG_UPLOAD_FLARM_OUT_DATA = 2;
    private static final int MSG_UPLOAD_VOLTAGE = 3;

    private Logger log = LoggerFactory.getLogger("car.ObdManage");

    private class ObdManageHandler extends Handler {
        public ObdManageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            log.info("OBD Handle Msg is ：" + msg.what);
            switch (msg.what) {
                case MSG_UPLOAD_RT_OBD: {
                    //采用RX机制
                }
                break;
                case MSG_UPLOAD_FLARM_OUT_DATA: {
                    FlamoutDataUpload flamoutDataUpload = (FlamoutDataUpload) msg.obj;
                    try {
                        if (ActiveDeviceManage.getInstance().isDeviceActived()) {
//                            log.info("Robert -> gson.toJson(flamoutDataUpload) = {}",gson.toJson(flamoutDataUpload));
                            NetworkManage.getInstance().sendMessage(new DriveHabitsDataUpload(gson.toJson(flamoutDataUpload)));
                        }

                    } catch (Exception e) {
                        log.error("异常：", e);
                    }
                }
                break;
                case MSG_UPLOAD_VOLTAGE: {
                    AdcVolDataUpload voltageData = (AdcVolDataUpload) msg.obj;
                    try {
                        if (ActiveDeviceManage.getInstance().isDeviceActived()) {
                            NetworkManage.getInstance().sendMessage(new VoltageDataUpload(gson.toJson(voltageData)));
                        }

                    } catch (Exception e) {
                        log.error("异常：", e);
                    }
                }
                break;
                default: {
                    log.info("unhandled msg msg.what = " + msg.what);
                }
                break;
            }
            super.handleMessage(msg);
        }
    }

    private ObdManage() {
        gson = new Gson();

        HandlerThread handlerThread = new HandlerThread("Thread.obdManage");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        handler = new ObdManageHandler(looper);
    }


    public static ObdManage getInstance() {
        if (instance == null) {
            synchronized (ObdManage.class) {
                if (instance == null) {
                    instance = new ObdManage();
                }
            }
        }
        return instance;
    }

    public void init() {
        sendStartDataStreamCmd();
        log.info("初始化obd数据上传");
        proObdRtData();
        proFlamoutData();

        /*开启ADC启动电压订阅*/
        startStartVolTask();
    }

    public void obdSleep() {
        try {
            //OBD休眠第一防线
            OBDStream.getInstance().exec("ATENTERSLEEP");
        } catch (Exception e) {
            log.error("ATENTERSLEEP", e);
            //OBD休眠第二防线
            com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
                    setCmd(SystemPropertiesProxy.SYS_OBD_SLEEP, SystemPropertiesProxy.high);
        }

        if (sleepModeSubscription != null) {
            sleepModeSubscription.unsubscribe();
            sleepModeSubscription = null;
        }

        //OBD休眠第三防线
        sleepModeSubscription = OBDStream.getInstance().sleepModeObservable()
                .timeout(10, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .subscribe(s -> sleepModeSubscription.unsubscribe(),
                        throwable -> {
                            if (throwable instanceof TimeoutException) {
                                makeSureOBDSleep();
                            }
                        },
                        () -> log.error("sleepModeSubscription结束"));
    }

    public void makeSureOBDSleep() {
        //复位后,处于非休眠模式
        log.info("SYS_OBD_RESET");
        /*com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
                setCmd(SystemPropertiesProxy.SYS_OBD_RESET, SystemPropertiesProxy.low);*/
        GpioControl.writeDevice(GpioControl.RESET_OBD, GpioControl.HIGH);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
//                setCmd(SystemPropertiesProxy.SYS_OBD_RESET, SystemPropertiesProxy.high);
        GpioControl.writeDevice(GpioControl.RESET_OBD, GpioControl.HIGH);

        //8s后再下发休眠命令
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.info("SYS_OBD_SLEEP");
        com.dudu.android.hideapi.SystemPropertiesProxy.getInstance().
                setCmd(SystemPropertiesProxy.SYS_OBD_SLEEP, SystemPropertiesProxy.high);
    }

    /**
     * 获取总里程
     */
    public static void obdGetTotalDistance() {
        try {
            OBDStream.getInstance().exec("ATGETMIL");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取剩余油量
     */
    public static void obdRemainL() {
        try {
            OBDStream.getInstance().exec("ATGETFUEL");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取ACC电压
     */
    public static void obdAccVoltage() {
        try {
            OBDStream.getInstance().exec("ATGETVOL");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void proObdRtData() {
        try {
            if (realTimeObdDataSubscription != null) {
                realTimeObdDataSubscription.unsubscribe();
                realTimeObdDataSubscription = null;
            }
            realTimeObdDataSubscription =
                    OBDStream.getInstance()
                            .OBDRTData()
                            .map((obdRtDataStringArray) -> new ObdRTData(obdRtDataStringArray))
                            .doOnNext((obdRTData) -> {
                                preProObdRealData(obdRTData);
                                CoolantTemperatureManager.getInstance().checkCoolantTemperature(obdRTData); //增加水温监测预警
                            })
                            .map((obdRTData) -> new ObdRTDataUpload(obdRTData))
                            .buffer(15, TimeUnit.SECONDS)
                            .subscribeOn(Schedulers.newThread())
                            .subscribe(obdRTDataUploadList -> {
                                        sendObdRealData(obdRTDataUploadList);
                                    },
                                    (throwable -> {
                                        log.error("OBDRTData buffer 异常：", throwable);
                                    }),
                                    () -> log.error("proObdRtData 该次读取结束"));
        } catch (IOException e) {
            log.error("proObdRtData 异常：", e);
        }
    }

    private void sendStartDataStreamCmd() {
        rx.Observable.timer(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe(l -> {
                    try {
                        log.info("发送开启数据流命令");
                        OBDStream.getInstance().exec("ATRON");
                    } catch (IOException e) {
                        log.error("异常：", e);
                    }
                }, throwable -> {
                    log.error("异常：", throwable);
                });
    }


    public void release() {
        if (realTimeObdDataSubscription != null) {
            realTimeObdDataSubscription.unsubscribe();
            realTimeObdDataSubscription = null;
        }
        if (flamoutDataSubscription != null) {
            flamoutDataSubscription.unsubscribe();
            flamoutDataSubscription = null;
        }

        if (totalDistanceAndRemainLSubscription != null) {
            totalDistanceAndRemainLSubscription.unsubscribe();
            totalDistanceAndRemainLSubscription = null;
        }

        if (voltageDataSubscription != null) {
            voltageDataSubscription.unsubscribe();
            voltageDataSubscription = null;
        }

        if (startVoltageDataSubscription != null) {
            startVoltageDataSubscription.unsubscribe();
            startVoltageDataSubscription = null;
        }

        if (startDetectSpdDataSubscription != null) {
            startDetectSpdDataSubscription.unsubscribe();
            startDetectSpdDataSubscription = null;
        }

        if (sleepModeSubscription != null) {
            sleepModeSubscription.unsubscribe();
            sleepModeSubscription = null;
        }
    }

    private void preProObdRealData(ObdRTData obdRTData) {
//        log.debug("车速：{}，转速：{}", obdRTData.getSpd(), obdRTData.getEngSpd());
    }

    private void sendObdRealData(List<ObdRTDataUpload> obdRTDataUploadList) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (ObdRTDataUpload obdRTDataUpload : obdRTDataUploadList) {
                jsonArray.put(new JSONObject(gson.toJson(obdRTDataUpload)));
            }
            if (jsonArray.length() > 0 && ActiveDeviceManage.getInstance().isDeviceActived()) {
                NetworkManage.getInstance().sendMessage(new ObdRtDataUpload(jsonArray));
            }
        } catch (JSONException e) {
            log.error("异常", e);
        }
    }


    private void proFlamoutData() {
        try {
            if (flamoutDataSubscription != null) {
                flamoutDataSubscription.unsubscribe();
                flamoutDataSubscription = null;
            }
            flamoutDataSubscription =
                    OBDStream.getInstance()
                            .OBDTTData()
                            .map((obdTTDataStringArray) -> new FlamoutData(obdTTDataStringArray))
                            .map((flamoutData1 -> new FlamoutDataUpload(flamoutData1)))
                            .subscribeOn(Schedulers.newThread())
                            .subscribe((flamoutData -> {
                                        log.debug("收到熄火数据：{}", gson.toJson(flamoutData));
                                        try {
                                            getTotalDistanceAndRemainL(flamoutData);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }),
                                    (throwable -> {
                                        log.error("异常：", throwable);
                                    }));
        } catch (IOException e) {
            log.error("异常：", e);
        }
    }

    private void getTotalDistanceAndRemainL(FlamoutDataUpload flamoutDataUpload) throws IOException {
        log.debug("getTotalDistanceAndRemainL");

        if (totalDistanceAndRemainLSubscription != null) {
            totalDistanceAndRemainLSubscription.unsubscribe();
            totalDistanceAndRemainLSubscription = null;
        }
        totalDistanceAndRemainLSubscription = Observable
                .zip(OBDStream.getInstance().totalDistanceStream(), OBDStream.getInstance().remainLStream(), (totalDistance, remainL) -> {
                    log.debug("getTotalDistance:" + totalDistance + ";AndRemainL:" + remainL);
                    flamoutDataUpload.setTotalDistance(totalDistance);
                    flamoutDataUpload.setRemainL(remainL);
                    return flamoutDataUpload;
                })
                .timeout(5, TimeUnit.SECONDS)
                .subscribe(
                        flamoutDataUpload1 -> sendFlamoutData(flamoutDataUpload1),
                        throwable -> {
                            log.debug("getTotalDistanceAndRemainL", throwable);
                            sendFlamoutData(flamoutDataUpload);
                        });

        ObdManage.obdRemainL();
        ObdManage.obdGetTotalDistance();

    }

    public void procAccVol() throws IOException {
        log.debug(" getAccVol");

        try {
            if (voltageDataSubscription != null) {
                voltageDataSubscription.unsubscribe();
                voltageDataSubscription = null;
            }
            voltageDataSubscription = OBDStream.getInstance()
                    .accVoltageStream()
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(voltageDataString -> {

                        sendAdcVoltageData(new AdcVolDataUpload(voltageDataString, "1"));
                        if (voltageDataSubscription != null && !voltageDataSubscription.isUnsubscribed()) {
                            voltageDataSubscription.unsubscribe();
                            voltageDataSubscription = null;
                        }
                    }, throwable -> {
                        log.error(" 异常：", throwable);
                    });

            ObdManage.obdAccVoltage();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendFlamoutData(FlamoutDataUpload flamoutDataUpload) {
        log.info("sendFlamoutData : {}", flamoutDataUpload.getRemainL());

        Message msg = Message.obtain();
        msg.what = MSG_UPLOAD_FLARM_OUT_DATA;
        msg.obj = flamoutDataUpload;
        handler.sendMessage(msg);

    }

    private void startStartVolTask() {
        try {
            log.info("startStartVolTask");
            if (startVoltageDataSubscription != null) {
                startVoltageDataSubscription.unsubscribe();
                startVoltageDataSubscription = null;
            }
            startVoltageDataSubscription = OBDStream.getInstance().startVolStream()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.newThread())
                    .subscribe(s -> {
                                log.info("获得启动点火电压:{}", s);

                                sendAdcVoltageData(new AdcVolDataUpload(s, "0"));

                                if (startVoltageDataSubscription != null && !startVoltageDataSubscription.isUnsubscribed()) {
                                    startVoltageDataSubscription.unsubscribe();
                                    startVoltageDataSubscription = null;
                                }
                            }
                            , Throwable::printStackTrace);

            if (startDetectSpdDataSubscription != null) {
                startDetectSpdDataSubscription.unsubscribe();
                startDetectSpdDataSubscription = null;
            }
            startDetectSpdDataSubscription =
                    OBDStream.getInstance()
                            .OBDRTData()
                            .map((obdRtDataStringArray) -> new ObdRTData(obdRtDataStringArray))
                            .filter((obdRTData) -> (obdRTData.getSpd()) == 0 && (obdRTData.getEngSpd() > 0))
                            .doOnNext((obdRTData) -> {

                            })
                            .subscribeOn(Schedulers.newThread())
                            .first()
                            .delay(2, TimeUnit.SECONDS)
                            .subscribeOn(Schedulers.io())
                            .subscribe(obdRTData -> {

                                        if (startDetectSpdDataSubscription != null && !startDetectSpdDataSubscription.isUnsubscribed()) {
                                            startDetectSpdDataSubscription.unsubscribe();
                                            startDetectSpdDataSubscription = null;
                                        }

                                        log.info("执行获取启动电压命令：ATGETSTARTVOL");
                                        //发送获取启动ADC电压的命令
                                        try {

                                            OBDStream.getInstance().exec("ATGETSTARTVOL");

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    },
                                    (throwable -> {
                                        log.error("获取启动电压时间点 异常：", throwable);
                                    }),
                                    () -> log.error("电压时间点读取结束"));
        } catch (IOException e) {
            log.error("startStartVolTask 异常：", e);
        }
    }

    private void sendAdcVoltageData(AdcVolDataUpload adcVolDataUpload) {
        log.debug("sendAdcVoltageData");
        Message msg = Message.obtain();
        msg.what = MSG_UPLOAD_VOLTAGE;
        msg.obj = adcVolDataUpload;
        handler.sendMessage(msg);

    }


    public int getCurSpeed() {
        return curSpeed;
    }

    public void setCurSpeed(int curSpeed) {
        this.curSpeed = curSpeed;
    }

    public float getCurRpm() {
        return curRpm;
    }

    public void setCurRpm(float curRpm) {
        this.curRpm = curRpm;
    }

    public CarStatus getCarStatus() {
        return carStatus;
    }

    public void setCarStatus(CarStatus carStatus) {
        this.carStatus = carStatus;
    }

    public float getCur_batteryV() {
        return cur_batteryV;
    }

    public void setCur_batteryV(float cur_batteryV) {
        this.cur_batteryV = cur_batteryV;
    }
}
