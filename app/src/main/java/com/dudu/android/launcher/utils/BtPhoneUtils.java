package com.dudu.android.launcher.utils;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.dudu.aios.ui.base.T;
import com.dudu.aios.ui.bt.Contact;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.commonlib.utils.File.SharedPreferencesUtil;
import com.dudu.event.DeviceEvent;
import com.dudu.voice.VoiceManagerProxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by Chad on 2016/4/9.
 */
public class BtPhoneUtils {
    private static final String TAG = "phone.BtPhoneUtils";
    /**
     * 蓝牙电话状态
     */
    public static int btCallState = -1;//默认设置-1为初始状态

    /**
     * 拨出的电话来源
     */
    public static int btCallOutSource = -1;
    public static final int BTCALL_OUT_SOURCE_DEFAULT = -1;
    /**
     * 手机端拨出电话
     */
    public static final int BTCALL_OUT_SOURCE_MOBILE = 1;
    /**
     * 后视镜键盘拨出电话
     */
    public static final int BTCALL_OUT_SOURCE_KEYBOARD = 2;
    /**
     * 语音呼叫号码拨出电话
     */
    public static final int BTCALL_OUT_SOURCE_VOIC = 3;

    /**
     * 蓝牙连接状态
     */
    public static int connectionState = -1;
    public static int prevConnectionState = -1;

    /**
     * 蓝牙电话通讯录同步连接状态
     */
    public static int pbapConnectState = -1;

    /**
     * The profile is in disconnected state
     */
    public static final int STATE_DISCONNECTED = 0;
    /**
     * The profile is in connecting state
     */
    public static final int STATE_CONNECTING = 1;
    /**
     * The profile is in connected state
     */
    public static final int STATE_CONNECTED = 2;
    /**
     * The profile is in disconnecting state
     */
    public static final int STATE_DISCONNECTING = 3;


    /* Call state */
    /**
     * Call default.
     */
    public static final int CALL_STATE_DEFAULT = -1;/**
     * Call is active.
     */
    public static final int CALL_STATE_ACTIVE = 0;
    /**
     * Call is in held state.
     */
    public static final int CALL_STATE_HELD = 1;
    /**
     * Outgoing call that is being dialed right now.
     */
    public static final int CALL_STATE_DIALING = 2;
    /**
     * Outgoing call that remote party has already been alerted about.
     */
    public static final int CALL_STATE_ALERTING = 3;
    /**
     * Incoming call that can be accepted or rejected.
     */
    public static final int CALL_STATE_INCOMING = 4;
    /**
     * Waiting call state when there is already an active call.
     */
    public static final int CALL_STATE_WAITING = 5;
    /**
     * Call that has been held by response and hold
     * (see Bluetooth specification for further references).
     */
    public static final int CALL_STATE_HELD_BY_RESPONSE_AND_HOLD = 6;
    /**
     * Call that has been already terminated and should not be referenced as a valid call.
     */
    public static final int CALL_STATE_TERMINATED = 7;

    /**
     * Use a simple string represents the long.
     */
    private static final String COLUMN_CONTACT_ID =
            ContactsContract.Data.CONTACT_ID;
    private static final String COLUMN_RAW_CONTACT_ID =
            ContactsContract.Data.RAW_CONTACT_ID;
    private static final String COLUMN_MIMETYPE =
            ContactsContract.Data.MIMETYPE;
    private static final String COLUMN_NAME =
            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME;
    private static final String COLUMN_NUMBER =
            ContactsContract.CommonDataKinds.Phone.NUMBER;
    private static final String COLUMN_NUMBER_TYPE =
            ContactsContract.CommonDataKinds.Phone.TYPE;
    private static final String MIMETYPE_STRING_NAME =
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE;
    private static final String MIMETYPE_STRING_PHONE =
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE;
    public static final String WLD_NAME = "wld_name";
    public static final String WLD_TYPE = "wld_type";
//    private static Logger logger = LoggerFactory.getLogger("phone");

    /**
     * 加载通讯录的状态
     */
    public enum SYNC_STATE {
        DEFAULT,//默认值
        DEL_BEGIN,//删除开始
        DEL_END,//删除结束
        INSERT_BEGIN,//插入开始
        INSERT_END,//插入结束
        SET_DEVICE,//准备同步
        SYNC_END, //同步完成
        SYNC_FAIL, //同步失败
        CONN_TIMEOUT//同步超时
    }
    public static SYNC_STATE mSyncState = SYNC_STATE.DEFAULT;

    private enum OPER_STATE{
        OPER_LIST_SYNC_BEGIN,//加载前
        OPER_LIST_SYNC_END,//加载后
        OPER_LIST_CLEAN//清除
    }
    /**
     * 联系人列表操作状态
     * 0未同步，1同步完成，2执行了清除
     */
    private static OPER_STATE mContactListOperState = OPER_STATE.OPER_LIST_CLEAN;
    private static List<Contact> mContactList;//联系人列表

    public static int mDialCounter = 0;//拨号次数
    public static long mLastDialTime = 0;//上次拨号时间
    public static long mLastDisConnectedBtTime = 0;//上次断开连接时的时间
    public static String mLastDisConnectedBtAddr = "";//上次断开连接时的地址

    public static boolean isSyncBt = false;
    private static int mAudioStateChangeConn = -1;//蓝牙连接中的音频切换(默认-1，蓝牙连接时切换为0，蓝牙连接断开时切换为1)
    /**
     * sco音频状态
     */
    private static int mPreAudioState;/**
     * sco音频状态
     */
    private static int mAudioState;
    public static boolean mForceEnd = false;//强制挂断
    /**
     * 建立连接后根据当前连接时间和上次断开的时间，是否同一个设备连接，如果是同一个设备5分钟内不做同步通讯录
     * @param connectedTime
     * @param currentBtAddr
     * @return false 不需要同步
     */
    public static boolean checkTimePullPhoneBook(long connectedTime, String currentBtAddr){
        boolean ret = false;
        Log.d(TAG,"mLastDisConnectedBtAddr:" + mLastDisConnectedBtAddr + ",currentBtAddr:" + currentBtAddr);
        //缓存本次蓝牙断开的时间
        mLastDisConnectedBtTime = SharedPreferencesUtil.getLongValue(LauncherApplication.getContext(), Constants.KEY_LAST_BLUETOOTH_DISCONN_TIME,0);
        if(mLastDisConnectedBtAddr.equals(currentBtAddr)){
            long value = connectedTime - mLastDisConnectedBtTime;
            Log.d(TAG,"connectedTime:" + connectedTime + ",mLastDisConnectedBtTime:" + mLastDisConnectedBtTime + ", value:" + value);
            Log.d(TAG,"mContactList:" + (null==mContactList?"null":mContactList.size()));
            //同一个设备判定断开时间，超过5分钟则需要同步
            if(value>(5*60*1000)){
                Log.d(TAG,"断开时间超过5分钟");
                ret = true;
            } else if(null==mContactList || mContactList.size()==0){
                ////如果缓存通讯录为空，先去数据库查找
                QueryContacts(LauncherApplication.getContext());
                Log.d(TAG,"mContactList:" + (null==mContactList?"null":mContactList.size()));
                //如果缓存通讯录依然为空
                if(null==mContactList || mContactList.size()==0){
                    ret = true;
                }
            }
            Log.d(TAG,"mContactList:" + (null==mContactList?"null":mContactList.size()));
        }else{
            //不是同一个设备就需要同步
            ret = true;
        }

        return ret;
    }

    public static boolean isCalling() {
        return (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ACTIVE ||
                BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_DIALING ||
                BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_INCOMING);
    }

    /**
     * @param context
     * @param name The contact who you getDefaultConfig the id from. The name of
     * the contact should be set.
     * @return 0 if contact not exist in contacts list. Otherwise return
     * the id of the contact.
     */
    public static String getContactID(Context context, String name) {
        String id = "0";
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(
                android.provider.ContactsContract.Contacts.CONTENT_URI,
                new String[]{android.provider.ContactsContract.Contacts._ID},
                android.provider.ContactsContract.Contacts.DISPLAY_NAME +
                        "='" + name + "'", null, null);
        if (cursor.moveToNext()) {
            id = cursor.getString(cursor.getColumnIndex(
                    android.provider.ContactsContract.Contacts._ID));
        }
        if (null != cursor) {
            cursor.close();
        }
        return id;
    }

    /**
     * You must specify the contact's ID.
     *
     * @param contact
     * @throws Exception The contact's name should not be empty.
     */
    public static void addContact(Context context, Contact contact) {
//        Log.w(TAG, "**add start**");
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ContentResolver resolver = context.getContentResolver();
        String id = getContactID(context, contact.getName());
        if (!id.equals("0")) {
//            Log.d(TAG, "contact already exist. exit.");
        } else if (contact.getName().trim().equals("")) {
//            Log.d(TAG, "contact name is empty. exit.");
        } else {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(COLUMN_RAW_CONTACT_ID, 0)
                    .withValue(COLUMN_MIMETYPE, MIMETYPE_STRING_NAME)
                    .withValue(COLUMN_NAME, contact.getName())
                    .build());
//            Log.d(TAG, "add name: " + contact.getName());

            if (!contact.getNumber().trim().equals("")) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(COLUMN_RAW_CONTACT_ID, 0)
                        .withValue(COLUMN_MIMETYPE, MIMETYPE_STRING_PHONE)
                        .withValue(COLUMN_NUMBER, contact.getNumber())
//                        .withValue(COLUMN_NUMBER_TYPE, contact.getNumberType())
                        .build());
//                Log.d(TAG, "add number: " + contact.getNumber());
            }

            try {
                resolver.applyBatch(ContactsContract.AUTHORITY, ops);
                Log.d(TAG, "add success");
            } catch (Exception e) {
                Log.d(TAG, "add failed");
                Log.d(TAG, e.getMessage());
            }
        }

    }
    /**
     * 更新联系人信息
     * */
    /**
     * @param contactOld The contact wants to be updated. The name should exists.
     * @param contactNew
     */
    public static void updateContact(Context context, Contact contactOld, Contact contactNew) {
//        logger.trace("**update start**");
        ContentResolver resolver = context.getContentResolver();
        String id = getContactID(context, contactOld.getName());
        if (id.equals("0")) {
//            logger.trace(contactOld.getName()+" not exist.");
        } else if (contactNew.getName().trim().equals("")) {
//            logger.trace( "contact name is empty. exit.");
        } else if (!getContactID(context, contactNew.getName()).equals("0")) {
//            logger.trace( "new contact name already exist. exit.");
        } else {

            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

            //update name
            ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(COLUMN_CONTACT_ID + "=? AND " + COLUMN_MIMETYPE + "=?",
                            new String[]{id, MIMETYPE_STRING_NAME})
                    .withValue(COLUMN_NAME, contactNew.getName())
                    .build());
//            logger.trace( "update name: " + contactNew.getName());

            //update number
            if (!contactNew.getNumber().trim().equals("")) {
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(COLUMN_CONTACT_ID + "=? AND " + COLUMN_MIMETYPE + "=?",
                                new String[]{id, MIMETYPE_STRING_PHONE})
                        .withValue(COLUMN_NUMBER, contactNew.getNumber())
//                        .withValue(COLUMN_NUMBER_TYPE, contactNew.getNumberType())
                        .build());
//                logger.trace("update number: " + contactNew.getNumber());
            }

            try {
                resolver.applyBatch(ContactsContract.AUTHORITY, ops);
                Log.d(TAG, "update success");
            } catch (Exception e) {
                Log.d(TAG, "update failed");
                Log.d(TAG, e.getMessage());
            }
        }
//        logger.trace( "**update end**");
    }

    /**
     * 查询指定电话的联系人姓名
     */
    public static String queryContactNameByNumber(Context context, final String phoneNum) throws Exception {
        if (null == phoneNum || "".equals(phoneNum)) {
            return "";
        }
        String name = "";
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, phoneNum); //根据电话号码查找联系人
//        Uri uri = Uri.parse("content://com.android.contacts/data/phones/filter/" + phoneNum);

        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(uri, new String[]{"display_name"}, null, null, null);
        if (null!=cursor && cursor.moveToFirst()) {
            name = cursor.getString(0);

            cursor.close();
        }
        return name;
    }

    /**
     * 在通讯录缓存里获取指定姓名的所有联系人
     * @param context
     * @param name_key
     * @return
     */
    public static List<Contact> obtainContactsByName(Context context, String name_key) {
        List<Contact> contacts = new ArrayList<>();
        if(null!=name_key && null!=mContactList && mContactList.size()>0){
            for(Contact contact:mContactList){
                if(contact.getName().equals(name_key)){
                    contacts.add(contact);
                }
            }
        }
        return contacts;
    }

    /**
     * 在通讯录缓存里获取指定号码的联系人
     * @param number
     * @return
     */
    public static Contact getContactByNumber(String number){
        Contact ret = null;
        String number_input = number.replace("-","").replace(" ","");
        String contact_number = "";
        if(!TextUtils.isEmpty(number_input) && null!=mContactList && mContactList.size()>0
                && mContactListOperState!=OPER_STATE.OPER_LIST_SYNC_BEGIN){
            synchronized (mContactList){

                for(Contact contact:mContactList){
                    contact_number = contact.getNumber().replace("-","").replace(" ","");
                    if(contact_number.equals(number_input)){
                        ret = contact;
                        break;
                    }
                }
            }
        }
        return ret;
    }
    /**
     * 从数据库里查询获取指定姓名的所有联系人
     *
     * @param context
     * @return
     */
    public static List<Contact> queryContactsByName(Context context, String name_key) {
        List<Contact> contacts = new ArrayList<>();
        if (null == context || TextUtils.isEmpty(name_key)) {
            return contacts;
        }
        Cursor cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                null, ContactsContract.Contacts.DISPLAY_NAME + " = ?",
                new String[]{name_key + ""}, ContactsContract.Contacts.DISPLAY_NAME + " desc");
        int contactIdIndex = 0;
        int nameIndex = 0;

        //chad modified begin
        if (null == cursor) {
            return contacts;
        }//chad modified end

        if (cursor.getCount() > 0) {
            contactIdIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        }
        while (cursor.moveToNext()) {
            String contactId = cursor.getString(contactIdIndex);
            String name = cursor.getString(nameIndex);
            if (name.equals(name_key)) {

                Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId,
                        null, null);
                int phoneIndex = 0;
                if (phones.getCount() > 0) {
                    phoneIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                }
                while (phones.moveToNext()) {
                    String phoneNumber = phones.getString(phoneIndex);
                    Contact contact = new Contact();
                    contact.setId(Integer.parseInt(contactId));
                    contact.setName(name);
                    contact.setNumber(phoneNumber);
                    contacts.add(contact);
                }
                if (null != phones) {
                    phones.close();
                }
            }
        }
        if (null != cursor) {
            cursor.close();
        }
        return contacts;
    }

    /**
     * 查询所有联系人
     * @param context
     * @return
     */
    public static List<Contact> QueryContacts(Context context) {
        if(null==mContactList){
            mContactList = new ArrayList<>();
        }else {
            clearContacts();
        }

        try {
            mContactListOperState = OPER_STATE.OPER_LIST_SYNC_BEGIN;
            //ContentResolver查询方法原型为:
            //public final Cursor query (Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
            // ContactsContract.Contacts._ID+"<?"//new String[]{"12"} //ContactsContract.Contacts.DISPLAY_NAME
            for(int i=0;i<700;i++){//默认查询不超过7万条，实际最多只有65535条

                Cursor cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                        null, null, null, ContactsContract.Contacts._ID + " desc  limit 100 offset "+100*i);

                if (null==cursor || cursor.getCount()==0 || null==context) {
                    return mContactList;
                }

                Log.d(TAG, "query contacts database cursor.getCount():" + cursor.getCount());
                if (cursor.moveToFirst()) {
                    //查找联系人下的号码
                    addContactToContactList(context, cursor);
                }

                if (null != cursor) {
                    cursor.close();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            mContactListOperState = OPER_STATE.OPER_LIST_SYNC_END;
        }
        return mContactList;
    }

    /**
     * 查找联系人下的号码
     * @param context
     * @param cursor
     */
    private static void addContactToContactList(Context context, Cursor cursor){
        int contactIdIndex = 0;
        int nameIndex = 0;
        if (cursor.getCount() > 0) {
            contactIdIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        }
        do {
            try {
                String contactId = cursor.getString(contactIdIndex);
                String name = cursor.getString(nameIndex);
//                Log.d(TAG, "Contacts._ID contactIdIndex:"+contactIdIndex+",contactId:"+contactId + ", name:"+name);
                Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId,
                        null, null);

                int phoneIndex = 0;
                if (null != phones) {

                    if (phones.getCount() > 0) {
                        phoneIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    }
                    if (phones.moveToFirst()) {
                        do {
                            String phoneNumber = phones.getString(phoneIndex).replace("-", "").replace(" ", "");
                            Contact contact = new Contact();
                            contact.setId(Integer.parseInt(contactId));
                            contact.setName(name);
                            contact.setNumber(phoneNumber);
                            mContactList.add(contact);
                        } while (phones.moveToNext());
                    }
                }
                if (null != phones) {
                    phones.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        } while (cursor.moveToNext());
    }


    /**
     * 获取所有联系人列表
     *
     * @param context
     * @return
     */
    public static List<Contact> obtainContacts(Context context) {

        if(null==mContactList){
            mContactList = new ArrayList<>();
        }

        return mContactList;
    }

    /**
     * 清空联系人缓存
     */
    public static void clearContacts(){
        if(null!=mContactList){
            if(mContactListOperState != OPER_STATE.OPER_LIST_SYNC_BEGIN){
                mContactListOperState = OPER_STATE.OPER_LIST_CLEAN;
                mContactList.clear();
            }
        }
    }
    /**
     * 清除通讯录记录
     */
    public static void doClearContacts(Context context) {

        ContentResolver resolver = context.getContentResolver();
        String where = ContactsContract.RawContacts.ACCOUNT_NAME + "= ?"
                + " AND " + ContactsContract.RawContacts.ACCOUNT_TYPE + "=?";
        resolver.delete(ContactsContract.RawContacts.CONTENT_URI, where,
                new String[]{WLD_NAME, WLD_TYPE});
    }

    public static void deleteContacts(Context context){
        ContentResolver resolver = context.getContentResolver();
        resolver.delete(ContactsContract.RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,"true").build(), null, null);
    }

    /**
     * 设置蓝牙电话音量
     * @type
     * STREAM_VOICE_CALL-- 通话，
     *STREAM_SYSTEM -- 系统音量,
     *STREAM_RING -- 铃声
     *STREAM_MUSIC -- 音乐,
     *STREAM_ALARM -- 闹钟 ,
     *STREAM_NOTIFICATION -- 通知,
     *STREAM_BLUETOOTH_SCO -- 蓝牙通话
     * @param value
     */
    public static void setBtPhoneVolume(Context context, int type, int value){
        //STREAM_BLUETOOTH_SCO -- 蓝牙通话
        AudioManager audiomanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if(null!=audiomanager){
            int currentVolume = audiomanager.getStreamVolume(type/*Constants.STREAM_BLUETOOTH_SCO AudioManager.STREAM_BLUETOOTH_SCO*/); // 获取当前值
            audiomanager.setParameters("hfp_volume="+currentVolume);
            audiomanager.setStreamVolume(type, value, 0);
        }
    }

    /**
     * 获取蓝牙电话最大音量
     * @param context
     * @type
     * *STREAM_VOICE_CALL-- 通话，
     *STREAM_SYSTEM -- 系统音量,
     *STREAM_RING -- 铃声
     *STREAM_MUSIC -- 音乐,
     *STREAM_ALARM -- 闹钟 ,
     *STREAM_NOTIFICATION -- 通知,
     *STREAM_BLUETOOTH_SCO -- 蓝牙通话
     * @return
     */
    public static int getBtPhoneMaxVolume(Context context, int type){
        //STREAM_BLUETOOTH_SCO -- 蓝牙通话
        AudioManager audiomanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = 0 ;
        if(null!=audiomanager) {
            maxVolume  = audiomanager.getStreamMaxVolume(type); // 获取当前值
        }
        return maxVolume;
    }

    /**
     * 获取蓝牙电话当前音量
     * @param context
     * @param streamType
     * @return
     */
    public static int getBtPhoneCurrentVolume(Context context, int streamType){
        //STREAM_BLUETOOTH_SCO -- 蓝牙通话
        AudioManager audiomanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = 0 ;
        if(null!=audiomanager) {
            currentVolume  = audiomanager.getStreamVolume(streamType/*AudioManager.STREAM_BLUETOOTH_SCO*/); // 获取当前值
        }
        return currentVolume;
    }

    public static void initAudio(Context context, int mode){
        //STREAM_BLUETOOTH_SCO -- 蓝牙通话
        AudioManager audiomanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if(null!=audiomanager){
            Log.d(TAG,"set audio mode : "+mode);
            //设置声音模式
            if(audiomanager.getMode()!=mode){
                audiomanager.setMode(mode);
            }
            //打开麦克风
            audiomanager.setMicrophoneMute(false);
            //打开扬声器
            audiomanager.setSpeakerphoneOn(true);
        }
    }
    /**
     * 与设备配对 参考源码：platform/packages/apps/Settings.git
     * \Settings\src\com\android\settings\bluetooth\CachedBluetoothDevice.java
     */
    public static boolean createBond(Class btClass, BluetoothDevice btDevice) throws Exception {
        Method createBondMethod = btClass.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    /**
     * 与设备解除配对 参考源码：platform/packages/apps/Settings.git
     * \Settings\src\com\android\settings\bluetooth\CachedBluetoothDevice.java
     */
    public static boolean removeBond(Class btClass, BluetoothDevice btDevice) throws Exception {
        Method removeBondMethod = btClass.getMethod("removeBond");
        Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    public static boolean setPin(Class btClass, BluetoothDevice btDevice, String str) throws Exception {
        try {
            Method removeBondMethod = btClass.getDeclaredMethod("setPin", new Class[]{byte[].class});
            Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice, new Object[]{str.getBytes()});
            Log.e("returnValue", "" + returnValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;

    }

    /**
     * 功能：取消用户输入
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean cancelPairingUserInput(Class btClass, BluetoothDevice device) throws Exception {
        Method createBondMethod = btClass.getMethod("cancelPairingUserInput");
        // cancelBondProcess()
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
    }

    /**
     * 功能：取消配对
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean cancelBondProcess(Class btClass, BluetoothDevice device) throws Exception {
        Method createBondMethod = btClass.getMethod("cancelBondProcess");
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
    }

    /**
     * 修改蓝牙设备名称
     * @param name
     * @return
     */
    public static boolean setBluetoothDeviceName(String name){
        Log.d(TAG," setBluetoothDeviceName : " + name);
        if(TextUtils.isEmpty(name) || "AIO Car".equals(name)||
            "AIOCar".equals(name.replace(" ",""))){
            Log.d(TAG, " format is not correct : " + name );
            return false;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (null != adapter && adapter.getState() == BluetoothAdapter.STATE_ON &&
                adapter.setName(name)) {
            Log.d(TAG, " setBluetoothDeviceName : " + name + " success");
            return true;
        } else {
            Log.d(TAG, " setBluetoothDeviceName : " + name + " fail");
        }
        return false;
    }

    /**
     * 获取当前设备的蓝牙名称
     * @return
     */
    public static String getBluetoothDeviceName(){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (null == adapter) {
            return null;
        }
        return adapter.getName();
    }

    /**
     * 设置蓝牙名称
     */
    public static void initBluetoothDeviceName() {
        String imei = DeviceIDUtil.getIMEI(LauncherApplication.getContext());
        Log.d(TAG,"bluetooth update name 3 imei:" + imei);
        if(!TextUtils.isEmpty(imei) && imei.length()>3){
            String name = BtPhoneUtils.getBluetoothDeviceName();
            if("AIO Car".equals(name)|| TextUtils.isEmpty(name)||
                    "AIOCar".equals(name.replace(" ",""))){
                String temp = "AIO Car "+imei.substring(imei.length()-4,imei.length());
                Log.d(TAG,"bluetooth update name 4 imei:" + imei + " , set bluetooth name as:" + temp);
                boolean ret = BtPhoneUtils.setBluetoothDeviceName(temp);
                Log.d(TAG, "bluetooth update name 5 ret->" + ret);
            }
        }
    }
    /**
     * 设置蓝牙可见性
     *
     * @param timeout
     */
    public static void setDiscoverableTimeout(int timeout) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, timeout);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据包名关掉进程
     *
     * @param packageName
     */
    public static void killProcessByName(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        am.killBackgroundProcesses(packageName);  //应用的包名
    }

    /**
     * 判断某个服务是否正在运行的方法
     *
     * @param mContext
     * @param serviceName 是包名+服务的类名（例如：net.loonggg.testbackstage.TestService）
     * @return true代表正在运行，false代表服务没有正在运行
     */
    public static boolean isServiceWork(Context mContext, String serviceName) {
        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(40);
        if (myList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }

    /**
     * 检查和更新蓝牙连接状态
     */
    public static void checkUpdateBluetoothConnectState(){
        //获取蓝牙电话连接状态
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
//        int a2dp = adapter.getProfileConnectionState(BluetoothProfile.A2DP);
//        int headset = adapter.getProfileConnectionState(BluetoothProfile.HEADSET);
//        int headset_client = adapter.getProfileConnectionState(16);
//        int input_device = adapter.getProfileConnectionState(4);
//        Log.d(TAG,"headset:" + headset + ", a2dp:" + a2dp + ",headset_client:" + headset_client + ", input_device:" + input_device);
        //连接
        if(getProfileConnectedState(adapter)!=-1){
            Log.v(TAG, "EventBus.getDefault().post on");
            EventBus.getDefault().post(new DeviceEvent.BluetoothState(DeviceEvent.ON));
        }
    }

    /**
     * 存在正在连接的设备
     * @param bluetoothAdapter
     * @return
     */
    public static int getProfileConnectedState(BluetoothAdapter bluetoothAdapter) {
        int flag = -1;
        if(null==bluetoothAdapter){
            return flag;
        }
        int a2dp = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP);
        int headset = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
        int health = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEALTH);
        Log.d(TAG,"BluetoothProfile.HEADSET:"+headset + ", BluetoothProfile.a2dp:"+a2dp +
        ",BluetoothProfile.HEALTH:" + BluetoothProfile.HEALTH);
        if (a2dp == BluetoothProfile.STATE_CONNECTED) {
            flag = a2dp;
        } else if (headset == BluetoothProfile.STATE_CONNECTED) {
            flag = headset;
        } else if (health == BluetoothProfile.STATE_CONNECTED) {
            flag = health;
        }
        return flag;
    }

    /**
     * 启动手势功能
     */
    public static void sensortekBroadcastEnable(){
        Log.v("BtPhoneUtils", "com.sensortek.broadcast.enable");
        Intent intentEnableGesture = new Intent();
        intentEnableGesture.setAction("com.sensortek.broadcast.enable");
        LauncherApplication.getContext().sendBroadcast(intentEnableGesture);
    }
    /**
     * 关闭手势功能
     */
    public static void sensortekBroadcastDisable(){
        Log.v("BtPhoneUtils", "com.sensortek.broadcast.disable");
        Intent intentDisableGesture = new Intent();
        intentDisableGesture.setAction("com.sensortek.broadcast.disable");
        LauncherApplication.getContext().sendBroadcast(intentDisableGesture);
    }
    /**
     * 格式电话号码
     * @param numberSrc
     * @return
     */
    public static String formatPhoneNumber(String numberSrc) {
        String number = "";
        if(TextUtils.isEmpty(numberSrc)){
            return number;
        }
        String iNumber = numberSrc.replace(" ","").replace("-","");
        try {
            if (iNumber.startsWith("1") && iNumber.length() == 11) {
                //1开头的中国大陆手机号
                number = iNumber.substring(0, 3) + "-" + iNumber.substring(3, 7) + "-" + iNumber.substring(7, 11);
            } else if (iNumber.startsWith("86") && iNumber.length() == 13) {
                //86开头的中国大陆手机号
                number = iNumber.substring(0, 5) + "-" + iNumber.substring(5, 9) + "-" + iNumber.substring(9, 13);
            } else if (iNumber.startsWith("+86") && iNumber.length() == 14) {
                //+86开头的中国大陆手机号
                number = iNumber.substring(0, 6) + "-" + iNumber.substring(6, 10) + "-" + iNumber.substring(10, 14);
            } else if ((iNumber.startsWith("010") ||iNumber.startsWith("020")||
                    iNumber.startsWith("021")||iNumber.startsWith("022")||
                    iNumber.startsWith("023")||iNumber.startsWith("024")||
                    iNumber.startsWith("025")||iNumber.startsWith("027")||
                    iNumber.startsWith("028")||iNumber.startsWith("029"))&& iNumber.length() == 11) {
                //0开头的中国大陆11位固话 北京市 010 广州市 020 上海市 021
                // 天津市 022 重庆市 023 沈阳市 024 南京市 025 武汉市 027 成都市 028 西安市 029
                number = iNumber.substring(0, 3) + "-" + iNumber.substring(3, 11);
            } else if (iNumber.startsWith("0") && iNumber.length() == 12) {
                //0开头的中国大陆12固话
                number = iNumber.substring(0, 4) + "-" + iNumber.substring(4, 12);
            }else {
                //其他号码因为格式多样不做处理
                number = iNumber;
            }
        }catch (Exception e){
            number = iNumber;
        }


        return number;
    }

    /**
     * 设置蓝牙音频切换状态
     * @param state 蓝牙连接中的音频切换(默认-1，蓝牙连接时切换为0，蓝牙连接断开时切换为1)
     */
    public static void setAudioStateChangeConn(int state){
        mAudioStateChangeConn = state;
        Log.d(TAG,"set mAudioStateChangeConn = " + state);
    }

    /**
     * 获取蓝牙音频切换状态
     * @return int state 蓝牙连接中的音频切换(默认-1，蓝牙连接时切换为0，蓝牙连接断开时切换为1)
     */
    public static int getAudioStateChangeConn(){
        Log.d(TAG,"get mAudioStateChangeConn = " + mAudioStateChangeConn);
        return mAudioStateChangeConn;
    }

    /**
     * 设置音频状态
     * @param state
     */
    public static void setAudioState(int state){
        mAudioState = state;
    }
    /**
     * 设置上一个音频状态
     * @param state
     */
    public static void setPreAudioState(int state){
        mPreAudioState = state;
    }
    /**
     * 获取音频状态
     * @return
     */
    public static int getAudioState(){
        return mAudioState;
    }

    /**
     * 获取音频状态
     * @return
     */
    public static int getPreAudioState(){
        return mPreAudioState;
    }
    public static void initISpeech(){
        Log.d(TAG,"初始化语音资源");
//        VoiceManagerProxy.getInstance().stopSpeaking();
//        VoiceManagerProxy.getInstance().stopUnderstanding();
        VoiceManagerProxy.getInstance().onInit();
//        VoiceManagerProxy.getInstance().startWakeup();
    }
    public static void releaseISpeech(){
        Log.d(TAG,"释放语音资源");
        VoiceManagerProxy.getInstance().stopSpeaking();
        VoiceManagerProxy.getInstance().stopUnderstanding();
        VoiceManagerProxy.getInstance().onDestroy();
    }
}
