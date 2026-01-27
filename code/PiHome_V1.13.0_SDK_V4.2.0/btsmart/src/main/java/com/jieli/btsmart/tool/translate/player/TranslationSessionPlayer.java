package com.jieli.btsmart.tool.translate.player;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.data.model.translation.TranslationSessionRecord;
import com.jieli.btsmart.tool.room.repository.TranslationRepository;
import com.jieli.component.utils.FileUtil;

/**
 * TranslationSessionPlayer
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译记录播放器
 * @since 2025/8/18
 */
public class TranslationSessionPlayer {

    private static final String TAG = TranslationSessionPlayer.class.getSimpleName();
    /**
     * 计算进度间隔
     */
    private static final long INTERVAL = 500L;
    /**
     * 计算进度消息
     */
    private static final int MSG_CALC_PROGRESS = 0x6549;

    /**
     * 单例对象
     */
    private static volatile TranslationSessionPlayer instance;
    /**
     * 音乐播放器
     */
    @NonNull
    private final MediaPlayer mMediaPlayer;
    /**
     * 翻译会议记录
     */
    private TranslationSessionRecord mSessionRecord;
    /**
     * 播放器状态回调
     */
    private OnPlayerStateCallback mCallback;
    /**
     * 播放状态
     */
    private int state;
    /**
     * 翻译记录播放索引
     */
    private int index;
    /**
     * 播放类型
     */
    private int playType;
    /**
     * 偏移进度
     */
    private int seekProgress;

    private final Handler mHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == MSG_CALC_PROGRESS) {
            handlePlayerProgress();
        }
        return true;
    });

    private TranslationSessionPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());
        mMediaPlayer.setOnCompletionListener(mp -> {
            JL_Log.d(TAG, "OnCompletion", "play finish.");
            handleMusicStop();
            playNextRecord(ErrorCode.ERR_NONE);
        });
        mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
            JL_Log.w(TAG, "OnError", "what : " + what + ", extra : " + extra);
            handlePlayerError(index, ErrorCode.SUB_ERR_IO_EXCEPTION);
            return true;
        });
        mMediaPlayer.setOnPreparedListener(mp -> {
            mp.start();
            state = AudioPlayer.STATE_PLAYING;
            final OnPlayerStateCallback callback = mCallback;
            if (callback != null) {
                TranslationRecord record = getCurrentRecord();
                if (null != record) {
                    callback.onTranslationRecord(index, record);
                }
                callback.onStateChange(AudioPlayer.STATE_PLAYING);
            }
            if (seekProgress > 0) {
                mp.seekTo(seekProgress);
                seekProgress = 0;
            }
            mHandler.removeMessages(MSG_CALC_PROGRESS);
            mHandler.sendEmptyMessage(MSG_CALC_PROGRESS);
        });
    }

    public static TranslationSessionPlayer getInstance() {
        if (null == instance) {
            synchronized (TranslationSessionPlayer.class) {
                if (null == instance) {
                    instance = new TranslationSessionPlayer();
                }
            }
        }
        return instance;
    }

    public void setOnPlayerStateCallback(OnPlayerStateCallback callback) {
        mCallback = callback;
    }

    public void updateTranslationSessionRecord(int sessionId) {
        TranslationRepository.getInstance().querySessionRecord(sessionId, this::updateTranslationSessionRecord);
    }

    public void updateTranslationSessionRecord(TranslationSessionRecord record) {
        mSessionRecord = record;
        if (record != null) {
            final OnPlayerStateCallback callback = mCallback;
            if (null != callback) {
                callback.onSessionRecordChange(record);
            }
        }
    }

    public TranslationSessionRecord getSessionRecord() {
        return mSessionRecord;
    }

    public TranslationRecord getCurrentRecord() {
        return getTranslationRecord(index);
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying() && state == AudioPlayer.STATE_PLAYING;
    }

    public boolean isPaused() {
        return !mMediaPlayer.isPlaying() && state == AudioPlayer.STATE_PAUSE;
    }

    public int getCurrentPosition() {
        return index;
    }

    public boolean autoPlay(int type) {
        if (null == mSessionRecord) return false;
        final OnPlayerStateCallback callback = mCallback;
        if (null != callback) {
            callback.onStart();
        }
        int position = 0;
        boolean ret = play(type, position);
        if (!ret) {
            stop();
        }
        return ret;
    }

    public boolean play(int type, int position) {
        TranslationRecord record = getTranslationRecord(position);
        if (null == record) return false;
        try {
            String wavFilePath = type == TranslationSessionRecord.TYPE_SRC_TEXT ? record.getSrcFilePath()
                    : record.getDestFilePath();
            JL_Log.d(TAG, "play", "type : " + type + ", position : " + position + ", wavFilePath : " + wavFilePath);
            if (!FileUtil.checkFileExist(wavFilePath)) {
                handlePlayerError(position, ErrorCode.SUB_ERR_NOT_FOUND_DATA);
                return false;
            }
            playType = type;
            index = position;
            stop(false);
            seekProgress = 0;
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(wavFilePath);
            mMediaPlayer.prepare();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            JL_Log.w(TAG, "play", "Exception : " + e.getMessage());
            handlePlayerError(position, ErrorCode.SUB_ERR_IO_EXCEPTION);
        }
        return false;
    }

    public boolean playByProgress(int type, int progress) {
        TranslationRecord record = findTranslationRecordByDuration(type, progress);
        if (null == record) return false;
        int position = mSessionRecord.findPosition(record);
        try {
            String wavFilePath = type == TranslationSessionRecord.TYPE_SRC_TEXT ? record.getSrcFilePath()
                    : record.getDestFilePath();
            JL_Log.d(TAG, "playByProgress", "type : " + type + ", position : " + position + ", wavFilePath : " + wavFilePath);
            if (!FileUtil.checkFileExist(wavFilePath)) {
                handlePlayerError(position, ErrorCode.SUB_ERR_NOT_FOUND_DATA);
                return false;
            }
            playType = type;
            index = position;
            stop(false);
            seekProgress = progress - mSessionRecord.getStartTimeByRecord(type, record);
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(wavFilePath);
            mMediaPlayer.prepare();
            return true;
        } catch (Exception e) {
            JL_Log.w(TAG, "playByProgress", "Exception : " + e.getMessage());
            handlePlayerError(position, ErrorCode.SUB_ERR_IO_EXCEPTION);
        }
        return false;
    }

    public boolean pause() {
        TranslationRecord record = getCurrentRecord();
        if (null == record) return false;
        if (isPaused()) return true;
        if (!isPlaying()) return false;
        try {
            mMediaPlayer.pause();
            state = AudioPlayer.STATE_PAUSE;
            final OnPlayerStateCallback callback = mCallback;
            if (callback != null) {
                callback.onStateChange(AudioPlayer.STATE_PAUSE);
            }
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    public boolean resume() {
        TranslationRecord record = getCurrentRecord();
        if (null == record) return false;
        if (isPlaying()) return true;
        if (!isPaused()) return false;
        try {
            mMediaPlayer.start();
            state = AudioPlayer.STATE_PLAYING;
            final OnPlayerStateCallback callback = mCallback;
            if (callback != null) {
                callback.onStateChange(AudioPlayer.STATE_PLAYING);
            }
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    public boolean stop() {
        return stop(true);
    }

    public boolean stop(boolean isAll) {
        TranslationRecord record = getCurrentRecord();
        if (null == record) return false;
        try {
            if (isAll) {
                index = mSessionRecord.getRecordSize();
                JL_Log.w(TAG, "stop", "Stop all music. index : " + index);
            }
            if (mMediaPlayer.isPlaying() || isPaused()) {
                mMediaPlayer.stop();
            }
            handleMusicStop();
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    public void release() {
        stop();
        mCallback = null;
        mHandler.removeCallbacksAndMessages(null);
        mMediaPlayer.release();
        instance = null;
    }

    private TranslationRecord getTranslationRecord(int index) {
        if (null == mSessionRecord) return null;
        return mSessionRecord.getItem(index);
    }

    private TranslationRecord findTranslationRecordByDuration(int type, int duration) {
        if (null == mSessionRecord) return null;
        return mSessionRecord.findRecordByDuration(type, duration);
    }

    private void playNextRecord(int result) {
        if (null == mSessionRecord) return;
        JL_Log.d(TAG, "playNextRecord", "result : " + result);
        final OnPlayerStateCallback callback = mCallback;
        if (index >= mSessionRecord.getRecordSize() - 1) {
            index = 0;
            mHandler.removeMessages(MSG_CALC_PROGRESS);
            if (null != callback) {
                callback.onStop(result);
            }
            return;
        }
        int position = index + 1;//递增
        boolean ret = play(playType, position);
        if (!ret) {
            handlePlayerError(position, ErrorCode.SUB_ERR_OP_FAILED);
        }
    }

    private void handleMusicStop() {
        state = AudioPlayer.STATE_IDLE;
        final OnPlayerStateCallback callback = mCallback;
        if (callback != null) {
            callback.onStateChange(AudioPlayer.STATE_IDLE);
        }
    }

    private void handlePlayerError(int position, int code) {
        final OnPlayerStateCallback callback = mCallback;
        JL_Log.w(TAG, "handlePlayerError", "position : " + position + ", code : " + CommonUtil.formatInt(code));
        if (callback != null) {
            callback.onError(position, playType, code, ErrorCode.code2Msg(code));
            callback.onStateChange(AudioPlayer.STATE_IDLE);
        }
        if (code != ErrorCode.ERR_NONE) {
            index = position;
        }
        playNextRecord(code);
    }

    private void handlePlayerProgress() {
        if (null == mSessionRecord) return;
        int progress = mMediaPlayer.getCurrentPosition();
        int duration = mMediaPlayer.getDuration();
        if (progress >= duration) {
            //播放完成
            JL_Log.d(TAG, "handlePlayerProgress", "Play finish.");
            return;
        }
        TranslationRecord record = getCurrentRecord();
        if (null == record) return;
        //回调进度
        final OnPlayerStateCallback callback = mCallback;
        if (null != callback) {
            int startTime = mSessionRecord.getStartTimeByRecord(playType, record);
            int readProgress = startTime * 1000 + progress;
            int value = Math.round(readProgress / 1000f);
            JL_Log.d(TAG, "handlePlayerProgress", "position : " + index + ", startTime : " + startTime
                    + ", progress : " + progress + ", readProgress : " + readProgress + ", value : " + value);
            callback.onProgress(value);
        }
        if (mMediaPlayer.isPlaying()) {
            mHandler.removeMessages(MSG_CALC_PROGRESS);
            mHandler.sendEmptyMessageDelayed(MSG_CALC_PROGRESS, INTERVAL);
        }
    }
}
