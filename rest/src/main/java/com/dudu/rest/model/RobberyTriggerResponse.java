package com.dudu.rest.model;

/**
 * Created by Administrator on 2016/5/23.
 */
public class RobberyTriggerResponse {
    private String obeId;
    private String lon;
    private String lat;
    private String datetime;

    public RobberyTriggerResponse(String obeId, String lon, String lat, String datetime) {
        this.obeId = obeId;
        this.lon = lon;
        this.lat = lat;
        this.datetime = datetime;
    }
}
