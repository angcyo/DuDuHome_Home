package com.dudu.aios.ui.control;

import com.dudu.aios.ui.utils.Rx;
import com.dudu.persistence.factory.RealmCallFactory;
import com.dudu.persistence.factory.RealmModelFactory;
import com.dudu.persistence.realmmodel.video.VideoEntity;
import com.dudu.persistence.realmmodel.video.VideoEntityRealm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.realm.RealmResults;
import rx.functions.Action1;

/**
 * Created by robi on 2016-06-02 18:17.
 */
public class VideoListControl {

    private List<VideoEntity> mFaceVideoData;//前置视频数据
    private List<VideoEntity> mBackVideoData;//后置视频数据
    private VideoComparator mVideoComparator;

    private Logger log = LoggerFactory.getLogger("video.VideoStorage");


    private IBackVideosListener mIBackVideosListener;
    private IFaceVideosListener mIFaceVideosListener;

    public VideoListControl() {
        mVideoComparator = new VideoComparator();
        mFaceVideoData = new ArrayList<>();
        mBackVideoData = new ArrayList<>();
    }

    public static VideoListControl instance() {
        return Holder.listControl;
    }

    private static void filterList(List<VideoEntity> list) {
        for (int i = 0; i < list.size(); i++) {
            if (!isExist(list.get(i).getAbsolutePath())) {
                list.remove(i);
                filterList(list);
                return;
            }
        }
    }

    private static boolean isExist(String filePath) {
        return new File(filePath).exists();
    }

    /**
     * 获取前置视频
     */
    public List<VideoEntity> getFaceVideos(IFaceVideosListener listener) {
        setIFaceVideosListener(listener);
        queryFaceVideos();
        return mFaceVideoData;
    }

    private void queryFaceVideos() {
//        ThreadExecutor.instance().onThread(() -> {
//            if (!com.dudu.drivevideo.utils.FileUtil.isTFlashCardExists()) {
//                log.info("前置 T卡不存在.");
//                mFaceVideoData = new ArrayList<>();
//            } else {
//                RealmCallFactory.tran(realm -> {
//                    RealmResults<VideoEntityRealm> faceVideos = realm.where(VideoEntityRealm.class).equalTo("cameraFlag", true).findAll();
//                    ArrayList<VideoEntity> videoEntityListFromRealmResult = RealmModelFactory.getVideoEntityListFromRealmResult(faceVideos);
//                    mFaceVideoData = videoEntityListFromRealmResult;
//
//                    Collections.sort(mFaceVideoData, mVideoComparator);
//                    int size = mFaceVideoData.size();
//                    filterList(mFaceVideoData);
//                    int size2 = mFaceVideoData.size();
//                    log.info("查询到前置视频数量:{} 过滤:{}", size2, size - size2);
//                });
//            }
//
//            ThreadExecutor.instance().onMain(() -> {
//                if (mIFaceVideosListener != null) {
//                    mIFaceVideosListener.onFaceVideos(mFaceVideoData);
//                }
//            });
//        });

        Rx.base("", face -> {
            if (!com.dudu.drivevideo.utils.FileUtil.isTFlashCardExists()) {
                log.info("前置 T卡不存在.");
                mFaceVideoData = new ArrayList<>();
            } else {
                RealmCallFactory.tran(realm -> {
                    RealmResults<VideoEntityRealm> faceVideos = realm.where(VideoEntityRealm.class).equalTo("cameraFlag", true).findAll();
                    ArrayList<VideoEntity> videoEntityListFromRealmResult = RealmModelFactory.getVideoEntityListFromRealmResult(faceVideos);
                    mFaceVideoData = videoEntityListFromRealmResult;

                    Collections.sort(mFaceVideoData, mVideoComparator);
                    int size = mFaceVideoData.size();
                    filterList(mFaceVideoData);
                    int size2 = mFaceVideoData.size();
                    log.info("查询到前置视频数量:{} 过滤:{}", size2, size - size2);
                });
            }

//            ThreadExecutor.instance().onMain(() -> {
//                if (mIFaceVideosListener != null) {
//                    mIFaceVideosListener.onFaceVideos(mFaceVideoData);
//                }
//            });
            return mFaceVideoData;
        }, new Action1<List<VideoEntity>>() {
            @Override
            public void call(List<VideoEntity> list) {
                if (mIFaceVideosListener != null) {
                    mIFaceVideosListener.onFaceVideos(list);
                }
            }
        });
    }

    private void queryBackVideos() {
//        ThreadExecutor.instance().onThread(() -> {
//            if (!com.dudu.drivevideo.utils.FileUtil.isTFlashCardExists()) {
//                log.info("后置 T卡不存在.");
//                mBackVideoData = new ArrayList<>();
//            } else {
//                RealmCallFactory.tran(realm -> {
//                    RealmResults<VideoEntityRealm> faceVideos = realm.where(VideoEntityRealm.class).equalTo("cameraFlag", false).findAll();
//                    ArrayList<VideoEntity> videoEntityListFromRealmResult = RealmModelFactory.getVideoEntityListFromRealmResult(faceVideos);
//                    mBackVideoData = videoEntityListFromRealmResult;
//
//                    Collections.sort(mBackVideoData, mVideoComparator);
//                    int size = mBackVideoData.size();
//                    filterList(mBackVideoData);
//                    int size2 = mBackVideoData.size();
//                    log.info("查询到后置视频数量:{} 过滤:{}", size2, size - size2);
//                });
//            }
//            ThreadExecutor.instance().onMain(() -> {
//                if (mIBackVideosListener != null) {
//                    mIBackVideosListener.onBackVideos(mBackVideoData);
//                }
//            });
//        });

        Rx.base("", face -> {
            if (!com.dudu.drivevideo.utils.FileUtil.isTFlashCardExists()) {
                log.info("后置 T卡不存在.");
                mBackVideoData = new ArrayList<>();
            } else {
                RealmCallFactory.tran(realm -> {
                    RealmResults<VideoEntityRealm> faceVideos = realm.where(VideoEntityRealm.class).equalTo("cameraFlag", false).findAll();
                    ArrayList<VideoEntity> videoEntityListFromRealmResult = RealmModelFactory.getVideoEntityListFromRealmResult(faceVideos);
                    mBackVideoData = videoEntityListFromRealmResult;

                    Collections.sort(mBackVideoData, mVideoComparator);
                    int size = mBackVideoData.size();
                    filterList(mBackVideoData);
                    int size2 = mBackVideoData.size();
                    log.info("查询到后置视频数量:{} 过滤:{}", size2, size - size2);
                });
            }
//            ThreadExecutor.instance().onMain(() -> {
//                if (mIBackVideosListener != null) {
//                    mIBackVideosListener.onBackVideos(mBackVideoData);
//                }
//            });
            return mBackVideoData;
        }, new Action1<List<VideoEntity>>() {
            @Override
            public void call(List<VideoEntity> list) {
                if (mIBackVideosListener != null) {
                    mIBackVideosListener.onBackVideos(list);
                }
            }
        });
    }

    public void setIBackVideosListener(IBackVideosListener IBackVideosListener) {
        mIBackVideosListener = IBackVideosListener;
    }

    public void setIFaceVideosListener(IFaceVideosListener IFaceVideosListener) {
        mIFaceVideosListener = IFaceVideosListener;
    }

    /**
     * 获取后置视频
     */
    public List<VideoEntity> getBackVideos(IBackVideosListener listener) {
        setIBackVideosListener(listener);
        queryBackVideos();
        return mBackVideoData;
    }

    public interface IFaceVideosListener {
        void onFaceVideos(List<VideoEntity> videos);
    }

    public interface IBackVideosListener {
        void onBackVideos(List<VideoEntity> videos);
    }

    private static class Holder {
        static final VideoListControl listControl = new VideoListControl();
    }

    private static class VideoComparator implements Comparator<VideoEntity> {

        @Override
        public int compare(VideoEntity lhs, VideoEntity rhs) {
            return rhs.getFileName().compareToIgnoreCase(lhs.getFileName());
        }
    }

}
