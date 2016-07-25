package com.dudu.drivevideo.frontcamera;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.dudu.android.hideapi.SystemPropertiesProxy;
import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.utils.process.ProcessUtils;
import com.dudu.drivevideo.exception.CameraErrorCode;
import com.dudu.drivevideo.exception.DirveVideoException;
import com.dudu.drivevideo.frontcamera.camera.CameraHandlerMessage;
import com.dudu.drivevideo.frontcamera.preview.PreviewState;
import com.dudu.drivevideo.video.VideoSaveTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by dengjun on 2016/5/22.
 * Description :
 */
public class DriveVideoHandler extends Handler {
    public static final int errorCountMaxToReboot = 20;
    private Logger log = LoggerFactory.getLogger("video1.frontdrivevideo");
    private DriveVideoHandler driveVideoHandler = this;
    private FrontCameraService frontCameraService;

    private int errorCount;

    public DriveVideoHandler(Looper looper, FrontCameraService frontCameraService) {
        super(looper);
        this.frontCameraService = frontCameraService;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            CameraHandlerMessage cameraHandlerMessage = (CameraHandlerMessage) msg.obj;
            log.info("收到消息：{}", cameraHandlerMessage.getMessage());
            switch (cameraHandlerMessage) {
                case INIT_CAMERA:
                    frontCameraService.getFrontCamera().initCamera();
                    break;
                case RELEASE_CAMERA:
                    frontCameraService.getFrontCamera().releaseCamera();
                    break;
                case START_RECORD:
                    startRecord();
                    break;
                /*case START_RECORD_ERROR:
                    frontCameraService.proCameraError();
                    break;*/
                case RELEASE_RECORD:
                    frontCameraService.getFrontCameraRecorder().releaseMediaRecorder();
                    break;
                case STOP_RECORD:
                    frontCameraService.getFrontCameraRecorder().stopRecord();
                    break;
                case RESET_RECORD:
                    frontCameraService.getFrontCameraRecorder().resetMediaRecorder();
                    break;
                case RELEASE_CAMERA_AND_RECORD:
                    frontCameraService.getFrontCameraRecorder().releaseMediaRecordAndCamera();
                    break;
                case START_PREVIEW:
                    startPreview();
                    break;
                case START_PREVIEW_ERROR:
                    frontCameraService.startPreview();
                    break;
                case STOP_PREVIEW:
                    frontCameraService.getFrontCamera().stopPreview();
                    break;
                case SAVE_VIDEO:
                    VideoSaveTools.saveCurVideoInfo(frontCameraService.getFrontCameraRecorder().getCurVideoFileAbsolutePath());
                    break;
                case TAKE_PICTURE:
                    frontCameraService.getPictureObtain().takePicture();
//                    frontCameraService.getPhotoObtain().takePicture();
                    break;
                case START_UPLOAD_VIDEO_STREAM:
                    log.debug(CameraHandlerMessage.START_UPLOAD_VIDEO_STREAM.getMessage());
                    frontCameraService.getUploadVideoStream().startUploadVideoStream();
                    frontCameraService.setStartVideoStream(true);
                    break;
                case STOP_UPLOAD_VIDEO_STREAM:
                    log.debug(CameraHandlerMessage.STOP_UPLOAD_VIDEO_STREAM.getMessage());
                    frontCameraService.getUploadVideoStream().stopUploadVideoStream();
                    frontCameraService.setStartVideoStream(false);
                    break;
                case DESTROY_UPLOAD_VIDEO_STREAM:
                    log.debug(CameraHandlerMessage.DESTROY_UPLOAD_VIDEO_STREAM.getMessage());
                    frontCameraService.getUploadVideoStream().destroyUploadVideoStream();
                    frontCameraService.setStartVideoStream(false);
                    break;
                case REGISTER_VIDEO_STREAM:
                    log.debug(CameraHandlerMessage.REGISTER_VIDEO_STREAM.getMessage());
                    final TcpSocketService socketService = frontCameraService.getSocketService();
                    if (socketService != null) {
                        socketService.repeatConnect(msg.arg1);
                    }
                    break;
            }
        } catch (DirveVideoException e) {
            CameraErrorCode errorCode = e.getErrorCode();
            log.error("异常：" + errorCode.getDetailErrMsg(), e);
            switch (errorCode) {
                case OPEN_CAMERA_ERROR:
                    ProcessUtils.killLauncherProcess();
                    frontCameraService.proCameraError();
                    break;
                case CLOSE_CAMERA_ERROR:
                case START_PREVIEW_ERROR:
                    frontCameraService.proCameraError();
                    break;
                case START_PREVIEW_SURFACE_NULL_ERROR:
                    sendMessageDelay(CameraHandlerMessage.START_PREVIEW_ERROR, 5);
                    break;
                case STOP_PREVIEW_ERROR:
                case STOP_RECORD_ERROR:
                    //not do nothing
                    break;
                case START_RECORD_ERROR:
                    frontCameraService.proCameraError();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("异常", e);
        }
    }

    private void startPreview() throws DirveVideoException {
        if (frontCameraService.getPreviewState() == PreviewState.MAINACITITY_PREVIEW && frontCameraService.getBlurGLSurfaceView() != null) {
            frontCameraService.getFrontCamera().startPreview(frontCameraService.getBlurGLSurfaceView().getSurfaceTexture());
        }
    }

    private void startRecord() throws DirveVideoException {
        if (frontCameraService.getFrontCamera().isPreviewIngFlag() == false) {
            log.info("预览未开启，直接返回再次执行开启录制流程");
            frontCameraService.startRecord();
            return;
        }
        if (/*FileUtil.isTFlashCardExists() && */frontCameraService.getFrontCameraRecorder().isRecordEnable()) {
            if (frontCameraService.getPreviewState() == PreviewState.MAINACITITY_PREVIEW && frontCameraService.getBlurGLSurfaceView() != null) {
                frontCameraService.getFrontCameraRecorder().startRecord();
            } else if (frontCameraService.getPreviewState() == PreviewState.WINDOW_PREVIEW) {
                frontCameraService.getFrontCameraRecorder().startRecord();
            }
        } else {
            log.info("录像未使能，不开启录像");
        }
    }


    public void sendMessage(CameraHandlerMessage cameraHandlerMessage) {
        if (driveVideoHandler != null) {
            Message message = driveVideoHandler.obtainMessage(cameraHandlerMessage.getNum(), cameraHandlerMessage);
            driveVideoHandler.sendMessage(message);
        }
    }

    public void sendMessage(CameraHandlerMessage cameraHandlerMessage, int arg1) {
        if (driveVideoHandler != null) {
            Message message = driveVideoHandler.obtainMessage(cameraHandlerMessage.getNum(), cameraHandlerMessage);
            message.arg1 = arg1;
            driveVideoHandler.sendMessage(message);
        }
    }

    public void sendMessageDelay(CameraHandlerMessage cameraHandlerMessage, int seconds) {
        if (driveVideoHandler != null) {
            Message message = driveVideoHandler.obtainMessage(cameraHandlerMessage.getNum(), cameraHandlerMessage);
            driveVideoHandler.sendMessageDelayed(message, seconds * 1000);
        }
    }


    public void removeMessage(CameraHandlerMessage cameraHandlerMessage) {
        if (driveVideoHandler != null) {
            driveVideoHandler.removeMessages(cameraHandlerMessage.getNum(), cameraHandlerMessage);
        }
    }

    public void removeAllMessage() {
        removeCallbacksAndMessages(null);
    }

    private void judgeToReboot() {
        log.info("行车记录模块出错次数：{}", errorCount++);
        if (errorCount > errorCountMaxToReboot) {
            log.info("出错次数超过，最大次数限制，重启设备");
            SystemPropertiesProxy.getInstance().set(CommonLib.getInstance().getContext(), "persist.sys.boot", "reboot");
        }
    }
}
