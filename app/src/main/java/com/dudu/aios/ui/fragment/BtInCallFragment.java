package com.dudu.aios.ui.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.dudu.aios.ui.activity.MainRecordActivity;
import com.dudu.aios.ui.bt.Contact;
import com.dudu.aios.ui.fragment.base.RBaseFragment;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.BtPhoneUtils;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.android.launcher.utils.cache.AsyncTask;
import com.dudu.drivevideo.frontcamera.FrontCameraManage;
import com.dudu.voice.FloatWindowUtils;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voip.VoipSDKCoreHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2016/5/11.
 */
public class BtInCallFragment extends RBaseFragment implements View.OnClickListener {
    private Button mAcceptButton, mDropButton;

    private ImageButton mBackButton;

    private TextView mCallerName, mCallerNumber;
    private Logger logger = LoggerFactory.getLogger("phone.BtInCallFragment");
    @Override
    protected int getContentView() {
        return R.layout.activity_blue_tooth_caller;
    }

    @Override
    protected void initViewData() {
        initView();
        initListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        logger.debug("onResume()");
        LoggerFactory.getLogger("video1.frontdrivevideo").debug("设置模糊状态：{}", true);
        FrontCameraManage.getInstance().setPreviewBlur(true);
        initData();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onShow() {
        super.onShow();
        logger.debug("onShow()");
        LoggerFactory.getLogger("video1.frontdrivevideo").debug("设置模糊状态：{}", true);
        FrontCameraManage.getInstance().setPreviewBlur(true);
        initData();
    }

    @Override
    public void onHide() {
        super.onHide();
    }

    private void initData() {
        logger.debug("BtPhoneUtils.connectionState:"+BtPhoneUtils.connectionState + ",BtPhoneUtils.btCallState:"+BtPhoneUtils.btCallState +
        ",VoipSDKCoreHelper.getInstance().eccall_state:"+VoipSDKCoreHelper.getInstance().eccall_state);
        mCallerNumber.setText("");
        if(BtPhoneUtils.btCallState==BtPhoneUtils.CALL_STATE_INCOMING ||
                BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ACTIVE){

            FloatWindowUtils.getInstance().removeFloatWindow();
            //暂停语音
            myHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_INCOMING ||
                            BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ACTIVE){
                        logger.debug("停止语音唤醒");
                        VoiceManagerProxy.getInstance().stopWakeup();
                    }
                }
            },2000);
        }

        if (FragmentConstants.TEMP_ARGS != null && BtPhoneUtils.btCallState==BtPhoneUtils.CALL_STATE_INCOMING) {
            String number;
            String iNumber = FragmentConstants.TEMP_ARGS.getString(Constants.EXTRA_PHONE_NUMBER, "");
            if(TextUtils.isEmpty(iNumber)){
                replaceFragment(MainRecordActivity.lastFragment);
                return;
            }
            //格式化号码
            number = BtPhoneUtils.formatPhoneNumber(iNumber);
            logger.debug("incoming number:" + number);
            mCallerNumber.setText(number);

            //通过电话号码查找通讯录对应的人名
            new LoadBtTask().execute(number);
        }else{
            replaceFragment(MainRecordActivity.lastFragment);
            return;
        }

        if(VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_ANSWERED||
                VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_PROCEEDING||
                VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_ALERTING){
            //如果网络电话正在通话中
            //延时进入网络电话通话界面
            myHandler.sendMessageDelayed(new Message(),1000);
        }

    }

    private void initListener() {
        mAcceptButton.setOnClickListener(this);
        mDropButton.setOnClickListener(this);
        mBackButton.setOnClickListener(this);
    }

    private void initView() {
        mAcceptButton = (Button) mViewHolder.v(R.id.button_accept);
        mDropButton = (Button) mViewHolder.v(R.id.button_drop);
        mBackButton = (ImageButton) mViewHolder.v(R.id.button_back);
        mCallerName = (TextView) mViewHolder.v(R.id.caller_name);
        mCallerNumber = (TextView) mViewHolder.v(R.id.caller_number);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_accept:
                acceptPhone();
                break;
            case R.id.button_drop:
                rejectPhone();
                break;
            case R.id.button_back:
                replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
                break;
        }
    }
    private void rejectPhone() {
        Intent intent = new Intent("wld.btphone.bluetooth.CALL_REJECT");
        mBaseActivity.sendBroadcast(intent);
    }

    private void acceptPhone() {
        Intent intent = new Intent("wld.btphone.bluetooth.CALL_ACCEPT");
        mBaseActivity.sendBroadcast(intent);
    }

    private void holdPhone(){
        Intent intent = new Intent("wld.btphone.bluetooth.CALL_HOLD");
        mBaseActivity.sendBroadcast(intent);
    }

    /**
     * 通过电话号码查找通讯录对应的人名
     */
    class LoadBtTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Contact contact = null;
            try{

                contact = BtPhoneUtils.getContactByNumber(params[0]);
            }catch (Exception e){
                e.printStackTrace();
            }
            return contact==null?"":contact.getName();
        }

        @Override
        protected void onPostExecute(String name) {
            mCallerName.setText(name);
        }
    }

    /**
     * 查询指定电话的联系人姓名
     * */
    private String queryContactNameByNumber(final String phoneNum) throws Exception {
        if(null==phoneNum || "".equals(phoneNum)){
            return "";
        }
        String name = "";
        Uri uri = Uri.parse("content://com.android.contacts/data/phones/filter/" + phoneNum);
        ContentResolver resolver = mBaseActivity.getContentResolver();
        Cursor cursor = resolver.query(uri, new String[]{ContactsContract.Contacts.DISPLAY_NAME}, null, null, null);
        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
            logger.debug("name:"+name+",phoneNum:"+phoneNum);
        }
        cursor.close();
        return name;
    }

    /*public void dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        logger.debug("keyEvent getKeyCode:"+code);
        if(code==93){
            //接听
            acceptPhone();
        }else if(code==92){
            //挂断
            rejectPhone();
        }
    }*/

    private Handler myHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //挂断蓝牙电话
            rejectPhone();
            replaceFragment(FragmentConstants.VOIP_CALLING_FRAGMENT);
        }
    };
}
