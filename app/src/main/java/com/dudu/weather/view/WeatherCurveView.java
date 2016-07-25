package com.dudu.weather.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.dudu.android.launcher.R;

/**
 * Created by Administrator on 2016/6/29.
 */
public class WeatherCurveView extends View {

    /**
     * x轴集合
     */
    private float mXAxis[] = new float[7];

    /**
     * y轴集合
     */
    private float mYAxis[] = new float[7];

    /**
     * x,y轴集合数
     */
    private static final int LENGTH = 7;

    /**
     * 最高温度集合
     */
    private int mHighTemp[] = new int[7];

    /**
     * 最低温度集合
     */
    private int mLowTemp[] = new int[7];

    /**
     * 控件高
     */
    private int mHeight;

    /**
     * 字体大小
     */
    private float mTextSize;

    /**
     * 圆半径
     */
    private float mRadius;

    /**
     * 文字移动位置距离
     */
    private float mTextSpace;

    /**
     * 曲线颜色
     */
    private int mCuverColor;

    /**
     * 屏幕密度
     */
    private float mDensity;

    /**
     * 控件边的空白空间
     */
    private float mBankSpace;

    /**
     * 线画笔
     */
    private Paint mLinePaint;

    /**
     * 点画笔
     */
    private Paint mPointPaint;

    /**
     * 字体画笔
     */
    private Paint mTextPaint;

    public WeatherCurveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @SuppressWarnings("deprecation")
    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WeatherCurveView);
        float densityText = getResources().getDisplayMetrics().scaledDensity;
        mTextSize = a.getDimensionPixelSize(R.styleable.WeatherCurveView_textSize,
                (int) (14 * densityText));
        mCuverColor = a.getColor(R.styleable.WeatherCurveView_curveColor,
                getResources().getColor(R.color.white));

        int textColor = a.getColor(R.styleable.WeatherCurveView_textColor, Color.WHITE);
        a.recycle();

        mDensity = getResources().getDisplayMetrics().density;
        mRadius = 3 * mDensity;
        mBankSpace = 3 * mDensity;
        mTextSpace = 10 * mDensity;

        float stokeWidth = 2 * mDensity;
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStrokeWidth(stokeWidth);
        mLinePaint.setStyle(Paint.Style.STROKE);

        mPointPaint = new Paint();
        mPointPaint.setAntiAlias(true);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(textColor);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public WeatherCurveView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mHeight == 0) {
            // 设置控件高度，x轴集合
            setHeightAndXAxis();
        }
        // 计算y轴集合数值
        computeYAxisValues();
        drawChart(canvas, mCuverColor, mHighTemp, mLowTemp, mYAxis);

        Log.d("weather", "curve-widht=" + getWidth());
        Log.d("weather", "curve-widht=" + getMeasuredWidth());
        if (listener != null) {
            listener.updateViewWidth(getWidth());
        }
    }

    /**
     * 计算y轴集合数值
     */
    private void computeYAxisValues() {
        // 存放最高温度集合中的最低温度
        int minHighTemp = mHighTemp[0];
        // 存放最高温度集合中的最高温度
        int maxHighTemp = mHighTemp[0];
        for (int item : mHighTemp) {
            if (item < minHighTemp) {
                minHighTemp = item;
            }
            if (item > maxHighTemp) {
                maxHighTemp = item;
            }
        }

        // 存放最低温度集合中的最低温度
        int minLowTemp = mLowTemp[0];
        // 存放最低温度集合中的最高温度
        int maxLowTemp = mLowTemp[0];
        for (int item : mLowTemp) {
            if (item < minLowTemp) {
                minLowTemp = item;
            }
            if (item > maxLowTemp) {
                maxLowTemp = item;
            }
        }

        // 份数（白天，夜间综合温差）
        float parts = maxHighTemp - minLowTemp;
        // y轴一端到控件一端的距离
        float length = mBankSpace + mTextSize + mTextSpace + mRadius;
        // y轴高度
        float yAxisHeight = mHeight - length * 2;

        // 当温度都相同时（被除数不能为0）
        if (parts == 0) {
            for (int i = 0; i < LENGTH; i++) {
                mYAxis[i] = yAxisHeight / 2 + length;
            }
        } else {
            float partValue = yAxisHeight / parts;
            for (int i = 0; i < LENGTH; i++) {
                mYAxis[i] = mHeight - partValue * (mHighTemp[i] - minLowTemp) - length;
            }
        }
    }

    /**
     * 画图
     *
     * @param canvas 画布
     * @param color  画图颜色
     * @param yAxis  y轴集合
     */
    private void drawChart(Canvas canvas, int color, int highTemp[], int lowTemp[], float[] yAxis) {
        mLinePaint.setColor(color);
        mPointPaint.setColor(color);

        int alpha2 = 255;
        for (int i = 0; i < LENGTH; i++) {
            if (i < LENGTH - 1) {
                mLinePaint.setAlpha(alpha2);
                mLinePaint.setPathEffect(null);
                Path path = new Path();
                path.moveTo(mXAxis[i], yAxis[i]);
                float wt = (mXAxis[i] + mXAxis[i + 1]) / 2;
                path.cubicTo(wt, yAxis[i], wt, yAxis[i + 1], mXAxis[i + 1], yAxis[i + 1]);
                canvas.drawPath(path, mLinePaint);
                //canvas.drawLine(mXAxis[i], yAxis[i], mXAxis[i + 1], yAxis[i + 1], mLinePaint);
            }
            mTextPaint.setAlpha(alpha2);
            drawText(canvas, mTextPaint, i, highTemp, lowTemp, yAxis);

        }
    }

    /**
     * 绘制文字
     *
     * @param canvas    画布
     * @param textPaint 画笔
     * @param i         索引
     * @param yAxis     y轴集合
     */
    private void drawText(Canvas canvas, Paint textPaint, int i, int highTemp[], int lowTemp[], float[] yAxis) {
        canvas.drawText(highTemp[i] + "°C", mXAxis[i], yAxis[i] - mRadius - mTextSpace, textPaint);
        canvas.drawText(lowTemp[i] + "°C", mXAxis[i], yAxis[i] + mTextSpace + mTextSize, textPaint);
    }

    /**
     * 设置高度，x轴集合
     */
    private void setHeightAndXAxis() {
        mHeight = getHeight();
        // 每一份宽
        float w = getWidth() / 14;
        mXAxis[0] = w;
        mXAxis[1] = w * 3;
        mXAxis[2] = w * 5;
        mXAxis[3] = w * 7;
        mXAxis[4] = w * 9;
        mXAxis[5] = w * 11;
        mXAxis[6] = w * 13;
    }

    /**
     * 设置每天最高温度
     */
    public void setHighTemp(int[] highTemp) {
        mHighTemp = highTemp;
    }

    /**
     * 设置每天最低温度
     */
    public void setLowTemp(int[] lowTemp) {
        mLowTemp = lowTemp;
    }

    public int getWeatherViewWidth() {
        return getWidth();
    }

    public UpdateViewListener listener;

    public interface UpdateViewListener {
        void updateViewWidth(int width);
    }

    public void setUpdateViewWidthListener(UpdateViewListener l) {
        this.listener = l;
    }
}
