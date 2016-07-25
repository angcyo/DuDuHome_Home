package com.dudu.drivevideo.spaceguard.event;

/**
 * Created by Robert on 2016/6/15.
 */
public class VideoSpaceUpdateEvent {

    public static final String RECORD_SPACE_UPDATE = "录像存储更新";

    private String msg;

    public VideoSpaceUpdateEvent(String message) {
        this.msg = message;
    }

    public String getUpdateMesasge() {
        return this.msg;
    }

    public void setUpdateMessage(String message) {
        this.msg = message;
    }
}
