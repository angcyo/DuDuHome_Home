package com.dudu.drivevideo.spaceguard;

import com.dudu.commonlib.utils.thread.ThreadPoolManager;
import com.dudu.drivevideo.frontcamera.FrontCameraManage;
import com.dudu.drivevideo.rearcamera.RearCameraManage;
import com.dudu.drivevideo.spaceguard.event.VideoSpaceEvent;
import com.dudu.drivevideo.spaceguard.event.VideoSpaceUpdateEvent;
import com.dudu.drivevideo.utils.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by dengjun on 2016/4/14.
 * Description :
 */
public class StorageSpaceService {
    private static final String TAG = "StorageSpaceService";
    private static StorageSpaceService instance = null;

    private VideoStorageResource videoStorageResource;
//    private Subscription videoStorageSubscription;

    private Logger log = LoggerFactory.getLogger("video.VideoStorage");

    private StorageSpaceService() {
        videoStorageResource = new VideoStorageResource();
        videoStorageResource.initResource();
    }

    public void init() {
        log.info("开启存储空间管理服务");
//        if (videoStorageSubscription != null) {
//            videoStorageSubscription.unsubscribe();
//        }
//        log.info("interval.io.create 守护录像存储空间");
//        videoStorageSubscription =
//                Observable.interval(10, 3 * 60, TimeUnit.SECONDS, Schedulers.io())
//                        .subscribe((l) -> {
//                            try {
//                                log.debug("interval.io 守护录像存储空间");
//                                guardVideoSpace();
//
//                                //发送录像存储更新消息通知相关页面刷新
//                                EventBus.getDefault().post(new VideoSpaceUpdateEvent(VideoSpaceUpdateEvent.RECORD_SPACE_UPDATE));
//
////                                voicePromptWhenNoEnoughSpace();//放到点火时判断
//                            } catch (Exception e) {
//                                log.error("interval.io 异常", e);
//                            }
//                        }, throwable -> log.error("interval.io startService", throwable));
        startService();
//        delayVoicePromptWhenNoEnoughSpace();
    }

    private void startService() {
        ThreadPoolManager.getInstance(TAG).cancelTaskThreads(TAG, true);

        ThreadPoolManager.getInstance(TAG).scheduleAtFixedRate(videoStorageThread, 0,  3*60, TimeUnit.SECONDS);
//        videoStorageThread.start();
    }

    private Thread videoStorageThread = new Thread() {
        @Override
        public void run() {
            try {
                log.debug("videoStorageThread 守护录像存储空间");

//                while(true)
//                {
                    guardVideoSpace();

                    //发送录像存储更新消息通知相关页面刷新
                    EventBus.getDefault().post(new VideoSpaceUpdateEvent(VideoSpaceUpdateEvent.RECORD_SPACE_UPDATE));

//                    Thread.sleep(1*60*1000);
//                }

//              voicePromptWhenNoEnoughSpace();//放到点火时判断
            } catch (Exception e) {
                log.error("videoStorageThread 异常", e);
            }
        }
    };

    public void release() {
        log.info("停止存储空间管理服务");
//        if (videoStorageSubscription != null){
//            videoStorageSubscription.unsubscribe();
//            videoStorageSubscription = null;
//        }
        ThreadPoolManager.getInstance(TAG).cancelTaskThreads(TAG, true);

        videoStorageResource.releaseResource();
    }


    private void guardVideoSpace() {
        videoStorageResource.guadSpace();
    }

    public void testStorageSpaceService() {
        new Thread(() -> {
            try {
                log.debug(" 守护录像存储空间");
                guardVideoSpace();
            } catch (Exception e) {
                log.error("异常", e);
            }
        }).start();
    }

    private void delayVoicePromptWhenNoEnoughSpace() {
        Observable
                .timer(30, TimeUnit.SECONDS, Schedulers.newThread())
                .subscribe(l -> {
                    voicePromptWhenNoEnoughSpace();
                }, throwable -> {
                    log.error("异常", throwable);
                });
    }

    private void voicePromptWhenNoEnoughSpace() {
        float sdFreeSpace = FileUtil.getSdFreeSpace();//剩余空间
        log.info("剩余磁盘空间百分比：{}", sdFreeSpace);
        if (sdFreeSpace < 0.1) {
            log.info(VideoSpaceEvent.driveRecordHaveNoStorageSpace);
            EventBus.getDefault().post(new VideoSpaceEvent(VideoSpaceEvent.driveRecordHaveNoStorageSpace));
            log.info("停止录像---");
            FrontCameraManage.getInstance().stopRecord();
            RearCameraManage.getInstance().stopRecord();
        }
    }


    public static StorageSpaceService getInstance() {
        if (instance == null) {
            synchronized (StorageSpaceService.class) {
                if (instance == null) {
                    instance = new StorageSpaceService();
                }
            }
        }
        return instance;
    }
}
