package com.dudu.drivevideo.exception;

import com.dudu.commonlib.utils.process.ProcessUtils;
import com.dudu.commonlib.utils.thread.ThreadPoolManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by dengjun on 2016/7/8.
 * Description :
 */
public class DriveVideoMonitor {
    public static final String Tag = DriveVideoMonitor.class.getName();
    private Logger log = LoggerFactory.getLogger("video1.frontdrivevideo");

    public static final int monitorTime = 10; //5秒
    private static boolean startMonitorFlag = true;


    public void startMonitor(){
        if (startMonitorFlag){
            log.info("添加监控任务");
            ThreadPoolManager.getInstance(Tag).schedule(()->{
                log.debug("时间到，执行监控动作");
                monitorAction();
            }, monitorTime, TimeUnit.SECONDS);
        }
    }

    public void cancerMonitor(){
        if (startMonitorFlag){
            log.debug("取消监控执行监控动作");
            ThreadPoolManager.getInstance(Tag).cancelTaskThreads(Tag,true);
        }
    }

    private void monitorAction(){
        log.info("监控 杀死launcher");
        ProcessUtils.killLauncherProcess();
    }
}
