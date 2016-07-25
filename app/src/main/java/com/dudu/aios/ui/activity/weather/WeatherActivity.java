package com.dudu.aios.ui.activity.weather;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dudu.aios.ui.voice.VoiceEvent;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.ActivitiesManager;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.android.launcher.utils.FileUtils;
import com.dudu.android.launcher.utils.WeatherUtils;
import com.dudu.rest.model.BaiduWeatherResponse;
import com.dudu.voice.FloatWindowUtils;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.weather.WeatherEvent;
import com.dudu.weather.model.WeatherConstants;
import com.dudu.weather.model.WeatherItem;
import com.dudu.weather.presenter.IWeatherPresenter;
import com.dudu.weather.presenter.WeatherPresenterImpl;
import com.dudu.weather.view.IWeatherView;
import com.dudu.weather.view.WeatherCurveView;
import com.dudu.weather.view.WeatherListAdapter;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;

/**
 * Created by Administrator on 2016/6/30.
 */
public class WeatherActivity extends FragmentActivity implements View.OnClickListener, IWeatherView, WeatherCurveView.UpdateViewListener {
    private Logger logger;

    private ImageButton backBtn;
    private TextView cityTxt, currTempTxt, rangeTempTxt, typeTxt;
    private TextView dateTxt, weekTxt;
    private ImageView typeIcon;
    private RelativeLayout loadingLayout;
    private SimpleDraweeView loadingView;

    private WeatherCurveView weatherCurveView;
    private RecyclerView recyclerView;
    private WeatherListAdapter adapter;

    private String city = "";//查看哪个城市
    private int whichDay = 0;//默认日期：今天
    private int[] highTemps = new int[7];
    private int[] lowTemps = new int[7];
    private ArrayList<WeatherItem> dataLists = new ArrayList<>();

    private IWeatherPresenter weatherPresenter;
    private Timer timer = new Timer();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fresco.initialize(this);
        setContentView(R.layout.activity_weather);

        logger = LoggerFactory.getLogger("weather.WeatherActivity");
        weatherPresenter = new WeatherPresenterImpl(this);
        initView();
        Intent intent = getIntent();
        city = intent.getStringExtra("city");
        whichDay = intent.getIntExtra("date", WeatherConstants.TODAY_WEATHER);

        setTimeout();
        startAnimLoading();
        requestWeather(city, whichDay);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    private void initView() {
        backBtn = (ImageButton) findViewById(R.id.button_back);
        backBtn.setOnClickListener(this);

        loadingView = (SimpleDraweeView) findViewById(R.id.view_loading);
        loadingLayout = (RelativeLayout) findViewById(R.id.layout_loading);

        cityTxt = (TextView) findViewById(R.id.txt_city);
        currTempTxt = (TextView) findViewById(R.id.txt_curr_temperature);
        rangeTempTxt = (TextView) findViewById(R.id.txt_range_temperature);
        typeTxt = (TextView) findViewById(R.id.txt_weather_type);

        dateTxt = (TextView) findViewById(R.id.txt_date);
        weekTxt = (TextView) findViewById(R.id.txt_week);
        typeIcon = (ImageView) findViewById(R.id.icon_weather_type);

        weatherCurveView = (WeatherCurveView) findViewById(R.id.curve_weather);
        weatherCurveView.setHighTemp(highTemps);
        weatherCurveView.setLowTemp(lowTemps);
        weatherCurveView.invalidate();
        weatherCurveView.setUpdateViewWidthListener(this);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview_weather);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new WeatherListAdapter(weatherCurveView.getWeatherViewWidth(), dataLists);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_back:
                VoiceManagerProxy.getInstance().stopSpeaking();
                VoiceManagerProxy.getInstance().onStop();
                finish();
                break;
        }
    }

    @Override
    public void showLoading() {
        loadingLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        loadingLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    public void getWeatherSucc(String result) {
        logger.debug("#back--" + result);
    }

    @Override
    public void getWeatherSucc(BaiduWeatherResponse.BaiduWeatherResult result) {

        BaiduWeatherResponse.BaiduWeatherResult.TodayResult todayResult = result.today;
        BaiduWeatherResponse.BaiduWeatherResult.ForecastResult[] forecastResults = result.forecast;
        BaiduWeatherResponse.BaiduWeatherResult.HistoryResult[] historyResults = result.history;

        logger.debug("getWeatherSucc-city =" + result.city);

        weatherCurveView.setVisibility(View.VISIBLE);

        if (todayResult != null) {
            dataLists.clear();
            WeatherItem item;
            for (int i = 0; i < historyResults.length; i++) {
                item = new WeatherItem();
                if (i < 3) {
                    item.setWeek(historyResults[i + 4].week);
                    item.setType(historyResults[i + 4].type);
                    highTemps[i] = WeatherUtils.formatWeatherTemp(historyResults[i + 4].hightemp);
                    lowTemps[i] = WeatherUtils.formatWeatherTemp(historyResults[i + 4].lowtemp);
                } else if (i == 3) {
                    item.setWeek(todayResult.week);
                    item.setType(todayResult.type);
                    highTemps[i] = WeatherUtils.formatWeatherTemp(todayResult.hightemp);
                    lowTemps[i] = WeatherUtils.formatWeatherTemp(todayResult.lowtemp);
                } else {
                    item.setWeek(forecastResults[i - 4].week);
                    item.setType(forecastResults[i - 4].type);
                    highTemps[i] = WeatherUtils.formatWeatherTemp(forecastResults[i - 4].hightemp);
                    lowTemps[i] = WeatherUtils.formatWeatherTemp(forecastResults[i - 4].lowtemp);
                }
                dataLists.add(item);
            }

            weatherCurveView.setHighTemp(highTemps);
            weatherCurveView.setLowTemp(lowTemps);
            weatherCurveView.invalidate();
            adapter.setViewWidth(1253);
            adapter.notifyDataSetChanged();

            updateView(result);
        }
    }

    /**
     * 获取天气失败
     */
    @Override
    public void getWeatherFail(String msg) {
        logger.debug(msg);
        requestFail();
    }

    /**
     * 网络未连接或者其他原因
     */
    @Override
    public void getWeatherthrowable(String msg) {
        logger.debug(msg);
        requestFail();
    }

    /**
     * 根据查看的日期更新UI
     */
    private void updateView(BaiduWeatherResponse.BaiduWeatherResult result) {
        BaiduWeatherResponse.BaiduWeatherResult.TodayResult todayResult = result.today;
        BaiduWeatherResponse.BaiduWeatherResult.ForecastResult[] forecastResults = result.forecast;
        BaiduWeatherResponse.BaiduWeatherResult.HistoryResult[] historyResults = result.history;

        String date = "";
        switch (whichDay) {
            case WeatherConstants.BEFORE_YESTERDAY_WEATHER:
                date = getString(R.string.weather_before_yesterday);
                todayResult.hightemp = historyResults[5].hightemp;
                todayResult.lowtemp = historyResults[5].lowtemp;
                todayResult.type = historyResults[5].type;
                todayResult.date = historyResults[5].date;
                todayResult.week = historyResults[5].week;
                todayResult.fengxiang = historyResults[5].fengxiang;
                todayResult.fengli = historyResults[5].fengli;
                rangeTempTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                currTempTxt.setText(todayResult.type);
                typeTxt.setVisibility(View.INVISIBLE);
                break;
            case WeatherConstants.YESTERDAY_WEATHER:
                date = getString(R.string.weather_yesterday);
                todayResult.hightemp = historyResults[6].hightemp;
                todayResult.lowtemp = historyResults[6].lowtemp;
                todayResult.type = historyResults[6].type;
                todayResult.date = historyResults[6].date;
                todayResult.week = historyResults[6].week;
                todayResult.fengxiang = historyResults[6].fengxiang;
                todayResult.fengli = historyResults[6].fengli;
                rangeTempTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                currTempTxt.setText(todayResult.type);
                typeTxt.setVisibility(View.INVISIBLE);
                break;
            case WeatherConstants.TODAY_WEATHER:
                date = getString(R.string.weather_today);
                rangeTempTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                currTempTxt.setText(todayResult.curTemp);
                typeTxt.setVisibility(View.VISIBLE);
                break;
            case WeatherConstants.TOMORROW_WEATHER:
                date = getString(R.string.weather_tomorrow);
                todayResult.hightemp = forecastResults[0].hightemp;
                todayResult.lowtemp = forecastResults[0].lowtemp;
                todayResult.type = forecastResults[0].type;
                todayResult.date = forecastResults[0].date;
                todayResult.week = forecastResults[0].week;
                todayResult.fengxiang = forecastResults[0].fengxiang;
                todayResult.fengli = forecastResults[0].fengli;
                rangeTempTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                currTempTxt.setText(todayResult.type);
                typeTxt.setVisibility(View.INVISIBLE);
                break;
            case WeatherConstants.AFTER_TOMORROW_WEATHER:
                date = getString(R.string.weather_after_tomorrow);
                todayResult.hightemp = forecastResults[1].hightemp;
                todayResult.lowtemp = forecastResults[1].lowtemp;
                todayResult.type = forecastResults[1].type;
                todayResult.date = forecastResults[1].date;
                todayResult.week = forecastResults[1].week;
                todayResult.fengxiang = forecastResults[1].fengxiang;
                todayResult.fengli = forecastResults[1].fengli;
                rangeTempTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                currTempTxt.setText(todayResult.type);
                typeTxt.setVisibility(View.INVISIBLE);
                break;
        }

        if (whichDay != WeatherConstants.TODAY_WEATHER) {
            int length = currTempTxt.getText().toString().length();
            switch (length) {
                case 1:
                case 2:
                    break;
                case 3:
                case 4:
                    currTempTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                    break;
                case 5:
                case 6:
                case 7:
                    currTempTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                    break;
                default:
                    currTempTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                    break;
            }
        }
        cityTxt.setText(getString(R.string.weather_city_date, result.city + " " + date));
        rangeTempTxt.setText(todayResult.lowtemp + "-" + todayResult.hightemp);
        typeTxt.setText(todayResult.type);
        typeIcon.setImageResource(WeatherUtils
                .getWeatherBigIcon(WeatherUtils.getWeatherType(todayResult.type)));
        dateTxt.setText(todayResult.date);
        weekTxt.setText(todayResult.week);

        String mSpeakWord = cityTxt.getText().toString()
                + " \n," + typeTxt.getText().toString()
                + " \n," + getString(R.string.weather_high_temp) + todayResult.hightemp
                + " \n," + getString(R.string.weather_low_temp) + todayResult.lowtemp
                + " \n," + getString(R.string.weather_fengli) + todayResult.fengli
                + " \n," + getString(R.string.weather_fengxiang) + todayResult.fengxiang;

        VoiceManagerProxy.getInstance().startSpeaking(mSpeakWord, TTSType.TTS_START_UNDERSTANDING, false);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        highTemps = null;
        lowTemps = null;
        timer.cancel();
        timer = null;
        FloatWindowUtils.needShowMessage = true;
        weatherPresenter.destroy();
        ActivitiesManager.getInstance().removeActivity(this);
        EventBus.getDefault().unregister(this);
    }

    private void startAnimLoading() {
        DraweeController draweeController =
                Fresco.newDraweeControllerBuilder()
                        .setUri(FileUtils.getLoadingUriFromAsset())
                        .setAutoPlayAnimations(true)
                        .build();
        loadingView.setController(draweeController);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FloatWindowUtils.needShowMessage = false;
        ActivitiesManager.getInstance().addActivity(this);
        EventBus.getDefault().unregister(this);
        EventBus.getDefault().register(this);
    }

    @Override
    public void updateViewWidth(int width) {
        if (adapter != null) {
            adapter.setViewWidth(width);
            adapter.notifyDataSetChanged();
        }
    }

    public void onEvent(WeatherEvent event) {
        logger.debug("onEvent--weather-date=" + event.getWitchdate());
        setTimeout();
        showLoading();
        startAnimLoading();
        requestWeather(event.getCity(), event.getWitchdate());
    }

    private void requestWeather(String city, int whichDay) {
        this.city = city;
        this.whichDay = whichDay;
        if (whichDay >= WeatherConstants.TODAY_WEATHER && whichDay <= WeatherConstants.AFTER_TOMORROW_WEATHER) {
            if (TextUtils.isEmpty(city) || city == null) {
                weatherPresenter.queryCity();
            } else {
                weatherPresenter.getRecentWeathers(city);
            }
        }
    }

    public void setTimeout() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (dataLists.size() <= 0) {
                        getWeatherFail("QueryWeather timeout!");
                    }
                }
            }, WeatherConstants.REQUEST_WEATHER_TIME_OUT_PERIOD);
        }
    }

    private void requestFail() {
        FloatWindowUtils.needShowMessage = true;
        EventBus.getDefault().post(VoiceEvent.SHOW_ANIM);
        VoiceManagerProxy.getInstance().startSpeaking(Constants.ERROR_GET_WEATHER, TTSType.TTS_START_UNDERSTANDING, true);
        finish();
    }
}
