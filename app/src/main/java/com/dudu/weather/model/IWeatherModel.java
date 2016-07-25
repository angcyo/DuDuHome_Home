package com.dudu.weather.model;

import com.dudu.rest.model.BaiduWeatherResponse;

import rx.Observable;

/**
 * Created by Administrator on 2016/6/29.
 */
public interface IWeatherModel {

    public Observable<BaiduWeatherResponse.BaiduWeatherResult> getRecentWeathers(String cityName);

    public Observable<String> queryCity();

}
