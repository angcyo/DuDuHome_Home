package com.dudu.drivevideo.frontcamera.event;

/**
 * Created by robi on 2016/6/3.
 * Description :
 */
public class TakePhotoEvent {
    public static final int TAKE_ING = 1;
    public static final int TAKE_END = 0;

    private int state;

    public TakePhotoEvent(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
