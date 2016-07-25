package com.dudu.weather;

import android.text.TextUtils;

import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.weather.LocalWeatherForecastResult;
import com.amap.api.services.weather.LocalWeatherLive;
import com.amap.api.services.weather.LocalWeatherLiveResult;
import com.amap.api.services.weather.WeatherSearch;
import com.amap.api.services.weather.WeatherSearchQuery;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.utils.NetworkUtils;
import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.utils.thread.ThreadPoolManager;
import com.dudu.monitor.Monitor;
import com.dudu.monitor.repo.location.LocationManage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.subjects.ReplaySubject;

/**
 * Created by lxh on 2016/1/22.
 */
public class WeatherStream {

    private static WeatherStream mInstance;
    private final Logger mLogger;

    private ScheduledExecutorService requestWeatherExecutor;

    private static ReplaySubject<LocalWeatherLive> mLiveWeatherSubject = ReplaySubject.create();

    private static ReplaySubject<WeatherInfo> mLocalWeatherForecastSub = ReplaySubject.create();

    private WeatherSearch mSearch;

    private boolean hasWeather = false;
    private volatile boolean canQuery = true;
    private Object queryLock;
//    private Subscription weatherLiveFlowSubscription;

    private Subscription requestWeatherSub;
    private WeatherSearchQuery mLiveQuery;
    private String mCity;

    private static final String TAG = "WeatherStream";

    private int reQueryCount = 0;

    public WeatherStream() {
        mLogger = LoggerFactory.getLogger("weather.flow");
        mSearch = new WeatherSearch(LauncherApplication.getContext());
        geocoderSearch = new GeocodeSearch(CommonLib.getInstance().getContext());
        geocoderSearch.setOnGeocodeSearchListener(geocodeSearchListener);
        queryLock = new Object();
    }

    public static Observable<LocalWeatherLive> getLiveWeatherStream() {
        return mLiveWeatherSubject.asObservable();
    }

    public static Observable<WeatherInfo> getForecastWeather() {
        return mLocalWeatherForecastSub.asObservable();
    }

    public static WeatherStream getInstance() {
        if (mInstance == null) {
            mInstance = new WeatherStream();
        }
        return mInstance;
    }

    private Thread requeatWeatherThread = new Thread() {

        @Override
        public void run() {
            mLogger.debug("requeatWeatherThread.run");
            weatherLiveFlow();
        }
    };

    public String getCity() {
        return city;
    }

    public void setCity(String city) {

        this.city = city;
    }

    private String city = "";

    private LatLonPoint latLonPoint;

    private GeocodeSearch geocoderSearch;


    public void startService() {
        mLogger.debug("startService");
        ThreadPoolManager.getInstance(TAG).cancelTaskThreads(TAG, true);
        ThreadPoolManager.getInstance(TAG).scheduleAtFixedRate(requeatWeatherThread, 20, 30 * 60, TimeUnit.SECONDS);
    }

//    public void startService() {
//
//        if (weatherLiveFlowSubscription != null && !weatherLiveFlowSubscription.isUnsubscribed()) {
//            return;
//        }
//        mLogger.info("interval.io.create WeatherStream startService");
//        weatherLiveFlowSubscription = Observable.interval(20, 30 * 60, TimeUnit.SECONDS, Schedulers.io())
//                .subscribe((l) -> {
//                            mLogger.debug("interval.io weatherLiveFlow");
//                            weatherLiveFlow();
//                        },
//                        throwable -> mLogger.debug("interval.io startService", throwable));
//    }
//
//    public void stopService() {
//        if (weatherLiveFlowSubscription != null) {
//            weatherLiveFlowSubscription.unsubscribe();
//            weatherLiveFlowSubscription = null;
//        }
//    }

    public void stopService() {
        ThreadPoolManager.getInstance(TAG).cancelTaskThreads(TAG, true);
    }

    private void weatherLiveFlow() {
        mLogger.debug("weatherLiveFlow");
        if (canQuery) {
            canQuery = false;
            mLogger.debug("canQuery");
            if (requestWeatherSub != null) {
                requestWeatherSub.unsubscribe();
                requestWeatherSub = null;
            }
            requestWeatherSub = Observable.timer(20, TimeUnit.SECONDS).subscribe(aLong -> {
                canQuery = true;

                if (!hasWeather && reQueryCount < 5) {
                    mLogger.debug("request weather time out");
                    weatherLiveFlow();
                    reQueryCount++;
                }
            }, throwable -> mLogger.error("weatherLiveFlow", throwable));

            if (!NetworkUtils.isNetworkConnected(CommonLib.getInstance().getContext())) {
                mLogger.debug("NetworkUtils.isNetworkConnected:false");
                canQuery = true;
                return;
            }

            city = getCurrentCity();
            if (TextUtils.isEmpty(city)) {
                mLogger.debug("city is empty");
                queryCity();
                return;
            }
            queryWeather(city);
        }
    }

    private WeatherSearch.OnWeatherSearchListener mWeatherListener = new WeatherSearch.OnWeatherSearchListener() {
        @Override
        public void onWeatherLiveSearched(LocalWeatherLiveResult localWeatherLiveResult, int i) {
            if (i == 1000) {
                mLogger.debug("weather-rx onWeatherLiveSearched ");
                hasWeather = true;
                mLiveWeatherSubject.onNext(localWeatherLiveResult.getLiveResult());
            }
        }

        @Override
        public void onWeatherForecastSearched(LocalWeatherForecastResult localWeatherForecastResult, int i) {

        }
    };

    private String getCurrentCity() {

        if (LocationManage.getInstance().getCurrentLocation() != null) {
            return LocationManage.getInstance().getCurrentLocation().getCity();
        }
        return null;
    }

    private void queryWeather(String city) {
        mLogger.debug("queryWeather");
        getLiveQuery(city);
        mSearch.setOnWeatherSearchListener(mWeatherListener);
        mSearch.setQuery(mLiveQuery);
        mSearch.searchWeatherAsyn();
    }

    private void getLiveQuery(String city) {
        if (city != null && !city.equals(mCity)) {
            mCity = city;
            mLiveQuery = new WeatherSearchQuery(city, WeatherSearchQuery.WEATHER_TYPE_LIVE);
        }
    }

    private void queryCity() {
        if (LocationManage.getInstance().getCurrentLocation() != null) {
            mLogger.debug("queryCity");
            latLonPoint = new LatLonPoint(Monitor.getInstance().getCurrentLocation().getLatitude(),
                    Monitor.getInstance().getCurrentLocation().getLongitude());

            RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200,
                    GeocodeSearch.AMAP);
            geocoderSearch.getFromLocationAsyn(query);
        }
    }

    private GeocodeSearch.OnGeocodeSearchListener geocodeSearchListener = new GeocodeSearch.OnGeocodeSearchListener() {
        @Override
        public void onRegeocodeSearched(RegeocodeResult result, int rCode) {

            if (rCode == 1000) {
                if (result != null && result.getRegeocodeAddress() != null
                        && !TextUtils.isEmpty(result.getRegeocodeAddress().getFormatAddress())) {
                    mLogger.debug("query city success");
                    city = result.getRegeocodeAddress().getCity();
                    queryWeather(city);
                }
            }
        }

        @Override
        public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

        }
    };

}