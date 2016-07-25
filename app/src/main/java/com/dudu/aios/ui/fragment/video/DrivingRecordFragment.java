package com.dudu.aios.ui.fragment.video;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.dudu.aios.ui.base.ObservableFactory;
import com.dudu.aios.ui.base.T;
import com.dudu.aios.ui.fragment.base.RBaseFragment;
import com.dudu.aios.ui.utils.Debug;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.broadcast.ACCReceiver;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.drivevideo.frontcamera.FrontCameraManage;
import com.dudu.drivevideo.rearcamera.BlurControl;
import com.dudu.drivevideo.rearcamera.RearCameraManage;
import com.dudu.drivevideo.utils.FileUtil;
import com.umeng.analytics.MobclickAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2016/2/2.
 */
public class DrivingRecordFragment extends RBaseFragment implements /*SurfaceHolder.Callback, */View.OnClickListener {

    public static boolean isFrontCameraPreView = true;
    View bottomLayout;
    private ImageButton mCheckVideoButton, mSwitchVideoButton, mTakePhotoButton, mCheckPhotoButton, mBackButton;
    private Logger log;
    boolean isBackCaring = false;
    private RelativeLayout mScreenContainer;

    private Handler handler = new Handler(Looper.getMainLooper());

    public DrivingRecordFragment() {
        log = LoggerFactory.getLogger("video.reardrivevideo");
    }

    @Override
    protected int getContentView() {
        return R.layout.fragment_driving_record;
    }

    @Override
    protected void initView(View rootView) {
        super.initView(rootView);
        initFragmentView(rootView);
    }

    @Override
    protected void initViewData() {

        initClickListener();
    }

    private void initFragmentView(View view) {
        mCheckVideoButton = (ImageButton) view.findViewById(R.id.check_video);
        mSwitchVideoButton = (ImageButton) view.findViewById(R.id.switch_video);
        mCheckPhotoButton = (ImageButton) view.findViewById(R.id.check_photo);
        mTakePhotoButton = (ImageButton) view.findViewById(R.id.take_photo);
        mBackButton = (ImageButton) view.findViewById(R.id.button_back);
        bottomLayout = view.findViewById(R.id.video_button_bottom);
        mScreenContainer = (RelativeLayout) view.findViewById(R.id.screen_container);


        handlerShow();
        setBottomVisibility();
    }

    private void showAnimationIfCaring() {
//        Bundle bundle = FragmentConstants.TEMP_ARGS;
//        if (bundle != null) {
//            isBackCaring = bundle.getBoolean(FragmentConstants.IS_BACK_CARING, false);
//        }
        isBackCaring = ACCReceiver.isBackCarIng;
        if(null==FragmentConstants.TEMP_ARGS){
            FragmentConstants.TEMP_ARGS = new Bundle();
        }
        if (!isBackCaring) {
            startAnim();
        }
    }

    private void setBottomVisibility(){
        if (!isBackCaring) {
//            bottomLayout.setVisibility(View.VISIBLE);
            mScreenContainer.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    int action = event.getAction();
                    if (action == MotionEvent.ACTION_DOWN) {
                        if (bottomLayout.getVisibility() == View.INVISIBLE) {
                            bottomLayout.setVisibility(View.VISIBLE);
                            startAnim();
                        } else {

                        }
                    }
                    return false;
                }
            });
        } else {
            bottomLayout.setVisibility(View.INVISIBLE);
            mScreenContainer.setOnTouchListener(null);
        }
    }

    private void initClickListener() {
        mSwitchVideoButton.setOnClickListener(this);
        mCheckVideoButton.setOnClickListener(this);
        mCheckPhotoButton.setOnClickListener(this);
        mTakePhotoButton.setOnClickListener(this);
        mBackButton.setOnClickListener(this);
    }


    @Override
    public void onResume() {
        super.onResume();
        log.debug("onResume");
        Debug.debug(this.getClass().getSimpleName() + " onResume");
        mCheckPhotoButton.setEnabled(true);
        mBackButton.setEnabled(true);
        mCheckVideoButton.setEnabled(true);

//        changeBlur();

//        mBaseActivity.setNoBlur();
//        FrontCameraManage.getInstance().setPreviewBlur(false);

        ObservableFactory.getInstance().getCommonObservable().hasBackground.set(false);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void delayHideVideoButton(long delayMillis) {
        handler.postDelayed(() -> {
            bottomLayout.clearAnimation();
            bottomLayout.setVisibility(View.INVISIBLE);
        }, delayMillis);
    }

    private void cancerHideVideoButton() {
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.check_video:
                MobclickAgent.onEvent(getActivity(), ClickEvent.DRIVE_VIDEO_LIST.getEventId());
                changeToFrontPreview();
                replaceFragment(FragmentConstants.FRAGMENT_VIDEO_LIST);
                break;
            case R.id.take_photo:
                MobclickAgent.onEvent(getActivity(), ClickEvent.DRIVE_TAKE_PICTURE.getEventId());
                takePicture();
//                replaceFragment(FragmentConstants.TIRE_FRAGMENT);
                break;
            case R.id.switch_video:
                MobclickAgent.onEvent(getActivity(), ClickEvent.DRIVE_CHANGE_SHOT.getEventId());
                log.debug("切换显示");
                changeShot();
                break;
            case R.id.check_photo:
                MobclickAgent.onEvent(getActivity(), ClickEvent.DRIVE_PICTURE_SCAN.getEventId());
                changeToFrontPreview();
                replaceFragment(FragmentConstants.FRAGMENT_PHOTO_LIST);
                break;
            case R.id.button_back:
                ObservableFactory.getInstance().getCommonObservable().hasBackground.set(true);
                changeToFrontPreview();

                //FrontCameraManage.getInstance().setPreviewBlur(true);
                replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
                break;
        }
    }

    private void showToast(String s) {
        if (mBaseActivity != null) {
            T.show(mBaseActivity, s);
        }
    }

    private void changeToRearPreview() {
        isFrontCameraPreView = false;
        if (RearCameraManage.getInstance().isRearCameraExist()){
            BlurControl.instance().setBlur(false);
        }else {
            RearCameraManage.log.info("后置摄像头不存在，重置USB mode");
            RearCameraManage.getInstance().resetUsbMode();
        }
    }

    private void changeBlur() {
//        if (this.isVisible()) {
//            BlurControl.instance().setBlur(false);
//        } else {
//            BlurControl.instance().setBlur(true);
//        }
    }

    private void changeToFrontPreview() {
        isFrontCameraPreView = true;
        BlurControl.instance().setBlur(true);
//        RearCameraManage.getInstance().stopPreview();
    }


    private void changePreview() {
        log.info("当前预览：{}", isFrontCameraPreView ? "前置预览" : "后置预览");
        if (isFrontCameraPreView) {
            changeToRearPreview();
        } else {
            changeToFrontPreview();
        }
    }

    private void startAnim() {
        if (bottomLayout == null) {
            return;
        }
        bottomLayout.setVisibility(View.VISIBLE);
        bottomLayout.setScaleX(0f);
        bottomLayout.setScaleY(0f);

        bottomLayout.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(300)
                .withEndAction(() -> bottomLayout.animate().scaleX(1f).scaleY(1f).setDuration(300).start()).start();

        delayHideVideoButton(10*1000);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
//        log.debug("onHiddenChanged : {}", hidden);
        super.onHiddenChanged(hidden);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onAdd() {
        super.onAdd();
        log.debug("onAdd");
        Debug.debug(this.getClass().getSimpleName() + " onAdd");
        fragmentShowed();
    }

    @Override
    public void onShow() {
        super.onShow();
        fragmentShowed();
        setBottomVisibility();
    }

    public void fragmentShowed(){
        //        ((MainRecordActivity) getActivity()).setBlur(false);
        log.debug("onshow");
        Debug.debug(this.getClass().getSimpleName() + " onShow");
        if (RearCameraManage.getInstance().isRearCameraExist()) {
            isFrontCameraPreView = false;
//            RearCameraManage.getInstance().startPreview();
            changeBlur();
        }
//        BlurControl.instance().setBlur(false);

        LoggerFactory.getLogger("video1.frontdrivevideo").debug("设置模糊状态：{}", false);
        FrontCameraManage.getInstance().setPreviewBlur(false);
//        FrontCameraInstance.getInstance().switchBlur(false);
        showAnimationIfCaring();
        handlerShow();
    }

    @Override
    public void onHide() {
        log.debug("onHide");
        Debug.debug(this.getClass().getSimpleName() + " onHide");
//        ((MainRecordActivity) getActivity()).setBlur(true);
        changeToFrontPreview();
        LoggerFactory.getLogger("video1.frontdrivevideo").debug("设置模糊状态：{}", true);
        FrontCameraManage.getInstance().setPreviewBlur(true);
//        FrontCameraInstance.getInstance().switchBlur(true);
        cancerHideVideoButton();
    }

    private void handlerShow() {
        if (FragmentConstants.TEMP_ARGS != null) {
            Bundle b = FragmentConstants.TEMP_ARGS;
            String type = b.getString(FragmentConstants.OPEN_VIDEO_TYPE);
            if (!TextUtils.isEmpty(type)) {
                switch (type) {
                    case FragmentConstants.TAKE_PICTURE:
                        takePicture();
                        break;
                    case FragmentConstants.CHANGE_SHOT:
                        changeShot();
                        break;
                }
                b.putString(FragmentConstants.OPEN_VIDEO_TYPE, "");
            }
        }

    }

    private void takePicture() {
        if (FileUtil.isTFlashCardExists()) {
            float sdFreeSpace = FileUtil.getSdFreeSpace();
            log.debug("拍照请求,T卡剩余空间比例:{}", sdFreeSpace);
            if (sdFreeSpace < 0.01) {
                showToast("存储卡空间不足, 无法拍照.");
            } else {
                log.info("拍照当前拍照：{}", isFrontCameraPreView ? "前置拍照" : "后置拍照");
                if (isFrontCameraPreView) {
                    //前置拍照
                    FrontCameraManage.getInstance().takePicture();
//                            FrontCameraInstance.getInstance().takePicture();
                } else {
                    //后置拍照
                    RearCameraManage.getInstance().takePhoto();
                }
            }
        } else {
            showToast("请确认存储卡是否已装置好");
        }
    }

    private void changeShot() {
        LoggerFactory.getLogger("video1.frontdrivevideo").debug("设置模糊状态：{}", false);
//                FrontCameraInstance.getInstance().switchBlur(false);
        FrontCameraManage.getInstance().setPreviewBlur(false);
        changePreview();
    }
}
