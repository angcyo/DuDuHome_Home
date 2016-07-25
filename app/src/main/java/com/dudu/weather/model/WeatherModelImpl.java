package com.dudu.weather.model;

import com.dudu.rest.common.RetrofitServiceFactory;
import com.dudu.rest.model.BaiduWeatherResponse;
import com.dudu.rest.service.BaiduApiService;

import rx.Observable;

/**
 * Created by Administrator on 2016/6/29.
 */
public class WeatherModelImpl implements IWeatherModel {

    public WeatherModelImpl(){
        super();
    }
    /**
     * 百度天气：获取过去七天，未来四天，当天天气
     *
     * @param cityName 城市名称
     * @return BaiduWeatherResult
     */
    @Override
    public Observable<BaiduWeatherResponse.BaiduWeatherResult> getRecentWeathers(String cityName) {
        return RetrofitServiceFactory.getBaiduWeatherService().requestWeathers(BaiduApiService.APIKEY_VALUE, cityName)
                .map(baiduWeatherResponse -> baiduWeatherResponse.retData);
    }

    /**
     * 高德定位：获取当前城市
     *
     * @return 城市名称
     */
    @Override
    public Observable<String> queryCity() {
        WeatherModelManager.getInstance().queryCurrentCity();
        return WeatherModelManager.queryCity();
    }
}
