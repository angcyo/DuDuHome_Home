package com.dudu.aios.ui.fragment.tire;

import com.dudu.aios.ui.fragment.base.RBaseFragment;
import com.dudu.android.launcher.R;
import com.dudu.monitor.tirepressure.TirePressureManager;
import com.dudu.navi.event.NaviEvent;
import com.dudu.workflow.tpms.TPMSInfo;

import org.slf4j.Logger;

import de.greenrobot.event.EventBus;

/**
 * Created by robi on 2016-06-28 11:01.
 */
public class TireFragment extends RBaseFragment implements TirePressureManager.ITireDataListener, TirePressureManager.ITireExceptionListener, TireDataPull.ITireDataChangeListener {

    /**
     * 异常界面是否显示过, 一次周期内(点火周期, 异常为改变周期),不显示第二次
     */
    private static boolean isExceptionShow = false;
    protected Logger log = TirePressureManager.log;
    TireViewMode mTireViewMode;

    /**
     * 设置异常是否显示过
     *
     * @param exceptionShow 是否显示过
     */
    public static void setExceptionShow(boolean exceptionShow) {
        isExceptionShow = exceptionShow;
    }

    public static boolean isExceptionShow() {
        return isExceptionShow;
    }

    @Override
    protected int getContentView() {
        return R.layout.fragment_tire_layout;
    }

    @Override
    protected void initViewData() {
        mTireViewMode = new TireViewMode(this);
        mTireViewMode.initViewMode(mViewHolder);

        EventBus.getDefault().unregister(this);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void initEvent() {
        mViewHolder.v(R.id.button_back).setOnClickListener(v -> {
            mTireViewMode.cancelPair();
            mBaseActivity.showMain();
        });
    }

    public void onEventMainThread(NaviEvent.ToMainFragmentEvent event) {
        mTireViewMode.cancelPair();
    }

    @Override
    public void onAdd() {
        super.onAdd();
        log.debug("TireFragment onAdd");
        addTireListener();

        TireFragment.setExceptionShow(true);
    }

    @Override
    public void onShow() {
        super.onShow();
        log.debug("TireFragment onShow");

        addTireListener();
        mTireViewMode.onShow();

        TireFragment.setExceptionShow(true);
    }

    @Override
    public void onHide() {
        super.onHide();
        log.debug("TireFragment onHide");

        removeTireListener();
        mTireViewMode.onHide();
    }

    /**
     * 添加胎压事件监听
     */
    private void addTireListener() {
        //查询轮胎信息
        TirePressureManager.instance().addTireDataListener(this);//数据监听,包括数据是否更改
        TirePressureManager.instance().addTireExceptionListener(this);//数据异常监听,某个轮胎异常了等
        TirePressureManager.instance().queryTireDatas(true);//手动触发一次查询

        TireDataPull.instance().addTireDataChangeListener(this);
    }

    /**
     * 移出事件监听
     */
    private void removeTireListener() {
        TirePressureManager.instance().removeTireDataListener(this);
        TirePressureManager.instance().removeTireExceptionListener(this);

        TireDataPull.instance().removeTireDataChangeListener(this);
    }

    @Override
    public void onTireData(boolean change1, boolean change2, boolean change3, boolean change4, TPMSInfo info1, TPMSInfo info2, TPMSInfo info3, TPMSInfo info4) {
        if (info1 == null && info2 == null && info3 == null && info4 == null) {
            log.debug("收到-->无胎压数据,是否数据改变:{}", change1 || change2 || change3 || change4);
        } else {
            log.debug("收到-->轮胎数据,是否数据改变:{}", change1 || change2 || change3 || change4);
        }
        mTireViewMode.setTireData(change1, change2, change3, change4, info1, info2, info3, info4);
    }

    @Override
    public void onTireException(int position, TPMSInfo info) {
        mTireViewMode.setTireException(position, info);
    }

    @Override
    public void onTireDataChange() {
        mTireViewMode.updateModeInfo();//更新预警值布局
//        TirePressureManager.instance().queryTireDatas(true);//触发查询
        TirePressureManager.instance().notifyTireDataChange(true, true, true, true);//预警值改变了,强制更新轮胎信息.(是否异常)
    }
}
