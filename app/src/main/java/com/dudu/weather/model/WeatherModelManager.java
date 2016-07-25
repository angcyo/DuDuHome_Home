package com.dudu.weather.model;

import android.text.TextUtils;
import android.util.Log;

import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.dudu.commonlib.CommonLib;
import com.dudu.monitor.Monitor;
import com.dudu.monitor.repo.location.LocationManage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.subjects.ReplaySubject;

/**
 * Created by Administrator on 2016/6/30.
 */
public class WeatherModelManager {

    private static WeatherModelManager mInstance;
    private final Logger mLogger;

    private static ReplaySubject<String> mCitySubject;

    public WeatherModelManager() {
        mLogger = LoggerFactory.getLogger("weather.WeatherModelManager");
        geocoderSearch = new GeocodeSearch(CommonLib.getInstance().getContext());
        geocoderSearch.setOnGeocodeSearchListener(geocodeSearchListener);
    }

    public static Observable<String> queryCity() {
        if (mCitySubject == null) {
            mCitySubject = ReplaySubject.create();
        }
        return mCitySubject.asObservable();
    }

    public static WeatherModelManager getInstance() {
        if (mInstance == null) {
            mInstance = new WeatherModelManager();
        }
        return mInstance;
    }

    private String city = "";

    private LatLonPoint latLonPoint;

    private GeocodeSearch geocoderSearch;


    public void queryCurrentCity() {
        city = getCurrentCity();
        if (TextUtils.isEmpty(city)) {
            mLogger.debug("city is empty");
            getCity();
        } else {
            if (mCitySubject != null)
                mCitySubject.onNext(city);
        }
    }

    private String getCurrentCity() {

        if (LocationManage.getInstance().getCurrentLocation() != null) {
            return LocationManage.getInstance().getCurrentLocation().getCity();
        }
        return null;
    }

    private void getCity() {
        if (Monitor.getInstance().getCurrentLocation() != null) {
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
            Log.e("weather","GeocodeSearch-city="+result.getRegeocodeAddress().getCity());
            if (rCode == 1000) {
                if (result != null && result.getRegeocodeAddress() != null
                        && !TextUtils.isEmpty(result.getRegeocodeAddress().getFormatAddress())) {
                    city = result.getRegeocodeAddress().getCity();
                    mCitySubject.onNext(city);
                }
            }
        }

        @Override
        public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

        }
    };

}