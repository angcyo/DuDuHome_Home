package com.dudu.voice.semantic.chain;

import android.text.TextUtils;

import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.utils.NetworkUtils;
import com.dudu.map.NavigationProxy;
import com.dudu.navi.vauleObject.SearchType;
import com.dudu.voice.semantic.bean.SemanticBean;
import com.dudu.voice.semantic.engine.SemanticReminder;

/**
 * Created by 赵圣琪 on 2015/10/30.
 */
public class DefaultChain extends SemanticChain {

    private static final String CHAT = "聊天";

    @Override
    public boolean matchSemantic(String service) {
        return true;
    }

    @Override
    public boolean doSemantic(SemanticBean semantic) {
        return isPlaceLocation(semantic);
    }


    private boolean isPlaceLocation(SemanticBean semantic) {
        if (semantic != null
                && !TextUtils.isEmpty(semantic.getText())
                && semantic.getText().endsWith("的位置") && semantic.getText().length() > 3) {
            String place = semantic.getText().split("的位置")[0];
            NavigationProxy.getInstance().searchControl(place, SearchType.SEARCH_PLACE);
            return true;
        }


        if (!NetworkUtils.isNetworkConnected(LauncherApplication.getContext())) {
            mVoiceManager.reminderSpeak(SemanticReminder.ReminderType.NETWORK_ERROR, true);

        } else {
            reminder(semantic);
        }
        return false;
    }

    private void reminder(SemanticBean semanticBean) {

        if (semanticBean != null && !TextUtils.isEmpty(semanticBean.getDomain()) && !semanticBean.getDomain().equals(CHAT)) {
            mVoiceManager.reminderSpeak(SemanticReminder.ReminderType.NO_SUPPORT, true);
        } else {
            mVoiceManager.reminderSpeak(SemanticReminder.ReminderType.UNDERSTAND_MISUNDERSTAND, true);
        }
    }
}