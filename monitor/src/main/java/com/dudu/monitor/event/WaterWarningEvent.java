package com.dudu.monitor.event;

import com.dudu.monitor.obd.modol.CoolantTemperatureData;

/**
 *  水温告警事件类
 *
 * Created by Robert on 2016/7/4.
 */
public class WaterWarningEvent {

    public CoolantTemperatureData mInfo;

    public WaterWarningEvent(CoolantTemperatureData info) {
        this.mInfo = info;
    }
}
