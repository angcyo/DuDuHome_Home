package com.dudu.drivevideo.utils;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.dudu.commonlib.utils.File.FileUtil;
import com.dudu.commonlib.utils.image.ImageUtils;
import com.dudu.commonlib.utils.shell.ShellExe;
import com.dudu.drivevideo.config.RearVideoConfigParam;
import com.dudu.persistence.factory.RealmCallFactory;
import com.dudu.persistence.realm.RealmCallBack;
import com.dudu.persistence.realmmodel.picture.PictureEntity;
import com.dudu.persistence.realmmodel.picture.PictureEntityRealm;
import com.dudu.persistence.realmmodel.video.VideoEntity;
import com.dudu.persistence.realmmodel.video.VideoEntityRealm;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by dengjun on 2016/4/25.
 * Description :
 */
public class RearVideoSaveTools {
    private static Logger log = LoggerFactory.getLogger("video.reardrivevideo");

    public static void saveCurVideoInfo(String fileAbPath, String curVideoThumbnailPath) {
        String filePath = fileAbPath;
        String videoThumbnailPath = curVideoThumbnailPath;
        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists())
            return;

        if (deleteSmallVideoFile(filePath)){
            return;
        }
        //交给rx调度，防止资源紧张情况开线程导致内存溢出
        Observable
                .timer(0,TimeUnit.SECONDS, Schedulers.io())
                .subscribe(l->{
                    saveCurVideoInfoAction(filePath, videoThumbnailPath);
                },throwable -> {
                    log.error("异常", throwable);
                });
    }

    private static void saveCurVideoInfoAction(String filePath, String curVideoThumbnailPath){
        File file = new File(filePath);
        try {
            if (file.exists()) {
                File mp4File = new File(filePath);
                if (mp4File.exists()){
//                    String curVideoThumbnailPath = ImageUtils.generateThumbnailFromVideo(filePath, generateCurVideoThumbnailPath());//由so直接生成缩列图

                    RealmCallFactory.saveVideoInfo(false, filePath, curVideoThumbnailPath,new RealmCallBack<VideoEntityRealm, Exception>() {
                        @Override
                        public void onRealm(VideoEntityRealm result) {
                            if (result != null) {
                                log.debug("加入视频文件：{}", new Gson().toJson(new VideoEntity(result)));
                            }
                        }
                        @Override
                        public void onError(Exception error) {
                            log.error("异常", error);
                        }
                    });
                }

            }
        } catch (Exception e) {
            log.error("异常", e);
        }
    }

    private static boolean deleteSmallVideoFile(String videoAbsolutePath){
        File file = new File(videoAbsolutePath);
        if (!file.exists())
            return true;
        float fileLenth = Float.valueOf(FileUtil.getFileSizeMb(videoAbsolutePath));
        log.debug("当前文件大小：{} MB", fileLenth);
        if (fileLenth < 1.0){
            log.info("录像文件大小小于1M，直接删除，不保存数据和生成缩略图");

            if (file.exists()){
                log.info("删除文件返回结果：{}", file.delete());
            }
            return true;
        }else {
            return false;
        }
    }

    public static String generateCurVideoThumbnailPath() {
        return com.dudu.drivevideo.utils.FileUtil.getTFlashCardDirFile("/dudu", RearVideoConfigParam.VIDEO_THUMBNAIL_STORAGE_PATH).getAbsolutePath()
                + File.separator + TimeUtils.format(TimeUtils.format6) + ".png";
    }


    public static void deleteCurVideo(String fileAbPath){
        String filePath = fileAbPath;
        Observable
                .timer(0, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe(l->{
                    File file = new File(filePath);
                    if (file.exists()) {
                        log.info("删除当前录像文件,返回结果：{}", file.delete());
                    }
                }, throwable -> {
                    log.error("异常", throwable);
                });
    }






    public static String generateCurVideoName264(){
        return com.dudu.drivevideo.utils.FileUtil.getTFlashCardDirFile("/dudu", RearVideoConfigParam.VIDEO_STORAGE_PATH).getAbsolutePath()
                + File.separator+ TimeUtils.format(TimeUtils.format6)+".h264";
    }

    public static String generateCurVideoNameMp4(){
        return com.dudu.drivevideo.utils.FileUtil.getTFlashCardDirFile("/dudu", RearVideoConfigParam.VIDEO_STORAGE_PATH).getAbsolutePath()
                + File.separator+ TimeUtils.format(TimeUtils.format6)+".mp4";
    }

    public static String generateCurVideoNameMp4(String fileName){
        return com.dudu.drivevideo.utils.FileUtil.getTFlashCardDirFile("/dudu", RearVideoConfigParam.VIDEO_STORAGE_PATH).getAbsolutePath()
                + File.separator+ fileName+".mp4";
    }

    private static String getVideoNameWithOutSuffix(String videoPath){
        File videoFile = new File(videoPath);
        String fileName = videoFile.getName();
        return fileName.split(".h264")[0];
    }


    private static String video264ToMp4(String filePath264, String filePathMp4){
        String cmd = "ffmpeg -f h264 -i " +  filePath264+ " -vcodec copy -r 30 "+ filePathMp4;
        return ShellExe.execShellCmd(cmd);
    }

    private static String generateRearCameraPicturePath() {
        return com.dudu.drivevideo.utils.FileUtil.getTFlashCardDirFile("/dudu", RearVideoConfigParam.VIDEO_PICTURE_STORAGE_PATH).getAbsolutePath()
                + File.separator + TimeUtils.format(TimeUtils.format5) + ".jpg";
    }

    public static void savePictureAction(Bitmap bitmap){
        new Thread(()->{
            String pictureAbApth = generateRearCameraPicturePath();
            log.info("拍摄照片：{}", pictureAbApth);
            ImageUtils.saveBitmapToJpg(bitmap, pictureAbApth);
            if (new File(pictureAbApth).exists()){
                RealmCallFactory.savePictureInfo(false, pictureAbApth, new RealmCallBack<PictureEntityRealm, Exception>() {
                    @Override
                    public void onRealm(PictureEntityRealm result) {
                        log.debug("保存的照片信息：{}", new Gson().toJson(new PictureEntity(result)));
                    }

                    @Override
                    public void onError(Exception error) {

                    }
                });
            }
        }).start();
    }
}
