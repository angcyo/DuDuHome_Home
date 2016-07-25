package com.dudu.android.launcher.utils;

import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.Log;

import com.dudu.android.launcher.R;
import com.dudu.commonlib.CommonLib;
import com.dudu.monitor.repo.location.LocationManage;
import com.dudu.weather.WeatherStream;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeatherUtils {

    private static final String TAG = "WeatherUtils";

    private final static String[] WEATHER_STRINGS = new String[]{"晴", "多云",
            "阴", "阵雨", "雷阵雨", "小雨", "中雨", "大雨", "暴雨", "大暴雨", "特大暴雨", "小雪",
            "中雪", "大雪", "阵雪", "暴雪", "雾", "小雨转中雨", "中雨转大雨", "大雨转暴雨", "暴雨转大暴雨",
            "大暴雨转特大暴雨", "小雪转中雪", "中雪转大雪", "大雪转暴雪"};

    private static List<String> weatherList = new ArrayList<String>();

    static {
        weatherList = Arrays.asList(WEATHER_STRINGS);
    }

    public static boolean isNight(long time) {
        SimpleDateFormat df = new SimpleDateFormat("HH", Locale.getDefault());
        String timeStr = df.format(new Date(System.currentTimeMillis()));
        try {
            int timeHour = Integer.parseInt(timeStr);
            return (timeHour >= 18 || timeHour <= 6);
        } catch (NumberFormatException e) {
            Log.v(TAG, e.getMessage() + "");
        }
        return false;
    }

    public static int getWeatherType(String weather) {
        if (TextUtils.isEmpty(weather)) {
            return Constants.NO_VALUE_FLAG;
        }

        int type = weatherList.indexOf(weather);
        if (type == -1) {
            return Constants.NO_VALUE_FLAG;
        }

        return type;
    }

    public static int getWeatherIcon(int type) {
        if (isNight(System.currentTimeMillis()))
            switch (type) {
                case Constants.SUNNY:
                    return R.drawable.weather_night_sunny;
                case Constants.CLOUDY:
                    return R.drawable.weather_night_cloudy;
                case Constants.LIGHT_RAIN:
                case Constants.MODERATE_RAIN:
                case Constants.HEAVY_RAIN:
                case Constants.SHOWER:
                case Constants.STORM:
                    return R.drawable.weather_rain;
                default:
                    break;
            }

        switch (type) {
            case Constants.SUNNY:
                return R.drawable.weather_sunny;
            case Constants.CLOUDY:
                return R.drawable.weather_cloudy;
            case Constants.OVERCAST:
                return R.drawable.weather_overcast;
            case Constants.SHOWER:
                return R.drawable.weather_rain;
            case Constants.THUNDERSHOWER:
                return R.drawable.weather_thunder_shower;
            case Constants.LIGHT_RAIN:
            case Constants.MODERATE_RAIN:
            case Constants.HEAVY_RAIN:
            case Constants.LIGHT_TO_MODERATE_RAIN:
            case Constants.MODERATE_TO_HEAVY_RAIN:
            case Constants.RAIN_TO_STORM:
                return R.drawable.weather_rain;
            case Constants.STORM:
            case Constants.HEAVY_STORM:
            case Constants.SEVERE_STORM:
            case Constants.STORM_TO_HEAVY_STORM:
            case Constants.HEAVY_TO_SEVERE_STORM:
                return R.drawable.weather_storm;
            case Constants.LIGHT_SNOW:
            case Constants.MODERATE_SNOW:
            case Constants.HEAVY_SNOW:
            case Constants.LIGHT_TO_MODERATE_SNOW:
            case Constants.MODERATE_TO_HEAVY_SNOW:
            case Constants.HEAVY_TO_SNOWSTORM:
                return R.drawable.weather_snow;
            case Constants.SNOWSTORM:
                return R.drawable.weather_snow_storm;
            case Constants.SNOW_SHOWER:
                return R.drawable.weather_snow_shower;
            case Constants.FOGGY:
                return R.drawable.weather_foggy;
            default:
                return R.drawable.weather_cloudy;
        }
    }

    /**
     * 天气，大图：240*240
     *
     * */
    public static int getWeatherBigIcon(int type) {
        if (isNight(System.currentTimeMillis()))
            switch (type) {
                case Constants.SUNNY:
                    return R.drawable.weather_night_sunny_big;
                case Constants.CLOUDY:
                    return R.drawable.weather_night_cloudy_big;
                case Constants.LIGHT_RAIN:
                case Constants.MODERATE_RAIN:
                case Constants.HEAVY_RAIN:
                case Constants.SHOWER:
                case Constants.STORM:
                    return R.drawable.weather_rain_small_big;
                default:
                    break;
            }

        switch (type) {
            case Constants.SUNNY:
                return R.drawable.weather_sunny_big;
            case Constants.CLOUDY:
                return R.drawable.weather_cloudy_big;
            case Constants.OVERCAST:
                return R.drawable.weather_overcast_big;
            case Constants.SHOWER:
                return R.drawable.weather_rain_small_big;
            case Constants.THUNDERSHOWER:
                return R.drawable.weather_thunder_shower_big;
            case Constants.LIGHT_RAIN:
            case Constants.MODERATE_RAIN:
            case Constants.HEAVY_RAIN:
            case Constants.LIGHT_TO_MODERATE_RAIN:
            case Constants.MODERATE_TO_HEAVY_RAIN:
            case Constants.RAIN_TO_STORM:
                return R.drawable.weather_rain_small_big;
            case Constants.STORM:
            case Constants.HEAVY_STORM:
            case Constants.SEVERE_STORM:
            case Constants.STORM_TO_HEAVY_STORM:
            case Constants.HEAVY_TO_SEVERE_STORM:
                return R.drawable.weather_rainstorm_big;
            case Constants.LIGHT_SNOW:
            case Constants.MODERATE_SNOW:
            case Constants.HEAVY_SNOW:
            case Constants.LIGHT_TO_MODERATE_SNOW:
            case Constants.MODERATE_TO_HEAVY_SNOW:
            case Constants.HEAVY_TO_SNOWSTORM:
            case Constants.SNOWSTORM:
            case Constants.SNOW_SHOWER:
                return R.drawable.weather_hailstone_big;
            case Constants.FOGGY:
                return R.drawable.weather_wind_big;
            default:
                return R.drawable.weather_cloudy;
        }
    }

    public static String getCurrentCity() {
        String currentCity = "";
        if (LocationManage.getInstance().getCurrentLocation() != null) {
            if (!TextUtils.isEmpty(LocationManage.getInstance().getCurrentLocation().getCity())) {
                currentCity = LocationManage.getInstance().getCurrentLocation().getCity();
            } else {
                currentCity = WeatherStream.getInstance().getCity();
            }
        }

        return currentCity;
    }

    public static int daysBetween(Date start, Date end) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            start = sdf.parse(sdf.format(start));
            end = sdf.parse(sdf.format(end));
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            long time1 = cal.getTimeInMillis();
            cal.setTime(end);
            long time2 = cal.getTimeInMillis();
            long between_days = (time2 - time1) / (1000 * 3600 * 24);

            return Integer.parseInt(String.valueOf(between_days));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * @param province 需要处理的字符串
     * @return 过滤一些末尾结束的文字(省)，避免查询不到结果
     */
    public static String getQueryProvince(String province) {

        if (!TextUtils.isEmpty(province)) {
            if (province.endsWith("省")) {
                province = province.substring(0, province.length() - 1);
            }
        }

        return province;
    }

    public static String getQueryCity(String city) {
        String target = "";

        if (!TextUtils.isEmpty(city)) {
            if (city.length() > 2) {
                if (city.endsWith("县")) {
                    target = city.substring(0, city.length() - 1);
                } else if (city.endsWith("市")) {
                    target = city.substring(0, city.length() - 1);
                } else {
                    target = city;
                }
            } else {
                target = city;
            }
        }

        return target;
    }

    /**
     * @param timeStr 需要处理的字符串 eg:20150915
     * @return 过滤一些末尾结束的文字(市，县)，避免查询不到结果
     */
    public static String getQueryTime(String timeStr) {
        String target;

        SimpleDateFormat targetFormat = new SimpleDateFormat("yyyyMMdd");

        if (!TextUtils.isEmpty(timeStr)) {
            target = timeStr;
        } else {
            target = targetFormat.format(new Date());
        }

        return target;
    }

    public static String getDate(int day) {
        Date date = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd");
        String nowDate = sf.format(date);
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(sf.parse(nowDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        cal.add(Calendar.DAY_OF_YEAR, day);
        String nextDate = sf.format(cal.getTime());
        return nextDate;
    }


    public static int getWitchDay(String time) {
        Date todayDate = new Date();
        Date queryDate = null;
        try {
            queryDate = new SimpleDateFormat("yyyyMMdd").parse(time);
        } catch (ParseException e) {

        }

        return daysBetween(todayDate, queryDate);
    }

    /**
     * 去除温度单位℃
     * */
    public static int formatWeatherTemp(String temp){
        if(temp.contains("℃")){
            return Integer.valueOf(temp.substring(0,temp.indexOf("℃")));
        }else {
            return 0;
        }
    }

    /**
     *通过省份查找省府，语音查找某省的天气，则返回该省府的
     *
     */
    public static String getCapital(String province) {
        return proCapMap().get(province);
    }

    private static Map<String,String> proCapMap(){
        Map<String, String> map = new HashMap<String, String>();
        XmlResourceParser xrp = CommonLib.getInstance().getContext().getResources().getXml(R.xml.china);

        try {
            while (xrp.getEventType() != XmlResourceParser.END_DOCUMENT) {
                if (xrp.getEventType() == XmlResourceParser.START_TAG) {
                    String tagName = xrp.getName();// 获取标签的名字
                    if (tagName.equals("item")) {
                        String id = xrp.getAttributeValue(null, "id");// 通过属性名来获取属性值
                        String province = xrp.getAttributeValue(1);// 通过属性名来获取属性值
                        String city = xrp.nextText();
                        map.put(province, city);
                    }
                }
                xrp.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }
}
