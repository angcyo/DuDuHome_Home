package com.dudu.drivevideo.rearcamera;

import android.view.ViewGroup;

import com.dudu.drivevideo.config.RearVideoConfigParam;
import com.dudu.drivevideo.rearcamera.preview.PreviewState;
import com.dudu.drivevideo.rearcamera.preview.RearCamera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by dengjun on 2016/5/18.
 * Description :
 */
public class RearCameraManage implements BlurControl.IBlurListener {
    private static RearCameraManage instance = null;
    public static Logger log = LoggerFactory.getLogger("video.reardrivevideo");

    private RearCameraHandler rearCameraHandler;

    private RearCameraManage() {
        rearCameraHandler = new RearCameraHandler();
        BlurControl.instance().addBlurListener(this);
    }

    public static RearCameraManage getInstance() {
        if (instance == null) {
            synchronized (RearCameraManage.class) {
                if (instance == null) {
                    instance = new RearCameraManage();
                }
            }
        }
        return instance;
    }

    public void takePhoto() {
        rearCameraHandler.takePhoto();
    }

    public void init() {
        rearCameraHandler.init();
    }

    public void release() {
        rearCameraHandler.release();
    }


    public void openCamera() {
        rearCameraHandler.openCamera();
    }

    public void closeCamera() {
        rearCameraHandler.closeCamera();
    }


    public void startPreview() {
        rearCameraHandler.startPreview();
    }

    public void stopPreview() {
        rearCameraHandler.stopPreview();
    }

    public void startRecord() {
        rearCameraHandler.startRecord();
    }

    public void stopRecord() {
        rearCameraHandler.stopRecord();
    }

    public RearVideoConfigParam getRearVideoConfigParam() {
        return rearCameraHandler.getRearVideoConfigParam();
    }

    public void setPreviewEnable(boolean previewEnable) {
        rearCameraHandler.setPreviewEnable(previewEnable);
    }

    public void setRecordEnable(boolean recordEnable) {
        rearCameraHandler.setRecordEnable(recordEnable);
    }

    public boolean isPreviewIng() {
        return rearCameraHandler.getRearCamera().isPreviewIng();
    }

    public boolean isRearCameraExist() {
        return rearCameraHandler.getRearCamera().detectCamera();
    }

    public void setRearPreviewViewGroup(ViewGroup rearPreviewViewGroup) {
        rearCameraHandler.setRearPreviewViewGroup(rearPreviewViewGroup);
    }

    public void setPreviewState(PreviewState previewState) {
        rearCameraHandler.setPreviewState(previewState);
    }

    public boolean isBackCarPreviewMode() {
        if (rearCameraHandler.getPreviewState() == PreviewState.BACK_CAR_WINDOW_PREVIEW) {
            return true;
        } else {
            return false;
        }
    }

    public RearCamera getRearCameraPreview() {
        return rearCameraHandler.getRearCamera();
    }

    public void UsbToClient() {
        rearCameraHandler.UsbToClient();
    }

    public void UsbToHost() {
        rearCameraHandler.UsbToHost();
    }

    public void resetUsbMode(){
        rearCameraHandler.resetUsbMode();
    }

    public void setCameraOpenFlag(boolean cameraOpenFlag) {
        rearCameraHandler.getRearCamera().setCameraOpenFlag(cameraOpenFlag);
    }

    public void setRecording(boolean recording) {
        rearCameraHandler.getRearCameraRecorder().cancerSendVideoFinishMessage();
        rearCameraHandler.getRearCameraRecorder().setRecording(recording);
    }


    @Override
    public void onBlurChange(boolean isBlur) {
        log.info("监听到模糊状态改变为:{}", isBlur);
        if (isBlur) {
            //模糊的时候,停止后置预览
            RearCameraManage.getInstance().stopPreview();
        } else {
            //清晰的时候,显示后置预览
            if (!RearCameraManage.getInstance().isPreviewIng()) {
                RearCameraManage.getInstance().startPreview();
            }
        }
    }
}



