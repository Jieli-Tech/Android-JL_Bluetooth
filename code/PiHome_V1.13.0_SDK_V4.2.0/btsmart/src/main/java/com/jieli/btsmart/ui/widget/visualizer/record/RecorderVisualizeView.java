package com.jieli.btsmart.ui.widget.visualizer.record;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.jieli.btsmart.ui.widget.visualizer.VisualizeView;
import com.jieli.btsmart.ui.widget.visualizer.record.data.IdleState;
import com.jieli.btsmart.ui.widget.visualizer.record.data.StartState;
import com.jieli.btsmart.ui.widget.visualizer.record.data.WorkingState;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import be.tarsos.dsp.util.fft.FFT;

/**
 * RecorderVisualizeView
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 音频录音音频振幅控件
 * @since 2025/6/24
 */
public class RecorderVisualizeView extends VisualizeView {

    /**
     * 采样率
     */
    public static int SAMPLE_RATE = 16000;
    /**
     * 声道数
     */
    public static int CHANNEL_NUM = AudioFormat.CHANNEL_IN_MONO;
    /**
     * 编码格式
     */
    public static int ENCODING_FORMAT = AudioFormat.ENCODING_PCM_16BIT;


    public static final int ERR_NONE = 0;
    public static final int ERR_IO_EXCEPTION = 1;
    public static final int ERR_MISSING_PERMISSION = 2;

    public static short[] bytesToShorts(byte[] bytes) {
        if (null == bytes || bytes.length < 2) {
            return new short[0];
        }
        short[] shorts = new short[bytes.length / 2];
        for (int i = 0; i < shorts.length; i++) {
            int index = i * 2;
            // 小端序: 低位在前，高位在后
            shorts[i] = (short) ((bytes[index] & 0xFF) | (bytes[index + 1] << 8));
        }
        return shorts;
    }

    /**
     * 录音机
     */
    private AudioRecord mAudioRecord;
    /**
     * 执行线程
     */
    private ExecutorService mThreadPool;
    /**
     * 降噪算法
     */
    private NoiseSuppressor mNoiseSuppressor;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public RecorderVisualizeView(Context context) {
        super(context);
    }

    public RecorderVisualizeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RecorderVisualizeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void release() {
        releaseAudioRecord();
        releaseThreadPool();
        super.release();
    }

    public boolean isRecording() {
        return mAudioRecord != null && mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED
                && mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING;
    }

    public void startRecord(OnRecordStateCallback callback) {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            //没有录音权限
            if (null != callback) {
                callback.onChange(new IdleState(ERR_MISSING_PERMISSION, "Lack of permissions. " + Manifest.permission.RECORD_AUDIO));
            }
            return;
        }
        try {
            release();
            mThreadPool = Executors.newSingleThreadExecutor();
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_NUM, ENCODING_FORMAT);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE, CHANNEL_NUM, ENCODING_FORMAT, bufferSize);
            mAudioRecord.startRecording();
            if (NoiseSuppressor.isAvailable()) { //检查设备是否支持噪声抑制
                mNoiseSuppressor = NoiseSuppressor.create(mAudioRecord.getAudioSessionId());
                if (null != mNoiseSuppressor) {
                    mNoiseSuppressor.setEnabled(true);
                }
            }
            if (null != callback)
                callback.onChange(new StartState(SAMPLE_RATE, CHANNEL_NUM, ENCODING_FORMAT));
            mThreadPool.submit(() -> processAudio(callback));
        } catch (Exception e) {
            e.printStackTrace();
            if (null != callback) {
                callback.onChange(new IdleState(ERR_IO_EXCEPTION, "IO Exception : " + e));
            }
        }
    }

    private void processAudio(OnRecordStateCallback callback) {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_NUM, ENCODING_FORMAT);
        byte[] audioBuffer = new byte[bufferSize];

        try {
            while (isRecording()) {
                // 读取音频数据
                int readSize = mAudioRecord.read(audioBuffer, 0, audioBuffer.length);
                if (readSize <= 0) continue;
                byte[] data = Arrays.copyOfRange(audioBuffer, 0, readSize);
                if (null != callback) {
                    callback.onChange(new WorkingState(data));
                }
                short[] audioData = bytesToShorts(data);
                float[] fftInput = new float[audioData.length];
                // 转换为单精度浮点数并归一化
                for (int i = 0; i < audioData.length; i++) {
                    fftInput[i] = audioData[i] / 32768.0f;
                }
                //创建FFT算法
                FFT fft = new FFT(audioData.length);

                // 执行FFT
                fft.forwardTransform(fftInput);

                // 计算幅度
                float[] magnitudes = new float[fftInput.length / 2];
                for (int k = 0; k < magnitudes.length; k++) {
                    int i = k * 2;
                    magnitudes[k] = (float) Math.hypot(fftInput[i], fftInput[i + 1]) * 3f;
                }
                uiHandler.post(() -> setRawAudioBytes(magnitudes));
            }
            if (null != callback) {
                callback.onChange(new IdleState(ERR_NONE, "Stop Recoding."));
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (null != callback) {
                callback.onChange(new IdleState(ERR_IO_EXCEPTION, "IO Exception : " + e));
            }
        }
        release();
    }

    private void releaseAudioRecord() {
        if (null != mNoiseSuppressor) {
            mNoiseSuppressor.setEnabled(false);
            mNoiseSuppressor.release();
            mNoiseSuppressor = null;
        }
        if (mAudioRecord != null) {
            if (isRecording()) {
                mAudioRecord.stop();
            }
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

    private void releaseThreadPool() {
        if (mThreadPool != null) {
            mThreadPool.shutdownNow();
            mThreadPool = null;
        }
    }
}
