package com.jieli.btsmart.tool.playcontroller;

import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

import com.jieli.audio.media_player.AudioFocusManager;
import com.jieli.audio.media_player.JL_MediaPlayer;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.JL_Log;

import static com.jieli.btsmart.constant.SConstant.ALLOW_SWITCH_FUN_DISCONNECT;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/23 16:49
 * @desc :此处只处理网络电台的耳机MediaButton广播
 */
public class MediaButtonSessionCallbackImpl extends MediaSessionCompat.Callback {
    private final static String TAG = MediaButtonSessionCallbackImpl.class.getSimpleName();
    private final PlayControlImpl playControl;
    private final JL_MediaPlayer mJl_mediaPlayer;
    private final RCSPController mRCSPController = RCSPController.getInstance();

    MediaButtonSessionCallbackImpl(JL_MediaPlayer jl_mediaPlayer, PlayControlImpl playControl) {
        this.mJl_mediaPlayer = jl_mediaPlayer;
        this.playControl = playControl;
    }

    @Override
    public void onPlay() {
        super.onPlay();
        //这里处理播放器逻辑 播放
        if (!mRCSPController.isDeviceConnected() && !ALLOW_SWITCH_FUN_DISCONNECT) return;
        JL_Log.d(TAG, "MediaSessionCompat PLAY");
        if (playControl.getMode() == PlayControlImpl.MODE_NET_RADIO) {
            playControl.play();
        } else {
            mJl_mediaPlayer.play();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //这里处理播放器逻辑 暂停
        JL_Log.d(TAG, "MediaSessionCompat onPause");
        if (!mRCSPController.isDeviceConnected() && !ALLOW_SWITCH_FUN_DISCONNECT) return;
        if (playControl.getMode() == PlayControlImpl.MODE_NET_RADIO) {
            if (AudioFocusManager.getInstance().isResume) {
                playControl.play();
            } else {
                playControl.pause();
            }
        } else {
            if (AudioFocusManager.getInstance().isResume) {
                mJl_mediaPlayer.play();
            } else {
                mJl_mediaPlayer.pause();
            }
        }
    }

    @Override
    public void onSkipToNext() {
        super.onSkipToNext();
        //CMD NEXT 这里处理播放器逻辑 下一曲
        JL_Log.d(TAG, "MediaSessionCompat onSkipToNext");
        if (!mRCSPController.isDeviceConnected() && !ALLOW_SWITCH_FUN_DISCONNECT) return;
        if (playControl.getMode() == PlayControlImpl.MODE_NET_RADIO) {
            playControl.playNext();
        } else {
            mJl_mediaPlayer.playNext();
        }
    }

    @Override
    public void onSkipToPrevious() {
        super.onSkipToPrevious();
        //这里处理播放器逻辑 上一曲
        JL_Log.d(TAG, "MediaSessionCompat onSkipToPrevious");
        if (!mRCSPController.isDeviceConnected() && !ALLOW_SWITCH_FUN_DISCONNECT) return;
        if (playControl.getMode() == PlayControlImpl.MODE_NET_RADIO) {
            playControl.playPre();
        } else {
            mJl_mediaPlayer.playPrev();
        }
    }

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        super.onPlayFromMediaId(mediaId, extras);
        JL_Log.d(TAG, "MediaSessionCompat onPlayFromMediaId");
        if (!mRCSPController.isDeviceConnected() && !ALLOW_SWITCH_FUN_DISCONNECT) return;
    }
}
