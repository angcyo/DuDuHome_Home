package com.dudu.aios.ui.voice;

import android.view.LayoutInflater;
import android.view.View;

import com.dudu.aios.ui.fragment.base.BaseFragment;
import com.dudu.android.launcher.R;
import com.dudu.drivevideo.frontcamera.FrontCameraManage;

import org.slf4j.LoggerFactory;

/**
 * Created by lxh on 2016/2/13.
 */
public class VoiceFragment extends BaseFragment {

    @Override
    public View getView() {

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.voice_layout, null);

        return view;
    }

    @Override
    public void onHide() {
        super.onHide();
    }

    @Override
    public void onShow() {
        super.onShow();
        LoggerFactory.getLogger("video1.frontdrivevideo").debug("设置模糊状态：{}", true);
        FrontCameraManage.getInstance().setPreviewBlur(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
