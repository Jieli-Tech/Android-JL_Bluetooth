package com.jieli.btsmart.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.jieli.btsmart.util.RGB2HSLUtil;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/6/29 5:21 PM
 * @desc :
 */
public class CircleBgImageView extends androidx.appcompat.widget.AppCompatImageButton {

    private Paint mColorPaint;
    private boolean isTendToWhite;
    private Paint mArcPaint;

    public CircleBgImageView(Context context) {
        super(context);
        init();
    }

    public CircleBgImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleBgImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mColorPaint = new Paint();
        mColorPaint.setColor(Color.RED);
        mColorPaint.setStyle(Paint.Style.FILL);
        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(true);
        mArcPaint.setColor(Color.parseColor("#D0D0D0"));
        mArcPaint.setStrokeWidth(1f);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);
    }


    public void setColor(int color) {
        this.mColorPaint.setColor(color);
        isTendToWhite = RGB2HSLUtil.checkIsTendToWhite(color, 90);
        invalidate();
    }

    public int getColor() {
        return mColorPaint.getColor();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = getWidth() / 2f;
        canvas.drawCircle(r, r, r - 1f, mColorPaint);
        if (isTendToWhite) {
            RectF rectF = new RectF(0, 0, 2*r,2* r);
            canvas.drawArc(rectF, 0, 360, false, mArcPaint);
        }
        super.onDraw(canvas);
    }
}
