package com.dudu.weather.view;

import com.dudu.rest.model.BaiduWeatherResponse;

/**
 * Created by Administrator on 2016/6/29.
 */
public interface IWeatherView {

    void showLoading();

    void hideLoading();

    void getWeatherSucc(String result);

    void getWeatherSucc(BaiduWeatherResponse.BaiduWeatherResult result);

    void getWeatherFail(String msg);

    void getWeatherthrowable(String msg);
}
