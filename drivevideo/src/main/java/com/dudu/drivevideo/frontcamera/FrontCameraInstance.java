package com.dudu.drivevideo.frontcamera;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.view.SurfaceHolder;
import android.view.View;

import com.blur.SoundPlayManager;
import com.dudu.drivevideo.config.FrontVideoConfigParam;
import com.dudu.drivevideo.frontcamera.camera.FrontRecorderImpl;
import com.dudu.drivevideo.frontcamera.preview.BlurGLSurfaceView;
import com.dudu.drivevideo.frontcamera.preview.SurfaceTextureCallback;
import com.dudu.drivevideo.rearcamera.utils.PictureUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

//front camera instance
public class FrontCameraInstance implements Camera.PreviewCallback, Camera.PictureCallback {

    public static final int DEFAULT_PREVIEW_RATE = 30;
    private static final String ASSERT_MSG = "检测到CameraDevice 为 null! 请检查";
    private static FrontCameraInstance mThisInstance;
    private Camera mCameraDevice;
    private Camera.Parameters mParams;
    private boolean mIsPreviewing = false;
    private int mDefaultCameraID = -1;
    private int mPreviewWidth;
    private int mPreviewHeight;

    private int mPictureWidth = 1920;
    private int mPictureHeight = 480;

    private int mPreferPreviewWidth = 1920;
    private int mPreferPreviewHeight = 480;
    private BlurGLSurfaceView mPreviewView;
    private FrontRecorderImpl mFrontRecorderImpl;
    private FrontVideoConfigParam frontVideoConfigParam;

    private int mFacing = 0;
    private boolean mCameraConnected = false;
    private String mCameraLock = "duduCameraLock";
    private boolean mSurfaceCreated = false;
    private boolean iskilled = false;
    private Logger log = LoggerFactory.getLogger("video1.frontdrivevideo");

    //保证从大到小排列
    private Comparator<Camera.Size> comparatorBigger = new Comparator<Camera.Size>() {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            int w = rhs.width - lhs.width;
            if (w == 0)
                return rhs.height - lhs.height;
            return w;
        }
    };
    //保证从小到大排列
    private Comparator<Camera.Size> comparatorSmaller = new Comparator<Camera.Size>() {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            int w = lhs.width - rhs.width;
            if (w == 0)
                return lhs.height - rhs.height;
            return w;
        }
    };
    private long takeTimeStamp = 0;

    private FrontCameraInstance() {
    }

    public static synchronized FrontCameraInstance getInstance() {
        if (mThisInstance == null) {
            mThisInstance = new FrontCameraInstance();
        }
        return mThisInstance;
    }

    public void startPreview() {
        if (mIsPreviewing) {
            log.debug("Camera is previewing...");
            return;
        }
        log.debug("startPreview mCameraDevice = " + mCameraDevice);

        if (!mCameraConnected || mCameraDevice == null) {
            if (mPreviewView != null && mSurfaceCreated) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mCameraLock) {
                            releaseRecord(false);
                            tryOpenCamera(null);
                            startPreview(mPreviewView.getSurfaceTexture());
                            setKill(false);
                            startRecord();
                        }
                    }
                }).start();
            } else {
                log.debug("mPreviewView = null");
            }
        } else {
            log.debug("camera Connected!");
        }
        onResumePreview();
        startRecord(); //防止点火状态，camera被release之后，重新开启录制
    }

    public void setPreviewView(BlurGLSurfaceView v) {
        /*熄火之后，灭屏幕之后释放了camera，但是亮屏的时候，需要重新预览，此时传进来的v == null; */
        if (v == null) {
            return;
        }
        mPreviewView = v;

        mPreviewView.setSurfaceTextureCallback(new SurfaceTextureCallback() {
            @Override
            public void surfaceCreated(SurfaceTexture surfaceTexture) {
                log.debug("onSurfaceCreate");
                /*这里不需要另外开线程执行tryOpenCamera操作，因为是在Surface线程中
                回调过来的，与主线程不是同一个。*/
                synchronized (mCameraLock) {
                    releaseRecord(false);
                    tryOpenCamera(null);
                    startPreview(mPreviewView.getSurfaceTexture());
                    mSurfaceCreated = true;
                    setKill(false);
                    startRecord();
                }
            }

            @Override
            public void surfaceChanged(SurfaceTexture surfaceTexture, GL10 gl, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder, SurfaceTexture surfaceTexture) {

            }
        });
    }

    public void onPausePreview() {
        if (mPreviewView != null) {
            mPreviewView.setVisibility(View.INVISIBLE);
        }
    }

    public void onResumePreview() {
        if (mPreviewView != null) {
            mPreviewView.setVisibility(View.VISIBLE);
        }
    }

    public void switchBlur(boolean flag) {
        if (mPreviewView != null) {
            mPreviewView.setBlur(flag);
        }
    }

    public void startRecord() {
        if (mFrontRecorderImpl == null) {
            mFrontRecorderImpl = new FrontRecorderImpl();
        }
        mFrontRecorderImpl.startRecorder();
    }

    public void releaseRecord(boolean recorder) {
        log.debug("releaseRecord");
        if (mFrontRecorderImpl == null) {
            return;
        }

        mFrontRecorderImpl.releaseRecorder();
    }

    public void releaseCamera() {
        log.debug("releaseCamera");
        synchronized (mCameraLock) {
            releaseRecord(false);
            stopPreview();
            if (mCameraDevice != null) {
                mCameraDevice.setPreviewCallback(null);
                mCameraDevice.release();
                onPausePreview();
                mCameraConnected = false;
                mCameraDevice = null;
            }
        }
    }

    public int getFacing() {
        return mFacing;
    }

/*    public synchronized void releaseCamera() {
        if (mCameraDevice != null) {
            Log.i(LOG_TAG, "释放摄像头");
            mIsPreviewing = false;
            mCameraDevice.stopPreview();
            mCameraDevice.setPreviewCallback(null);
            mCameraDevice.release();
            mCameraDevice = null;
        }
    }*/

    public boolean tryOpenCamera(CameraOpenCallback callback) {
        return tryOpenCamera(callback, Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    public synchronized boolean tryOpenCamera(CameraOpenCallback callback, int facing) {
        log.debug("try open camera...");

        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                int numberOfCameras = Camera.getNumberOfCameras();

                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < numberOfCameras; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == facing) {
                        mDefaultCameraID = i;
                        mFacing = facing;
                    }
                }
            }
            stopPreview();
            if (mCameraDevice != null)
                mCameraDevice.release();

            if (mDefaultCameraID >= 0) {
                mCameraDevice = Camera.open(mDefaultCameraID);
            } else {
                mCameraDevice = Camera.open();
                mFacing = Camera.CameraInfo.CAMERA_FACING_BACK; //default: back facing
            }
            //mCameraDevice.setDisplayOrientation(270);
        } catch (Exception e) {
            log.debug("Open Camera Failed!");
            e.printStackTrace();
            mCameraDevice = null;
            return false;
        }

        if (mCameraDevice != null) {
            log.debug("Camera opened!");

            try {
                initCamera(DEFAULT_PREVIEW_RATE);
            } catch (Exception e) {
                mCameraDevice.release();
                mCameraDevice = null;
                return false;
            }

            if (callback != null) {
                callback.cameraReady();
            }
            mCameraConnected = true;
            return true;
        }

        return false;
    }

    public boolean isCameraOpened() {
        return mCameraDevice != null;
    }

    public synchronized void startPreview(SurfaceTexture texture, int degrees) {
        log.debug("Camera startPreview...");
        if (mIsPreviewing) {
            log.debug("Err: camera is previewing...");
//            stopPreview();
            return;
        }

        if (mCameraDevice != null) {
            try {
                mCameraDevice.setDisplayOrientation(degrees);
                mCameraDevice.setPreviewTexture(texture);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mCameraDevice.startPreview();
            mIsPreviewing = true;
        }
    }

    public synchronized void startPreview(SurfaceTexture texture) {
        log.debug("Camera startPreview...");
        if (mIsPreviewing) {
            log.debug("Err: camera is previewing...");
            return;
        }

        log.debug("texture = " + texture);
        if (mCameraDevice != null) {
            try {
                mCameraDevice.setDisplayOrientation(270);
                mCameraDevice.setPreviewTexture(texture);
                mCameraDevice.startPreview();
                mIsPreviewing = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void stopPreview() {
        log.debug("stopPreview mIsPreviewing = " + mIsPreviewing + " mCameraDevice =" + mCameraDevice);
        if (mIsPreviewing && mCameraDevice != null) {
            mIsPreviewing = false;
            mCameraDevice.stopPreview();
        }
    }

    public synchronized Camera.Parameters getParams() {
        if (mCameraDevice != null)
            return mCameraDevice.getParameters();
        assert mCameraDevice != null : ASSERT_MSG;
        return null;
    }

    public synchronized void setParams(Camera.Parameters param) {
        if (mCameraDevice != null) {
            mParams = param;
            mCameraDevice.setParameters(mParams);
        }
        assert mCameraDevice != null : ASSERT_MSG;
    }

    public Camera getCameraDevice() {
        return mCameraDevice;
    }

    public void initCamera(int previewRate) {
        if (mCameraDevice == null) {
            log.debug("initCamera: Camera is not opened!");
            return;
        }

        mParams = mCameraDevice.getParameters();
        List<Integer> supportedPictureFormats = mParams.getSupportedPictureFormats();

        for (int fmt : supportedPictureFormats) {
            log.debug(String.format("Picture Format: %x", fmt));
        }

//        mParams.setPictureFormat(PixelFormat.JPEG);

        List<Camera.Size> picSizes = mParams.getSupportedPictureSizes();
        Camera.Size picSz = null;

        Collections.sort(picSizes, comparatorBigger);

        for (Camera.Size sz : picSizes) {
//            Log.i(LOG_TAG, String.format("Supported picture size: %d x %d", sz.width, sz.height));
            if (picSz == null || (sz.width >= mPictureWidth && sz.height >= mPictureHeight)) {
                picSz = sz;
            }
        }

        List<Camera.Size> prevSizes = mParams.getSupportedPreviewSizes();
        Camera.Size prevSz = null;

        Collections.sort(prevSizes, comparatorBigger);

        for (Camera.Size sz : prevSizes) {
//            Log.i(LOG_TAG, String.format("Supported preview size: %d x %d", sz.width, sz.height));
            if (prevSz == null || (sz.width >= mPreferPreviewWidth && sz.height >= mPreferPreviewHeight)) {
                prevSz = sz;
            }
        }

        List<Integer> frameRates = mParams.getSupportedPreviewFrameRates();

        int fpsMax = 0;

        for (Integer n : frameRates) {
//            Log.i(LOG_TAG, "Supported frame rate: " + n);
            if (fpsMax < n) {
                fpsMax = n;
            }
        }

//        mParams.setPreviewSize(prevSz.width, prevSz.height);
//        mParams.setPictureSize(picSz.width, picSz.height);

        mParams.setPreviewSize(1920, 1080);
        mParams.setPictureSize(1920, 1080);
        List<String> focusModes = mParams.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        previewRate = fpsMax;
//        mParams.setPreviewFrameRate(previewRate); //设置相机预览帧率
//        mParams.setPreviewFpsRange(20, 60);

        try {
            mCameraDevice.setParameters(mParams);
        } catch (Exception e) {
            e.printStackTrace();
        }


//        mParams = mCameraDevice.getParameters();

        Camera.Size szPic = mParams.getPictureSize();
        Camera.Size szPrev = mParams.getPreviewSize();

        mPreviewWidth = szPrev.width;
        mPreviewHeight = szPrev.height;

        mPictureWidth = szPic.width;
        mPictureHeight = szPic.height;

//        Log.i(LOG_TAG, String.format("Camera Picture Size: %d x %d", szPic.width, szPic.height));
//        Log.i(LOG_TAG, String.format("Camera Preview Size: %d x %d", szPrev.width, szPrev.height));
    }

    public boolean isRecording() {
        if (mFrontRecorderImpl == null) {
            return false;
        }
        return mFrontRecorderImpl.isRecording();
    }

    public synchronized void setFocusMode(String focusMode) {

        if (mCameraDevice == null)
            return;

        mParams = mCameraDevice.getParameters();
        List<String> focusModes = mParams.getSupportedFocusModes();
        if (focusModes.contains(focusMode)) {
            mParams.setFocusMode(focusMode);
        }
    }

    public synchronized void setPictureSize(int width, int height, boolean isBigger) {

        if (mCameraDevice == null) {
            mPictureWidth = width;
            mPictureHeight = height;
            return;
        }

        mParams = mCameraDevice.getParameters();


        List<Camera.Size> picSizes = mParams.getSupportedPictureSizes();
        Camera.Size picSz = null;

        if (isBigger) {
            Collections.sort(picSizes, comparatorBigger);
            for (Camera.Size sz : picSizes) {
                if (picSz == null || (sz.width >= width && sz.height >= height)) {
                    picSz = sz;
                }
            }
        } else {
            Collections.sort(picSizes, comparatorSmaller);
            for (Camera.Size sz : picSizes) {
                if (picSz == null || (sz.width <= width && sz.height <= height)) {
                    picSz = sz;
                }
            }
        }

        mPictureWidth = picSz.width;
        mPictureHeight = picSz.height;

        try {
            mParams.setPictureSize(mPictureWidth, mPictureHeight);
            mParams.setPreviewSize(mPictureWidth, mPictureHeight);
            mCameraDevice.setParameters(mParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void focusAtPoint(float x, float y, final Camera.AutoFocusCallback callback) {
        focusAtPoint(x, y, 0.2f, callback);
    }

    public synchronized void focusAtPoint(float x, float y, float radius, final Camera.AutoFocusCallback callback) {
        if (mCameraDevice == null) {
            log.debug("Error: focus after release.");
            return;
        }

        mParams = mCameraDevice.getParameters();

        if (mParams.getMaxNumMeteringAreas() > 0) {

            int focusRadius = (int) (radius * 1000.0f);
            int left = (int) (x * 2000.0f - 1000.0f) - focusRadius;
            int top = (int) (y * 2000.0f - 1000.0f) - focusRadius;

            Rect focusArea = new Rect();
            focusArea.left = Math.max(left, -1000);
            focusArea.top = Math.max(top, -1000);
            focusArea.right = Math.min(left + focusRadius, 1000);
            focusArea.bottom = Math.min(top + focusRadius, 1000);
            List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
            meteringAreas.add(new Camera.Area(focusArea, 800));

            try {
                mCameraDevice.cancelAutoFocus();
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mParams.setFocusAreas(meteringAreas);
                mCameraDevice.setParameters(mParams);
                mCameraDevice.autoFocus(callback);
            } catch (Exception e) {
                log.debug("Error: focusAtPoint failed: " + e.toString());
            }
        } else {
            log.debug("The device does not support metering areas...");
            try {
                mCameraDevice.autoFocus(callback);
            } catch (Exception e) {
                log.debug("Error: focusAtPoint failed: " + e.toString());
            }
        }

    }

    public boolean isPreviewing() {
        return mIsPreviewing;
    }

    public int previewWidth() {
        return mPreviewWidth;
    }

    public int previewHeight() {
        return mPreviewHeight;
    }

    public int pictureWidth() {
        return mPictureWidth;
    }

    public int pictureHeight() {
        return mPictureHeight;
    }

    public void setPreferPreviewSize(int w, int h) {
        mPreferPreviewHeight = w;
        mPreferPreviewWidth = h;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // TODO Auto-generated method stub
        //get the prew frame here,the data of default is YUV420_SP
        //you should change YUV420_SP to YUV420_P
        log.debug("data = " + data.length);
    }

    //拍照的实现
    public synchronized void takePicture() {
        if (mCameraDevice != null && (System.currentTimeMillis() - takeTimeStamp > 800)) {
            try {
                SoundPlayManager.play();//拍照声效
                mCameraDevice.takePicture(null, null, this);
            } catch (Exception e) {
                log.error("异常", e);
            } finally {
                takeTimeStamp = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        try {
            PictureUtil.savePicture(data);
            camera.startPreview();
        } catch (Exception e) {
            log.error("异常", e);
        }
    }

    public interface CameraOpenCallback {
        void cameraReady();
    }

    public void setRecorderEnable(boolean enable) {
        if (mFrontRecorderImpl == null) {
            mFrontRecorderImpl = new FrontRecorderImpl();
        }
        mFrontRecorderImpl.setRecorderEnable(enable);
    }

    public void setKill(boolean enable) {
        if (mFrontRecorderImpl == null) {
            mFrontRecorderImpl = new FrontRecorderImpl();
        }
        mFrontRecorderImpl.setKilled(enable);
    }

}
