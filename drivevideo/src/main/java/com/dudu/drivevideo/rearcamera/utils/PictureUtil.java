package com.dudu.drivevideo.rearcamera.utils;

import android.graphics.Bitmap;

import com.dudu.commonlib.utils.image.ImageUtils;
import com.dudu.drivevideo.config.FrontVideoConfigParam;
import com.dudu.drivevideo.utils.FileUtil;
import com.dudu.drivevideo.utils.TimeUtils;
import com.dudu.persistence.factory.RealmCallFactory;
import com.dudu.persistence.realm.RealmCallBack;
import com.dudu.persistence.realmmodel.picture.PictureEntity;
import com.dudu.persistence.realmmodel.picture.PictureEntityRealm;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by robi on 2016-06-20 15:49.
 */
public class PictureUtil {
    static Logger log = LoggerFactory.getLogger("video1.frontdrivevideo");

    public static void savePicture(byte[] pictureData) {
        Observable
                .timer(0, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.computation())
                .subscribe(l -> {
                    if (com.dudu.commonlib.utils.File.FileUtil.isTFlashCardExists()) {
                        savePictureAction(pictureData);
                    } else {
                        log.info("sd卡不存在，无法保存照片");
                    }
                }, throwable -> {
                    log.error("异常", throwable);
                });
    }

    public static void savePictureAction(byte[] pictureData) {
        String pictureAbApth = generatePicturePath();
        log.info("拍摄照片：{}", pictureAbApth);
        ImageUtils.savePictureData(pictureData, new File(pictureAbApth));
        if (new File(pictureAbApth).exists()) {
            saveToRealm(pictureAbApth);
        }
    }

    public static String generatePicturePath() {
        return FileUtil.getTFlashCardDirFile("/dudu", FrontVideoConfigParam.VIDEO_PICTURE_STORAGE_PATH).getAbsolutePath()
                + File.separator + TimeUtils.format(TimeUtils.format5) + ".jpg";
    }

    public static void savePictureAction(Bitmap bitmap) {
        String pictureAbApth = generatePicturePath();
        log.info("拍摄照片：{}", pictureAbApth);
        try {
            ImageUtils.saveBitmap(bitmap, pictureAbApth);
            if (new File(pictureAbApth).exists()) {
                saveToRealm(pictureAbApth);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveToRealm(String filePath) {
        RealmCallFactory.savePictureInfo(true, filePath, new RealmCallBack<PictureEntityRealm, Exception>() {
            @Override
            public void onRealm(PictureEntityRealm result) {
                log.debug("保存的照片信息：{}", new Gson().toJson(new PictureEntity(result)));
            }

            @Override
            public void onError(Exception error) {
                log.error("异常", error);
            }
        });
    }
}
