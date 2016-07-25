package tm.dudu.ext;

import com.dudu.android.hideapi.SystemPropertiesProxy;
import com.dudu.commonlib.CommonLib;

/**
 * Created by sean on 6/15/16.
 */
public class PortalCtl {
    public static void start() {
        SystemPropertiesProxy.getInstance().set(CommonLib.getInstance().getContext(), "persist.sys.nodog", "start");
    }

    public static void stop() {
        SystemPropertiesProxy.getInstance().set(CommonLib.getInstance().getContext(), "persist.sys.nodog", "stop");
    }

    public static String queryStatus() {
        return SystemPropertiesProxy.getInstance().get("persist.sys.nodog", "stop");
    }

    public static void queryVisitedAsync() {
        SystemPropertiesProxy.getInstance().set(CommonLib.getInstance().getContext(), "persist.sys.nodog", "views");
    }

    public static String getVisited() {
        return SystemPropertiesProxy.getInstance().get("persist.sys.views", "0");
    }
}
