package com.dudu.voice;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.event.DeviceEvent;
import com.dudu.voice.semantic.constant.TTSType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by 赵圣琪 on 2015/12/24.
 */
public abstract class BaseVoiceManager implements VoiceManager {

    public static final int MAX_MISUNDERSTAND_COUNT = 2;

    /**
     * 语音出错次数，限制为3次
     */
    protected AtomicInteger mMisunderstandCounter = new AtomicInteger(0);

    protected Context mContext;

    protected TTSType mType;

    protected Logger log;

    protected boolean isInitOver = false;

    protected int initStep = 0;

    protected long initTime;

    protected Subscription reInitSub;

    protected Handler mHandler = new Handler(Looper.getMainLooper());

    protected boolean canStartUnderstand = true;

    public BaseVoiceManager() {
        log = LoggerFactory.getLogger("voice.manager");

        mContext = LauncherApplication.getContext();
    }

    private Runnable speakHelloRunnable = new Runnable() {
        @Override
        public void run() {
            log.debug("startSpeaking 您好");
            canStartUnderstand = true;
            startSpeaking(Constants.WAKEUP_WORDS, TTSType.TTS_START_UNDERSTANDING, false);
        }
    };

    private void speakHello() {
        if (mHandler != null && speakHelloRunnable != null) {
            mHandler.removeCallbacks(speakHelloRunnable);
        }
        mHandler.postDelayed(speakHelloRunnable, 200);
    }

    @Override
    public void startVoiceService() {

        log.debug("开启语义服务... isInitOver {}", isInitOver);
        if (!isInitOver) {
            reInit();
            return;
        }
        EventBus.getDefault().post(new DeviceEvent.Screen(DeviceEvent.ON));
        clearMisUnderstandCount();
        stopSpeaking();
        speakHello();
        FloatWindowUtils.getInstance().showAnimWindow();

    }

    protected void reInit() {

        log.debug("initTime :{}", System.currentTimeMillis() - initTime);
        if (reInitSub != null && !reInitSub.isUnsubscribed())
            return;
        if (System.currentTimeMillis() - initTime > 20 * 1000) {
            onDestroy();
            reInitSub = Observable.timer(2, TimeUnit.SECONDS)
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(aLong -> {
                        onInit();
                    }, throwable -> log.error("reInit",throwable));
        }
    }

    @Override
    public void startSpeaking(String playText) {
        startSpeaking(playText, TTSType.TTS_START_UNDERSTANDING);
    }

    @Override
    public void startSpeaking(String playText, TTSType type) {
        startSpeaking(playText, TTSType.TTS_START_UNDERSTANDING, true);
    }

    @Override
    public void incrementMisUnderstandCount() {
        mMisunderstandCounter.incrementAndGet();
    }

    @Override
    public void clearMisUnderstandCount() {
        mMisunderstandCounter.set(0);
    }

    protected boolean checkMisUnderstandCount() {
        if (mMisunderstandCounter.get() >= MAX_MISUNDERSTAND_COUNT) {
            return true;
        }

        return false;
    }


}
