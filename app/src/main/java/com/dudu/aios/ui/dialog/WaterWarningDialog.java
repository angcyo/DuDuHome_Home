package com.dudu.aios.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.dudu.android.launcher.R;
import com.dudu.monitor.obd.CoolantTemperatureManager;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.TTSType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.greenrobot.event.EventBus;

/**
 * Created by Robert on 2016/7/6.
 */
public class WaterWarningDialog extends Dialog{

    private Logger log = LoggerFactory.getLogger("WaterWarningDialog");
    private ImageView ivWaterWarning = null;

    public WaterWarningDialog(Context context) {
        super(context);
    }

    public WaterWarningDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    public WaterWarningDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
        initEvent();
    }

    @Override
    protected void onStart() {
        super.onStart();

        startFlashAnimation();
        VoiceManagerProxy.getInstance().startSpeaking("警告,水温过高,请检查!", TTSType.TTS_DO_NOTHING, false);
        CoolantTemperatureManager.getInstance().setDisplayWarning(true);
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopFlashAnimation();
        CoolantTemperatureManager.getInstance().setDisplayWarning(false);
    }

    void initView()
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.fragment_water_warning_layout);

        ivWaterWarning = (ImageView) findViewById(R.id.id_iv_water_warning);
        log.info("hibox initView");
    }

    void initEvent()
    {
        EventBus.getDefault().unregister(this);
        EventBus.getDefault().register(this);
    }

    /*开始闪烁动画*/
    public void startFlashAnimation()
    {
        log.info("hibox startFlashAnimation");
        AlphaAnimation flashAnimation = new AlphaAnimation(0.3f, 1f);
        flashAnimation.setDuration(200);
        flashAnimation.setInterpolator(new AccelerateInterpolator());
        flashAnimation.setRepeatCount(Animation.INFINITE);
        flashAnimation.setRepeatMode(Animation.REVERSE);
        ivWaterWarning.setAnimation(flashAnimation);
        flashAnimation.start();
    }

    /*停止动画*/
    public void stopFlashAnimation()
    {
        log.info("hibox stopFlashAnimation");
        ivWaterWarning.clearAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        log.info("hibox -> onTouch = {}",event.getAction());
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                //记录告警弹窗时间
                final long currentTime = System.currentTimeMillis();
                CoolantTemperatureManager.getInstance().setLastHighTime(currentTime);

                dismiss();

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
