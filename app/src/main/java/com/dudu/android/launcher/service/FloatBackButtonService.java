package com.dudu.android.launcher.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.dudu.aios.ui.activity.MainRecordActivity;
import com.dudu.android.launcher.R;
import com.dudu.navi.event.NaviEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.greenrobot.event.EventBus;


/**
 * Created by lxh on 2015/11/24.
 */
public class FloatBackButtonService extends Service {

    // 悬浮窗View的参数
    private WindowManager.LayoutParams windowParams;

    // 用于控制在屏幕上添加或移除悬浮窗
    private WindowManager windowManager;

    private View floatButton;

    private boolean isShow = false;

    private Logger logger;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.debug("floatButtonService onStartCommand");

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        logger = LoggerFactory.getLogger("init.floatService");

        EventBus.getDefault().register(this);

        initButton();

    }

    @Override
    public void onDestroy() {
        logger.debug("floatButtonService onDestroy");
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void initButton() {

        floatButton = new Button(this);
        floatButton.setBackgroundResource(R.drawable.back_button_selector);
        if (windowParams == null) {
            windowParams = new WindowManager.LayoutParams();
            windowParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            windowParams.format = PixelFormat.RGBA_8888;
            windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            windowParams.width = 100;
            windowParams.height = 100;

            windowParams.x = -getWmWidth() / 2 + 180;
            windowParams.y = 180;
            windowParams.alpha = 1.0f;
        }

        floatButton.setOnClickListener(v -> {
            Intent intent = new Intent(getBaseContext(), MainRecordActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        });
    }

    private int getWmWidth() {
        return getWindowManager().getDefaultDisplay().getWidth();// 屏幕宽度
    }

    private synchronized WindowManager getWindowManager() {
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        }
        return windowManager;
    }

    public void onEventMainThread(NaviEvent.FloatButtonEvent event) {

        switch (event) {
            case SHOW:
                if (windowManager != null && floatButton != null) {
                    isShow = true;
                    windowManager.addView(floatButton, windowParams);
                }

                break;
            case HIDE:
                if (windowManager != null && floatButton != null && isShow) {
                    isShow = false;
                    windowManager.removeView(floatButton);
                }
/*
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!ActivitiesManager.getInstance().isTopActivity(FloatBackButtonService.this, "com.dudu.android.launcher")) {
                            EventBus.getDefault().post(NaviEvent.FloatButtonEvent.SHOW);
                        }
                    }
                }, 3000);
*/
                break;
        }
    }
}
