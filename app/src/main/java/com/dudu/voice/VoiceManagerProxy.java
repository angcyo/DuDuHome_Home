package com.dudu.voice;

import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.voice.semantic.engine.SemanticReminder;
import com.dudu.voice.speech.SpeechManagerImpl;

import org.slf4j.LoggerFactory;

/**
 * Created by 赵圣琪 on 2015/12/24.
 */
public class VoiceManagerProxy implements VoiceManager {

    private static final VoiceManagerProxy mInstance = new VoiceManagerProxy();

    private VoiceManager mVoiceManager;

    public static VoiceManagerProxy getInstance() {
        return mInstance;
    }

    private VoiceManagerProxy() {
        // 默认使用思必驰
        LoggerFactory.getLogger("voice").debug("VoiceManager create");
        mVoiceManager = new SpeechManagerImpl();
    }

    @Override
    public void startSpeaking(String playText) {
        if (mVoiceManager != null) {
            mVoiceManager.startSpeaking(playText);
        }
    }

    @Override
    public void startSpeaking(String playText, TTSType type) {
        if (mVoiceManager != null) {
            mVoiceManager.startSpeaking(playText, type);
        }
    }

    @Override
    public void startSpeaking(String playText, TTSType type, boolean showMessage) {
        if (mVoiceManager != null) {
            mVoiceManager.startSpeaking(playText, type, showMessage);
        }
    }

    @Override
    public void onInit() {

        if (mVoiceManager != null) {
            mVoiceManager.onInit();
        }
    }

    @Override
    public void startWakeup() {
        if (mVoiceManager != null) {
            mVoiceManager.startWakeup();
        }
    }

    @Override
    public void stopWakeup() {
        if (mVoiceManager != null) {
            mVoiceManager.stopWakeup();
        }
    }

    @Override
    public void startVoiceService() {
        if (mVoiceManager != null) {
            mVoiceManager.startVoiceService();
        }
    }

    @Override
    public void startUnderstanding() {
        if (mVoiceManager != null) {
            mVoiceManager.startUnderstanding();
        }
    }

    @Override
    public void stopUnderstanding() {
        if (mVoiceManager != null) {
            mVoiceManager.stopUnderstanding();
        }
    }

    @Override
    public void stopSpeaking() {
        if (mVoiceManager != null) {
            mVoiceManager.stopSpeaking();
        }
    }

    @Override
    public void onStop() {
        if (mVoiceManager != null) {
            mVoiceManager.onStop();
        }
    }

    @Override
    public void onDestroy() {
        if (mVoiceManager != null) {
            mVoiceManager.onDestroy();
        }
    }

    @Override
    public void incrementMisUnderstandCount() {
        if (mVoiceManager != null) {
            mVoiceManager.incrementMisUnderstandCount();
        }
    }

    @Override
    public void clearMisUnderstandCount() {
        if (mVoiceManager != null) {
            mVoiceManager.clearMisUnderstandCount();
        }
    }

    @Override
    public void updateNativeGrammar() {
        mVoiceManager.updateNativeGrammar();
    }

    @Override
    public void reminderSpeak(SemanticReminder.ReminderType type, boolean showMessage) {
        mVoiceManager.reminderSpeak(type, showMessage);
    }

    /**
     * 彻底关闭语音播报与语音唤醒
     */
    public void cancelVoice() {
//        VoiceManagerProxy.getInstance().stopSpeaking();
//        VoiceManagerProxy.getInstance().onStop();
        VoiceManagerProxy.getInstance().stopWakeup();
    }

}
