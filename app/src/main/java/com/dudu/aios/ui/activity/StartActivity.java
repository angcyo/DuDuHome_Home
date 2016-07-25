package com.dudu.aios.ui.activity;

import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.dudu.aios.ui.base.VolBrightnessSetting;
import com.dudu.aios.ui.dialog.DisclaimerDialog;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.ActivitiesManager;
import com.dudu.android.launcher.utils.DialogUtils;
import com.dudu.commonlib.utils.File.KeyConstants;
import com.dudu.commonlib.utils.File.SharedPreferencesUtil;

public class StartActivity extends FragmentActivity implements View.OnClickListener, DisclaimerDialog.OnConfirmListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        iniView();
        initListener();
        initData();
        //开机默认设置为自动亮度调节，手势一旦调节则切换为手动模式
        VolBrightnessSetting.setScreenMode(this, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        if(SharedPreferencesUtil.getBooleanValue(this,KeyConstants.KEY_EGREED_DISCLAIMER,false)){
            KeyConstants.is_agree_disclaimer = true;
            ActivitiesManager.toMainActivity();
            finish();
        }else{
            DialogUtils.showDisclaimerDialog(StartActivity.this);
            DialogUtils.setDisclaimerConfirmListener(this,this);
        }
    }


    private void initData() {

    }


    private void initListener() {

    }

    private void iniView() {

    }

    @Override
    public void onClick(View v) {

    }

    public void onStartActivity(View view) {
        //ActivitiesManager.toMainActivity();
        //finish();
    }

    @Override
    public void onConfirm(boolean noneed_remind) {
        if(noneed_remind){
            SharedPreferencesUtil.putBooleanValue(this, KeyConstants.KEY_EGREED_DISCLAIMER,true);
        }
        ActivitiesManager.toMainActivity();
        KeyConstants.is_agree_disclaimer = true;
        finish();
    }
}
