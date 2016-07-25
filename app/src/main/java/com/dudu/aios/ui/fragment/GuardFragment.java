package com.dudu.aios.ui.fragment;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dudu.aios.ui.fragment.base.BaseFragment;
import com.dudu.aios.ui.robbery.RobberyConstant;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.aios.ui.view.RobberyAnimView;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.Contacts;
import com.dudu.commonlib.event.Events;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.event.DeviceEvent;
import com.dudu.persistence.UserMessage.UserMessage;
import com.dudu.workflow.common.DataFlowFactory;
import com.dudu.workflow.common.RequestFactory;
import com.dudu.workflow.obd.CarLock;
import com.dudu.workflow.push.model.PushParams;
import com.dudu.workflow.push.model.ReceiverPushData;
import com.umeng.analytics.MobclickAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.greenrobot.event.EventBus;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class GuardFragment extends BaseFragment implements View.OnClickListener {

    private Logger logger = LoggerFactory.getLogger("car.GuardFragment");

    private Logger log_web = LoggerFactory.getLogger("network.SocketClient");

    private PublishSubject subject = PublishSubject.create();

    private View guard_unlock_layout, guard_locked_layout;

    private TextView tvTitleCh, tvTitleEn, tvLicensePrompt;

    private RelativeLayout animContainer;

    private RobberyAnimView animView;

    private boolean stopAnim = true;

    private Handler handler = new AnimHandler();

    private LinearLayout viewContainer, mLicenseContainer;

    private long mAuditStatus;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.vehicle_guard_layout, container, false);
        initView(view);
        initListener();
        initData();
        EventBus.getDefault().unregister(this);
        EventBus.getDefault().register(this);
        return view;
    }

    private void initListener() {
        guard_locked_layout.setOnClickListener(this);
        guard_unlock_layout.setOnClickListener(this);
    }

    private void initView(View view) {
        viewContainer = (LinearLayout) view.findViewById(R.id.view_container);
        mLicenseContainer = (LinearLayout) view.findViewById(R.id.license_view_container);
        guard_unlock_layout = view.findViewById(R.id.vehicle_unlock_layout);
        guard_locked_layout = view.findViewById(R.id.vehicle_locked_layout);
        animContainer = (RelativeLayout) view.findViewById(R.id.anim_container);
        tvLicensePrompt = (TextView) view.findViewById(R.id.license_prompt);
        tvTitleCh = (TextView) view.findViewById(R.id.text_title_ch);
        tvTitleCh.setText(getResources().getString(R.string.vehicle_guard_ch));
        tvTitleEn = (TextView) view.findViewById(R.id.text_title_en);
        tvTitleEn.setText(getResources().getString(R.string.vehicle_guard_en));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.vehicle_unlock_layout:
                //上锁
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_LOCK_GUARD.getEventId());
                actionLock();
                break;
            case R.id.vehicle_locked_layout:
                //解锁动作
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_UNLOCK_GUARD.getEventId());
                actionUnlock();
                break;
        }
    }

    private void actionLock() {
        logger.debug("actionLock");
        viewContainer.setVisibility(View.GONE);
        //播放动画
        toggleAnim();
        checkCarlock(true);
        showLockView();
        DataFlowFactory.getSwitchDataFlow()
                .saveGuardSwitch(true);
        //请求网络
        stopAnim = false;
        syncServerGuardSwitch(true);
    }

    private void actionUnlock() {
        logger.debug("actionUnlock");
        DataFlowFactory.getUserMessageFlow().obtainUserMessage().subscribe(new Action1<UserMessage>() {
            @Override
            public void call(UserMessage userMessage) {
                boolean gesturePasswordSwitchState = userMessage.isGesturePasswordSwitchState();
                boolean digitPasswordSwitchState = userMessage.isDigitPasswordSwitchState();
                logger.debug("获取本地手势密码的开关状态：" + gesturePasswordSwitchState);
                logger.debug("获取本地数字密码的开关状态：" + digitPasswordSwitchState);
                if (gesturePasswordSwitchState) {
                    transferParameters(true);
                } else if (digitPasswordSwitchState) {
                    transferParameters(false);
                } else {
                    checkCarlock(false);
                    showUnlockView();
                    DataFlowFactory.getSwitchDataFlow()
                            .saveGuardSwitch(false);
                    syncServerGuardSwitch(false);
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                logger.debug("获取密码状态失败：" + throwable.toString());
                transferParameters(true);
            }
        });

    }

    @Override
    public void onShow() {
        super.onShow();
        logger.debug("fragment is onShow()");
        Bundle bundle = FragmentConstants.TEMP_ARGS;
        if (bundle != null) {
            String pass = bundle.getString("pass");
            if (pass != null && pass.equals("1")) {
                logger.debug("initData");
                showGuardContainerView();
                checkCarlock(false);
                showUnlockView();
                DataFlowFactory.getSwitchDataFlow()
                        .saveGuardSwitch(false);
                syncServerGuardSwitch(false);
//                RobberyFlow.checkGunSwitch();
                return;
            }
        }

        queryGuardMessageDB();

    }

    private void initData() {
        logger.debug("fragment is initData()");
        queryGuardMessageDB();
    }

    private void transferParameters(boolean isGesturePassword) {
        Bundle bundle = new Bundle();
        bundle.putString(RobberyConstant.CATEGORY_CONSTANT, RobberyConstant.GUARD_CONSTANT);
        FragmentConstants.TEMP_ARGS = bundle;
        if (isGesturePassword) {
            replaceFragment(GestureFragment.class, R.id.vehicle_right_layout);
        } else {
            replaceFragment(VehiclePasswordSetFragment.class, R.id.vehicle_right_layout);
        }

    }

    private void showUnlockView() {
        logger.debug("unlockView");
        guard_locked_layout.setVisibility(View.GONE);
        guard_unlock_layout.setVisibility(View.VISIBLE);
        EventBus.getDefault().post(new Events.GuardSwitchState(false));
    }

    private void showLockView() {
        logger.debug("lockView");
        guard_locked_layout.setVisibility(View.VISIBLE);
        guard_unlock_layout.setVisibility(View.GONE);
        EventBus.getDefault().post(new Events.GuardSwitchState(true));
    }

    private void showLicenseViewContainer() {
        mLicenseContainer.setVisibility(View.VISIBLE);
        viewContainer.setVisibility(View.GONE);
        setLicensePromptMessage();

    }

    private void showGuardViewContainer() {
        viewContainer.setVisibility(View.VISIBLE);
        mLicenseContainer.setVisibility(View.GONE);

    }

    private void setLicensePromptMessage() {
        switch ((int) mAuditStatus) {
            case Contacts.AUDIT_STATE_NOT_APPROVE:
                tvLicensePrompt.setText(getResources().getString(R.string.driving_license_upload_prompt));
                break;
            case Contacts.AUDIT_STATE_AUDITING:
                tvLicensePrompt.setText(getResources().getString(R.string.driving_license_auditing_prompt));
                break;
            case Contacts.AUDIT_STATE_REJECT:
                tvLicensePrompt.setText(getResources().getString(R.string.driving_license_reject_prompt));
                break;
        }
    }

    private void toggleAnim() {
        animView = new RobberyAnimView(getActivity());
        animView.setZOrderOnTop(true);
        animView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        animContainer.addView(animView);
        animContainer.setVisibility(View.VISIBLE);
        animView.setOnAnimPlayListener(new RobberyAnimView.OnAnimPlayListener() {
            @Override
            public boolean play() {
                logger.debug("stopAnim:" + stopAnim);
                if (stopAnim) {
                    subject.onNext(stopAnim);
                }
                return stopAnim;
            }
        });
        subject.subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean o) {
                if (o) {
                    handler.sendEmptyMessage(0);
                }
            }
        }, throwable -> logger.error("subject", throwable));
    }

    private void clearAnim() {
        if (animContainer != null && animContainer.getChildCount() != 0) {
            animContainer.removeAllViews();
            if (animView != null) {
                animView.stopAnim();
                stopAnim = false;
                viewContainer.setVisibility(View.VISIBLE);
            }
        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        clearAnim();
    }

    private void checkGuardSwitch(boolean locked) {
        logger.debug("checkGuardSwitch:locked:" + locked);
        showGuardContainerView();
        if (locked) {
            showLockView();
        } else {
            showUnlockView();
        }
    }

    public void checkCarlock(boolean lock) {
        if (lock) {
            CarLock.guardLockCar();
        } else {
            CarLock.guardUnlockCar();
        }
    }

    private class AnimHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            clearAnim();
        }
    }

    @Override
    public void onHide() {
        super.onHide();
        logger.debug("fragment is onHide()");
        hideAllContainerView();
        clearAnim();
    }


    private void queryGuardMessageDB() {
        DataFlowFactory.getUserMessageFlow().obtainUserMessage().subscribe(new Action1<UserMessage>() {
            @Override
            public void call(UserMessage userMessage) {
                logger.debug("获取数据库防盗的信息：" + userMessage.toString());
                long auditStatus = userMessage.getAudit_state();
                mAuditStatus = auditStatus;
                if (auditStatus == 2) {
                    showGuardContainerView();
                    queryGuardSwitchDB();
                } else {
                    showLicenseShowContainerView();
                }
            }
        }, throwable -> log_web.debug("queryGuardMessageDB", throwable));
    }

    private void queryGuardSwitchDB() {
        logger.debug("开始数据库防盗开关的信息。。");
        DataFlowFactory.getSwitchDataFlow()
                .getGuardSwitch()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(locked -> {
                    checkGuardSwitch(locked);
                }, (error) -> {
                    logger.error("reflashViews", error);
                });
    }

    private void showLicenseShowContainerView() {
        mLicenseContainer.setVisibility(View.VISIBLE);
        viewContainer.setVisibility(View.GONE);
        showLicensePrompt();
    }

    private void showLicensePrompt() {
        switch (Integer.parseInt(String.valueOf(mAuditStatus))) {
            case Contacts.AUDIT_STATE_NOT_APPROVE:
                tvLicensePrompt.setText(getResources().getString(R.string.driving_license_upload_prompt));
                break;
            case Contacts.AUDIT_STATE_AUDITING:
                tvLicensePrompt.setText(getResources().getString(R.string.driving_license_auditing_prompt));
                break;
            case Contacts.AUDIT_STATE_REJECT:
                tvLicensePrompt.setText(getResources().getString(R.string.driving_license_reject_prompt));
                break;
        }
    }

    private void showGuardContainerView() {
        viewContainer.setVisibility(View.VISIBLE);
        mLicenseContainer.setVisibility(View.GONE);

    }

    private void hideAllContainerView() {
        viewContainer.setVisibility(View.GONE);
        mLicenseContainer.setVisibility(View.GONE);
    }


    private void syncServerGuardSwitch(boolean isOpen) {
        logger.debug(isOpen ? "lockGuard" : "unlockGuard");
        RequestFactory.getGuardRequest()
                .checkLockCar(isOpen)
                .subscribe(setGuardStateResponse -> {
                    if (setGuardStateResponse.resultCode == 0) {
                        logger.debug(isOpen ? "加锁成功" : "解锁成功");
                    } else {
                        logger.debug(setGuardStateResponse.resultMsg);
                    }
                    stopAnim = true;
                }, throwable -> {
                    logger.error(isOpen ? "lockGuard" : "unlockGuard" + " getError", throwable);
                    stopAnim = true;
                });
    }

    public void onEventMainThread(ReceiverPushData receiverData) {
        log_web.debug("防盗的页面接受到修改UI状态的命令");
        if (receiverData != null && receiverData.result != null) {
            String method = receiverData.result.method;
            if (receiverData.resultCode == 0 && method != null) {
                if (method.equals(PushParams.GUARD_STATE)) {
                    String guardSwitch = receiverData.result.thiefSwitchState;
                    logger.debug("防盗的状态：" + guardSwitch);
                    if (guardSwitch.equals("0")) {
                        showUnlockView();
                    } else {
                        showLockView();
                    }
                }
            }
        }
    }


    public void onEventMainThread(DeviceEvent.SafetyMainFragmentBack event) {
        logger.debug("收到点击安全中心的返回键的事件");
        hideAllContainerView();
        clearAnim();
    }
}
