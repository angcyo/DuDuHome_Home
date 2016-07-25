package com.dudu.android.launcher.broadcast;

import android.app.Activity;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;

import com.dudu.aios.ui.activity.MainRecordActivity;
import com.dudu.aios.ui.base.BaseActivity;
import com.dudu.aios.ui.base.ObservableFactory;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.utils.ActivitiesManager;
import com.dudu.android.launcher.utils.BtPhoneUtils;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.commonlib.utils.File.KeyConstants;
import com.dudu.drivevideo.frontcamera.FrontCameraManage;
import com.dudu.event.DeviceEvent;
import com.dudu.init.CarFireManager;
import com.dudu.map.NavigationProxy;
import com.dudu.navi.NavigationManager;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voip.VoipSDKCoreHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * Created by 赵圣琪 on 2016/1/18.
 */
public class BtCallReceiver extends BroadcastReceiver {

    public static final int CALL_STATE_TERMINATED = 7;
    public static final String EXTRA_RESULT_CODE = "android.bluetooth.handsfreeclient.extra.RESULT_CODE";
    public static final String EXTRA_CME_CODE = "android.bluetooth.handsfreeclient.extra.CME_CODE";
    private static final String ACTIVITY_NAME_AUTONAVI = "com.autonavi.auto.MainMapActivity";
    private Logger logger = LoggerFactory.getLogger("phone.BtCallReceiver");
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private String mScreenState = Intent.ACTION_SCREEN_OFF;
    private int MSG_BT_IN_CALL = 1;
    private int MSG_BT_OUT_CALL = 2;
    private int MSG_BT_CALLING = 3;
    private int MSG_BT_END = 4;
    public static HashMap<String, Long> phoneStartTimeList = new HashMap<>(); //通话开始时间

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!KeyConstants.is_agree_disclaimer)
            return;
        String action = intent.getAction();
        logger.debug("接收到蓝牙电话广播: " + action);
        if (action.equals(Constants.ACTION_BLUETOOTH_PHONE_INCALL)) {
            BtPhoneUtils.btCallState = BtPhoneUtils.CALL_STATE_INCOMING;
            //暂停语音
            logger.debug("停止语音唤醒");
            VoiceManagerProxy.getInstance().stopWakeup();
//            BtPhoneUtils.releaseISpeech();
            //启动手势功能
            logger.debug("ACTION_BLUETOOTH_PHONE_INCALL 启动手势功能");
            BtPhoneUtils.sensortekBroadcastEnable();
            setAudioMode(AudioManager.MODE_RINGTONE);//BtPhoneUtils.initAudio(LauncherApplication.getContext(),AudioManager.MODE_RINGTONE);
            LoggerFactory.getLogger("video1.frontdrivevideo").debug("设置模糊状态：{}", true);
            FrontCameraManage.getInstance().setPreviewBlur(true);
//            FrontCameraInstance.getInstance().switchBlur(true);

            //设置音量
            int curVolume = BtPhoneUtils.getBtPhoneCurrentVolume(context, Constants.STREAM_BLUETOOTH_SCO);
            logger.debug("in call current STREAM_BLUETOOTH_SCO volume:" + curVolume + ",set default ring volume 5");
            BtPhoneUtils.setBtPhoneVolume(context, Constants.STREAM_BLUETOOTH_SCO, 5);

            //点亮屏幕
            CarFireManager.getInstance().acquireLock();

            // 获取电话号码
            String phoneNumber = intent.getStringExtra("HFP_NUMBER");
            logger.debug("incoming number:" + phoneNumber);
            if (!TextUtils.isEmpty(phoneNumber)) {
                if (null == FragmentConstants.TEMP_ARGS) {
                    FragmentConstants.TEMP_ARGS = new Bundle();
                }
                FragmentConstants.TEMP_ARGS.putString(Constants.EXTRA_PHONE_NUMBER, phoneNumber);
            }

            toMainActivity(LauncherApplication.getContext());

            Message msg = new Message();
            msg.what = MSG_BT_IN_CALL;
            myHandler.sendMessage(msg);

        } else if (action.equals(Constants.ACTION_BLUETOOTH_PHONE_OUTCALL) ||
                action.equals(Constants.ACTION_BLUETOOTH_PHONE_ALERTING)) {
            BtPhoneUtils.btCallState = BtPhoneUtils.CALL_STATE_DIALING;
            //暂停语音
            logger.debug("停止语音唤醒");
            VoiceManagerProxy.getInstance().stopWakeup();
//            BtPhoneUtils.releaseISpeech();
            //启动手势功能
            logger.debug("ACTION_BLUETOOTH_PHONE_OUTCALL 启动手势功能");
            BtPhoneUtils.sensortekBroadcastEnable();
            //设置音量
            setAudioMode(AudioManager.MODE_RINGTONE);//BtPhoneUtils.initAudio(LauncherApplication.getContext(),AudioManager.MODE_NORMAL);
            int curVolume = BtPhoneUtils.getBtPhoneCurrentVolume(context, Constants.STREAM_BLUETOOTH_SCO);

            logger.debug("out call current STREAM_BLUETOOTH_SCO Volume:" + curVolume + ",set volume 10");
            BtPhoneUtils.setBtPhoneVolume(context, Constants.STREAM_BLUETOOTH_SCO, 10);
            LoggerFactory.getLogger("video1.frontdrivevideo").debug("设置模糊状态：{}", true);
            FrontCameraManage.getInstance().setPreviewBlur(true);
            //点亮屏幕
            CarFireManager.getInstance().acquireLock();

            // 获取电话号码
            String phoneNumber = intent.getStringExtra("HFP_NUMBER");
            logger.debug("outcall number:" + phoneNumber);

            if (!TextUtils.isEmpty(phoneNumber)) {
                if (null == FragmentConstants.TEMP_ARGS) {
                    FragmentConstants.TEMP_ARGS = new Bundle();
                }
                FragmentConstants.TEMP_ARGS.putString(Constants.EXTRA_PHONE_NUMBER, phoneNumber);
            }

            toMainActivity(LauncherApplication.getContext());

            if (BtPhoneUtils.btCallOutSource == BtPhoneUtils.BTCALL_OUT_SOURCE_DEFAULT) {
                BtPhoneUtils.btCallOutSource = BtPhoneUtils.BTCALL_OUT_SOURCE_MOBILE;
                Message msg = new Message();
                msg.what = MSG_BT_OUT_CALL;
                logger.debug("myHandler:" + phoneNumber);
                myHandler.sendMessage(msg);
            }
            //重置强制变量为false，当用户在拨号界面挂断才置为true
            myHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BtPhoneUtils.mForceEnd = false;
                }
            }, 2000);
        } else if (action.equals(Constants.ACTION_BLUETOOTH_PHONE_CONNECT)) {

            BtPhoneUtils.btCallState = BtPhoneUtils.CALL_STATE_ACTIVE;
            logger.debug("ACTION_BLUETOOTH_PHONE_CONNECT btCallState:" + BtPhoneUtils.btCallState);
            setAudioMode(AudioManager.MODE_IN_CALL);//BtPhoneUtils.initAudio(context,AudioManager.MODE_IN_CALL);
//            AudioManager audiomanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//            audiomanager.setBluetoothScoOn(false);
//            audiomanager.stopBluetoothSco();
//            audiomanager.setBluetoothScoOn(true);
//            audiomanager.startBluetoothSco();
//            logger.debug("mode:" + audiomanager.getMode() + ",isBluetoothScoOn:" + audiomanager.isBluetoothScoOn() +
//                    ",isMicrophoneMute:"+audiomanager.isMicrophoneMute());
            //设置音量
            int curVolume = BtPhoneUtils.getBtPhoneCurrentVolume(context, Constants.STREAM_BLUETOOTH_SCO);

            logger.debug("connect call current STREAM_BLUETOOTH_SCO Volume:" + curVolume + ",set volume " + 10);
            BtPhoneUtils.setBtPhoneVolume(context, Constants.STREAM_BLUETOOTH_SCO, 10);

            if (null == FragmentConstants.TEMP_ARGS) {
                FragmentConstants.TEMP_ARGS = new Bundle();
            }
            //获取电话号码
            String phoneNumber = intent.getStringExtra("HFP_NUMBER");
            if (!TextUtils.isEmpty(phoneNumber)) {
                FragmentConstants.TEMP_ARGS.putString(Constants.EXTRA_PHONE_NUMBER, phoneNumber);
                //缓存通话号码的起始时间
                phoneStartTimeList.remove(phoneNumber);
                phoneStartTimeList.put(phoneNumber, System.currentTimeMillis());
            }

            //如果网络电话正在通话中
            if (VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_ANSWERED ||
                    VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_PROCEEDING ||
                    VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_ALERTING) {

                toMainActivity(LauncherApplication.getContext());
                if (null != LauncherApplication.getContext().getInstance()) {
                    logger.debug("LauncherApplication.getContext().getInstance() " + LauncherApplication.getContext().getInstance());
                    LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.VOIP_CALLING_FRAGMENT);
                }
            } else {
                toMainActivity(LauncherApplication.getContext());
                if (null != LauncherApplication.getContext().getInstance()) {
                    logger.debug("LauncherApplication.getContext().getInstance() " + LauncherApplication.getContext().getInstance());
//                    LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.BT_CALLING);
                    Message msg = new Message();
                    msg.what = MSG_BT_CALLING;
                    logger.debug("myHandler:" + phoneNumber);
                    myHandler.sendMessage(msg);
                }
            }

            if (NavigationManager.getInstance(LauncherApplication.getContext()).isNavigatining()) {
                logger.debug("NavigationManager navigatining");
                //导航
                NavigationProxy.getInstance().openGaode();
                //NavigationProxy.getInstance().openNavi(NavigationProxy.OPEN_MANUAL);
                logger.debug("停止语音唤醒");
                VoiceManagerProxy.getInstance().stopWakeup();
            }

        } else if (action.equals(Constants.ACTION_BLUETOOTH_PHONE_END)) {
            BtPhoneUtils.setAudioStateChangeConn(-1);
//            BtPhoneUtils.mForceEnd = false;
            setAudioMode(AudioManager.MODE_NORMAL);
            disposeBtPhoneEnd(intent);
            replaceFragment();

        } else if (action.equals(Constants.ACTION_AUDIO_STATE_CHANGED)) {
            int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, 0);
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
            BtPhoneUtils.setPreAudioState(prevState);
            BtPhoneUtils.setAudioState(state);
            logger.debug("BtPhoneUtils.connectionState:" + BtPhoneUtils.connectionState + ",ACTION_AUDIO_STATE_CHANGED prevState: " + prevState + ",state: " + state +
                    ",BtPhoneUtils.btCallState:" + BtPhoneUtils.btCallState + ",mAudioStateChangeConn:" + BtPhoneUtils.getAudioStateChangeConn());
            //取消防盗防劫界面的动画
            EventBus.getDefault().post(new DeviceEvent.SafetyMainFragmentBack());
            AudioManager audiomanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            logger.debug("mode:" + audiomanager.getMode() + ",isBluetoothScoOn:" + audiomanager.isBluetoothScoOn() +
                    ",isMicrophoneMute:" + audiomanager.isMicrophoneMute());
            //检查蓝牙连接状态
            if (!ObservableFactory.getInstance().getTitleObservable().bluetooth.get()) {

                BtPhoneUtils.checkUpdateBluetoothConnectState();
            }
            // 获取电话号码
            Bundle bundle = FragmentConstants.TEMP_ARGS;
            String phoneNumber = "";
            if (null != bundle) {
                phoneNumber = bundle.getString(Constants.EXTRA_PHONE_NUMBER, "");
                intent.putExtra("HFP_NUMBER", phoneNumber);
                logger.debug("audio state changed call number:" + phoneNumber);
            }

            //如果音频已连接
            if (prevState == 1 && state == 2) {
                setAudioMode(AudioManager.MODE_IN_CALL);//BtPhoneUtils.initAudio(context,AudioManager.MODE_IN_CALL);

                //启动手势功能
                logger.debug("ACTION_AUDIO_STATE_CHANGED 启动手势功能");
                BtPhoneUtils.sensortekBroadcastEnable();
                //设置音量
                int curVolume = BtPhoneUtils.getBtPhoneCurrentVolume(context, Constants.STREAM_BLUETOOTH_SCO);
                logger.debug("connect call current STREAM_BLUETOOTH_SCO Volume:" + curVolume + ",set volume " + 10);
                BtPhoneUtils.setBtPhoneVolume(context, Constants.STREAM_BLUETOOTH_SCO, 10);

                //如果蓝牙电话状态没有改变，又收到ACTION_AUDIO_STATE_CHANGED广播才进入界面跳转
                if ((BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_DEFAULT ||
                        BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_TERMINATED) &&
                        !TextUtils.isEmpty(phoneNumber) && BtPhoneUtils.getAudioStateChangeConn() == 1) {

                    Message msg = new Message();
                    if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ACTIVE) {
                        msg.what = MSG_BT_CALLING;
                    } else if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_INCOMING) {
                        msg.what = MSG_BT_IN_CALL;
                    } else if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_DIALING) {
                        msg.what = MSG_BT_OUT_CALL;
                    } else if (state != 2) {
                        msg.what = MSG_BT_END;
                    } else {
                        BtPhoneUtils.btCallState = BtPhoneUtils.CALL_STATE_ACTIVE;
                        msg.what = MSG_BT_CALLING;
                    }
                    logger.debug("call number:" + phoneNumber);
                    myHandler.sendMessage(msg);
                } else if (!TextUtils.isEmpty(phoneNumber) && BtPhoneUtils.getAudioStateChangeConn() == 0) {
                    Message msg = new Message();
                    if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ACTIVE) {
                        msg.what = MSG_BT_CALLING;
                    } else if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_INCOMING) {
                        msg.what = MSG_BT_IN_CALL;
                    } else if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_DIALING) {
                        msg.what = MSG_BT_OUT_CALL;
                    } else if (state != 2) {
                        msg.what = MSG_BT_END;
                    } else {
                        BtPhoneUtils.btCallState = BtPhoneUtils.CALL_STATE_ACTIVE;
                        msg.what = MSG_BT_CALLING;
                    }
                    logger.debug("call number:" + phoneNumber);
                    myHandler.sendMessage(msg);
                }
            } else if (prevState == 2 && state == 0) {
                setAudioMode(AudioManager.MODE_NORMAL);//BtPhoneUtils.initAudio(LauncherApplication.getContext(),AudioManager.MODE_NORMAL);
                //停止手势功能
                logger.debug("停止手势功能");
                BtPhoneUtils.sensortekBroadcastDisable();

                try {
                    //蓝牙连接没断开，而音频由连接到断开
                    if (BtPhoneUtils.connectionState == BtPhoneUtils.STATE_CONNECTED) {
                        BtPhoneUtils.setAudioStateChangeConn(0);
                    } else if (BtPhoneUtils.connectionState != BtPhoneUtils.STATE_CONNECTED) {
                        BtPhoneUtils.setAudioStateChangeConn(1);
                    }
                    //蓝牙音频断开当挂断处理
                    disposeBtPhoneEnd(intent);
                    replaceFragment();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (action.equals(Constants.ACTION_AG_CALL_CHANGED)) {
            logger.debug("BtPhoneUtils.btCallState:" + BtPhoneUtils.btCallState);
            //来电振铃
            //因为影响倒车时来电界面显示，注释掉来电振铃处理，不影响来电时弹出界面
            /*if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_INCOMING) {
                toMainActivity(LauncherApplication.getContext());
                Message msg = new Message();
                msg.what = 1;
                myHandler.sendMessageDelayed(msg, 1000);
            }*/
        } else if (action.equals(Constants.ACTION_CONNECTION_STATE_CHANGED)) {
            int prevState = intent.getIntExtra(Constants.EXTRA_PREVIOUS_STATE, 0);
            int state = intent.getIntExtra(Constants.EXTRA_STATE, 0);
            if (null != FragmentConstants.TEMP_ARGS) {
                String number = FragmentConstants.TEMP_ARGS.getString(Constants.EXTRA_PHONE_NUMBER, "");
                intent.putExtra("HFP_NUMBER", number);
            }

            BtPhoneUtils.connectionState = state;
            BtPhoneUtils.prevConnectionState = prevState;
            logger.debug("HandsFreeClient prevState: " + prevState + " state: " + state);
//            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            logger.debug("BtPhoneUtils.btCallState = " + BtPhoneUtils.btCallState);
            if ((BtPhoneUtils.isCalling()) && (state == BluetoothProfile.STATE_DISCONNECTED)) {
                setAudioMode(AudioManager.MODE_NORMAL);
                disposeBtPhoneEnd(intent);
                replaceFragment();
            }
        } else if (action.equals(Constants.ACTION_HFP_RESULT)) {
            //拨打无SIM卡的手机时
            int result = intent.getIntExtra(EXTRA_RESULT_CODE, -1);
            int cme = intent.getIntExtra(EXTRA_CME_CODE, -1);
            if ((result == 1 && (cme == -1 || cme == 27)) ||
                    (result == 7 && cme == 0)) {
                //停止手势功能
                logger.debug("停止手势功能");
                BtPhoneUtils.sensortekBroadcastDisable();
                if (null == FragmentConstants.TEMP_ARGS) {
                    FragmentConstants.TEMP_ARGS = new Bundle();
                }
                FragmentConstants.TEMP_ARGS.putString(Constants.EXTRA_PHONE_NUMBER, "");
                //恢复唤醒语音
                logger.debug("恢复语音唤醒");
                VoiceManagerProxy.getInstance().startWakeup();

                BtPhoneUtils.btCallState = BtPhoneUtils.CALL_STATE_TERMINATED;
                BtPhoneUtils.btCallOutSource = BtPhoneUtils.BTCALL_OUT_SOURCE_DEFAULT;
                replaceFragment();
            }
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            mScreenState = Intent.ACTION_SCREEN_OFF;
        } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
            mScreenState = Intent.ACTION_SCREEN_ON;
        }
    }

    /**
     * 处理蓝牙电话挂断事务
     *
     * @param intent
     */
    private void disposeBtPhoneEnd(Intent intent) {
        BtPhoneUtils.btCallState = BtPhoneUtils.CALL_STATE_TERMINATED;
        BtPhoneUtils.btCallOutSource = BtPhoneUtils.BTCALL_OUT_SOURCE_DEFAULT;
        //停止手势功能
        logger.debug("停止手势功能");
        BtPhoneUtils.sensortekBroadcastDisable();
        //获取电话号码
        String phoneNumber = intent.getStringExtra("HFP_NUMBER");
        if (null == FragmentConstants.TEMP_ARGS) {
            FragmentConstants.TEMP_ARGS = new Bundle();
        }
        if (!TextUtils.isEmpty(phoneNumber)) {
            FragmentConstants.TEMP_ARGS.putString(Constants.EXTRA_PHONE_NUMBER, phoneNumber);
            //清除通话号码起始时间
            phoneStartTimeList.remove(phoneNumber);
        } else {
            phoneStartTimeList.clear();
        }
        //如果网络电话在通话中
        if ((VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_ANSWERED ||
                VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_PROCEEDING ||
                VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_ALERTING)) {
            //停止手势功能
            logger.debug("开启手势功能");
            BtPhoneUtils.sensortekBroadcastEnable();
        } else {
            //恢复唤醒语音
            logger.debug("恢复语音唤醒");
            VoiceManagerProxy.getInstance().startWakeup();
        }
    }

    /**
     * 处理停止通话后的界面
     */
    private void replaceFragment() {

        logger.debug("BaseActivity.lastFragment:" + BaseActivity.lastFragment +
                ",BaseActivity.lastSecondFragment:" + BaseActivity.lastSecondFragment +
                ",BaseActivity.lastThirdFragment:" + BaseActivity.lastThirdFragment +
                ",BtPhoneUtils.btCallState:" + BtPhoneUtils.btCallState);
        //如果导航界面不在前台
        if (!ActivitiesManager.getInstance().isForegroundActivity(LauncherApplication.getContext(), ACTIVITY_NAME_AUTONAVI)) {
            toMainActivity(LauncherApplication.getContext());

            if (null != LauncherApplication.getContext().getInstance()) {
                if (VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_ANSWERED ||
                        VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_PROCEEDING ||
                        VoipSDKCoreHelper.getInstance().eccall_state == VoipSDKCoreHelper.ERROR_ECCALL_ALERTING) {
                    //如果网络电话正在通话中

                    setAudioMode(AudioManager.MODE_IN_COMMUNICATION);//BtPhoneUtils.initAudio(LauncherApplication.getContext(), AudioManager.MODE_IN_COMMUNICATION);
                    LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.VOIP_CALLING_FRAGMENT);
                } else if (NavigationManager.getInstance(LauncherApplication.getContext()).isNavigatining()) {
                    LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
                    //导航
                    NavigationProxy.getInstance().openNavi(NavigationProxy.OPEN_MANUAL);
                } else {
                    //如果挂断电话前有两个通话，判断当前如果还有一个通话
                    if (null != phoneStartTimeList && phoneStartTimeList.size() > 0) {
                        Iterator iter = phoneStartTimeList.entrySet().iterator();
                        if (iter.hasNext()) {

                            Map.Entry entry = (Map.Entry) iter.next();
                            if (null == FragmentConstants.TEMP_ARGS) {
                                FragmentConstants.TEMP_ARGS = new Bundle();
                            }
                            FragmentConstants.TEMP_ARGS.putString(Constants.EXTRA_PHONE_NUMBER, (String) entry.getKey());
                            BtPhoneUtils.btCallState = BtPhoneUtils.CALL_STATE_ACTIVE;//设置蓝牙电话状态为通话中
                            setAudioMode(AudioManager.MODE_IN_CALL);//BtPhoneUtils.initAudio(LauncherApplication.getContext(), AudioManager.MODE_IN_CALL);
                            LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.BT_CALLING);
                        }
                    } else if (null == phoneStartTimeList || phoneStartTimeList.size() == 0) {
                        logger.debug("当前没有通话");
                        setAudioMode(AudioManager.MODE_NORMAL);//BtPhoneUtils.initAudio(LauncherApplication.getContext(), AudioManager.MODE_NORMAL);
                        if (FragmentConstants.FRAGMENT_DRIVING_RECORD.equals(MainRecordActivity.lastFragment)) {
                            if (!ACCReceiver.isBackCarIng) {
                                //不在倒车
                                LauncherApplication.getContext().getInstance().replaceFragment(MainRecordActivity.lastSecondFragment);
                            } else {
                                LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_DRIVING_RECORD);
                            }
                        } else {
                            LauncherApplication.getContext().getInstance().replaceFragment(MainRecordActivity.lastFragment);
                            //释放wakelock
                            CarFireManager.getInstance().releaseWakeLockIfNotFired();
                        }
                    }
                }
            }
        } else if (NavigationManager.getInstance(LauncherApplication.getContext()).isNavigatining()) {
            logger.debug("NavigationManager navigatining");
            if (null != LauncherApplication.getContext().getInstance()) {
                LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.FRAGMENT_MAIN_PAGE);
            }
            //正在导航，打开导航界面
            NavigationProxy.getInstance().openNavi(NavigationProxy.OPEN_MANUAL);
        } else {
            //释放wakelock
            CarFireManager.getInstance().releaseWakeLockIfNotFired();
        }
    }

    private void startBtPhoneActivity(Context context,
                                      Class<? extends Activity> clzz, Intent intent) {
        Intent i = new Intent(context, clzz);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(Constants.EXTRA_PHONE_NUMBER, intent.getStringExtra("HFP_NUMBER"));
        i.putExtra(Constants.EXTRA_CONTACT_NAME, intent.getStringExtra("HFP_NAME"));
        context.startActivity(i);
    }

    private void toMainActivity(Context context) {

        if (!ActivitiesManager.getInstance().isForegroundActivity(context, MainRecordActivity.class.getName())) {

            Intent intent = new Intent();
            intent.setClass(context, MainRecordActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            if (LauncherApplication.getContext().getInstance() != null) {
                ActivitiesManager.getInstance().setTopActivity(LauncherApplication.getContext().getInstance());
            }
        }
    }

    /**
     * 设置音频模式
     *
     * @param mode
     */
    private void setAudioMode(final int mode) {
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    BtPhoneUtils.initAudio(LauncherApplication.getContext(), mode);
                }
            }
        });
    }

    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            logger.debug("msg:" + msg + ",BtPhoneUtils.mForceEnd:" + BtPhoneUtils.mForceEnd);
            toMainActivity(LauncherApplication.getContext());
            String number = "";
            if (null != FragmentConstants.TEMP_ARGS) {
                number = FragmentConstants.TEMP_ARGS.getString(Constants.EXTRA_PHONE_NUMBER);
            }
            if (msg.what == MSG_BT_IN_CALL) {
                if (null != LauncherApplication.getContext().getInstance() && !TextUtils.isEmpty(number)) {
                    LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.BT_IN_CALL);
                }
            } else if (msg.what == MSG_BT_OUT_CALL) {
                //对拨出的号码进行非空判断，以及是否强制挂断判断
                if (null != LauncherApplication.getContext().getInstance() && !BtPhoneUtils.mForceEnd && !TextUtils.isEmpty(number)) {
                    LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.BT_OUT_CALL);
                }
            } else if (msg.what == MSG_BT_CALLING) {
                if (null != LauncherApplication.getContext().getInstance() && !TextUtils.isEmpty(number)) {
                    LauncherApplication.getContext().getInstance().replaceFragment(FragmentConstants.BT_CALLING);
                }
            } else if (msg.what == MSG_BT_END) {
                replaceFragment();
            }
        }
    };
}
