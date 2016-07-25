package com.dudu.monitor.event;

import com.dudu.workflow.tpms.TPMSInfo;

/**
 * Created by robi on 2016-06-28 23:42.
 */
public class TireExceptionEvent {
    public final TPMSInfo mInfo;
    public boolean isException;

    public TireExceptionEvent(final TPMSInfo info, boolean isException) {
        this.mInfo = info;
        this.isException = isException;
    }
}
