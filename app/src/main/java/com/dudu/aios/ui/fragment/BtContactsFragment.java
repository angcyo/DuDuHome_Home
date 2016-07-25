package com.dudu.aios.ui.fragment;

import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.dudu.aios.ui.bt.CallRecord;
import com.dudu.aios.ui.bt.Contact;
import com.dudu.aios.ui.dialog.DeleteContactDialog;
import com.dudu.aios.ui.dialog.ShowContactDetailDialog;
import com.dudu.aios.ui.fragment.base.RBaseFragment;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.BtPhoneUtils;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.android.launcher.utils.cache.AsyncTask;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.TTSType;
import com.umeng.analytics.MobclickAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Robi on 2016-03-10 18:35.
 */
public class BtContactsFragment extends RBaseFragment implements View.OnClickListener {

    private BtContactsFragment mFragment;
    private ImageButton mButtonBack;

    private ImageButton mSearchContacts;
    private ImageView mInputDelete;

    private ImageButton mKeyBoardButton;
    private LinearLayout mLinearLayoutDialKeyboard;
    private LinearLayout mLinearLayoutContactsTitle;

    private ListView mRecordListView, mContactsListView;

    private EditText mEdittextContactKeyWord;
    private TextView mTextViewLoading;
    private EditText mEditTextBtName;
    private TextView mTextViewBtEdit;
    private RecordAdapter mRecordAdapter;

    private ContactsAdapter mContactsAdapter;

    private ArrayList<CallRecord> mRecordData;

    private ArrayList<Contact> mContactsData;
    private ArrayList<Contact> mContactsDataSearchResult;
    private IntentFilter mIntentFilter;
    private int TAG_NORMAL = 0;//正常状态
    private int TAG_FAIL = 1;//失败状态

    private Logger logger = LoggerFactory.getLogger("phone.BtContactsFragment");
    TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            logger.debug("beforeTextChanged()");
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            logger.debug("onTextChanged()");
        }

        @Override
        public void afterTextChanged(Editable s) {
            logger.debug("afterTextChanged()");
            //根据关键字查找联系人
            searchContact(s);
        }
    };
    public View.OnClickListener clkListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            MobclickAgent.onEvent(mBaseActivity, ClickEvent.click49.getEventId());
            logger.trace("item clk");
            replaceFragment(FragmentConstants.BT_OUT_CALL);
        }
    };
    private BroadcastReceiver mBluetoothPhoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logger.debug("通讯录界面收到的广播:" + action);
            if (action.equals(Constants.ACTION_CONNECTION_STATE_CHANGED)) {
                int prevState = intent.getIntExtra(Constants.EXTRA_PREVIOUS_STATE, 0);
                int state = intent.getIntExtra(Constants.EXTRA_STATE, 0);

                if (prevState == BluetoothProfile.STATE_CONNECTED && state == BluetoothProfile.STATE_DISCONNECTED) {

                    if (null != mContactsData && null != mContactsAdapter) {
                        mContactsData.clear();
                        mContactsAdapter.setData(mContactsData);
                        fixSoftInput();
                    }
                    mEdittextContactKeyWord.setText("");
                    mTextViewLoading.setVisibility(View.VISIBLE);
                    mTextViewLoading.setText(R.string.bt_noti_connect_waiting);
                    mContactsListView.setVisibility(View.INVISIBLE);
                } else if (state == BluetoothProfile.STATE_CONNECTED) {
                    mTextViewLoading.setVisibility(View.VISIBLE);
                    mTextViewLoading.setText(R.string.bt_noti_connected);
                    mContactsListView.setVisibility(View.INVISIBLE);
                }
            } else if (action.equals(Constants.BLUETOOTH_DEL_PHONE_BOOK_BEGIN)) {
                mTextViewLoading.setVisibility(View.VISIBLE);
                mTextViewLoading.setText(R.string.bt_contacts_sync_loading);
                mContactsListView.setVisibility(View.INVISIBLE);
            } else if (action.equals(Constants.BLUETOOTH_DEL_PHONE_BOOK_END)) {
                mTextViewLoading.setVisibility(View.VISIBLE);
                mTextViewLoading.setText(R.string.bt_contacts_sync_loading);
                mContactsListView.setVisibility(View.INVISIBLE);
            } else if (action.equals(Constants.BLUETOOTH_SET_DEVICE)) {
                mTextViewLoading.setVisibility(View.VISIBLE);
                mTextViewLoading.setTag(TAG_NORMAL);
                mTextViewLoading.setText(R.string.bt_device_prepare_connect);
                mContactsListView.setVisibility(View.INVISIBLE);
            } else if (action.equals(Constants.BLUETOOTH_INSERT_PHONE_BOOK_BEGIN)) {
                mTextViewLoading.setVisibility(View.VISIBLE);
                mTextViewLoading.setText(R.string.bt_contacts_sync_loading);
                mTextViewLoading.setTag(TAG_NORMAL);
                mContactsListView.setVisibility(View.INVISIBLE);
            } else if (action.equals(Constants.BLUETOOTH_INSERT_PHONE_BOOK_END)) {
                mTextViewLoading.setVisibility(View.VISIBLE);
                mTextViewLoading.setText(R.string.bt_contacts_sync_loading);
                mTextViewLoading.setTag(TAG_NORMAL);
                mContactsListView.setVisibility(View.INVISIBLE);
            } else if (action.equals(Constants.BLUETOOTH_SYNC_PHONE_BOOK_END)) {
                if (null != mFragment && mFragment.isVisible()) {
                    loadContactsData();//加载缓存通讯录，不需要异步线程处理new LoadBtTask().execute();
                }
            } else if (action.equals(Constants.BLUETOOTH_PBAP_CONNECT_TIMEOUT)) {
                BtPhoneUtils.mSyncState = BtPhoneUtils.SYNC_STATE.CONN_TIMEOUT;
                disposePbapConnTimeOut();
            } else if (action.equals(Constants.BLUETOOTH_PULL_PHONE_BOOK_FAILED) ||
                    action.equals(Constants.BLUETOOTH_SHOW_CONNECT_FAIL)) {
                if(BtPhoneUtils.connectionState!=BtPhoneUtils.STATE_CONNECTED){
                    mTextViewLoading.setVisibility(View.VISIBLE);
                    mTextViewLoading.setText(R.string.bt_noti_connect_waiting);
                    mContactsListView.setVisibility(View.INVISIBLE);
                }else {
                    BtPhoneUtils.mSyncState = BtPhoneUtils.SYNC_STATE.SYNC_FAIL;
                    mTextViewLoading.setVisibility(View.VISIBLE);
                    mTextViewLoading.setTag(TAG_FAIL);
                    mTextViewLoading.setText(R.string.bt_contacts_sync_fail);
                    mContactsListView.setVisibility(View.INVISIBLE);
                }
            } else if (action.equals(Constants.BLUETOOTH_PBAP_CONNECTION_STATE)) {
                //通讯录同步连接变化
                BtPhoneUtils.pbapConnectState = intent.getIntExtra(Constants.BLUETOOTH_EXTRA_CONNECT_STATE, BtPhoneUtils.STATE_DISCONNECTED);
                if (BtPhoneUtils.STATE_DISCONNECTED == BtPhoneUtils.pbapConnectState) {
                    //0为断开
                    if (BtPhoneUtils.mSyncState == BtPhoneUtils.SYNC_STATE.DEL_END && (null == mContactsData || mContactsData.size() == 0)) {
                        mTextViewLoading.setVisibility(View.VISIBLE);
                        mTextViewLoading.setTag(TAG_FAIL);
                        mTextViewLoading.setText(R.string.bt_contacts_sync_fail);
                        mContactsListView.setVisibility(View.INVISIBLE);
                    }
                }
            }else if(Constants.ACTION_AUDIO_STATE_CHANGED.equals(action)){
                hideInputMethod();
            }

        }
    };

    @Override
    protected int getContentView() {
        return R.layout.activity_bt_contacts;
    }

    private void initOnListener() {
        mButtonBack.setOnClickListener(this);
        mEdittextContactKeyWord.addTextChangedListener(textWatcher);
        mEdittextContactKeyWord.setClickable(true);
//        mSearchContacts.setOnClickListener(this);
        mInputDelete.setOnClickListener(this);
        mKeyBoardButton.setOnClickListener(this);
        mLinearLayoutDialKeyboard.setOnClickListener(this);
        mTextViewLoading.setOnClickListener(this);
        mTextViewBtEdit.setOnClickListener(this);
        mEditTextBtName.addTextChangedListener(btNameTextWatcher);
    }

    /**
     * 隐藏输入法
     */
    private void hideInputMethod(){
        if(null!=mEditTextBtName){
            InputMethodManager inputManager =
                    (InputMethodManager)mEditTextBtName.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(mEditTextBtName.getWindowToken(), 0);
        }
    }
    private void dial(Contact contact) {
        if (null != contact) {

            String number = contact.getNumber().replace("-", "");
            //拨出电话的广播在BtOutCallFragment中发出，
            //在BtCallReceiver中接收电话接通广播后显示通话中界面
            Bundle bundle = new Bundle();
            bundle.putString(Constants.EXTRA_PHONE_NUMBER, number);
            FragmentConstants.TEMP_ARGS = bundle;
            replaceFragment(FragmentConstants.BT_OUT_CALL);
        }
    }

    /**
     * 通讯录同步超时处理
     */
    private void disposePbapConnTimeOut(){
        if(null!=mTextViewLoading && null!=mContactsListView &&
                BtPhoneUtils.mSyncState == BtPhoneUtils.SYNC_STATE.CONN_TIMEOUT){
            mTextViewLoading.setVisibility(View.VISIBLE);
            mTextViewLoading.setText(R.string.bt_contacts_sync_fail);
            mTextViewLoading.setTag(TAG_FAIL);
            mContactsListView.setVisibility(View.INVISIBLE);
        }
    }
    /**
     * 根据关键字查找联系人
     *
     * @param s
     */
    private void searchContact(Editable s) {
        if (null == mContactsDataSearchResult) {
            mContactsDataSearchResult = new ArrayList<>();
        } else {
            mContactsDataSearchResult.clear();
        }
        if (null == s || TextUtils.isEmpty(s.toString())) {
            logger.debug("isEmpty()");
            //恢复旧联系人数据
            if (null != mContactsAdapter) {
                mContactsAdapter.setData(mContactsData);
                fixSoftInput();
            }
        } else {
            if (null != mContactsData && mContactsData.size() > 0) {
                String keyWord = s.toString().replace(" ", "");
                String content;
                Contact contactTemp = new Contact();
                for (Contact contact : mContactsData) {
                    contactTemp.setNumber(contact.getNumber());
                    contactTemp.setName(contact.getName());
                    contactTemp.setId(contact.getId());

                    content = contactTemp.getName().replace(" ", "");
                    if (content.contains(keyWord)) {

                        mContactsDataSearchResult.add(contact);
                    }
                    content = contactTemp.getNumber().replace("-", "").replace(" ", "");
                    if (content.contains(keyWord)) {
                        mContactsDataSearchResult.add(contact);
                    }
                }
                if (null != mContactsAdapter) {
                    mContactsAdapter.setData(mContactsDataSearchResult);
//                    fixSoftInput();
                }
            }
        }
    }

    private void fixSoftInput() {
        //注释掉
        /*if (mEdittextContactKeyWord != null) {
            logger.debug("fixSoftInput()1");
            mEdittextContactKeyWord.setVisibility(View.GONE);
            mEdittextContactKeyWord.postDelayed(() -> {
                logger.debug("fixSoftInput()2");
                mEdittextContactKeyWord.setVisibility(View.VISIBLE);
                mEdittextContactKeyWord.requestFocus();
                mEdittextContactKeyWord.requestLayout();
            }, 300);
        }*/
    }

    @Override
    protected void initView(View rootView) {
        mTextViewLoading = (TextView) mViewHolder.v(R.id.textView_loading);
        mEditTextBtName = (EditText) mViewHolder.v(R.id.editText_bt_name);
        mTextViewBtEdit = (TextView) mViewHolder.v(R.id.textView_bt_edit);
        mEdittextContactKeyWord = (EditText) mViewHolder.v(R.id.edittext_contact_key_word);
        mButtonBack = (ImageButton) mViewHolder.v(R.id.button_back);
        mSearchContacts = (ImageButton) mViewHolder.v(R.id.button_search_contacts);
        mInputDelete = (ImageView) mViewHolder.v(R.id.imageview_input_delete);
        mRecordListView = (ListView) mViewHolder.v(R.id.listView_call_record);
        mContactsListView = (ListView) mViewHolder.v(R.id.listView_contacts);
        mKeyBoardButton = (ImageButton) mViewHolder.v(R.id.button_dial_keyboard);
        mLinearLayoutDialKeyboard = (LinearLayout) mViewHolder.v(R.id.linearLayout_dial_keyboard);

        mTextViewLoading.setTag(TAG_NORMAL);
        mViewHolder.v(R.id.button_contacts).setSelected(true);
    }

    @Override
    protected void initViewData() {
        mRecordData = new ArrayList<>();
        mRecordAdapter = new RecordAdapter(mBaseActivity, mRecordData);
        mRecordListView.setAdapter(mRecordAdapter);

        mContactsData = new ArrayList<>();
        mContactsAdapter = new ContactsAdapter(mBaseActivity, mContactsData);
        mContactsAdapter.setOnClickListener(clkListener);
        mContactsListView.setAdapter(mContactsAdapter);
        mFragment = this;
        initOnListener();
        registerBcReceiver();
    }

    private void registerBcReceiver(){
        if (null == mIntentFilter) {
            mIntentFilter = new IntentFilter();
        }
        mIntentFilter.addAction(Constants.ACTION_CONNECTION_STATE_CHANGED);
        mIntentFilter.addAction(Constants.BLUETOOTH_PBAP_CONNECTION_STATE);
        mIntentFilter.addAction(Constants.BLUETOOTH_DEL_PHONE_BOOK_BEGIN);
        mIntentFilter.addAction(Constants.BLUETOOTH_DEL_PHONE_BOOK_END);
        mIntentFilter.addAction(Constants.BLUETOOTH_INSERT_PHONE_BOOK_BEGIN);
        mIntentFilter.addAction(Constants.BLUETOOTH_INSERT_PHONE_BOOK_END);
        mIntentFilter.addAction(Constants.BLUETOOTH_SYNC_PHONE_BOOK_END);
        mIntentFilter.addAction(Constants.BLUETOOTH_PBAP_CONNECT_TIMEOUT);
        mIntentFilter.addAction(Constants.BLUETOOTH_SET_DEVICE);
        mIntentFilter.addAction(Constants.BLUETOOTH_PULL_PHONE_BOOK_FAILED);
        mIntentFilter.addAction(Constants.BLUETOOTH_SHOW_CONNECT_FAIL);
        mIntentFilter.addAction(Constants.ACTION_AUDIO_STATE_CHANGED);

        mBaseActivity.registerReceiver(mBluetoothPhoneReceiver, mIntentFilter);
    }

    @Override
    public void onResume() {
        super.onResume();
        logger.debug("onResume()");
        //更新UI的蓝牙名称
        updateBtName();
        showContacts();
        disposePbapConnTimeOut();
    }

    @Override
    public void onPause() {
        super.onPause();
        logger.debug("onPause()");
        hideInputMethod();
    }

    @Override
    public void onShow() {
        super.onShow();
        logger.debug("onShow()");
        //更新UI的蓝牙名称
        updateBtName();
        showContacts();
        disposePbapConnTimeOut();
    }

    @Override
    public void onHide() {
        super.onHide();
        logger.debug("onHide()");
        hideInputMethod();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mBaseActivity.unregisterReceiver(mBluetoothPhoneReceiver);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_back:
                mBaseActivity.showMain();
                break;
            case R.id.button_search_contacts:
                if (null != mEdittextContactKeyWord) {
                    MobclickAgent.onEvent(mBaseActivity, ClickEvent.click47.getEventId());
                    searchContact(mEdittextContactKeyWord.getEditableText());
                }
                break;
            case R.id.imageview_input_delete:
                if (null != mEdittextContactKeyWord) {
                    MobclickAgent.onEvent(mBaseActivity, ClickEvent.click48.getEventId());
                    mEdittextContactKeyWord.setText("");
                    searchContact(mEdittextContactKeyWord.getEditableText());
                }
                break;
            case R.id.linearLayout_dial_keyboard:
            case R.id.button_dial_keyboard:
                //如果在通话中，则进入通话中界面，否则进入拨号界面
                if (BtPhoneUtils.btCallState == BtPhoneUtils.CALL_STATE_ACTIVE) {
                    replaceFragment(FragmentConstants.BT_CALLING);
                } else {
                    MobclickAgent.onEvent(mBaseActivity, ClickEvent.click43.getEventId());
                    replaceFragment(FragmentConstants.BT_DIAL);
                }
                break;
            case R.id.textView_loading:
                if (null != mTextViewLoading && TAG_FAIL == (Integer) mTextViewLoading.getTag()) {
                    mTextViewLoading.setTag(TAG_NORMAL);
                    mTextViewLoading.setText(R.string.bt_contacts_sync_loading);
                    Intent intent = new Intent(Constants.BLUETOOTH_PULL_PHONE_BOOK_BEGIN);
                    mBaseActivity.sendBroadcast(intent);
                }
                break;
            case R.id.textView_bt_edit:
                //弹出蓝牙名称编辑框
                mEditTextBtName.setEnabled(true);
                mEditTextBtName.setFocusable(true);
                mEditTextBtName.setFocusableInTouchMode(true);
                mEditTextBtName.requestFocus();
                mEditTextBtName.setSelection(mEditTextBtName.getText().length());//将光标移至文字末尾
                InputMethodManager inputManager =
                        (InputMethodManager)mEditTextBtName.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(mEditTextBtName, InputMethodManager.SHOW_FORCED);
                break;
        }
    }

    private void showContacts() {
        //蓝牙连接状态下
        if (BluetoothProfile.STATE_CONNECTED == BtPhoneUtils.connectionState) {
            if (null != mTextViewLoading && null != mContactsListView) {
                if (TAG_FAIL == (Integer) mTextViewLoading.getTag()) {
                    mTextViewLoading.setVisibility(View.VISIBLE);
                    mTextViewLoading.setTag(TAG_FAIL);
                    mTextViewLoading.setText(R.string.bt_contacts_sync_fail);
                    mContactsListView.setVisibility(View.INVISIBLE);
                } else {
                    logger.debug("mSyncState:" + BtPhoneUtils.mSyncState + ",BtPhoneUtils.pbapConnectState:" + BtPhoneUtils.pbapConnectState);
                    if (BtPhoneUtils.mSyncState == BtPhoneUtils.SYNC_STATE.SYNC_END &&
                            null != mFragment && mFragment.isVisible() &&
                            BtPhoneUtils.pbapConnectState != BtPhoneUtils.STATE_CONNECTED &&
                            (null != mContactsData && mContactsData.size() == 0)) {
                        loadContactsData();//加载缓存通讯录，不需要异步线程处理new LoadBtTask().execute();
                    } else if (BtPhoneUtils.pbapConnectState == BtPhoneUtils.STATE_CONNECTED) {
                        //如果通讯录同步连接正常
                        mTextViewLoading.setVisibility(View.VISIBLE);
                        mTextViewLoading.setText(R.string.bt_contacts_sync_loading);
                    } else if (null != mContactsData && mContactsData.size() == 0) {
                        //如果通讯录同步连接断开
                        mTextViewLoading.setVisibility(View.VISIBLE);
                        mTextViewLoading.setText(R.string.bt_contacts_no);
                    }

                }
            }
        } else {
            //蓝牙未连接提示连接
            if (null != mContactsData && mContactsData.size() > 0) {
                mContactsData.clear();
            }
            if (null != mTextViewLoading && null != mContactsListView) {
                mTextViewLoading.setVisibility(View.VISIBLE);
                mTextViewLoading.setText(R.string.bt_noti_connect_waiting);
                mContactsListView.setVisibility(View.INVISIBLE);
            }
        }

    }

    /**
     * 更新UI的蓝牙名称
     */
    private void updateBtName() {
        if (null != mEditTextBtName) {
            if(!BtPhoneUtils.getBluetoothDeviceName().equals(mEditTextBtName.getText().toString())){
                mEditTextBtName.setText(BtPhoneUtils.getBluetoothDeviceName());
            }
        }
    }

    /**
     * 添加联系人
     */
    private void actionAdd() {
        showAddContactDialog();
    }

    private void showAddContactDialog() {
        ShowContactDetailDialog detailDialog = new ShowContactDetailDialog(mBaseActivity);
        detailDialog.show();
        detailDialog.setOnAddContactListener(new ShowContactDetailDialog.OnAddContactListener() {
            @Override
            public void saveContact(Contact contact) {
//                dbHelper.insertContact(contact.getName(), contact.getNumber());
                BtPhoneUtils.addContact(mBaseActivity, contact);
                mContactsData.add(contact);
                mContactsAdapter.setData(mContactsData);
                fixSoftInput();
            }
        });
    }

    /**
     * 加载通讯录
     */
    private synchronized void loadContactsData() {
        //设置界面状态提示
        mTextViewLoading.setVisibility(View.VISIBLE);
        mTextViewLoading.setText(R.string.bt_contacts_loading);
        mContactsListView.setVisibility(View.INVISIBLE);
        //开始加载通讯录
        logger.debug("Load Bt contacts begin");
        List<Contact> list = BtPhoneUtils.obtainContacts(mBaseActivity);
        if (list != null && list.size() != 0) {
            if (null != mContactsData && mContactsData.size() > 0) {
                mContactsData.clear();
            }
            mContactsData.addAll(list);
            //联系人名字正排序
            Collections.sort(mContactsData, new SortChineseName());
            String iNumber = "";
            for (Contact ct : mContactsData) {
                iNumber = ct.getNumber();
                if (iNumber.startsWith("1") && iNumber.length() == 11) {
                    ct.setNumber(iNumber.substring(0, 3) + "-" + iNumber.substring(3, 7) + "-" + iNumber.substring(7, 11));
                } else if (iNumber.startsWith("4") && iNumber.length() == 10) {
                    ct.setNumber(iNumber.substring(0, 3) + "-" + iNumber.substring(3, 7) + "-" + iNumber.substring(7, 10));
                } else if (iNumber.startsWith("+86") && iNumber.length() == 14) {
                    ct.setNumber(iNumber.substring(0, 3) + " " + iNumber.substring(3, 6) + "-" + iNumber.substring(6, 10) + "-" + iNumber.substring(10, 14));
                }
//                logger.debug("contact:"+ct.getName()+":"+ct.getNumber());
            }

        } else {
            mContactsData.clear();
        }
        //修改加载完整的状态提示
        logger.debug("Load Bt contacts adapter setData");
        if (null == mContactsData || mContactsData.size() == 0) {
            mTextViewLoading.setText(R.string.bt_contacts_no);
            mTextViewLoading.setVisibility(View.VISIBLE);
            mContactsListView.setVisibility(View.INVISIBLE);
        } else {
            mTextViewLoading.setVisibility(View.GONE);
            mContactsListView.setVisibility(View.VISIBLE);
        }
        mRecordAdapter.setData(mRecordData);
        mContactsAdapter.setData(mContactsData);
        logger.debug("Load Bt contacts end");
    }

    private void loadCallRecordData() {

    }
    public void dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        logger.debug("keyEvent getKeyCode:"+code);
        if(code==66){
            String btName = mEditTextBtName.getText().toString();
            logger.debug("BtContactsFragment key event set bluetooth name:"+btName);
            //设置蓝牙名称
            if(!TextUtils.isEmpty(btName) && !BtPhoneUtils.getBluetoothDeviceName().equals(btName)){

                BtPhoneUtils.setBluetoothDeviceName(btName);
            }
        }
    }
    private TextWatcher btNameTextWatcher = new TextWatcher(){

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if(null!=s && s.length()>0){
                //设置蓝牙名称
//                logger.debug("bluetooth update name 1 BtContactsFragment set bluetooth name:"+mEditTextBtName.getText().toString());
//                BtPhoneUtils.setBluetoothDeviceName(mEditTextBtName.getText().toString());
            }else{
                BtPhoneUtils.initBluetoothDeviceName();
            }
            mEditTextBtName.setCursorVisible(false);
        }
    };

    /**
     * 中文名排序
     */
    public class SortChineseName implements Comparator<Contact> {
        Collator cmp = Collator.getInstance(java.util.Locale.CHINA);

        @Override
        public int compare(Contact o1, Contact o2) {
            if (cmp.compare(o1.getName(), o2.getName()) > 0) {
                return 1;
            } else if (cmp.compare(o1.getName(), o2.getName()) < 0) {
                return -1;
            }
            return 0;
        }
    }

    class LoadBtTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            logger.debug("LoadBtTask onPreExecute()");
            mTextViewLoading.setVisibility(View.VISIBLE);
            mTextViewLoading.setText(R.string.bt_contacts_loading);
            mContactsListView.setVisibility(View.INVISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            logger.debug("LoadBtTask doInBackground()");
//            loadCallRecordData();
//            loadContactsData();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            logger.debug("LoadBtTask onPostExecute()1");
            if (null == mContactsData || mContactsData.size() == 0) {
                mTextViewLoading.setText(R.string.bt_contacts_no);
                mTextViewLoading.setVisibility(View.VISIBLE);
                mContactsListView.setVisibility(View.INVISIBLE);
            } else {
                mTextViewLoading.setVisibility(View.GONE);
                mContactsListView.setVisibility(View.VISIBLE);
            }
            mRecordAdapter.setData(mRecordData);
            mContactsAdapter.setData(mContactsData);
            logger.debug("LoadBtTask onPostExecute()2");
            fixSoftInput();
        }
    }
}

class RecordAdapter extends BaseAdapter {

    private Context context;

    private ArrayList<CallRecord> data;

    private LayoutInflater inflater;

    public RecordAdapter(Context context, ArrayList<CallRecord> data) {
        this.context = context;
        this.data = data;
        inflater = LayoutInflater.from(context);
    }

    public void setData(ArrayList<CallRecord> data) {
        this.data = (ArrayList<CallRecord>) data.clone();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.bt_record_item, parent, false);
            holder.tvName = (TextView) convertView.findViewById(R.id.call_name);
            holder.tvType = (TextView) convertView.findViewById(R.id.call_state);
            holder.tvTime = (TextView) convertView.findViewById(R.id.call_time);
            holder.tvDuration = (TextView) convertView.findViewById(R.id.call_duration);
            holder.btnAdd = (ImageButton) convertView.findViewById(R.id.button_add);
            holder.btnDelete = (ImageButton) convertView.findViewById(R.id.button_delete);
            holder.btnEdit = (ImageButton) convertView.findViewById(R.id.button_edit);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        CallRecord callRecord = data.get(position);
        holder.tvName.setText(callRecord.getName());
        if (callRecord.getState() == 0) {
            holder.tvType.setText("未接来电");
        } else if (callRecord.getState() == 1) {
            holder.tvType.setText("已接来电");
        }
        holder.tvTime.setText(String.valueOf(callRecord.getTime()));
        holder.tvDuration.setText(String.valueOf(callRecord.getDuration()));
        return convertView;
    }

    class ViewHolder {
        TextView tvName;
        TextView tvType;
        TextView tvTime;
        TextView tvDuration;
        ImageButton btnAdd;
        ImageButton btnEdit;
        ImageButton btnDelete;
    }
}

class ContactsAdapter extends BaseAdapter {

    private ArrayList<Contact> data;

    private Context context;
    private LayoutInflater inflater;
    private View.OnClickListener clkListener;

    public ContactsAdapter(Context context, ArrayList<Contact> data) {
        this.data = data;
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    public void setOnClickListener(View.OnClickListener clkListener) {
        this.clkListener = clkListener;
    }

    public void setData(ArrayList<Contact> data) {
        this.data = (ArrayList<Contact>) data.clone();
        notifyDataSetChanged();

    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.bt_contacts_item, parent, false);
            holder.tvName = (TextView) convertView.findViewById(R.id.contacts_name);
            holder.tvNumber = (TextView) convertView.findViewById(R.id.contacts_number);
            holder.btnDetails = (ImageButton) convertView.findViewById(R.id.button_details);
            holder.btnDelete = (ImageButton) convertView.findViewById(R.id.button_delete);
            holder.linearLayoutContactsTitle = (LinearLayout) convertView.findViewById(R.id.linearLayout_contacts_title);
            convertView.setBackgroundColor(Color.TRANSPARENT);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Contact contacts = data.get(position);
        holder.tvName.setText(contacts.getName());
        holder.tvNumber.setText(contacts.getNumber());
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteContactDialog(data.get(position));
            }
        });
        holder.btnDelete.setVisibility(View.INVISIBLE);
        holder.btnDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContactDetailsDialog(data.get(position));
            }
        });
        holder.btnDetails.setVisibility(View.INVISIBLE);
        holder.linearLayoutContactsTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (BtPhoneUtils.connectionState != BtPhoneUtils.STATE_CONNECTED) {
                    VoiceManagerProxy.getInstance().startSpeaking(
                            context.getString(R.string.bt_noti_connect_waiting), TTSType.TTS_DO_NOTHING, false);
                    return;
                }

                if (null != contacts) {
                    String number = contacts.getNumber().replace("-", "").replace(" ", "");
                    //拨出电话的广播在BtOutCallFragment中发出，
                    //在BtCallReceiver中接收电话接通广播后显示通话中界面
                    BtPhoneUtils.btCallOutSource = BtPhoneUtils.BTCALL_OUT_SOURCE_KEYBOARD;
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.EXTRA_PHONE_NUMBER, number);
                    FragmentConstants.TEMP_ARGS = bundle;
                    clkListener.onClick(v);
                }
            }
        });
        return convertView;
    }

    private void showDeleteContactDialog(Contact contact) {
        DeleteContactDialog dialog = new DeleteContactDialog(context);
        dialog.show();
        dialog.setOnDialogButtonClickListener(new DeleteContactDialog.OnDialogButtonClickListener() {
            @Override
            public void onConfirmClick() {
                data.remove(contact);
               /* mContactsData.remove(contact);
                dbHelper.deleteContact(contact);*/
                notifyDataSetChanged();
            }
        });
    }

    private void showContactDetailsDialog(Contact contactOld) {
        ShowContactDetailDialog dialog = new ShowContactDetailDialog(context);
        dialog.show();
        dialog.setContactInfo(contactOld);
        dialog.setOnAddContactListener(new ShowContactDetailDialog.OnAddContactListener() {
            @Override
            public void saveContact(Contact contact) {
//                DbHelper dbHelper = DbHelper.getDbHelper();
//                dbHelper.updateContact(contact);
//                mContactsData.add(contact);
//                mContactsAdapter.setData(mContactsData);
                BtPhoneUtils.updateContact(context, contactOld, contact);
                contactOld.setName(contact.getName());
                contactOld.setNumber(contact.getNumber());
                notifyDataSetChanged();
            }
        });
    }

    class ViewHolder {
        TextView tvName;
        TextView tvNumber;
        ImageButton btnDetails;
        ImageButton btnDelete;
        LinearLayout linearLayoutContactsTitle;
    }
}
