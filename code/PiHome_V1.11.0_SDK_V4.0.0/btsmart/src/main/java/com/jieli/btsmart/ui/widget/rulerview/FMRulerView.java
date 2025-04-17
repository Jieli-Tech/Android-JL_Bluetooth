package com.jieli.btsmart.ui.widget.rulerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.jieli.btsmart.R;
import com.jieli.btsmart.util.AppUtil;

import java.util.ArrayList;

public class FMRulerView extends View {
    /**
     * Exist radio scale color
     */
    private int mExistColor = Color.RED;
    /**
     * Big Scale Color
     */
    private int mBigScaleColor = Color.BLACK;
    /**
     * Small Scale Color
     */
    private int mSmallScaleColor = Color.BLACK;
    /**
     * Text color
     */
    private int mTextColor = Color.BLACK;
    /**
     * Scale distance interval (dp）
     */
    private float mScaleDistanceInterval = 0f;
    /**
     * Scale value interval
     */
    private int mScaleValueInterval = 1;
    /**
     * MaxValue MinValue
     */
    private int mMaxValue, mMinValue;
    /**
     * Text size
     */
    private float mTextSize;
    /**
     * Big Scale Line width ,Small Scale Line width
     */
    private float mBigScaleLineWidth, mSmallScaleLineWidth;
    /**
     * text Length after decimal point
     */
    private int mRetainLen = 0;
    //    /**
//     * Center Image Scale resource id
//     */
//    private int mImageScaleResourceId = -1;
    private float mRulerViewWidth = 0;
    private float mBigScaleHeight = 0;
    private float mSmallScaleHeight = 0;
    private float mTextAndScaleMargin = 0;
    private float mAllScaleHeight = 0;
    private float mBigScaleMarginTop = 0;
    private float mSmallScaleMarginTop = 0;
    /**
     * exist Radio array
     */
    private ArrayList<Integer> mExistRadio = new ArrayList<>();
    /**
     * current center
     */
    private int mSelectedIndex;
    /**
     * scale total count
     */
    private int mScaleTotalCount = 0;
    private int mViewScopeSize;
    private Paint mExistBigLinePaint;
    private Paint mExistSmallLinePaint;
    private Paint mBigScaleLinePaint;
    private Paint mSmallScaleLinePaint;
    private Paint mTextPaint;


    public FMRulerView(Context context) {
        this(context, null);
    }

    public FMRulerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(attributeSet);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureHeight(int heightMeasureSpec) {
        int measureMode = MeasureSpec.getMode(heightMeasureSpec);
        int result = getSuggestedMinimumWidth();
        int allViewHeight = (int) Math.ceil(mAllScaleHeight + mTextSize + mTextAndScaleMargin + 1);
        switch (measureMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                result = allViewHeight;
                break;
        }
        return result;
    }

    private int measureWidth(int widthMeasureSpec) {
        int measureMode = MeasureSpec.getMode(widthMeasureSpec);
        int result = getSuggestedMinimumWidth();
        int allViewLen = (int) ((mScaleTotalCount - 1) * mScaleDistanceInterval + mRulerViewWidth);
        switch (measureMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                result = allViewLen;
                break;
            case MeasureSpec.UNSPECIFIED:
                break;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawScale(canvas);
    }

    public ArrayList<Integer> getExistRadio() {
        return mExistRadio;
    }

    public void setExistRadio(ArrayList<Integer> existRadio) {
        this.mExistRadio = existRadio;
        invalidate();
    }

    public void setMinValueAndMaxValue(int minValue, int maxValue) {
        if (minValue > 0) {
            this.mMinValue = minValue;
        }
        if (maxValue > 0) {
            this.mMaxValue = maxValue;
        }
        calculateTotal();
        requestLayout();
    }

    public int getScaleTotalCount() {
        return mScaleTotalCount;
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public int getMinValue() {
        return mMinValue;
    }

    public float getScaleDistanceInterval() {
        return mScaleDistanceInterval;
    }

    /**
     * calculate the ruler-line's amount by the maximum and the minimum value
     */
    private void calculateTotal() {
        mScaleTotalCount = ((mMaxValue - mMinValue) / mScaleValueInterval) + 1;
        Log.e("ZHM", "FMRulerView: mScaleTotalCount" + mScaleTotalCount);
    }

    private void init(AttributeSet attributeSet) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float density = displayMetrics.density;
        mTextSize = density * 14;
        mScaleDistanceInterval = density * 9;
        mBigScaleLineWidth = (float) (density * 1.5);
        mSmallScaleLineWidth = density * 1;
        mAllScaleHeight = density * 67;
        mBigScaleMarginTop = density * 17;
        mSmallScaleMarginTop = density * 24;
        mBigScaleHeight = (float) (density * 32.5);
        mSmallScaleHeight = density * 19;
        TypedArray typedArray = attributeSet == null ? null : getContext()
                .obtainStyledAttributes(attributeSet, R.styleable.fmRulerView);
        if (typedArray != null) {
            mExistColor = typedArray.getColor(R.styleable.fmRulerView_existColor, mExistColor);
            mBigScaleColor = typedArray.getColor(R.styleable.fmRulerView_bigScaleColor, mBigScaleColor);
            mSmallScaleColor = typedArray.getColor(R.styleable.fmRulerView_smallScaleColor, mSmallScaleColor);
            mTextColor = typedArray.getColor(R.styleable.fmRulerView_textColor, mTextColor);
            mScaleDistanceInterval = typedArray.getDimension(R.styleable.fmRulerView_intervalDistance, mScaleDistanceInterval);
            mScaleValueInterval = typedArray.getInteger(R.styleable.fmRulerView_intervalValue, mScaleValueInterval);
            mMaxValue = typedArray.getInteger(R.styleable.fmRulerView_maxValue, 1080);
            mMinValue = typedArray.getInteger(R.styleable.fmRulerView_minValue, 760);
            mTextSize = typedArray.getDimension(R.styleable.fmRulerView_textSize, mTextSize);
            mBigScaleLineWidth = typedArray.getDimension(R.styleable.fmRulerView_bigScaleLineWidth, mBigScaleLineWidth);
            mSmallScaleLineWidth = typedArray.getDimension(R.styleable.fmRulerView_smallScaleLineWidth, mSmallScaleLineWidth);
            mRetainLen = typedArray.getInteger(R.styleable.fmRulerView_retainLength, mRetainLen);
            mRulerViewWidth = typedArray.getDimension(R.styleable.fmRulerView_rulerViewWidth, mRulerViewWidth);
            mBigScaleHeight = typedArray.getDimension(R.styleable.fmRulerView_bigScaleHeight, mBigScaleHeight);
            mSmallScaleHeight = typedArray.getDimension(R.styleable.fmRulerView_smallScaleHeight, mSmallScaleHeight);
            mTextAndScaleMargin = typedArray.getDimension(R.styleable.fmRulerView_textAndScaleMargin, mTextAndScaleMargin);
            mAllScaleHeight = typedArray.getDimension(R.styleable.fmRulerView_allScaleHeight, mAllScaleHeight);
            mBigScaleMarginTop = typedArray.getDimension(R.styleable.fmRulerView_bigScaleMarginTop, mBigScaleMarginTop);
            mSmallScaleMarginTop = typedArray.getDimension(R.styleable.fmRulerView_smallScaleMarginTop, mSmallScaleMarginTop);
        }
        if (mRulerViewWidth == 0) {
            mRulerViewWidth = displayMetrics.widthPixels;
        }
        if (mMaxValue > 1080) {
            mMaxValue = 1080;
        }
        if (mMinValue < 760) {
            mMinValue = 760;
        }
        if (typedArray != null) {
            typedArray.recycle();
        }
        initPaint();
        calculateTotal();
    }

    private void initPaint() {
        mExistBigLinePaint = new Paint();
        mExistBigLinePaint.setColor(mExistColor);
        mExistBigLinePaint.setStrokeWidth(mBigScaleLineWidth);
        mExistBigLinePaint.setAntiAlias(true);
        mExistBigLinePaint.setStyle(Paint.Style.STROKE);

        mExistSmallLinePaint = new Paint();
        mExistSmallLinePaint.setColor(mExistColor);
        mExistSmallLinePaint.setStrokeWidth(mSmallScaleLineWidth);
        mExistSmallLinePaint.setAntiAlias(true);
        mExistSmallLinePaint.setStyle(Paint.Style.STROKE);

        mBigScaleLinePaint = new Paint();
        mBigScaleLinePaint.setColor(mBigScaleColor);
        mBigScaleLinePaint.setStrokeWidth(mBigScaleLineWidth);
        mBigScaleLinePaint.setAntiAlias(true);
        mBigScaleLinePaint.setStyle(Paint.Style.STROKE);

        mSmallScaleLinePaint = new Paint();
        mSmallScaleLinePaint.setColor(mSmallScaleColor);
        mSmallScaleLinePaint.setStrokeWidth(mSmallScaleLineWidth);
        mSmallScaleLinePaint.setAntiAlias(true);
        mSmallScaleLinePaint.setStyle(Paint.Style.STROKE);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
    }

    private void drawScale(Canvas canvas) {
        for (int i = 0; i < mScaleTotalCount; i++) {
            float offsetX = i * mScaleDistanceInterval + mRulerViewWidth / 2;
            int scaleValue = mMinValue + i;
            if ((mExistRadio != null && mExistRadio.size() > 0) && mExistRadio.contains(scaleValue)) {
                if (Math.abs(scaleValue) % 5 == 0) {
                    canvas.drawLine(offsetX - mBigScaleLineWidth / 2, mBigScaleMarginTop, offsetX - mBigScaleLineWidth / 2, mBigScaleMarginTop + mBigScaleHeight, mExistBigLinePaint);
                    String text = AppUtil.formatString("%.1f", getFloatValue(scaleValue));
                    float textWidth = mTextPaint.measureText(text);
                    canvas.drawText(text, offsetX - textWidth / 2, mAllScaleHeight + mTextSize + mTextAndScaleMargin, mTextPaint);
                } else {
                    canvas.drawLine(offsetX - mSmallScaleLineWidth / 2, mSmallScaleMarginTop, offsetX - mSmallScaleLineWidth / 2, mSmallScaleMarginTop + mSmallScaleHeight, mExistSmallLinePaint);
                }
            } else {
                if (Math.abs(scaleValue) % 5 == 0) {
                    canvas.drawLine(offsetX - mBigScaleLineWidth / 2, mBigScaleMarginTop, offsetX - mBigScaleLineWidth / 2, mBigScaleMarginTop + mBigScaleHeight, mBigScaleLinePaint);
                    String text = AppUtil.formatString("%.1f", getFloatValue(scaleValue));
                    float textWidth = mTextPaint.measureText(text);
                    canvas.drawText(text, offsetX - textWidth / 2, mAllScaleHeight + mTextSize + mTextAndScaleMargin, mTextPaint);
                } else {
                    canvas.drawLine(offsetX - mSmallScaleLineWidth / 2, mSmallScaleMarginTop, offsetX - mSmallScaleLineWidth / 2, mSmallScaleMarginTop + mSmallScaleHeight, mSmallScaleLinePaint);
                }
            }
        }
    }

    private float getFloatValue(int value) {
        return value / 10.0f;
    }
}
