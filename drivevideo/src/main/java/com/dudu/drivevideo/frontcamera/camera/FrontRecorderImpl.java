package com.dudu.drivevideo.frontcamera.camera;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;

import com.dudu.drivevideo.config.FrontVideoConfigParam;
import com.dudu.drivevideo.frontcamera.FrontCameraInstance;
import com.dudu.drivevideo.frontcamera.event.VideoEvent;
import com.dudu.drivevideo.utils.AudioUtils;
import com.dudu.drivevideo.video.VideoSaveTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.greenrobot.event.EventBus;

/**
 * Created by poecao on 16-6-14.
 */
public class FrontRecorderImpl implements MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener {

    public static MediaRecorder mMediaRecorder;
    private static String curFileName;//保存当前录像的文件路径
    private boolean isRecording = false;
    private boolean recordEnable = false;
    private boolean isKilled = false;
    private FrontVideoConfigParam frontVideoConfigParam;
    private Logger log = LoggerFactory.getLogger("video1.frontdrivevideo");

    public FrontRecorderImpl() {
        frontVideoConfigParam = new FrontVideoConfigParam();
    }

    public static String getVideoFileName() {
        curFileName = VideoSaveTools.generateCurVideoName();
        return curFileName;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isRecorderEnable() {
        return recordEnable;
    }

    public void setRecorderEnable(boolean enable) {
        recordEnable = enable;
    }

    public void setKilled(boolean killed) {
        isKilled = killed;
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        log.debug("onInfo");
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {

            if(mMediaRecorder != null)
            {
                if (!isKilled) {
                    mMediaRecorder.reset();
                }
            }
            log.debug("start to save current video info -> curFileName = "+curFileName);
            VideoSaveTools.saveCurVideoInfo(curFileName);//保存录像文件至数据库

            loopRecorder(getVideoFileName());
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        log.debug("onError " + what + " " + extra);
        if (what == MediaRecorder.MEDIA_ERROR_SERVER_DIED) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            isRecording = false;
            EventBus.getDefault().post(new VideoEvent(VideoEvent.OFF));
            //startRecorder();
        }

        FrontCameraInstance.getInstance().releaseCamera();
        FrontCameraInstance.getInstance().startPreview();
    }

    public void startRecorder() {
        log.debug("isRecording = " + isRecording);
        if (!isRecording && isRecorderEnable()) {
            isRecording = true;
            EventBus.getDefault().post(new VideoEvent(VideoEvent.ON));
            loopRecorder(getVideoFileName());
        }
    }

    public void releaseRecorder() {
        log.debug("isRecording = " + isRecording + " mMediaRecorder = " + mMediaRecorder);
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder = null;
            log.debug("stopRecorder mMediaRecorder.reset()");
        }

        isRecording = false;
        EventBus.getDefault().post(new VideoEvent(VideoEvent.OFF));

        if (curFileName != null) {
            VideoSaveTools.saveCurVideoInfo(curFileName);//保存录像文件至数据库
            curFileName = null;
        }
    }

    public void loopRecorder(String filePath) {
        log.debug("loopRecorder");
        log.debug("start next new record -> filePath = "+filePath);

        if (isRecording == false || isKilled == true) {
            return;
        }

        log.debug("filePath = " + filePath);
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }

        Camera cameraDevice = FrontCameraInstance.getInstance().getCameraDevice();
        if (cameraDevice == null) {
            log.debug("camera == null, 终止录制");
            isRecording = false;
            EventBus.getDefault().post(new VideoEvent(VideoEvent.OFF));
            return;
        }

        log.debug("MediaRecorder reset before");
        mMediaRecorder.reset();
        log.debug("MediaRecorder reset after");

        /*if (!isSetPreview) {
            try {
                cameraDevice.stopPreview();
                cameraDevice.setDisplayOrientation(0);
                cameraDevice.setPreviewTexture(mSurfaceTexture);
                isSetPreview = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/

        cameraDevice.unlock();
        mMediaRecorder.setCamera(cameraDevice);

        mMediaRecorder.setOnInfoListener(this);
        mMediaRecorder.setOnErrorListener(this);

        CamcorderProfile mProfile = initCamcorderProfile();
        initParam(mProfile);

        mMediaRecorder.setMaxDuration(frontVideoConfigParam.getVideoInterval());
        log.debug("MediaRecorder setOutputFile before");
        mMediaRecorder.setOutputFile(filePath);
        log.debug("MediaRecorder setOutputFile after");

        try {

            log.debug("MediaRecorder prepare before");
            mMediaRecorder.prepare();
            log.debug("MediaRecorder prepare after");
            mMediaRecorder.start();
            log.debug("MediaRecorder start after");
            log.debug("开始录制:" + filePath);
        } catch (Exception e) {
            log.error("poecao 录制失败", e);
            isRecording = false;
            EventBus.getDefault().post(new VideoEvent(VideoEvent.OFF));
            cameraDevice.lock();
            //e("重新 录制失败:" + e.getMessage());
            e.printStackTrace();
            //mHandler.sendEmptyMessage(MSG_ERROR);
        }

    }

    private CamcorderProfile initCamcorderProfile() {
        CamcorderProfile profile = CamcorderProfile.get(Camera.CameraInfo.CAMERA_FACING_BACK, frontVideoConfigParam.getQuality());

        profile.audioSampleRate = 16000;//16K, 必须;
        profile.audioCodec = MediaRecorder.AudioEncoder.AMR_WB;//必须
//        profile.audioChannels = 1;//录音通道 单通道

        profile.videoBitRate = frontVideoConfigParam.getVideoBitRate();
        profile.videoFrameWidth = frontVideoConfigParam.getWidth();
        profile.videoFrameHeight = frontVideoConfigParam.getHeight();
        profile.videoFrameRate = frontVideoConfigParam.getRate();
        profile.videoCodec = MediaRecorder.VideoEncoder.H264;
        return profile;
    }

    private void initParam(CamcorderProfile profile) {
        boolean isMultiMic = AudioUtils.isMultiMic();
        if (isMultiMic) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        }

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);

        if (isMultiMic) {
            /*一个都不能少*/
            mMediaRecorder.setAudioChannels(profile.audioChannels);
            mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
            mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
            mMediaRecorder.setAudioEncoder(profile.audioCodec);
        }

        mMediaRecorder.setVideoEncoder(profile.videoCodec);

        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
    }
}
