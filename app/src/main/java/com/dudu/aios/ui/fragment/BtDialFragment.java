package com.dudu.aios.ui.fragment;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dudu.aios.ui.base.BaseActivity;
import com.dudu.aios.ui.fragment.base.RBaseFragment;
import com.dudu.aios.ui.utils.contants.FragmentConstants;
import com.dudu.android.launcher.LauncherApplication;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.ui.activity.DebugActivity;
import com.dudu.android.launcher.utils.BtPhoneUtils;
import com.dudu.android.launcher.utils.Constants;
import com.dudu.android.launcher.utils.DeviceIDUtil;
import com.dudu.android.launcher.utils.LogUtils;
import com.dudu.commonlib.umeng.ClickEvent;
import com.dudu.voice.VoiceManagerProxy;
import com.dudu.voice.semantic.constant.TTSType;
import com.umeng.analytics.MobclickAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Subscription;

/**
 * 拨号界面
 * Created by Robi on 2016-03-10 17:35.
 */
public class BtDialFragment extends RBaseFragment implements
        View.OnClickListener, TextWatcher {

    private EditText mDigits;

    private Button mDialButton, mDeleteButton;

    private ImageButton mBackButton, mContactsButton;
    private EditText mEditTextBtName;
    private TextView mTextViewBtNameEdit;
    private LinearLayout mLinearLayoutContacts;
    private Handler handler;
    private Logger logger = LoggerFactory.getLogger("phone.BtDialFragment");
    private AudioManager mAudioMgr;
    private DeleteNumberRunnable deleteRunnable;
    @Override
    protected int getContentView() {
        return R.layout.activity_blue_tooth_dial;
    }

    @Override
    protected void initView(View rootView) {
        mEditTextBtName = (EditText) mViewHolder.v(R.id.editText_bt_name);
        mTextViewBtNameEdit = (TextView) mViewHolder.v(R.id.textView_bt_edit);
        mDigits = (EditText) mViewHolder.v(R.id.dial_digits);
        mDialButton = (Button) mViewHolder.v(R.id.button_dial);
        mBackButton = (ImageButton) mViewHolder.v(R.id.back_button);
        mDeleteButton = (Button) mViewHolder.v(R.id.delete_button);
        mContactsButton = (ImageButton) mViewHolder.v(R.id.button_contacts);
        mLinearLayoutContacts = (LinearLayout) mViewHolder.v(R.id.linearLayout_contacts);

        mViewHolder.v(R.id.button_dial_keyboard).setSelected(true);

        mAudioMgr = (AudioManager) mBaseActivity.getSystemService(mBaseActivity.AUDIO_SERVICE);

        expandViewTouchDelegate(mDeleteButton, 30, 30, 30, 30);

        deleteRunnable = new DeleteNumberRunnable();

        handler = new DeleteDigitHandler();
    }

    @Override
    protected void initViewData() {
//        mDigits.setClickable(false);
        mDigits.setEnabled(false);
        mDigits.setOnClickListener(this);
        mDigits.addTextChangedListener(this);
        mDigits.setCursorVisible(false);
        mDialButton.setOnClickListener(this);
        mBackButton.setOnClickListener(this);
        mEditTextBtName.addTextChangedListener(btNameTextWatcher);
        mTextViewBtNameEdit.setOnClickListener(this);
        // mDeleteButton.setOnClickListener(this);
        mContactsButton.setOnClickListener(this);
        mLinearLayoutContacts.setOnClickListener(this);
        final Subscription[] subscriber = new Subscription[1];
        mDeleteButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });
        mDeleteButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case KeyEvent.ACTION_DOWN:
                    logger.debug("touch delete");
                    if(isDigitsEmpty()){
                        break;
                    }
                    MobclickAgent.onEvent(mBaseActivity, ClickEvent.click46.getEventId());
                    handler.post(deleteRunnable);
                    mAudioMgr.playSoundEffect(AudioManager.FX_FOCUS_NAVIGATION_LEFT);
                   /* subscriber[0] = Observable.timer(100, 100, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io())
                            .subscribe(aLong -> {
                                logger.debug("touch delete timer----------");
                                mAudioMgr.playSoundEffect(AudioManager.FX_FOCUS_NAVIGATION_LEFT);
                                handler.sendEmptyMessage(0);
                            }, throwable -> logger.error("Observable.timer", throwable));*/

                    break;
                case KeyEvent.ACTION_UP:
                    logger.debug("touch delete up");
                    /*if (subscriber.length > 0 && subscriber[0] != null) {
                        subscriber[0].unsubscribe();
                    }*/
                    handler.removeCallbacks(deleteRunnable);
                    break;
            }
            return true;
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_dial:
                MobclickAgent.onEvent(mBaseActivity, ClickEvent.click45.getEventId());
                doDial();
                break;
            case R.id.back_button:
                mBaseActivity.showMain();
                break;
           /* case R.id.delete_button:
                logger.debug("clk delete");
                removeSelectedDigit();
                break;*/
            case R.id.button_contacts:
            case R.id.linearLayout_contacts:
                MobclickAgent.onEvent(mBaseActivity, ClickEvent.click44.getEventId());
                replaceFragment(FragmentConstants.BT_CONTACTS);
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
    private void registerBcReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_AUDIO_STATE_CHANGED);
        LauncherApplication.getContext().registerReceiver(receiver,intentFilter);
    }
    private void unRegisterBcReceiver(){
        LauncherApplication.getContext().unregisterReceiver(receiver);
    }
    private void removeSelectedDigit() {
        logger.debug("remove begin");
        final int length = mDigits.length();
        final int start = mDigits.getSelectionStart();
        final int end = mDigits.getSelectionEnd();
        if (start < end) {
            mDigits.getEditableText().replace(start, end, "");
        } else {
            if (mDigits.isCursorVisible()) {
                if (end > 0) {
                    mDigits.getEditableText().replace(end - 1, end, "");
                }
            } else {
                if (length > 1) {
                    mDigits.getEditableText().replace(length - 1, length, "");
                } else {
                    mDigits.getEditableText().clear();
                }
            }
            String digitString = mDigits.getText().toString();
            if (digitString.length() > 0) {
                if (digitString.substring(digitString.length() - 1, digitString.length()).equals(" ")) {
                    removeSelectedDigit();
                }
            }
        }

        if (isDigitsEmpty()) {
            mDeleteButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(null!=mDigits){
            mDigits.setText("");
        }
        //修改蓝牙设备名称
//        initBluetoothDeviceName();
        registerBcReceiver();
        //更新UI的蓝牙名称
        updateBtName();

        if (isDigitsEmpty()) {
            mDeleteButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unRegisterBcReceiver();
        hideInputMethod();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onShow() {
        super.onShow();

        if (FragmentConstants.BT_OUT_CALL.equals(BaseActivity.lastSecondFragment) ||
                FragmentConstants.BT_OUT_CALL.equals(BaseActivity.lastFragment) ||
                FragmentConstants.BT_CALLING.equals(BaseActivity.lastSecondFragment) ||
                FragmentConstants.BT_CALLING.equals(BaseActivity.lastFragment) ||
                FragmentConstants.FRAGMENT_MAIN_PAGE.equals(BaseActivity.lastSecondFragment)) {
            if (null != mDigits) {
                mDigits.setText("");
            }
        }

        //修改蓝牙设备名称
//        initBluetoothDeviceName();
        //更新UI的蓝牙名称
        updateBtName();
        if (isDigitsEmpty()) {
            mDeleteButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onHide() {
        super.onHide();
        BtPhoneUtils.mLastDialTime = 0;
        BtPhoneUtils.mDialCounter = 0;
        hideInputMethod();
    }

    /**
     * 更新UI的蓝牙名称
     */
    private void updateBtName(){
        if(null!=mEditTextBtName){
            if (BtPhoneUtils.getBluetoothDeviceName() == null) {
                logger.debug("BluetoothDeviceName =  null, return");
                return;
            }

            if(!BtPhoneUtils.getBluetoothDeviceName().equals(mEditTextBtName.getText().toString())){
                mEditTextBtName.setText(BtPhoneUtils.getBluetoothDeviceName());
            }
        }
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
    private void doDial() {
        logger.debug("doDial()...");
        //拨号前先判断蓝牙是否处于连接状态
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (null == adapter) {
            VoiceManagerProxy.getInstance().startSpeaking(
                    getString(R.string.bt_noti_disenable), TTSType.TTS_DO_NOTHING, false);
            return;
        }
        if (!adapter.isEnabled()) {
            adapter.enable();
            VoiceManagerProxy.getInstance().startSpeaking(
                    getString(R.string.bt_noti_connect_waiting), TTSType.TTS_DO_NOTHING, false);
            return;
        }

        if (BtPhoneUtils.connectionState == BtPhoneUtils.STATE_CONNECTED) {

            String dialString = mDigits.getText().toString().replace(" ","").replace("-","");
            if (TextUtils.isEmpty(dialString)) {
                return;
            }
            //蓝牙电话拨号界面拨出电话
            BtPhoneUtils.btCallOutSource = BtPhoneUtils.BTCALL_OUT_SOURCE_KEYBOARD;
            mDigits.setText("");
            //拨出电话的广播在BtOutCallFragment中发出，
            //在BtCallReceiver中接收电话接通广播后显示通话中界面
            if(null==FragmentConstants.TEMP_ARGS){
                FragmentConstants.TEMP_ARGS = new Bundle();
            }
            FragmentConstants.TEMP_ARGS.putString(Constants.EXTRA_PHONE_NUMBER, dialString);
            replaceFragment(FragmentConstants.BT_OUT_CALL);
        } else {
            if(BtPhoneUtils.mDialCounter<2){

                if( (System.currentTimeMillis() - BtPhoneUtils.mLastDialTime)<1000){
                    logger.debug("拨号太频繁...");
                    BtPhoneUtils.mDialCounter++;
                }else{
                    BtPhoneUtils.mDialCounter = 0;
                    BtPhoneUtils.mLastDialTime = System.currentTimeMillis();
                    VoiceManagerProxy.getInstance().startSpeaking(
                            mBaseActivity.getString(R.string.bt_noti_connect_waiting), TTSType.TTS_DO_NOTHING, false);
                }
            }else{
                logger.debug("拨号太频繁，延时处理");
            }
        }
    }

    public boolean isDigitsEmpty() {
        return mDigits.length() == 0;
    }

    private void handleDialButtonClick(String digit) {
        final int length = mDigits.length();
        final int start = mDigits.getSelectionStart();
        final int end = mDigits.getSelectionEnd();
        if (length == start && length == end) {
            mDigits.setCursorVisible(false);
        }

        if (start < end) {
            mDigits.getEditableText().replace(start, end, digit);
        } else {
            mDigits.getEditableText().insert(mDigits.getSelectionEnd(), digit);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        //原有的文本s中，从start开始的count个字符替换长度为before的旧文本
        if (s == null || s.length() == 0) return;
        //对输入的字符串设置格式
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {//循环判断输入的字符串
            if (i != 3 && i != 8 && s.charAt(i) == ' ') {//如果不是第4、9个字符，又为空格，则continue，即去掉该空格
                continue;
            } else {
                sb.append(s.charAt(i));//字符串连接
                if ((sb.length() == 4 || sb.length() == 9) && sb.charAt(sb.length() - 1) != ' ') {//第4、9个字符的位置不为空则插入空格
                    sb.insert(sb.length() - 1, ' ');
                }
            }
        }
        //如果设置格式后字符串与输入字符串不同
        if (!sb.toString().equals(s.toString())) {
            int index = start + 1;
            logger.debug("s:"+s + ",sb:"+sb+",start:"+start + ",before:"+before + ",count:"+count);
            if (start<sb.length() && sb.charAt(start) == ' ') {
                if (before == 0) {
                    index++;
                } else {
                    index--;
                }
            } else {
                if (before == 1) {
                    index--;
                }
            }
            logger.debug("index:"+index);
            mDigits.setText(sb.toString());
            mDigits.setSelection(index);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if(null!=s && s.length()>13){
            if(null!=mDigits){
                mDigits.setTextSize(35);
            }
        }else if(null!=s && s.length()<13){
            if(null!=mDigits){
                mDigits.setTextSize(40);
            }
        }

        //判断是否"**0806##"
        if(null!=s && "**0806##".equals(s.toString().replace(" ",""))){
            final Intent intent = new Intent(getActivity(), DebugActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        }
    }

    public void onDialButtonClick(View view) {
        if (mDeleteButton.getVisibility() == View.INVISIBLE) {
            mDeleteButton.setVisibility(View.VISIBLE);
        }
        handleDialButtonClick((String) view.getTag());
        LogUtils.v("keyboard", "--" + view.getTag());
    }

    private class DeleteDigitHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            removeSelectedDigit();
        }
    }

    /**
     * 扩大View的触摸和点击响应范围,最大不超过其父View范围
     *
     * @param view
     * @param top
     * @param bottom
     * @param left
     * @param right
     */
    public static void expandViewTouchDelegate(final View view, final int top,
                                               final int bottom, final int left, final int right) {

        ((View) view.getParent()).post(new Runnable() {
            @Override
            public void run() {
                Rect bounds = new Rect();
                view.setEnabled(true);
                view.getHitRect(bounds);

                bounds.top -= top;
                bounds.bottom += bottom;
                bounds.left -= left;
                bounds.right += right;

                TouchDelegate touchDelegate = new TouchDelegate(bounds, view);

                if (View.class.isInstance(view.getParent())) {
                    ((View) view.getParent()).setTouchDelegate(touchDelegate);
                }
            }
        });
    }

    class DeleteNumberRunnable implements Runnable {

        @Override
        public void run() {
            try {
                logger.debug("removeSelectedDigit()");
                removeSelectedDigit();
                Thread.sleep(100);
                handler.post(deleteRunnable);
            } catch (InterruptedException e) {

            }

        }
    }

    /**
     * 重置拨号次数
     */
    class DelayResetDialCounterRunabel implements Runnable{

        @Override
        public void run(){
            try {
                logger.debug("delay reset dial counter");
                Thread.sleep(10000);
                BtPhoneUtils.mLastDialTime = 0;
                BtPhoneUtils.mDialCounter = 0;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void dispatchKeyEvent(KeyEvent event) {
            int code = event.getKeyCode();
            logger.debug("keyEvent getKeyCode:"+code);
            if(code==66){
                String btName = mEditTextBtName.getText().toString();
                logger.debug("BtDialFragment key event set bluetooth name:"+btName);
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
            /*mEditTextBtName.setEnabled(false);*/
            if(null!=s && s.length()>0){
//                logger.debug("bluetooth update name 2 BtDialFragment set bluetooth name:"+mEditTextBtName.getText().toString());
                //设置蓝牙名称
//                BtPhoneUtils.setBluetoothDeviceName(mEditTextBtName.getText().toString());
            }else{
//                BtPhoneUtils.initBluetoothDeviceName();
            }
            mEditTextBtName.setCursorVisible(false);
//            mEditTextBtName.clearFocus();
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(Constants.ACTION_AUDIO_STATE_CHANGED.equals(action)){
                hideInputMethod();
            }
        }
    };
}
