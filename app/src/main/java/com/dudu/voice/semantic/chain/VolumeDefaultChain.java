package com.dudu.voice.semantic.chain;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;

import com.dudu.aios.ui.activity.MainRecordActivity;
import com.dudu.aios.ui.base.BaseFragmentManagerActivity;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.utils.ActivitiesManager;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.map.NavigationProxy;
import com.dudu.voice.semantic.bean.SemanticBean;
import com.dudu.voice.semantic.constant.SceneType;
import com.dudu.voice.semantic.engine.SemanticEngine;
import com.dudu.voice.semantic.engine.SemanticReminder;
import com.umeng.analytics.MobclickAgent;

/**
 * Created by Robi on 2016-04-06 15:46.
 */
public class VolumeDefaultChain extends DefaultChain {

    public static final int VOLUME_INCREMENTAL = 3;
    private static String SMALL = "小一点";
    private static String LARGE = "大一点";
    private static String SMALL_THREE = "声音太大了";
    private static String SMALL_TWO = "低一点";
    private static String LARGE_TWO = "高一点";
    private static String LARGE_THREE = "声音太小了";

    private AudioManager mAudioManager;
    private int mMaxVolume;
    private int mCurVolume;

    public VolumeDefaultChain() {

        mAudioManager = (AudioManager) LauncherApplication.getContext()
                .getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mCurVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean matchSemantic(String service) {
        return true;
    }

    @Override
    public boolean doSemantic(SemanticBean bean) {

        return changeVolume(bean);
    }

    private boolean changeVolume(SemanticBean bean) {

        if (bean != null) {
            String text = bean.getText();

            if (text.contains(SMALL) || text.contains(SMALL_TWO)||text.contains(SMALL_THREE)) {
                turnDownVolume();
                return true;
            } else if (text.contains(LARGE) || text.contains(LARGE_TWO) || text.contains(LARGE_THREE)) {
                turnUpVolume();
                return true;
            }
        }
        mVoiceManager.reminderSpeak(SemanticReminder.ReminderType.UNDERSTAND_MISUNDERSTAND, true);

        return false;
    }

    private void turnUpVolume() {
        mCurVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                mCurVolume + VOLUME_INCREMENTAL >= mMaxVolume ? mMaxVolume
                        : mCurVolume + VOLUME_INCREMENTAL,
                AudioManager.FLAG_PLAY_SOUND);
        setSceneType();
        MobclickAgent.onEvent(mContext, ClickEvent.voice45.getEventId());

    }

    private void turnDownVolume() {
        mCurVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                mCurVolume - VOLUME_INCREMENTAL <= 0 ? 0
                        : mCurVolume - VOLUME_INCREMENTAL,
                AudioManager.FLAG_PLAY_SOUND);

        setSceneType();
        MobclickAgent.onEvent(mContext, ClickEvent.voice45.getEventId());
    }

    private void setSceneType() {
        if (!NavigationProxy.getInstance().isShowList() && !isCarChecking()) {
            SemanticEngine.getProcessor().switchSemanticType(SceneType.VOLUME);
        }
        mVoiceManager.startUnderstanding();
    }

    private boolean isCarChecking() {

        Activity topActivity = ActivitiesManager.getInstance().getTopActivity();

        if (topActivity instanceof MainRecordActivity
                && ((BaseFragmentManagerActivity) topActivity).getCurrentStackTag().equals(FragmentConstants.REPAIR_FAULT_CODE_FRAGMENT)) {
            return true;
        }
        return false;

    }
}
