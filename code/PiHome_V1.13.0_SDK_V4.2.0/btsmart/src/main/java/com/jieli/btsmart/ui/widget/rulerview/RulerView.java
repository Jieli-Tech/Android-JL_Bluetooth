package com.jieli.btsmart.ui.widget.rulerview;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/6 14:58
 * @desc :
 */

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.media.AudioAttributes;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.OverScroller;
import android.widget.RelativeLayout;

import androidx.core.view.MotionEventCompat;
import androidx.core.view.VelocityTrackerCompat;
import androidx.core.view.ViewCompat;

import com.jieli.btsmart.R;

import java.util.ArrayList;

import static android.content.Context.VIBRATOR_SERVICE;

/**
 * Created by zhufeng on 2017/7/26.
 */

public class RulerView extends RelativeLayout {
    private static String TAG = RulerView.class.getSimpleName();
    private static final int INVALID_POINTER = -1;
    public static final int SCROLL_STATE_IDLE = 0;//滚动状态空闲
    public static final int SCROLL_STATE_DRAGGING = 1;//滚动状态拖动
    public static final int SCROLL_STATE_SETTLING = 2;//滚动状态设置

    private int mScrollState = SCROLL_STATE_IDLE;
    private int mScrollPointerId = INVALID_POINTER;
    private VelocityTracker mVelocityTracker;//用于计算加速度
    private int mLastTouchX;
    private int mTouchSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private ViewFlinger mViewFlinger;
    private FMRulerView mFMRulerView;
    private int mImageScaleResourceId = -1;
    private float mScaleDistanceInterval = 0f;
    //    private int mCurrentPosition = -1;
    private OnValueChangeListener onValueChangeListener;
    private Activity mActivity;
    private Vibrator vibrator;
    private int lastTransScaleNum = 0;
    private boolean canFlingTrueCallback = false;
    //f(x) = (x-1)^5 + 1
    private static final Interpolator sQuinticInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };
    private boolean isCanDrag = true;

    public RulerView(Context context) {
        this(context, null);
    }

    public RulerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RulerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = attrs == null ? null : getContext()
                .obtainStyledAttributes(attrs, R.styleable.fmRulerView);
        if (typedArray != null) {
            mImageScaleResourceId = typedArray.getResourceId(R.styleable.fmRulerView_selectedSrcImages, -1);
//            mScaleDistanceInterval = typedArray.getDimension(R.styleable.fmRulerView_intervalDistance, mScaleDistanceInterval);
        }
        typedArray.recycle();
        init(context, attrs);
    }


    private void init(Context context, AttributeSet attrs) {
        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        DisplayMetrics metric = context.getResources().getDisplayMetrics();

        mFMRulerView = new FMRulerView(context, attrs);
        addView(mFMRulerView);
        mViewFlinger = new ViewFlinger();
        mScaleDistanceInterval = mFMRulerView.getScaleDistanceInterval();
        ImageView imageView = new ImageView(context);
        if (mImageScaleResourceId < 0) {
            imageView.setImageResource(R.drawable.ic_fm_ruler_hight_light);
        } else {
            imageView.setImageResource(mImageScaleResourceId);
        }
        LayoutParams imageViewLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        imageViewLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, TRUE);
        addView(imageView, imageViewLayoutParams);
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
/*        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), mImageScaleResourceId);
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0, 0, paint);
        }*/
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {//告诉父组件不要拦截该事件
        getParent().requestDisallowInterceptTouchEvent(true);
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 设置是否可以拖动
     *
     * @param
     * @return
     * @description 描述一下方法的作用
     */
    public void setCanDragState(boolean state) {
        if (isCanDrag == state) return;
        isCanDrag = state;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isCanDrag) {
            if (onValueChangeListener != null) {
                onValueChangeListener.onCanNotSlide();
            }
            return true;
        }
        ;
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        boolean eventAddedToVelocityTracker = false;
        final int action = MotionEventCompat.getActionMasked(event);
        final int actionIndex = MotionEventCompat.getActionIndex(event);
        final MotionEvent vtev = MotionEvent.obtain(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                setScrollState(SCROLL_STATE_IDLE);
                mScrollPointerId = event.getPointerId(0);
                mLastTouchX = (int) (event.getX() + 0.5f);
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                mScrollPointerId = event.getPointerId(actionIndex);
                mLastTouchX = (int) (event.getX(actionIndex) + 0.5f);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int index = event.findPointerIndex(mScrollPointerId);
                if (index < 0) {
                    return false;
                }

                final int x = (int) (event.getX(index) + 0.5f);
                int dx = mLastTouchX - x;

                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    boolean startScroll = false;

                    if (Math.abs(dx) > mTouchSlop) {
                        if (dx > 0) {
                            dx -= mTouchSlop;
                        } else {
                            dx += mTouchSlop;
                        }
                        startScroll = true;
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }

                if (mScrollState == SCROLL_STATE_DRAGGING) {
                    mLastTouchX = x;
                    constrainScrollBy(dx, 0);
                }

                float afterCalculationDeviationX = mFMRulerView.getScrollX() + dx;
                float different = Math.abs(afterCalculationDeviationX - lastTransScaleNum * mScaleDistanceInterval);//前后两次大于一个刻度值
                if (different >= mScaleDistanceInterval) {
                    int value = Math.round(afterCalculationDeviationX / mScaleDistanceInterval);
                    handlerCallback(value, TOUCH_TYPE_MOVE, false);
                }
              /*  float afterCalculationDeviationX = mFMRulerView.getScrollX() + dx;
                float different = Math.abs(afterCalculationDeviationX - lastTransScaleNum * mScaleDistanceInterval);//前后两次大于一个刻度值
                if (vibrator != null && different >= mScaleDistanceInterval) {
                    lastTransScaleNum = Math.round(afterCalculationDeviationX / mScaleDistanceInterval);
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build();
                    vibrator.vibrate(2, audioAttributes);
                }
                if (onValueChangeListener != null) {
                    onValueChangeListener.onChange((lastTransScaleNum) + mFMRulerView.getMinValue());
                }*/
                break;
            }
            case MotionEventCompat.ACTION_POINTER_UP: {
                if (event.getPointerId(actionIndex) == mScrollPointerId) {
                    // Pick a new pointer to pick up the slack.
                    final int newIndex = actionIndex == 0 ? 1 : 0;
                    mScrollPointerId = event.getPointerId(newIndex);
                    mLastTouchX = (int) (event.getX(newIndex) + 0.5f);
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                mVelocityTracker.addMovement(vtev);
                eventAddedToVelocityTracker = true;
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                float xVelocity = -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId);
                if (Math.abs(xVelocity) < mMinFlingVelocity) {
                    xVelocity = 0F;
                } else {
                    xVelocity = Math.max(-mMaxFlingVelocity, Math.min(xVelocity, mMaxFlingVelocity));
                }
                if (xVelocity != 0) {
                    mViewFlinger.fling((int) xVelocity);
                } else {
                    setScrollState(SCROLL_STATE_IDLE);
                    int value = Math.round(mFMRulerView.getScrollX() / mScaleDistanceInterval);//余数
                    int tranX = (int) (value * mScaleDistanceInterval);//多出的距离
                    handlerCallback(value, TOUCH_TYPE_UP, false);
                    int dx = tranX - mFMRulerView.getScrollX();
                    constrainScrollBy(dx, 0);
                   /* int remainder = Math.round(mFMRulerView.getScrollX() / mScaleDistanceInterval);//余数
                    int tranX = (int) (remainder * mScaleDistanceInterval);//多出的距离
                    if (vibrator != null && lastTransScaleNum != remainder) {
                        lastTransScaleNum = remainder;
                        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .build();
                        vibrator.vibrate(2, audioAttributes);
                    }
                    if (onValueChangeListener != null) {
                        onValueChangeListener.onActionUp((lastTransScaleNum) + mFMRulerView.getMinValue());
                    }
                    int dx = tranX - mFMRulerView.getScrollX();
                    constrainScrollBy(dx, 0);*/
                }
                resetTouch();
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                resetTouch();
                break;
            }
        }
        if (!eventAddedToVelocityTracker) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();
        return true;
    }

    public void setActivity(Activity activity) {
        if (activity == null) return;
        this.mActivity = activity;
        vibrator = (Vibrator) mActivity.getSystemService(VIBRATOR_SERVICE);
        if (!vibrator.hasVibrator()) {// 判断手机硬件是否有振动器
            vibrator = null;
        }
    }

    /**
     * 设置当前位置：onCreateView的时候无效（暂时无法解决）
     */
    public void setCurrentPosition(int position) {
//        mCurrentPosition = position;
        if (mFMRulerView != null) {
            int tranSection = position - mFMRulerView.getMinValue();
            float tranX = mScaleDistanceInterval * tranSection;
            mFMRulerView.setScrollX((int) tranX);
            if (onValueChangeListener != null) {
                onValueChangeListener.onChange(position);
            }
        }
    }

    public void setOnValueChangeListener(OnValueChangeListener onValueChangeListener) {
        this.onValueChangeListener = onValueChangeListener;
    }

    /**
     * 设置搜到的频道
     *
     * @param
     * @return
     * @description 描述一下方法的作用
     */
    public void setChannelList(ArrayList<Integer> channelList) {
        mFMRulerView.setExistRadio(channelList);
    }

    public void setMinValueAndMaxValue(int minValue, int maxValue) {
        mFMRulerView.setMinValueAndMaxValue(minValue, maxValue);
    }

    /**
     * 重置Touch相关数据（清除mVelocityTracker的计算数据）
     *
     * @param
     * @return
     * @description 描述一下方法的作用
     */
    private void resetTouch() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
    }

    /**
     * 设置当前的View的滚动状态(如果)
     *
     * @param
     * @return
     * @description 描述一下方法的作用
     */
    private void setScrollState(int state) {
        if (state == mScrollState) {
            return;
        }
        mScrollState = state;
        if (state != SCROLL_STATE_SETTLING) {//如果不是惯性动作，就需要停下Flinger的惯性移动
            mViewFlinger.stop();
        }
    }

    private class ViewFlinger implements Runnable {
        private int mLastFlingX = 0;
        private OverScroller mScroller;
        private boolean mEatRunOnAnimationRequest = false;
        private boolean mReSchedulePostAnimationCallback = false;

        public ViewFlinger() {
            mScroller = new OverScroller(mFMRulerView.getContext(), sQuinticInterpolator);
        }

        @Override
        public void run() {
            disableRunOnAnimationRequests();
            final OverScroller scroller = mScroller;
            if (scroller.computeScrollOffset()) {
                final int x = scroller.getCurrX();
                int dx = x - mLastFlingX;
                mLastFlingX = x;
                constrainScrollBy(dx, 0);
                if (scroller.isFinished()) {//惯性停下的时候
                    int value = Math.round(mFMRulerView.getScrollX() / mScaleDistanceInterval);//余数
                    int tranX = (int) (value * mScaleDistanceInterval);//多出的距离
//                    Log.e(TAG, "onFlingtime:  value:" + "  isFlingEnd:true");
                    handlerCallback(value, TOUCH_TYPE_FLING, true);
                    int dx1 = tranX - mFMRulerView.getScrollX();
                    constrainScrollBy(dx1, 0);
                } else {
                    float afterCalculationDeviationX = mFMRulerView.getScrollX() + dx;
                    float different = Math.abs(afterCalculationDeviationX - lastTransScaleNum * mScaleDistanceInterval);//前后两次大于一个刻度值
                    if (different >= mScaleDistanceInterval) {
                        int value = Math.round(afterCalculationDeviationX / mScaleDistanceInterval);
                        handlerCallback(value, TOUCH_TYPE_FLING, false);
                    }
                }
                postOnAnimation();
            }
            enableRunOnAnimationRequests();
        }

        public void fling(int velocityX) {
            mLastFlingX = 0;
//            mIsStartScroll = true;
            canFlingTrueCallback = true;
            setScrollState(SCROLL_STATE_SETTLING);
            mScroller.fling(0, 0, velocityX, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            postOnAnimation();
        }

        public void stop() {
            removeCallbacks(this);
            mScroller.abortAnimation();
        }

        private void disableRunOnAnimationRequests() {
            mReSchedulePostAnimationCallback = false;
            mEatRunOnAnimationRequest = true;
        }

        private void enableRunOnAnimationRequests() {
            mEatRunOnAnimationRequest = false;
            if (mReSchedulePostAnimationCallback) {
                postOnAnimation();
            }
        }

        void postOnAnimation() {
            if (mEatRunOnAnimationRequest) {
                mReSchedulePostAnimationCallback = true;
            } else {
                removeCallbacks(this);
                ViewCompat.postOnAnimation(RulerView.this, this);
            }
        }
    }

    private void constrainScrollBy(int dx, int dy) {
//        Log.e(TAG, "mFMRulerView.getWidth():" + mFMRulerView.getWidth() + "width:" + getWidth());
        int scrollX = mFMRulerView.getScrollX();
//        Log.e(TAG, "constrainScrollBy: dx" + dx + "scrollX:" + scrollX);
        if (dx + getWidth() + scrollX > mFMRulerView.getWidth()) {
            dx = mFMRulerView.getWidth() - (getWidth() + scrollX);
            //暂停fling
            mViewFlinger.stop();
        } else if (-scrollX - dx > 0) {
            dx = -scrollX;
            //暂停fling
            mViewFlinger.stop();
        }
        mFMRulerView.scrollBy(dx, 0);
    }

    private final int TOUCH_TYPE_MOVE = 0;
    private final int TOUCH_TYPE_UP = 1;
    private final int TOUCH_TYPE_FLING = 2;

    /**
     * @param value:刻度值
     * @param touchType:事件类型
     * @param isFlingEnd     :惯性滚动是否结束
     * @desc
     */
    private void handlerCallback(int value, int touchType, boolean isFlingEnd) {
//        Log.e(TAG, "handlerCallback: value:" + value + "mFMRulerView.getScaleTotalCount():" + mFMRulerView.getScaleTotalCount());
        if (isFlingEnd && canFlingTrueCallback) {
            canFlingTrueCallback = false;
        } else {
            if (value < 0) {
                value = 0;
            }
            if (value > mFMRulerView.getScaleTotalCount() - 1) {
                value = mFMRulerView.getScaleTotalCount() - 1;
            }
            if (value == lastTransScaleNum && touchType != TOUCH_TYPE_UP) return;
        }
        lastTransScaleNum = value;
        if (vibrator != null) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            vibrator.vibrate(2, audioAttributes);
        }
        value += mFMRulerView.getMinValue();
        if (onValueChangeListener != null && (value >= mFMRulerView.getMinValue() && value <= mFMRulerView.getMaxValue())) {
            switch (touchType) {
                case TOUCH_TYPE_MOVE:
                    onValueChangeListener.onChange(value);
                    break;
                case TOUCH_TYPE_UP:
                    onValueChangeListener.onActionUp(value);
                    break;
                case TOUCH_TYPE_FLING:
                    onValueChangeListener.onFling(value, isFlingEnd);
                    break;
            }
        }
    }

    public interface OnValueChangeListener {
        /**
         * when the current selected index changed will call this method
         *
         * @param value represent the selected value
         */
        void onChange(int value);

        void onActionUp(int value);

        void onFling(int value, boolean isFlingEnd);

        void onCanNotSlide();
    }
}
