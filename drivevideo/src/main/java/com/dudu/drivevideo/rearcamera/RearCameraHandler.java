package com.dudu.drivevideo.rearcamera;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.utils.File.FileUtil;
import com.dudu.drivevideo.config.RearVideoConfigParam;
import com.dudu.drivevideo.exception.CameraErrorCode;
import com.dudu.drivevideo.exception.DirveVideoException;
import com.dudu.drivevideo.rearcamera.camera.CameraHandlerMessage;
import com.dudu.drivevideo.rearcamera.camera.RearCameraListener;
import com.dudu.drivevideo.rearcamera.camera.RearCameraListenerMessage;
import com.dudu.drivevideo.rearcamera.camera.RearCameraRecorder;
import com.dudu.drivevideo.rearcamera.preview.PreviewState;
import com.dudu.drivevideo.rearcamera.preview.RearCamera;
import com.dudu.drivevideo.rearcamera.utils.DeviceUtils;
import com.dudu.drivevideo.rearcamera.utils.RearVideoDeleteTools;
import com.dudu.drivevideo.utils.RearVideoSaveTools;
import com.dudu.drivevideo.utils.UsbControl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by dengjun on 2016/5/18.
 * Description :
 */
public class RearCameraHandler implements RearCameraListener, SurfaceHolder.Callback {
    private RearCameraHandler rearCameraHandler = this;

    private HandlerThread handlerThread;
    private boolean handlerThreadInitFlag = false;

    private DriveVideoHandler driveVideoHandler;

    private RearVideoConfigParam rearVideoConfigParam;
    private RearCamera rearCamera;

    private SurfaceView rearCameraPreview;
    private ViewGroup rearPreviewViewGroup;
    private PreviewState previewState = PreviewState.MAINACITITY_PREVIEW;

    private RearCameraRecorder rearCameraRecorder;

    private Logger log = LoggerFactory.getLogger("video.reardrivevideo");

    public RearCameraHandler() {
        rearVideoConfigParam = new RearVideoConfigParam();
        rearCamera =  new RearCamera(rearVideoConfigParam);
        rearCamera.setRearCameraListener(this);

        rearCameraRecorder = new RearCameraRecorder(rearVideoConfigParam);
        rearCameraRecorder.setRearCameraListener(this);
    }

    private void initHandlerThread() {
        handlerThread = new HandlerThread("Rear Camera Thread") {
            @Override
            protected void onLooperPrepared() {
                super.onLooperPrepared();
                log.info("handlerThread 初始化完成");
                driveVideoHandler = new DriveVideoHandler(handlerThread.getLooper());
            }
        };
    }


    @Override
    public void onInfo(RearCameraListenerMessage rearCameraListenerMessage) {
        log.info("RearCameraInterface 回调信息 what = {}， message= {}", rearCameraListenerMessage.getWhat(), rearCameraListenerMessage.getMessage());
        if (rearCameraListenerMessage == RearCameraListenerMessage.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            if (FileUtil.isTFlashCardExists()) {
                sendMessage(CameraHandlerMessage.SAVE_VIDEO);
                sendMessage(CameraHandlerMessage.SET_VIDEO_PATH);
//                sendMessageDelay(CameraHandlerMessage.DELETE_UNUSED_264_VIDEO, 8);
            } else {
                log.info("sd卡不存在，停止录像");
                sendMessage(CameraHandlerMessage.STOP_RECORD);
                sendMessage(CameraHandlerMessage.DELETE_CUR_VIDEO);
            }
        }else if (rearCameraListenerMessage == RearCameraListenerMessage.PREVIEW_FINISH){
            removeRearPreview();
        }
    }

    @Override
    public void onError(RearCameraListenerMessage rearCameraListenerMessage) {
        log.error("mediaRedcord 报错 what = {}， message = {}", rearCameraListenerMessage.getWhat(), rearCameraListenerMessage.getMessage());
        switch (rearCameraListenerMessage) {
            case CAMERA_ERROR:
                closeCamera();
                break;
            case PREVIEW_ERROR:
                proPreviewError();
                break;
            case RECORD_ERROR:
               proRecordError();
                break;
        }
    }

    private void proRecordError(){
        sendMessage(CameraHandlerMessage.STOP_RECORD);
        sendMessage(CameraHandlerMessage.DELETE_CUR_VIDEO);
        if (rearCameraRecorder.detectCamera()){
            sendMessageDelay(CameraHandlerMessage.SET_VIDEO_PATH, 3);
            sendMessageDelay(CameraHandlerMessage.START_RECORD, 3);
        }else {
            log.info("proRecordError 设备文件不存在，不开启录制");
        }
    }

    public synchronized void init() {
        synchronized (this) {
            if (handlerThread == null && handlerThreadInitFlag == false) {
                log.info("RearCameraHandler初始化");

                initHandlerThread();
                handlerThread.start();
                handlerThreadInitFlag = true;
            } else {
                log.info("handlerThread 已经初始化了");
            }
        }
    }

    /**
     * 拍照
     */
    public synchronized void takePhoto() {
        if (rearCameraPreview != null && rearCamera.isPreviewIng()) {
            sendMessage(CameraHandlerMessage.TAKE_PICTURE);
        } else {
            log.info("未开始预览.");
        }
    }

    public synchronized void release() {
        synchronized (this) {
            if (handlerThread != null && handlerThread.isAlive()) {
                log.info("RearCameraHandler释放");
                driveVideoHandler.removeAllMesages();
                stopPreview();
                closeCamera();
                stopRecord();
                handlerThread.quitSafely();
                driveVideoHandler = null;
                handlerThread = null;
                handlerThreadInitFlag = false;
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        log.debug("RearCameraHandler surfaceCreated创建");
        startPreviewAciton();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        log.debug("RearCameraHandler  surfaceChanged改变  format = {}  w = {}  h=  {}", format, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        log.debug("RearCameraHandler surfaceDestroyed销毁");
    }

    public synchronized void openCamera() {
        sendMessage(CameraHandlerMessage.INIT_CAMERA);
    }

    public synchronized void closeCamera() {
        sendMessage(CameraHandlerMessage.RELEASE_CAMERA);
    }

    public synchronized void startPreviewAciton() {
        log.info("预览使能状态：{}, 是否正在预览：{}", rearCamera.isPreviewEnable(), rearCamera.isPreviewIng());
        if (rearCamera.detectCamera()) {
            rearCamera.setPreviewEnable(true);//防止有launcher重启，有设备文件的情况下不能预览
            if (rearCamera.isPreviewIng() == false) {
                rearCamera.stopPreview();
                openCamera();
                sendMessage(CameraHandlerMessage.START_PREVIEW);
            }else {
                log.info("后置预览正在预览");
            }
        } else {
            log.info("后置预览设备文件不存在，无法开启预览");
        }
        sendMessage(CameraHandlerMessage.PRINT_DEV_NAME);
    }

    public synchronized void startPreview(){
        log.debug("开启后置预览----");
        if (rearCamera.detectCamera() == false){
            log.info("startPreview 后置预览设备文件不存在，无法开启预览");
            return;
        }

        if (rearPreviewViewGroup != null){
            rearPreviewViewGroup.post(()->{
                rearCameraPreview = new SurfaceView(CommonLib.getInstance().getContext());
                rearCameraPreview.getHolder().addCallback(this);

                if (rearCameraPreview.getParent() == null && rearPreviewViewGroup != null){
                    log.debug("加入后置预览----");
                    rearPreviewViewGroup.removeAllViews();
                    rearCameraPreview.setZOrderMediaOverlay(true);
                    rearPreviewViewGroup.addView(rearCameraPreview);
                }else {
                    log.debug("rearPreviewViewGroup 为null，不开启预览 ");
                }
            });
        }
    }


    private synchronized void proPreviewError() {
        stopPreview();
        closeCamera();
    }

    public synchronized void stopPreview() {
        if (driveVideoHandler != null) {
            driveVideoHandler.removeMessages(CameraHandlerMessage.START_PREVIEW.getNum(), CameraHandlerMessage.START_PREVIEW);
            driveVideoHandler.removeMessages(CameraHandlerMessage.INIT_CAMERA.getNum(), CameraHandlerMessage.INIT_CAMERA);
        }
//        sendMessage(CameraHandlerMessage.STOP_PREVIEW);
        rearCamera.stopPreview();
    }


    private void removeRearPreview(){
        if (rearPreviewViewGroup != null &&rearCameraPreview != null &&  rearCameraPreview.getParent() != null){
            rearPreviewViewGroup.post(()->{
                log.debug("移除后置预览-----");
                rearPreviewViewGroup.removeView(rearCameraPreview);
                rearCameraPreview = null;
            });
        }
    }

    public synchronized void startRecord() {
        log.debug("startRecord 后置录制状态：{}，使能状态：{}", rearCameraRecorder.isRecording(), rearCameraRecorder.isRecordEnable());
        if (rearCameraRecorder.detectCamera() && !rearCameraRecorder.isRecording()) {
            sendMessage(CameraHandlerMessage.SET_VIDEO_PATH);
            sendMessage(CameraHandlerMessage.START_RECORD);
        }
    }

    public synchronized void stopRecord() {
        log.debug("stopRecord 后置录制状态：{}，使能状态：{}", rearCameraRecorder.isRecording(), rearCameraRecorder.isRecordEnable());
        sendMessage(CameraHandlerMessage.STOP_RECORD);
        sendMessage(CameraHandlerMessage.SAVE_VIDEO);
    }

    private void sendMessage(CameraHandlerMessage cameraHandlerMessage) {
        if (handlerThread != null && handlerThread.isAlive() && driveVideoHandler != null) {
            Message message = driveVideoHandler.obtainMessage(cameraHandlerMessage.getNum(), cameraHandlerMessage);
            driveVideoHandler.sendMessage(message);
        }
    }

    private void sendMessageDelay(CameraHandlerMessage cameraHandlerMessage, int seconds) {
        if (handlerThread != null && handlerThread.isAlive() && driveVideoHandler != null) {
            Message message = driveVideoHandler.obtainMessage(cameraHandlerMessage.getNum(), cameraHandlerMessage);
            driveVideoHandler.sendMessageDelayed(message, seconds * 1000);
        }
    }

    public RearVideoConfigParam getRearVideoConfigParam() {
        return rearVideoConfigParam;
    }

    public void setPreviewEnable(boolean previewEnable) {
        rearCamera.setPreviewEnable(previewEnable);
    }

    public void setRecordEnable(boolean recordEnable) {
        rearCameraRecorder.setRecordEnable(recordEnable);
    }



    public RearCamera getRearCamera() {
        return rearCamera;
    }

    public void setRearPreviewViewGroup(ViewGroup rearPreviewViewGroup) {
        this.rearPreviewViewGroup = rearPreviewViewGroup;
    }

    public PreviewState getPreviewState() {
        return previewState;
    }

    public void setPreviewState(PreviewState previewState) {
        this.previewState = previewState;
    }

    public void UsbToClient() {
        sendMessage(CameraHandlerMessage.USB_TO_CLIENT);
    }

    public void UsbToHost() {
        sendMessage(CameraHandlerMessage.USB_TO_HOST);
    }

    public void resetUsbMode(){
        sendMessage(CameraHandlerMessage.USB_TO_CLIENT);
        sendMessageDelay(CameraHandlerMessage.USB_TO_HOST,1);
    }

    public synchronized void proCameraOpenError(){
        if (driveVideoHandler != null){
            log.debug("错误次数：{}", driveVideoHandler.errorNum);
            driveVideoHandler.errorNum++;
            if (driveVideoHandler.errorNum >= 4){
                driveVideoHandler.errorNum = 0;
                driveVideoHandler.removeAllMesages();
                stopPreview();
                closeCamera();
                stopRecord();
                sendMessage(CameraHandlerMessage.USB_TO_CLIENT);
                sendMessageDelay(CameraHandlerMessage.USB_TO_HOST,1);
            }else {
                closeCamera();
            }
        }
    }

    public RearCameraRecorder getRearCameraRecorder() {
        return rearCameraRecorder;
    }

    private class DriveVideoHandler extends Handler {
        public int errorNum = 0;

        public DriveVideoHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                CameraHandlerMessage cameraHandlerMessage = (CameraHandlerMessage) msg.obj;
                log.info("收到消息：{}", cameraHandlerMessage.getMessage());
                switch (cameraHandlerMessage) {
                    case INIT_CAMERA:
                        if (rearCamera.isPreviewEnable()) {
                            rearCamera.init();
                        }
                        errorNum = 0;
                        break;
                    case RELEASE_CAMERA:
                        rearCamera.release();
                        break;
                    case START_RECORD:
                        if (FileUtil.isTFlashCardExists() && rearCameraRecorder.isRecordEnable()) {
                            rearCameraRecorder.startRecord();
                        } else {
                            log.info("TF卡不存在或者录制没有使能，不开启录像");
                            rearCameraHandler.sendMessage(CameraHandlerMessage.STOP_RECORD);
                            rearCameraHandler.sendMessage(CameraHandlerMessage.DELETE_CUR_VIDEO);
                        }
                        break;
                    case STOP_RECORD:
                        rearCameraRecorder.stopRecord();
                        break;
                    case TAKE_PICTURE:
                        rearCamera.takePhoto();
                        break;
                    case START_PREVIEW:
                        if (rearCamera.isPreviewEnable() && rearCamera.isPreviewIng() == false) {
                            if (previewState == PreviewState.MAINACITITY_PREVIEW) {
                                rearCamera.startPreview(rearCameraPreview.getHolder());
                            } else if (previewState == PreviewState.BACK_CAR_WINDOW_PREVIEW) {
                                rearCamera.startPreview();
                            }
                        }
                        break;
                    case STOP_PREVIEW:
                        rearCamera.stopPreview();
                        break;
                    case SAVE_VIDEO:
                        RearVideoSaveTools.saveCurVideoInfo(rearCameraRecorder.getCurVideoPath(), rearCameraRecorder.getCurVideoThumbnailPath());
                        break;
                    case SET_VIDEO_PATH:
                        rearCameraRecorder.setCurVideoPath(RearVideoSaveTools.generateCurVideoNameMp4(), RearVideoSaveTools.generateCurVideoThumbnailPath());
                        break;
                    case DELETE_CUR_VIDEO:
                        RearVideoSaveTools.deleteCurVideo(rearCameraRecorder.getCurVideoPath());
                        break;
                    case DELETE_UNUSED_264_VIDEO:
                        RearVideoDeleteTools.deleteUnUsed264VideoFile(rearCameraRecorder.getCurVideoPath());
                        break;
                    case USB_TO_CLIENT:
                        UsbControl.setToClient();
                        break;
                    case USB_TO_HOST:
                        UsbControl.setToHost();
                        break;
                    case PRINT_DEV_NAME:
                        DeviceUtils.printDevVideos();
                        break;
                }
            } catch (DirveVideoException e) {
                CameraErrorCode errorCode = e.getErrorCode();
                log.error(errorCode.getDetailErrMsg(), e);
                switch (errorCode) {
                    case OPEN_CAMERA_ERROR:
                        proCameraOpenError();
                        break;
                    case CLOSE_CAMERA_ERROR:
                    case START_PREVIEW_ERROR:
                        proPreviewError();
                        break;
                    case STOP_PREVIEW_ERROR:
                    case STOP_RECORD_ERROR:
                        //not do nothing
                        break;
                    case START_RECORD_ERROR:
                        rearCameraHandler.sendMessage(CameraHandlerMessage.START_RECORD);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                log.error("异常", e);
            }
        }

        public void removeAllMesages(){
            this.removeCallbacksAndMessages(null);
        }
    }
}
