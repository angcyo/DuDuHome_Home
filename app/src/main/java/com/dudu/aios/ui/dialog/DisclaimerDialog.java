package com.dudu.aios.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.dudu.aios.ui.activity.ProtocalActivity;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.DialogUtils;

/**
 * Created by Administrator on 2016/7/15.
 */
public class DisclaimerDialog extends Dialog implements View.OnClickListener{

    private Button confirmBtn;
    private TextView protocalTxt;
    private CheckBox remindCbox;
    private OnConfirmListener listener;
    private Context context;
    public DisclaimerDialog(Context context) {
        super(context, R.style.AiosBaseTheme);
        this.context =context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_disclaimer);
        confirmBtn = (Button) findViewById(R.id.btn_confirm);
        protocalTxt = (TextView) findViewById(R.id.txt_protocal);
        remindCbox = (CheckBox) findViewById(R.id.checkbox_remind);
        confirmBtn.setOnClickListener(this);
        protocalTxt.setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_confirm) {
            if (listener != null) {
                listener.onConfirm(remindCbox.isChecked());
            }
        }else if(v.getId() == R.id.txt_protocal){
            context.startActivity(new Intent(context, ProtocalActivity.class));
        }
    }

    public interface OnConfirmListener {
        void onConfirm(boolean noneed_remind);
    }

    public void setConfirmListener(OnConfirmListener l) {
        this.listener = l;
    }
}
