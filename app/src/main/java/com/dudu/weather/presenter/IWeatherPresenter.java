package com.dudu.weather.presenter;

/**
 * Created by Administrator on 2016/6/29.
 */
public interface IWeatherPresenter {

    void getRecentWeathers(String cityname);

    void queryCity();

    void destroy();

}
