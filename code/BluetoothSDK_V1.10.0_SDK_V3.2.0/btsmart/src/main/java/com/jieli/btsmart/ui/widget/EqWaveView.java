package com.jieli.btsmart.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import com.jieli.btsmart.R;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.EqCovertUtil;
import com.jieli.component.utils.ValueUtil;
import com.jieli.eq.EQPlotCore;

public class EqWaveView extends View {
    private TextPaint mTextPaint;
    private Paint mLinePaint;
    private Paint mTopLinePaint;
    private Paint mScalePaint;
    private int noteCounts = 2000;
    private EQPlotCore mEQPlotCore;
    private float[] mScreenPointData;
    private int[] mEqValues = new int[10];
    private EqCovertUtil mCovert = null;
    private float[] mEqPointData;
    private Path mPath;

    private int mTopLineColor = 0xFF805BEB;
    private int mContentStartColor = 0xFF805BEB;
    private int mContentEndColor = 0x7f5e41eb;

    private int mTopLineDisableColor = 0xD4D4D4;
    private int mContentStartDisableColor = 0xBBBBBB;
    private int mContentEndDisableColor = 0xBBBBBB;

    private int[] mDefaultFreqs = new int[]{31, 63, 125, 250, 500, 1000, 2000, 8000, 16000};


    public void setFreqs(int[] freqs) {
        //如果频率相同则不更新频率
        if (freqs.length == mDefaultFreqs.length) {
            boolean same = true;
            for (int i = 0; i < freqs.length; i++) {
                if (freqs[i] != mDefaultFreqs[i]) {
                    same = false;
                }
            }
            if (same) {
                return;
            }
        }

        this.mDefaultFreqs = freqs;
        if (mCovert != null) {
            mCovert.setPadding(ValueUtil.dp2px(AppUtil.getContext(), 20));
            mCovert.setStartAndEndFreq(mDefaultFreqs[0], mDefaultFreqs[mDefaultFreqs.length - 1]);
        }
        mEQPlotCore = new EQPlotCore(noteCounts, mDefaultFreqs.length, mDefaultFreqs);
        if (mEqPointData == null) {
            mEqPointData = new float[4 * (noteCounts - 2) + 4];
        }
        setData(mEqValues);
    }

    public EqWaveView(Context context) {
        super(context);
        init(null, 0);
    }

    public EqWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public EqWaveView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.EqWaveView);
        if (typedArray != null) {
            mTopLineColor = typedArray.getColor(R.styleable.EqWaveView_topLineColor, mTopLineColor);
            mTopLineDisableColor = typedArray.getColor(R.styleable.EqWaveView_topLineDisableColor, mTopLineDisableColor);

            mContentStartColor = typedArray.getColor(R.styleable.EqWaveView_contentStartColor, mContentStartColor);
            mContentStartDisableColor = typedArray.getColor(R.styleable.EqWaveView_contentStartDisableColor, mContentStartDisableColor);

            mContentEndColor = typedArray.getColor(R.styleable.EqWaveView_contentEndColor, mContentEndColor);
            mContentEndDisableColor = typedArray.getColor(R.styleable.EqWaveView_contentEndDisableColor, mContentEndDisableColor);

        }
        DisplayMetrics metric = getContext().getApplicationContext().getResources()
                .getDisplayMetrics();

        noteCounts = metric.widthPixels;
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setTextSize(sp2px(8));
        mTextPaint.setColor(Color.WHITE);


        mLinePaint = new Paint();
        mLinePaint.setColor(mContentStartColor);
        mLinePaint.setTextAlign(Paint.Align.CENTER);
        mLinePaint.setStrokeWidth(5);

        mTopLinePaint = new Paint();
        mTopLinePaint.setColor(mTopLineColor);
        mTopLinePaint.setTextAlign(Paint.Align.CENTER);
        mTopLinePaint.setStrokeWidth(dp2px(1.5f));
        mTopLinePaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        mScalePaint = new Paint();
        mScalePaint.setColor(Color.WHITE);
        mScalePaint.setTextAlign(Paint.Align.CENTER);
        mScalePaint.setStrokeWidth(1);

        mPath = new Path();
        initEqArg();

    }

    private void initEqArg() {
        mEQPlotCore = new EQPlotCore(noteCounts, mDefaultFreqs.length, mDefaultFreqs);
        if (mEqPointData == null) {
            mEqPointData = new float[4 * (noteCounts - 2) + 4];
        }
    }

    //刷新所有频率的增益
    public void setData(int[] value) {
        for (int i = 0; i < mDefaultFreqs.length && i < value.length; i++) {
            mEQPlotCore.updatePara(i, mDefaultFreqs[i], value[i]);
            mEqValues[i] = value[i];
            mEQPlotCore.getEQPlotData(mEqPointData, i);
        }
        if (mCovert == null) {
            return;
        }
        mScreenPointData = mCovert.pPoint2SPoint(mEqPointData);
        invalidate();
    }


    //刷新指定频率的增益
    public void updateData(int index, int value) {
        mEqValues[index] = value;
        mEQPlotCore.updatePara(index, mDefaultFreqs[index], value);
        if (mEqPointData == null) {
            mEqPointData = new float[4 * (noteCounts - 2) + 4];
        }
        float gain = mEQPlotCore.getEQPlotData(mEqPointData, index);
        mScreenPointData = mCovert.pPoint2SPoint(mEqPointData);
        invalidate();
    }


    public void setEnabled(boolean enable) {
        super.setEnabled(enable);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCovert = new EqCovertUtil(w, h - sp2px(6));
        mCovert.setPadding(ValueUtil.dp2px(AppUtil.getContext(), 20));
        mCovert.setStartAndEndFreq(mDefaultFreqs[0], mDefaultFreqs[mDefaultFreqs.length - 1]);
        if (mEqPointData == null) {
            mEqPointData = new float[4 * (noteCounts - 2) + 4];
        }

        mEQPlotCore.getEQPlotData(mEqPointData, 0);
        setData(mEqValues);
        mScreenPointData = mCovert.pPoint2SPoint(mEqPointData);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        canvas.drawColor(Color.RED);

        Shader shader = new LinearGradient(0, canvas.getHeight(), 0, 0,
                isEnabled() ? mContentEndColor : mContentEndDisableColor, isEnabled() ? mContentStartColor : mContentStartDisableColor, Shader.TileMode.REPEAT);
        mLinePaint.setShader(shader);

        mTopLinePaint.setColor(!isEnabled() ? mTopLineDisableColor : mTopLineColor);
        mPath.reset();
        mPath.moveTo(0, getHeight());
        for (int i = 0; i < mScreenPointData.length; i += 2) {
            mPath.lineTo(mScreenPointData[i], mScreenPointData[i + 1]);
        }
        mPath.lineTo(getWidth(), mScreenPointData[mScreenPointData.length - 1]);
        canvas.drawPath(mPath, mTopLinePaint);
        mPath.lineTo(getWidth(), getHeight());
        mPath.lineTo(0, getHeight());
        canvas.drawPath(mPath, mLinePaint);
        canvas.clipPath(mPath);
        drawScale(canvas);
    }


    private void drawScale(Canvas canvas) {
        for (int i = 0; i < mDefaultFreqs.length; i++) {
            float x = mCovert.px2sx(mDefaultFreqs[i]);
            canvas.drawLine(x, 0, x, getHeight(), mScalePaint);
            canvas.drawText(mEqValues[i] + "", x + sp2px(2), getHeight() - 2, mTextPaint);
        }
    }


    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    private int sp2px(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                getResources().getDisplayMetrics());
    }


}
