package com.jieli.btsmart.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.component.utils.ValueUtil;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;

/**
 * Created by lsp on 2017/10/13 10 47
 * Email:6161391073@qq.com
 */

public class RulerView extends View {
    private static final String TAG = "RulerView";
    /**
     * 2个大刻度之间间距，默认为1
     */
    private int scaleLimit = 1;
    /**
     * 尺子高度
     */
    private int rulerHeight = 50;

    /**
     * 尺子和屏幕顶部以及结果之间的高度
     */
    private int rulerToResultgap = rulerHeight / 4;
    /**
     * 刻度平分多少份
     */
    private int scaleCount = 10;  //刻度评分多少份
    /**
     * 刻度间距
     */
    private int scaleGap = 20;
    /**
     * 刻度最小值
     */
    private int minScale = 0;
    /**
     * 第一次显示的刻度
     */
    private float firstScale = 50f;
    /**
     * 刻度最大值
     */
    private float maxScale = 100;
    /**
     * 刻度最大值
     */
    private float maxScaleValue = 100;
    /**
     * 背景颜色
     */
    private int bgColor = 0xfffcfffc;
    /**
     * 小刻度的颜色
     */
    private int smallScaleColor = 0xff999999;
    /**
     * 中刻度的颜色
     */
    private int midScaleColor = 0xff666666;
    /**
     * 大刻度的颜色
     */
    private int largeScaleColor = 0xff50b586;
    /**
     * 刻度颜色
     */
    private int scaleNumColor = 0xff333333;
    /**
     * 结果值颜色
     */
    private int resultNumColor = 0xff50b586;
    /**
     * kg颜色
     */
    private String unit = "kg";
    /**
     * kg颜色
     */
    private int unitColor = 0xff50b586;
    /**
     * 小刻度粗细大小
     */
    private int smallScaleStroke = 1;
    /**
     * 中刻度粗细大小
     */
    private int midScaleStroke = 2;
    /**
     * 大刻度粗细大小
     */
    private int largeScaleStroke = 3;
    /**
     * 结果字体大小
     */
    private int resultNumTextSize = 20;
    /**
     * 刻度字体大小
     */
    private int scaleNumTextSize = 16;
    /**
     * 单位字体大小
     */
    private int unitTextSize = 13;
    /**
     * 是否显示刻度结果
     */
    private boolean showScaleResult = true;
    /**
     * 是否背景显示圆角
     */
    private boolean isBgRoundRect = true;

    /**
     * 结果回调
     */
    private OnChooseResulterListener onChooseResulterListener;
    /**
     * 滑动选择刻度
     */
    private float computeScale = -1;
    /**
     * 当前刻度
     */
    public float currentScale = firstScale;

    private ValueAnimator valueAnimator;
    private final VelocityTracker velocityTracker = VelocityTracker.obtain();
    private String resultText = String.valueOf(firstScale);
    private Paint bgPaint;
    private Paint smallScalePaint;
    private Paint midScalePaint;
    private Paint lagScalePaint;
    private Paint scaleNumPaint;
    private Paint resultNumPaint;
    private Paint kgPaint;
    private Rect scaleNumRect;
    private Rect resultNumRect;
    private Rect kgRect;
    private RectF bgRect;
    private int height, width;
    private int smallScaleHeight;
    private int midScaleHeight;
    private int lagScaleHeight;
    private int rulerRight = 0;
    private int resultNumRight;
    private float downX;
    private float moveX = 0;
    private float currentX;
    private float lastMoveX = 0;
    private boolean isUp = false;
    private int leftScroll;
    private int rightScroll;
    private int xVelocity;

    private int drawStartY;

    private int readMaxScale;

    public RulerView(Context context) {
        this(context, null);
    }

    public RulerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setAttr(attrs, defStyleAttr);
        init();
    }

    public void setOnChooseResulterListener(OnChooseResulterListener onChooseResulterListener) {
        this.onChooseResulterListener = onChooseResulterListener;
    }

    private void setAttr(AttributeSet attrs, int defStyleAttr) {

        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.RulerView, defStyleAttr, 0);

        scaleLimit = a.getInt(R.styleable.RulerView_scaleLimit, scaleLimit);

        rulerHeight = a.getDimensionPixelSize(R.styleable.RulerView_rulerHeight, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, rulerHeight, getResources().getDisplayMetrics()));

        rulerToResultgap = a.getDimensionPixelSize(R.styleable.RulerView_rulerToResultgap, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, rulerToResultgap, getResources().getDisplayMetrics()));

        scaleCount = a.getInt(R.styleable.RulerView_scaleCount, scaleCount);

        scaleGap = a.getDimensionPixelSize(R.styleable.RulerView_scaleGap, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, scaleGap, getResources().getDisplayMetrics()));
        scaleGap = ValueUtil.dp2px(getContext(), scaleGap);

        minScale = a.getInt(R.styleable.RulerView_minScale, minScale) / scaleLimit;

        firstScale = a.getFloat(R.styleable.RulerView_firstScale, firstScale) / scaleLimit;

        maxScale = a.getFloat(R.styleable.RulerView_maxScale, maxScale) / scaleLimit;

        bgColor = a.getColor(R.styleable.RulerView_rulerBgColor, bgColor);

        smallScaleColor = a.getColor(R.styleable.RulerView_rulerSmallScaleColor, smallScaleColor);

        midScaleColor = a.getColor(R.styleable.RulerView_midScaleColor, midScaleColor);

        largeScaleColor = a.getColor(R.styleable.RulerView_largeScaleColor, largeScaleColor);

        scaleNumColor = a.getColor(R.styleable.RulerView_scaleNumColor, scaleNumColor);

        resultNumColor = a.getColor(R.styleable.RulerView_resultNumColor, resultNumColor);

        unitColor = a.getColor(R.styleable.RulerView_unitColor, unitColor);

        String tempUnit = unit;
        unit = a.getString(R.styleable.RulerView_unit);
        if (TextUtils.isEmpty(unit)) {
            unit = tempUnit;
        }

        smallScaleStroke = a.getDimensionPixelSize(R.styleable.RulerView_smallScaleStroke, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, smallScaleStroke, getResources().getDisplayMetrics()));

        midScaleStroke = a.getDimensionPixelSize(R.styleable.RulerView_midScaleStroke, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, midScaleStroke, getResources().getDisplayMetrics()));
        largeScaleStroke = a.getDimensionPixelSize(R.styleable.RulerView_largeScaleStroke, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, largeScaleStroke, getResources().getDisplayMetrics()));
        resultNumTextSize = a.getDimensionPixelSize(R.styleable.RulerView_resultNumTextSize, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, resultNumTextSize, getResources().getDisplayMetrics()));

        scaleNumTextSize = a.getDimensionPixelSize(R.styleable.RulerView_scaleNumTextSize, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, scaleNumTextSize, getResources().getDisplayMetrics()));

        unitTextSize = a.getDimensionPixelSize(R.styleable.RulerView_unitTextSize, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, unitTextSize, getResources().getDisplayMetrics()));


        showScaleResult = a.getBoolean(R.styleable.RulerView_showScaleResult, showScaleResult);
        isBgRoundRect = a.getBoolean(R.styleable.RulerView_isBgRoundRect, isBgRoundRect);

        a.recycle();
    }


    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        smallScalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        midScalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lagScalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scaleNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        resultNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        kgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        bgPaint.setColor(bgColor);
        smallScalePaint.setColor(smallScaleColor);
        midScalePaint.setColor(midScaleColor);
        lagScalePaint.setColor(largeScaleColor);
        scaleNumPaint.setColor(scaleNumColor);
        resultNumPaint.setColor(resultNumColor);
        kgPaint.setColor(unitColor);

        resultNumPaint.setStyle(Paint.Style.FILL);
        kgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setStyle(Paint.Style.FILL);
        smallScalePaint.setStyle(Paint.Style.FILL);
        midScalePaint.setStyle(Paint.Style.FILL);
        lagScalePaint.setStyle(Paint.Style.FILL);

        lagScalePaint.setStrokeCap(Paint.Cap.ROUND);
        midScalePaint.setStrokeCap(Paint.Cap.ROUND);
        smallScalePaint.setStrokeCap(Paint.Cap.ROUND);

        smallScalePaint.setStrokeWidth(smallScaleStroke);
        midScalePaint.setStrokeWidth(midScaleStroke);
        lagScalePaint.setStrokeWidth(largeScaleStroke);

        resultNumPaint.setTextSize(resultNumTextSize);
        kgPaint.setTextSize(unitTextSize);
        scaleNumPaint.setTextSize(scaleNumTextSize);

        bgRect = new RectF();
        resultNumRect = new Rect();
        scaleNumRect = new Rect();
        kgRect = new Rect();

        resultNumPaint.getTextBounds(resultText, 0, resultText.length(), resultNumRect);
        kgPaint.getTextBounds(resultText, 0, 1, kgRect);

        smallScaleHeight = rulerHeight / 4;
        midScaleHeight = rulerHeight / 2;
        lagScaleHeight = rulerHeight; //rulerHeight / 2 + 5;
        drawStartY = rulerHeight / 2;
        valueAnimator = new ValueAnimator();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int heightModule = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        switch (heightModule) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED:
                height = rulerHeight + (showScaleResult ? resultNumRect.height() : 0) + rulerToResultgap * 2 + getPaddingTop() + getPaddingBottom() + ValueUtil.dp2px(getContext(), 20);
                break;
            case MeasureSpec.EXACTLY:
                height = heightSize + getPaddingTop() + getPaddingBottom();
                break;
        }

        width = widthSize + getPaddingLeft() + getPaddingRight();

        setMeasuredDimension(width, height);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBg(canvas);
        drawScaleAndNum(canvas);
        drawResultText(canvas, resultText);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        currentX = event.getX();
        isUp = false;
        velocityTracker.computeCurrentVelocity(500);
        velocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //按下时如果属性动画还没执行完,就终止,记录下当前按下点的位置
                if (valueAnimator != null && valueAnimator.isRunning()) {
                    valueAnimator.end();
                    valueAnimator.cancel();
                }
                downX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                //滑动时候,通过假设的滑动距离,做超出左边界以及右边界的限制。
                moveX = currentX - downX + lastMoveX;
                if (moveX >= width / 2) {
                    moveX = width / 2;
                } else if (moveX <= getWhichScalMovex(maxScale)) {
                    moveX = getWhichScalMovex(maxScale);
                    JL_Log.d(TAG, "moveX = " + moveX + ", max = " + getWhichScalMovex(maxScale));
                }
                break;
            case MotionEvent.ACTION_UP:
                //手指抬起时候制造惯性滑动
                lastMoveX = moveX;
                xVelocity = (int) velocityTracker.getXVelocity();
                autoVelocityScroll(xVelocity);
                velocityTracker.clear();
                break;
        }
        invalidate();
        return true;
    }

    private void autoVelocityScroll(int xVelocity) {
        //惯性滑动的代码,速率和滑动距离,以及滑动时间需要控制的很好,应该网上已经有关于这方面的算法了吧。。这里是经过N次测试调节出来的惯性滑动
        if (Math.abs(xVelocity) < 50) {
            isUp = true;
            return;
        }
        if (valueAnimator.isRunning()) {
            return;
        }
        valueAnimator = ValueAnimator.ofInt(0, xVelocity / 20).setDuration(Math.abs(xVelocity / 10));
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveX += (int) animation.getAnimatedValue();
                if (moveX >= width / 2) {
                    moveX = width / 2;
                } else if (moveX <= getWhichScalMovex(maxScale)) {
                    moveX = getWhichScalMovex(maxScale);
                }
                lastMoveX = moveX;
                invalidate();
            }

        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isUp = true;
                invalidate();
            }
        });

        valueAnimator.start();
    }

    private float getWhichScalMovex(float scale) {
        return width / 2 - scaleGap * scaleCount * (scale - minScale);
    }

    private boolean isCallbackScale;

    private void drawScaleAndNum(Canvas canvas) {
        canvas.translate(0, (showScaleResult ? resultNumRect.height() : 0) + rulerToResultgap);//移动画布到结果值的下面

        int num1;//确定刻度位置
        float num2;

        if (firstScale != -1) {   //第一次进来的时候计算出默认刻度对应的假设滑动的距离moveX
            moveX = getWhichScalMovex(firstScale);          //如果设置了默认滑动位置，计算出需要滑动的距离
            lastMoveX = moveX;
            firstScale = -1;                                //将结果置为-1，下次不再计算初始位置
        }

        if (computeScale != -1) {//弹性滑动到目标刻度
            Log.d(TAG, "moveX 000 >> " + moveX + ", computeScale >> " + computeScale);
            lastMoveX = moveX;
//            Log.w(TAG, "moveX 111 currentScale >> " + getWhichScalMovex(currentScale)/ scaleLimit + ", computeScale >> " + getWhichScalMovex(computeScale)/ scaleLimit);
            if (valueAnimator != null && !valueAnimator.isRunning()) {
                valueAnimator = ValueAnimator.ofFloat(lastMoveX, getWhichScalMovex(computeScale));
                valueAnimator.addUpdateListener(animation -> {
                    moveX = (float) animation.getAnimatedValue();
//                    Log.d(TAG, "moveX 222 >> " + moveX);
                    lastMoveX = moveX;
                    invalidate();
                });
                valueAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        //这里是滑动结束时候回调给使用者的结果值
                        if (currentScale == computeScale) {
                            if (onChooseResulterListener != null) {
                                onChooseResulterListener.onEndResult(resultText);
                            }
                        } else {
                            moveX = getWhichScalMovex(computeScale);
                            lastMoveX = moveX;
                            isCallbackScale = true;
                        }
                        computeScale = -1;
                    }
                });
                valueAnimator.setDuration(Math.abs((long) ((getWhichScalMovex(computeScale) - lastMoveX))));
                valueAnimator.start();
            }
        }

        num1 = -(int) (moveX / scaleGap);                   //滑动刻度的整数部分
        num2 = (moveX % scaleGap);                         //滑动刻度的小数部分

        canvas.save();                                      //保存当前画布

        rulerRight = 0;                                    //准备开始绘制当前屏幕,从最左面开始

        if (isUp) {   //这部分代码主要是计算手指抬起时，惯性滑动结束时，刻度需要停留的位置
            num2 = ((moveX - width / 2 % scaleGap) % scaleGap);     //计算滑动位置距离整点刻度的小数部分距离
            if (num2 <= 0) {
                num2 = scaleGap - Math.abs(num2);
            }
            leftScroll = (int) Math.abs(num2);                        //当前滑动位置距离左边整点刻度的距离
            rightScroll = (int) (scaleGap - Math.abs(num2));         //当前滑动位置距离右边整点刻度的距离

            float moveX2 = num2 <= scaleGap / 2 ? moveX - leftScroll : moveX + rightScroll; //最终计算出当前位置到整点刻度位置需要滑动的距离

            if (valueAnimator != null && !valueAnimator.isRunning()) {      //手指抬起，并且当前没有惯性滑动在进行，创建一个惯性滑动
                valueAnimator = ValueAnimator.ofFloat(moveX, moveX2);
                valueAnimator.addUpdateListener(animation -> {
                    moveX = (float) animation.getAnimatedValue();            //不断滑动去更新新的位置
                    lastMoveX = moveX;
                    invalidate();
                });
                valueAnimator.addListener(new AnimatorListenerAdapter() {       //增加一个监听，用来返回给使用者滑动结束后的最终结果刻度值
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        //这里是滑动结束时候回调给使用者的结果值
                        if (onChooseResulterListener != null) {
                            onChooseResulterListener.onEndResult(resultText);
                        }
                    }
                });
                valueAnimator.setDuration(300);
                valueAnimator.start();
                isUp = false;
            }

            num1 = (int) -(moveX / scaleGap);                //重新计算当前滑动位置的整数以及小数位置
            num2 = (moveX % scaleGap);
        }


        canvas.translate(num2, 0);    //不加该偏移的话，滑动时刻度不会落在0~1之间只会落在整数上面,其实这个都能设置一种模式了，毕竟初衷就是指针不会落在小数上面

        //这里是滑动时候不断回调给使用者的结果值
        currentScale = new WeakReference<>(new BigDecimal(((width / 2 - moveX) / (scaleGap * scaleCount) + minScale) * scaleLimit)).get().setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();
        resultText = String.valueOf(currentScale);


        if (onChooseResulterListener != null) {
            onChooseResulterListener.onScrollResult(resultText); //接口不断回调给使用者结果值
        }
        if (isCallbackScale) {
            isCallbackScale = false;
            if (onChooseResulterListener != null) {
                onChooseResulterListener.onEndResult(resultText);
            }
        }
        float startX = 0;
        float stopX = width;
        //重新计算真实的最大格数
        int maxRealScale = maxScale > Math.round(maxScale) ? Math.round(maxScale) + 1 : Math.round(maxScale);
        //绘制当前屏幕可见刻度,不需要裁剪屏幕,while循环只会执行·屏幕宽度/刻度宽度·次,大部分的绘制都是if(curDis<width)这样子内存暂用相对来说会比较高。。
        while (rulerRight < width) {
            if (num1 % scaleCount == 0) {    //绘制整点刻度以及文字
                if ((moveX >= 0 && rulerRight < moveX - scaleGap) || width / 2 - rulerRight <= getWhichScalMovex(maxRealScale + 1) - moveX) {
                    //当滑动出范围的话，不绘制，去除左右边界
                } else {
                    float val = (num1 / (float) scaleCount + minScale) * scaleLimit;
//                    JL_Log.e(TAG, " val : " + val + ", max = " + maxScale * scaleLimit);
                    if (val <= maxScale * scaleLimit) {
                        //绘制刻度，绘制刻度数字
                        canvas.drawLine(0, drawStartY, 0, drawStartY + midScaleHeight, midScalePaint);
                        scaleNumPaint.getTextBounds(num1 / scaleGap + minScale + "", 0, (num1 / scaleGap + minScale + "").length(), scaleNumRect);
                        canvas.drawText((int)val + "", -scaleNumRect.width() / 2, lagScaleHeight +
                                (rulerHeight - lagScaleHeight) / 2 + scaleNumRect.height() + ValueUtil.dp2px(getContext(), 10), scaleNumPaint);
                    }

                    if (val == minScale * scaleLimit) {
                        startX = (width >> 1) - ((currentScale - val) * scaleGap / scaleLimit * scaleCount);
                    } else if (val == maxRealScale * scaleLimit) {
                        stopX = (val - currentScale) * scaleGap / scaleLimit * scaleCount + width / 2;
                    }
                }
            } else {   //绘制小数刻度
                if ((moveX >= 0 && rulerRight < moveX) || width / 2 - rulerRight < getWhichScalMovex(maxRealScale) - moveX) {
                    //当滑动出范围的话，不绘制，去除左右边界
                } else {
                    float val = (num1 / (float) scaleCount + minScale) * scaleLimit;
//                    JL_Log.w(TAG, " val : " + val + ", max = " + maxScale);
                    if(val <= maxScale * scaleLimit) {
                        //绘制小数刻度
                        canvas.drawLine(0, drawStartY, 0, drawStartY + smallScaleHeight, smallScalePaint);
                    }
                }
            }
            ++num1;  //刻度加1
            rulerRight += scaleGap;  //绘制屏幕的距离在原有基础上+1个刻度间距
            canvas.translate(scaleGap, 0); //移动画布到下一个刻度
        }

        canvas.restore();

        /*if (currentScale == minScale * scaleLimit) {
            startX = width / 2;
        } else if (currentScale == maxScale * scaleLimit) {
            stopX = width / 2;
        }*/
        //绘制屏幕中间用来选中刻度的最大刻度
        canvas.drawLine(width / 2, 0, width / 2, rulerHeight, lagScalePaint);
        canvas.drawLine(startX, drawStartY, stopX, drawStartY, smallScalePaint);
    }

    //绘制上面的结果 结果值+单位
    private void drawResultText(Canvas canvas, String resultText) {
        if (!showScaleResult) {   //判断用户是否设置需要显示当前刻度值，如果否则取消绘制
            return;
        }
        canvas.translate(0, -resultNumRect.height() - rulerToResultgap / 2);  //移动画布到正确的位置来绘制结果值
        resultNumPaint.getTextBounds(resultText, 0, resultText.length(), resultNumRect);
        canvas.drawText(resultText, width / 2 - resultNumRect.width() / 2, resultNumRect.height(), //绘制当前刻度结果值
                resultNumPaint);
        resultNumRight = width / 2 + resultNumRect.width() / 2 + 10;
        canvas.drawText(unit, resultNumRight, kgRect.height() + 2, kgPaint);            //在当前刻度结果值的又面10px的位置绘制单位
    }

    private void drawBg(Canvas canvas) {
        bgRect.set(0, 0, width, height);
        if (isBgRoundRect) {
            canvas.drawRoundRect(bgRect, 20, 20, bgPaint); //20->椭圆的用于圆形角x-radius
        } else {
            canvas.drawRect(bgRect, bgPaint);
        }
    }

    public void computeScrollTo(float scale) {
        scale = scale / scaleLimit;
        if (scale < minScale || scale > maxScale) {
            return;
        }

        computeScale = scale;
        invalidate();

    }

    public interface OnChooseResulterListener {
        void onEndResult(String result);

        void onScrollResult(String result);
    }

    public int getMinScale() {
        return minScale * scaleLimit;
    }

    public float getMaxScale() {
        return maxScale * scaleLimit;
    }

    public float getMaxScaleValue() {
        return maxScaleValue;
    }

    public void setMaxScaleValue(float maxScaleValue) {
        this.maxScaleValue = maxScaleValue;
    }

    public void setRulerHeight(int rulerHeight) {
        this.rulerHeight = rulerHeight;
        invalidate();
    }

    public void setRulerToResultgap(int rulerToResultgap) {
        this.rulerToResultgap = rulerToResultgap;
        invalidate();
    }

    public void setScaleCount(int scaleCount) {
        this.scaleCount = scaleCount;
        invalidate();
    }

    public void setScaleGap(int scaleGap) {
        this.scaleGap = scaleGap;
        invalidate();
    }

    public void setMinScale(int minScale) {
        this.minScale = minScale / scaleLimit;
        invalidate();
    }

    public void setFirstScale(float firstScale) {
        this.firstScale = firstScale / scaleLimit;
        invalidate();
    }

    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale / (float) scaleLimit;
        this.maxScaleValue  = maxScale;
        invalidate();
    }

    public void setScale(int minScale, float maxScale, float firstScale){
        this.minScale = minScale / scaleLimit;
        this.maxScale = maxScale / (float) scaleLimit;
        this.firstScale = firstScale / scaleLimit;
        this.maxScaleValue  = maxScale;
        invalidate();
    }

    public void setBgColor(int bgColor) {
        this.bgColor = bgColor;
        invalidate();
    }

    public void setSmallScaleColor(int smallScaleColor) {
        this.smallScaleColor = smallScaleColor;
        invalidate();
    }

    public void setMidScaleColor(int midScaleColor) {
        this.midScaleColor = midScaleColor;
        invalidate();
    }

    public void setLargeScaleColor(int largeScaleColor) {
        this.largeScaleColor = largeScaleColor;
    }

    public void setScaleNumColor(int scaleNumColor) {
        this.scaleNumColor = scaleNumColor;
        invalidate();
    }

    public void setResultNumColor(int resultNumColor) {
        this.resultNumColor = resultNumColor;
        invalidate();
    }

    public void setUnit(String unit) {
        this.unit = unit;
        invalidate();
    }

    public void setUnitColor(int unitColor) {
        this.unitColor = unitColor;
        invalidate();
    }

    public void setSmallScaleStroke(int smallScaleStroke) {
        this.smallScaleStroke = smallScaleStroke;
        invalidate();
    }

    public void setMidScaleStroke(int midScaleStroke) {
        this.midScaleStroke = midScaleStroke;
        invalidate();
    }

    public void setLargeScaleStroke(int largeScaleStroke) {
        this.largeScaleStroke = largeScaleStroke;
        invalidate();
    }

    public void setResultNumTextSize(int resultNumTextSize) {
        this.resultNumTextSize = resultNumTextSize;
        invalidate();
    }

    public void setScaleNumTextSize(int scaleNumTextSize) {
        this.scaleNumTextSize = scaleNumTextSize;
        invalidate();
    }

    public void setUnitTextSize(int unitTextSize) {
        this.unitTextSize = unitTextSize;
        invalidate();
    }

    public void setShowScaleResult(boolean showScaleResult) {
        this.showScaleResult = showScaleResult;
        invalidate();
    }

    public void setIsBgRoundRect(boolean bgRoundRect) {
        isBgRoundRect = bgRoundRect;
        invalidate();
    }

    public void setScaleLimit(int scaleLimit) {
        this.scaleLimit = scaleLimit;
        invalidate();
    }
}
