package com.jieli.btsmart.ui.widget.visualizer;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * MediaVisualizeView
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 媒体音频振幅控件
 * @since 2025/6/23
 */
public class MediaVisualizeView extends VisualizeView {

    /**
     * 音乐播放器
     */
    private MediaPlayer mMediaPlayer;

    public MediaVisualizeView(Context context) {
        super(context);
    }

    public MediaVisualizeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MediaVisualizeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    public boolean playMusic(@NonNull Context context, int rawId) {
        try {
            releaseMediaPlayer();
            mMediaPlayer = MediaPlayer.create(context, rawId);
            if (mMediaPlayer == null) return false;
            mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                release();
                return true;
            });
            mMediaPlayer.setOnCompletionListener(mp -> release());
            mMediaPlayer.setOnPreparedListener(mp -> {
                setAudioSessionId(mp.getAudioSessionId());
                mp.start();
            });
            mMediaPlayer.prepare();
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    public boolean playMusic(@NonNull Context context, String filePath) {
        try {
            releaseMediaPlayer();
            mMediaPlayer = MediaPlayer.create(context, Uri.parse(filePath));
            if (mMediaPlayer == null) return false;
            mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                release();
                return true;
            });
            mMediaPlayer.setOnCompletionListener(mp -> release());
            mMediaPlayer.setOnPreparedListener(mp -> {
                setAudioSessionId(mp.getAudioSessionId());
                mp.start();
            });
            mMediaPlayer.prepare();
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    @Override
    public void release() {
        releaseMediaPlayer();
        super.release();
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

}
