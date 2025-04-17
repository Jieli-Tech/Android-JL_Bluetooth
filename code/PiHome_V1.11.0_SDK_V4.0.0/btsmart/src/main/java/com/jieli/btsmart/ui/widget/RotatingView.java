package com.jieli.btsmart.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.jieli.btsmart.R;


/**
 * 温度控制
 * Created by yangle on 2016/11/29.
 */
public class RotatingView extends View {

    private int width;
    private int height;
    private int arcRadius;

    private Paint textValuePaint;
    private Paint arcPaint;
    private Paint buttonPaint;
    private Paint arc2Paint;

    private int min = -12;
    private int max = 12;
    private int currentValue = min;
    private int angleRate = 1;


    private Bitmap buttonImage = BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_rotatview_thumb);
    private Bitmap indicatorImage = BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_rotatview_indicator_sup);
    private Bitmap reduceImage = BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_rotatview_reduce);
    private Bitmap plusImage = BitmapFactory.decodeResource(getResources(),
            R.drawable.ic_rotatview_plus);


    private PaintFlagsDrawFilter paintFlagsDrawFilter;

    private OnValueChangeListener onValueChangeListener;

    private OnClickListener onClickListener;

    private int mContentStartColor = 0xFF805BEB;
    private int mContentEndColor = 0x7f5e41eb;
    private int mContentTextColor = 0xff575757;
    // 当前按钮旋转的角度
    private float rotateAngle;
    // 当前的角度
    private float currentAngle;

    private int mContentLineWidth = dp2px(5);
    private int mBackgroundLineWidth = dp2px(3);
    private int mPaddingWidth = dp2px(3);
    private int mTextSize = sp2px(16);
    SweepGradient sweepGradient;
    private int startAngle = 130;
    private float angleOne = 1.0f * getMaxRotateAngle() / (max - min) / angleRate;

    public RotatingView(Context context) {
        this(context, null);
    }

    public RotatingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RotatingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.RotatingView);
        if (typedArray != null) {
            mContentStartColor = typedArray.getColor(R.styleable.RotatingView_contentStartColor, mContentStartColor);
            mContentEndColor = typedArray.getColor(R.styleable.RotatingView_contentEndColor, mContentEndColor);
            mContentTextColor = typedArray.getColor(R.styleable.RotatingView_contentTextColor, mContentTextColor);
            mContentLineWidth = typedArray.getDimensionPixelSize(R.styleable.RotatingView_contentLineWidth, mContentLineWidth);
            mBackgroundLineWidth = typedArray.getDimensionPixelSize(R.styleable.RotatingView_backgroundLineWidth, mBackgroundLineWidth);
            mPaddingWidth = typedArray.getDimensionPixelSize(R.styleable.RotatingView_paddingWidth, mPaddingWidth);
            mTextSize = typedArray.getDimensionPixelSize(R.styleable.RotatingView_rTextSize, mTextSize);
            int imageResId = typedArray.getResourceId(R.styleable.RotatingView_indicatorImage, -1);
            if (imageResId != -1) {
                indicatorImage = BitmapFactory.decodeResource(getResources(), imageResId);
            }
        }
        textValuePaint = new Paint();
        textValuePaint.setAntiAlias(true);
        textValuePaint.setTextSize(mTextSize);
        textValuePaint.setColor(mContentTextColor);
        textValuePaint.setTextAlign(Paint.Align.CENTER);


        arcPaint = new Paint();
        arcPaint.setAntiAlias(true);
        arcPaint.setColor(Color.parseColor("#3CB7EA"));
        arcPaint.setStrokeWidth(mBackgroundLineWidth);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        arcPaint.setColor(0xffe1e1e1);

        buttonPaint = new Paint();
        paintFlagsDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        arc2Paint = new Paint();
        arc2Paint.setAntiAlias(true);
        arc2Paint.setStrokeWidth(mContentLineWidth);
        arc2Paint.setStyle(Paint.Style.STROKE);
        arc2Paint.setStrokeCap(Paint.Cap.ROUND);


        int[] colors = {mContentStartColor, mContentStartColor, mContentEndColor, mContentStartColor};
        float[] postions = {0f, 0.05f, 0.8f, 0.99f};
        sweepGradient = new SweepGradient(0, 0, colors, postions);
        arc2Paint.setShader(sweepGradient);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 控件宽、高
        width = height = Math.min(h, w);
        // 圆弧半径
        arcRadius = width / 2 - mPaddingWidth * 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawArc(canvas);
        drawButton(canvas);
        drawIndicator(canvas);
        drawSymbol(canvas);
        drawText(canvas);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //noinspection SuspiciousNameCombination
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureWidth(heightMeasureSpec));
    }

    private int measureWidth(int widthMeasureSpec) {
        int measureMode = MeasureSpec.getMode(widthMeasureSpec);
        int measureSize = MeasureSpec.getSize(widthMeasureSpec);
        int result = measureSize;
        switch (measureMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                result = measureSize;
                break;
            default:
                break;
        }
        return result;
    }


    /**
     * 绘制刻度盘下的圆弧
     *
     * @param canvas 画布
     */
    private void drawArc(Canvas canvas) {
        canvas.save();
        canvas.translate(getWidth() / 2f, getHeight() / 2f);
        canvas.rotate(startAngle);
        RectF rectF = new RectF(-arcRadius, -arcRadius, arcRadius, arcRadius);
        canvas.drawArc(rectF, 0, getMaxRotateAngle(), false, arcPaint);
        canvas.drawArc(rectF, 0, rotateAngle, false, arc2Paint);
        canvas.restore();
    }

    private void drawText(Canvas canvas) {
        String text = currentValue + "";
        Rect bounds = new Rect();
        textValuePaint.getTextBounds(text, 0, text.length(), bounds);
        int textX = getWidth() / 2;
        int textY = getHeight() / 2 + bounds.height() / 2;
        canvas.drawText(currentValue + "", textX, textY, textValuePaint);

    }

    private void drawSymbol(Canvas canvas) {
        float sinW = (float) (arcRadius * Math.sin(Math.toRadians((startAngle - 90))));

        float x = getWidth() / 2f - sinW - reduceImage.getWidth() / 2f;
        float y = getHeight() - plusImage.getWidth();
        canvas.drawBitmap(reduceImage, x, y, null);
        x = getWidth() / 2f + sinW - reduceImage.getWidth() / 2f;
        canvas.drawBitmap(plusImage, x, y, null);

    }


    /**
     * 绘制刻度盘下的圆弧
     *
     * @param canvas 画布
     */
    private void drawIndicator(Canvas canvas) {
        canvas.save();
        canvas.rotate(rotateAngle + startAngle - 180, getWidth() / 2f, getHeight() / 2f);
        Bitmap bitmap = indicatorImage;
        float x = width / 2.0f - arcRadius - bitmap.getWidth() / 2.0f;
        float y = getHeight() / 2.0f - bitmap.getHeight() / 2.0f;
        canvas.setDrawFilter(paintFlagsDrawFilter);
        canvas.drawBitmap(bitmap, x, y, buttonPaint);
        canvas.restore();

    }


    private int getMaxRotateAngle() {
        return 360 - (startAngle - 90) * 2;
    }

    /**
     * 绘制旋转按钮
     *
     * @param canvas 画布
     */
    private void drawButton(Canvas canvas) {
        int paddingDp = (int) (arc2Paint.getStrokeWidth() * 1.2);
        Bitmap bitmap = zoomImg(buttonImage, arcRadius * 2 - paddingDp, arcRadius * 2 - paddingDp);
        // 按钮宽高
        int buttonWidth = bitmap.getWidth();
        int buttonHeight = bitmap.getHeight();
        Matrix matrix = new Matrix();
        // 设置按钮位置，移动到控件中心
        matrix.setTranslate((width - buttonWidth) / 2f, (height - buttonHeight) / 2f);
        // 设置旋转角度，旋转中心为控件中心，当前也是按钮中心
//        matrix.postRotate(-135 + rotateAngle, width / 2, height / 2);
        //设置抗锯齿
        canvas.setDrawFilter(paintFlagsDrawFilter);
        canvas.drawBitmap(bitmap, matrix, buttonPaint);
    }


    public static Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
    }


    private boolean isDown;
    private boolean isMove;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return super.dispatchTouchEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isClickable()) return true;
        int lastValue = currentValue;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDown = true;
                float downX = event.getX();
                float downY = event.getY();
                currentAngle = calcAngle(downX, downY);
                setPressed(true);
                break;
            case MotionEvent.ACTION_MOVE:
                isMove = true;
                float targetX;
                float targetY;
                downX = targetX = event.getX();
                downY = targetY = event.getY();
                float angle = calcAngle(targetX, targetY);
                // 滑过的角度增量
                float angleIncreased = angle - currentAngle;

                // 防止越界
                if (angleIncreased < -getMaxRotateAngle()) {
                    angleIncreased = angleIncreased + 360;
                } else if (angleIncreased > getMaxRotateAngle()) {
                    angleIncreased = angleIncreased - 360;
                }

                IncreaseAngle(angleIncreased);
                currentAngle = angle;
                // 回调温度改变监听
                if (onValueChangeListener != null
                        && isDown
                        && currentValue != lastValue) {
                    onValueChangeListener.change(this, currentValue, false);
                }
                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                setPressed(false);
                if (isDown) {
                    if (isMove) {
                        // 纠正指针位置
                        rotateAngle = (currentValue - min) * angleRate * angleOne;
                        // 回调温度改变监听
                        if (onValueChangeListener != null) {
                            onValueChangeListener.change(this, currentValue, true);
                        }
                        invalidate();
                        isMove = false;
                    } else {
                        // 点击事件
                        if (onClickListener != null) {
                            onClickListener.onClick(currentValue);
                        }
                    }
                    isDown = false;
                }
                break;
            }
        }


        return true;
    }


    /**
     * 以按钮圆心为坐标圆点，建立坐标系，求出(targetX, targetY)坐标与x轴的夹角
     *
     * @param targetX x坐标
     * @param targetY y坐标
     * @return (targetX, targetY)坐标与x轴的夹角
     */
    private float calcAngle(float targetX, float targetY) {
        float x = targetX - width / 2f;
        float y = targetY - height / 2f;
        double radian;

        if (x != 0) {
            float tan = Math.abs(y / x);
            if (x > 0) {
                if (y >= 0) {
                    radian = Math.atan(tan);
                } else {
                    radian = 2 * Math.PI - Math.atan(tan);
                }
            } else {
                if (y >= 0) {
                    radian = Math.PI - Math.atan(tan);
                } else {
                    radian = Math.PI + Math.atan(tan);
                }
            }
        } else {
            if (y > 0) {
                radian = Math.PI / 2;
            } else {
                radian = -Math.PI / 2;
            }
        }
        return (float) ((radian * 180) / Math.PI);
    }

    /**
     * 增加旋转角度
     *
     * @param angle 增加的角度
     */
    private void IncreaseAngle(float angle) {
        rotateAngle += angle;
        if (rotateAngle < 0) {
            rotateAngle = 0;
        } else if (rotateAngle > 360 - (startAngle - 90) * 2) {
            rotateAngle = 360 - (startAngle - 90) * 2;
        }
        // 加上0.5是为了取整时四舍五入
        currentValue = (int) ((rotateAngle / angleOne) / angleRate + 0.5) + min;
    }


    /**
     * 设置温度
     *
     * @param value 设置的值
     */
    public void setValue(int value) {
        setValue(min, max, value);
    }

    /**
     * 设置温度
     *
     * @param min   最小值
     * @param max   最大值
     * @param value 设置的值
     */
    public void setValue(int min, int max, int value) {

        if (min == this.min
                && this.max == max
                && this.currentValue == value) {
            return;
        }


        this.min = min;
        this.max = max;
        value = Math.max(min, value);
        this.currentValue = value;
        // 计算每格的角度
        angleOne = (float) getMaxRotateAngle() / (max - min) / angleRate;
        // 计算旋转角度
        rotateAngle = (value - min) * angleRate * angleOne;
        invalidate();
    }


    /**
     * 设置值改变监听
     *
     * @param onValueChangeListener 监听接口
     */
    public void setOnValueChangeListener(OnValueChangeListener onValueChangeListener) {
        this.onValueChangeListener = onValueChangeListener;
    }

    /**
     * 设置点击监听
     *
     * @param onClickListener 点击回调接口
     */
    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public int getValue() {
        return currentValue;
    }

    public void setContentStartColor(int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mContentStartColor = getResources().getColor(colorResId, null);
        } else {
            mContentStartColor = getResources().getColor(colorResId);
        }
        int[] colors = {mContentStartColor, mContentStartColor, mContentEndColor, mContentStartColor};
        float[] postions = {0f, 0.05f, 0.8f, 0.99f};
        sweepGradient = new SweepGradient(0, 0, colors, postions);
        arc2Paint.setShader(sweepGradient);
    }

    public void setContentEndColor(int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mContentEndColor = getResources().getColor(colorResId, null);
        } else {
            mContentEndColor = getResources().getColor(colorResId);
        }
        int[] colors = {mContentStartColor, mContentStartColor, mContentEndColor, mContentStartColor};
        float[] postions = {0f, 0.05f, 0.8f, 0.99f};
        sweepGradient = new SweepGradient(0, 0, colors, postions);
        arc2Paint.setShader(sweepGradient);
    }

    public void setContentTextColor(int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mContentTextColor = getResources().getColor(colorResId, null);
        } else {
            mContentTextColor = getResources().getColor(colorResId);
        }
        textValuePaint.setColor(mContentTextColor);
    }

    public void setIndicatorImage(int imageResId) {
        indicatorImage = BitmapFactory.decodeResource(getResources(), imageResId);
    }

    /**
     * 值改变监听接口
     */
    public interface OnValueChangeListener {
        /**
         * 回调方法
         *
         * @param value 值
         */
        void change(RotatingView view, int value, boolean end);
    }

    /**
     * 点击回调接口
     */
    public interface OnClickListener {
        /**
         * 点击回调方法
         *
         * @param temp 温度
         */
        void onClick(int temp);
    }

    public int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    private int sp2px(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                getResources().getDisplayMetrics());
    }
}
