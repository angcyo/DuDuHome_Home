package tm.dudu.ext;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.dudu.commonlib.CommonLib;

public class GPSCtl {
    public static void on() {
        set(true);
    }

    public static void off() {
        set(false);
    }

    public static void startFixService() {
        try {
            CommonLib.getInstance().getContext().startService(getGPSFixServiceIntent());
        } catch (Exception e) {
        }
    }

    public static void stopFixService() {
        try {
            CommonLib.getInstance().getContext().stopService(getGPSFixServiceIntent());
        } catch (Exception e) {
        }
    }

    @NonNull
    private static Intent getGPSFixServiceIntent() {
        Intent i = new Intent();
        i.setComponent(new ComponentName("com.android.locationserver", "com.android.locationserver.LoacationService"));
        return i;
    }

    public static void set(boolean state) {
        Intent intent = new Intent("dudu.android.SET_GPS");
        intent.putExtra("on", state);
        try {
            CommonLib.getInstance().getContext().sendBroadcast(intent);
        } catch (ActivityNotFoundException exception) {

        }
    }
}
