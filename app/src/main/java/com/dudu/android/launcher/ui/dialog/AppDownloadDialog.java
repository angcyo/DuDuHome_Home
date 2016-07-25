package com.dudu.android.launcher.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.dudu.android.launcher.R;
import com.dudu.commonlib.event.Events;

import de.greenrobot.event.EventBus;

/**
 * Created by Administrator on 2016/7/8.
 */
public class AppDownloadDialog extends Dialog {

    private OnClickAppDownloadListener onClickAppDownloadListener;

    public void setOnClickAppDownloadListener(OnClickAppDownloadListener onClickAppDownloadListener) {
        this.onClickAppDownloadListener = onClickAppDownloadListener;
    }

    public AppDownloadDialog(Context context) {
        super(context, R.style.AppDownloadDialogStyle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_app_download_entrance);
        findViewById(R.id.container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClickAppDownloadListener != null) {
                    onClickAppDownloadListener.clickCancel();
                }
                EventBus.getDefault().post(new Events.AppDownloadIconEvent(false));
                dismiss();
            }
        });
    }

    public interface OnClickAppDownloadListener {
        void clickCancel();
    }

}
