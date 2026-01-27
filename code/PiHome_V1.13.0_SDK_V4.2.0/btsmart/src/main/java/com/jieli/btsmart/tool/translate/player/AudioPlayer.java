package com.jieli.btsmart.tool.translate.player;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.component.utils.FileUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * AudioPlayer
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 音频播放器
 * @since 2025/8/18
 */
public class AudioPlayer {

    private static final String TAG = AudioPlayer.class.getSimpleName();
    /**
     * 声道数 --- 单声道
     */
    public static final int CHANNEL_NUM = AudioFormat.CHANNEL_OUT_MONO;
    /**
     * 音频格式 --- PCM_16Bit
     */
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    /**
     * 采样率 --- 16kHz
     */
    public static final int SAMPLE_RATE = 16000;


    /**
     * 空闲状态
     */
    public static final int STATE_IDLE = 0;
    /**
     * 正在播放
     */
    public static final int STATE_PLAYING = 1;
    /**
     * 暂停播放
     */
    public static final int STATE_PAUSE = 2;


    /**
     * PCM类型
     */
    private static final int TYPE_PCM = 0;
    /**
     * WAV类型
     */
    private static final int TYPE_WAV = 1;

    /**
     * 检查间隔
     */
    private static final long CHECK_INTERVAL = 50L;
    /**
     * 检查超时
     */
    private static final long CHECK_TIMEOUT = 3000L;

    /**
     * 单例对象
     */
    private static volatile AudioPlayer instance;

    /**
     * PCM播放器
     */
    @NonNull
    private final AudioTrack mAudioTrack;
    /**
     * 音频播放器
     */
    @NonNull
    private final MediaPlayer mMediaPlayer;
    /**
     * 任务线程池
     */
    private ExecutorService mThreadPool;
    /**
     * 处理任务线程
     */
    private HandleTaskThread mHandleTaskThread;
    /**
     * 播放音乐封装器
     */
    private volatile PlayMusicWrapper mWrapper;

    private AudioPlayer() {
        mAudioTrack = new AudioTrack(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
                new AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AUDIO_FORMAT)
                        .setChannelMask(CHANNEL_NUM)
                        .build(),
                AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_NUM, AUDIO_FORMAT), //缓冲区大小
                AudioTrack.MODE_STREAM, //流模式
                AudioManager.AUDIO_SESSION_ID_GENERATE); //自动生成会话ID
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(mp -> {
            JL_Log.i(TAG, "OnCompletion", "play finish.");
            callbackCompletion();
        });
        mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
            JL_Log.i(TAG, "onError", "---> what : " + what + ", extra : " + extra);
            callbackError(what, "" + extra);
            return true;
        });
        mMediaPlayer.setOnPreparedListener(mp -> {
            JL_Log.i(TAG, "onPrepared", "---> start play");
            mp.start(); //开始播放
            callbackStart(mp.getDuration(), 0);
        });
    }

    public static AudioPlayer getInstance() {
        if (null == instance) {
            synchronized (AudioPlayer.class) {
                if (null == instance) {
                    instance = new AudioPlayer();
                }
            }
        }
        return instance;
    }

    public boolean isPlaying() {
        final PlayMusicWrapper wrapper = mWrapper;
        if (null == wrapper) return false;
        return wrapper.playState == STATE_PLAYING;
    }

    public boolean isPaused() {
        final PlayMusicWrapper wrapper = mWrapper;
        if (null == wrapper) return false;
        return wrapper.playState == STATE_PAUSE;
    }

    public boolean play(PlayTask task) {
        if (null == mHandleTaskThread || !mHandleTaskThread.isRunning) {
            mHandleTaskThread = new HandleTaskThread();
            mHandleTaskThread.start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {

            }
        }
        return mHandleTaskThread.addMusicTask(task);
    }

    public boolean pause() {
        final PlayMusicWrapper wrapper = mWrapper;
        if (null == wrapper) return false;
        if (wrapper.playState == STATE_PAUSE) return true;
        if (wrapper.playState == STATE_IDLE) return false;
        if (wrapper.type == TYPE_PCM) {
            mAudioTrack.pause();
        } else {
            mMediaPlayer.pause();
        }
        wrapper.playState = STATE_PAUSE;
        return true;
    }

    public boolean resume() {
        final PlayMusicWrapper wrapper = mWrapper;
        if (null == wrapper) return false;
        if (wrapper.playState == STATE_PLAYING) return true;
        if (wrapper.playState == STATE_IDLE) return false;
        if (wrapper.type == TYPE_PCM) {
            mAudioTrack.play();
        } else {
            mMediaPlayer.start();
        }
        wrapper.playState = STATE_PLAYING;
        return true;
    }

    public void stop() {
        stop(true);
    }

    public void stop(boolean isAll) {
        if (isAll) {
            if (mHandleTaskThread != null) {
                mHandleTaskThread.stopThread();
                mHandleTaskThread = null;
            }
        }
        final PlayMusicWrapper wrapper = mWrapper;
        if (null == wrapper) return;
        if (wrapper.playState == STATE_IDLE) return;
        if (wrapper.type == TYPE_PCM) {
            stopPlayPcm();
        } else {
            stopPlayWav();
        }
    }

    public void release() {
        stop();
        releaseThreadPool();
        mAudioTrack.release();
        mMediaPlayer.release();
        instance = null;
    }

    private void playPcmFile(String pcmFilePath, OnMusicStateCallback callback) {
        stop(false);
        try {
            mWrapper = new PlayMusicWrapper(pcmFilePath, callback, TYPE_PCM);
            byte[] pcmData = FileUtil.getBytes(pcmFilePath);
            int duration = (int) (((double) pcmData.length / 2.0) * 1000.0 / SAMPLE_RATE);
            mAudioTrack.play();
            callbackStart(duration, pcmData.length);
            mAudioTrack.write(pcmData, 0, pcmData.length);
        } catch (Exception e) {
            callbackError(ErrorCode.SUB_ERR_IO_EXCEPTION, "Exception : " + e.getMessage());
        }
    }

    private void stopPlayPcm() {
        final PlayMusicWrapper wrapper = mWrapper;
        if (null == wrapper || wrapper.type != TYPE_PCM) return;
        try {
            if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                mAudioTrack.stop();
            }
        } catch (Exception ignored) {
        }
        callbackCompletion();
    }

    private void playWavFile(String wavFilePath, OnMusicStateCallback callback) {
        playWavFile(wavFilePath, false, callback);
    }

    private void playWavFile(String wavFilePath, boolean isUseSpeaker, OnMusicStateCallback callback) {
        stop(false);
        try {
            mWrapper = new PlayMusicWrapper(wavFilePath, callback, TYPE_WAV);
            mMediaPlayer.reset();
            if (isUseSpeaker) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build());
            } else {
                mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build());
            }
            mMediaPlayer.setDataSource(wavFilePath);
            mMediaPlayer.prepare();
        } catch (Exception e) {
            callbackError(ErrorCode.SUB_ERR_IO_EXCEPTION, "Exception : " + e.getMessage());
        }
    }

    private void stopPlayWav() {
        final PlayMusicWrapper wrapper = mWrapper;
        if (null == wrapper || wrapper.type != TYPE_WAV) return;
        try {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
        } catch (Exception ignored) {

        }
        callbackCompletion();
    }

    private void callbackStart(int duration, int dataSize) {
        final PlayMusicWrapper wrapper = mWrapper;
        if (null == wrapper) return;
        String filePath = wrapper.filePath;
        OnMusicStateCallback callback = wrapper.callback;
        wrapper.duration = duration;
        wrapper.dataSize = dataSize;
        wrapper.playState = STATE_PLAYING;
        if (null != callback) {
            callback.onStart(filePath, duration);
        }
        if (null == mThreadPool || mThreadPool.isShutdown()) {
            mThreadPool = Executors.newSingleThreadExecutor();
        }
        Runnable runnable = wrapper.type == TYPE_PCM ? checkPlaybackStatusRunnable : calcProcessRunnable;
        mThreadPool.submit(runnable);
    }

    private void callbackCompletion() {
        final PlayMusicWrapper wrapper = mWrapper;
        if (null == wrapper) return;
        String filePath = wrapper.filePath;
        OnMusicStateCallback callback = wrapper.callback;
        wrapper.playState = STATE_IDLE;
        mWrapper = null;
        releaseThreadPool();
        JL_Log.d(TAG, "callbackCompletion", "---> " + filePath);
        if (null != callback) {
            callback.onCompletion(filePath);
        }
    }

    private void callbackError(int code, String message) {
        final PlayMusicWrapper wrapper = mWrapper;
        if (null == wrapper) return;
        String filePath = wrapper.filePath;
        OnMusicStateCallback callback = wrapper.callback;
        wrapper.playState = STATE_IDLE;
        mWrapper = null;
        releaseThreadPool();
        JL_Log.w(TAG, "callbackError", "---> " + filePath + ", \ncode : " + code + ", " + message);
        if (null != callback) {
            callback.onError(filePath, code, message);
        }
    }

    private void releaseThreadPool() {
        if (mThreadPool != null) {
            if (!mThreadPool.isShutdown()) {
                mThreadPool.shutdownNow();
            }
            mThreadPool = null;
        }
    }

    private final Runnable calcProcessRunnable = new Runnable() {
        @Override
        public void run() {
            while (null != mWrapper) {
                final PlayMusicWrapper wrapper = mWrapper;
                String filePath = wrapper.filePath;
                if (TextUtils.isEmpty(filePath) || wrapper.playState == STATE_IDLE) return;
                int progress = mMediaPlayer.getCurrentPosition();
                int duration = mMediaPlayer.getDuration();
                if (progress >= duration) {
                    //播放完成
                    JL_Log.d(TAG, "calcProcess", "Play finish.");
                    return;
                }
                //回调进度
                final OnMusicStateCallback callback = wrapper.callback;
                if (null != callback) {
                    callback.onProgress(filePath, progress);
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    callbackError(ErrorCode.SUB_ERR_IO_EXCEPTION, "Exception : " + e.getMessage());
                    return;
                }
            }
        }
    };

    private final Runnable checkPlaybackStatusRunnable = new Runnable() {
        @Override
        public void run() {
            while (null != mWrapper) {
                final PlayMusicWrapper wrapper = mWrapper;
                String filePath = wrapper.filePath;
                if (TextUtils.isEmpty(filePath) || wrapper.playState == STATE_IDLE) return;
                int dataSize = wrapper.dataSize;
                int position = mAudioTrack.getPlaybackHeadPosition();
                int totalFrames = dataSize / 2;
                if (position >= totalFrames) {
                    //播放完成
                    JL_Log.d(TAG, "checkPlaybackStatus", "Play finish.");
                    callbackCompletion();
                    return;
                }
                //回调进度
                final OnMusicStateCallback callback = wrapper.callback;
                if (null != callback) {
                    callback.onProgress(filePath, position);
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    callbackError(ErrorCode.SUB_ERR_IO_EXCEPTION, "Exception : " + e.getMessage());
                    return;
                }
            }
        }
    };


    private static class PlayMusicWrapper {
        final String filePath;
        final OnMusicStateCallback callback;
        final int type;
        int duration;
        int dataSize;
        int playState;

        public PlayMusicWrapper(String filePath, OnMusicStateCallback callback, int type) {
            this.filePath = filePath;
            this.callback = callback;
            this.type = type;
        }
    }

    private class HandleTaskThread extends Thread {

        private final LinkedBlockingQueue<PlayTask> taskQueue = new LinkedBlockingQueue<>();
        private volatile boolean isRunning;
        private volatile boolean isQueueEmpty = true;
        private volatile boolean isLocked;
        private volatile boolean isScoConnected;

        private int originalVolume;
        private int originalRouting;

        private int result = Integer.MIN_VALUE;
        private PlayTask playTask = null;

        @Override
        public void run() {
            isRunning = true;
            synchronized (taskQueue) {
                while (isRunning) {
                    playTask = null;
                    isQueueEmpty = taskQueue.isEmpty();
                    if (isQueueEmpty) {
                        lock();
                        continue;
                    }
                    PlayTask task = taskQueue.poll();
                    if (null == task || task.isInvalid()) continue;
                    playFile(task);
                }
                isRunning = false;
                taskQueue.clear();
                isQueueEmpty = true;
            }
        }

        public boolean addMusicTask(PlayTask task) {
            if (!isRunning || null == task || task.isInvalid()) return false;
            try {
                taskQueue.put(task);
                if (isQueueEmpty && isLocked && playTask == null) {
                    isQueueEmpty = false;
                    unlock();
                }
                return true;
            } catch (Exception ignored) {

            }
            return false;
        }

        public void stopThread() {
            isRunning = false;
            unlock();
        }

        private void lock() {
            synchronized (taskQueue) {
                if (isLocked) return;
                try {
                    isLocked = true;
                    taskQueue.wait();
                } catch (Exception e) {

                }
                isLocked = false;
            }
        }

        private void unlock() {
            synchronized (taskQueue) {
                if (!isLocked) return;
                try {
                    taskQueue.notifyAll();
                } catch (Exception ignored) {

                }
            }
        }

        private void playFile(PlayTask task) {
            playTask = task;
            final String filePath = task.getFilePath();
            boolean isPcmFile = filePath.endsWith(".pcm") || filePath.toLowerCase().endsWith(".pcm");
            result = Integer.MIN_VALUE;
            final OnMusicStateCallback handler = new OnMusicStateCallback() {
                @Override
                public void onStart(String url, int duration) {

                }

                @Override
                public void onProgress(String url, int position) {

                }

                @Override
                public void onCompletion(String url) {
                    result = ErrorCode.ERR_NONE;
                    unlock();
                }

                @Override
                public void onError(String url, int code, String message) {
                    result = code;
                    unlock();
                }
            };
            boolean isUseSpeaker = task.isUseSpeaker();
            JL_Log.d(TAG, "playFile", "isUseSpeaker : " + isUseSpeaker + ", filePath : " + filePath);
            if (isUseSpeaker) {
                switchToSpeaker();
            }
            if (isPcmFile) {
                playPcmFile(filePath, handler);
            } else {
                playWavFile(filePath, isUseSpeaker, handler);
            }
            if (result == Integer.MIN_VALUE) {
                lock();
            }
            if (isUseSpeaker) {
                resetAudioMode();
            }
        }

        private Context getContext() {
            return MainApplication.getApplication();
        }

        private boolean isBluetoothHeadsetConnected(@NonNull AudioManager audioManager) {
            if (!CommonUtil.checkHasConnectPermission(getContext())) return false;
            return audioManager.isBluetoothA2dpOn() || audioManager.isBluetoothScoOn();
        }

        private void switchToSpeaker() {
            AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            if (null == audioManager) return;
            try {
                originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                originalRouting = audioManager.getMode();

                JL_Log.d(TAG, "switchToSpeaker", "originalVolume =  " + originalVolume + ", originalRouting : " + originalRouting);
                long timeout = 0;
                isScoConnected = false;
                // 关闭蓝牙SCO
                if (isBluetoothHeadsetConnected(audioManager)) { // 需要先检查蓝牙耳机是否连接
                    isScoConnected = audioManager.isBluetoothScoOn();
                    JL_Log.d(TAG, "switchToSpeaker", "isScoConnected --->  " + isScoConnected);
                    if (isScoConnected) {
                        audioManager.stopBluetoothSco();
                        audioManager.setBluetoothScoOn(false);
                    }
                }

                int mode = AudioManager.MODE_IN_COMMUNICATION;
                JL_Log.d(TAG, "switchToSpeaker", "setMode ---> " + mode);
                audioManager.setMode(mode);
                while (audioManager.getMode() != mode) {
                    try {
                        Thread.sleep(CHECK_INTERVAL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    timeout += CHECK_INTERVAL;
                    if (timeout >= CHECK_TIMEOUT) {
                        break;
                    }
                }
                JL_Log.d(TAG, "switchToSpeaker", "mode --->  " + audioManager.getMode());
                // 设置扬声器开启
                JL_Log.d(TAG, "switchToSpeaker", "setSpeakerphoneOn --->  true");
                audioManager.setSpeakerphoneOn(true); // 开启扬声器
                while (!audioManager.isSpeakerphoneOn()) {
                    try {
                        Thread.sleep(CHECK_INTERVAL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    timeout += CHECK_INTERVAL;
                    if (timeout >= CHECK_TIMEOUT) {
                        break;
                    }
                }
                JL_Log.d(TAG, "switchToSpeaker", "isSpeakerphoneOn --->  " + audioManager.isSpeakerphoneOn());
                // 调整音量到合适水平
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int targetVolume = (int) (maxVolume * 0.7); // 70%音量
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0);

                JL_Log.d(TAG, "switchToSpeaker", "current volume : " + targetVolume + "/" + maxVolume);

                // 再次检查SCO状态，关闭蓝牙SCO
                if (isBluetoothHeadsetConnected(audioManager)) { // 需要先检查蓝牙耳机是否连接
                    boolean isScoConnected = audioManager.isBluetoothScoOn();
                    JL_Log.d(TAG, "switchToSpeaker", "check again, isScoConnected --->  " + isScoConnected);
                    if (isScoConnected) {
                        audioManager.stopBluetoothSco();
                        audioManager.setBluetoothScoOn(false);
                    }
                }
            } catch (Exception ignored) {

            }
        }

        private void resetAudioMode() {
            AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            if (null == audioManager) return;
            try {
                long timeout = 0;
                JL_Log.d(TAG, "resetAudioMode", "setSpeakerphoneOn --->  false");
                audioManager.setSpeakerphoneOn(false); // 关闭扬声器
                while (audioManager.isSpeakerphoneOn()) {
                    try {
                        Thread.sleep(CHECK_INTERVAL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    timeout += CHECK_INTERVAL;
                    if (timeout >= CHECK_TIMEOUT) {
                        break;
                    }
                }
                JL_Log.d(TAG, "resetAudioMode", "isSpeakerphoneOn --->  " + audioManager.isSpeakerphoneOn());

                // 如果是蓝牙耳机并需要重新连接
                if (isBluetoothHeadsetConnected(audioManager)) {
                    if (isScoConnected) {
                        JL_Log.d(TAG, "resetAudioMode", "start sco connect");
                        isScoConnected = false;
                        audioManager.startBluetoothSco();
                        audioManager.setBluetoothScoOn(true);
                    }
                }
                //恢复音量
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);

                JL_Log.d(TAG, "resetAudioMode", "setMode ---> " + originalRouting);
                audioManager.setMode(originalRouting); // 恢复普通模式
                while (audioManager.getMode() != originalRouting) {
                    try {
                        Thread.sleep(CHECK_INTERVAL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    timeout += CHECK_INTERVAL;
                    if (timeout >= CHECK_TIMEOUT) {
                        break;
                    }
                }
                JL_Log.d(TAG, "resetAudioMode", "mode --->  " + audioManager.getMode());
            } catch (Exception ignored) {

            }
        }
    }

    public static class PlayTask {
        private final String filePath;
        private final boolean isUseSpeaker;

        public PlayTask(String filePath) {
            this(filePath, false);
        }

        public PlayTask(String filePath, boolean isUseSpeaker) {
            this.filePath = filePath;
            this.isUseSpeaker = isUseSpeaker;
        }

        public String getFilePath() {
            return filePath;
        }

        public boolean isUseSpeaker() {
            return isUseSpeaker;
        }

        public boolean isInvalid() {
            return null == filePath || filePath.isEmpty();
        }

        @Override
        public String toString() {
            return "PlayTask{" +
                    "filePath='" + filePath + '\'' +
                    ", isUseSpeaker=" + isUseSpeaker +
                    '}';
        }
    }
}
