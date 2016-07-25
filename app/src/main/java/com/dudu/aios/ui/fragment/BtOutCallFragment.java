package com.dudu.aios.ui.fragment;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.dudu.aios.ui.activity.MainRecordActivity;
import com.dudu.aios.ui.base.BaseActivity;
import com.dudu.aios.ui.bt.Contact;
import com.dudu.aios.ui.fragment.base.RBaseFragment;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.broadcast.ACCReceiver;
import com.dudu.android.launcher.utils.BtPhoneUtils;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.android.launcher.utils.cache.AsyncTask;
import com.dudu.commonlib.CommonLib;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.map.NavigationProxy;
import com.dudu.navi.NavigationManager;
import com.dudu.voice.FloatWindowUtils;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.TTSType;
import com.dudu.voip.VoipSDKCoreHelper;
import com.umeng.analytics.MobclickAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by Robi on 2016-03-10 18:36.
 * Edit Chad on 2016-04-08 11:30
 */
public class BtOutCallFragment extends RBaseFragment implements View.OnClickListener {

    private Logger logger = LoggerFactory.getLogger("phone.BtOutCallFragment");

    private Button mTerminateButton;

    private TextView mContactNameView, mContactsNumberView;

    @Override
    protected int getContentView() {
        return R.layout.activity_blue_tooth_dialing;
    }

    @Override
    protected void initView(View rootView) {
        mTerminateButton = (Button) mViewHolder.v(R.id.button_drop);
        mContactNameView = (TextView) mViewHolder.v(R.id.caller_name);
        mContactsNumberView = (TextView) mViewHolder.v(R.id.caller_number);
    }

    @Override
    protected void initViewData() {
        logger.trace("initViewData()");
        mTerminateButton.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        logger.debug("onResume()");
        dial();
    }

    @Override
    public void onPause() {
        super.onPause();
        BtPhoneUtils.btCallOutSource = BtPhoneUtils.BTCALL_OUT_SOURCE_DEFAULT;
        myHandler.removeMessages(2);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        logger.debug("onHiddenChanged() hidden:"+hidden);
        if (hidden) {
            //停止手势功能
//            BtPhoneUtils.sensortekBroadcastDisable(mBaseActivity);
            mContactNameView.setText("");
            mContactsNumberView.setText("");

            myHandler.removeMessages(2);
            BtPhoneUtils.btCallOutSource = BtPhoneUtils.BTCALL_OUT_SOURCE_DEFAULT;
        } else {
            //启动手势功能
//            BtPhoneUtils.sensortekBroadcastEnable(mBaseActivity);
            dial();
        }
    }

    //拨号
    private void dial() {
        if(BtPhoneUtils.btCallState==BtPhoneUtils.CALL_STATE_DIALING ||
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

        if (FragmentConstants.TEMP_ARGS != null  && this.isVisible()/*
                BtPhoneUtils.btCallOutSource != BtPhoneUtils.BTCALL_OUT_SOURCE_DEFAULT &&
                */) {
            mContactNameView.setText("");
            String name = FragmentConstants.TEMP_ARGS.getString(Constants.EXTRA_CONTACT_NAME);
            String number = FragmentConstants.TEMP_ARGS.getString(Constants.EXTRA_PHONE_NUMBER);
            logger.debug("name:"+name + ",number:"+number);
            String phoneNum = "";

            try{
                if (TextUtils.isEmpty(number)) {
                    replaceFragment(MainRecordActivity.lastFragment);
                    return;
                } else {
                    phoneNum = number.replace("-", "").replace(" ","");
                    mContactsNumberView.setText(BtPhoneUtils.formatPhoneNumber(phoneNum));
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            logger.debug("bluetooth hfp connect state " + BtPhoneUtils.connectionState + " ,lastFragment:" + MainRecordActivity.lastFragment);
            if (BtPhoneUtils.connectionState != BtPhoneUtils.STATE_CONNECTED) {
//                VoiceManagerProxy.getInstance().startSpeaking(
//                        mBaseActivity.getString(R.string.bt_noti_connect_waiting), TTSType.TTS_DO_NOTHING, false);
                replaceFragment(MainRecordActivity.lastFragment);
                return;
            }

            logger.debug("BtPhoneUtils.btCallState:"+BtPhoneUtils.btCallState +
                    ",BtPhoneUtils.btCallOutSource:"+BtPhoneUtils.btCallOutSource);

            //查找通讯录该号码的姓名并更新UI
//            new LoadBtTask().execute(phoneNum);
            //从通讯录缓存循环查找获取
            if(TextUtils.isEmpty(name)){
                Contact contact = BtPhoneUtils.getContactByNumber(phoneNum);
                if(null!=contact){
                    if (!TextUtils.isEmpty(contact.getName())) {
                        mContactNameView.setText(contact.getName());
                    }
                }else{
                    logger.debug("contact is null");
                }
            }else{
                mContactNameView.setText(name);
            }
            //如果不在通话中
            if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_DEFAULT||
                    BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_TERMINATED) {
                //如果是从键盘、语音呼叫拨号（不是从蓝牙连接的手机端发出）
                if ((BtPhoneUtils.btCallOutSource==BtPhoneUtils.BTCALL_OUT_SOURCE_KEYBOARD ||
                        BtPhoneUtils.btCallOutSource==BtPhoneUtils.BTCALL_OUT_SOURCE_VOIC)) {
//                    FloatWindowUtils.getInstance().removeFloatWindow();
                    //如果不是蓝牙设备拨出的电话
                    Intent intent = new Intent(Constants.BLUETOOTH_DIAL);
                    intent.putExtra(Constants.DIAL_NUMBER, phoneNum);
                    mBaseActivity.sendBroadcast(intent);
                    BtPhoneUtils.btCallOutSource = BtPhoneUtils.BTCALL_OUT_SOURCE_DEFAULT;
                    // 延时判断当前是否在通话中，否则关闭当前页面
                    // 修复当手机无SIM卡时拨号不关闭拨出界面的问题
                    Message msg = new Message();
                    msg.what = 2;
                    myHandler.sendMessageDelayed(msg, 30000);
                }

            } else {
                logger.trace("还在通话中，不能再次拨号");
            }

        } else {
            logger.debug("FragmentConstants.TEMP_ARGS:" + FragmentConstants.TEMP_ARGS +
                    "this.isVisible():"+this.isVisible()+
                    " BaseActivity.lastFragment:"+BaseActivity.lastFragment);
            switchFragmentByBtCallState();
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_drop:
                MobclickAgent.onEvent(mBaseActivity, ClickEvent.click50.getEventId());
                BtPhoneUtils.mForceEnd = true;//强制挂断
                myHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        replaceFragment(MainRecordActivity.lastFragment);
                        callTermination();
                    }
                },1000);
                break;
        }
    }

    Handler myHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            logger.debug("msg.what:"+msg.what);
            if(msg.what==1){
                callTermination();
            }else if(msg.what==2){
                if(BtPhoneUtils.btCallState!=BtPhoneUtils.CALL_STATE_ACTIVE &&
                        (BtPhoneUtils.btCallState!=BtPhoneUtils.CALL_STATE_DIALING ||
                        BtPhoneUtils.btCallState==BtPhoneUtils.CALL_STATE_TERMINATED)){
                    replaceFragment(FragmentConstants.BT_DIAL);
                    callTermination();
                }
            }
        }
    };
    /**
     * 发出挂断电话广播
     */
    private void callTermination() {
        BtPhoneUtils.btCallOutSource = BtPhoneUtils.BTCALL_OUT_SOURCE_DEFAULT;
        Intent intent = new Intent("wld.btphone.bluetooth.CALL_TERMINATION");
        mBaseActivity.sendBroadcast(intent);
    }

    /*public void dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        logger.debug("手势keyEvent getKeyCode:"+code);

        if(code==92){
            //挂断
            callTermination();
        }else if(code==93){
            //挂断
            callTermination();
        }
    }*/

    class LoadBtTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String name = "";
            try {
                //根据联系人号码查询姓名
                name = BtPhoneUtils.queryContactNameByNumber(mBaseActivity, params[0]);
                logger.debug("拨出号码："+params[0] + ",姓名："+ name);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return name;
        }

        @Override
        protected void onPostExecute(String name) {
            if (null != mContactNameView) {

                mContactNameView.setText(name);
            }
        }
    }

    /**
     * 根据蓝牙电话状态显示界面
     */
    private void checkStateReplaceFragment(){
        Log.d("phone", "bt call state:" + BtPhoneUtils.btCallState +
                "voip call state:" + VoipSDKCoreHelper.getInstance().eccall_state+
                ",acc is back caring:" + ACCReceiver.isBackCarIng +
                ",navigat state:" + NavigationManager.getInstance(CommonLib.getInstance().getContext()).isNavigatining());

//        if(ACCReceiver.isBackCarIng){
//            //正在倒车
//            if (null != LauncherApplication.getContext().getInstance()) {
//
//                replaceFragment(FragmentConstants.FRAGMENT_DRIVING_RECORD);
//            }
//        }else if (NavigationManager.getInstance(CommonLib.getInstance().getContext()).isNavigatining()) {
//            //正在导航
//            NavigationProxy.getInstance().openNavi(NavigationProxy.OPEN_MANUAL);
//        }else if (VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_PROCEEDING ||
//                VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_ANSWERED ||
//                VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_ALERTING) {
//            //正在拨打VIP电话
//            replaceFragment(FragmentConstants.VOIP_CALLING_FRAGMENT);
//        }
    }

    private void switchFragmentByBtCallState(){
        //网络电话不在工作中
        String number = "";
        if (null != FragmentConstants.TEMP_ARGS) {
            number = FragmentConstants.TEMP_ARGS.getString(Constants.EXTRA_PHONE_NUMBER);
        }
        if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ACTIVE &&
                !TextUtils.isEmpty(number)) {
            //通话中
            replaceFragment(FragmentConstants.BT_CALLING);
        } else if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_INCOMING &&
                !TextUtils.isEmpty(number)) {
            //来电中
            replaceFragment(FragmentConstants.BT_IN_CALL);
        } else if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_DIALING &&
                !TextUtils.isEmpty(number)) {
            //拨号中
            replaceFragment(FragmentConstants.BT_OUT_CALL);
        }else if(BtPhoneUtils.getAudioState()==2 && !TextUtils.isEmpty(number)){
            //蓝牙语音在连接中
            replaceFragment(FragmentConstants.BT_CALLING);
        }else{
            replaceFragment(MainRecordActivity.lastFragment);
        }
    }
}
