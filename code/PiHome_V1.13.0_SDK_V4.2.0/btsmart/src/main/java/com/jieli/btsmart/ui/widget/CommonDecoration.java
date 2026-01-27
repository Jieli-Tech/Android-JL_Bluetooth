package com.jieli.btsmart.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 通用分割线
 * @since 2022/6/13
 */
public class CommonDecoration extends RecyclerView.ItemDecoration {
    //采用系统内置的风格的分割线
    private static final int[] attrs = new int[]{android.R.attr.listDivider};
    private Drawable mDivider;
    private int orientation;
    private int dividerHeight = 1;
    private boolean includeEdge; //是否包含边缘

    public CommonDecoration(@NonNull Context context, int orientation) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs);
        Drawable drawable = typedArray.getDrawable(0);
        if (drawable != null) {
            init(orientation, drawable, drawable.getIntrinsicHeight());
        }
        typedArray.recycle();
    }

    public CommonDecoration(@NonNull Context context, int orientation, int color, int height) {
        init(orientation, new ColorDrawable(color), height);
    }

    public CommonDecoration(@NonNull Context context, int orientation, @NonNull Drawable drawable) {
        init(orientation, drawable, drawable.getIntrinsicHeight());
    }

    private void init(int orientation, Drawable drawable, int height) {
        mDivider = drawable;
        this.orientation = orientation;
        this.dividerHeight = height;
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (OrientationHelper.HORIZONTAL == orientation) {
            drawHDivider(c, parent);
        } else {
            drawVDivider(c, parent);
        }
    }

    public boolean isIncludeEdge() {
        return includeEdge;
    }

    public void setIncludeEdge(boolean includeEdge) {
        this.includeEdge = includeEdge;
    }

    /**
     * 绘制水平方向的分割线
     *
     * @param c      画布
     * @param parent 父类
     */
    private void drawHDivider(Canvas c, RecyclerView parent) {
        c.save();
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();
        int childCount = parent.getChildCount();
        if (!includeEdge) childCount--;
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
            int top = child.getBottom() + layoutParams.bottomMargin;
            int bottom = top + dividerHeight;
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
        c.restore();
    }

    /**
     * 绘制垂直方向的分割线
     *
     * @param c      画布
     * @param parent 父类
     */
    private void drawVDivider(Canvas c, RecyclerView parent) {
        c.save();
        int top = parent.getPaddingTop();
        int bottom = parent.getHeight() - parent.getPaddingBottom();
        int childCount = parent.getChildCount();
        if (!includeEdge) childCount--;
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
            int left = child.getRight() + layoutParams.rightMargin;
            int right = left + dividerHeight;
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
        c.restore();
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (mDivider == null) {
            outRect.set(0, 0, 0, 0);
            return;
        }
        if (OrientationHelper.HORIZONTAL == orientation) {
            outRect.set(0, 0, dividerHeight, 0);
        } else {
            outRect.set(0, 0, 0, dividerHeight);
        }
    }
}
