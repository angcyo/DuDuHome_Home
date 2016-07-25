package com.dudu.drivevideo.rearcamera.camera;

import com.dudu.aios.uvc.Native;
import com.dudu.commonlib.utils.File.FileUtil;
import com.dudu.drivevideo.config.RearVideoConfigParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

/**
 * Created by dengjun on 2016/5/18.
 * Description :
 */
public class RearCameraRecorder {
    private RearVideoConfigParam rearVideoConfigParam;
    private String curVideoPath;
    private String curVideoThumbnailPath;

    private RearCameraListener rearCameraListener;
    private Subscription recordSubscription;

    private boolean recordEnable = false;
    /* 标记是否在录像*/
    private boolean isRecording = false;

    private Logger log = LoggerFactory.getLogger("video.reardrivevideo");

    public RearCameraRecorder(RearVideoConfigParam rearVideoConfigParam) {
        this.rearVideoConfigParam = rearVideoConfigParam;
    }


    public void startRecord() {
        if (isRecording == true){
            log.info("后置正在录像，不重复开启");
            return;
        }
        Thread recordThread = new Thread(()->{
            log.info("开启后置录像:{}", "/dev/"+rearVideoConfigParam.getVideoRecordDevice());
            isRecording = true;
            int ret = Native.uvcAP("/dev/"+rearVideoConfigParam.getVideoRecordDevice());
            isRecording = false;
            log.info("UVC AP 录像结束 Exit:{}", ret);
            preStartRecordReturnValue(ret);
        });
        recordThread.setName("Rear camera record");
        recordThread.setPriority(Thread.MAX_PRIORITY);
        recordThread.start();
    }

    private void preStartRecordReturnValue(int ret){
        if (ret == 1){
            if (rearCameraListener != null){
                rearCameraListener.onError(RearCameraListenerMessage.RECORD_ERROR);
            }
        }
    }

    public void stopRecord() {
        log.info("停止后置录像");
        cancerSendVideoFinishMessage();
        Native.uvcStop();
        isRecording = false;
    }

    public void cancerSendVideoFinishMessage(){
        if (recordSubscription != null && !recordSubscription.isUnsubscribed()){
            recordSubscription.unsubscribe();
        }
    }

    public void setRearCameraListener(RearCameraListener rearCameraListener) {
        this.rearCameraListener = rearCameraListener;
    }

    public void setCurVideoPath(String videoPath) {
        curVideoPath = videoPath;
        log.info("当前录像文件路径：{}", curVideoPath);
        byte[] fn = curVideoPath.getBytes();
        Native.uvcSetFileName(fn, fn.length);

        delaySendVideoFinishMessage();
    }

    public void setCurVideoPath(String videoPath, String curVideoThumbnailPath) {
        curVideoPath = videoPath;
        this.curVideoThumbnailPath = curVideoThumbnailPath;
        log.info("当前录像文件路径：{}", curVideoPath);
        byte[] fn = curVideoPath.getBytes();
        byte[] tp = curVideoThumbnailPath.getBytes();
        Native.uvcSetThumbnailFileName(tp, tp.length);
        Native.uvcSetFileName(fn, fn.length);
        delaySendVideoFinishMessage();
    }

    public String getCurVideoPath() {
        return curVideoPath;
    }

    private void delaySendVideoFinishMessage(){
        recordSubscription =
                Observable
                        .timer(60, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.newThread())
                        .subscribe(l->{
                            if (rearCameraListener != null){
                                rearCameraListener.onInfo(RearCameraListenerMessage.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED);
                            }
                        },throwable -> {
                            log.error("异常", throwable);
                        });
    }

    public boolean detectCamera(){
        return FileUtil.detectFileExist("/dev/"+ rearVideoConfigParam.getVideoRecordDevice());
    }

    public boolean isRecordEnable() {
        return recordEnable;
    }

    public void setRecordEnable(boolean recordEnable) {
        this.recordEnable = recordEnable;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }

    public String getCurVideoThumbnailPath() {
        return curVideoThumbnailPath;
    }

    public void setCurVideoThumbnailPath(String curVideoThumbnailPath) {
        this.curVideoThumbnailPath = curVideoThumbnailPath;
    }
}
