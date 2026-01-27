package com.jieli.btsmart.ui.alarm;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.jieli.btsmart.R;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 4/1/21
 * @desc :
 */
public class NumSeekBar extends View {


    private Paint bgPaint;
    private Paint progressPaint;
    private TextPaint textPaint;
    private Bitmap bmpThumb;
    private float baseLine;

    private int currentIndex = 0;
    private float currentPos = 0;
    private float step = 0;
    private float lineCenterY = 0;
    private float halfThumbW = 0;
    private boolean isAnimator = false;

    private int[] data = new int[]{5, 10, 15, 20, 25, 30};

    private OnSelectChange onSelectChange;


    public NumSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public NumSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setDataAndValue(int[] data, int value) {
        this.data = data;
        this.currentIndex = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == value) {
                currentIndex = i;
                break;
            }
        }
        requestLayout();
    }

    public int getValue() {
        return data[currentIndex];
    }

    public void setOnSelectChange(OnSelectChange onSelectChange) {
        this.onSelectChange = onSelectChange;
    }

    public NumSeekBar(Context context) {
        this(context, null);
    }

    private void init() {
        bgPaint = new Paint();
        bgPaint.setStrokeWidth(dp2px(2));
        bgPaint.setColor(Color.parseColor("#D8D8D8"));
        progressPaint = new Paint();
        progressPaint.setStrokeWidth(dp2px(2));
        progressPaint.setColor(Color.parseColor("#805BEB"));

        textPaint = new TextPaint();
        textPaint.setColor(Color.parseColor("#4B4B4B"));
        textPaint.setTextSize(dp2px(12));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        bmpThumb = BitmapFactory.decodeResource(getResources(), R.drawable.ic_alarm_bell_interval_thumb);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);//宽度

        float space = dp2px(7);
        Paint.FontMetricsInt fontMetrics = textPaint.getFontMetricsInt();
        float textH = (textPaint.getFontMetricsInt().bottom - textPaint.getFontMetricsInt().top);
        float h = bmpThumb.getHeight() + space + textH; //高度

        halfThumbW = bmpThumb.getWidth() >> 1;
        lineCenterY = (bmpThumb.getHeight() - bgPaint.getStrokeWidth()) / 2f;//拖动条y轴位置

        //文字基线计算
        baseLine = ((fontMetrics.descent - fontMetrics.ascent) >> 1) - fontMetrics.descent;
        float textY = h - textH / 2f;
        baseLine = baseLine + textY;


        float size = data.length;
        step = (w - bmpThumb.getWidth()) / (size - 1); //文字间隔

        currentPos = currentIndex * step + halfThumbW;//当前位置
        setMeasuredDimension(w, (int) Math.floor(h));
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //背景条
        canvas.drawLine(halfThumbW, lineCenterY, getWidth() - halfThumbW, lineCenterY, bgPaint);
        //进度
        canvas.drawLine(halfThumbW, lineCenterY, currentPos, lineCenterY, progressPaint);

        //thumb
        canvas.drawBitmap(bmpThumb, currentPos - halfThumbW, 0, null);
        //text
        float size = data.length;
        for (int i = 0; i < size; i++) {
            canvas.drawText(String.valueOf(data[i]), i * step + halfThumbW, baseLine, textPaint);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return isHoldThumb(event) && !isAnimator;
            case MotionEvent.ACTION_MOVE:
                updatePos(x);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                updatePos(x);
                float lastPos = getLostPos();
                currentIndex = (int) (lastPos / step);
                animateToPos(currentPos, lastPos);
                break;
        }

        return true;
    }

    public void setCurrentPos(float currentPos) {
        this.currentPos = currentPos;
        invalidate();
    }

    private boolean isHoldThumb(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float eventPadding = bmpThumb.getWidth();
        return (x >= currentPos - eventPadding)
                && (x <= currentPos + eventPadding)
                && y >= 0
                && y < bmpThumb.getHeight();
    }

    private void updatePos(float x) {
        if (x < halfThumbW) {
            currentPos = halfThumbW;
        } else if (x > getWidth() - halfThumbW) {
            currentPos = getWidth() - halfThumbW;
        } else {
            currentPos = x;
        }
        invalidate();
    }

    private void animateToPos(float startX, float endX) {
        isAnimator = true;
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(this, "currentPos", startX, endX);
        objectAnimator.setDuration(100);
        objectAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimator = false;
                if (onSelectChange != null) {
                    onSelectChange.onSelect(currentIndex, getValue());
                }

            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isAnimator = false;
                onAnimationEnd(animation);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        objectAnimator.start();
    }

    private float getLostPos() {
        float rem = currentPos % step;
        int index = (int) (currentPos / step);
        int minPos = (int) (index * step + halfThumbW);
        float pos = minPos + (Math.round(rem / step) * step);
        return pos;
    }

    private float dp2px(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    public interface OnSelectChange {
        void onSelect(int index, int value);
    }

}
