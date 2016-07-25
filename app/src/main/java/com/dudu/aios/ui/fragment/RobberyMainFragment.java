package com.dudu.aios.ui.fragment;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dudu.aios.ui.fragment.base.BaseFragment;
import com.dudu.aios.ui.robbery.RobberyConstant;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.Contacts;
import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.commonlib.utils.File.SharedPreferencesUtil;
import com.dudu.commonlib.utils.TextVerify;
import com.dudu.persistence.RobberyMessage.RobberyMessage;
import com.dudu.persistence.UserMessage.UserMessage;
import com.dudu.rest.model.common.RequestResponse;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.workflow.common.DataFlowFactory;
import com.dudu.workflow.common.RequestFactory;
import com.dudu.workflow.obd.CarLock;
import com.dudu.workflow.push.model.PushParams;
import com.dudu.workflow.push.model.ReceiverPushData;
import com.dudu.workflow.robbery.RobberyStateModel;
import com.umeng.analytics.MobclickAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.greenrobot.event.EventBus;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class RobberyMainFragment extends BaseFragment implements View.OnClickListener {

    private Logger logger = LoggerFactory.getLogger("RobberyMainFragment");

    private Logger log_web = LoggerFactory.getLogger("SocketClient");

    private View view;

    private ImageView robbery_mode_switch_off_img, robbery_mode_switch_on_img;

    private TextView tvRobberySwitchPrompt, tvLicensePrompt;

    private RobberyMessage mRobberyMessage;

    private LinearLayout viewContainer, mLicenseContainer;

    private LinearLayout robberyModeContainer;

    private FrameLayout robberyLockContainer;

    private LinearLayout robberyLockView, robberyUnlockView;

    private long mAuditStatus;

    @Override

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_robbery_main, container, false);
        initView();
        initListener();
//        initData();
        EventBus.getDefault().unregister(this);
        EventBus.getDefault().register(this);
        return view;
    }


    private void initListener() {
        robbery_mode_switch_off_img.setOnClickListener(this);
        robbery_mode_switch_on_img.setOnClickListener(this);
        robberyLockView.setOnClickListener(this);
        robberyUnlockView.setOnClickListener(this);
    }

    private void initView() {
        robbery_mode_switch_off_img = (ImageView) view.findViewById(R.id.robbery_switch_off_img);
        robbery_mode_switch_on_img = (ImageView) view.findViewById(R.id.robbery_switch_on_img);
        tvRobberySwitchPrompt = (TextView) view.findViewById(R.id.robbery_switch_prompt);
        viewContainer = (LinearLayout) view.findViewById(R.id.robbery_view_container);
        mLicenseContainer = (LinearLayout) view.findViewById(R.id.license_show_container);
        tvLicensePrompt = (TextView) view.findViewById(R.id.license_prompt);
        robberyLockView = (LinearLayout) view.findViewById(R.id.robbery_locked_layout);
        robberyUnlockView = (LinearLayout) view.findViewById(R.id.robbery_unlock_layout);
        robberyLockContainer = (FrameLayout) view.findViewById(R.id.robbery_lock_container);
        robberyModeContainer = (LinearLayout) view.findViewById(R.id.robbery_mode_container);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.robbery_switch_off_img:
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_SWITCH_OFF.getEventId());
                actionRobberyModeLock();
                //saveSwitch(CommonParams.GUN, true);
                break;
            case R.id.robbery_switch_on_img:
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_SWITCH_ON.getEventId());
                actionRobberyModeUnlock();
                break;

            case R.id.robbery_locked_layout:
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_UNLOCK_ROBBERY.getEventId());
                actionRobberyUnlock();
                break;

        }
    }

    @Override
    public void onHide() {
        super.onHide();
        hideAllContainerView();
    }

    private void actionRobberyLock() {
        logger.debug("action is lock");
        DataFlowFactory.getRobberyMessageFlow().saveRobberyTriggerStatus(true);
    }

    private void actionRobberyUnlock() {
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
                    mRobberyMessage.setRobberTrigger(false);
                    syncRobberySwitchView(mRobberyMessage);
                    DataFlowFactory.getRobberyMessageFlow().saveRobberyTriggerStatus(false);
                    requestRobberySwitch(false, RobberyConstant.ROBBERY_TRIGGER_TYPE);
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

    private void transferParameters(boolean isGesturePassword) {
        Bundle bundle = new Bundle();
        bundle.putString(RobberyConstant.CATEGORY_CONSTANT, RobberyConstant.ROBBERY_CONSTANT);
        FragmentConstants.TEMP_ARGS = bundle;
        if (isGesturePassword) {
            replaceFragment(GestureFragment.class, R.id.vehicle_right_layout);
        } else {
            replaceFragment(VehiclePasswordSetFragment.class, R.id.vehicle_right_layout);
        }

    }

    private void actionRobberyModeLock() {
        if (mRobberyMessage != null) {
            if (mRobberyMessage.getOperationNumber() == null && mRobberyMessage.getCompleteTime() == null && mRobberyMessage.getRotatingSpeed() == null) {
                VoiceManagerProxy.getInstance().startSpeaking(
                        CommonLib.getInstance().getContext().getString(R.string.robbery_switch_close_prompt), TTSType.TTS_DO_NOTHING, false);
                return;
            }
            showRobberyModeLockView();
            if (mRobberyMessage != null) {
                setRobberyModeLockPrompt(tvRobberySwitchPrompt, mRobberyMessage.getCompleteTime(), mRobberyMessage.getOperationNumber(), mRobberyMessage.getRotatingSpeed());
            }

            DataFlowFactory.getRobberyMessageFlow().saveRobberySwitch(true);
            requestRobberySwitch(true, RobberyConstant.ROBBERY_SWITCH_TYPE);
        }
    }

    private void actionRobberyModeUnlock() {
        showRobberyModeUnLockView();
        DataFlowFactory.getRobberyMessageFlow().saveRobberySwitch(false);
        requestRobberySwitch(false, RobberyConstant.ROBBERY_SWITCH_TYPE);
    }

    @Override
    public void onShow() {
        logger.debug("fragment is onShow()");
        Bundle bundle = FragmentConstants.TEMP_ARGS;
        if (bundle != null) {
            String pass = bundle.getString("pass");
            if (pass != null && pass.equals("1")) {
                logger.debug("initData()");
                queryRobberyMessageDB();
                showRobberyContainerView();
                DataFlowFactory.getRobberyMessageFlow().saveRobberyTriggerStatus(false);
                showRobberyModeContainerView();
                checkCarlock(false);
                requestRobberySwitch(false, RobberyConstant.ROBBERY_TRIGGER_TYPE);
                return;
            }
        }

        queryAuditStatus();

    }

    private void setRobberyModeLockPrompt(TextView tvRobberySwitchPrompt, String completeTime, String operationNumber, String rotatingSpeed) {
        String format = getResources().getString(R.string.robbery_switch_open_prompt);
        String openPrompt = String.format(format, completeTime, operationNumber, rotatingSpeed);
        if (completeTime != null && operationNumber != null && rotatingSpeed != null) {
            setRobberyLockPrompt(openPrompt, tvRobberySwitchPrompt, completeTime.length(), operationNumber.length(), rotatingSpeed.length());
        }
    }

    private void showRobberyModeLockView() {
        robbery_mode_switch_on_img.setVisibility(View.VISIBLE);
        robbery_mode_switch_off_img.setVisibility(View.GONE);
        tvRobberySwitchPrompt.setText("");
    }

    private void showRobberyModeUnLockView() {
        robbery_mode_switch_on_img.setVisibility(View.GONE);
        robbery_mode_switch_off_img.setVisibility(View.VISIBLE);
        tvRobberySwitchPrompt.setText(getResources().getString(R.string.robbery_switch_close_prompt));
    }


    private void showRobberyModeContainerView() {
        robberyModeContainer.setVisibility(View.VISIBLE);
        robberyLockContainer.setVisibility(View.GONE);
    }

    private void showRobberyLockContainerView() {
        robberyLockContainer.setVisibility(View.VISIBLE);
        robberyModeContainer.setVisibility(View.GONE);
    }

    private void showLockView() {
        robberyLockView.setVisibility(View.VISIBLE);
        robberyUnlockView.setVisibility(View.GONE);
    }

    private void showUnlockView() {
        robberyUnlockView.setVisibility(View.VISIBLE);
        robberyLockView.setVisibility(View.GONE);
    }

    private void showRobberyContainerView() {
        viewContainer.setVisibility(View.VISIBLE);
        mLicenseContainer.setVisibility(View.GONE);
    }

    private void showLicenseShowContainerView() {
        mLicenseContainer.setVisibility(View.VISIBLE);
        viewContainer.setVisibility(View.GONE);
        showLicensePrompt();
    }

    private void hideAllContainerView() {
        viewContainer.setVisibility(View.GONE);
        mLicenseContainer.setVisibility(View.GONE);

    }

    private void setRobberyLockPrompt(String openPrompt, TextView tvRobberySwitchPrompt, int paramsOneSize, int paramTwoSize, int paramThreeSize) {

        int startParamOneIndex = 1;
        int endParamOneIndex = 1 + paramsOneSize;
        int startParamTwoIndex = 6 + paramsOneSize;
        int endParamTwoIndex = 6 + paramsOneSize + paramTwoSize;
        int startParamThreeIndex = 14 + paramsOneSize + paramTwoSize;
        int endParamThreeIndex = 14 + paramsOneSize + paramTwoSize + paramThreeSize;

        SpannableString spanString = new SpannableString(openPrompt);

        ForegroundColorSpan colorSpan = new ForegroundColorSpan(getResources().getColor(R.color.blue));
        spanString.setSpan(colorSpan, startParamOneIndex, endParamOneIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ForegroundColorSpan colorSpan1 = new ForegroundColorSpan(getResources().getColor(R.color.blue));
        spanString.setSpan(colorSpan1, startParamTwoIndex, endParamTwoIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ForegroundColorSpan colorSpan2 = new ForegroundColorSpan(getResources().getColor(R.color.blue));
        spanString.setSpan(colorSpan2, startParamThreeIndex, endParamThreeIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        UnderlineSpan strikeSpan = new UnderlineSpan();
        spanString.setSpan(strikeSpan, startParamOneIndex, endParamOneIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        UnderlineSpan strikeSpan1 = new UnderlineSpan();
        spanString.setSpan(strikeSpan1, startParamTwoIndex, endParamTwoIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        UnderlineSpan strikeSpan2 = new UnderlineSpan();
        spanString.setSpan(strikeSpan2, startParamThreeIndex, endParamThreeIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        AbsoluteSizeSpan absoluteSizeSpan = new AbsoluteSizeSpan(36);
        spanString.setSpan(absoluteSizeSpan, startParamOneIndex, endParamOneIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        AbsoluteSizeSpan absoluteSizeSpan1 = new AbsoluteSizeSpan(36);
        spanString.setSpan(absoluteSizeSpan1, startParamTwoIndex, endParamTwoIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        AbsoluteSizeSpan absoluteSizeSpan2 = new AbsoluteSizeSpan(36);
        spanString.setSpan(absoluteSizeSpan2, startParamThreeIndex, endParamThreeIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvRobberySwitchPrompt.append(spanString);
    }

    private void syncRobberySwitchView(RobberyMessage robberyMessage) {
        if (robberyMessage.isRobberTrigger()) {
            showRobberyContainerView();
            showRobberyLockContainerView();
            showLockView();
        } else {
            showRobberyContainerView();
            showRobberyModeContainerView();
            if (robberyMessage.isRobberySwitch()) {
                showRobberyModeLockView();
                setRobberyModeLockPrompt(tvRobberySwitchPrompt, robberyMessage.getCompleteTime(), robberyMessage.getOperationNumber(), robberyMessage.getRotatingSpeed());
            } else {
                showRobberyModeUnLockView();
            }
        }
    }

    private void requestRobberySwitch(boolean isOpen, int robberyType) {
        if (mRobberyMessage == null) {
            mRobberyMessage = new RobberyMessage();
        }
        if (robberyType == RobberyConstant.ROBBERY_SWITCH_TYPE) {
            mRobberyMessage.setRobberySwitch(isOpen);
        } else if (robberyType == RobberyConstant.ROBBERY_TRIGGER_TYPE) {
            mRobberyMessage.setRobberTrigger(isOpen);
        }
        logger.debug("请求的参数：" + mRobberyMessage.toString());

        RequestFactory.getRobberyRequest().settingAntiRobberyMode(mRobberyMessage).subscribe(new Action1<RequestResponse>() {
            @Override
            public void call(RequestResponse requestResponse) {
                if (requestResponse.resultCode == 0) {
                    logger.debug("请求网络：" + requestResponse.resultMsg);
                } else {
                    logger.debug("请求网络：" + requestResponse.resultMsg + "   " + requestResponse.resultCode);
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                logger.debug("请求网络失败：" + throwable);
            }
        });
    }


    public void checkCarlock(boolean lock) {
        if (lock) {
            CarLock.robberyLockCar();
        } else {
            CarLock.robbertUnlockCar();
        }

    }

    private void queryAuditStatus() {
        long auditStatus = SharedPreferencesUtil.getLongValue(getActivity(), Contacts.AUDIT_STATE, -1);
        logger.debug("获取数据库防劫的审核状态：" + auditStatus);
        mAuditStatus = auditStatus;
        if (auditStatus == 2) {
            queryRobberyMessageDB();
        } else {
            showLicenseShowContainerView();
        }
    }


    private void queryRobberyMessageDB() {
        DataFlowFactory.getRobberyMessageFlow().obtainRobberyMessage().subscribe(new Action1<RobberyMessage>() {
            @Override
            public void call(RobberyMessage robberyMessage) {
                logger.debug("获取数据库防劫信息成功" + robberyMessage.toString());
                mRobberyMessage = robberyMessage;
                syncRobberySwitchView(robberyMessage);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                logger.debug("获取防劫信息失败", throwable);
            }
        });
    }


    private void showLicensePrompt() {
        switch ((int) mAuditStatus) {
            case Contacts.AUDIT_STATE_NOT_APPROVE:
                tvLicensePrompt.setText(getResources().getString(R.string.insurance_license_upload_prompt));
                break;
            case Contacts.AUDIT_STATE_AUDITING:
                tvLicensePrompt.setText(getResources().getString(R.string.insurance_license_auditing_prompt));
                break;
            case Contacts.AUDIT_STATE_REJECT:
                tvLicensePrompt.setText(getResources().getString(R.string.insurance_license_reject_prompt));
                break;
        }
    }

    public void onEventMainThread(RobberyStateModel event) {
        if (event.getRobberyState() == RobberyStateModel.ROBBERY_IS_TRIGGERED) {
            logger.debug("收到防劫模式触发事件:" + event.getRobberyState());
            DataFlowFactory.getRobberyMessageFlow().obtainRobberyMessage()
                    .map(robberyMessage -> robberyMessage.isRobberySwitch())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(switchIsOn -> {
                        logger.debug("查询本地是否打开了防3次踩油门:" + switchIsOn);
                        if (switchIsOn) {
                            showRobberyLockContainerView();
                            showLockView();
                        }
                    }, (error) -> {
                        logger.error("收到防劫模式触发事件:" + event.getRobberyState(), error);
                    });
        }
    }

    public void onEventMainThread(ReceiverPushData receiverData) {
        log_web.debug("防劫的页面接受到修改UI状态的命令");
        if (receiverData != null && receiverData.result != null) {
            String method = receiverData.result.method;
            logger.debug("推送的数据：method:" + method);
            if (receiverData.resultCode == 0 && method != null) {
                if (method.equals(PushParams.ROBBERY_STATE)) {
                    String thiefSwitchState = receiverData.result.robberySwitchs;
                    String operationNumber = receiverData.result.numberOfOperations;
                    String completeTime = receiverData.result.completeTime;
                    String rotatingSpeed = receiverData.result.revolutions;
                    String robberyTrigger = receiverData.result.protectRobTriggerSwitchState;
                    logger.debug("推送的数据：防劫的状态:" + thiefSwitchState);
                    if (!TextVerify.isEmpty(thiefSwitchState) && !TextVerify.isEmpty(operationNumber) && !TextVerify.isEmpty(completeTime) && !TextVerify.isEmpty(rotatingSpeed)) {
                        RobberyMessage robberyMessage = new RobberyMessage();
                        robberyMessage.setObied(CommonLib.getInstance().getObeId());
                        robberyMessage.setRobberySwitch(thiefSwitchState.endsWith("1") ? true : false);
                        robberyMessage.setOperationNumber(operationNumber);
                        robberyMessage.setRotatingSpeed(rotatingSpeed);
                        robberyMessage.setCompleteTime(completeTime);
                        mRobberyMessage = robberyMessage;
                        syncRobberySwitchView(robberyMessage);
                    }
                    if (robberyTrigger != null) {
                        if ("0".equals(robberyTrigger)) {
                            mRobberyMessage.setRobberTrigger(false);
                            syncRobberySwitchView(mRobberyMessage);
                        }
                    }
                }
            }
        }
    }


}
