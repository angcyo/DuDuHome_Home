package com.dudu.aios.ui.control;

import com.dudu.aios.ui.utils.Rx;
import com.dudu.persistence.factory.RealmCallFactory;
import com.dudu.persistence.factory.RealmModelFactory;
import com.dudu.persistence.realmmodel.picture.PictureEntity;
import com.dudu.persistence.realmmodel.picture.PictureEntityRealm;

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
 * Created by robi on 2016-06-02 19:50.
 */
public class PhotoListControl {


    private List<PictureEntity> photoItemBeanList;
    private IPhotoListener mIPhotoListener;
    private PhotoListComparator mListComparator;
    private Logger log = LoggerFactory.getLogger("photo");

    public PhotoListControl() {
        photoItemBeanList = new ArrayList<>();
        mListComparator = new PhotoListComparator();
    }

    public static PhotoListControl instance() {
        return Holder.control;
    }

    private static boolean isExist(String filePath) {
        return new File(filePath).exists();
    }

    private static void filterList(List<PictureEntity> list) {
        for (int i = 0; i < list.size(); i++) {
            if (!isExist(list.get(i).getAbsolutePath())) {
                list.remove(i);
                filterList(list);
                return;
            }
        }
    }

    public void setIPhotoListener(IPhotoListener IPhotoListener) {
        mIPhotoListener = IPhotoListener;
    }

    public List<PictureEntity> getPhotos(IPhotoListener listener) {
        setIPhotoListener(listener);
        queryPhoto();
        return photoItemBeanList;
    }

    private void queryPhoto() {
//        ThreadExecutor.instance().onThread(() -> {
//            if (com.dudu.drivevideo.utils.FileUtil.isTFlashCardExists()) {
//                RealmCallFactory.tran(realm -> {
//                    RealmResults<PictureEntityRealm> facePhotos = realm.where(PictureEntityRealm.class).equalTo("cameraFlag", true).findAll();//所有前置图片
//                    List<PictureEntity> photos = RealmModelFactory.getPictureEntityListFromRealResuls(facePhotos);
//
//                    RealmResults<PictureEntityRealm> backPhotosRealm = realm.where(PictureEntityRealm.class).equalTo("cameraFlag", false).findAll();//所有后置图片
//                    List<PictureEntity> backPhotos = RealmModelFactory.getPictureEntityListFromRealResuls(backPhotosRealm);
//
//                    photos.addAll(backPhotos);
//
//                    Collections.sort(photos, mListComparator);
//                    int size = photos.size();
//                    filterList(photos);
//                    int size2 = photos.size();
//                    log.info("获取到图片数量:{} 过滤不存在的图片数量:{}", size2, size - size2);
//                    for (int i = 0; i < photos.size(); i++) {
//                        photos.get(i).itemPosition = i;
//                    }
//
//                    photoItemBeanList = photos;
//                });
//            } else {
//                photoItemBeanList = new ArrayList<>();
//            }
//
//            ThreadExecutor.instance().onMain(() -> {
//                if (mIPhotoListener != null) {
//                    mIPhotoListener.onPhotos(photoItemBeanList);
//                }
//            });
//        });

        Rx.base("", s -> {
            if (com.dudu.drivevideo.utils.FileUtil.isTFlashCardExists()) {
                RealmCallFactory.tran(realm -> {
                    RealmResults<PictureEntityRealm> facePhotos = realm.where(PictureEntityRealm.class).equalTo("cameraFlag", true).findAll();//所有前置图片
                    List<PictureEntity> photos = RealmModelFactory.getPictureEntityListFromRealResuls(facePhotos);

                    RealmResults<PictureEntityRealm> backPhotosRealm = realm.where(PictureEntityRealm.class).equalTo("cameraFlag", false).findAll();//所有后置图片
                    List<PictureEntity> backPhotos = RealmModelFactory.getPictureEntityListFromRealResuls(backPhotosRealm);

                    photos.addAll(backPhotos);

                    Collections.sort(photos, mListComparator);
                    int size = photos.size();
                    filterList(photos);
                    int size2 = photos.size();
                    log.info("获取到图片数量:{} 过滤不存在的图片数量:{}", size2, size - size2);
                    for (int i = 0; i < photos.size(); i++) {
                        photos.get(i).itemPosition = i;
                    }

                    photoItemBeanList = photos;
                });
                return photoItemBeanList;
            } else {
                photoItemBeanList = new ArrayList<>();
            }

//            ThreadExecutor.instance().onMain(() -> {
//                if (mIPhotoListener != null) {
//                    mIPhotoListener.onPhotos(photoItemBeanList);
//                }
//            });
            return photoItemBeanList;
        }, new Action1<List<PictureEntity>>() {
            @Override
            public void call(List<PictureEntity> pictureEntities) {
                if (mIPhotoListener != null) {
                    mIPhotoListener.onPhotos(pictureEntities);
                }
            }
        });
    }

    public interface IPhotoListener {
        void onPhotos(List<PictureEntity> photos);
    }

    static class PhotoListComparator implements Comparator<PictureEntity> {

        @Override
        public int compare(PictureEntity lhs, PictureEntity rhs) {
            return (int) (rhs.getTimeStamp() - lhs.getTimeStamp());
        }
    }

    static class Holder {
        static final PhotoListControl control = new PhotoListControl();
    }
}
