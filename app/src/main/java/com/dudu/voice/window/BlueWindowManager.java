package com.dudu.voice.window;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.dudu.aios.ui.activity.MainRecordActivity;
import com.dudu.aios.ui.base.BaseFragmentManagerActivity;
import com.dudu.aios.ui.base.VolBrightnessSetting;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.aios.ui.voice.VoiceEvent;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.model.WindowMessageEntity;
import com.dudu.android.launcher.ui.adapter.MessageAdapter;
import com.dudu.android.launcher.utils.ActivitiesManager;
import com.dudu.map.NavigationProxy;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.SceneType;
import com.dudu.voice.semantic.engine.SemanticEngine;
import com.dudu.voice.window.view.SiriViewLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by 赵圣琪 on 2016/1/4.
 */
public class BlueWindowManager extends BaseWindowManager {


    private ListView mMessageListView;

    private MessageAdapter mMessageAdapter;

    private List<WindowMessageEntity> mMessageData;

    private Logger logger;

    private Button voiceBack;

    private SiriViewLayout rippleView;
    private LinearLayout voice_animLayout;
    private ImageView voiceCircleAnimView;
    private View message_layout;

    private boolean isInit = false;

    private VolBrightnessSetting volBrightnessSetting;

    private RotateAnimation circleAnimation;


    @Override
    public void initWindow() {

        if (isInit)
            return;

        logger = LoggerFactory.getLogger("voice.float");

        mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mLayoutParams.format = PixelFormat.RGBA_8888;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.width = mContext.getResources().getDisplayMetrics().widthPixels;
        mLayoutParams.height = mContext.getResources().getDisplayMetrics().heightPixels;
        mLayoutParams.x = 0;
        mLayoutParams.y = 0;


        voiceBack = (Button) mFloatWindowView.findViewById(R.id.voiceBack);

        voiceBack.setOnClickListener(v -> {
            VoiceManagerProxy.getInstance().stopSpeaking();
            removeFloatWindow();
        });

        mMessageData = new ArrayList<>();
        mMessageListView = (ListView) mFloatWindowView.findViewById(R.id.message_listView);
        mMessageAdapter = new MessageAdapter(mContext, mMessageData);
        mMessageListView.setAdapter(mMessageAdapter);
        message_layout = mFloatWindowView.findViewById(R.id.message_layout);

        initAnimView();

        volBrightnessSetting = new VolBrightnessSetting(ActivitiesManager.getInstance().getTopActivity(), mFloatWindowView);

        isInit = true;
    }

    private void initAnimView() {
        voice_animLayout = (LinearLayout) mFloatWindowView.findViewById(R.id.voice_anim_layout);


        rippleView = (SiriViewLayout) mFloatWindowView.findViewById(R.id.speech_wave_view);

        voiceCircleAnimView = (ImageView) mFloatWindowView.findViewById(R.id.voice_circle);

        circleAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        circleAnimation.setInterpolator(new LinearInterpolator());//不停顿
        circleAnimation.setRepeatMode(Animation.RESTART);
        circleAnimation.setRepeatCount(3000);
        circleAnimation.setFillAfter(true);//停在最后
        circleAnimation.setDuration(2000);
        //动画开始
        voiceCircleAnimView.startAnimation(circleAnimation);

    }


    @Override
    public int getFloatWindowLayout() {
        return R.layout.speech_dialog_window_new;
    }

    @Override
    public void showMessage(WindowMessageEntity message) {

        if (NavigationProxy.getInstance().isShowList()) {
            return;
        }

        if (!(ActivitiesManager.getInstance().getTopActivity() instanceof MainRecordActivity)
                || LauncherApplication.getContext().isReceivingOrder()) {
            mFloatWindowView.setBackgroundResource(R.drawable.black_bg);
        }

        addFloatView();

        stopAnimWindow();

        message_layout.setVisibility(View.VISIBLE);

        mMessageListView.setVisibility(View.VISIBLE);

        mMessageAdapter.addMessage(message);

        mMessageListView.setSelection(mMessageData.size() - 1);

        EventBus.getDefault().post(VoiceEvent.SHOW_MESSAGE);

        mShowFloatWindow = true;
    }

    @Override
    public void showStrategy() {
    }

    @Override
    public void showAddress() {
    }

    @Override
    public void onVolumeChanged(int volume) {
    }

    @Override
    public void onNextPage() {
    }

    @Override
    public void onPreviousPage() {
    }

    @Override
    public void onChoosePage(int page) {
    }

    @Override
    public void removeFloatWindow() {

        logger.debug("removeFloatWindow");

        mMessageData.clear();
        mMessageAdapter.notifyDataSetChanged();
        removeFloatView();
        isInit = false;
        stopAnimWindow();
        EventBus.getDefault().post(VoiceEvent.DISMISS_WINDOW);
        Activity topActivity = ActivitiesManager.getInstance().getTopActivity();
        if (topActivity instanceof MainRecordActivity) {
            String currentStackTag = ((BaseFragmentManagerActivity) topActivity).getCurrentStackTag();
            if (FragmentConstants.CAR_CHECKING.equals(currentStackTag)
                    || FragmentConstants.REPAIR_FAULT_CODE_FRAGMENT.equals(currentStackTag)
                    || FragmentConstants.VEHICLE_ANIMATION_FRAGMENT.equals(currentStackTag)) {
                return;
            }
        }
        if (!NavigationProxy.getInstance().isShowList()) {
            SemanticEngine.getProcessor().switchSemanticType(SceneType.HOME);
        }

    }

    @Override
    public void setItemClickListener(AdapterView.OnItemClickListener listener) {
    }

    public void showAnimWindow() {

        stopAnimWindow();

        if (!(ActivitiesManager.getInstance().getTopActivity() instanceof MainRecordActivity)
                || LauncherApplication.getContext().isReceivingOrder()) {
            mFloatWindowView.setBackgroundResource(R.drawable.black_bg);
        }
        addFloatView();

        visibleAnimView();

        message_layout.setVisibility(View.GONE);

        voice_animLayout.setBackgroundColor(Color.TRANSPARENT);
        voiceCircleAnimView.startAnimation(circleAnimation);

        mShowFloatWindow = true;
    }

    public void stopAnimWindow() {

        voiceCircleAnimView.clearAnimation();

        voice_animLayout.setVisibility(View.GONE);

        voiceCircleAnimView.setVisibility(View.GONE);

        rippleView.setVisibility(View.GONE);
    }

    private void visibleAnimView() {
        voice_animLayout.setVisibility(View.VISIBLE);
        rippleView.setVisibility(View.VISIBLE);
        voiceCircleAnimView.setVisibility(View.VISIBLE);
    }

}
