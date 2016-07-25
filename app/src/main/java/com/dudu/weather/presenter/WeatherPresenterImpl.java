package com.dudu.weather.presenter;

import android.text.TextUtils;

import com.dudu.android.launcher.utils.WeatherUtils;
import com.dudu.weather.model.IWeatherModel;
import com.dudu.weather.model.WeatherModelImpl;
import com.dudu.weather.view.IWeatherView;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.observables.SyncOnSubscribe;
import rx.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/6/30.
 */
public class WeatherPresenterImpl implements IWeatherPresenter {
    IWeatherModel weatherModel;
    IWeatherView weatherView;
    private Subscription subscription;

    public WeatherPresenterImpl(IWeatherView weatherView) {
        this.weatherView = weatherView;
        weatherModel = new WeatherModelImpl();

    }

    @Override
    public void getRecentWeathers(String cityname) {
        weatherModel.getRecentWeathers(cityname)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(baiduWeatherResult -> {
                    if (baiduWeatherResult != null) {
                        if (weatherView != null) {
                            weatherView.getWeatherSucc(baiduWeatherResult);
                            weatherView.hideLoading();
                        }
                    } else {
                        if (weatherView != null) {
                            weatherView.getWeatherFail("getRecentWeathers-null");
                        }
                    }
                }, throwable -> {
                    if (weatherView != null) {
                        weatherView.getWeatherthrowable("getRecentWeathers-throwable="+throwable);
                    }
                });
    }

    @Override
    public void queryCity() {
        subscription = weatherModel.queryCity()
                .onBackpressureBuffer()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onBackpressureDrop()
                .subscribe(city -> {

                    if (!TextUtils.isEmpty(city)) {
                        String cityFormat = WeatherUtils.getQueryCity(city);
                        getRecentWeathers(cityFormat);
                    } else {
                        weatherView.getWeatherFail("queryCity-null");
                    }
                    if (!subscription.isUnsubscribed()) {
                        subscription.unsubscribe();
                    }
                }, throwable -> {
                    weatherView.getWeatherthrowable("queryCity-throwable="+throwable);
                });
    }

    @Override
    public void destroy() {
        weatherView = null;
        if(subscription != null) {
            if (!subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
        }
    }

}
