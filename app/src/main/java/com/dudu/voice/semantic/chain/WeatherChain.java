package com.dudu.voice.semantic.chain;

import android.content.Intent;
import android.text.TextUtils;

import com.dudu.aios.ui.activity.weather.WeatherActivity;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.ActivitiesManager;
import com.dudu.android.launcher.utils.CmdType;
import com.dudu.android.launcher.utils.WeatherUtils;
import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.voice.semantic.bean.SemanticBean;
import com.dudu.voice.semantic.bean.WeatherBean;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.weather.WeatherEvent;
import com.umeng.analytics.MobclickAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.greenrobot.event.EventBus;

/**
 * Created by lxh on 2016/2/17.
 */
public class WeatherChain extends SemanticChain {

    private String mCity;

    private String mDate;

    private String mTimeDealed;

    private Logger logger;

    private int witchDay = 0;

    private String province;

    public WeatherChain() {
        logger = LoggerFactory.getLogger("voice.weather");

//        mWeatherManager = new WeatherManager();

//        mWeatherManager.setOnWeatherDataListener(this);
    }

    @Override
    public boolean matchSemantic(String service) {
        return CmdType.SERVICE_WEATHER.equals(service);
    }

    @Override
    public boolean doSemantic(SemanticBean semantic) {
        WeatherBean bean = (WeatherBean) semantic;
        initTimeAndCity(bean);
        if (witchDay < 0) {
            mVoiceManager.startSpeaking(mContext.getString(R.string.notice_pastWeather), TTSType.TTS_START_UNDERSTANDING, true);
            return true;
        }
        floatWindowUtils.removeFloatWindow();
        if (ActivitiesManager.getInstance().getTopActivity() instanceof WeatherActivity) {
            EventBus.getDefault().post(new WeatherEvent(mCity, witchDay));
        }
        Intent intent = new Intent(mContext, WeatherActivity.class);
        intent.putExtra("city", mCity);
        intent.putExtra("date", witchDay);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        return true;
    }


    private void initTimeAndCity(WeatherBean weather) {
        mCity = weather.getCity();
        mDate = weather.getDate();
        province = weather.getProvince();
        mTimeDealed = WeatherUtils.getQueryTime(mDate);
        witchDay = WeatherUtils.getWitchDay(mTimeDealed);
        if (!TextUtils.isEmpty(mCity)) {//城市不为空
            logger.debug("城市不为空");
            if (witchDay == 0) {
                MobclickAgent.onEvent(CommonLib.getInstance().getContext(), ClickEvent.voice34.getEventId());
            } else if (witchDay > 0) {
                MobclickAgent.onEvent(CommonLib.getInstance().getContext(), ClickEvent.voice35.getEventId());
            }
        } else {//城市为空
            if (!TextUtils.isEmpty(province)) {
                mCity = WeatherUtils.getQueryProvince(province);
                mCity = WeatherUtils.getCapital(mCity);
            }
            if (!TextUtils.isEmpty(weather.getArea())) {
                mCity = weather.getArea();
            }
            if (witchDay == 0) {
                MobclickAgent.onEvent(CommonLib.getInstance().getContext(), ClickEvent.voice33.getEventId());
            } else if (witchDay > 0) {
                MobclickAgent.onEvent(CommonLib.getInstance().getContext(), ClickEvent.voice41.getEventId());
            }
        }
    }

}
