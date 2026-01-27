package com.jieli.btsmart.ui.widget.visualizer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;

/**
 * VisualizeView
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 音频振幅控件
 * @since 2025/6/23
 */
public class VisualizeView extends SurfaceView implements SurfaceHolder.Callback {
    /**
     * 最大帧数
     */
    private static final int MAX_SAMPLES = 320;
    /**
     * 振渲染间隔
     */
    private static final long TARGET_FRAME_TIME_MS = 112; // 60fps in nanoseconds

    /**
     * dp covert to px
     *
     * @param context 上下文
     * @param dp      dp
     */
    public static int dp2px(Context context, int dp) {
        if (context == null) {
            throw new RuntimeException("context is null");
        }
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    /**
     * the count of spectrum
     */
    protected int mSpectrumCount;
    /**
     * the margin of adjoin spectrum
     */
    protected float mItemMargin;
    /**
     * ratio of spectrum, between 0.0f - 2.0f
     */
    protected float mSpectrumRatio;
    /**
     * the width of every spectrum
     */
    protected float mStrokeWidth;
    /**
     * the color of drawing spectrum
     */
    protected int mColor;
    /**
     * control enable of visualize
     */
    protected boolean isVisualizationEnabled = true;
    /**
     * audio data transform by hypot
     */
    protected final float[] mRawAudioBytes = new float[MAX_SAMPLES];
    /**
     * 默认线条高度
     */
    private int lineHeight;

    /**
     * 绘画区域
     */
    protected RectF mRect;
    /**
     * 画笔参数
     */
    protected Paint mPaint;
    /**
     * 路径
     */
    protected Path mPath;
    /**
     * 绘画中心X
     */
    protected float centerX;
    /**
     * 绘画中心Y
     */
    protected float centerY;
    /**
     * 音频可视化工具
     */
    private Visualizer mVisualizer;
    /**
     * 渲染线程
     */
    private RenderThread renderThread;
    /**
     * 线程是否运行中
     */
    private volatile boolean isRunning = false;

    public VisualizeView(Context context) {
        this(context, null);
    }

    public VisualizeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VisualizeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public VisualizeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr);
    }

    /*@Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mRect.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom());
        centerX = mRect.width() / 2;
        centerY = (float) getHeight() / 2;
        mSpectrumCount = Math.round((mRect.width() - mStrokeWidth) / (mItemMargin + mStrokeWidth)) + 1;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int wSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int hSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(wSpecSize, hSpecSize);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        drawView(canvas);
    }*/

    // 将 px 值转换为 dp
    public static float pxToDp(Context context, float px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        if (displayMetrics.density == 0) return 0;
        return px / displayMetrics.density;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        JL_Log.d("zzc", "surfaceCreated", "getWidth : " + getWidth()
                + ", getHeight : " + getHeight() + " --> " + pxToDp(getContext(), getHeight()));
        //视图创建
        mRect.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom());
        centerX = mRect.width() / 2;
        centerY = (float) getHeight() / 2;
        mSpectrumCount = Math.round((mRect.width() - mStrokeWidth) / (mItemMargin + mStrokeWidth)) + 1;
        renderThread = new RenderThread(holder);
        isRunning = true;
        renderThread.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        JL_Log.d("zzc", "surfaceChanged", "width : " + width + ", height : " + height
                + ", getWidth : " + getWidth() + ", getHeight : " + getHeight() + " --> " + pxToDp(getContext(), getHeight()));
        //视图尺寸改变
        mRect.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom());
        centerX = mRect.width() / 2;
        centerY = (float) getHeight() / 2;
        mSpectrumCount = Math.round((mRect.width() - mStrokeWidth) / (mItemMargin + mStrokeWidth)) + 1;
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        //视图销毁
        isRunning = false;
        if (null != renderThread) {
            try {
                renderThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            renderThread = null;
        }
    }

    public void release() {
        releaseVisualizer();
        setRawAudioBytes(new float[0]);
    }

    public void setRawAudioBytes(float[] data) {
        if (null == data || mSpectrumCount <= 0) return;
        synchronized (mRawAudioBytes) {
            int dataSize = data.length;
            int itemCount = Math.min(mSpectrumCount, MAX_SAMPLES);
            for (int i = 0; i < itemCount; i++) {
                mRawAudioBytes[i] = i < dataSize ? data[i] : 0f;
            }
        }
    }

    public boolean setAudioSessionId(int audioSessionId) {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (mVisualizer != null) {
            releaseVisualizer();
        }
        //初始化
        mVisualizer = new Visualizer(audioSessionId);
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        //设置数据回调
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {

            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                int n = fft.length;
                float[] magnitudes = new float[n / 2 + 1];
                magnitudes[0] = (float) Math.abs(fft[0]);      // DC
                magnitudes[n / 2] = (float) Math.abs(fft[1]);  // Nyquist
                for (int k = 1; k < n / 2; k++) {
                    int i = k * 2;
                    //优化振幅效果，频幅/2
                    magnitudes[k] = (float) Math.hypot(fft[i], fft[i + 1]) / 2f;
                }
                setRawAudioBytes(magnitudes);
            }
        }, Visualizer.getMaxCaptureRate() / 2, false, true);

        mVisualizer.setEnabled(true); //开启采集
        return true;
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.VisualizeView, defStyle, 0);
        try {
            mColor = ta.getColor(R.styleable.VisualizeView_visualize_color, Color.WHITE);
            mStrokeWidth = ta.getInteger(R.styleable.VisualizeView_visualize_item_width, dp2px(context, 2));
            mSpectrumRatio = ta.getFloat(R.styleable.VisualizeView_visualize_ratio, 1.0f);
            mItemMargin = ta.getDimension(R.styleable.VisualizeView_visualize_item_margin, dp2px(context, 2));
        } catch (Exception ignored) {

        } finally {
            ta.recycle();
        }

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setColor(mColor);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setAntiAlias(true);
        mPaint.setMaskFilter(new BlurMaskFilter(5, BlurMaskFilter.Blur.SOLID));

        mRect = new RectF();
        mPath = new Path();

        lineHeight = dp2px(context, 2);

        final SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        // 设置SurfaceView为透明背景
        setZOrderOnTop(true);
        holder.setFormat(PixelFormat.TRANSPARENT);
    }

    private void drawView(@NonNull Canvas canvas) {
        if (!isVisualizationEnabled || mSpectrumCount <= 0) return;
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        float[] localData;
        synchronized (mRawAudioBytes) {
            localData = mRawAudioBytes.clone();
        }
        int maxUpY = getPaddingTop();
        int maxBottomY = Math.round(getPaddingTop() + mRect.height());
        if (maxBottomY > (getHeight() - getPaddingBottom())) {
            maxBottomY = getHeight() - getPaddingBottom();
        }
        for (int i = 0; i < mSpectrumCount; i++) {
            float value = localData[i];
            float x = Math.round(getPaddingLeft() + mRect.width() * i / mSpectrumCount + mItemMargin);
            float upY = Math.round(centerY - lineHeight - mSpectrumRatio * value);
            if (upY < maxUpY) {
                upY = maxUpY;
            }
            float bottomY = Math.round(centerY + lineHeight + mSpectrumRatio * value);
            if (bottomY > maxBottomY) {
                bottomY = maxBottomY;
            }
            canvas.drawLine(x, centerY, x, upY, mPaint);
            canvas.drawLine(x, centerY, x, bottomY, mPaint);
        }
    }

    private void releaseVisualizer() {
        if (mVisualizer != null) {
            mVisualizer.setEnabled(false);
            mVisualizer.release();
            mVisualizer = null;
        }
    }

    private class RenderThread extends Thread {
        private final SurfaceHolder surfaceHolder;
        private long lastFrameTime;

        public RenderThread(SurfaceHolder holder) {
            this.surfaceHolder = holder;
            this.lastFrameTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            while (isRunning) {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - lastFrameTime;

                // 控制帧率
                if (elapsedTime < TARGET_FRAME_TIME_MS) {
                    try {
                        long sleepTime = TARGET_FRAME_TIME_MS - elapsedTime;
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }

                lastFrameTime = currentTime;

                Canvas canvas = null;
                try {
                    canvas = surfaceHolder.lockCanvas();
                    if (canvas != null) {
                        drawView(canvas);
                    }
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }
}
