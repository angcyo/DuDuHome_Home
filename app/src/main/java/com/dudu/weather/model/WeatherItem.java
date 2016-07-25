package com.dudu.weather.model;

/**
 * Created by Administrator on 2016/7/1.
 */
public class WeatherItem {

    public String week = "";//周几

    public String type = "";//天气类型

    public WeatherItem() {
        super();
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public String getWeek() {
        return week;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
