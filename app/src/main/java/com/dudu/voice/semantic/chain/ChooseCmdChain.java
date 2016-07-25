package com.dudu.voice.semantic.chain;

import android.app.Activity;
import android.os.Bundle;

import com.dudu.aios.ui.activity.MainRecordActivity;
import com.dudu.aios.ui.base.BaseFragmentManagerActivity;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.ActivitiesManager;
import com.dudu.android.launcher.utils.BtPhoneUtils;
import com.dudu.android.launcher.utils.ChoiseUtil;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.event.ChooseEvent;
import com.dudu.map.NavigationProxy;
import com.dudu.voice.semantic.bean.ChooseCmdBean;
import com.dudu.voice.semantic.bean.SemanticBean;
import com.dudu.voice.semantic.constant.SceneType;
import com.dudu.voice.semantic.constant.SemanticConstant;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.voice.semantic.engine.SemanticEngine;
import com.dudu.voice.semantic.engine.SemanticReminder;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

/**
 * Created by lxh on 2016-04-15 10:43.
 */
public class ChooseCmdChain extends SemanticChain {

    public static final String PAGE = "页";
    public static final String PAGE_TWO = "夜";
    public static final String LAST_PAGE = "last_page";
    public static final String LAST_ONE = "last_one";

    private NavigationProxy navigationProxy;

    public ChooseCmdChain() {
        this.navigationProxy = NavigationProxy.getInstance();
    }

    @Override
    public boolean matchSemantic(String service) {
        return SemanticConstant.SERVICE_CHOOSE_CMD.equalsIgnoreCase(service)
                || SemanticConstant.SERVICE_CHOOSE_STRATEGY.equalsIgnoreCase(service);
    }

    @Override
    public boolean doSemantic(SemanticBean bean) {

        ChooseCmdBean mapChooseBean = (ChooseCmdBean) bean;
        String type = mapChooseBean.getChoose_type();
        int number = ChoiseUtil.getChoiseSize(mapChooseBean.getChoose_number());
        if (number == 0 && (!type.equals(LAST_ONE) && !type.equals(LAST_PAGE))) {
            mVoiceManager.reminderSpeak(SemanticReminder.ReminderType.CHOOSE_OVER_FLOW, false);
            return true;
        }
        switch (type) {

            case PAGE:
            case PAGE_TWO:
                mVoiceManager.startUnderstanding();
                return choosePage(number);
            case SemanticConstant.SERVICE_CHOOSE_STRATEGY:
                if (navigationProxy.isShowList() && navigationProxy.getChooseStep() == 2) {
                    navigationProxy.onChooseNumber(number);
                    return true;
                } else {
                    return false;
                }
            case LAST_ONE:
                return lastOne();
            case LAST_PAGE:
                return lastPage();
            default:
                return chooseNumber(number);
        }

    }

    private boolean isCarChecking() {

        Activity topActivity = ActivitiesManager.getInstance().getTopActivity();

        if (topActivity instanceof MainRecordActivity
                && ((BaseFragmentManagerActivity) topActivity).getCurrentStackTag().equals(FragmentConstants.REPAIR_FAULT_CODE_FRAGMENT)) {
            return true;
        }
        return false;

    }

    private boolean choosePage(int number) {
        if (isCarChecking()) {
            MobclickAgent.onEvent(mContext, ClickEvent.voice6.getEventId());
            EventBus.getDefault().post(new ChooseEvent(ChooseEvent.ChooseType.CHOOSE_PAGE, number));
            return true;
        }
        if (navigationProxy.isShowList()) {
            navigationProxy.onChoosePage(number);
            return true;
        }

        return false;
    }

    private boolean chooseNumber(int number) {

        if (isCarChecking()) {
            MobclickAgent.onEvent(mContext, ClickEvent.voice7.getEventId());
            EventBus.getDefault().post(new ChooseEvent(ChooseEvent.ChooseType.CHOOSE_NUMBER, number));
            return true;
        }
        if (navigationProxy.isShowList()) {
            navigationProxy.onChooseNumber(number);
            return true;
        }
        if (isBtoutCall()) {
            choosePhoneNumber(number);
            return true;
        }

        return false;
    }

    private boolean isBtoutCall() {
        if (SemanticEngine.getProcessor().getCurScene().getType() == SceneType.BTCALL) {
            return true;
        }
        return false;
    }

    private void choosePhoneNumber(int number) {
        Bundle bundle = FragmentConstants.TEMP_ARGS;
        ArrayList<String> numberList = bundle.getStringArrayList(Constants.EXTRA_PHONE_NUMBER_LIST);
        if (null != numberList) {
            if (number > numberList.size()) {
                mVoiceManager.startSpeaking(mContext.getString(R.string.choose_error), TTSType.TTS_START_UNDERSTANDING, false);
            } else {
                bundle.putString(Constants.EXTRA_PHONE_NUMBER, numberList.get(number - 1));
                FragmentConstants.TEMP_ARGS = bundle;
                BtPhoneUtils.btCallOutSource = BtPhoneUtils.BTCALL_OUT_SOURCE_VOIC;
                if (LauncherApplication.getContext().getInstance() != null) {
                    LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.BT_OUT_CALL);
                }
                SemanticEngine.getProcessor().switchSemanticType(SceneType.HOME);
            }
        }
    }

    private boolean lastPage() {
        mVoiceManager.startUnderstanding();

        if (isCarChecking()) {
            MobclickAgent.onEvent(mContext, ClickEvent.voice5.getEventId());
            EventBus.getDefault().post(new ChooseEvent(ChooseEvent.ChooseType.LAST_PAGE, 0));
            return true;
        }

        if (navigationProxy.isShowList()) {
            navigationProxy.onLastPage();
            return true;
        }

        return false;
    }

    private boolean lastOne() {
        mVoiceManager.startUnderstanding();

        if (isCarChecking()) {

            EventBus.getDefault().post(new ChooseEvent(ChooseEvent.ChooseType.LAST_ONE, 0));

            return true;
        }

        if (navigationProxy.isShowList()) {
            navigationProxy.onLastOne();
            return true;
        }
        return false;
    }


}
