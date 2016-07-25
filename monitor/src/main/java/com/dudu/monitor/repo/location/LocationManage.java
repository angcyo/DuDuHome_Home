package com.dudu.monitor.repo.location;

import android.content.Context;

import com.amap.api.location.AMapLocation;
import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.utils.thread.ThreadPoolManager;
import com.dudu.monitor.active.ActiveDeviceManage;
import com.dudu.monitor.valueobject.LocationInfo;
import com.dudu.monitor.valueobject.LocationInfoUpload;
import com.dudu.network.NetworkManage;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import tm.dudu.ext.GPSCtl;

/**
 * Created by dengjun on 2015/11/26.
 * Description :
 */
public class LocationManage implements ILocationListener {
    private static final String TAG = "LocationManage";
    private static LocationManage instance = null;

    private ILocation mILocation;

    private List<LocationInfoUpload> locationInfoList;


    private AMapLocation currentLoction; // 当前位置点 未过滤
    private LocationInfo mCurLocation; //当前位置点 过滤后的

    private Logger log;
    private Context mContext;

    private Gson gson;

    public static LocationManage getInstance() {
        if (instance == null) {
            synchronized (LocationManage.class) {
                if (instance == null) {
                    instance = new LocationManage();
                }
            }
        }
        return instance;
    }

    private LocationManage() {
        mILocation = new AmapLocation();
        mILocation.setLocationListener(this);

        locationInfoList = new ArrayList<>();
        log = LoggerFactory.getLogger("lbs.gps");

        gson = new Gson();
    }


    @Override
    public void onLocationResult(Object locationInfo) {
        if (locationInfo instanceof AMapLocation) {
            log.debug("AMapLocation changed");
            currentLoction = (AMapLocation) locationInfo;

        } else if (locationInfo instanceof LocationInfo) {
            log.debug("gpsFilter changed");

            locationInfoList.add(new LocationInfoUpload((LocationInfo) locationInfo));
            mCurLocation = (LocationInfo) locationInfo;
        }
    }

    @Override
    public void onLocationState(int locationState) {

    }

    public AMapLocation getCurrentLocation() {
        return currentLoction;
    }

    public LocationInfo getCurLocation() {
        return mCurLocation;
    }

    public void init(){
        mContext = CommonLib.getInstance().getContext();
        GPSCtl.on();
        mILocation.startLocation(mContext);
    }

    public void release() {
        cancerSendLocation();
        mILocation.stopLocation();
        GPSCtl.off();
    }


    public JSONArray getLocationJSONArray() {
        JSONArray locationInfoArray = null;
        try {
            if (locationInfoList.size() > 0) {
                locationInfoArray = new JSONArray();
                for (LocationInfoUpload locationInfoUpload : locationInfoList) {
//                DuduLog.d("monitor-位置信息："+ gson.toJson(locationInfo).toString() );
                    if (locationInfoUpload != null) {
                        locationInfoArray.put(new JSONObject(gson.toJson(locationInfoUpload)));
                    }
                }
                locationInfoList.clear();
            }
        } catch (Exception e) {
            log.error("monitor-发送位置信息异常：" + e);
        }
        return locationInfoArray;
    }


    public void startSendLocation() {
        log.info("startSendLocation 发送位置信息");
        init();
        ThreadPoolManager.getInstance(TAG).cancelTaskThreads(TAG, true);
        ThreadPoolManager.getInstance(TAG).scheduleAtFixedRate(sendLocationInfoActionThread, 4, 30, TimeUnit.SECONDS);
    }

    private Thread sendLocationInfoActionThread = new Thread() {

        @Override
        public void run() {
            log.debug("sendLocationInfoActionThread.run");
            sendLocationInfoAction();
        }
    };

    public void cancerSendLocation() {
        log.debug("cancerSendLocation");
        ThreadPoolManager.getInstance(TAG).cancelTaskThreads(TAG, true);
        locationInfoList.clear();
    }

    //发送位置数据
    private void sendLocationInfoAction() {
        try {
            JSONArray loactionJsonArray = getLocationJSONArray();
            if (loactionJsonArray != null && ActiveDeviceManage.getInstance().isDeviceActived()){
                log.debug("sendLocationInfoAction 发送位置信息");
                NetworkManage.getInstance().sendMessage(new com.dudu.network.message.LocationInfoUpload(loactionJsonArray));
            }
        } catch (Exception e) {
            log.error("interval.io 异常", e);
        }
    }
}
