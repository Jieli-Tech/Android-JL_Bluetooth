package com.jieli.btsmart.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Shader;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.jieli.btsmart.R;

/**
 * TODO: document your custom view class.
 */
public class WaveView extends View {


    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;
    private Paint mLinePaint;
    private Paint mEndPaint;
    private Paint mScalePaint;


    private Point[] mPoint = new Point[10];
    private int mOffsetY;

    public WaveView(Context context) {
        super(context);
        init(null, 0);
    }

    public void setOffsetY(int mOffsetY) {
        this.mOffsetY = mOffsetY;
        invalidate();
    }

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public WaveView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setColor(Color.BLUE);


        mLinePaint = new Paint();
        mLinePaint.setColor(0xFF805BEB);
        mLinePaint.setTextAlign(Paint.Align.CENTER);
        mLinePaint.setStrokeWidth(5);

        mScalePaint = new Paint();
        mScalePaint.setColor(Color.WHITE);
        mScalePaint.setTextAlign(Paint.Align.CENTER);
        mScalePaint.setStrokeWidth(1);

        mEndPaint = new Paint();
        mEndPaint.setColor(Color.GRAY);

        for (int i = 0; i < mPoint.length; i++) {
            mPoint[i] = new Point(i * 10, (int) (Math.random() * 100));
        }

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.RED);
        Path path = new Path();
        Point[] points = calculatePosition();
        points = segmentPoints(points);
        path.moveTo(points[0].x, points[0].y);
        Point firstC = calculateControlPoint(points[0], points[1]);
        path.quadTo(firstC.x, firstC.y, points[1].x, points[1].y);
        for (int i = 1; i < points.length - 2; i += 2) {
            Point controlP = points[i + 1];
            Point end = points[i + 2];
            path.quadTo(controlP.x, controlP.y, end.x, end.y);
            canvas.drawCircle(controlP.x, controlP.y, 12, mTextPaint);
//            Point p = points[i];
//            canvas.drawCircle(p.x, p.y, 12, mLinePaint);
        }

        Point lastC = calculateControlPoint(points[points.length - 2], points[points.length - 1]);
        path.quadTo(lastC.x, lastC.y, points[points.length - 1].x, points[points.length - 1].y);

//        canvas.drawCircle(1080, 50, 12, mEndPaint);
//        canvas.drawCircle(0, 50, 12, mEndPaint);


        canvas.save();
        mLinePaint.setStyle(Paint.Style.FILL);
        int maxHeight = getMaxHeight(points);
        Shader shader = new LinearGradient(0, getHeight() - maxHeight, 0, maxHeight, 0xFF805BEB, 0x7f5e41eb, Shader.TileMode.REPEAT);
        mLinePaint.setShader(shader);
        canvas.drawPath(path, mLinePaint);
        mLinePaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(path, mLinePaint);

        canvas.clipPath(path);
        drawScale(canvas, points);
    }


    private void drawScale(Canvas canvas, Point[] points) {
        for (int i = 0; i < points.length - 2; i += 2) {
            canvas.drawLine(points[i].x, points[i].y, points[i].x, getHeight(), mScalePaint);
        }
    }

    private int getMaxHeight(Point[] points) {
        int max = 0;
        for (Point p : points) {
            if (p.y > max) {
                max = p.y;
            }
        }
        return max;
    }


    // TODO: 2020/5/30  for test
    public void refresh() {
        for (int i = 0; i < mPoint.length; i++) {
            mPoint[i] = new Point(i * 10, (int) (Math.random() * 100));
        }
        invalidate();
    }

    public void update(int index, int value) {
        mPoint[index].y = 100-value;
        invalidate();
    }

    public void setData(int [] value){
        if(value.length!=10){
            return;
        }

        for(int i=0;i<mPoint.length;i++){
            mPoint[i].y=value[i];
        }
        invalidate();
    }


    private Point[] calculatePosition() {
        Point[] points = new Point[mPoint.length + 2];

        int contentWidth = getWidth();
        int contentHeight = getHeight();
        int step = contentWidth / (points.length - 1);
        int offset = step;


        for (int i = 1; i < points.length - 1; i++) {
            Point temp = new Point(offset, (int) (mPoint[i - 1].y / 100f * contentHeight));
            points[i] = temp;
            offset += step;
        }

        points[0] = new Point(0, contentHeight);//增加头节点
        points[points.length - 1] = new Point(contentWidth, contentHeight); //增加尾节点
        return points;

    }


    private Point[] segmentPoints(Point[] points) {
        Point[] result = new Point[points.length * 2 - 1];
        Point last = points[0];
        result[0] = last;
        for (int i = 1; i < points.length; i++) {
            result[i * 2 - 1] = calculateCenterPoint(last, points[i]);
            result[i * 2] = points[i];
            last = points[i];
        }
        return result;
    }


    private Point calculateCenterPoint(Point start, Point end) {
        Point point = new Point();
        point.x = start.x + (end.x - start.x) / 2;
        point.y = start.y + (end.y - start.y) / 2;
        return point;
    }

    private Point calculateControlPoint(Point start, Point end) {
        Point point = new Point();
        point.x = start.x + (end.x - start.x) / 2;
        point.y = (end.y - start.y) / 2 + start.y;
        return point;
    }


}
