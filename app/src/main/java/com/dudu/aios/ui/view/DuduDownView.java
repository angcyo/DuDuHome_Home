package com.dudu.aios.ui.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.dudu.android.launcher.R;

/**
 * Created by robi on 2016-06-04 10:41.
 */
public class DuduDownView extends View {

    public static final int STATE_NO = -1;
    public static final int STATE_NOR = 0;
    public static final int STATE_ERROR = 1;
    public static final int STATE_OK = 2;

    public DuduDownView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable background = getBackground();

        if (background != null) {
            setMeasuredDimension(background.getIntrinsicWidth(), background.getIntrinsicHeight());
        } else {
            setMeasuredDimension(0, 0);
        }
    }

    public void setState(int state) {
        switch (state) {
            case STATE_NOR:
                setBackgroundResource(R.drawable.v_down_nor);
                break;
            case STATE_ERROR:
                setBackgroundResource(R.drawable.v_down_error);
                break;
            case STATE_OK:
                setBackgroundResource(R.drawable.v_down_ok);
                break;
            default:
                setBackground(null);
                break;
        }
    }
}
