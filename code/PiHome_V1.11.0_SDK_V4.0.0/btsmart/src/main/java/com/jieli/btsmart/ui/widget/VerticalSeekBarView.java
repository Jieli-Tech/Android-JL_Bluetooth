package com.jieli.btsmart.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.jieli.btsmart.R;

/**
 * TODO: document your custom view class.
 */
public class VerticalSeekBarView extends View {

    private TextPaint mTextPaint;
    private TextPaint mLabelTextPaint;


    private Paint mSelectedProgressPaint;
    private Paint mProgressPaint;
    private String mText = "125";
    private int currentValue = 0;
    private PointF mLastPoint = new PointF();
    public ValueListener mValueListener;
    public HoverListener mHoverListener;
    private boolean enable = false;

    private Bitmap thumbImage = BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_eq_btn_slider_nor);
    private Bitmap thumbSelectedImage = BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_eq_btn_slider_sel);

    private Bitmap labelImage = BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_eq_sb_labal_bg);


    private boolean mActive;

    private int index = 0;
    private int min = -8;
    private int max = 8;

    public void setIndex(int index) {
        this.index = index;
    }

    public VerticalSeekBarView(Context context) {
        super(context);
        init(null, 0);
    }

    public VerticalSeekBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public VerticalSeekBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(sp2px(12));
        mTextPaint.setColor(getContext().getResources().getColor(R.color.gray_8B8B8B));

        mLabelTextPaint = new TextPaint();
        mLabelTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mLabelTextPaint.setTextAlign(Paint.Align.CENTER);
        mLabelTextPaint.setTextSize(sp2px(14));
        mLabelTextPaint.setColor(Color.WHITE);

        mSelectedProgressPaint = new Paint();
        mSelectedProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        mSelectedProgressPaint.setStrokeWidth(dp2px(3));
        setEnable(true);


        mProgressPaint = new Paint();
        mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        mProgressPaint.setStrokeWidth(dp2px(3));
        mProgressPaint.setColor(getContext().getResources().getColor(R.color.gray_E5E5E5));
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        canvas.drawColor(Color.BLUE);
        float currentProgressY = mLastPoint.y;
        float x = getWidth() / 2f;
        Bitmap bitmap = isActive() ? thumbSelectedImage : thumbImage;
        canvas.drawLine(x, getProgressStartY(), x, getProgressEndY(), mProgressPaint);
        //处理部分手机startY和endY相同时起点无效并自动默认为原点（0,0）的问题，目前发现三星SM-G9200有此问题
        if (currentProgressY != getProgressEndY()) {
            canvas.drawLine(x, currentProgressY, x, getProgressEndY(), mSelectedProgressPaint);
        }
        float thumbY = currentProgressY - bitmap.getHeight() / 2f;
        float thumbX = (getWidth() - bitmap.getWidth()) / 2f;
        canvas.drawBitmap(bitmap, thumbX, thumbY, null);
        canvas.drawText(mText, x, getHeight() - sp2px(2), mTextPaint);
//        JL_Log.e("sen","currentProgressY="+currentProgressY+"\tvalue="+currentValue+"\thash="+hashCode());
        if (isActive()) {
            x = (getWidth() - labelImage.getWidth()) / 2f;
            float y = thumbY - labelImage.getHeight() + bitmap.getHeight() / 4f;
            canvas.drawBitmap(labelImage, x, y, null);
            x = getWidth() / 2f;
            y = y + labelImage.getHeight() / 1.65f;
            canvas.drawText(currentValue + "", x, y, mLabelTextPaint);
        }

    }


    public void setText(String text) {
        this.mText = text;
        invalidate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mActive = isThumbHover(event.getX(), event.getY());
        }
        if (isActive()) {
            getParent().requestDisallowInterceptTouchEvent(true);
            return super.dispatchTouchEvent(event);
        } else {
            getParent().requestDisallowInterceptTouchEvent(false);
            return false;
        }


    }


    public void setEnable(boolean enable) {
        this.enable = enable;
        mSelectedProgressPaint.setColor(!enable ? getContext().getResources().getColor(R.color.gray_bbbbbb) :
                getContext().getResources().getColor(R.color.colorPrimary));
        invalidate();
    }

    public boolean isEnable() {
        return enable;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float div = getProgressHeight() / (max - min);
        float y = (max - currentValue) / (max - min * 1.0f) * getProgressHeight() + getProgressStartY();
        if (Math.abs(mLastPoint.y - y) > div) {
            mLastPoint.set(0, getAvailableY(y));
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!enable) {
            mActive = false;
            return super.onTouchEvent(event);
        }
        float y = getAvailableY(event.getY());
        boolean end = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mHoverListener != null) {
                    mHoverListener.onChange(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_CANCEL:
                y = mLastPoint.y;
            case MotionEvent.ACTION_UP:
                end = true;
                if (mHoverListener != null) {
                    mHoverListener.onChange(false);
                }
                getParent().requestDisallowInterceptTouchEvent(false);
                mActive = false;
                break;
        }
        invalidate();
        int value = calculateValue(y);
        mLastPoint.set(event.getX(), y);
        if ((MotionEvent.ACTION_DOWN != event.getAction() && value != currentValue) || end) {
            currentValue = value;
            if (mValueListener != null) {
                mValueListener.onChange(currentValue, end);
            }
        }
        return isActive();
    }


    public void setValue(int value) {
        //去设置抖动问题
        if (isActive()) {
            //滑动状态或者值相等则忽略设置值
            return;
        }

        if (value < min) {
            value = min;
        } else if (value > max) {
            value = max;
        }
        float y = (max - value) / (1.0f * max - min) * getProgressHeight() + getProgressStartY();
        mLastPoint.set(0, getAvailableY(y));
        currentValue = value;
        invalidate();
    }


    public void setValueListener(ValueListener valueListener) {
        this.mValueListener = valueListener;
    }

    public void setHoverListener(HoverListener hoverListener) {
        this.mHoverListener = hoverListener;
    }

    public interface ValueListener {
        void onChange(int value, boolean end);
    }


    public interface HoverListener {
        void onChange(boolean hover);
    }


    private boolean isThumbHover(float x, float y) {

        float currentY = mLastPoint.y;
        float bmpHalfHeight = thumbImage.getHeight() / 2f;
        return y < currentY + bmpHalfHeight && y > currentY - bmpHalfHeight;
    }


    private int calculateValue(float y) {
        float value = max - (y - getProgressStartY()) / getProgressHeight() * (max - min);
        return (int) value;
    }

    private boolean isActive() {
        return mActive;
    }

    private float getProgressHeight() {
        return getHeight() - thumbImage.getHeight() / 1.1f - getProgressStartY();
    }

    private float getProgressEndY() {
        return getProgressStartY() + getProgressHeight();
    }


    private float getProgressStartY() {
        return thumbImage.getHeight() * 1.15f;
    }

    private float getAvailableY(float y) {
        if (y < getProgressStartY()) {
            return getProgressStartY();
        } else if (y > getProgressEndY()) {
            return getProgressEndY();
        }
        return y;
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
