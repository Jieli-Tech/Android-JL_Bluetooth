package com.jieli.btsmart.ui.settings.device.assistivelistening.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: FittingChart
 * @Description: 辅听图表
 * @Author: ZhangHuanMing
 * @CreateDate: 2022/6/29 11:42
 * 需求：
 * 1.小于6条自适应，大于6条在屏幕外面
 * 2.两种数据类型，柱状图，一种折线图
 */
public class FittingChart extends View {
    private String TAG = "ZHM";
    private float mHorizontalPadding = 6;//水平内间距
    private int mScreenDataNum = 6;//屏幕内显示数据的
    private float mItemIntervalWidth = 20;//间隔的大小
    private float mItemPartWidth = 20;//每一部分的宽度
    private float mItemPartHeight = 200;//每一部分的高度
    private float mMarginTop = 10;//顶部间隔 --等分线的粗细
    private float mBottomScaleHeight = 20;//下方刻度尺子高度
    private float mScaleTextSize = 20;//刻度尺的字大小
    private float mWidth = 200;//可视界面大小
    private float mHeight = 200;//可视界面大小
    private float mRealWidth = 200;//真实界面大小
    private boolean mIsHighLightLast = false;//是否高亮最后一个
    private int mDataLen = 6;
    private List<BarChartData> mBarData = new ArrayList<>();
    private List<List<LineChartData>> mLineData = new ArrayList<>();
    private ValueFormatter mValueFormatter = new ValueFormatter();
    private int mRange = 100;
    private GestureDetector mGestureDetectorCompat;

    private Paint mItemBackgroundColorsPaint;//item的背景图
    private Paint mScaleTextPaint;//刻度值的
    private Paint mBarPaint;//柱状图

    // FIXME: 2022/7/5 顶部要留画圆点的padding,当前的实现方式无解
    public FittingChart(Context context) {
        super(context);
        init(null, 0);
    }

    public FittingChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public FittingChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        mHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        calculationItemWidthAndHeight(mDataLen);
        setMeasuredDimension((int) mRealWidth, (int) mHeight);
    }

    public void setDataLen(int dataLen) {
        this.mDataLen = dataLen;
        invalidate();//会触发onMeasure吗？
    }

    public int getDataLen() {
        return mDataLen;
    }

    public void setValueFormatter(ValueFormatter valueFormatter) {
        mValueFormatter = valueFormatter;
    }

    /**
     * 将不在可视范围内的item移到可视范围内
     */
    public void scrollToPosition(int position) {
        if (position < 0 || position >= mDataLen) return;
        float positionViewOffsetX = (mItemIntervalWidth + mItemPartWidth) * (position) + mHorizontalPadding;
        if (positionViewOffsetX < -offsetX) {//在屏幕的左边
            offsetX = -(positionViewOffsetX - mHorizontalPadding);
        } else if (positionViewOffsetX > mWidth - offsetX) {// 在屏幕的右边
            //偏移进屏幕
            offsetX = -(positionViewOffsetX + mItemPartWidth - mWidth + mHorizontalPadding);
        } else {//在屏幕内
            return;
        }
        postInvalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            setViewPager2InputEnable(false);//禁止viewPager2的滑动处理
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            setViewPager2InputEnable(true);
        }
        mGestureDetectorCompat.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    /**
     * 计算item的宽高
     */
    private void calculationItemWidthAndHeight(int dataLen) {
        int intervalWithItemMultiple = 1;//间隔与item的宽度倍数关系 1:1
        if (dataLen <= mScreenDataNum) {//小于6条在屏幕内等分
            mItemPartWidth = (mWidth - mHorizontalPadding * 2) / (dataLen + intervalWithItemMultiple * (dataLen - 1));
            mItemIntervalWidth = mItemPartWidth * intervalWithItemMultiple;
            mRealWidth = (mWidth);
        } else {//
            mItemPartWidth = (mWidth - mHorizontalPadding * 2) / (mScreenDataNum + intervalWithItemMultiple * (mScreenDataNum - 1));
            mItemIntervalWidth = mItemPartWidth * intervalWithItemMultiple;
            mRealWidth = mItemIntervalWidth * (dataLen - 1) + mItemPartWidth * dataLen + mHorizontalPadding * 2;
        }
        mItemPartHeight = mHeight - mBottomScaleHeight - 2 * mMarginTop;//减去 一个刻度尺的高度 和 两个等分线的高度
    }

    /**
     * 设置是否高亮最后一个值(BarData)
     */
    public void setHighLightLast(boolean isHighLightLast) {
        this.mIsHighLightLast = isHighLightLast;
        invalidate();
    }

    /**
     * 设置柱状图数据
     */
    public void setBarData(List<BarChartData> barData) {
        mBarData = barData;
        invalidate();
    }

    /**
     * 设置折线图数据
     */
    public void setLineData(List<List<LineChartData>> data) {
        mLineData = data;
        invalidate();
    }

    private void init(AttributeSet attrs, int defStyle) {
        mBottomScaleHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f,
                getContext().getResources().getDisplayMetrics());
        mScaleTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f,
                getContext().getResources().getDisplayMetrics());
        mMarginTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f,
                getContext().getResources().getDisplayMetrics());
        mHorizontalPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3.5f,
                getContext().getResources().getDisplayMetrics());
        mItemBackgroundColorsPaint = new Paint();
        mItemBackgroundColorsPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mItemBackgroundColorsPaint.setColor(Color.parseColor("#E9EBF0"));
        mItemBackgroundColorsPaint.setStrokeWidth(3);

        mScaleTextPaint = new Paint();
        mScaleTextPaint.setColor(Color.parseColor("#ABABAB"));
        mScaleTextPaint.setTextSize(mScaleTextSize);

        mBarPaint = new Paint();
        mBarPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mBarPaint.setColor(Color.parseColor("#805BEB"));
        mBarPaint.setStrokeWidth(3);
        mGestureDetectorCompat = new GestureDetector(getContext(), mGestureListener);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(offsetX, 0);
        //上一个折线点的坐标
        int mLastLineDatOffsetX = 0;
        int mLastLineDatOffsetY = 0;
        int mCornerRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f,
                getContext().getResources().getDisplayMetrics());

        //缓存需要绘画的折线点
        List<List<Point>> lineDatList = new ArrayList<>();
        for (int j = 0; j < mLineData.size(); j++) {
            lineDatList.add(new ArrayList<>());
        }

        for (int i = 0; i < mDataLen; i++) {
            //先绘画背景图
            float itemOriginX = (i * (mItemIntervalWidth + mItemPartWidth)) + mHorizontalPadding;
            float itemOriginY = mMarginTop;
            RectF itemRectF = new RectF(itemOriginX, itemOriginY, itemOriginX + mItemPartWidth, itemOriginY + mItemPartHeight);
            canvas.drawRoundRect(itemRectF, mCornerRadius, mCornerRadius, mItemBackgroundColorsPaint);
            //绘画柱状图
            boolean isSignLastData = false;
            float currentDataValue = i;
            if (mBarData.size() > i) {
                BarChartData data = mBarData.get(i);
                isSignLastData = mIsHighLightLast && mBarData.size() == (i + 1);
                float partHeight = (int) (mItemPartHeight * (data.y / mRange));
                if (partHeight > mItemPartHeight) {
                    partHeight = mItemPartHeight;
                } else if (partHeight < 0) {
                    partHeight = 0;
                }
                RectF barRectF = new RectF(itemOriginX, itemOriginY + mItemPartHeight - partHeight, itemOriginX + mItemPartWidth, itemOriginY + mItemPartHeight);
                mBarPaint.setShader(new LinearGradient(0, 0, 0, barRectF.bottom, Color.parseColor("#805BEB"), Color.parseColor("#A896DD"), Shader.TileMode.CLAMP));
                canvas.drawRoundRect(barRectF, mCornerRadius, mCornerRadius, mBarPaint);
                if (isSignLastData) {//标识
                    int offsetX = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f,
                            getContext().getResources().getDisplayMetrics());//偏移
                    Paint highLightBgPaint = new Paint();
                    highLightBgPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    highLightBgPaint.setColor(Color.parseColor("#F8B64B"));
                    highLightBgPaint.setStrokeWidth(3);
                    int radius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f,
                            getContext().getResources().getDisplayMetrics());
                    int highLightHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f,
                            getContext().getResources().getDisplayMetrics());//偏移
                    RectF highLightRectF = new RectF(itemOriginX - offsetX
                            , itemOriginY + mItemPartHeight - partHeight
                            , itemOriginX + mItemPartWidth + offsetX
                            , itemOriginY + mItemPartHeight - partHeight + highLightHeight);
                    canvas.drawRoundRect(highLightRectF, radius, radius, highLightBgPaint);
                }
            }
            //绘画折线图
            if (mLineData.size() > 0) {
                for (int j = 0; j < mLineData.size(); j++) {
                    List<LineChartData> lineChartDataList = mLineData.get(j);
                    if (lineChartDataList.size() > i) {
                        LineChartData chartData = lineChartDataList.get(i);
                        Point point = new Point();
                        point.x = (int) (itemOriginX + mItemPartWidth / 2);//圆点中心
                        float partHeight = (mItemPartHeight * (chartData.y / mRange));
                        if (partHeight > mItemPartHeight) {
                            partHeight = mItemPartHeight;
                        } else if (partHeight < 0) {
                            partHeight = 0;
                        }
                        point.y = (int) (itemOriginY + mItemPartHeight - partHeight);
                        lineDatList.get(j).add(point);
                    }
                }
            }
            //绘画刻度值
            String scaleStr = mValueFormatter.getFormattedValue(currentDataValue);
            int highLightBgHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f,
                    getContext().getResources().getDisplayMetrics());//偏移
            float scaleOffsetX = itemOriginX + (mItemPartWidth / 2) - (mScaleTextPaint.measureText(scaleStr) / 2);
            if (isSignLastData) {// 需要标记
                //高亮背景
                int radius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f,
                        getContext().getResources().getDisplayMetrics());
                int offsetX = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f,
                        getContext().getResources().getDisplayMetrics());//偏移
                Paint highLightBgPaint = new Paint();
                highLightBgPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                highLightBgPaint.setColor(Color.parseColor("#805BEB"));
                highLightBgPaint.setStrokeWidth(3);
                RectF highLightRectF = new RectF();
                float highLightOriginX = itemOriginX - offsetX;
                highLightRectF.left = highLightOriginX;
                highLightRectF.top = mHeight - highLightBgHeight;
                highLightRectF.right = highLightOriginX + mItemPartWidth + 2 * offsetX;
                highLightRectF.bottom = mHeight;
                canvas.drawRoundRect(highLightRectF, radius, radius, highLightBgPaint);
                //高亮text
                Paint highLightTextPaint = new Paint();
                highLightTextPaint.setColor(Color.parseColor("#FFFFFF"));
                highLightTextPaint.setTextSize(mScaleTextSize);
                canvas.drawText(scaleStr, scaleOffsetX, mHeight - (highLightBgHeight - mScaleTextSize), highLightTextPaint);
            } else {
                canvas.drawText(scaleStr, scaleOffsetX, mHeight - (highLightBgHeight - mScaleTextSize), mScaleTextPaint);
            }
        }
        int[] lineDatColorArray = new int[]{Color.parseColor("#4E89F4"), Color.parseColor("#E7933B")};//先左耳，后右耳
        int[] lineColorArray = new int[]{Color.parseColor("#8DB3FA"), Color.parseColor("#F7C189")};
        //绘画折线图
        int listIndex = 0;
        for (List<Point> pointList : lineDatList) {//画折线
            float lastDatx = 0;
            float lastDaty = 0;
            for (int i = 0; i < pointList.size(); i++) {
                Point point = pointList.get(i);
                if (lastDatx == 0 && lastDaty == 0) {
                    lastDatx = point.x;
                    lastDaty = point.y;
                } else {
                    Paint paint = new Paint();
                    paint.setStyle(Paint.Style.FILL_AND_STROKE);
                    paint.setColor(lineColorArray[listIndex]);
                    paint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f,
                            getContext().getResources().getDisplayMetrics()));
                    canvas.drawLine(lastDatx, lastDaty, point.x, point.y, paint);
                    lastDatx = point.x;
                    lastDaty = point.y;
                }
            }
            listIndex++;
        }
        listIndex = 0;
        for (List<Point> pointList : lineDatList) {//画圆点
            for (int i = 0; i < pointList.size(); i++) {
                Point point = pointList.get(i);
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                paint.setColor(lineDatColorArray[listIndex]);
                canvas.drawCircle(point.x, point.y, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f,
                        getContext().getResources().getDisplayMetrics()), paint);
            }
            listIndex++;
        }
        canvas.restore();
    }

    private void setViewPager2InputEnable(boolean enable) {
        ViewPager2 viewPager2 = getViewPager2();
        if (viewPager2 != null) {
            viewPager2.setUserInputEnabled(enable);
        }
    }

    private ViewPager2 getViewPager2() {
        ViewParent parent = this.getParent();
        while ((parent = parent.getParent()) != null) {
            if (parent instanceof ViewPager2) {
                return (ViewPager2) parent;
            }
        }
        return null;
    }

    private float offsetX = 0;
    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent motionEvent) {
            Log.d(TAG, "onDown: ");
            return false;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {
            Log.d(TAG, "onShowPress: ");
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            Log.d(TAG, "onSingleTapUp: ");
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            Log.d("ZHM", "onScroll: v: " + v + " v1: " + v1);
            offsetX += -v;
            if (offsetX >= 0) {
                offsetX = 0;
                setViewPager2InputEnable(true);//碰到左边界
            }
            if (offsetX <= mWidth - mRealWidth) {
                offsetX = mWidth - mRealWidth;
                setViewPager2InputEnable(true);//碰到右边界
            }
            invalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {
            Log.d(TAG, "onLongPress: ");
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            Log.d(TAG, "onFling: ");
            return true;
        }
    };

    public static class ValueFormatter {
        public String getFormattedValue(float value) {
            return String.valueOf(500);
        }
    }

    public static class LineChartData {
        int lineColor;
        int datColor;
        float x;
        float y;

        public LineChartData(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public LineChartData(int lineColor, int datColor, float x, float y) {
            this.lineColor = lineColor;
            this.datColor = datColor;
            this.x = x;
            this.y = y;
        }
    }

    public static class BarChartData {
        float x;
        float y;

        public BarChartData(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
