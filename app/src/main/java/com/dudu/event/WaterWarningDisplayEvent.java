package com.dudu.event;

/**
 * Created by Robert on 2016/7/6.
 */
public class WaterWarningDisplayEvent {

    public WaterWarningDisplayEvent(boolean mNeedClose) {
        this.mNeedClose = mNeedClose;
    }

    public boolean ismNeedClose() {
        return mNeedClose;
    }

    public void setmNeedClose(boolean mNeedClose) {
        this.mNeedClose = mNeedClose;
    }

    private boolean mNeedClose;

}
