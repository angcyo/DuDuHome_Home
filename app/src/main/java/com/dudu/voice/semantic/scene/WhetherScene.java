package com.dudu.voice.semantic.scene;

import com.dudu.voice.semantic.chain.DefaultChain;
import com.dudu.voice.semantic.chain.map.WhetherDefaultChain;
import com.dudu.voice.semantic.constant.SceneType;

/**
 * Created by lxh on 2015/12/1.
 */
public class WhetherScene extends SemanticScene {


    @Override
    public void initChains() {
        initNomalChan();
        setType(SceneType.COMMON_WHETHER);

    }

    @Override
    public DefaultChain getDefaultChain() {
        return new WhetherDefaultChain();
    }
}
