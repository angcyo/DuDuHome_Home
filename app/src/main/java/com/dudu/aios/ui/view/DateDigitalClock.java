package com.dudu.aios.ui.view;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;

import java.util.Calendar;

/**
 * Created by Administrator on 2016/7/9.
 */
public class DateDigitalClock extends android.widget.DigitalClock {
    private Calendar mCalendar;
    private final static String mFormat = "yyyy/MM/dd";
    private final static String dayNames[] = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五",
            "星期六"};
    private final static String space = " ";
    private FormatChangeObserver mFormatChangeObserver;
    private StringBuffer dateText = new StringBuffer();

    private Runnable mTicker;
    private Handler mHandler;

    private boolean mTickerStopped = false;

    public DateDigitalClock(Context context) {
        super(context);
        initClock();
    }

    public DateDigitalClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        initClock();
    }

    private void initClock() {
        if (mCalendar == null) {
            mCalendar = Calendar.getInstance();
        }
        mFormatChangeObserver = new FormatChangeObserver();
        getContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, mFormatChangeObserver);
    }

    @Override
    protected void onAttachedToWindow() {
        mTickerStopped = false;
        super.onAttachedToWindow();

        mHandler = new Handler();

        mTicker = new Runnable() {
            @Override
            public void run() {
                if (mTickerStopped) {
                    return;
                }
                mCalendar.setTimeInMillis(System.currentTimeMillis());
                dateText.delete(0, dateText.length());
                dateText.append(DateFormat.format(mFormat, mCalendar));
                dateText.append(space);

                int dayOfWeek = mCalendar.get(Calendar.DAY_OF_WEEK) - 1;
                if (dayOfWeek < 0) {
                    dayOfWeek = 0;
                }
                dateText.append(dayNames[dayOfWeek]);
                setText(dateText);

                invalidate();
                long now = SystemClock.uptimeMillis();
                long next = now + (1000 - now % 1000);
                mHandler.postAtTime(mTicker, next);
            }
        };
        mTicker.run();
    }


    private class FormatChangeObserver extends ContentObserver {

        public FormatChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {

        }
    }
}
