package com.dudu.workflow.common;

import com.dudu.persistence.RobberyMessage.RealRobberyMessageDataService;
import com.dudu.persistence.UserMessage.RealUserMessageDataService;
import com.dudu.persistence.app.RealmAppVersionService;
import com.dudu.persistence.driving.RealmFaultCodeService;
import com.dudu.persistence.switchmessage.RealmSwitchMessageService;
import com.dudu.workflow.app.LocalAppVersionFlow;
import com.dudu.workflow.driving.DrivingFlow;
import com.dudu.workflow.robbery.RobberyMessageFlow;
import com.dudu.workflow.switchmessage.SwitchDataFlow;
import com.dudu.workflow.userMessage.UserMessageFlow;

/**
 * Created by Administrator on 2016/2/17.
 */
public class DataFlowFactory {
    private static SwitchDataFlow switchDataFlow;
    private static DrivingFlow drivingFlow;
    private static LocalAppVersionFlow localAppVersionFlow;

    private static UserMessageFlow userMessageFlow;

    private static RobberyMessageFlow robberyMessageFlow;

    public static void init() {
        userMessageFlow = new UserMessageFlow(new RealUserMessageDataService());
        switchDataFlow = new SwitchDataFlow(new RealmSwitchMessageService());
        drivingFlow = new DrivingFlow();
        drivingFlow.setFaultCodeService(new RealmFaultCodeService());
        localAppVersionFlow = new LocalAppVersionFlow(new RealmAppVersionService());
        robberyMessageFlow = new RobberyMessageFlow(new RealRobberyMessageDataService());
    }

    public static SwitchDataFlow getSwitchDataFlow() {
        if(switchDataFlow==null){
            switchDataFlow = new SwitchDataFlow(new RealmSwitchMessageService());
        }
        return switchDataFlow;
    }

    public static DrivingFlow getDrivingFlow() {
        if(drivingFlow==null){
            drivingFlow = new DrivingFlow();
            drivingFlow.setFaultCodeService(new RealmFaultCodeService());
        }
        return drivingFlow;
    }

    public static UserMessageFlow getUserMessageFlow() {
        if(userMessageFlow == null){
            userMessageFlow = new UserMessageFlow(new RealUserMessageDataService());
        }
        return userMessageFlow;
    }

    public static LocalAppVersionFlow getLocalAppVersionFlow() {
        if(localAppVersionFlow==null){
            localAppVersionFlow = new LocalAppVersionFlow(new RealmAppVersionService());
        }
        return localAppVersionFlow;
    }

    public static RobberyMessageFlow getRobberyMessageFlow() {
        if(robberyMessageFlow==null){
            robberyMessageFlow = new RobberyMessageFlow(new RealRobberyMessageDataService());
        }
        return robberyMessageFlow;
    }
}
