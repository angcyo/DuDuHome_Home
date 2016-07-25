package com.dudu.voice.semantic.chain.map;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;

import com.dudu.aios.ui.activity.MainRecordActivity;
import com.dudu.aios.ui.base.BaseFragmentManagerActivity;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.ActivitiesManager;
import com.dudu.android.launcher.utils.BtPhoneUtils;
import com.dudu.android.launcher.utils.ChoiseUtil;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.event.ChooseEvent;
import com.dudu.map.NavigationProxy;
import com.dudu.navi.vauleObject.SearchType;
import com.dudu.voice.semantic.bean.SemanticBean;
import com.dudu.voice.semantic.chain.DefaultChain;
import com.dudu.voice.semantic.constant.SceneType;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.voice.semantic.engine.SemanticEngine;
import com.dudu.voice.semantic.engine.SemanticReminder;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

/**
 * Created by lxh on 2015/11/12.
 */
public class MapChoiseDefaultChain extends DefaultChain {

    public static final String NEXT_PAGE = "下一页";
    public static final String SHORT_NEXT_PAGE = "下页";

    public static final String PREVIOUS_PAGE = "上一页";
    public static final String SHORT_PREVIOUS_PAGE = "上页";

    private NavigationProxy mProxy;

    public MapChoiseDefaultChain() {
        mProxy = NavigationProxy.getInstance();
    }

    @Override
    public boolean matchSemantic(String service) {
        return true;
    }

    @Override
    public boolean doSemantic(SemanticBean semantic) {

        String text = semantic == null ? "" : semantic.getText();
        return handleMapChoise(text);
    }

    private boolean handleMapChoise(String text) {
        if (mProxy.isShowList() || isCarChecking() || isBtoutCall()) {
            if (text.contains(NEXT_PAGE) || text.contains(SHORT_NEXT_PAGE)) {
                mProxy.onNextPage();
                mVoiceManager.startUnderstanding();
            } else if (text.contains(PREVIOUS_PAGE) || text.contains(SHORT_PREVIOUS_PAGE)) {
                mProxy.onPreviousPage();
                mVoiceManager.startUnderstanding();
            } else {
                if (!handleChoosePageOrNumber(text)) {
                    if (!TextUtils.isEmpty(text) && text.contains("的位置")) {
                        String place = text.split("的位置")[0];
                        NavigationProxy.getInstance().searchControl(place, SearchType.SEARCH_PLACE);
                        return true;
                    }
                    if (mProxy.getChooseStep() == 1) {
                        mVoiceManager.reminderSpeak(SemanticReminder.ReminderType.CHOOSE_REMINDER, false);

                    } else {
                        mVoiceManager.reminderSpeak(SemanticReminder.ReminderType.UNDERSTAND_MISUNDERSTAND, false);

                    }
                    return false;
                }

            }

        }
        return false;
    }

    private boolean handleChoosePageOrNumber(String text) {
        int option;
        if (text.startsWith("第") && (text.length() == 3 || text.length() == 4)) {

            option = ChoiseUtil.getChoiseSize(text.length() == 3 ? text.substring(1, 2) : text.substring(1, 3));

            if (text.endsWith("个") || text.endsWith("项") || text.endsWith("向") || text.endsWith("巷")) {
                if (isCarChecking()) {
                    EventBus.getDefault().post(new ChooseEvent(ChooseEvent.ChooseType.CHOOSE_NUMBER, option));
                    return true;
                }
                if (isBtoutCall()) {
                    choosePhoneNumber(option);
                    return true;
                }
                mProxy.onChooseNumber(option);
            } else if (text.endsWith("页") || text.endsWith("夜")) {
                if (isCarChecking()) {
                    EventBus.getDefault().post(new ChooseEvent(ChooseEvent.ChooseType.CHOOSE_PAGE, option));
                    return true;
                }
                mProxy.onChoosePage(option);
                mVoiceManager.startUnderstanding();
            } else {
                return false;
            }

            return true;
        }

        if (mProxy.getChooseStep() == 2) {
            switch (text) {
                case "速度最快":
                    option = 1;
                    break;
                case "避免收费":
                    option = 2;
                    break;
                case "距离最短":
                    option = 3;
                    break;
                case "不走高速快速路":
                    option = 4;
                    break;
                case "时间最短且躲避拥堵":
                    option = 5;
                    break;
                case "避免收费且躲避拥堵":
                    option = 6;
                    break;
                default:
                    return false;
            }

            mProxy.onChooseNumber(option);
            return true;
        }

        return false;
    }

    private boolean isCarChecking() {

        Activity topActivity = ActivitiesManager.getInstance().getTopActivity();

        if (topActivity instanceof MainRecordActivity
                && ((BaseFragmentManagerActivity) topActivity).getCurrentStackTag().equals(FragmentConstants.REPAIR_FAULT_CODE_FRAGMENT)) {
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
        if (number > numberList.size()) {
            mVoiceManager.startSpeaking(mContext.getString(R.string.choose_error), TTSType.TTS_START_UNDERSTANDING, false);
        } else {
            floatWindowUtils.removeFloatWindow();
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
