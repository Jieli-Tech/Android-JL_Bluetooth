package com.jieli.btsmart.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class BlockClickEventLayout extends LinearLayout {
    private boolean isIntercept;

    public BlockClickEventLayout(Context context) {
        super(context);
    }

    public BlockClickEventLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BlockClickEventLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BlockClickEventLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setIsIntercept(boolean isIntercept) {
        this.isIntercept = isIntercept;
    }

    public boolean isIntercept() {
        return isIntercept;
    }

    @Override

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isIntercept) return true;
        return super.onInterceptTouchEvent(ev);
    }
}
