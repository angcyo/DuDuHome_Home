package com.dudu.carChecking;

import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.Contacts;
import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.event.Events;
import com.dudu.commonlib.utils.File.SharedPreferencesUtil;
import com.dudu.commonlib.utils.TextVerify;
import com.dudu.monitor.obd.ObdManage;
import com.dudu.obd.ClearFaultResultEvent;
import com.dudu.obd.FaultCodesEvent;
import com.dudu.obd.ShowFaultPageEvent;
import com.dudu.persistence.driving.FaultCode;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.workflow.common.DataFlowFactory;
import com.dudu.workflow.common.ObservableFactory;
import com.dudu.workflow.common.RequestFactory;
import com.dudu.workflow.obd.CarCheckFlow;
import com.dudu.workflow.obd.CarCheckType;
import com.dudu.workflow.obd.FaultCodeFlow;
import com.dudu.workflow.obd.OBDStream;
import com.dudu.workflow.obd.ObdFlow;
import com.dudu.workflow.obd.SpeedFlow;
import com.dudu.workflow.obd.VehicleConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.dudu.workflow.obd.CarCheckType.ABS;
import static com.dudu.workflow.obd.CarCheckType.ECM;
import static com.dudu.workflow.obd.CarCheckType.SRS;
import static com.dudu.workflow.obd.CarCheckType.TCM;

/**
 * Created by lxh on 2016/2/21.
 */
public class CarCheckingProxy {

    private static CarCheckingProxy carCheckingProxy;

    private boolean isCheckingFaults = false;
    private boolean isClearingFault = false;
    private boolean checkingIsPause = false;
    private boolean clearingIsPause = false;
    private boolean isWaitingForClearingFault = false;

    private List<CarCheckType> carCheckTypeList;
    private List<CarCheckType> carCheckTypeCheckedList;
    private List<CarCheckType> clearCarCheckTypeList;

    private Logger logger;

    private Subscription clearSubscription;

    private Subscription execSubscription;
    private Subscription carIsStopSubscription;
    private Subscription setCarTypeSubscription;
    private Subscription requestCarBrandSubscription;
    private Subscription getUserMessageSubscription;
    private Subscription getCarTypeSubscription;
    private Subscription checkingSpeedSubscription;
    private Subscription carIsStopedSubscription;
    private Subscription checkingSpeedToClearingSubscription;
    private Subscription doNextClearingSubscription;
    private boolean isRunning;

    private CarCheckingProxy() {
        init();
    }

    public static CarCheckingProxy getInstance() {
        if (carCheckingProxy == null) {
            carCheckingProxy = new CarCheckingProxy();
        }
        return carCheckingProxy;
    }

    private void init() {
        logger = LoggerFactory.getLogger("car.checking");
        carCheckTypeList = new ArrayList<>();
        carCheckTypeCheckedList = new ArrayList<>();
        clearCarCheckTypeList = new ArrayList<>();
    }

    public void requestCarTypeAndStartCarchecking(boolean fire) {
        logger.debug("requestCarTypeAndStartCarchecking:" + fire);
        SharedPreferencesUtil.putLongValue(CommonLib.getInstance().getContext(), Contacts.CAR_CHECKING_TIME, System.currentTimeMillis());

        if (isCheckingFaults) {
            return;
        }
        isCheckingFaults = true;
        if (requestCarBrandSubscription != null) {
            requestCarBrandSubscription.unsubscribe();
        }
        requestCarBrandSubscription = RequestFactory.getDrivingRequest().getCarBrand()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(requestResponse -> {
                    logger.debug("requestCarTypeAndStartCarchecking.requestResponse " + requestResponse.resultCode);
                    if (requestResponse.resultCode == 0) {
                        SharedPreferencesUtil.putBooleanValue(CommonLib.getInstance().getContext(), Contacts.BINDING_STATE, true);
                        if (requestResponse.result != null && requestResponse.result.audit_state.equals("2")) {
                            DataFlowFactory.getUserMessageFlow().saveCarType(requestResponse.result);
                            checkCarTypeAndStartCarChecking(requestResponse.result.obd_car_no, fire);
                            return;
                        }
                    }
                    startCarCheckingFromDBCarType(fire);
                }, throwable -> {
                    logger.debug("checkCarTypeAndStartCarChecking", throwable);
                    startCarCheckingFromDBCarType(fire);
                });
    }

    private void startCarCheckingFromDBCarType(boolean fire) {
        logger.debug("startCarCheckingFromDBCarType:" + fire);
        if (getUserMessageSubscription != null) {
            getUserMessageSubscription.unsubscribe();
        }
        getUserMessageSubscription = DataFlowFactory.getUserMessageFlow().obtainUserMessage()
                .map(userMessage -> userMessage.getCarType())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(carType -> {
                    logger.debug("startCarCheckingFromDBCarType+carType:" + carType);
                    checkCarTypeAndStartCarChecking(carType, fire);
                }, throwable1 -> {
                    isCheckingFaults = false;
                    EventBus.getDefault().post(new ShowFaultPageEvent(null, ""));
                    if (!fire) {
                        VoiceManagerProxy.getInstance().startSpeaking(
                                CommonLib.getInstance().getContext().getString(R.string.check_faults_fail), TTSType.TTS_DO_NOTHING, false);
                    }
                    logger.error("startChecking", throwable1);
                });
    }

    private void checkCarTypeAndStartCarChecking(long carType, boolean fire) {
        logger.debug("checkCarTypeAndStartCarChecking:" + carType + "; fire:" + fire);
        if (getCarTypeSubscription != null) {
            getCarTypeSubscription.unsubscribe();
        }
        getCarTypeSubscription = OBDStream.getInstance().OBDGetCarType()
                .timeout(20, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(theCarType -> {
                    getCarTypeSubscription.unsubscribe();
                    try {
                        if (carType == Long.valueOf(theCarType)) {
                            startCarCheckingIfCarStop(fire);
                        } else {
                            setCarTypeAndStartCarChecking(carType, fire);
                        }
                    } catch (NumberFormatException e) {
                        setCarTypeAndStartCarChecking(carType, fire);
                    }
                }, throwable -> {
                    getCarTypeSubscription.unsubscribe();
                    isCheckingFaults = false;
                    EventBus.getDefault().post(new ShowFaultPageEvent(null, ""));
                    if (!fire) {
                        VoiceManagerProxy.getInstance().startSpeaking(
                                CommonLib.getInstance().getContext().getString(R.string.check_faults_fail_timeout), TTSType.TTS_DO_NOTHING, false);
                    }
                    logger.error("checkCarTypeAndStartCarChecking", throwable);
                });
        ObdFlow.getCarType();
    }

    private void setCarTypeAndStartCarChecking(long carType, boolean fire) {
        logger.debug("setCarTypeAndStartCarChecking:" + carType + "; fire:" + fire);
        try {
            if (setCarTypeSubscription != null) {
                setCarTypeSubscription.unsubscribe();
            }
            setCarTypeSubscription = OBDStream.getInstance().OBDSetCarType()
                    .timeout(20, TimeUnit.SECONDS)
                    .subscribe(
                            result -> {
                                logger.debug("setCarTypeAndStartCarChecking.result:" + result);
                                if (setCarTypeSubscription != null) {
                                    setCarTypeSubscription.unsubscribe();
                                }
                                if (carType == VehicleConstants.CAR_TYPE_BMW) {

                                }
                                startCarCheckingIfCarStop(fire);
                            },
                            throwable -> {
                                if (setCarTypeSubscription != null) {
                                    setCarTypeSubscription.unsubscribe();
                                }
                                isCheckingFaults = false;
                                EventBus.getDefault().post(new ShowFaultPageEvent(null, ""));
                                if (!fire) {
                                    VoiceManagerProxy.getInstance().startSpeaking(
                                            CommonLib.getInstance().getContext().getString(R.string.check_faults_fail_timeout), TTSType.TTS_DO_NOTHING, false);
                                }
                                logger.error("setCarTypeAndStartCarChecking", throwable);
                            });

        } catch (IOException e) {
            logger.error("setCarTypeAndStartCarChecking", e);
            isCheckingFaults = false;
            EventBus.getDefault().post(new ShowFaultPageEvent(null, ""));
            if (!fire) {
                VoiceManagerProxy.getInstance().startSpeaking(
                        CommonLib.getInstance().getContext().getString(R.string.check_faults_fail), TTSType.TTS_DO_NOTHING, false);
            }
        }
        ObdFlow.setCarType(carType);
    }

    private void startBMWCarChecking(boolean fire) {
        try {
            OBDStream.getInstance().exec("ATROFF");
            if (!fire) {
                VoiceManagerProxy.getInstance().startSpeaking(
                        CommonLib.getInstance().getContext().getString(R.string.check_BMW_faults_after_flamout_fired), TTSType.TTS_DO_NOTHING, false);

                Observable.timer(1, TimeUnit.MINUTES)
                        .subscribeOn(Schedulers.newThread())
                        .subscribe(aLong -> isWaitingForClearingFault = false
                                , throwable -> logger.error("startBMWCarChecking", throwable));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startCarCheckingIfCarStop(boolean fire) {
        logger.debug("startCarCheckingIfCarStop:" + fire);
        try {
            carIsStopSubscription = SpeedFlow.carIsStoped()
                    .doOnNext(aBoolean -> logger.debug("obdString" + aBoolean))
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(stoped -> {
                                startCarCheckingIfCarStoped(stoped, fire);
                            }, throwable -> {
                                isCheckingFaults = false;
                                EventBus.getDefault().post(new ShowFaultPageEvent(null, ""));
                                if (!fire) {
                                    VoiceManagerProxy.getInstance().startSpeaking(
                                            CommonLib.getInstance().getContext().getString(R.string.check_faults_fail), TTSType.TTS_DO_NOTHING, false);
                                }
                                carIsStopSubscription.unsubscribe();
                                logger.error("carIsStoped", throwable);
                            }
                    );
        } catch (Exception e) {
            logger.error("startChecking", e);
            isCheckingFaults = false;
            EventBus.getDefault().post(new ShowFaultPageEvent(null, ""));
            if (!fire) {
                VoiceManagerProxy.getInstance().startSpeaking(
                        CommonLib.getInstance().getContext().getString(R.string.check_faults_fail), TTSType.TTS_DO_NOTHING, false);
            }
        }
    }

    private void startCarCheckingIfCarStoped(boolean stoped, boolean fire) {
        logger.debug("startCarCheckingIfCarStoped:" + stoped + "; fire:" + fire);
        carIsStopSubscription.unsubscribe();
        if (stoped) {
            startCarCheckingInOrder(false, new CarCheckType[]{CarCheckType.ECM, CarCheckType.TCM, CarCheckType.ABS, CarCheckType.SRS});
        } else {
            isCheckingFaults = false;
            EventBus.getDefault().post(new ShowFaultPageEvent(null, ""));
            if (!fire) {
                VoiceManagerProxy.getInstance().startSpeaking(
                        CommonLib.getInstance().getContext().getString(R.string.check_after_stop), TTSType.TTS_DO_NOTHING, false);
            }
        }
    }

    private void startCarCheckingInOrder(boolean afterClearCode, CarCheckType... carCheckTypes) {
        logger.debug("startCarCheckingInOrder:" + afterClearCode);
        isCheckingFaults = true;
        initCheckingTypes(carCheckTypes);
        EventBus.getDefault().post(new Events.CarCheckingStartEvent());
        checkToStartNextChecking(afterClearCode);
    }

    private void startCheckingIfStoped(boolean afterClearCode) {
        if (checkingSpeedSubscription != null) {
            checkingSpeedSubscription.unsubscribe();
        }
        try {
            checkingSpeedSubscription = SpeedFlow.carStop()
                    .subscribe(stoped -> {
                        if (stoped && isCheckingFaults && checkingIsPause) {
                            logger.debug("checkingSpeedSubscription.startNextTypeChecking");
                            checkingIsPause = false;
                            startNextTypeChecking(afterClearCode);
                        }
                    }, throwable -> logger.error("checkingSpeedSubscription", throwable));
        } catch (IOException e) {
            logger.error("startCheckingIfStoped", e);
            isCheckingFaults = false;
            EventBus.getDefault().post(new ShowFaultPageEvent(null, ""));
        }
    }

    private void initCheckingTypes(CarCheckType... carCheckTypes) {
        logger.debug("subsriberInitChecking");
        carCheckTypeList.clear();
        for (CarCheckType carCheckType : carCheckTypes) {
            carCheckTypeList.add(carCheckType);
        }
        carCheckTypeCheckedList.clear();
    }

    private void checkToStartNextChecking(boolean afterClearCode) {
        logger.debug("subsriberNextChecking.afterClearCode:" + afterClearCode);
        if (carCheckTypeList.size() <= 0) {
            finishCarChecking(afterClearCode);
            return;
        }
        try {
            if (carIsStopedSubscription != null) {
                carIsStopedSubscription.unsubscribe();
            }
            carIsStopedSubscription = SpeedFlow.carIsStoped()
                    .subscribe(stoped -> {
                        logger.debug("carIsStopedSubscription:" + stoped);
                        if (stoped) {
                            startNextTypeChecking(afterClearCode);
                        } else {
                            checkingIsPause = true;
                            if (!afterClearCode) {
                                VoiceManagerProxy.getInstance().startSpeaking(
                                        CommonLib.getInstance().getContext().getString(R.string.check_cartype_pause), TTSType.TTS_DO_NOTHING, false);
                            } else {
                                VoiceManagerProxy.getInstance().startSpeaking(
                                        CommonLib.getInstance().getContext().getString(R.string.clear_cartype_pause), TTSType.TTS_DO_NOTHING, false);
                            }
                            startCheckingIfStoped(afterClearCode);
                        }
                    }, throwable -> {
                        checkingIsPause = false;
                        logger.error("carIsStopedSubscription", throwable);
                    });
        } catch (IOException e) {
            logger.error("checkToStartNextChecking", e);
            isCheckingFaults = false;
            EventBus.getDefault().post(new ShowFaultPageEvent(null, ""));
        }
    }

    private void startNextTypeChecking(boolean afterClearCode) {
        logger.debug("startNextTypeChecking.afterClearCode:" + afterClearCode);
        CarCheckType nextCheckType = carCheckTypeList.get(0);
        carCheckTypeList.remove(0);
        if (nextCheckType != null) {
            if (!afterClearCode) {
                EventBus.getDefault().post(new FaultCodesEvent(nextCheckType, isRunning ? FaultCodesEvent.CHECKING_RESUME : FaultCodesEvent.CHECK_CODES_START, FaultCodesEvent.CHECK_CODES_RESULT_NO_CODES));
            }
            final CarCheckType finalNextCheckType = nextCheckType;
            Observable.timer(1, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(aLong -> {
                        try {
                            CarCheckFlow.startCarCheck(finalNextCheckType);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }, throwable -> logger.error("startNextTypeChecking", throwable));

            try {
                logger.debug("subsriberNextChecking:" + nextCheckType);
                switch (nextCheckType) {
                    case ECM:
                        execSubscription = subscribeCheckingType(ObservableFactory.engineFailed(), ECM, afterClearCode);
                        break;
                    case TCM:
                        execSubscription = subscribeCheckingType(ObservableFactory.gearboxFailed(), TCM, afterClearCode);
                        break;
                    case ABS:
                        execSubscription = subscribeCheckingType(ObservableFactory.ABSFailed(), ABS, afterClearCode);
                        break;
                    case SRS:
                        execSubscription = subscribeCheckingType(ObservableFactory.SRSFailed(), SRS, afterClearCode);
                        break;
                }
            } catch (IOException e) {
                logger.error("startNextTypeChecking", e);
                isCheckingFaults = false;
                EventBus.getDefault().post(new ShowFaultPageEvent(null, ""));
            }
        } else {
            isCheckingFaults = false;
            EventBus.getDefault().post(new ShowFaultPageEvent(null, ""));
        }
    }

    private Subscription subscribeCheckingType(Observable<String> observable, CarCheckType currentType, boolean afterClear) {
        logger.debug("startNextChecking:" + currentType);
        return observable
                .doOnNext(s -> logger.debug("startNextChecking:" + s))
                .timeout(ObdManage.READ_FAULT_TIME, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .subscribe(codes -> {
                    logger.debug("startNextChecking:" + codes + " " + currentType);
                    finishLastCheckingAndStartNextChecking(codes, currentType, afterClear);
                }, throwable -> {
                    logger.error("startNextChecking", throwable);
                    finishLastCheckingAndStartNextChecking(null, currentType, afterClear);
                });
    }

    private void finishLastCheckingAndStartNextChecking(String faultCodes, CarCheckType currentType, boolean afterClear) {
        logger.debug("finishLastCheckingAndStartNextChecking:" + faultCodes + " " + currentType + " " + afterClear);
        boolean noFault = TextVerify.isEmpty(faultCodes) || faultCodes.trim().endsWith(VehicleConstants.VEHICLE_NULL);
        boolean notSupport = !TextVerify.isEmpty(faultCodes) && faultCodes.trim().endsWith(VehicleConstants.VEHICLE_SUPPORT);
        boolean error = !TextVerify.isEmpty(faultCodes) && faultCodes.trim().endsWith(VehicleConstants.VEHICLE_ERROR);
        isRunning = !TextVerify.isEmpty(faultCodes) && faultCodes.trim().endsWith(VehicleConstants.VEHICLE_CONDITIONNOTMET);
        int result = isRunning ? FaultCodesEvent.CHECK_CAR_IS_RUNNING :
                (notSupport ? FaultCodesEvent.CHECK_CODES_RESULT_NOT_SUPPORT :
                        (error ? FaultCodesEvent.CHECK_CODES_RESULT_ERROR :
                                (noFault ? FaultCodesEvent.CHECK_CODES_RESULT_NO_CODES : FaultCodesEvent.CHECK_CODES_RESULT_HAS_CODES)));
        execSubscription.unsubscribe();
        if (!afterClear) {
            EventBus.getDefault().post(new FaultCodesEvent(currentType, FaultCodesEvent.CHECK_CODES_STOP, result));
        }
        if (!isRunning) {
            carCheckTypeCheckedList.add(currentType);
            checkToStartNextChecking(afterClear);
        } else {
            carCheckTypeList.add(0, currentType);
            checkingIsPause = true;
            if (!afterClear) {
                VoiceManagerProxy.getInstance().startSpeaking(
                        CommonLib.getInstance().getContext().getString(R.string.check_cartype_pause), TTSType.TTS_DO_NOTHING, false);
            } else {
                VoiceManagerProxy.getInstance().startSpeaking(
                        CommonLib.getInstance().getContext().getString(R.string.clear_cartype_pause), TTSType.TTS_DO_NOTHING, false);
            }
            startCheckingIfStoped(afterClear);
        }
    }

    private void finishCarChecking(boolean afterClear) {
        logger.debug("finishCarChecking:" + afterClear);
        DataFlowFactory.getDrivingFlow()
                .getAllFaultCodes()
                .timeout(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .subscribe(faultCodeList -> {
                    logger.debug("getDefaultConfig carChecking is Over" + faultCodeList.size());
                    isCheckingFaults = false;
                    if (!afterClear) {
                        showCheckingError(faultCodeList);
                    } else {
                        if (faultCodeList.size() > 0) {
                            EventBus.getDefault().post(new ClearFaultResultEvent(ClearFaultResultEvent.CLEAR_HAS_CODES));
                        } else {
                            EventBus.getDefault().post(new ClearFaultResultEvent(ClearFaultResultEvent.CLEAR_OK));
                        }
                    }
                }, throwable -> {
                    isCheckingFaults = false;
                    if (afterClear) {
                        EventBus.getDefault().post(new ClearFaultResultEvent(ClearFaultResultEvent.CLEAR_HAS_CODES));
                    } else {
                        EventBus.getDefault().post(new ShowFaultPageEvent(null, ""));
                    }
                    logger.error("startNextChecking", throwable);
                });

    }

    public void cancelChecking() {
        logger.debug("cancelChecking");
        if (getUserMessageSubscription != null) {
            getUserMessageSubscription.unsubscribe();
        }
        if (getCarTypeSubscription != null) {
            getCarTypeSubscription.unsubscribe();
        }
        if (requestCarBrandSubscription != null) {
            requestCarBrandSubscription.unsubscribe();
        }
        if (setCarTypeSubscription != null) {
            setCarTypeSubscription.unsubscribe();
        }
        if (carIsStopSubscription != null && !carIsStopSubscription.isUnsubscribed()) {
            carIsStopSubscription.unsubscribe();
        }
        if (execSubscription != null && !execSubscription.isUnsubscribed()) {
            execSubscription.unsubscribe();
        }
        if (checkingSpeedSubscription != null) {
            checkingSpeedSubscription.unsubscribe();
        }
        if (carIsStopedSubscription != null) {
            carIsStopedSubscription.unsubscribe();
        }
        isCheckingFaults = false;
        checkingIsPause = false;
    }

    public void clearFault(CarCheckType... carCheckTypes) {
        logger.debug("clearFault:" + carCheckTypes.toString());
        try {
            if (isClearingFault) {
                logger.debug("isClearingFault");
                return;
            }
            initClearCheckingList(carCheckTypes);
            if (clearCarCheckTypeList.size() > 0) {
                isClearingFault = true;
                checkToStartNextClearing(carCheckTypes);
            }
        } catch (Exception e) {
            logger.error("carChecking error ", e);
            isClearingFault = false;
        }
    }

    private void clearingFaultsIfStop() {
        if (checkingSpeedToClearingSubscription != null) {
            checkingSpeedToClearingSubscription.unsubscribe();
        }
        try {
            checkingSpeedToClearingSubscription = SpeedFlow.carStop()
                    .subscribe(stoped -> {
                        if (stoped && isClearingFault && clearingIsPause) {
                            logger.debug("checkingSpeedToClearingSubscription.doNextClearing:");
                            clearingIsPause = false;
                            doNextClearing();
                        }
                    }, throwable -> logger.error("checkingSpeedToClearingSubscription", throwable));
        } catch (IOException e) {
            logger.error("clearingFaultsIfStop", e);
            isClearingFault = false;
        }
    }

    private void initClearCheckingList(CarCheckType... carCheckTypes) {
        logger.debug("initClearCheckingList:" + carCheckTypes.toString());
        clearCarCheckTypeList.clear();
        for (CarCheckType carCheckType : carCheckTypes) {
            clearCarCheckTypeList.add(carCheckType);
        }
    }

    private void checkToStartNextClearing(CarCheckType... carCheckTypes) throws IOException {
        logger.debug("subsriberNextClearChecking");
        if (clearCarCheckTypeList.size() <= 0) {
            logger.debug("clearCarCheckTypeList <= 0");
            isClearingFault = false;
            startCarCheckingInOrder(true, carCheckTypes);
            return;
        }
        if (doNextClearingSubscription != null) {
            doNextClearingSubscription.unsubscribe();
        }
        doNextClearingSubscription = SpeedFlow.carIsStoped()
                .subscribe(stoped -> {
                    logger.debug("checkingSpeedToClearingSubscription.stoped:" + stoped);
                    if (stoped) {
                        doNextClearing(carCheckTypes);
                    } else {
                        clearingIsPause = true;
                        VoiceManagerProxy.getInstance().startSpeaking(
                                CommonLib.getInstance().getContext().getString(R.string.clear_cartype_pause), TTSType.TTS_DO_NOTHING, false);
                        clearingFaultsIfStop();
                    }
                }, throwable -> {
                    logger.error("doNextClearingSubscription", throwable);
                    isClearingFault = false;
                });
    }

    private void doNextClearing(CarCheckType... carCheckTypes) {
        logger.debug("doNextClearing");
        CarCheckType type = clearCarCheckTypeList.get(0);
        clearCarCheckTypeList.remove(0);
        try {
            clearSubscription = startNextClearCarCheckError(ObservableFactory.getCarCheckFlow().getFaultClear(type), type, carCheckTypes);
            CarCheckFlow.clearCarCheckError(type);
        } catch (IOException e) {
            logger.error("doNextClearing", e);
            isClearingFault = false;
        }
    }

    private Subscription startNextClearCarCheckError(Observable<String> observable, CarCheckType currentType, CarCheckType... carCheckTypes) {
        logger.debug("startNextClearCarCheckError:");
        return observable
                .timeout(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    logger.debug(result);
                    clearSubscription.unsubscribe();
                    try {
                        if (result.endsWith(VehicleConstants.VEHICLE_OK)) {
                            checkToStartNextClearing(carCheckTypes);
                        } else if (result.endsWith(VehicleConstants.VEHICLE_ERROR)) {
                            clearCarCheckTypeList.clear();
                            checkToStartNextClearing(carCheckTypes);
                        } else if (result.endsWith(VehicleConstants.VEHICLE_CONDITIONNOTMET)) {
                            clearingIsPause = true;
                            clearCarCheckTypeList.add(currentType);
                            VoiceManagerProxy.getInstance().startSpeaking(
                                    CommonLib.getInstance().getContext().getString(R.string.clear_cartype_pause), TTSType.TTS_DO_NOTHING, false);
                            clearingFaultsIfStop();
                        } else {
                            isClearingFault = false;
                            clearCarCheckTypeList.clear();
                            EventBus.getDefault().post(new ClearFaultResultEvent(ClearFaultResultEvent.CLEAR_NOT_SUPPORT));
                        }
                    } catch (IOException e) {
                        isClearingFault = false;
                        logger.error("startNextClearCarCheckError", e);
                    }
                }, throwable -> {
                    clearSubscription.unsubscribe();
                    isClearingFault = false;
                    EventBus.getDefault().post(new ClearFaultResultEvent(ClearFaultResultEvent.CLEAR_ERROR));
                    logger.error("startNextClearCarCheckError", throwable);
                });
    }

    private void showCheckingError(List<FaultCode> faultCodeList) {
        logger.debug("showCheckingError:" + faultCodeList.toArray().toString());
        String[] faultCodeTypes = new String[faultCodeList.size()];
        for (int i = 0; i < faultCodeList.size(); i++) {
            FaultCode faultCode = faultCodeList.get(i);
            faultCodeTypes[i] = FaultCodeFlow.getVehicleConstants(faultCode.getCarCheckType());
        }
        logger.debug("carChecking showCheckingError {}", faultCodeTypes.length);
        if (faultCodeList.size() > 0) {
            StringBuffer playText = new StringBuffer();
            for (String faultCodeType : faultCodeTypes) {
                playText.append(FaultCodeFlow.getShowConstants(faultCodeType));
                playText.append("、");
            }
            EventBus.getDefault().post(new ShowFaultPageEvent(faultCodeTypes, playText.toString()));
        } else {
            VoiceManagerProxy.getInstance().startSpeaking(
                    CommonLib.getInstance().getContext().getString(R.string.checking_cartype_health), TTSType.TTS_DO_NOTHING, false);
            EventBus.getDefault().post(new ShowFaultPageEvent(null, ""));
        }
    }

    public void cancelClearFaults() {
        if (clearSubscription != null && !clearSubscription.isUnsubscribed()) {
            clearSubscription.unsubscribe();
        }
        if (checkingSpeedToClearingSubscription != null) {
            checkingSpeedToClearingSubscription.unsubscribe();
        }
        if (execSubscription != null && !execSubscription.isUnsubscribed()) {
            execSubscription.unsubscribe();
        }
        if (doNextClearingSubscription != null) {
            doNextClearingSubscription.unsubscribe();
        }
        isClearingFault = false;
        clearingIsPause = false;
    }

    public boolean isCheckingFaults() {
        return isCheckingFaults;
    }

    public boolean isClearingFault() {
        return isClearingFault;
    }

    public List<CarCheckType> getCarCheckTypeCheckedList() {
        return carCheckTypeCheckedList;
    }

    public List<CarCheckType> getCarCheckTypeList() {
        return carCheckTypeList;
    }

}
