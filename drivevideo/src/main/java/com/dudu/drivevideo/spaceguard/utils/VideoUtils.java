package com.dudu.drivevideo.spaceguard.utils;

import com.dudu.commonlib.utils.File.FileUtil;
import com.dudu.drivevideo.config.FrontVideoConfigParam;
import com.dudu.drivevideo.config.RearVideoConfigParam;
import com.dudu.persistence.factory.RealmCallFactory;
import com.dudu.persistence.realm.RealmCallBack;
import com.dudu.persistence.realmmodel.video.VideoEntity;
import com.dudu.persistence.realmmodel.video.VideoEntityRealm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by dengjun on 2016/4/14.
 * Description :
 */
public class VideoUtils {

    public static Logger log = LoggerFactory.getLogger("video.VideoStorage");


    public static void deleteOldestVideo(boolean cameraFlag) {
        RealmCallFactory.removeOneVideo(realmQuery -> {
            Number oldestNumber = realmQuery.min("timeStamp");
            long timeOldest = 0;
            if (oldestNumber != null) {
                timeOldest = realmQuery.min("timeStamp").longValue();
            }
            return (VideoEntityRealm) realmQuery.equalTo("timeStamp", timeOldest).equalTo("cameraFlag", cameraFlag).findFirst();
        }, new RealmCallBack<VideoEntityRealm, Exception>() {
            @Override
            public void onRealm(VideoEntityRealm result) {
                if (result != null) {
                    File fileToDelete = new File(result.getAbsolutePath());
                    if (fileToDelete.exists()) {
                        fileToDelete.delete();
                    }
                    log.debug("删除视频文件：{}， 时间：{}，时间戳：{}", result.getFileName(), result.getCreateTime(), result.getTimeStamp());
                }
            }

            @Override
            public void onError(Exception error) {

            }
        });
    }

    private static boolean isDeleteSuccess = false;
    public static boolean deleteOldestVideo(){
        isDeleteSuccess = false;
        RealmCallFactory.tran(realm -> {
            boolean isDelete = false;//是否删除过


            int frontVideoNum = VideoUtils.getVideoNum(realm, true);
            int rearVideoNum = VideoUtils.getVideoNum(realm, false);
            float realmVideoTotalVideoSize = VideoUtils.getRealmTotalVideoSize(realm);
            log.info("前置数据库录像数目：{}，后置数据库录像数目：{}, 数据库记录视频总大小：{}", frontVideoNum, rearVideoNum, realmVideoTotalVideoSize);

            int frontVideoDirVideoNum = VideoUtils.getFrontVideoDirVideoNum();
            int rearVideoDirVideoNum = VideoUtils.getRearVideoDirVideoNum();
            float videoDirTotalVideoSize = VideoUtils.getVideoDirAllVideoSize();
            log.info("前置目录录像数目：{}，后置目录录像数目：{}, 目录中视频总大小：{}", frontVideoDirVideoNum, rearVideoDirVideoNum, videoDirTotalVideoSize);

            if (frontVideoNum > rearVideoNum){
                isDeleteSuccess = deleteVideo(realm, false, true);
            }else if (frontVideoNum == rearVideoNum){
                boolean isSuccessed1 = deleteVideo(realm, false, true);
                boolean isSuccessed2 = deleteVideo(realm, false, false);

                isDeleteSuccess = isSuccessed1|isSuccessed2;
            }else if (frontVideoNum < rearVideoNum){
                isDeleteSuccess = deleteVideo(realm, false, false);
            }
            log.debug("\n");


          /*  if (!isDelete) {
                //锁定的视频,在容量特别紧张的情况下,也要无情的删除
                deleteVideo(realm, true, true);
                deleteVideo(realm, true, false);
            }*/
        });

        return isDeleteSuccess;
    }

    public static boolean deleteVideo(Realm realm, boolean isLock, boolean isFace) {
        Number min = realm.where(VideoEntityRealm.class).equalTo("lockFlag", isLock).equalTo("cameraFlag", isFace).min("timeStamp");//
        if (min != null) {
            VideoEntityRealm first = realm.where(VideoEntityRealm.class).equalTo("cameraFlag", isFace).equalTo("timeStamp", min.longValue()).findFirst();
            if (first != null) {
                FileUtil.deleteFile(first.getAbsolutePath());
                FileUtil.deleteFile(first.getThumbnailAbsolutePath());
                log.debug("删除{}视频文件：{}，缩略图:{}，时间：{}，时间戳：{}，大小:{}MB，锁定：{}",
                        isFace ? "前置":"后置",
                        first.getAbsolutePath(), first.getThumbnailAbsolutePath(),
                        first.getCreateTime(), first.getTimeStamp(), first.getFileSize(),
                        first.isLockFlag());
                first.removeFromRealm();
                return true;
            }
        }
        return false;
    }

    public static int getVideoNum(Realm realm, boolean cameraFlag){
        RealmResults<VideoEntityRealm> videoEntityRealms = realm.where(VideoEntityRealm.class).equalTo("cameraFlag", cameraFlag).findAll();
        return videoEntityRealms.size();
    }


    public static float getRealmTotalVideoSize(Realm realm){
        RealmResults<VideoEntityRealm> videoEntityRealms = realm.where(VideoEntityRealm.class).findAll();
        return getAllVideoTotalSizeMb(videoEntityRealms);
    }

    public static float getAllVideoTotalSizeMb(RealmResults<VideoEntityRealm> videoEntityArrayList){
        float allVideoTotalSizeMb = 0;
        for (VideoEntityRealm videoEntityRealm : videoEntityArrayList) {
            allVideoTotalSizeMb += Float.valueOf(videoEntityRealm.getFileSize());
            allVideoTotalSizeMb += Float.valueOf(videoEntityRealm.getThumbnailFileSize());
        }
        return allVideoTotalSizeMb;
    }

    public static int getFrontVideoDirVideoNum(){
        return getVideoSaveDirVideoNum(FrontVideoConfigParam.FRONT_VIDEO_STORAGE_PATH);
    }

    public static int getRearVideoDirVideoNum(){
        return getVideoSaveDirVideoNum(RearVideoConfigParam.REAR_VIDEO_STORAGE_PATH);
    }

    public static float getVideoDirAllVideoSize(){
        return  getFrontVideoSaveDirAllVideoSize() + getRearVideoSaveDirAllVideoSize();
    }

    public static float getFrontVideoSaveDirAllVideoSize(){
        return  getVideoSaveDirAllVideoSize(FrontVideoConfigParam.FRONT_VIDEO_STORAGE_PATH)
                + getVideoSaveDirAllVideoSize(FrontVideoConfigParam.FRONT_VIDEO_THUMBNAIL_STORAGE_PATH);
    }

    public static float getRearVideoSaveDirAllVideoSize(){
        return  getVideoSaveDirAllVideoSize(RearVideoConfigParam.REAR_VIDEO_STORAGE_PATH)
                + getVideoSaveDirAllVideoSize(RearVideoConfigParam.REAR_VIDEO_THUMBNAIL_STORAGE_PATH);
    }

    private static int getVideoSaveDirVideoNum(String saveDir){
        File videoDir = new File(saveDir);
        int videoNum = 0;
        if (videoDir.exists() && videoDir.isDirectory()){
            videoNum = videoDir.list().length;
        }
        return videoNum;
    }

    private static float getVideoSaveDirAllVideoSize(String saveDir){
        File videoDir = new File(saveDir);
        float allVideoSize = 0;
        if (videoDir.exists()&& videoDir.isDirectory()){
            long allVideoSizeLong = 0;
            for (File videoFile: videoDir.listFiles()){
                allVideoSizeLong += videoFile.length();
            }
            allVideoSize = allVideoSizeLong /1024 / 1024;
        }
        return allVideoSize;
    }


    public static void  testDeleteVideoNoRealmRecord(){
        new Thread(()->{
            deleteVideoNoRealmRecord();
        }).start();
    }

    /**
     * 删除数据库没记录的视频文件
     * 包括缩列图
     */
    public static void deleteVideoNoRealmRecord(){
        RealmCallFactory.findAllVideo(true, new RealmCallBack<ArrayList<VideoEntity>, Exception>() {
            @Override
            public void onRealm(ArrayList<VideoEntity> result) {
                log.info("deleteVideoNoRealmRecord前置数据库保存视频数目：{}", result.size());
                deleteFontVideoNoRealmRecord(result);
            }

            @Override
            public void onError(Exception error) {
                log.error("异常：", error);
            }
        });

        RealmCallFactory.findAllVideo(false, new RealmCallBack<ArrayList<VideoEntity>, Exception>() {
            @Override
            public void onRealm(ArrayList<VideoEntity> result) {
                log.info("deleteVideoNoRealmRecord后置数据库保存视频数目：{}", result.size());
                deleteRearVideoNoRealmRecord(result);
            }

            @Override
            public void onError(Exception error) {
                log.error("异常：", error);
            }
        });
    }


    private static void deleteFontVideoNoRealmRecord(ArrayList<VideoEntity> videoEntityArrayList){
        deleteFileHaveNoRecordAtList(FrontVideoConfigParam.FRONT_VIDEO_STORAGE_PATH, false, videoEntityArrayList);
        deleteFileHaveNoRecordAtList(FrontVideoConfigParam.FRONT_VIDEO_THUMBNAIL_STORAGE_PATH,true,  videoEntityArrayList);
    }

    private static void deleteRearVideoNoRealmRecord(ArrayList<VideoEntity> videoEntityArrayList){
        deleteFileHaveNoRecordAtList(RearVideoConfigParam.REAR_VIDEO_STORAGE_PATH, false, videoEntityArrayList);
        deleteFileHaveNoRecordAtList(RearVideoConfigParam.REAR_VIDEO_THUMBNAIL_STORAGE_PATH, true, videoEntityArrayList);
    }

    private static void deleteFileHaveNoRecordAtList(String filePath, boolean isThumbnail,ArrayList<VideoEntity> videoEntityArrayList){
        File videoDir = new File(filePath);
        if (videoDir.exists()){
            String[] fileNameArray = videoDir.list();
            for(String fileName: fileNameArray){
                boolean haveFlag = false;
                File file = new File(filePath+"/"+fileName);
                if (file.isFile() && System.currentTimeMillis() - file.lastModified() > 2*60*1000){//3*60*60*1000){//只对3小时之前的文件进行处理，防止删除正在录制的视频
                    for (VideoEntity videoEntity : videoEntityArrayList){
                        if (isThumbnail){
                            if (fileName.equals(new File(videoEntity.getThumbnailAbsolutePath()).getName())){
                                haveFlag = true;
                            }
                        }else {
                            if (fileName.equals(videoEntity.getFileName())){
                                haveFlag = true;
                            }
                        }
                    }
                    if (haveFlag == false){
                        log.debug("删除数据库中没有记录的文件：{}", filePath+"/"+fileName);
                        FileUtil.deleteFile(filePath+"/"+fileName);
                    }
                }
                if (file.isDirectory()){
                    log.debug("删除路径：{}", file.getAbsolutePath());
                    FileUtil.DeleteFolder(file.getAbsolutePath());
                }
            }
        }else {
            log.info("路径不存在，不清理");
        }
    }


    /**
     * 删除数据库中有记录，但是实际文件不存在的数据库记录
     *删除数据库有记录，大小为零视频文件的记录
     */
    public static void deleteFileHaveRecordButNotExistAndZeroSizeVideo(){
        RealmCallFactory.tran(realm -> {
            RealmResults<VideoEntityRealm> videoEntityRealms = realm.where(VideoEntityRealm.class).findAll();
            if (FileUtil.isTFlashCardExists()){
                deleteFileHaveRecordButNotExistAndZeroSizeVideo(videoEntityRealms);
            }else {
                log.info("deleteFileHaveRecordButNotExistAndZeroSizeVideo sd卡不存在");
            }
        });
    }


    /**
     * 删除数据库有记录，实际不存在的视频文件的记录
     * 删除数据库有记录，大小为零视频文件的记录
     * @param videoEntityRealms
     */
    private static void deleteFileHaveRecordButNotExistAndZeroSizeVideo(RealmResults<VideoEntityRealm> videoEntityRealms){
        Iterator<VideoEntityRealm> iterator = videoEntityRealms.iterator();
        List<VideoEntityRealm> videoEntityRealmList = new ArrayList<VideoEntityRealm>();
        List<VideoEntityRealm> zeroSzieVideoEntityRealmList = new ArrayList<VideoEntityRealm>();
        while (iterator.hasNext()){
            VideoEntityRealm videoEntityRealm = iterator.next();
            File videoFile = new File(videoEntityRealm.getAbsolutePath());
            //删除数据库有记录，实际不存在的视频文件的记录只针对48小时之前的视频文件，防止videoFile.exists()判断有误导致误删文件
            if (!videoFile.exists() && (System.currentTimeMillis() - videoEntityRealm.getTimeStamp() > 48*60*60*1000)){
                videoEntityRealmList.add(videoEntityRealm);
            }else {
                if (videoFile.exists() && videoFile.length() == 0){
                    zeroSzieVideoEntityRealmList.add(videoEntityRealm);
                }
            }
        }
        for (VideoEntityRealm videoEntityRealm: videoEntityRealmList){
            if (!FileUtil.isTFlashCardExists()){
                log.info("sd不存在，停止删除数据库有记录，实际不存在的视频文件的记录");
                return;
            }

            log.info("删除数据库有记录，实际不存在的视频文件的记录：{}, 时间戳：{}", videoEntityRealm.getAbsolutePath(), videoEntityRealm.getCreateTime());
            videoEntityRealm.removeFromRealm();
        }

        for (VideoEntityRealm videoEntityRealm: zeroSzieVideoEntityRealmList){
            log.info("删除数据库有记录，大小为零视频文件的记录：{},文件大小：{}kb", videoEntityRealm.getAbsolutePath(), videoEntityRealm.getFileSize());
            FileUtil.deleteFile(videoEntityRealm.getAbsolutePath());
            FileUtil.deleteFile(videoEntityRealm.getThumbnailAbsolutePath());
            videoEntityRealm.removeFromRealm();
        }
    }
}
