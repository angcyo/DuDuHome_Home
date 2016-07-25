package com.dudu.workflow.robbery;

import com.dudu.commonlib.event.Events;
import com.dudu.commonlib.utils.TextVerify;
import com.dudu.workflow.common.DataFlowFactory;
import com.dudu.workflow.obd.CarLock;
import com.dudu.workflow.obd.OBDStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/2/19.
 */
public class RobberyFlow {

    private static Logger logger = LoggerFactory.getLogger("workFlow.Robbery");

    private static RobberyFlow mInstance = new RobberyFlow();

    private boolean robberyTriggered = false;
    private int revolutions, numberOfOperations, completeTime;
    private long firstTime = 0;
    private long lastTime;
    private long accelerateTimes;
    private long tryToStartRobberyTimes = 0;
    private Subscription gunToggleSubscription;

    public static RobberyFlow getInstance() {
        return mInstance;
    }

    public void checkGunSwitch() {
        logger.debug("checkGunSwitch");
        if (DataFlowFactory.getRobberyMessageFlow() != null) {
            DataFlowFactory.getRobberyMessageFlow().obtainRobberyMessage()
                    .doOnNext(robberyMessage2 -> logger.debug("robberyMessage2.getRotatingSpeed():" + robberyMessage2.getRotatingSpeed()))
                    .filter(robberyMessage1 -> !TextVerify.isEmpty(robberyMessage1.getRotatingSpeed()) && Integer.valueOf(robberyMessage1.getRotatingSpeed()) > 0)
                    .doOnNext(robberyMessage1 -> logger.debug("checkGunSwitch", robberyMessage1.toString()))
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(robberyMessage -> {
                                tryToStartRobberyTimes = 0;
                                EventBus.getDefault().post(new Events.RobberyEvent(Integer.valueOf(robberyMessage.getRotatingSpeed()),
                                        Integer.valueOf(robberyMessage.getOperationNumber()),
                                        Integer.valueOf(robberyMessage.getCompleteTime())));
                            }
                            , throwable -> logger.error("checkGunSwitch", throwable)
                    );
            DataFlowFactory.getRobberyMessageFlow().obtainRobberyMessage()
                    .filter(robberyMessage1 -> TextVerify.isEmpty(robberyMessage1.getRotatingSpeed()) || Integer.valueOf(robberyMessage1.getRotatingSpeed()) <= 0)
                    .filter(robberyMessage2 -> tryToStartRobberyTimes < 3)
                    .delay(2, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(robberyMessage -> {
                        logger.debug("delay to start robbery");
                        tryToStartRobberyTimes++;
                        EventBus.getDefault().post(new RobberyStateModel(RobberyStateModel.START_ROBBERY));
                    }, throwable -> logger.error("checkGunSwitch", throwable));

        }
    }

    /**
     * 防劫踩油门逻辑，在completeTime的时间内踩油门numberOfOperations次，每次转速超过revolutions，则触发防劫
     *
     * @return
     * @throws IOException
     */
    public void startAccelerometersMonitoring(int revolutions, int numberOfOperations, int completeTime) throws IOException {
        logger.debug("startAccelerometersMonitoring");
        setRobberyArgs(revolutions, numberOfOperations, completeTime);
        resetRobberyTimes();
        DataFlowFactory.getRobberyMessageFlow().getRobberyTriggerSwitch()
                .subscribe(robberyTfiggerSwitch -> setRobberyTriggered(robberyTfiggerSwitch)
                        , throwable -> logger.error("accelerometersMonitoring", throwable));
        if (gunToggleSubscription == null) {
            gunToggle();
        }
    }

    /**
     * 防劫踩油门逻辑，在completeTime的时间内踩油门numberOfOperations次，每次转速超过revolutions，则触发防劫
     *
     * @param revolutions        限制的转速
     * @param numberOfOperations 踩油门次数
     * @param completeTime       限定的时间
     * @return
     * @throws IOException
     */
    public Observable<Boolean> gunToggle(int revolutions, int numberOfOperations, int completeTime) throws IOException {
        return OBDStream.getInstance().engSpeedStream()
                .doOnNext(aDouble1 -> logger.debug("obd gunToggle过滤前转速：" + aDouble1))
                .skip(1)
//                .skip(2,TimeUnit.SECONDS)
                .filter(aDouble -> !robberyTriggered)
                .filter(rotatespeed -> rotatespeed > 0)
                .doOnNext(aDouble1 -> logger.debug("obd gunToggle过滤后转速：" + aDouble1))
                .map(rotatespeed1 -> rotatespeed1 > revolutions)
                .distinctUntilChanged()
                .doOnNext(aBoolean1 -> logger.debug("obd.gun3Toggle:" + aBoolean1))
                .filter(aBoolean -> aBoolean)
                .take(numberOfOperations)
                .filter(aDouble -> !robberyTriggered)
                .timeout(completeTime, TimeUnit.SECONDS);
    }

    /**
     * 防劫踩油门逻辑，在completeTime的时间内踩油门numberOfOperations次，每次转速超过revolutions，则触发防劫
     *
     * @return
     * @throws IOException
     */
    public void gunToggle() throws IOException {
        logger.debug("gunToggle");
        if (gunToggleSubscription != null) {
            gunToggleSubscription.unsubscribe();
            gunToggleSubscription = null;
        }
        gunToggleSubscription = OBDStream.getInstance().engSpeedStream()
                .filter(aDouble -> !robberyTriggered)
                .doOnNext(aDouble1 -> logger.debug("obd 转速：" + aDouble1))
                .filter(rotatespeed -> rotatespeed > 0)
                .map(rotatespeed1 -> rotatespeed1 > revolutions)
                .distinctUntilChanged()
                .doOnNext(accelerate -> logger.debug("obd.gun3Toggle:" + accelerate))
                .filter(accelerate1 -> accelerate1)
                .doOnNext(accelerate2 -> {
                    if (firstTime == 0) {
                        firstTime = System.currentTimeMillis();
                        logger.debug("obd.gun3Toggle.firstTime:" + firstTime);
                    }
                })
                .doOnNext(accelerate3 -> lastTime = System.currentTimeMillis())
                .map(accelerate4 -> ((lastTime - firstTime) / 1000) > completeTime)
                .doOnNext(timout -> {
                    if (timout) {
                        accelerateTimes = 1;
                        firstTime = lastTime;
                        logger.debug("obd.gun3Toggle.timeout.accelerateTimes:" + accelerateTimes);
                    }
                })
                .doOnNext(timout1 -> {
                    if (lastTime != firstTime) {
                        accelerateTimes++;
                        logger.debug("obd.gun3Toggle.accelerateTimes++:" + accelerateTimes);
                    }
                })
                .doOnNext(timout2 -> {
                    if (lastTime == firstTime) {
                        accelerateTimes = 1;
                        logger.debug("obd.gun3Toggle.accelerateTimes==1:" + accelerateTimes);
                    }
                })
                .filter(accelerate5 -> accelerateTimes >= numberOfOperations)
                .doOnNext(accelerateTimesGot -> firstTime = 0)
                .doOnNext(accelerateTimesGot1 -> lastTime = 0)
                .doOnNext(accelerateTimesGot2 -> accelerateTimes = 0)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                            logger.debug("obd.checkGunSwitch.触发防劫:" + aBoolean);
                            EventBus.getDefault().post(new RobberyStateModel(RobberyStateModel.ROBBERY_IS_TRIGGERED));
                        }
                        , throwable -> {
                            gunToggleSubscription.unsubscribe();
                            gunToggleSubscription = null;
                            EventBus.getDefault().post(new Events.RobberyEvent(revolutions, numberOfOperations, completeTime));
                            logger.debug("obd.checkGunSwitch.防劫流程出错", throwable);
                        }
                        , () -> {
                            gunToggleSubscription.unsubscribe();
                            gunToggleSubscription = null;
                            logger.debug("obd.checkGunSwitch.防劫流程完成");
                            EventBus.getDefault().post(new Events.RobberyEvent(revolutions, numberOfOperations, completeTime));
                        });
    }

    /**
     * 防劫踩油门逻辑，在completeTime的时间内踩油门numberOfOperations次，每次转速超过revolutions，则触发防劫
     *
     * @param revolutions        限制的转速
     * @param numberOfOperations 踩油门次数
     * @param completeTime       限定的时间
     * @return
     * @throws IOException
     */
    public Observable<Double> accelerometersMonitoring6(int revolutions, int numberOfOperations, int completeTime) throws IOException {
        return OBDStream.getInstance().engSpeedStream()
                .scan((lastSpeed, currentSpeed) -> {
                    logger.debug("accelerometersMonitoring" + lastSpeed + " " + currentSpeed);
                    return (currentSpeed - lastSpeed) > revolutions ? 1.0 : 0.0;
                })
                .map(aDouble -> aDouble > 0)
                .doOnNext(aBoolean1 -> logger.debug("obd.gun3Toggle:" + aBoolean1))
                .filter(hasAcced -> hasAcced)
                .zipWith(OBDStream.getInstance().engSpeedStream(), (hasAcced1, speed) -> speed)
                .take(numberOfOperations)
                .timeout(completeTime, TimeUnit.SECONDS);
    }

    private boolean hasAcc = false;

    public Observable<Double> accelerometersMonitoring(int revolutions, int numberOfOperations, int completeTime) throws IOException {
        return OBDStream.getInstance().engSpeedStream()
                .scan((lastSpeed, currentSpeed) -> {
                    logger.debug("accelerometersMonitoring" + lastSpeed + " " + currentSpeed);
                    hasAcc = (currentSpeed - lastSpeed) >= revolutions;
                    return currentSpeed;
                })
                .doOnNext(aBoolean1 -> logger.debug("obd.gun3Toggle:" + hasAcc))
                .filter(hasAcced -> hasAcc)
                .take(numberOfOperations)
                .timeout(completeTime, TimeUnit.SECONDS);
    }

    public boolean isRobberyTriggered() {
        return robberyTriggered;
    }

    public void setRobberyTriggered(boolean robberyTriggered) {
        this.robberyTriggered = robberyTriggered;
    }

    public void resetRobberyTimes() {
        firstTime = 0;
        lastTime = 0;
        accelerateTimes = 0;
    }

    /**
     * 设置防劫参数
     *
     * @param revolutions        限制的转速
     * @param numberOfOperations 踩油门次数
     * @param completeTime       限定的时间
     */
    private void setRobberyArgs(int revolutions, int numberOfOperations, int completeTime) {
        this.revolutions = revolutions;
        this.numberOfOperations = numberOfOperations;
        this.completeTime = completeTime;
    }

    public static void unlockRobbery(String robberyTrigger) {
        if (robberyTrigger != null) {
            DataFlowFactory.getRobberyMessageFlow().getRobberyTriggerSwitch()
                    .filter(robberyTriggerSwitch -> robberyTriggerSwitch)
                    .subscribe(robberyTriggerSwitch1 -> {
                        if ("0".equals(robberyTrigger)) {
                            CarLock.robbertUnlockCar();
                        }
                    }, throwable -> logger.error("unlockRobbery", throwable));
            DataFlowFactory.getRobberyMessageFlow().saveRobberyTriggerStatus("1".equals(robberyTrigger));
        }
    }
}
