package com.dudu.workflow.robbery;

import com.dudu.persistence.RobberyMessage.RealRobberyMessageDataService;
import com.dudu.persistence.RobberyMessage.RobberyMessage;
import com.dudu.workflow.common.ObservableFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import rx.Observable;
import rx.functions.Action1;

/**
 * Created by Administrator on 2016/4/21.
 */
public class RobberyMessageFlow {

    private static final String TAG = "RobberyMessageFlow";

    private RealRobberyMessageDataService service;

    private Logger logger = LoggerFactory.getLogger(TAG);

    public RobberyMessageFlow(RealRobberyMessageDataService service) {
        this.service = service;
    }


    public void saveRobberyMessage(RobberyMessage robberyMessage) {
        service.saveRobberyMessage(robberyMessage).subscribe(new Action1<RobberyMessage>() {
            @Override
            public void call(RobberyMessage robberyMessage) {
                logger.debug("保存防劫的信息--成功:" + robberyMessage.toString() + "");
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                logger.debug("保存防劫的信息--失败:" + robberyMessage.toString() + "");
            }
        });
    }

    public void changeRobberyMessage(boolean robberySwitchStatus, String rotatingSpeed, String operationNumber, String completeTime) {
        obtainRobberyMessage().subscribe(new Action1<RobberyMessage>() {
            @Override
            public void call(RobberyMessage robberyMessage) {
                if (!robberyMessage.isRobberySwitch()) {
                    ObservableFactory.robberyFlow().resetRobberyTimes();
                }
                robberyMessage.setRobberySwitch(robberySwitchStatus);
                robberyMessage.setRotatingSpeed(rotatingSpeed);
                robberyMessage.setOperationNumber(operationNumber);
                robberyMessage.setCompleteTime(completeTime);
                saveRobberyMessage(robberyMessage);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                logger.debug("error:" + throwable);
            }
        });
//        EventBus.getDefault().post(new Events.RobberyEvent(Integer.valueOf(rotatingSpeed), Integer.valueOf(operationNumber), Integer.valueOf(completeTime)));
        try {
            ObservableFactory.robberyFlow().startAccelerometersMonitoring(Integer.valueOf(rotatingSpeed), Integer.valueOf(operationNumber), Integer.valueOf(completeTime));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Observable<RobberyMessage> obtainRobberyMessage() {
        return service.findRobberyMessage();
    }

    public void saveRobberySwitch(boolean isOpen) {
        obtainRobberyMessage().subscribe(robberyMessage -> {
            logger.debug("开始保存防劫的开关：" + isOpen);
            if (!robberyMessage.isRobberySwitch()) {
                ObservableFactory.robberyFlow().resetRobberyTimes();
            }
            robberyMessage.setRobberySwitch(isOpen);
            saveRobberyMessage(robberyMessage);
        }, throwable -> logger.error("saveRobberySwitch", throwable));
    }

    public void saveRobberyTriggerStatus(boolean isOpen) {
        ObservableFactory.robberyFlow().setRobberyTriggered(isOpen);
        obtainRobberyMessage().subscribe(new Action1<RobberyMessage>() {
            @Override
            public void call(RobberyMessage robberyMessage) {
                logger.debug("开始保存防劫触发的状态." + (isOpen ? "打开" : "关闭"));
                robberyMessage.setRobberTrigger(isOpen);
                saveRobberyMessage(robberyMessage);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                logger.debug("saveRobberyTriggerStatus", throwable);
            }
        });
    }

    public Observable<Boolean> getRobberyTriggerSwitch() {
        return getSwitchKey();
    }

    private Observable<Boolean> getSwitchKey() {
        return service.findRobberyMessage().map(robberyMessage -> {
            logger.debug("防劫触发的值：" + robberyMessage.isRobberTrigger());
            return robberyMessage.isRobberTrigger();
        });
    }

}
