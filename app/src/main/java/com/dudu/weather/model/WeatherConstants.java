package com.dudu.weather.model;

/**
 * Created by Administrator on 2016/7/2.
 */
public class WeatherConstants {

    public static final int BEFORE_YESTERDAY_WEATHER = -2;//前天天气

    public static final int YESTERDAY_WEATHER = -1;//昨天天气

    public static final int TODAY_WEATHER = 0;//今天天气

    public static final int TOMORROW_WEATHER = 1;//明天天气

    public static final int AFTER_TOMORROW_WEATHER = 2;//后天天气


    public static final int REQUEST_WEATHER_TIME_OUT_PERIOD = 15 * 1000;//十秒无返回则为请求失败

}
