package com.dudu.drivevideo.frontcamera.camera;

import android.hardware.Camera;

import com.blur.SoundPlayManager;
import com.dudu.drivevideo.rearcamera.utils.PictureUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by dengjun on 2016/2/19.
 * Description :
 */
public class PictureObtain implements Camera.PictureCallback {
    Logger log = LoggerFactory.getLogger("video1.frontdrivevideo");

    private FrontCamera  frontCamera;

    private long takeTimeStamp = 0;

    private Camera.ShutterCallback shutterCallback = null;
    public AtomicInteger takePictureCount = new AtomicInteger(0);

    public PictureObtain(FrontCamera frontCamera) {
        this.frontCamera = frontCamera;
    }

    public void takePicture() {
        if (frontCamera.getCamera() != null && (System.currentTimeMillis() - takeTimeStamp > 800)) {
            try {
                log.debug("PictureObtain takePicture");
                SoundPlayManager.play();//拍照声效
                frontCamera.getCamera().takePicture(null, null, this);
                takeTimeStamp = System.currentTimeMillis();
            } catch (Exception e) {
                log.error("异常",e);
                takeTimeStamp = System.currentTimeMillis();
            }
        }else {
            log.debug("过滤800毫秒内的拍照");
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        try {
            log.debug("PictureObtain onPictureTaken");
            PictureUtil.savePicture(data);
            if (shutterCallback != null){
                shutterCallback.onShutter();
            }
        } catch (Exception e) {
            log.error("异常", e);
        }
    }


    public void setShutterCallback(Camera.ShutterCallback shutterCallback) {
        this.shutterCallback = shutterCallback;
    }
}
