package com.dudu.drivevideo.rearcamera.preview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import com.blur.SoundPlayManager;
import com.dudu.commonlib.utils.File.FileUtil;
import com.dudu.commonlib.utils.thread.ThreadPoolManager;
import com.dudu.commonlib.utils.thread.ThreadUtils;
import com.dudu.drivevideo.config.RearVideoConfigParam;
import com.dudu.drivevideo.exception.CameraErrorCode;
import com.dudu.drivevideo.exception.DirveVideoException;
import com.dudu.drivevideo.rearcamera.camera.RearCameraListener;
import com.dudu.drivevideo.rearcamera.camera.RearCameraListenerMessage;
import com.dudu.drivevideo.rearcamera.utils.PictureUtil;
import com.hclydao.webcam.Ffmpeg;
import com.hclydao.webcam.V4L2VALUE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by dengjun on 2016/5/17.
 * Description :
 */
public class RearCamera {
    Rect src = new Rect(0, 200, 1280, 520);
    Rect dst = new Rect(0, 0, 1920, 480);
    private RearVideoConfigParam rearVideoConfigParam;
    private boolean cameraOpenFlag = false;
    private V4L2VALUE v4l2value;
    private int frameIndex;

    private byte[] frameDataBuffer;
    private int numBuffer = 4;
    private boolean preViewFlag = false;
    private RearCameraListener rearCameraListener;
    private boolean previewEnable = false;
    private boolean previewFinishFlag = true;
    private Canvas canvas = null;
    private Paint mPaint = new Paint();
    private ByteBuffer Imagbuf;
    private Logger log = LoggerFactory.getLogger("video.reardrivevideo");
    private Bitmap bitmap;
    private Matrix matrix;

    private AtomicInteger mAtomicInteger = new AtomicInteger(0);
    private long takeTimeStamp = 0;

    public RearCamera(RearVideoConfigParam rearVideoConfigParam) {

        this.rearVideoConfigParam = rearVideoConfigParam;

        initMatix();
    }

    private void initBuffer(){
        bitmap = Bitmap.createBitmap(1280, 520, Bitmap.Config.RGB_565);
        frameDataBuffer = new byte[1280 * 520 * 2];
        Imagbuf = ByteBuffer.wrap(frameDataBuffer);
    }

    private void releaseBuffer(){
        if (Imagbuf != null){
            Imagbuf.clear();
            Imagbuf = null;
        }
       frameDataBuffer = null;
        if (bitmap != null){
            bitmap.recycle();
            bitmap = null;
        }
    }

    /**
     * 拍照
     */
    public synchronized void takePhoto() {
        if (FileUtil.isTFlashCardExists() && (System.currentTimeMillis() - takeTimeStamp > 800)) {
            mAtomicInteger.getAndIncrement();
            takeTimeStamp = System.currentTimeMillis();
        }
    }

    private void takePhotoInternal(Bitmap bitmap) {
        //拍照的实现
        if (mAtomicInteger.get() > 0) {
            final Bitmap bitmapSave = Bitmap.createBitmap(bitmap);
            mAtomicInteger.getAndDecrement();
            ThreadPoolManager.getInstance(this.getClass().getName()).schedule(()->{
                SoundPlayManager.play();//拍照声效
                PictureUtil.savePictureAction(bitmapSave);
                bitmapSave.recycle();
            },0, TimeUnit.SECONDS);
        }
    }

    public void init() throws DirveVideoException {
        if (cameraOpenFlag) {
            log.debug("摄像头已经打开");
            return;
        }
        if (!detectCamera()) {
            log.error("摄像头设备文件不存在");
            throw new DirveVideoException(CameraErrorCode.OPEN_CAMERA_ERROR);
        }
        log.info("初始化摄像头{}", "/dev/" + rearVideoConfigParam.getVideoDevice());
        int ret = Ffmpeg.open("/dev/" + rearVideoConfigParam.getVideoDevice());
        if (ret < 0) {
            log.error("Ffmpeg.open错误");
            throw new DirveVideoException(CameraErrorCode.OPEN_CAMERA_ERROR);
        }
        ret = Ffmpeg.init(rearVideoConfigParam.getWidth(), rearVideoConfigParam.getHeight(), numBuffer);
        if (ret < 0) {
            log.error("Ffmpeg.init错误");
            throw new DirveVideoException(CameraErrorCode.OPEN_CAMERA_ERROR);
        }
        ret = Ffmpeg.streamon();
        if (ret < 0) {
            log.error("Ffmpeg.streamon错误");
            throw new DirveVideoException(CameraErrorCode.OPEN_CAMERA_ERROR);
        }
        log.info("初始化摄像头成功-------------------------");
        cameraOpenFlag = true;
    }

    public void release() {
        log.info("释放摄像头");
        while (!previewFinishFlag) {

        }
        ;
        Ffmpeg.release();
        cameraOpenFlag = false;
    }

    public void startPreview() throws DirveVideoException {
        if (preViewFlag == true) {
            log.debug("startPreviewAciton  0");
            return;
        }
        log.debug("startPreviewAciton  1");
        if (cameraOpenFlag == false) {
            log.info("摄像头未打开");
            throw new DirveVideoException(CameraErrorCode.START_PREVIEW_ERROR);
        }
        preViewFlag = true;
    }

    public void startPreview(SurfaceHolder surfaceHolder) throws DirveVideoException {
        if (preViewFlag == true) {
            log.debug("startPreviewAciton  0");
            return;
        }
        log.debug("startPreviewAciton  1");
        if (cameraOpenFlag == false) {
            log.info("摄像头未打开");
            throw new DirveVideoException(CameraErrorCode.START_PREVIEW_ERROR);
        }

        if (surfaceHolder != null && surfaceHolder.getSurface().isValid()) {
            preViewFlag = true;
            doPreview(surfaceHolder);
        } else {
            log.error("surfaceHolder 为null，无法开启预览");
        }
    }

    public void stopPreview() {
        log.info("停止预览-----------");
        /*** in case of forever loop ***/
        if(!preViewFlag)
            return;

        preViewFlag = false;
        while(!previewFinishFlag){
            ThreadUtils.threadSleep(30);
        }
        log.info("停止预览 success-----------");
    }


    private void proDqBugError() {
        preViewFlag = false;
        previewFinishFlag = true;
        log.error("Ffmpeg.dqbuf error 错误");
        if (rearCameraListener != null) {
            rearCameraListener.onError(RearCameraListenerMessage.PREVIEW_ERROR);//重新开启
        }
    }


    private Bitmap decodeBitmapFromByteArray(byte[] bitmapDataBuffer) {
        return BitmapFactory.decodeByteArray(bitmapDataBuffer, 0, v4l2value.byteused);
    }

    private void doPreview(SurfaceHolder holder) {
        Thread previewThread = new Thread(() -> {
            try {
                log.info("开始预览--------------------------------------");
                initBuffer();
                while (preViewFlag) {
                    previewFinishFlag = false;
                    frameIndex = Ffmpeg.dqbufdecode(frameDataBuffer);

                    if (frameIndex < 0) {
                        proDqBugError();
                        break;
                    }

                    if (holder.getSurface().isValid()) {
                        try {

                            canvas = holder.lockCanvas();
                            // Bitmap bitmap = decodeBitmapFromByteArray(frameDataBuffer);
                            if (canvas != null) {

                                bitmap.copyPixelsFromBuffer(Imagbuf);
                                takePhotoInternal(bitmap);
                                //mImag.setImageBitmap(bitmap);
                                canvas.drawBitmap(bitmap, matrix, mPaint);
                                //canvas.drawBitmap(bitmap, src, dst, mPaint);
                                Imagbuf.clear();
                                //bitmap.recycle();
                                //bitmap = null;
                            }
                        } catch (Exception e) {
                            log.error("error", e);
                        } finally {
                            if (canvas != null) {
                                holder.unlockCanvasAndPost(canvas);
                            }
                        }
                    } else {
                        log.info("Surface无效------------------------------------------");
                        Imagbuf.clear();
                        preViewFlag = false;
                    }

                   if ( Ffmpeg.qbuf(frameIndex) < 0) {
                        proDqBugError();
                        break;
                    }
                }
                if (canvas != null) {
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                }
                log.info("预览结束------------------------------------------");
                previewFinishFlag = true;
                releaseBuffer();
                if (rearCameraListener != null) {
                    rearCameraListener.onInfo(RearCameraListenerMessage.PREVIEW_FINISH);
                }
            } catch (Exception e) {
                log.error("异常", e);
                preViewFlag = false;
                previewFinishFlag = true;
                releaseBuffer();
                if (rearCameraListener != null) {
                    rearCameraListener.onError(RearCameraListenerMessage.PREVIEW_ERROR);
                }
            }
        });
        previewThread.setName("Rear camera preview");
        previewThread.setPriority(Thread.MAX_PRIORITY);
        previewThread.start();
    }



    private void initMatix() {
        matrix = new Matrix();

        float scaleFactor = (float) 1920 / 1280;
        float tanslateY = 520 - 320;
        matrix.setTranslate(0, -tanslateY);
        matrix.postScale(scaleFactor, scaleFactor, 0, 0);
    }


    public void setRearCameraListener(RearCameraListener rearCameraListener) {
        this.rearCameraListener = rearCameraListener;
    }



    public boolean detectCamera() {
        return FileUtil.detectFileExist("/dev/" + rearVideoConfigParam.getVideoDevice());
    }

    public boolean isPreviewEnable() {
        return previewEnable;
    }

    public void setPreviewEnable(boolean previewEnable) {
        this.previewEnable = previewEnable;
    }



    public boolean isPreviewIng() {
        return preViewFlag;
    }

    public void setCameraOpenFlag(boolean cameraOpenFlag) {
        this.cameraOpenFlag = cameraOpenFlag;
    }
}
