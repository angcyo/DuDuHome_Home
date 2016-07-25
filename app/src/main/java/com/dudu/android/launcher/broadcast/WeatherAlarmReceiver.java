package com.dudu.android.launcher.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dudu.commonlib.utils.File.KeyConstants;

public class WeatherAlarmReceiver extends BroadcastReceiver {
    public WeatherAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(!KeyConstants.is_agree_disclaimer)
            return;
//        WeatherUtils.requestWeather(context);
    }
}
