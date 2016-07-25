package com.dudu.voice.window.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.dudu.android.launcher.R;

/**
 * Created by Administrator on 2016/7/11.
 */
public class SiriViewLayout extends LinearLayout {

    private SiriView siriView;
    private float wavePeriod;

    public SiriViewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.WaveView);
        wavePeriod = attributes.getFloat(R.styleable.WaveView_wave_period, 1.5f);
        attributes.recycle();

        siriView = new SiriView(context, null);
        siriView.setWavePeriod(wavePeriod);

        addView(siriView);
    }

    public void addWaveView() {
        addView(siriView);
    }

    public void removeWaveView() {
        removeView(siriView);
    }
}