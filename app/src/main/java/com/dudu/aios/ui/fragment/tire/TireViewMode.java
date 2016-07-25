package com.dudu.aios.ui.fragment.tire;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.dudu.aios.ui.fragment.base.RBaseFragment;
import com.dudu.aios.ui.fragment.base.RBaseViewHolder;
import com.dudu.aios.ui.view.RotateAnimation;
import com.dudu.android.launcher.R;
import com.dudu.init.CarFireManager;
import com.dudu.monitor.tirepressure.TirePairSp;
import com.dudu.monitor.tirepressure.TirePressureManage;
import com.dudu.monitor.tirepressure.TirePressureManager;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.workflow.driving.CarStatusUtils;
import com.dudu.workflow.tpms.TPMSInfo;

import org.slf4j.Logger;

import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by robi on 2016-06-28 14:05.
 */
public class TireViewMode {
    protected Logger log = TirePressureManager.log;
    RBaseFragment mBaseFragment;
    RBaseViewHolder mBaseViewHolder;
    /*4个轮子布局*/
    TpmsLayoutMode tpmsL1Mode;
    TpmsLayoutMode tpmsL2Mode;
    TpmsLayoutMode tpmsR1Mode;
    TpmsLayoutMode tpmsR2Mode;
    /*轮子四根线*/
    ImageView tpmsL1ModeLine;
    ImageView tpmsL2ModeLine;
    ImageView tpmsR1ModeLine;
    ImageView tpmsR2ModeLine;
    /*3个范围布局*/
    ItemInfoMode tempMode;
    ItemInfoMode frontMode;
    ItemInfoMode backMode;
    TireValueHelper mValueHelper;
    //    AlphaAnimation mAlphaAnimation;
    AnimationSet mAnimationSet;//进入界面时,播放

    AlphaAnimation splashAnimation;//闪烁动画,异常时播放

    RotateAnimation mRotateAnimation;//Y轴旋转动画

    ScaleAnimation mScaleAnimation;//值改变时,播放

    boolean isInvert = false;

    /**
     * 当前正在对码的轮胎
     */
    int curPairPosition = -1;

    TPMSInfo oldInfo1;
    TPMSInfo oldInfo2;
    TPMSInfo oldInfo3;
    TPMSInfo oldInfo4;

    public TireViewMode(RBaseFragment baseFragment) {
        mBaseFragment = baseFragment;
        mValueHelper = new TireValueHelper();
//        mAlphaAnimation = new AlphaAnimation(0f, 1f);
//        mAlphaAnimation.setDuration(700);
//        mAlphaAnimation.setInterpolator(new DecelerateInterpolator());

        /*进入界面的动画*/
        mAnimationSet = new AnimationSet(true);
        ScaleAnimation scaleAnimation = new ScaleAnimation(2f, 1f, 2f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
        mAnimationSet.setDuration(500);
        mAnimationSet.setInterpolator(new DecelerateInterpolator());
        mAnimationSet.addAnimation(scaleAnimation);
        mAnimationSet.addAnimation(alphaAnimation);

        /*闪烁动画*/
        splashAnimation = new AlphaAnimation(0.3f, 1f);
        splashAnimation.setDuration(200);
        splashAnimation.setInterpolator(new AccelerateInterpolator());
        splashAnimation.setRepeatCount(Animation.INFINITE);
        splashAnimation.setRepeatMode(Animation.REVERSE);

        /*Y周旋转动画*/
        mRotateAnimation = new RotateAnimation(34 / 2, 74 / 2, RotateAnimation.ROTATE_DECREASE);
        mRotateAnimation.setDuration(700);
//        mRotateAnimation.setRepeatCount(RotateAnimation.INFINITE);

        /*值改变时的动画*/
        mScaleAnimation = new ScaleAnimation(1.5f, 1f, 1.5f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mScaleAnimation.setDuration(300);
        mScaleAnimation.setInterpolator(new DecelerateInterpolator());
    }

    public void initViewMode(RBaseViewHolder viewHolder) {
        mBaseViewHolder = viewHolder;

        initAllView();
        initAllData();
    }

    private void initAllView() {
        tpmsL1Mode = new TpmsLayoutMode(mBaseViewHolder.v(R.id.tpmsL1Layout));
        tpmsL2Mode = new TpmsLayoutMode(mBaseViewHolder.v(R.id.tpmsL2Layout));
        tpmsR1Mode = new TpmsLayoutMode(mBaseViewHolder.v(R.id.tpmsR1Layout));
        tpmsR2Mode = new TpmsLayoutMode(mBaseViewHolder.v(R.id.tpmsR2Layout));

        tpmsL1ModeLine = mBaseViewHolder.imgV(R.id.tpmsL1Line);
        tpmsL2ModeLine = mBaseViewHolder.imgV(R.id.tpmsL2Line);
        tpmsR1ModeLine = mBaseViewHolder.imgV(R.id.tpmsR1Line);
        tpmsR2ModeLine = mBaseViewHolder.imgV(R.id.tpmsR2Line);

        tempMode = new ItemInfoMode(mBaseViewHolder.v(R.id.tempItemLayout));
        frontMode = new ItemInfoMode(mBaseViewHolder.v(R.id.frontItemLayout));
        backMode = new ItemInfoMode(mBaseViewHolder.v(R.id.backItemLayout));
    }

    private void initAllData() {
        tempMode.mImageView.setImageResource(R.drawable.icon_temp);
        tempMode.titleView.setText("轮胎高温预警");

        frontMode.mImageView.setImageResource(R.drawable.icon_tire_front);
        frontMode.titleView.setText("前胎压预警范围");

        backMode.mImageView.setImageResource(R.drawable.icon_tire_back);
        backMode.titleView.setText("后胎压预警范围");

        updateModeInfo();
        startAnim();

//        tpmsL1Mode.showTip("数据\n读取中...");
//        tpmsL2Mode.showTip("数据\n读取中...");
//        tpmsR1Mode.showTip("数据\n读取中...");
//        tpmsR2Mode.showTip("数据\n读取中...");

        tpmsL1Mode.showCodeLayout("点击对码");
        tpmsL2Mode.showCodeLayout("点击对码");
        tpmsR1Mode.showCodeLayout("点击对码");
        tpmsR2Mode.showCodeLayout("点击对码");

        tpmsL1ModeLine.setImageResource(R.drawable.line_blue_left);
        tpmsL2ModeLine.setImageResource(R.drawable.line_blue_left);
        tpmsR1ModeLine.setImageResource(R.drawable.line_blue_right);
        tpmsR2ModeLine.setImageResource(R.drawable.line_blue_right);

        tpmsL1Mode.position = TirePressureManager.L_1;
        tpmsL2Mode.position = TirePressureManager.L_2;
        tpmsR1Mode.position = TirePressureManager.R_1;
        tpmsR2Mode.position = TirePressureManager.R_2;
    }

    /**
     * 更新,温度,胎压范围值
     */
    public void updateModeInfo() {
        backMode.valueView.setText(mValueHelper.getBackTpmsRange());
        frontMode.valueView.setText(mValueHelper.getFrontTpmsRange());
        tempMode.valueView.setText(mValueHelper.getMaxTempString());
    }

    public void onShow() {
        startAnim();
    }

    public void onHide() {
        curPairPosition = -1;//取消对码轮胎标识
        cancelAnim();
//        mValueHelper.pairCancel();
        checkTpms();
        stopSplashAnimation(tpmsL1Mode);
        stopSplashAnimation(tpmsL2Mode);
        stopSplashAnimation(tpmsR1Mode);
        stopSplashAnimation(tpmsR2Mode);
    }

    public void cancelPair(){
        mValueHelper.pairCancel();
    }

    private void checkTpms() {
        CarStatusUtils.isFired()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fired -> {
                    if (!fired) {
                        //如果界面退出.依然没有点火,则关闭胎压串口
                        CarFireManager.getInstance().releaseTpms();
                    }
                }, throwable -> log.error("checkFireStart", throwable));
    }

    private void startAnim() {
        mBaseViewHolder.itemView.startAnimation(mAnimationSet);
//        mBaseViewHolder.v(R.id.rightLayout).startAnimation(mAlphaAnimation);

        startTempModeAnim();

        checkCarFire();

        TireDataPull.instance().pullTireData();
    }

    /**
     * 开始温度图标动画
     */
    private void startTempModeAnim() {
        cleanModeAnim();
        mRotateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                startFrontModeAnim();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        tempMode.mImageView.startAnimation(mRotateAnimation);
//        frontMode.mImageView.startAnimation(mRotateAnimation);
//        backMode.mImageView.startAnimation(mRotateAnimation);
    }


    /**
     * 开始前轴轮图标动画
     */
    private void startFrontModeAnim() {
        cleanModeAnim();
        mRotateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isInvert) {
                    startTempModeAnim();
                } else {
                    startBackModeAnim();
                }
                isInvert = !isInvert;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
//        tempMode.mImageView.startAnimation(mRotateAnimation);
        frontMode.mImageView.startAnimation(mRotateAnimation);
//        backMode.mImageView.startAnimation(mRotateAnimation);
    }


    /**
     * 开始后轴轮图标动画
     */
    private void startBackModeAnim() {
        cleanModeAnim();
        mRotateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
//                startTempModeAnim();
                startFrontModeAnim();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
//        tempMode.mImageView.startAnimation(mRotateAnimation);
//        frontMode.mImageView.startAnimation(mRotateAnimation);
        backMode.mImageView.startAnimation(mRotateAnimation);
    }

    private void cancelAnim() {
        mBaseViewHolder.itemView.clearAnimation();

        mRotateAnimation.setAnimationListener(null);
        cleanModeAnim();

        mScaleAnimation.cancel();
    }

    private void cleanModeAnim() {
        tempMode.mImageView.clearAnimation();
        frontMode.mImageView.clearAnimation();
        backMode.mImageView.clearAnimation();
    }

    /**
     * 检测车辆是否点火
     */
    private void checkCarFire() {
        CarStatusUtils.isFired()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(TireValueHelper::setIsFire, throwable -> log.error("checkCarFire", throwable));
    }

    /**
     * 设置轮胎信息
     * mTPMSInfo1;//右前轮   轮胎信息
     * mTPMSInfo2;//  左前轮 轮胎信息
     * mTPMSInfo3;//右后轮   轮胎信息
     * mTPMSInfo4;//  左后轮 轮胎信息
     */
    public void setTireData(boolean change1, boolean change2, boolean change3, boolean change4, TPMSInfo info1, TPMSInfo info2, TPMSInfo info3, TPMSInfo info4) {
//        if (change1) {
        tpmsR1(info1);
//        }

//        if (change2) {
        tpmsL1(info2);
//        }

//        if (change3) {
        tpmsR2(info3);
//        }

//        if (change4) {
        tpmsL2(info4);
//        }
    }

    /**
     * 设置轮胎异常
     */
    public void setTireException(int position, TPMSInfo info) {
        if (position == TirePressureManager.R_1) {
            //"右前轮";
        } else if (position == TirePressureManager.L_1) {
            //"左前轮";
        } else if (position == TirePressureManager.R_2) {
            //"右后轮";
        } else if (position == TirePressureManager.L_2) {
            //"左后轮";
        }
    }

    /*左前轮*/
    private void tpmsL1(TPMSInfo info) {
        final boolean exception = tpmsHandle(tpmsL1Mode, oldInfo2, info, TirePressureManager.L_1);
        oldInfo2 = info;
        if (exception) {
            tpmsL1ModeLine.setImageResource(R.drawable.line_red_left);
        } else {
            tpmsL1ModeLine.setImageResource(R.drawable.line_blue_left);
        }
    }

    /*左后轮*/
    private void tpmsL2(TPMSInfo info) {
        final boolean exception = tpmsHandle(tpmsL2Mode, oldInfo4, info, TirePressureManager.L_2);
        oldInfo4 = info;
        if (exception) {
            tpmsL2ModeLine.setImageResource(R.drawable.line_red_left);
        } else {
            tpmsL2ModeLine.setImageResource(R.drawable.line_blue_left);
        }
    }

    /*右前轮*/
    private void tpmsR1(TPMSInfo info) {
        final boolean exception = tpmsHandle(tpmsR1Mode, oldInfo1, info, TirePressureManager.R_1);
        oldInfo1 = info;
        if (exception) {
            tpmsR1ModeLine.setImageResource(R.drawable.line_red_right);
        } else {
            tpmsR1ModeLine.setImageResource(R.drawable.line_blue_right);
        }
    }

    /*右后轮*/
    private void tpmsR2(TPMSInfo info) {
        final boolean exception = tpmsHandle(tpmsR2Mode, oldInfo3, info, TirePressureManager.R_2);
        oldInfo3 = info;
        if (exception) {
            tpmsR2ModeLine.setImageResource(R.drawable.line_red_right);
        } else {
            tpmsR2ModeLine.setImageResource(R.drawable.line_blue_right);
        }
    }

    private boolean tpmsHandle(TpmsLayoutMode mode, TPMSInfo oldInfo, TPMSInfo info, int pos) {
        stopSplashAnimation(mode);//停止之前的动画

        boolean isExp = false;//是否出现了异常
        if (info == null) {
            mode.showCodeLayout("点击对码");
            log.info("对码:info==null");
            TirePressureManage.uploadEmptyTirePressureData(pos);
        } else if (curPairPosition == info.getPosition()) {
            //当前是否正在对码...
            mode.showTip("对码中...");
        } else if (!TirePairSp.isTpmsPair(info.getPosition())) {
            mode.showCodeLayout("点击对码");
            log.info("对码{}:未对码", TirePressureManager.getTireChinese(info.getPosition()));
            TirePressureManage.uploadEmptyTirePressureData(pos);
        } else if (info.pressure == 0) {//胎压为0时,提示对码
            mode.showCodeLayout("点击对码");
            log.info("对码{}:info.pressure==0", TirePressureManager.getTireChinese(info.getPosition()));
            TirePairSp.setTpmsPair(info.getPosition(), false);
            TirePressureManage.uploadEmptyTirePressureData(pos);
        } else if (TirePressureManager.isTpmsEmpty(info)) {
            mode.showCodeLayout("传感器异常\n点击对码");
            TirePairSp.setTpmsPair(info.getPosition(), false);
            log.info("对码{}:info.temperature == 0 && info.pressure == 0", TirePressureManager.getTireChinese(info.getPosition()));
            TirePressureManage.uploadEmptyTirePressureData(pos);
        } else if (info.noData) {
            mode.showCodeLayout("点击对码");
//            TirePairSp.setTpmsPair(info.getPosition(), false);//noData 不重置对码标识
            mValueHelper.pairCancel();//取消对码
            log.info("对码{}:noData==true", TirePressureManager.getTireChinese(info.getPosition()));
            TirePressureManage.uploadEmptyTirePressureData(pos);
        } else if (info.gasLeaks == 1) {
            mode.showTip("轮胎漏气", Color.RED);
            mode.textView.startAnimation(splashAnimation);
            isExp = true;
        } else {
            mode.showInfoLayout();
            mode.tempView.setText(mValueHelper.getTempTextView(info.getTemperature()));
            mode.tireView.setText(mValueHelper.getTpmsTextView(info.getPressure()));

            /*温度异常正常,颜色处理*/
//            if (TirePressureManager.isTempException(info.getTemperature())) {
            if (TirePressureManager.isTempException(info)) {
                isExp = true;
                mode.tempView.setTextColor(Color.RED);
                mode.tempView.startAnimation(splashAnimation);
            } else {
                mode.tempView.setTextColor(Color.WHITE);

                    /*值改变了,播放强调动画*/
                if (oldInfo == null || oldInfo.getTemperature() != info.getTemperature()) {
                    accentView(mode.tempView);
                }
            }

            /*前后轮胎压异常正常,颜色处理*/
            final int position = info.getPosition();
            if (position == TirePressureManager.R_1 || position == TirePressureManager.L_1) {
//                if (TirePressureManager.isFrontTpmsException(info.getPressure())) {
                if (TirePressureManager.isTpmsException(info)) {
                    isExp = true;
                    mode.tireView.setTextColor(Color.RED);
                    mode.tireView.startAnimation(splashAnimation);
                } else {
                    mode.tireView.setTextColor(Color.WHITE);

                    /*值改变了,播放强调动画*/
                    if (oldInfo == null || oldInfo.getPressure() != info.getPressure()) {
                        accentView(mode.tireView);
                    }
                }
            } else if (position == TirePressureManager.R_2 || position == TirePressureManager.L_2) {
//                if (TirePressureManager.isBackTpmsException(info.getPressure())) {
                if (TirePressureManager.isTpmsException(info)) {
                    isExp = true;
                    mode.tireView.setTextColor(Color.RED);
                    mode.tireView.startAnimation(splashAnimation);
                } else {
                    mode.tireView.setTextColor(Color.WHITE);

                    /*值改变了,播放强调动画*/
                    if (oldInfo == null || oldInfo.getPressure() != info.getPressure()) {
                        accentView(mode.tireView);
                    }
                }
            }

            //上传胎压信息
//            log.info("TireViewMode 准备上传{} {}胎压信息.", info.getPosition(),
//                    TirePressureManager.getTireChinese(info.getPosition()));
//            TirePressureManage.uploadTirePressureData(new TirePressureUpload(info));
        }

//        final int position = info.getPosition();
//        if (position == TirePressureManager.R_1) {
//            mode.showTip("右前轮");
//            isExp = true;
//        } else if (position == TirePressureManager.L_1) {
//            mode.showTip("左前轮");
//        } else if (position == TirePressureManager.R_2) {
//            mode.showTip("右后轮");
//            isExp = true;
//        } else if (position == TirePressureManager.L_2) {
//            mode.showTip("左后轮");
//        }

        return isExp;
    }

    /**
     * 强调View, 播放一个缩小的动画
     */
    private void accentView(View view) {
        view.startAnimation(mScaleAnimation);
    }

    private void stopSplashAnimation(TpmsLayoutMode mode) {
        mode.tempView.clearAnimation();
        mode.tireView.clearAnimation();
        mode.textView.clearAnimation();
    }

    /**
     * 胎压布局,包括胎压值, 温度值, 是否对码
     */
    class TpmsLayoutMode {

        //信息布局,控制隐藏显示
        public View infoLayout;
        //温度信息
        public TextView tempView;
        //胎压信息
        public TextView tireView;
        //对码布局
        public View codeLayout;
        //对码信息
        public TextView textView;
        public int position;//轮胎的位置
        Runnable pairOkRunnable = () -> {
            //对码成功回调
            curPairPosition = -1;
            mValueHelper.pairCancel();

            TirePairSp.setTpmsPair(position, true);
            //设置一次模组预警数据
            // TODO: 2016-07-06
        };
        Runnable timeOutRunnable = () -> {
            //对码超时回调处理
            showCodeLayout("点击对码");
            curPairPosition = -1;
            mValueHelper.pairCancel();
            TirePairSp.setTpmsPair(position, false);
        };

        public TpmsLayoutMode(View itemLayout) {
            infoLayout = itemLayout.findViewById(R.id.infoLayout);
            tireView = (TextView) itemLayout.findViewById(R.id.tireView);
            tempView = (TextView) itemLayout.findViewById(R.id.tempView);

            codeLayout = itemLayout.findViewById(R.id.codeLayout);
            textView = (TextView) itemLayout.findViewById(R.id.textView);
        }

        /*展示提示信息*/
        public void showTip(String tip) {
            showTip(tip, Color.WHITE);
        }

        /*展示提示信息*/
        public void showTip(String tip, @ColorInt int color) {
            infoLayout.setVisibility(View.GONE);
            codeLayout.setVisibility(View.VISIBLE);
            codeLayout.setOnClickListener(null);
            codeLayout.setClickable(false);
            textView.setText(tip);
            textView.setTextColor(color);
        }

        /*展示对码按钮*/
        public void showCodeLayout(String btText) {
            infoLayout.setVisibility(View.GONE);
            codeLayout.setVisibility(View.VISIBLE);
            codeLayout.setOnClickListener(v -> {

                if (mValueHelper.canPairStart()) {
                    mValueHelper.checkFireStart(() -> {
                        //开始对码
                        if (position == TirePressureManager.R_1) {
                            mValueHelper.tpms6601RF(timeOutRunnable, pairOkRunnable);
                            curPairPosition = TirePressureManager.R_1;
                        } else if (position == TirePressureManager.L_1) {
                            mValueHelper.tpms6602LF(timeOutRunnable, pairOkRunnable);
                            curPairPosition = TirePressureManager.L_1;
                        } else if (position == TirePressureManager.R_2) {
                            mValueHelper.tpms6603RB(timeOutRunnable, pairOkRunnable);
                            curPairPosition = TirePressureManager.R_2;
                        } else if (position == TirePressureManager.L_2) {
                            mValueHelper.tpms6604LB(timeOutRunnable, pairOkRunnable);
                            curPairPosition = TirePressureManager.L_2;
                        } else {
                            curPairPosition = -1;
                        }

                        showTip("对码中...");
                        log.info("正在对码轮胎位置:{}", curPairPosition);
                    });
                } else {
                    VoiceManagerProxy.getInstance().startSpeaking("请等待上一个轮胎对码结束.", TTSType.TTS_DO_NOTHING, false);
                }
            });
            textView.setText(btText);
            textView.setTextColor(Color.WHITE);
        }

        /*展示信息布局*/
        private void showInfoLayout() {
            infoLayout.setVisibility(View.VISIBLE);
            codeLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 温度,范围信息布局
     */
    class ItemInfoMode {
        public ImageView mImageView;
        public TextView titleView;
        public TextView valueView;
        public View itemLayout;

        public ItemInfoMode(View itemLayout) {
            this.itemLayout = itemLayout;
            mImageView = (ImageView) itemLayout.findViewById(R.id.imageView);
            titleView = (TextView) itemLayout.findViewById(R.id.titleView);
            valueView = (TextView) itemLayout.findViewById(R.id.valueView);
        }
    }
}
