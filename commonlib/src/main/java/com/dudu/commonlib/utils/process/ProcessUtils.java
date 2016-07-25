package com.dudu.commonlib.utils.process;

import android.app.ActivityManager;
import android.content.Context;

import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.utils.File.SharedPreferencesUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by dengjun on 2016/5/23.
 * Description :
 */
public class ProcessUtils {
    public static Logger log = LoggerFactory.getLogger("commonlib.ProcessUtils");

    /**
     * 上一次launcher启动的进程id key
     */
    public static final String LAST_LAUNCHER_PROCESS_ID = "last_launcher_process_Id";

    public  static  String getCurProcessName (Context context) {
        int     pid = android.os.Process.myPid ();
        ActivityManager mActivityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningAppProcessInfo appProcess : mActivityManager.getRunningAppProcesses())  {
            if(appProcess.pid == pid){
                return appProcess.processName;
            }
        }
        return null ;
    }


    /**
     * 删除processName的僵尸进程
     * @param context
     * @param processName
     */
    public static void killZombieProcess(Context context, String processName){
        int     pid = android.os.Process.myPid ();
        log.debug("当前进程ID：{}", pid);
        ActivityManager mActivityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningAppProcessInfo appProcess : mActivityManager.getRunningAppProcesses())  {
            if (processName.equals(appProcess.processName) && appProcess.pid != pid){
                log.debug("杀死当前进程的僵尸进程ID：{}", pid);
                android.os.Process.killProcess(appProcess.pid);
            }
        }
    }

    /**
     * 判断当前进程id是否是僵尸进程
     * @param context
     * @param processId
     * @return
     */
    public static boolean isZombieProcess(Context context, int processId){
        ActivityManager mActivityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningAppProcessInfo appProcess : mActivityManager.getRunningAppProcesses())  {
            if (appProcess.pid == processId){
                log.info("进程：{}，不是僵尸进程", processId);
                return false;
            }
        }
        log.info("进程：{}，是僵尸进程", processId);
        return true;
    }

    /**
     * 杀死指定的僵尸进程
     * @param context
     * @param processId
     */
    public static void killAppointZombieProcess(Context context, int processId){
        if (isZombieProcess(context,processId)){
            log.debug("杀死僵尸进程ID：{}", processId);
            android.os.Process.killProcess(processId);
        }
    }


    public static void saveProcessId(Context context, String key, int processId){
        SharedPreferencesUtil.putIntValue(context, key, processId);
    }

    public static int getProcessId(Context context, String key){
        return SharedPreferencesUtil.getIntValue(context, key, -1);
    }


    public static void killZombieProcessFlow(Context context, String  key){
        int processId = getProcessId(context, key);
        log.info("当前进程ID：{}, 前一次启动进程ID：{}",  android.os.Process.myPid (), processId);
        if (processId != -1){
            killAppointZombieProcess(context, processId);
        }
        saveProcessId(context, key, android.os.Process.myPid ());
    }

    public static void killLauncherZombieProcessFlow(Context context){
        killZombieProcessFlow(context, LAST_LAUNCHER_PROCESS_ID);
    }

    public static void killLauncherProcess(){
        killPackageProcess(CommonLib.getInstance().getContext(), "dudu.com.dudu.android.launcher");
    }

    public static void killPackageProcess(Context context, String packageName){
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        try {
            log.info("杀死包名：{} 的所有进程", packageName);
            Method method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
            method.invoke(mActivityManager, packageName);  //packageName是需要强制停止的应用程序包名
        } catch (IllegalAccessException e) {
            log.error("异常",e);
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            log.error("异常",e);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            log.error("异常",e);
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            log.error("异常",e);
            e.printStackTrace();
        } catch (Exception e){
            log.error("异常",e);
        }
    }
}
