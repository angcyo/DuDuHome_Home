package com.dudu.monitor.obd;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;

import com.dudu.commonlib.CommonLib;
import com.dudu.monitor.event.WaterWarningEvent;
import com.dudu.monitor.obd.modol.CoolantTemperatureData;
import com.dudu.monitor.obd.modol.ObdRTData;
import com.dudu.workflow.common.RequestFactory;
import com.dudu.workflow.obd.WaterWarningData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import de.greenrobot.event.EventBus;

/**
 *  冷却液水温管理类
 * Created by Robert on 2016/7/4.
 */
public class CoolantTemperatureManager {

    private Logger log = LoggerFactory.getLogger("CoolantTemperatureManager");

    private static CoolantTemperatureManager instance = null;

    private static Context appContext = null;

    /*判断水温是否过高的阈值*/
    public static final float TEMPERATURE_THRESHOLD = 115;

    /*预警最小时间间隔*/
    public static final float MIN_TIME_INTERVAL = 5*60*1000; //30*1000;//

    /*上一次的水温是否过高标志*/
    private boolean lastFlag = false;

    /*上一次水温过高预结束的时间 for launcher*/
    private long lastHighTime = 0;

    /*上一次水温过高预警的开始的时间 for app*/
    private long lastAppHighTime = 0;

    /*是否正在显示告警*/
    private boolean isDisplayWarning = false;


    /**
     *  获取水温管理类实例,采用单例管理
     *
     * @return instance 水温管理类实例
     */
    public static CoolantTemperatureManager getInstance()
    {
        if(instance == null)
        {
            instance = new CoolantTemperatureManager();

            appContext = CommonLib.getInstance().getContext();
        }
        return instance;
    }

    /**
     * 从实时OBD数据中得到冷却液水温数据，根据是否超标来决定是否进行上传和弹窗预警
     *
     * @param obdRTData 订阅得到的实时OBD数据
     */
    public void checkCoolantTemperature(ObdRTData obdRTData)
    {
        boolean currentHighFlag = false;
        float currentTemperature = obdRTData.getEngCoolant();

        if(!(TEMPERATURE_THRESHOLD > currentTemperature))
        {
            //水温告警
            currentHighFlag = true;
        }
        else
        {
            //水温正常
            currentHighFlag = false;
        }

//        log.info("WaterWarning -> currentHighFlag {}",currentHighFlag);
//        log.info("WaterWarning -> currentTemperature {}",currentTemperature);

        CoolantTemperatureData coolantTemperatureData = new CoolantTemperatureData();
        coolantTemperatureData.setEngCoolant(currentTemperature);
        coolantTemperatureData.setHighFlag(currentHighFlag);



        //通知UI界面
        EventBus.getDefault().post(new WaterWarningEvent(coolantTemperatureData));


        //网络发送通知手机app
        WaterWarningData waterWarningData = new WaterWarningData(coolantTemperatureData.getEngCoolant(),coolantTemperatureData.isHighFlag());
        sendToApp(waterWarningData);



        setLastFlag(currentHighFlag);
    }

    public long getLastHighTime() {
        return this.lastHighTime;
    }

    public void setLastHighTime(long lastHighTime) {
        this.lastHighTime = lastHighTime;
    }

    public  boolean isLastFlag() {
        return this.lastFlag;
    }

    public  void setLastFlag(boolean lastFlag) {
        this.lastFlag = lastFlag;
    }

    public boolean isTopActivity()
    {
        String TAG = "WaterWarningActivity";
        boolean isTop = false;
        ActivityManager am = (ActivityManager) appContext.getSystemService(appContext.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
//        log.info("isTopActivity = " + cn.getClassName());
        if (cn.getClassName().contains(TAG))
        {
            isTop = true;
        }
//        log.info("isTop = " + isTop);
        return isTop;
    }

    public boolean isForeground(Context context, String className) {
        if (context == null || TextUtils.isEmpty(className)) {
            return false;
        }

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ComponentName cpn = list.get(0).topActivity;
            if (className.equals(cpn.getClassName())) {
                return true;
            }
        }

        return false;
    }

    public boolean isDisplayWarning() {
        return isDisplayWarning;
    }

    public void setDisplayWarning(boolean displayWarning) {
        isDisplayWarning = displayWarning;
    }

    private void sendToApp(WaterWarningData waterWarningData)
    {

        if(!isNeedToSend(waterWarningData))
        {
//            log.info("WaterWarning 无需上报.");
            return;
        }

//        log.info("WaterWarning 需要上报.");


        RequestFactory.getWaterWarningRequest().uploadWaterWarning(waterWarningData).subscribe(requestResponse -> {
            if (requestResponse.resultCode == 0) {
                log.info("WaterWarning 信息,上报成功.");
            } else {
                log.info("WaterWarning 信息,上报失败.");
            }
        }, throwable -> {
            log.error(" WaterWarning 信息,上报异常:{}",throwable);
        });
    }

    private boolean isNeedToSend(WaterWarningData waterWarningData)
    {

        float currentTemperature = waterWarningData.getEngCoolant();
        final long currentTime = System.currentTimeMillis();
        if(!(TEMPERATURE_THRESHOLD > currentTemperature)) //水温超标
        {
            if(MIN_TIME_INTERVAL < (currentTime - getLastAppHighTime()))
            {
                //记录app告警弹窗时间
                setLastAppHighTime(currentTime);
                return true;
            }
        }

        return false;
    }

    public long getLastAppHighTime() {
        return lastAppHighTime;
    }

    public void setLastAppHighTime(long lastAppHighTime) {
        this.lastAppHighTime = lastAppHighTime;
    }
}
