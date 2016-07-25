package com.dudu.android.launcher.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.utils.BtPhoneUtils;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.android.launcher.utils.DeviceIDUtil;
import com.dudu.commonlib.utils.File.SharedPreferencesUtil;
import com.dudu.event.DeviceEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.greenrobot.event.EventBus;
import wld.btphone.bluetooth.aidl.PbapService;

/**
 * Created by Administrator on 2016/1/20.
 */
public class BluetoothService extends Service {

    private BluetoothAdapter mAdapter;

    private PbapService mPbapService;
    private HandlerThread mBtServiceThread;
    private Handler mBtServiceHandler;
    private List<String> mBondedDevices;//已经配对的设备地址
    private List<BluetoothDevice> mConnDevices;//已经连接的设备
    private Set<BluetoothDevice> mFondDevices = new HashSet<>();//发现附近的设备
    private Logger logger = LoggerFactory.getLogger("phone.BluetoothService");

    private int log_step;

    @Override
    public void onCreate() {
        super.onCreate();

        logger.debug("[{}]BluetoothService onCreate()...",log_step++);
        //初始化
        initService();
    }

    private void initService(){
        mBtServiceThread = new HandlerThread("bluetooth Service");
        mBtServiceThread.start();

        mBtServiceHandler = new Handler(mBtServiceThread.getLooper());
        //注册广播接收器
        registerReceiverForBluetoothPhone();
        //初始化蓝牙适配器
        initBlueAdapter();
        //绑定蓝牙电话底层服务
        onBindService();
    }
    /**
     * 初始化蓝牙适配器
     */
    private void initBlueAdapter(){
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        if (null != mAdapter) {
            if(!mAdapter.isEnabled()){
                mAdapter.enable();
            }
            if (mAdapter.isDiscovering()) {
                mAdapter.cancelDiscovery();
            }
//            logger.debug("mAdapter:" + mAdapter.toString());
//            //设置蓝牙可见性
//            int time = 0;//0为永不超时
//            BtPhoneUtils.setDiscoverableTimeout(time);
        }
    }


    /**
     * 获取绑定的蓝牙设备地址
     */
    private void getBondedDevices() {
        if (null == mBondedDevices) {
            mBondedDevices = new ArrayList<>();
        } else {
            mBondedDevices.clear();
        }
        if (null != mAdapter) {
            Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();
            if (null != bondedDevices && bondedDevices.size() > 0) {

                for (BluetoothDevice device : bondedDevices) {
                    logger.debug("已配对设备： " + device.getAddress() + ", 名称:" + device.getName());
                    mBondedDevices.add(device.getAddress());
                }
            } else {
                logger.debug("[phone][{}]已配对设备：无", log_step++);
            }
        }else{
            //初始化蓝牙适配器
            initBlueAdapter();
        }
    }

    private void startFindDevices(){
        if (null != mAdapter) {
            logger.debug("[phone][{}]启动蓝牙搜索附近的设备", log_step++);
            mAdapter.startDiscovery();
        }
    }
    /**
     * 判断该蓝牙设备地址是否存在于已配对设备地址列表里
     *
     * @param address
     * @return
     */
    private boolean existBtAddress(String address) {
        if (null != mBondedDevices) {
            for (String dev : mBondedDevices) {
                if (dev.equals(address)) {
                    logger.debug("最后一次连接的蓝牙设备在已配对设备列表中");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * BindService 绑定蓝牙电话底层服务
     */
    private void onBindService() {
        Intent intent = new Intent("wld.btphone.bluetooth.ProfileService");
        bindService(intent, mPbapServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.debug("[{}]BluetoothService onStartCommand().",log_step++);

        return START_STICKY;//super.onStartCommand(intent, flags, startId);//START_REDELIVER_INTENT
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logger.debug("[{}]BluetoothService onDestroy().",log_step++);

        try {
            unbindService(mPbapServiceConnection);
            unregisterReceiver(mBluetoothPhoneReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null!=mAdapter){
            mAdapter = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private ServiceConnection mPbapServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPbapService = PbapService.Stub.asInterface(service);
            logger.debug("[{}]连接蓝牙电话底层服务成功.mPbapService:" + mPbapService,log_step++);

            mBtServiceHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (null != mAdapter) {
                            if (!mAdapter.isEnabled()) {
                                mAdapter.enable();
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            //获取已配对的蓝牙设备
                            getBondedDevices();

                            //获取已经连接的设备
                            getConnectedDevice(mAdapter);
                            //连接在配对设备列表中上次连接过的设备
                            connectLastDevice(mAdapter);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            mPbapService = null;
            logger.debug("[{}]蓝牙电话底层服务已断开.",log_step++);
        }
    };

    /**
     * 注册广播接收器
     */
    private void registerReceiverForBluetoothPhone() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.BLUETOOTH_SHOW_CONNECT_FAIL);
        intentFilter.addAction(Constants.BLUETOOTH_SHOW_WAITDIALOG);
        intentFilter.addAction(Constants.BLUETOOTH_DISMISS_WAITDIALOG);
        intentFilter.addAction(Constants.BLUETOOTH_PBAP_CONNECTION_STATE);
        intentFilter.addAction(Constants.BLUETOOTH_PBAP_CONNECT_TIMEOUT);
        intentFilter.addAction(Constants.BLUETOOTH_PULL_PHONE_BOOK);
        intentFilter.addAction(Constants.BLUETOOTH_ACL_DISCONNECTED);
        intentFilter.addAction(Constants.ACTION_NEW_BLUETOOTH_DEVICE);
        intentFilter.addAction(Constants.ACTION_CONNECTION_STATE_CHANGED);
//        intentFilter.addAction(Constants.ACTION_AG_CALL_CHANGED);
//        intentFilter.addAction(Constants.ACTION_BLUETOOTH_PHONE_END);
        intentFilter.addAction(Constants.BLUETOOTH_DEL_PHONE_BOOK_BEGIN);
        intentFilter.addAction(Constants.BLUETOOTH_DEL_PHONE_BOOK_END);
        intentFilter.addAction(Constants.BLUETOOTH_INSERT_PHONE_BOOK_BEGIN);
        intentFilter.addAction(Constants.BLUETOOTH_INSERT_PHONE_BOOK_END);
        intentFilter.addAction(Constants.BLUETOOTH_PULL_PHONE_BOOK_BEGIN);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
//        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        intentFilter.addAction(Constants.ACTION_COM_SENSORTEK_LEFT2RIGHT);
        intentFilter.addAction(Constants.ACTION_COM_SENSORTEK_RIGHT2LEFT);
        registerReceiver(mBluetoothPhoneReceiver, intentFilter);
    }

    private BroadcastReceiver mBluetoothPhoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logger.debug("[phone][{}]接收到蓝牙连接状态改变广播：" + action, log_step++);

            if (action.equals(Constants.BLUETOOTH_SHOW_CONNECT_FAIL)) {
                logger.debug("[phone][{}]连接失败删除旧通讯录...", log_step++);
                int headset = mAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
                //如果蓝牙电话连接已断开
                if (headset != BluetoothProfile.STATE_CONNECTED) {
                    //清空联系人缓存
                    BtPhoneUtils.clearContacts();
//                BtPhoneUtils.deleteContacts(context);
                }
            } else if (action.equals(Constants.BLUETOOTH_SHOW_WAITDIALOG)) {

            } else if (action.equals(Constants.BLUETOOTH_PBAP_CONNECTION_STATE)) {
                try {
                    BtPhoneUtils.pbapConnectState = mPbapService.GetPbapConnectStateStub();//intent.getIntExtra(Constants.BLUETOOTH_EXTRA_CONNECT_STATE, BtPhoneUtils.STATE_DISCONNECTED);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                logger.debug("[phone][{}] BluetoothAdapter mPbapConnectState: " + BtPhoneUtils.pbapConnectState, log_step++);
            } else if(action.equals(Constants.BLUETOOTH_PBAP_CONNECT_TIMEOUT)){
                BtPhoneUtils.mSyncState = BtPhoneUtils.SYNC_STATE.CONN_TIMEOUT;
            }else if (action.equals(Constants.BLUETOOTH_ACL_DISCONNECTED)) {

//                if(BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ACTIVE){
//                    Intent intentBtEnd = new Intent(Constants.ACTION_BLUETOOTH_PHONE_END);
//                    context.sendBroadcast(intentBtEnd);
//                }
            } else if (action.equals(Constants.BLUETOOTH_PULL_PHONE_BOOK)) {

            }else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)){
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if(null!=adapter){
                    int blueconState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, 0);
                    switch(blueconState){
                        case BluetoothAdapter.STATE_CONNECTED:
                            EventBus.getDefault().post(new DeviceEvent.BluetoothState(DeviceEvent.ON));
                            logger.debug("[phone][{}]蓝牙已经连接 state->" +
                                    ",EventBus post bluetooth state on", log_step++);
                            break;
                        case BluetoothAdapter.STATE_DISCONNECTED:
                            EventBus.getDefault().post(new DeviceEvent.BluetoothState(DeviceEvent.OFF));
                            logger.debug("[phone][{}]蓝牙已经断开 state->" +
                                    ",EventBus post bluetooth state off", log_step++);
                            break;
                    }
                }
            } else if (action.equals(Constants.ACTION_CONNECTION_STATE_CHANGED)) {
                int prevState = intent.getIntExtra(Constants.EXTRA_PREVIOUS_STATE, 0);
                int state = intent.getIntExtra(Constants.EXTRA_STATE, 0);

                BtPhoneUtils.connectionState = state;
                BtPhoneUtils.prevConnectionState = prevState;
                logger.debug("[phone][{}] HandsFreeClient prevState: " + prevState + " state: " + state, log_step++);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    //处理蓝牙电话连接
                    disposeBtConnected(context, device, state);
                }else if(prevState==BluetoothProfile.STATE_CONNECTED && state == BluetoothProfile.STATE_DISCONNECTED){
                    //处理蓝牙电话断开
                    disposeBtDisConnected(context, device, state);
                }
            } else if (action.equals(Constants.BLUETOOTH_DEL_PHONE_BOOK_BEGIN)) {
                logger.debug("[phone][{}]开始删除旧通讯录...", log_step++);
                BtPhoneUtils.mSyncState = BtPhoneUtils.SYNC_STATE.DEL_BEGIN;
            } else if (action.equals(Constants.BLUETOOTH_DEL_PHONE_BOOK_END)) {
                logger.debug("[phone][{}]删除旧通讯录完成.", log_step++);
                BtPhoneUtils.mSyncState = BtPhoneUtils.SYNC_STATE.DEL_END;
            } else if (action.equals(Constants.BLUETOOTH_PULL_PHONE_BOOK_BEGIN)) {
                logger.debug("[phone][{}]手动触发获取蓝牙通讯录...BtPhoneUtils.pbapConnectState:"+BtPhoneUtils.pbapConnectState, log_step++);
                if(BtPhoneUtils.STATE_DISCONNECTED == BtPhoneUtils.pbapConnectState){
                    pullPhoneBook();
                }
            } else if (action.equals(Constants.BLUETOOTH_INSERT_PHONE_BOOK_BEGIN)) {
                logger.debug("[phone][{}]同步完成开始保存通讯录...", log_step++);
                BtPhoneUtils.mSyncState = BtPhoneUtils.SYNC_STATE.INSERT_BEGIN;
            } else if (action.equals(Constants.BLUETOOTH_INSERT_PHONE_BOOK_END)) {
                logger.debug("[phone][{}]保存通讯录完成.", log_step++);
                BtPhoneUtils.mSyncState = BtPhoneUtils.SYNC_STATE.INSERT_END;
                mBtServiceHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //加载通讯录到缓存列表
                        logger.debug("[phone][{}]加载通讯录到缓存->开始.", log_step++);
                        BtPhoneUtils.QueryContacts(LauncherApplication.getContext());
                        logger.debug("[phone][{}]加载通讯录到缓存->完成.", log_step++);
                        BtPhoneUtils.mSyncState = BtPhoneUtils.SYNC_STATE.SYNC_END;
                        //广播同步完成
                        Intent syncEnd = new Intent(Constants.BLUETOOTH_SYNC_PHONE_BOOK_END);
                        context.sendBroadcast(syncEnd);
                    }
                });
            }else if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                // 状态改变的广播
                 BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                 logger.debug("device.getName():"+device.getName() + ",device.getBondState():"+device.getBondState());

            }else if(action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)){
                //处理蓝牙配对请求
                disposeBtPairRequest(context, intent);
            }else if(BluetoothDevice.ACTION_FOUND.equals(action)){
                //发现附近的蓝牙设备
                if(null!=intent){
                    BluetoothDevice device = intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // 搜索到的是已经绑定的蓝牙设备
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        logger.debug("[phone][{}]蓝牙搜索发现已经绑定的蓝牙设备name:"+device.getName()+",addr:"+device.getAddress(), log_step++);
                        mFondDevices.add(device);
                    }
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                if (null != mAdapter) {
                    mAdapter.cancelDiscovery();
                    //搜索完成
                    connectLastDevice(mAdapter);
                }
            }else if(Constants.ACTION_COM_SENSORTEK_LEFT2RIGHT.equals(action)){
                logger.debug("BtPhoneUtils.btCallState:"+BtPhoneUtils.btCallState);
                if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ACTIVE ||
                        BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_DIALING) {
                    BtPhoneUtils.btCallState = BtPhoneUtils.CALL_STATE_TERMINATED;//设置蓝牙电话状态为挂断
                    //挂断电话
                    logger.debug("[phone][{}]挂断电话 左往右");
                    Intent intent_sensortek = new Intent(Constants.BLUETOOTH_CALL_TERMINATION);
                    sendBroadcast(intent_sensortek);
                } else if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_INCOMING) {
                    //接听电话 左往右
                    logger.debug("[phone][{}]接听电话 左往右");
                    Intent intent_sensortek = new Intent(Constants.BLUETOOTH_CALL_ACCEPT);
                    sendBroadcast(intent_sensortek);
                }
            }else if(Constants.ACTION_COM_SENSORTEK_RIGHT2LEFT.equals(action)){
                logger.debug("BtPhoneUtils.btCallState:"+BtPhoneUtils.btCallState);
                if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ACTIVE ||
                        BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_DIALING) {
                    //挂断电话
                    logger.debug("[phone][{}]挂断电话 右往左");
                    BtPhoneUtils.btCallState = BtPhoneUtils.CALL_STATE_TERMINATED;//设置蓝牙电话状态为挂断
                    Intent intent_sensortek = new Intent(Constants.BLUETOOTH_CALL_TERMINATION);
                    sendBroadcast(intent_sensortek);
                } else if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_INCOMING) {
                    //拒接电话 右往左
                    logger.debug("[phone][{}]拒接电话 右往左");
                    BtPhoneUtils.btCallState = BtPhoneUtils.CALL_STATE_TERMINATED;//设置蓝牙电话状态为挂断
                    Intent intent_sensortek = new Intent(Constants.BLUETOOTH_CALL_REJECT);
                    sendBroadcast(intent_sensortek);
                }
            }
        }
    };

    /**
     * 获取已经连接的设备
     * @param bluetoothAdapter
     */
    private void getConnectedDevice(BluetoothAdapter bluetoothAdapter){
//        int a2dp = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP);
//        int headset = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
//        int health = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEALTH);
        //获取蓝牙协议连接状态
        int flag = BtPhoneUtils.getProfileConnectedState(bluetoothAdapter);

        if(flag!=-1){
            bluetoothAdapter.getProfileProxy(LauncherApplication.getContext(), new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    List<BluetoothDevice> mDevices = proxy.getConnectedDevices();
                    if (mDevices != null && mDevices.size() > 0) {
                        for (BluetoothDevice device : mDevices) {
                            logger.debug("device name:" + device.getName());
                        }
                        mConnDevices = mDevices;
                    }
                }
                @Override
                public void onServiceDisconnected(int profile) {
                    logger.debug("device null profile:" + profile);
                }
            },flag);
        }
    }

    /**
     * 连接在配对设备列表中上次连接过的设备
     * */
    private void connectLastDevice(BluetoothAdapter btAdapter){
        String lastAddr = SharedPreferencesUtil.getStringValue(getApplicationContext(), Constants.KEY_LAST_BLUETOOTH_CLIENT_ADDRESS, "");
        int flag = BtPhoneUtils.getProfileConnectedState(btAdapter);
        logger.debug("[phone][{}]上次配对的设备 lastAddr:" + lastAddr, log_step++);
        if(null!=mConnDevices && mConnDevices.size()>0 && flag!=-1){
            setDevice(mConnDevices.get(0).getAddress());
        }else if(!TextUtils.isEmpty(lastAddr)){
            for (String addr : mBondedDevices) {
                if (addr.equals(lastAddr)) {

                    setDevice(addr);
                    return;
                }
            }
        }
    }

    /**
     * 处理蓝牙配对请求
     * @param context
     * @param intent
     */
    private void disposeBtPairRequest(Context context, Intent intent){
        //开始设置静音模式
        AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        //静音模式
        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        //广播取消BtPhone的循环重连
        Intent syncEnd = new Intent(Constants.BLUETOOTH_CANCEL_CHECK_SERVICE);
        context.sendBroadcast(syncEnd);

        int varient = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1);
        logger.debug("[phone][{}]蓝牙配对请求.varient:"+varient, log_step++);
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device != null) {
            try {
                logger.debug("[phone][{}]请求设备"+device.getName()+" 的蓝牙地址"+device.getAddress(), log_step++);
                //自动确认配对
                device.setPairingConfirmation(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //延时恢复声音正常模式
        mBtServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //声音模式
                if(null!=audioManager){
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                }
            }
        });
    }

    /**
     * 蓝牙连接的处理
     * @param context
     * @param device
     * @param state
     */
    private void disposeBtConnected(Context context, BluetoothDevice device, int state){
        BtPhoneUtils.mSyncState = BtPhoneUtils.SYNC_STATE.DEFAULT;
        EventBus.getDefault().post(new DeviceEvent.BluetoothState(DeviceEvent.ON));
        logger.debug("[phone][{}]蓝牙HRF连接成功 state->" + state +
                ",BtPhoneUtils.mSyncState->" + BtPhoneUtils.mSyncState +
                ",EventBus post bluetooth state on", log_step++);
        if (null != mPbapService) {
            try {
                //停止BtPhone 对状态检查
                logger.debug("停止BtPhone 对状态检查");
                mPbapService.startCheckConnectStub(0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (device != null) {

            logger.debug("[phone][{}]device name: " + device.getName() + " device address: " +
                    device.getAddress() + " device type: " + device.getType(), log_step++);

            mBtServiceHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
//                        logger.debug("[phone][{}]蓝牙通讯录同步前设置设备", log_step++);
//                        setDevice(device.getAddress());
                        //延时去发起同步通讯录
                        Thread.sleep(1000);
                        if(BtPhoneUtils.STATE_DISCONNECTED == BtPhoneUtils.pbapConnectState||
                                BtPhoneUtils.CALL_STATE_DEFAULT == BtPhoneUtils.pbapConnectState){
                            //获取上次连接蓝牙的地址
                            BtPhoneUtils.mLastDisConnectedBtAddr = SharedPreferencesUtil.getStringValue(context, Constants.KEY_LAST_BLUETOOTH_CLIENT_ADDRESS,"");
                            //根据断开的时间和是否同一台蓝牙设备判定是否需要同步通讯录
                            if(BtPhoneUtils.checkTimePullPhoneBook(System.currentTimeMillis(),device.getAddress())){
                                logger.debug("[phone][{}]需要同步通讯录", log_step++);
                                pullPhoneBook();
                            }else{
                                logger.debug("[phone][{}]无需同步通讯录，使用缓存通讯录.", log_step++);
                                BtPhoneUtils.mSyncState = BtPhoneUtils.SYNC_STATE.SYNC_END;
                                //广播同步完成
                                Intent syncEnd = new Intent(Constants.BLUETOOTH_SYNC_PHONE_BOOK_END);
                                context.sendBroadcast(syncEnd);
                            }
                            //缓存本次蓝牙地址作为下次自动连接默认地址
                            SharedPreferencesUtil.putStringValue(context, Constants.KEY_LAST_BLUETOOTH_CLIENT_ADDRESS,
                                    device.getAddress());
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * 蓝牙断开的处理
     * @param context
     * @param device
     * @param state
     */
    private void disposeBtDisConnected(Context context, BluetoothDevice device, int state){
        BtPhoneUtils.mSyncState = BtPhoneUtils.SYNC_STATE.DEFAULT;
        EventBus.getDefault().post(new DeviceEvent.BluetoothState(DeviceEvent.OFF));
        logger.debug("[phone][{}]蓝牙HRF连接断开 state->" + state +
                ",BtPhoneUtils.mSyncState->" + BtPhoneUtils.mSyncState +
                ",EventBus post bluetooth state off", log_step++);
        if (null != mPbapService) {
            try {
                logger.debug("启动BtPhone 对状态检查");
                mPbapService.startCheckConnectStub(1);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        //缓存本次蓝牙断开的时间
        SharedPreferencesUtil.putLongValue(context, Constants.KEY_LAST_BLUETOOTH_DISCONN_TIME,
                System.currentTimeMillis());
//        if(BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ALERTING||
//                BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_INCOMING||
//                BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ACTIVE||
//                BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_WAITING){
//            //如果在通话中状态
//            stopBtCalling();
//        }
        //清空联系人缓存
//        BtPhoneUtils.clearContacts();
    }
    private void stopBtCalling(){
        logger.debug("stopBtCalling()");
        Intent intent = new Intent(Constants.ACTION_BLUETOOTH_PHONE_END);
        sendBroadcast(intent);
    }
    private void setDevice(String address) {

        if(!BluetoothAdapter.checkBluetoothAddress(address)){
            logger.debug("蓝牙地址无效");
            return;
        }
        Intent intent = new Intent(Constants.BLUETOOTH_SET_DEVICE);
        sendBroadcast(intent);
        try {
            if (null != mPbapService) {
                logger.debug("mPbapService.setDeviceStub([{}])", address);
                mPbapService.setDeviceStub(address);
            }else{
                Intent intent2 = new Intent(Constants.BLUETOOTH_PULL_PHONE_BOOK_FAILED);
                sendBroadcast(intent2);
            }
        } catch (NullPointerException e) {
            logger.error("[{}]蓝牙通讯录设置设备失败,NullPointerException...",log_step++);
            Intent intent3 = new Intent(Constants.BLUETOOTH_PULL_PHONE_BOOK_FAILED);
            sendBroadcast(intent3);
        } catch (RemoteException e){
            logger.error("[{}]蓝牙通讯录设置设备失败,RemoteException...",log_step++);
            Intent intent4 = new Intent(Constants.BLUETOOTH_PULL_PHONE_BOOK_FAILED);
            sendBroadcast(intent4);
        }
    }

    private synchronized void pullPhoneBook() {
        try {
            if (null != mPbapService) {
                mPbapService.PullphonebookStub();
            }else{
                Intent intent2 = new Intent(Constants.BLUETOOTH_PULL_PHONE_BOOK_FAILED);
                sendBroadcast(intent2);
            }
        } catch (RemoteException e) {
            Intent intent3 = new Intent(Constants.BLUETOOTH_PULL_PHONE_BOOK_FAILED);
            sendBroadcast(intent3);
            logger.error("[{}]蓝牙获取通讯录失败...",log_step++);
        }
    }
}
