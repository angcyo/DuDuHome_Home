package com.dudu.service.storage;

import com.dudu.commonlib.utils.thread.ThreadPoolManager;
import com.dudu.persistence.factory.RealmCallFactory;
import com.dudu.persistence.realm.RealmCallBack;
import com.dudu.persistence.realmmodel.video.VideoEntityRealm;
import com.dudu.resource.resource.ResourceFactory;
import com.dudu.resource.resource.ResourceState;
import com.dudu.resource.storage.VideoStorageResource;
import com.dudu.service.service.AbstactService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by dengjun on 2016/4/14.
 * Description :
 */
public class StorageSpaceService extends AbstactService {
    private static final String TAG = "StorageSpaceService";
    private VideoStorageResource videoStorageResource;
//    private Subscription videoStorageSubscription;

    private Logger log = LoggerFactory.getLogger("video.VideoStorage");

    @Override
    protected void startService() {
        log.info("开启存储空间管理服务");
        videoStorageResource = ResourceFactory.getInstance().getResourceContainer().getInstance(VideoStorageResource.class);
        videoStorageResource.init();
        log.info("videoStorageThread 守护录像存储空间");
        ThreadPoolManager.getInstance(TAG).cancelTaskThreads(TAG, true);
        ThreadPoolManager.getInstance(TAG).scheduleAtFixedRate(videoStorageThread, 20, 30 * 60, TimeUnit.SECONDS);
//        videoStorageSubscription =
//                Observable.interval(10, 5 * 60, TimeUnit.SECONDS, Schedulers.io())
//                        .subscribe((l) -> {
//                            try {
//                                log.debug("interval.io 守护录像存储空间");
//                                guardVideoSpace();
//                            } catch (Exception e) {
//                                log.error("interval.io 异常", e);
//                            }
//                        }, throwable -> log.error("interval.io startService", throwable));
    }

    private Thread videoStorageThread = new Thread() {

        @Override
        public void run() {
            try {
                log.debug("videoStorageThread.run 守护录像存储空间");
                guardVideoSpace();
            } catch (Exception e) {
                log.error("videoStorageThread", e);
            }
        }
    };

    @Override
    protected void stopService() {
        log.info("停止存储空间管理服务");
//        videoStorageSubscription.unsubscribe();
        ThreadPoolManager.getInstance(TAG).cancelTaskThreads(TAG, true);
        videoStorageResource.release();
    }


    private void guardVideoSpace() {
        if (videoStorageResource.getResourceState().equals(ResourceState.Inited)) {
            videoStorageResource.guadSpace();
        }
    }

    private void test() {
        for (int i = 0; i < 8; i++) {
            RealmCallFactory.saveVideoInfo(true, "/storage/sdcard1/dudu/frontVideo/20160407203300.mp4", new RealmCallBack<VideoEntityRealm, Exception>() {
                @Override
                public void onRealm(VideoEntityRealm result) {
                    if (result != null) {
                        log.debug("加入视频文件：{}， 时间：{}，时间戳：{}", result.getFileName(), result.getCreateTime(), result.getTimeStamp());
                    }
                }

                @Override
                public void onError(Exception error) {

                }
            });
            try {
                Thread.sleep(2 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
