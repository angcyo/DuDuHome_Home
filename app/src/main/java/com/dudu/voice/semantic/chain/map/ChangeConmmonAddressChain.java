package com.dudu.voice.semantic.chain.map;

import com.dudu.android.launcher.utils.CommonAddressUtil;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.map.NavigationProxy;
import com.dudu.navi.NavigationManager;
import com.dudu.navi.vauleObject.CommonAddressType;
import com.dudu.navi.vauleObject.SearchType;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.bean.SemanticBean;
import com.dudu.voice.semantic.bean.map.ChangeCommonAdrBean;
import com.dudu.voice.semantic.chain.SemanticChain;
import com.dudu.voice.semantic.constant.SceneType;
import com.dudu.voice.semantic.constant.SemanticConstant;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.voice.semantic.engine.SemanticEngine;
import com.umeng.analytics.MobclickAgent;

/**
 * Created by lxh on 2016-04-13 17:42.
 */
public class ChangeConmmonAddressChain extends SemanticChain {
    @Override
    public boolean matchSemantic(String service) {
        return SemanticConstant.SERVICE_CHANGE_CONMMONADDTRSS.equalsIgnoreCase(service);
    }

    @Override
    public boolean doSemantic(SemanticBean bean) {
        if (bean != null) {
            String addressType = ((ChangeCommonAdrBean) bean).getCommonAddressType();
            if (addressType.contains("地址")) {
                addressType = addressType.split("地址")[0];
            }
            switch (addressType) {
                case CommonAddressUtil.HOME:
                    NavigationManager.getInstance(mContext).setCommonAddressType(CommonAddressType.HOME);

                    break;
                case CommonAddressUtil.COMPANY:
                    NavigationManager.getInstance(mContext).setCommonAddressType(CommonAddressType.COMPANY);

                    break;
                case CommonAddressUtil.HOMETOWN:
                    NavigationManager.getInstance(mContext).setCommonAddressType(CommonAddressType.HOMETOWN);
                    break;
            }
            MobclickAgent.onEvent(mContext, ClickEvent.voice20.getEventId());
            NavigationProxy.getInstance().searchControl("", SearchType.SEARCH_COMMONADDRESS);
            VoiceManagerProxy.getInstance().startSpeaking("您好，请说出您要修改的地址", TTSType.TTS_START_UNDERSTANDING, true);
            SemanticEngine.getProcessor().switchSemanticType(SceneType.NAVIGATION);

        }
        return true;
    }
}
