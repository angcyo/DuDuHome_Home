package com.dudu.aios.ui.fragment.Coolant;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.dudu.aios.ui.fragment.base.RBaseFragment;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.R;
import com.dudu.monitor.obd.CoolantTemperatureManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 水温告警页面
 *
 * Created by Robert on 2016/7/4.
 */
public class WaterWarningFragment extends RBaseFragment implements View.OnTouchListener{

    private Logger log = LoggerFactory.getLogger("WaterWarningFragment");

    private ImageView ivWaterWarning = null;

    @Override
    protected int getContentView() {

        log.info("hibox getContentView");
        return R.layout.fragment_water_warning_layout;
    }

    @Override
    protected void initViewData() {

        log.info("hibox initViewData");
    }

    @Override
    protected void initView(View rootView) {

        rootView.setOnTouchListener(this);
        ivWaterWarning = (ImageView) mViewHolder.v(R.id.id_iv_water_warning);
        log.info("hibox initView");
        super.initView(rootView);
    }

    @Override
    protected void initEvent() {
        super.initEvent();

        log.info("hibox initEvent");


        startFlashAnimation();
    }

    /*开始闪烁动画*/
    public void startFlashAnimation()
    {
        log.info("hibox startFlashAnimation");
        AlphaAnimation flashAnimation = new AlphaAnimation(0.3f, 1f);
        flashAnimation.setDuration(200);
        flashAnimation.setInterpolator(new AccelerateInterpolator());
        flashAnimation.setRepeatCount(Animation.INFINITE);
        flashAnimation.setRepeatMode(Animation.REVERSE);
        ivWaterWarning.setAnimation(flashAnimation);
        flashAnimation.start();
    }

    /*停止动画*/
    public void stopFlashAnimation()
    {
        log.info("hibox stopFlashAnimation");
        ivWaterWarning.clearAnimation();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        log.info("hibox -> onTouch = {}",event.getAction());
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);

                //记录告警弹窗时间
                final long currentTime = System.currentTimeMillis();
                CoolantTemperatureManager.getInstance().setLastHighTime(currentTime);

                break;
            case MotionEvent.ACTION_UP:

                break;
            case MotionEvent.ACTION_MOVE:
                break;
            default:
                break;
        }

        return true;
    }


    @Override
    public void onShow() {
        super.onShow();

        log.info("hibox onShow");
        startFlashAnimation();
    }

    @Override
    public void onHide() {
        super.onHide();

        log.info("hibox onHide");
        stopFlashAnimation();
    }

    @Override
    public void onAdd() {
        super.onAdd();

        log.info("hibox onAdd");
//        startFlashAnimation();
    }
}
