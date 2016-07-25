package com.dudu.aios.ui.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.dudu.android.launcher.R;
import com.dudu.event.WaterWarningDisplayEvent;
import com.dudu.monitor.obd.CoolantTemperatureManager;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.TTSType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.greenrobot.event.EventBus;

public class WaterWarningActivity extends AppCompatActivity {

    private Logger log = LoggerFactory.getLogger("WaterWarningActivity");
    private ImageView ivWaterWarning = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        log.info("WaterWarning onCreate");
        setContentView(R.layout.fragment_water_warning_layout);
        ivWaterWarning = (ImageView) findViewById(R.id.id_iv_water_warning);
//        log.info("WaterWarning initView");

        EventBus.getDefault().unregister(this);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        CoolantTemperatureManager.getInstance().setDisplayWarning(true);
        startFlashAnimation();
        VoiceManagerProxy.getInstance().startSpeaking("警告,水温过高,请检查!", TTSType.TTS_DO_NOTHING, false);
    }

    @Override
    protected void onPause() {
        super.onPause();

        CoolantTemperatureManager.getInstance().setDisplayWarning(false);
        stopFlashAnimation();
    }

    public void onEventMainThread(WaterWarningDisplayEvent event)
    {
//        log.info("WaterWarning  event.ismNeedClose {}",event.ismNeedClose());
        if(event.ismNeedClose())
        {
            finish();
        }
    }

    /*开始水温告警闪烁*/
    public void startFlashAnimation()
    {
//        log.info("WaterWarning startFlashAnimation");
        AlphaAnimation flashAnimation = new AlphaAnimation(0.1f, 1f);
        flashAnimation.setDuration(300);
        flashAnimation.setInterpolator(new AccelerateInterpolator());
        flashAnimation.setRepeatCount(Animation.INFINITE);
        flashAnimation.setRepeatMode(Animation.REVERSE);
        ivWaterWarning.setAnimation(flashAnimation);
        flashAnimation.start();
    }

    /*停止动画*/
    public void stopFlashAnimation()
    {
//        log.info("WaterWarning stopFlashAnimation");
        ivWaterWarning.clearAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        log.info("WaterWarning -> onTouch = {}",event.getAction());
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                finish();

                //记录告警弹窗时间
                final long currentTime = System.currentTimeMillis();
                CoolantTemperatureManager.getInstance().setLastHighTime(currentTime);

                break;
            case MotionEvent.ACTION_UP:

                break;
            case MotionEvent.ACTION_MOVE:
                break;
            default:
                break;
        }

        return true;
    }


}
