package com.dudu.android.launcher.utils;

import android.content.Context;
import android.view.Window;
import android.view.WindowManager;

import com.dudu.aios.ui.dialog.DisclaimerDialog;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.ui.dialog.AppDownloadDialog;
import com.dudu.android.launcher.ui.dialog.ErrorMessageDialog;

/**
 * Created by Administrator on 2015/11/25.
 */
public class DialogUtils {

    private static ErrorMessageDialog mOBDErrorDialog;

    private static ErrorMessageDialog mWithoutSimCardDialog;

    private static ErrorMessageDialog mSimCardReplaceDialog;

    private static AppDownloadDialog mAppDownloadDialog;


    public static void showOBDErrorDialog(Context context) {
        if (Utils.isDemoVersion(context)) {
            return;
        }

        if (mOBDErrorDialog != null && mOBDErrorDialog.isShowing()) {
            return;
        }
        mOBDErrorDialog = new ErrorMessageDialog(context, R.string.obd_checking_unconnected,
                R.drawable.obd_checking_icon);

        Window dialogWindow = mOBDErrorDialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.x = 10;
        lp.y = 0;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialogWindow.setAttributes(lp);
        mOBDErrorDialog.show();
    }

    public static void dismissOBDErrorDialog(Context context) {
        if (Utils.isDemoVersion(context)) {
            return;
        }

        if (mOBDErrorDialog != null && mOBDErrorDialog.isShowing()) {
            mOBDErrorDialog.dismiss();
            mOBDErrorDialog = null;
        }
    }

    public static void showWithoutSimCardDialog(Context context) {
        if (Utils.isDemoVersion(context)) {
            return;
        }

        if (mWithoutSimCardDialog != null && mWithoutSimCardDialog.isShowing()) {
            return;
        }

        /*mWithoutSimCardDialog = new ErrorMessageDialog(context, R.string.without_sim_card,
                R.drawable.sim_card_uninserted_icon);
        mWithoutSimCardDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mWithoutSimCardDialog.show();*/
    }

    public static void dismissWithoutSimCardDialog() {
        if (mWithoutSimCardDialog != null && mWithoutSimCardDialog.isShowing()) {
            mWithoutSimCardDialog.dismiss();
            mWithoutSimCardDialog = null;
        }
    }

    public static void showSimCardReplaceDialog(Context context) {
        if (Utils.isDemoVersion(context)) {
            return;
        }

        if (mSimCardReplaceDialog != null && mSimCardReplaceDialog.isShowing()) {
            return;
        }

        mSimCardReplaceDialog = new ErrorMessageDialog(context, R.string.sim_card_replaced_error,
                R.drawable.sim_card_uninserted_icon);
        mSimCardReplaceDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mSimCardReplaceDialog.show();
    }

    public static void dismissSimCardReplaceDialog() {
        if (mSimCardReplaceDialog != null && mSimCardReplaceDialog.isShowing()) {
            mSimCardReplaceDialog.dismiss();
            mSimCardReplaceDialog = null;
        }
    }

    public static void showAppDownloadDialog(Context context) {
        if (mAppDownloadDialog == null) {
            mAppDownloadDialog = new AppDownloadDialog(context);
        }
        if (mAppDownloadDialog.isShowing()) {
            return;
        }
        mAppDownloadDialog.show();
    }

    public static void dismissAppDownloadDialog() {
        if (mAppDownloadDialog != null && mAppDownloadDialog.isShowing()) {
            mAppDownloadDialog.dismiss();
            mAppDownloadDialog = null;
        }
    }

    public static void setAppDownloadListener(Context context, AppDownloadDialog.OnClickAppDownloadListener onClickAppDownloadListener) {
        if (mAppDownloadDialog == null) {
            mAppDownloadDialog = new AppDownloadDialog(context);
        }
        mAppDownloadDialog.setOnClickAppDownloadListener(onClickAppDownloadListener);
    }


    private static DisclaimerDialog mDisclaimerDialog;

    /**
     *免责声明dialog
     */
    public static void showDisclaimerDialog(Context context) {

            mDisclaimerDialog = new DisclaimerDialog(context);

            Window dialogWindow = mDisclaimerDialog.getWindow();
            WindowManager.LayoutParams lp = dialogWindow.getAttributes();
            lp.x = 10;
            lp.y = 0;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            dialogWindow.setAttributes(lp);
            mDisclaimerDialog.show();
    }

    public static void dismissDisclaimerDialog(Context context) {
        if (mDisclaimerDialog != null && mDisclaimerDialog.isShowing()) {
            mDisclaimerDialog.dismiss();
            mDisclaimerDialog = null;
        }
    }

    public static void setDisclaimerConfirmListener(Context context, DisclaimerDialog.OnConfirmListener listener) {
        if (mDisclaimerDialog == null) {
            mDisclaimerDialog = new DisclaimerDialog(context);
        }
        mDisclaimerDialog.setConfirmListener(listener);
    }

}
