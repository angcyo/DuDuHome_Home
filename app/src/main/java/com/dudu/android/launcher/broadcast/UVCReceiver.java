package com.dudu.android.launcher.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dudu.aios.ui.fragment.video.DrivingRecordFragment;
import com.dudu.commonlib.utils.File.KeyConstants;
import com.dudu.drivevideo.rearcamera.RearCameraManage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class UVCReceiver extends BroadcastReceiver {
    private final static String ACTION_UVC_CHANGED = "android.intent.action.UVC";

    private Logger log;
    private static int videoCount = 0;

    public UVCReceiver() {
        log = LoggerFactory.getLogger("video.reardrivevideo");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(!KeyConstants.is_agree_disclaimer)
            return;
        if (intent.getAction().equals(ACTION_UVC_CHANGED)) {
            String action = intent.getStringExtra("ACTION");
            String dev_name = intent.getStringExtra("DEVNAME");
            log.debug("收到UVCReceiver DEV广播:{}->Action:{}", dev_name, action);
            if ("add".equals(action)){
                if (videoCount == 0){
                    RearCameraManage.getInstance().getRearVideoConfigParam().setVideoDevice(dev_name);
                    RearCameraManage.getInstance().setPreviewEnable(true);

                    Observable
                            .timer(2, TimeUnit.SECONDS, Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(l->{
                                RearCameraManage.getInstance().openCamera();

                                if(ACCReceiver.isBackCarIng == true){
                                    log.info("正在倒车，恢复预览-------------");
                                    ACCReceiver.startBackCarPreview();
                                }
                                if(DrivingRecordFragment.isFrontCameraPreView == false){
                                    ACCReceiver.proDrivingView();
                                }
                            },throwable -> {log.error("异常", throwable);});

                    videoCount++;
                } else if (videoCount == 1) {
                    RearCameraManage.getInstance().getRearVideoConfigParam().setVideoRecordDevice(dev_name);
                    videoCount--;
                    RearCameraManage.getInstance().startRecord();//如果已经使能了开启录制，没有则实际不会开启录像
                }
            }else if ("remove".equals(action)){
            /*    if (RearVideoConfigParam.DEFAULT_VIDEO_DEVICE.equals(dev_name)){
                    log.info("停止预览 dev_name：{}", dev_name);
                    RearCameraManage.getInstance().setPreviewEnable(false);
                    RearCameraManage.getInstance().stopPreview();
                    RearCameraManage.getInstance().setCameraOpenFlag(false);
//                    RearCameraManage.getInstance().closeCamera();
                } else if (RearVideoConfigParam.DEFAULT_VIDEO_RECORD_DEVICE.equals(dev_name)) {
                    log.info("停止录像 dev_name：{}", dev_name);
//                    RearCameraManage.getInstance().stopRecord();
                }*/

                if (videoCount == 0){
                    videoCount++;
                    log.info("停止预览 dev_name：{}", dev_name);
                    RearCameraManage.getInstance().setPreviewEnable(false);
                    RearCameraManage.getInstance().stopPreview();
                    RearCameraManage.getInstance().setCameraOpenFlag(false);
                } else if (videoCount == 1)  {
                    videoCount--;
                    log.info("停止录像 dev_name：{}", dev_name);
                    RearCameraManage.getInstance().setRecording(false);
                }
            }
        }
    }


    private void sendBackCarBroadcast(Context context, boolean backFlag){
        Intent intent = new Intent("android.intent.action.ACC_BL");
        intent.putExtra("backed", backFlag);
        context.sendBroadcast(intent);
    }
}
