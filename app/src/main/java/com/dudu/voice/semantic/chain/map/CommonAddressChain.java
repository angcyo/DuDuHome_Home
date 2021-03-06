package com.dudu.voice.semantic.chain.map;

import android.content.Context;
import android.text.TextUtils;

import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.utils.CommonAddressUtil;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.map.NavigationProxy;
import com.dudu.navi.NavigationManager;
import com.dudu.navi.entity.Navigation;
import com.dudu.navi.entity.Point;
import com.dudu.navi.vauleObject.CommonAddressType;
import com.dudu.navi.vauleObject.NaviDriveMode;
import com.dudu.navi.vauleObject.NavigationType;
import com.dudu.voice.semantic.bean.SemanticBean;
import com.dudu.voice.semantic.bean.map.MapCommonAddressBean;
import com.dudu.voice.semantic.chain.SemanticChain;
import com.dudu.voice.semantic.constant.SceneType;
import com.dudu.voice.semantic.constant.SemanticConstant;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.voice.semantic.engine.SemanticEngine;
import com.umeng.analytics.MobclickAgent;


public class CommonAddressChain extends SemanticChain {

    private String addressType;

    private String address;

    private double[] location;

    private Context mContext;

    public CommonAddressChain() {

        this.mContext = LauncherApplication.getContext().getApplicationContext();
    }

    @Override
    public boolean matchSemantic(String service) {
        return SemanticConstant.SERVICE_COMMONADDRESS.equalsIgnoreCase(service);
    }

    @Override
    public boolean doSemantic(SemanticBean semantic) {
        if (semantic != null) {
            addressType = ((MapCommonAddressBean) semantic).getPoiName();
            if (addressType.contains("地址")) {
                addressType = addressType.split("地址")[0];
            }
            if (!TextUtils.isEmpty(addressType)) {

                switch (addressType) {

                    case CommonAddressUtil.HOME:
                        address = CommonAddressUtil.getHome(mContext);
                        location = CommonAddressUtil.getHomeAddress(mContext);
                        NavigationManager.getInstance(mContext).setCommonAddressType(CommonAddressType.HOME);
                        break;
                    case CommonAddressUtil.HOMETOWN:
                        address = CommonAddressUtil.getHometown(mContext);
                        location = CommonAddressUtil.getHometownAddress(mContext);
                        NavigationManager.getInstance(mContext).setCommonAddressType(CommonAddressType.HOMETOWN);
                        break;

                    case CommonAddressUtil.COMPANY:
                        address = CommonAddressUtil.getCompany(mContext);
                        location = CommonAddressUtil.getCompanyAddress(mContext);
                        NavigationManager.getInstance(mContext).setCommonAddressType(CommonAddressType.COMPANY);
                        break;

                }
                if (!TextUtils.isEmpty(address) && !checkPoint(location)) {
                    floatWindowUtils.removeFloatWindow();
                    Navigation navigation = new Navigation(new Point(location[0], location[1]), NaviDriveMode.SPEEDFIRST, NavigationType.NAVIGATION);
                    NavigationProxy.getInstance().startNavigation(navigation);
                    MobclickAgent.onEvent(mContext, ClickEvent.voice18.getEventId());

                } else {
                    MobclickAgent.onEvent(mContext, ClickEvent.voice19.getEventId());
                    String playText = "您还没有" + addressType + "地址，是否添加？";
                    mVoiceManager.startSpeaking(playText, TTSType.TTS_START_UNDERSTANDING, true);
                    SemanticEngine.getProcessor().switchSemanticType(SceneType.COMMON_WHETHER);
                }

                return true;
            }

            return true;
        }
        return false;
    }

    private boolean checkPoint(double[] point) {
        return ((point[0] == -90) && (point[1] == -90));
    }

}
