package com.dudu.aios.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dudu.aios.ui.fragment.base.BaseFragment;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.Contacts;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.event.DeviceEvent;
import com.dudu.monitor.utils.SharedPreferencesUtil;
import com.dudu.workflow.common.DataFlowFactory;
import com.dudu.workflow.obd.VehicleConstants;
import com.umeng.analytics.MobclickAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.greenrobot.event.EventBus;

public class SafetyMainFragment extends BaseFragment implements View.OnClickListener {

    private LinearLayout vehicle_guard_btn, vehicle_robbery_btn;

    private ImageButton buttonBack;

    private View view;

    private Logger logger = LoggerFactory.getLogger("SafetyFragment");

    private boolean showGuardFragment = true;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.prevent_rob_layout, container, false);
        initView(view);
        initOnListener();
        initFragments();
        logger.debug("initView()...");
        return view;
    }

    private void initOnListener() {
        vehicle_guard_btn.setOnClickListener(this);
        vehicle_robbery_btn.setOnClickListener(this);
        buttonBack.setOnClickListener(this);
    }

    private void initView(View view) {
        vehicle_guard_btn = (LinearLayout) view.findViewById(R.id.vehicle_guard_btn);
        vehicle_robbery_btn = (LinearLayout) view.findViewById(R.id.vehicle_robbery_btn);
        buttonBack = (ImageButton) view.findViewById(R.id.vehicle_back_button);
    }

    private void initFragments() {
        initFragment(GuardFragment.class, R.id.vehicle_right_layout);
        initFragment(RobberyMainFragment.class, R.id.vehicle_right_layout);
        showGuardOrRobberyFragment();
    }

    private void showGuardOrRobberyFragment(){
        if (showGuardFragment) {
            actionGuard(new Bundle());
        } else {
            actionRobbery(new Bundle());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onAdd() {
        super.onAdd();

    }

    @Override
    public void onShow() {
        super.onShow();
        logger.debug("fragment is show()");
        Bundle bundle = FragmentConstants.TEMP_ARGS;
        if (bundle != null) {
            Bundle args = new Bundle();
            int fragmentType = bundle.getInt(VehicleConstants.SHOW_GUARD_OR_ROBBERY, Contacts.SHOW_INIT_FRAGMENT);
            if (fragmentType == Contacts.SHOW_GUARD_FRAGMENT) {
                args.putBoolean(VehicleConstants.UNLOCK_GUARD, bundle.getBoolean(VehicleConstants.UNLOCK_GUARD, false));
                actionGuard(args);
                return;
            } else if (fragmentType == Contacts.SHOW_ROBBERY_FRAGMENT) {
                args.putBoolean(VehicleConstants.OPEN_ROBBERY, bundle.getBoolean(VehicleConstants.OPEN_ROBBERY, false));
                actionRobbery(args);
                return;
            }
            showGuardOrRobberyFragment();
        }
    }

    @Override
    public void onHide() {
        super.onHide();
        logger.debug("fragment is onHide()");
    }

    private void queryAuditStateDB() {
        boolean hasBinded = SharedPreferencesUtil.getBooleanValue(Contacts.BINDING_STATE, false);
        if (hasBinded) {
            DataFlowFactory.getUserMessageFlow().obtainUserMessage()
                    .map(userMessage -> userMessage.getAudit_state())
                    .filter(audit_state -> audit_state != 2)
                    .subscribe(auditState -> {
                        logger.debug("查询数据库的审核状态：" + auditState);
                        //showBindingFragments();
                        showLicensePromptFragment();
                    }, throwable -> logger.error("queryAuditStateDB", throwable));

        } else {
            showBindingFragments();
        }
    }

    private void showLicensePromptFragment() {
        SharedPreferencesUtil.putLongValue(Contacts.LICENSE_TYPE, Contacts.DRIVING_TYPE);
        replaceFragment(FragmentConstants.LICENSE_UPLOAD_UPLOAD_FRAGMENT);
    }

    protected void showBindingFragments() {
        replaceFragment(FragmentConstants.FRAGMENT_DEVICE_BINDING);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.vehicle_guard_btn:
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_GUARD.getEventId());
                actionGuard(new Bundle());
                break;
            case R.id.vehicle_robbery_btn:
                MobclickAgent.onEvent(getContext(), ClickEvent.CLICK_ROBBERY.getEventId());
                actionRobbery(new Bundle());
                break;
            case R.id.vehicle_back_button:
                EventBus.getDefault().post(new DeviceEvent.SafetyMainFragmentBack());
                replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
                break;
        }
    }

    private void actionRobbery(Bundle bundle) {
        vehicle_robbery_btn.setEnabled(false);
        vehicle_guard_btn.setEnabled(true);
        showGuardFragment = false;
        robbery();
        FragmentConstants.TEMP_ARGS = bundle;
        showFragment(RobberyMainFragment.class, R.id.vehicle_right_layout);
    }

    private void actionGuard(Bundle bundle) {
        vehicle_guard_btn.setEnabled(false);
        vehicle_robbery_btn.setEnabled(true);
        showGuardFragment = true;
        guard();
        FragmentConstants.TEMP_ARGS = bundle;
        showFragment(GuardFragment.class, R.id.vehicle_right_layout);
    }

    private void robbery() {
        ((ImageView) view.findViewById(R.id.vehicle_guard_icon)).setImageResource(R.drawable.vehicle_guard_normal);
        ((TextView) view.findViewById(R.id.text_vehicle_guard_ch)).setTextColor(getResources().getColor(R.color.unchecked_textColor));
        ((TextView) view.findViewById(R.id.text_vehicle_guard_en)).setTextColor(getResources().getColor(R.color.unchecked_textColor));

        ((ImageView) view.findViewById(R.id.vehicle_robbery_icon)).setImageResource(R.drawable.vehicle_robbery_clicked_icon);
        ((TextView) view.findViewById(R.id.text_vehicle_robbery_ch)).setTextColor(getResources().getColor(R.color.white));
        ((TextView) view.findViewById(R.id.text_vehicle_robbery_en)).setTextColor(getResources().getColor(R.color.white));
    }

    private void guard() {
        ((ImageView) view.findViewById(R.id.vehicle_guard_icon)).setImageResource(R.drawable.vehicle_guard_clicked);
        ((TextView) view.findViewById(R.id.text_vehicle_guard_ch)).setTextColor(getResources().getColor(R.color.white));
        ((TextView) view.findViewById(R.id.text_vehicle_guard_en)).setTextColor(getResources().getColor(R.color.white));

        ((ImageView) view.findViewById(R.id.vehicle_robbery_icon)).setImageResource(R.drawable.vehicle_robbery_normal);
        ((TextView) view.findViewById(R.id.text_vehicle_robbery_ch)).setTextColor(getResources().getColor(R.color.unchecked_textColor));
        ((TextView) view.findViewById(R.id.text_vehicle_robbery_en)).setTextColor(getResources().getColor(R.color.unchecked_textColor));

    }
}
