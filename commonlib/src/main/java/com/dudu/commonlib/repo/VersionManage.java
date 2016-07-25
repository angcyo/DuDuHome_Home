package com.dudu.commonlib.repo;

import android.content.Context;

import com.dudu.commonlib.utils.VersionTools;

/**
 * Created by dengjun on 2016/1/21.
 * Description :
 */
public class VersionManage {
    private boolean demoVersionFlag;
    private boolean portalFlag;

    public void init(Context context) {
        initVersionFlag(context);
    }

    private void initVersionFlag(Context context) {
        String ver = VersionTools.getAppVersion(context);

        demoVersionFlag = ver.contains("demo");
        portalFlag = !ver.contains("AIOS");
    }

    public boolean isDemoVersionFlag() {
        return demoVersionFlag;
    }

    public boolean isPortalNeeded() {
        return portalFlag;
    }
}
