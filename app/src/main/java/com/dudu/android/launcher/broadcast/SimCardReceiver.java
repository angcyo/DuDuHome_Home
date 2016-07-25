package com.dudu.android.launcher.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dudu.android.launcher.utils.Utils;
import com.dudu.commonlib.utils.File.KeyConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimCardReceiver extends BroadcastReceiver {
    private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";

    private Logger log;

    public SimCardReceiver() {
        log = LoggerFactory.getLogger("init.receiver.sim");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(!KeyConstants.is_agree_disclaimer)
            return;
        if (intent.getAction().equals(ACTION_SIM_STATE_CHANGED)) {
            log.debug("onReceive sim state:{}", intent.getExtras());
            Utils.checkSimCardState(context);
        }
    }
}
