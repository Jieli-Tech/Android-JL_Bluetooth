package com.jieli.btsmart.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

/**
 * @author : zpc18-003
 * @e-mail :
 * @date : 2020/6/9 3:59 PM
 * @desc :
 */
public class ViewPager2RecycleView extends RecyclerView {

    private float mStartX;
    private MotionEvent mDownEvent = null;

    public ViewPager2RecycleView(@NonNull Context context) {
        super(context);
    }

    public ViewPager2RecycleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ViewPager2RecycleView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        int action = e.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            mDownEvent = e;
            mStartX = e.getX();
            setViewPager2InputEnable(false);//禁止viewPager2的滑动处理
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            setViewPager2InputEnable(true);
            mDownEvent = null;
        } else if (action == MotionEvent.ACTION_MOVE) {
            //判断第一次移动是否需要禁止ViewPager2的滑动
            if (mDownEvent != null) {
                float x = e.getX();
                float div = x - mStartX;
                boolean canScrollRight = canScrollHorizontally(1);
                boolean canScrollLeft = canScrollHorizontally(-1);
                if (div < 0) {
                    setViewPager2InputEnable(!canScrollRight);
                } else {
                    setViewPager2InputEnable(!canScrollLeft);
                }
                mDownEvent = null;
            }

        }
        return super.dispatchTouchEvent(e);
    }


    private void setViewPager2InputEnable(boolean enable) {
        ViewPager2 viewPager2 = getViewPager2();
        if (viewPager2 != null) {
            viewPager2.setUserInputEnabled(enable);
        }
    }

    private ViewPager2 getViewPager2() {
        ViewParent parent = this;
        while ((parent = parent.getParent()) != null) {
            if (parent instanceof ViewPager2) {
                return (ViewPager2) parent;
            }
        }
        return null;

    }
}
