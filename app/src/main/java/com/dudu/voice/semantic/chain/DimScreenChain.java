package com.dudu.voice.semantic.chain;

import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.event.DeviceEvent;
import com.dudu.voice.semantic.bean.SemanticBean;
import com.dudu.voice.semantic.constant.SemanticConstant;
import com.umeng.analytics.MobclickAgent;

import de.greenrobot.event.EventBus;

/**
 * Created by 赵圣琪 on 2015/11/18.
 */
public class DimScreenChain extends SemanticChain {

    @Override
    public boolean matchSemantic(String service) {
        return SemanticConstant.SERVICE_DIM.equals(service);
    }

    @Override
    public boolean doSemantic(SemanticBean semantic) {
        EventBus.getDefault().post(new DeviceEvent.Screen(DeviceEvent.OFF));
        floatWindowUtils.removeFloatWindow();
        return true;
    }

}
