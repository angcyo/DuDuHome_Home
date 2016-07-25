package com.dudu.weather;

/**
 * Created by lxh on 2016-07-09 11:57.
 */
public class WeatherEvent {

    private String city;

    private int witchdate;

    public WeatherEvent(String city, int witchdate) {
        this.city = city;
        this.witchdate = witchdate;
    }

    public String getCity() {
        return city;
    }

    public int getWitchdate() {
        return witchdate;
    }
}
