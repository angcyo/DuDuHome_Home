package com.dudu.voice.semantic.scene;

import com.dudu.voice.semantic.chain.DefaultChain;
import com.dudu.voice.semantic.chain.FaultDefaultChain;
import com.dudu.voice.semantic.constant.SceneType;
import com.dudu.voice.semantic.constant.SemanticConstant;

/**
 * Created by lxh on 2016/2/16.
 */
public class FaultScene extends SemanticScene {
    @Override
    public void initChains() {

        initNomalChan();
        mChainMap.put(SemanticConstant.SERVICE_FAULT_CMD, mChainFactory.getFaultCmdChain());
        setType(SceneType.CAR_CHECKING);

    }

    @Override
    public DefaultChain getDefaultChain() {
        return new FaultDefaultChain();
    }
}
