package com.dudu.drivevideo.rearcamera;

import android.os.Handler;
import android.os.Looper;

import java.util.HashSet;
import java.util.Set;

/**
 * 模糊控制
 * <p>
 * Created by robi on 2016-07-07 16:52.
 */
public class BlurControl {

    private boolean isBlur = true;
    private Set<IBlurListener> mBlurListeners;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable postRunnable = () -> notifyListener();

    private BlurControl() {
        mBlurListeners = new HashSet<>();
    }

    public static BlurControl instance() {
        return Holder.blur;
    }

    public boolean isBlur() {
        return isBlur;
    }

    public synchronized void setBlur(boolean blur) {
//        if (isBlur != blur) {
            isBlur = blur;
            mainHandler.removeCallbacks(postRunnable);
            mainHandler.postDelayed(postRunnable, 0);//延迟通知
//        }
    }

    private void notifyListener() {
        for (IBlurListener listener : mBlurListeners) {
            listener.onBlurChange(isBlur);
        }
    }

    public void addBlurListener(IBlurListener listener) {
        mBlurListeners.add(listener);
    }

    public void removeBlurListener(IBlurListener listener) {
        mBlurListeners.remove(listener);
    }

    public interface IBlurListener {
        void onBlurChange(boolean isBlur);
    }

    static class Holder {
        static final BlurControl blur = new BlurControl();
    }
}
