package com.dudu.commonlib.event;

/**
 * Created by robi on 2016-06-04 11:08.
 */
public class DownEvent {
    public static final int STATE_NO = -1;
    public static final int STATE_NOR = 0;
    public static final int STATE_ERROR = 1;
    public static final int STATE_OK = 2;

    private int state;

    public DownEvent(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }
}
