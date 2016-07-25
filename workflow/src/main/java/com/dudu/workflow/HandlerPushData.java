package com.dudu.workflow;

import android.content.Context;
import android.text.TextUtils;

import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.event.Events;
import com.dudu.commonlib.utils.DataJsonTranslation;
import com.dudu.commonlib.utils.IPConfig;
import com.dudu.commonlib.utils.TextVerify;
import com.dudu.drivevideo.frontcamera.FrontCameraManage;
import com.dudu.persistence.factory.RealmCallFactory;
import com.dudu.persistence.realmmodel.tirepressure.TireInfoSetDataRealm;
import com.dudu.rest.common.RetrofitServiceFactory;
import com.dudu.rest.model.common.RequestResponse;
import com.dudu.rest.model.driving.response.GetCarBrandResponse;
import com.dudu.workflow.common.DataFlowFactory;
import com.dudu.workflow.common.ObservableFactory;
import com.dudu.workflow.common.RequestFactory;
import com.dudu.workflow.log.SendLogs;
import com.dudu.workflow.obd.CarLock;
import com.dudu.workflow.obd.SpeedFlow;
import com.dudu.workflow.push.model.PushParams;
import com.dudu.workflow.push.model.ReceiverPushData;
import com.dudu.workflow.push.model.VideoStreamConstant;
import com.dudu.workflow.push.model.VideoStreamMessage;
import com.dudu.workflow.robbery.RobberyFlow;
import com.dudu.workflow.tpms.TireInfoSetManager;
import com.dudu.workflow.upgrade.LauncherUpgrade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import de.greenrobot.event.EventBus;
import rx.Subscription;
import rx.functions.Action1;


/**
 * Created by Administrator on 2016/3/30.
 */
public class HandlerPushData {

    private static HandlerPushData mInstance;
    private static Context mContext;
    private Logger logger = LoggerFactory.getLogger("workFlow.webSocket.HandlerPushData");
    private Subscription speedSubscription;

    public static HandlerPushData getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new HandlerPushData();
        }
        if (mContext == null) {
            mContext = context;
        }
        return mInstance;
    }

    public void handlerData(ReceiverPushData data) {
        logger.debug("推送的数据：" + data.toString());
        if (data != null && data.result != null) {
            String method = data.result.method;
            logger.debug("推送的数据：method:" + method);
            if (data.resultCode == 0 && method != null) {
                String messageId = data.result.messageId;
                String requestStartTime = data.result.requestStartTime;
                if (messageId != null && requestStartTime != null
                        && !TextUtils.equals(method, PushParams.SET_TIRE_PRESSURE)//如果是配置胎压预警值,则在配置成功之后调用
                        ) {
                    logger.debug("messageId:" + messageId);
                    RequestFactory.getPushCallBackRequest().pushCallBack(messageId, method).subscribe(new Action1<RequestResponse>() {
                        @Override
                        public void call(RequestResponse requestResponse) {
                            logger.debug("推送的回调返回的信息：" + "code:" + requestResponse.resultCode + "  msg:" + requestResponse.resultMsg);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            logger.debug("error:" + throwable);
                        }
                    });
                }
                switch (method) {
                    case PushParams.ROBBERY_STATE:
                        String thiefSwitchState = data.result.robberySwitchs;
                        String operationNumber = data.result.numberOfOperations;
                        String completeTime = data.result.completeTime;
                        String rotatingSpeed = data.result.revolutions;
                        String robberyTrigger = data.result.protectRobTriggerSwitchState;
                        logger.debug("推送的数据：防劫的状态:" + thiefSwitchState);
                        if (!TextVerify.isEmpty(thiefSwitchState) && !TextVerify.isEmpty(operationNumber) && !TextVerify.isEmpty(completeTime) && !TextVerify.isEmpty(rotatingSpeed)) {
                            DataFlowFactory.getRobberyMessageFlow().changeRobberyMessage(thiefSwitchState.endsWith("1") ? true : false, rotatingSpeed, operationNumber, completeTime);
                        }
                        RobberyFlow.unlockRobbery(robberyTrigger);
                        break;
                    case PushParams.TEST_SPEED_START:
                        try {
                            logger.debug("订阅获取当前车速的事件");
                            speedSubscription = SpeedFlow.carSpeed()
                                    .subscribe(speed -> {
                                        logger.debug("取消订阅获取当前车速的事件");
                                        speedSubscription.unsubscribe();
                                        if (speed > 0) {
                                            logger.debug("检测到车速大于0，提示用户先停车然后在进行加速测试");
                                            requestStopSpeedTest();
                                            EventBus.getDefault().post(new Events.TestSpeedEvent(Events.TEST_SPEED_ZERO));
                                        } else {
                                            try {
                                                logger.debug("开始加速测试，跳转加速测试的界面");
                                                EventBus.getDefault().post(new Events.TestSpeedEvent(Events.TEST_SPEED_START));
                                                ObservableFactory.testAccSpeedFlow(data);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }, throwable -> {
                                        speedSubscription.unsubscribe();
                                        logger.debug("订阅获取当前车速的事件失败", throwable);
                                    });
                        } catch (IOException e) {
                            logger.debug("获取当前速度方法错误:" + e.getMessage());
                        }
                        break;
                    case PushParams.TEST_SPEED_STOP:
                        logger.debug("收到停止加速测试的推送。。");
                        ObservableFactory.stopAccelerationTestFlow();
                        EventBus.getDefault().post(new Events.TestSpeedEvent(Events.TEST_SPEED_REQUEST_STOP));
                        break;
                    case PushParams.GUARD_STATE:
                        String guardSwitchState = data.result.thiefSwitchState;
                        logger.debug("推送的数据：防盗的状态:" + guardSwitchState);
                        if (guardSwitchState != null) {
                            DataFlowFactory.getSwitchDataFlow().saveGuardSwitch(guardSwitchState.equals("1") ? true : false);
                            EventBus.getDefault().post(new Events.GuardSwitchState(guardSwitchState.equals("1") ? true : false));
                            if ("1".equals(guardSwitchState)) {
                                CarLock.guardLockCar();
                            } else {
                                CarLock.guardUnlockCar();
                            }
                        }
                        break;
                    case PushParams.GUARD_SET_PASSWORD:
                        String gesturePassword = data.result.protectThiefSignalPassword;
                        String gesturePasswordSwitchState = data.result.protectThiefSignalState;
                        String digitPassword = data.result.protectThiefPassword;
                        String digitPasswordSwitchState = data.result.protectThiefState;
                        logger.debug("推送的数据：手势密码:" + gesturePassword);
                        logger.debug("推送的数据：手势密码的开关状态:" + gesturePasswordSwitchState);
                        logger.debug("推送的数据：数字密码:" + digitPassword);
                        logger.debug("推送的数据：数字密码的开关状态:" + digitPasswordSwitchState);
                        if (!TextVerify.isEmpty(gesturePassword)) {
                            DataFlowFactory.getUserMessageFlow().saveGesturePassword(gesturePassword);
                        }
                        if (!TextVerify.isEmpty(gesturePasswordSwitchState)) {
                            boolean isOpen = gesturePasswordSwitchState.trim().equals("0") ? false : true;
                            DataFlowFactory.getUserMessageFlow().saveGesturePasswordSwitchState(isOpen);
                        }
                        if (!TextVerify.isEmpty(digitPassword)) {
                            DataFlowFactory.getUserMessageFlow().saveDigitPassword(digitPassword);
                        }
                        if (!TextVerify.isEmpty(digitPasswordSwitchState)) {
                            boolean isOpen = digitPasswordSwitchState.trim().equals("0") ? false : true;
                            DataFlowFactory.getUserMessageFlow().saveDigitPasswordSwitchState(isOpen);
                        }
                        break;
                    case PushParams.THEFT_APPROVAL:
                        GetCarBrandResponse.GetCarBrandResult getCarBrandResult = new GetCarBrandResponse.GetCarBrandResult();
                        getCarBrandResult.brand = data.result.brand;
                        getCarBrandResult.audit_state = String.valueOf(data.result.audit_state);
                        getCarBrandResult.obd_car_no = data.result.obd_car_no;
                        getCarBrandResult.model = data.result.model;
                        getCarBrandResult.cars_category = data.result.cars_category;
                        DataFlowFactory.getUserMessageFlow()
                                .saveCarType(getCarBrandResult);
                        break;
                    case PushParams.LOG_UPLOAD:
                        logger.info("日志上传地址：{}", data.result.logUploadUrl);
                        new SendLogs().uploadLog(data.result.logUploadUrl);
                        break;
                    case PushParams.LAUNCHER_UPGRADE:
                        LauncherUpgrade.queryVersionInfo();
                        break;
                    case PushParams.START_STREAM:
//                        EventBus.getDefault().post(new EventStartStream());
                        FrontCameraManage.getInstance().startUploadVideoStream();
                        break;
                    case PushParams.STOP_STREAM:
//                        EventBus.getDefault().post(new EventStopStream());
                        FrontCameraManage.getInstance().stopUploadVideoStream();
                        break;
                    case PushParams.VIDEO_STREAM:
                        if (TextUtils.equals(data.result.Message.getCommand(), VideoStreamConstant.COMMAND_REGISTER)) {
                            //注册
                            if (TextUtils.equals(data.result.Message.getIP(), IPConfig.getInstance().getSOCKET_ADDRESS()) && TextUtils.equals(data.result.DeviceID, CommonLib.getInstance().getObeId())) {
                                //服务器地址和obid一致再处理
                                FrontCameraManage.getInstance().registerVideoStream(data.result.Message.getPort());
                                confirmMethod(data.result);
                            }
                        } else if (TextUtils.equals(data.result.Message.getCommand(), VideoStreamConstant.COMMAND_NOSTREAM)) {
                            //停止
                            FrontCameraManage.getInstance().stopUploadVideoStream();
                        } else if (TextUtils.equals(data.result.Message.getCommand(), VideoStreamConstant.COMMAND_STREAM)) {
                            //开始
                            FrontCameraManage.getInstance().startUploadVideoStream();
                        }
                        break;
                    case PushParams.SET_TIRE_PRESSURE: {
                        logger.info("开始保存轮胎信息设置");
//                            TireInfoSetDataRealm tireInfoSetDataRealm = new TireInfoSetDataRealm(CommonLib.getInstance().getObeId());
//                            tireInfoSetDataRealm.setTireHighestTemperatureValue(data.result.tire_highest_temperature_value);
//                            tireInfoSetDataRealm.setFrontAxleTirePressureRangeLowest(data.result.front_axle_tire_pressure_range_lowest);
//                            tireInfoSetDataRealm.setFrontAxleTirePressureRangeHighest(data.result.front_axle_tire_pressure_range_highest);
//                            tireInfoSetDataRealm.setRearAxleTirePressureRangeLowest(data.result.rear_axle_tire_pressure_range_lowest);
//                            tireInfoSetDataRealm.setRearAxleTirePressureRangeHighest(data.result.rear_axle_tire_pressure_range_highest);
//                            RealmCallFactory.saveTireInfoSetDataSync(tireInfoSetDataRealm, new RealmCallBack() {
//                                @Override
//                                public void onRealm(Object result) {
//                                    logger.info("轮胎信息设置保存成功!");
//                                }
//
//                                @Override
//                                public void onError(Object error) {
//                                    logger.info("轮胎信息设置保存失败！");
//                                }
//                            });

                        RealmCallFactory.tran(realm -> {
                            final TireInfoSetDataRealm realmObject = realm.createObject(TireInfoSetDataRealm.class);
                            realmObject.setTireHighestTemperatureValue(data.result.tire_highest_temperature_value);
                            realmObject.setFrontAxleTirePressureRangeLowest(data.result.front_axle_tire_pressure_range_lowest);
                            realmObject.setFrontAxleTirePressureRangeHighest(data.result.front_axle_tire_pressure_range_highest);
                            realmObject.setRearAxleTirePressureRangeLowest(data.result.rear_axle_tire_pressure_range_lowest);
                            realmObject.setRearAxleTirePressureRangeHighest(data.result.rear_axle_tire_pressure_range_highest);

                            //发出胎压信息设置更新消息
                            TireInfoSetManager.getInstance().setMessageId(messageId);
                            TireInfoSetManager.getInstance().setMethod(method);
                            TireInfoSetManager.getInstance().notifyTireInfoSetUpdate(realmObject);
                        });

                    }
                    break;
                    default:
                        logger.info("unhandled PUSH method ：{}", method);
                        break;
                }

            }
        }
    }

    private void confirmMethod(ReceiverPushData.ReceivedDataResult result) {
        StringBuilder json = new StringBuilder();
        VideoStreamMessage videoStreamMessage = new VideoStreamMessage();
        videoStreamMessage.setCommand(result.Message.getCommand());
        videoStreamMessage.setDeviceID(result.Message.getDeviceID());
        videoStreamMessage.setIP("");
        videoStreamMessage.setKey(result.Message.getKey());
        videoStreamMessage.setPort(result.Message.getPort());
        videoStreamMessage.setStatus(2);

        json.append("{");
        json.append("\"DeviceID\":\"");
        json.append(result.DeviceID);
        json.append("\",");

        json.append("\"MessageID\":\"");
        json.append(result.MessageID);
        json.append("\",");

        json.append("\"Receiver\":\"");
        json.append(result.Receiver);
        json.append("\",");

        json.append("\"Message\":");
        json.append(DataJsonTranslation.objectToJson(videoStreamMessage));

        json.append("}");

        logger.info("VideoStreamService 发送数据:{}", json.toString());
        RetrofitServiceFactory.getVideoStreamService().confirmMethod(json.toString()).subscribe(videoStreamBean -> {
            logger.info("VideoStreamService 返回收到:{}", DataJsonTranslation.objectToJson(videoStreamBean));
        }, throwable -> logger.error("confirmMethod", throwable));
    }

    private void requestStopSpeedTest() {
        RequestFactory.getDrivingRequest().pushAcceleratedTestData(null, null, null, "1").subscribe(new Action1<RequestResponse>() {
            @Override
            public void call(RequestResponse requestResponse) {
                logger.debug("结果：" + requestResponse.toString());

            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                logger.debug("请求失败:" + throwable);
            }
        });
    }

}
