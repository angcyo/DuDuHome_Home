package com.dudu.voice.window.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.dudu.android.launcher.R;

/**
 * Created by Moruna on 2016/7/11.
 */

/**
 * y=Asin(ωx+φ)+k
 * <p>
 * 振幅A
 * 波数ω
 * 波长λ=2π/ω
 */

public class SiriView extends View {

    private RefreshRunnable mRefreshRunnable;

    //不同波宽
    float[] waveWidth = {3, 2, 2, 1, 1};
    //不同波的透明度
    float[] waveAlpha = {1.0f, 0.9f, 0.7f, 0.5f, 0.4f};
    //副波纹振幅
    float[] waveAmplitude = {1.0f, 0.7f, 0.4f, 0.1f, -0.2f};

    float period = 1.5f;// 区域内，正弦波的周期，默认1.5f
    private float phase;
    private Paint paint = new Paint();
    private int width;
    private int height;
    private float midWidth;
    private float midHeight;

    public SiriView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < waveWidth.length; i++) {
            waveWidth[i] = waveWidth[i] * density / 2;
        }
        init();
    }

    private void init() {
        // 将绘图原点设置到区域中心
        width = getWidth();
        height = getHeight();
        midWidth = width / 2.0f;
        midHeight = height / 2.0f;

        // 初始化画笔
        paint.setStrokeWidth(1);// 画线宽度
        paint.setStyle(Paint.Style.STROKE);//空心效果
        paint.setAntiAlias(true);//抗锯齿

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            paint.setColor(ContextCompat.getColor(getContext(), R.color.blue));
        } else {
            paint.setColor(getResources().getColor(R.color.blue));
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.translate(midWidth, midHeight);
        // 初始化线条
        Path sinPath = new Path();
        sinPath.moveTo(-midWidth, 0);

        for (int i = 0; i < waveWidth.length; i++) {
            paint.setStrokeWidth(waveWidth[i]);//画笔宽度
            paint.setAlpha((int) (waveAlpha[i] * 255));//画笔透明度

            sinPath.reset();//重置线条

            sinPath.moveTo(-midWidth, 0); //移动到起始位置

            if (i % 2 == 0) {

                for (float x = -midWidth; x < midWidth; x++) {
                    double scaling = 1 - Math.pow(1 / midWidth * x, 2);
                    double sine = Math.sin(2 * Math.PI * period * ((x + phase) / width));// 该点的正弦值
                    float y = (float) (midHeight / 2 // 将正弦值限定到绘图区的高度上
                            * sine    // 正弦值
                            * scaling // 振幅修正 - 距离中心越远，振幅越小
                            * waveAmplitude[i] // 副波纹振幅修正
                    );
                    sinPath.lineTo(x, y);
                }
            } else {

                for (float x = -midWidth; x < midWidth; x++) {
                    double scaling = 1 - Math.pow(1 / midWidth * x, 2);
                    double sine = Math.sin(2 * Math.PI * period * ((x + phase) / width));// 该点上的正弦值
                    float y = -(float) (midHeight / 2 //将正弦值限定到绘图区的高度上
                            * sine   // 正弦值
                            * scaling // 振幅修正 - 距离中心越远，振幅越小
                            * waveAmplitude[i]// 副波纹振幅修正
                    );
                    sinPath.lineTo(x, y);
                }
            }

            canvas.drawPath(sinPath, paint);// 绘制线条
        }
        canvas.restore();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);

        if (View.GONE == visibility) {
            removeCallbacks(mRefreshRunnable);
        } else {
            removeCallbacks(mRefreshRunnable);
            mRefreshRunnable = new RefreshRunnable();
            post(mRefreshRunnable);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            if (width == 0) {
                init();
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (width == 0) {
            init();
        }
    }

    public void setWavePeriod(float wavePeriod) {
        period = wavePeriod;
    }

    public void nextPhase(float n) {
        phase -= n;
        invalidate();
    }

    private class RefreshRunnable implements Runnable {
        public void run() {
            synchronized (SiriView.this) {
                long start = System.currentTimeMillis();

                nextPhase(35);

                long gap = 20 - (System.currentTimeMillis() - start);
                postDelayed(this, gap < 0 ? 0 : gap);
            }
        }
    }
}
