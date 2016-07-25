package com.dudu.android.launcher.broadcast;

public class BootReceiver /*extends BroadcastReceiver */{
   /* private final static String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    private Logger log;

    public BootReceiver() {
        log = LoggerFactory.getLogger("init.receiver.boot");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_BOOT_COMPLETED)) {
            log.debug("onReceive boot completed:{}", intent.getExtras());

            //权宜之计，kill掉，重启确保后置可以预览
            android.os.Process.killProcess(android.os.Process.myPid());

//            ShellExe.execShellCmd("am start com.dudu.android.launcher");
        }
    }*/
}
