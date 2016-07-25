package com.dudu.drivevideo.spaceguard;

import com.dudu.commonlib.utils.File.FileUtil;
import com.dudu.drivevideo.spaceguard.utils.VideoUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;


/**
 * Created by dengjun on 2016/4/14.
 * Description :
 */
public class VideoStorageResource {
    private static final double availbleScale = 0.7;
    private float tfCardSpace;
    private double freeSpace;

    private Logger log = LoggerFactory.getLogger("video.VideoStorage");

    public static String format(double mbSize) {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(mbSize);
    }


    protected void initResource() {
        log.debug("初始化VideoStorageResource");
        tfCardSpace = FileUtil.getTFlashCardSpaceMbFloat();
        freeSpace = tfCardSpace * availbleScale;
        log.debug("磁盘空间：{}MB，录像存储最大可用空间 : {}MB", tfCardSpace, freeSpace);
    }


    protected void releaseResource() {

    }

    public void guadSpace() {
        int failLimitNum = 5;

        if (!FileUtil.isTFlashCardExists()) {
            log.debug("T卡不存在,停止整理.");
            return;
        }

        FileUtil.clearLostDirFolder();

        VideoUtils.deleteVideoNoRealmRecord();
        VideoUtils.deleteFileHaveRecordButNotExistAndZeroSizeVideo();

//        for (int i = 0; FileUtil.isTFlashCardExists() && needClean() && i < 50; i++)
        while(FileUtil.isTFlashCardExists() && needCleanByDynamicRange() && failLimitNum > 0)
        {
            boolean result = VideoUtils.deleteOldestVideo();
            if(!result)
            {
                failLimitNum--;
            }
        }

//        FileUtil.clearLostDirFolder();
//
//        VideoUtils.deleteVideoNoRealmRecord();
//        VideoUtils.deleteFileHaveRecordButNotExistAndZeroSizeVideo();

//        VideoUtils.delete4DayAgoVideoNoRealmRecord();
    }

    public boolean needClean() {
        double tFlashCardFreeSpace = FileUtil.getTFlashCardFreeSpaceMbFloat();//TF卡剩余空间
        double tfCardSpace = FileUtil.getTFlashCardSpaceMbFloat();//TF卡总空间
        log.debug("{} TF空间：{}MB，剩余空间 : {}MB, 必须剩余:{}MB",
                Thread.currentThread().getId(),
                tfCardSpace,
                format(tFlashCardFreeSpace),
                format(tfCardSpace * (1 - availbleScale)));
        return (tfCardSpace * (1 - availbleScale)) > tFlashCardFreeSpace;
    }

    private boolean mSdCardFullFlag = false;
    public boolean needCleanByDynamicRange() {
        double tFlashCardFreeSpace = FileUtil.getTFlashCardFreeSpaceMbFloat();//TF卡剩余空间
        double tfCardSpace = FileUtil.getTFlashCardSpaceMbFloat();//TF卡总空间

        log.debug("EXT -> {} TF空间：{}MB，剩余空间 : {}MB, 必须剩余:{}MB",
                Thread.currentThread().getId(),
                tfCardSpace,
                format(tFlashCardFreeSpace),
                format(tfCardSpace * (1 - availbleScale)));

        if(tFlashCardFreeSpace < tfCardSpace*0.2)
        {
            mSdCardFullFlag = true;
            return true;
        }
        else if(mSdCardFullFlag)
        {
            if(tFlashCardFreeSpace < tfCardSpace*0.4)
            {
                return true;
            }
            else
            {
                mSdCardFullFlag = false;
                return false;
            }
        }

        return false;
    }
}
