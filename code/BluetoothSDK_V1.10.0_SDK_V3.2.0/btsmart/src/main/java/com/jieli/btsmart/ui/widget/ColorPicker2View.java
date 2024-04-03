package com.jieli.btsmart.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.jieli.btsmart.ui.light.GlideCircleWithBorder;
import com.jieli.btsmart.util.ColorHSL;
import com.jieli.btsmart.util.ColorRGB;
import com.jieli.btsmart.util.RGB2HSLUtil;
import com.jieli.component.utils.ValueUtil;


public class ColorPicker2View extends FrameLayout {
    private ColorPlateView mColorPlateView;
    public ImageView markerImageView;
    private int markerRadius;
    public static boolean isHSL = true;
    public static int defaultLuminance = 70;

    private boolean currentIsTendToWhite = false;

    public ColorPicker2View(Context context) {
        this(context, null);
    }

    public ColorPicker2View(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPicker2View(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    private void init(Context context, AttributeSet attrs) {
        mColorPlateView = new ColorPlateView(context, attrs);
        addView(mColorPlateView);
        markerImageView = new ImageView(context);
        markerRadius = ValueUtil.dp2px(context, 13);
        LayoutParams imageViewLayoutParams = new LayoutParams(markerRadius * 2, markerRadius * 2);
        addView(markerImageView, imageViewLayoutParams);
        ColorDrawable colorDrawable = new ColorDrawable(Color.TRANSPARENT);
        Glide.with(getContext())
                .load(colorDrawable)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .transform(new GlideCircleWithBorder(getContext(), 1f, Color.parseColor("#ffffff")))
                .into(markerImageView);

        mColorPlateView.setOnMarkerImageViewMoveListener(new OnMarkerImageViewMoveListener() {
            @Override
            public void onMarkerImageViewMove(int PointX, int PointY, boolean isTendToWhite) {
                markerImageView.setTranslationX(PointX - markerRadius);
                markerImageView.setTranslationY(PointY - markerRadius);
                if (currentIsTendToWhite == isTendToWhite) return;
                currentIsTendToWhite = isTendToWhite;
                if (isTendToWhite) {
                    Glide.with(getContext())
                            .load(colorDrawable)
                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                            .transform(new GlideCircleWithBorder(getContext(), 1f, Color.parseColor("#242424")))
                            .into(markerImageView);
                } else {
                    Glide.with(getContext())
                            .load(colorDrawable)
                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                            .transform(new GlideCircleWithBorder(getContext(), 1f, Color.parseColor("#ffffff")))
                            .into(markerImageView);
                }
            }
        });
        setWillNotDraw(false);
    }

    public void setColor(int color) {
        mColorPlateView.setColor(color);
    }

    public void setHueAndSaturation(float hue, float saturation) {
        mColorPlateView.setHueAndSaturation(hue, saturation);
    }

    public int getCurrentColor() {
        return mColorPlateView.getCurrentColor();
    }

    public void setColorPickerListener(OnColorPickerListener listener) {
        mColorPlateView.setColorPickerListener(listener);
    }

    public float getHue() {
        return mColorPlateView.getHue();
    }


    public float getSaturation() {
        return mColorPlateView.getSaturation();
    }


    private class ColorPlateView extends View {

        private final String TAG = ColorPlateView.class.getSimpleName();
        private boolean isFirstInitView = false;
        private int colorWheelRadius;
        private PointF markerPoint = new PointF();
        private OnColorPickerListener mColorPickerListener;
        private float hue;
        private float saturation;

        public ColorPlateView(Context context) {
            super(context);
            init();
        }

        public ColorPlateView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        private Paint colorWheelPaint;
        private Paint mSelectPaint;

        private float mSelectRadius;

        private int mWidth;
        private int mHeight;
        private Bitmap colorWheelBitmap;
        private int currentColor;
        private float centerWheelX;
        private float centerWheelY;
        private PointF currentPoint = new PointF();

        private long mLastMoveTime;
        private OnMarkerImageViewMoveListener markerImageViewMoveListener;

        public void init() {
            colorWheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            colorWheelPaint.setStyle(Paint.Style.FILL);
            isFirstInitView = true;

            mSelectPaint = new Paint();
            mSelectPaint.setStyle(Paint.Style.STROKE);
            mSelectPaint.setStrokeWidth(3f);
            mSelectPaint.setColor(Color.WHITE);
            mSelectPaint.setAntiAlias(true);
        }

        public float getHue() {
            return hue;
        }

        public float getSaturation() {
            return saturation;
        }

        @SuppressLint("DrawAllocation")
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            mWidth = MeasureSpec.getSize(widthMeasureSpec);
            mHeight = MeasureSpec.getSize(heightMeasureSpec);
            int min = Math.min(mWidth, mHeight);
            mWidth = mHeight = min;
            setMeasuredDimension(min, min);
            colorWheelRadius = (int) (min * 0.5f);
            centerWheelX = mWidth * 0.5f;
            centerWheelY = 0.5f * mHeight;
            mSelectRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12.5f,
                    getContext().getResources().getDisplayMetrics());
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Log.i(TAG, "onDraw: ");
            colorWheelBitmap = createColorWheelBitmap(colorWheelRadius * 2, colorWheelRadius * 2);
            Log.i(TAG, "onDraw: " + colorWheelBitmap.getWidth() + ",  " + colorWheelBitmap.getHeight());
            //绘制色盘
            if (isFirstInitView) {
                isFirstInitView = false;
                markerPoint.x = colorWheelBitmap.getWidth() / 2;
                markerPoint.y = colorWheelBitmap.getHeight() / 2;
            }
            canvas.drawBitmap(colorWheelBitmap, /*mColorWheelRect.left*/0, 0/*mColorWheelRect.top*/, null);
        }

        //创建色盘Bitmap
        private Bitmap createColorWheelBitmap(int width, int height) {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            int colorCount = 12;
            int colorAngleStep = 360 / colorCount;
            int colors[] = new int[colorCount + 1];
            float hsv[] = new float[]{0f, 1f, 1f};
            for (int i = 0; i < colors.length; i++) {
                hsv[0] = 360 - (i * colorAngleStep) % 360;
            /*int h = 360 - (i * colorAngleStep) % 360;//这里用HSL还是HSV都一样，因为两者的H是一样的
            ColorRGB colorRGB = RGB2HSLUtil.HSLtoRGB(new ColorHSL(h, 100, 50));
            int color = Color.rgb(colorRGB.getRed(), colorRGB.getGreen(), colorRGB.getBlue());
            colors[i] =color;*/
                colors[i] = Color.HSVToColor(hsv);
            }
            colors[colorCount] = colors[0];
            SweepGradient sweepGradient = new SweepGradient(width / 2, height / 2, colors, null);
            RadialGradient radialGradient = new RadialGradient(width / 2, height / 2, colorWheelRadius, 0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
            ComposeShader composeShader = new ComposeShader(sweepGradient, radialGradient, PorterDuff.Mode.SRC_OVER);
            colorWheelPaint.setShader(composeShader);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawCircle(width / 2, height / 2, colorWheelRadius, colorWheelPaint);

            //默认取圆心颜色，给一个默认颜色用于显示点标记
            currentColor = getColorAtPoint(markerPoint.x, markerPoint.y);
            return bitmap;
        }

        private int colorTmp;///用于判断颜色是否发生改变
        private PointF downPointF = new PointF();//按下的位置

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mLastMoveTime = System.currentTimeMillis();
                    colorTmp = currentColor;
                    downPointF.x = event.getX();
                    downPointF.y = event.getY();
                case MotionEvent.ACTION_MOVE:
                    update(event);
                    if (200 < System.currentTimeMillis() - mLastMoveTime) {
                        mLastMoveTime = System.currentTimeMillis();
                        if (null != mColorPickerListener && colorTmp != currentColor) {
                            mColorPickerListener.onColorChanged(currentColor, false);
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (null != mColorPickerListener && colorTmp != currentColor) {
                        mColorPickerListener.onColorChanged(currentColor, true);
                    }
                    break;
                default:
                    return true;
            }
            return super.onTouchEvent(event);
        }

        private void update(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            updateSelector(x, y);
        }

        /**
         * 刷新s色盘所选择的颜色
         *
         * @param eventX
         * @param eventY
         */
        private void updateSelector(float eventX, float eventY) {
            float x = eventX - centerWheelX;
            float y = eventY - centerWheelY;
            double r = Math.sqrt(x * x + y * y);
            //判断是否在圆内
            if (r > colorWheelRadius) {
                //不在圆形范围内
                return;
            }
            currentPoint.x = x + centerWheelX;
            currentPoint.y = y + centerWheelY;
            markerPoint.x = currentPoint.x;//改变点标记位置
            markerPoint.y = currentPoint.y;
            currentColor = getColorAtPoint(eventX, eventY);//获取到的颜色

            markerImageViewMoveListener.onMarkerImageViewMove((int) markerPoint.x, (int) markerPoint.y, RGB2HSLUtil.checkIsTendToWhite(currentColor, 90));
        }

        /**
         * 获取两条线的夹角
         *
         * @param centerX
         * @param centerY
         * @param xInView
         * @param yInView
         * @return
         */
        public int getRotationBetweenLines(float centerX, float centerY, float xInView, float yInView) {
            double rotation = 0;
            double k1 = (double) (centerY - centerY) / (centerX * 2 - centerX);
            double k2 = (double) (yInView - centerY) / (xInView - centerX);
            double tmpDegree = Math.atan((Math.abs(k1 - k2)) / (1 + k1 * k2)) / Math.PI * 180;

            if (xInView > centerX && yInView < centerY) {  //第一象限
                rotation = 90 - tmpDegree;
            } else if (xInView > centerX && yInView > centerY) //第二象限
            {
                rotation = 90 + tmpDegree;
            } else if (xInView < centerX && yInView > centerY) { //第三象限
                rotation = 270 - tmpDegree;
            } else if (xInView < centerX && yInView < centerY) { //第四象限
                rotation = 270 + tmpDegree;
            } else if (xInView == centerX && yInView < centerY) {
                rotation = 0;
            } else if (xInView == centerX && yInView > centerY) {
                rotation = 180;
            }

            return (int) rotation;
        }

        public void setOnMarkerImageViewMoveListener(OnMarkerImageViewMoveListener listener) {
            markerImageViewMoveListener = listener;
        }

        /**
         * 根据坐标获取颜色
         *
         * @param eventX
         * @param eventY
         * @return
         */
        private int getColorAtPoint(float eventX, float eventY) {
            if (!isHSL) {
                float x = eventX - centerWheelX;
                float y = eventY - centerWheelY;
                double r = Math.sqrt(x * x + y * y);
                float[] hsv = {0, 0, 1};
                hsv[0] = (float) (Math.atan2(y, -x) / Math.PI * 180f) + 180;
                hsv[1] = Math.max(0f, Math.min(1f, (float) (r / colorWheelRadius)));
                hue = hsv[0];
                saturation = hsv[1];
                return Color.HSVToColor(hsv);
            } else {
                float x = eventX - centerWheelX;
                float y = eventY - centerWheelY;
                double r = Math.sqrt(x * x + y * y);
                ColorHSL colorHSL = new ColorHSL();
//            float[] hsv = {0, 0, 1};
                hue = (float) (Math.atan2(y, -x) / Math.PI * 180f) + 180;
                saturation = Math.max(0f, Math.min(1f, (float) (r / colorWheelRadius)));
                colorHSL.setHue(hue);
                colorHSL.setSaturation(saturation * 100f);
                colorHSL.setLuminance((((2 - saturation) / 2) * 100f));
                ColorRGB colorRGB = RGB2HSLUtil.HSLtoRGB(colorHSL);
                int color = Color.rgb(colorRGB.getRed(), colorRGB.getGreen(), colorRGB.getBlue());
                Log.i(TAG, "getColorAtPoint: :color  " + color);
                return color;
            }
        }

        public void setColor(int color) {
            if (color == currentColor) return;
            if (!isHSL) {
                //hsv
                float[] hsv = {0, 0, 1};
                Color.colorToHSV(color, hsv);
                //根据hsv角度及半径获取坐标
                //根据角度和半径获取坐标
                float radian = (float) Math.toRadians(-hsv[0]);
                float colorDotRadius = hsv[1] * colorWheelRadius;
                float colorDotX = (float) (centerWheelX + Math.cos(radian) * colorDotRadius);
                float colorDotY = (float) (centerWheelY + Math.sin(radian) * colorDotRadius);
                //设置marker位置
                hue = hsv[0];
                saturation = hsv[1];
                markerPoint.x = colorDotX;
                markerPoint.y = colorDotY;
                currentColor = getColorAtPoint(markerPoint.x, markerPoint.y);//设置当前颜色
                //设置色环按钮位置
                markerImageViewMoveListener.onMarkerImageViewMove((int) markerPoint.x, (int) markerPoint.y, RGB2HSLUtil.checkIsTendToWhite(currentColor, 90));
            } else {
                //hsl
                ColorHSL colorHSL = RGB2HSLUtil.RGBtoHSL(color);
                //根据hsv角度及半径获取坐标
                //根据角度和半径获取坐标
//                float l = colorHSL.getSaturation();
                float radian = (float) Math.toRadians(-colorHSL.getHue());
                float colorDotRadius = (colorHSL.getSaturation() / 100) * colorWheelRadius;
                float colorDotX = (float) (centerWheelX + Math.cos(radian) * colorDotRadius);
                float colorDotY = (float) (centerWheelY + Math.sin(radian) * colorDotRadius);
                hue = colorHSL.getHue();
                saturation = colorHSL.getSaturation() / 100;
                //设置marker位置
                markerPoint.x = colorDotX;
                markerPoint.y = colorDotY;
                currentColor = getColorAtPoint(markerPoint.x, markerPoint.y);//设置当前颜色
                //设置色环按钮位置
                markerImageViewMoveListener.onMarkerImageViewMove((int) markerPoint.x, (int) markerPoint.y, RGB2HSLUtil.checkIsTendToWhite(currentColor, 90));
            }
        }

        public void setHueAndSaturation(float hue, float saturation) {
            if (this.hue == hue && this.saturation == saturation) return;
            //根据hsv角度及半径获取坐标
            //根据角度和半径获取坐标
//            float l = saturation;
            float radian = (float) Math.toRadians(-hue);
            float colorDotRadius = (saturation / 100) * colorWheelRadius;
            float colorDotX = (float) (centerWheelX + Math.cos(radian) * colorDotRadius);
            float colorDotY = (float) (centerWheelY + Math.sin(radian) * colorDotRadius);
            //设置marker位置
            markerPoint.x = colorDotX;
            markerPoint.y = colorDotY;
            currentColor = getColorAtPoint(markerPoint.x, markerPoint.y);//设置当前颜色
            //设置色环按钮位置
            markerImageViewMoveListener.onMarkerImageViewMove((int) markerPoint.x, (int) markerPoint.y, RGB2HSLUtil.checkIsTendToWhite(currentColor, 90));
        }

        public int getCurrentColor() {
            return currentColor;
        }

        public void setColorPickerListener(OnColorPickerListener listener) {
            mColorPickerListener = listener;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            getParent().requestDisallowInterceptTouchEvent(true);
            return super.dispatchTouchEvent(event);
        }
    }

    public interface OnColorPickerListener {
        void onColorChanged(int color, boolean end/*, int hue, int saturation, int luminance*/);
    }

    private interface OnMarkerImageViewMoveListener {
        void onMarkerImageViewMove(int PointX, int PointY, boolean isTendToWhite);
    }
}
