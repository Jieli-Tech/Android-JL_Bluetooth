package com.jieli.btsmart.tool.playcontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.jieli.audio.media_player.JL_MediaPlayer;
import com.jieli.audio.media_player.JL_MediaPlayerCallback;
import com.jieli.audio.media_player.JL_PlayMode;
import com.jieli.audio.media_player.Music;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.HandlerManager;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.ToastUtil;

import java.io.IOException;


/**
 * Created by chensenhua on 2018/1/17.
 */

public class LocalPlayControlImpl implements PlayControl {

    private static final String tag = LocalPlayControlImpl.class.getSimpleName();
    private PlayControlCallback mPlayControlCallback;
    private JL_MediaPlayer mJL_MediaPlayer;
    private final RCSPController mRCSPController = RCSPController.getInstance();

    private VolumeReceiver mVolumeReceiver;
    private AudioManager audioManager;
    private MediaMetadataRetriever mMediaMetadataRetriever;
    private final Context context;

    private static boolean isSetVolume = false;
    private boolean onFrontdesk = false;


    public LocalPlayControlImpl(JL_MediaPlayer mJL_MediaPlayer, Context context) {
        this.mJL_MediaPlayer = mJL_MediaPlayer;
        JL_Log.i(tag, "-LocalPlayControlImpl init-");
        this.context = context.getApplicationContext();
        mMediaMetadataRetriever = new MediaMetadataRetriever();
        this.mJL_MediaPlayer.registerMusicPlayerCallback(mJl_mediaPlayerCallback);
        audioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mVolumeReceiver = new VolumeReceiver();
        context.getApplicationContext().registerReceiver(mVolumeReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
    }


    private final Runnable requestMusicPositon = new Runnable() {
        @Override
        public void run() {
            if (mJL_MediaPlayer == null || mPlayControlCallback == null) {
                JL_Log.i(tag, tag + "-LocalPlayControlImpl requestMusicPositon--->null");
                return;
            }
            if ((mJL_MediaPlayer.getDuration() > 0 && mJL_MediaPlayer.getDuration() < 36000) || mJL_MediaPlayer.getCurrentPosition() != 0) {
                mPlayControlCallback.onTimeChange(mJL_MediaPlayer.getCurrentPosition(), mJL_MediaPlayer.getDuration());
            }
            //用sdk内部计时
//            if (mJL_MediaPlayer.isPlaying() && onFrontdesk) {
//                HandlerManager.getInstance().getMainHandler().postDelayed(requestMusicPositon, 1000);
//            }
        }
    };


    @Override
    public void playNext() {
        if (mJL_MediaPlayer != null)
            mJL_MediaPlayer.playNext();
    }

    @Override
    public void playPre() {
        if (mJL_MediaPlayer != null)
            mJL_MediaPlayer.playPrev();
    }

    @Override
    public void play() {
        JL_Log.i("zzc", tag + "-play-");
        if (mJL_MediaPlayer != null) {
            mJL_MediaPlayer.play();
        }
    }

    @Override
    public void pause() {
        if (mJL_MediaPlayer != null && mJL_MediaPlayer.isPlaying()) {
            mJL_MediaPlayer.pause();
        }
    }


    @Override
    public void playOrPause() {
        mJL_MediaPlayer.playOrPause();
    }

    @Override
    public void setVolume(int value) {
        if (mRCSPController.isDeviceConnected()) {
            if (isSetVolume) return;
            mRCSPController.adjustVolume(mRCSPController.getUsingDevice(), value, null);
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, AudioManager.FLAG_SHOW_UI);
        }
    }

    @Override
    public void volumeUp() {
        volumeUp(SConstant.DEVICE_VOLUME_STEP);
    }


    private void volumeUp(int step) {
        if (mRCSPController.isDeviceConnected()) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
            if (isSetVolume || deviceInfo == null) return;
            int volume = deviceInfo.getVolume();
            int max = deviceInfo.getMaxVol();
            if (volume == max) {
                ToastUtil.showToastTop(AppUtil.getContext().getString(R.string.current_volume) + volume, Toast.LENGTH_SHORT);
                return;
            }

            int currentVol = volume + step;
            if (currentVol > max) {
                currentVol = max;
            }
            setVolume(currentVol);
        } else {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
        }
    }

    @Override
    public void volumeUpByHand() {
        volumeUp(1);
    }

    @Override
    public void volumeDown() {
        volumeDown(SConstant.DEVICE_VOLUME_STEP);
    }


    @Override
    public void volumeDownByHand() {
        volumeDown(1);
    }

    private void volumeDown(int step) {
        if (mRCSPController.isDeviceConnected()) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
            if (isSetVolume || deviceInfo == null) return;

            int volume = deviceInfo.getVolume();
            if (volume == 0) {
                ToastUtil.showToastTop(AppUtil.getContext().getString(R.string.current_volume) + volume, Toast.LENGTH_SHORT);
                return;
            }
            int currentVol = volume - step;
            if (currentVol < 0) {
                currentVol = 0;
            }
            setVolume(currentVol);
        } else {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        }
    }

    @Override
    public void setPlaymode(JL_PlayMode playmode) {
        if (mJL_MediaPlayer != null)
            mJL_MediaPlayer.setPlayMode(playmode);
    }

    @Override
    public void setNextPlaymode() {
        mJL_MediaPlayer.setNextPlayMode();
    }

    @Override
    public void setPlayControlCallback(PlayControlCallback callback) {
        mPlayControlCallback = callback;
    }

    @Override
    public void refresh() {
        if (mJL_MediaPlayer == null || mPlayControlCallback == null) return;
        mJL_MediaPlayer.registerMusicPlayerCallback(mJl_mediaPlayerCallback);
        Music music = mJL_MediaPlayer.getCurrentPlayMusic();

        if (music == null) {
            mJL_MediaPlayer.setData(mJL_MediaPlayer.getPhoneMusicList());
            if (mJL_MediaPlayer.getData() != null && mJL_MediaPlayer.getData().size() > 0) {
                music = mJL_MediaPlayer.getData().get(0);
            } else {
                mPlayControlCallback.onFailed("没有可以播放的资源");
                return;
            }
        }

        if (music != null) {
            refreshMusic(music);
            mPlayControlCallback.onPlayStateChange(mJL_MediaPlayer.isPlaying());
            mPlayControlCallback.onPlayModeChange(mJL_MediaPlayer.getCurrentPlayMode());
        }

        if (mJL_MediaPlayer.isPlaying()) {
            HandlerManager.getInstance().getMainHandler().removeCallbacks(requestMusicPositon);
            HandlerManager.getInstance().getMainHandler().post(requestMusicPositon);
        }
        updateVolume();
        isSetVolume = false;
    }

    @Override
    public void release() {
        HandlerManager.getInstance().getMainHandler().removeCallbacks(requestMusicPositon);
        if (mVolumeReceiver != null) {
            context.unregisterReceiver(mVolumeReceiver);
            mVolumeReceiver = null;
        }
        if (audioManager != null) {
            audioManager = null;
        }
        if (mMediaMetadataRetriever != null) {
            try {
                mMediaMetadataRetriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            mMediaMetadataRetriever = null;
        }
        mJL_MediaPlayer = null;

        JL_Log.i(tag, tag + "-LocalPlayControlImpl release-");
    }

    @Override
    public void seekTo(int position) {

        mJL_MediaPlayer.setCurrentPosition(position);
    }

    @Override
    public boolean isPlay() {
        return mJL_MediaPlayer.isPlaying();
    }

    private final JL_MediaPlayerCallback mJl_mediaPlayerCallback = new JL_MediaPlayerCallback() {
        @Override
        public void onMusicPlay() {
            if (mPlayControlCallback == null || mJL_MediaPlayer == null) {
                return;
            }
            HandlerManager.getInstance().getMainHandler().removeCallbacks(requestMusicPositon);
            HandlerManager.getInstance().getMainHandler().post(requestMusicPositon);
            mPlayControlCallback.onPlayStateChange(true);

        }

        @Override
        public void onMusicPause() {
            HandlerManager.getInstance().getMainHandler().removeCallbacks(requestMusicPositon);
            if (mPlayControlCallback == null || mJL_MediaPlayer == null) {
                return;
            }
            mPlayControlCallback.onPlayStateChange(false);
        }

        @Override
        public void onMusicCompletion(Music music) {
            if (mPlayControlCallback != null) {
                mPlayControlCallback.onTimeChange(mJL_MediaPlayer.getCurrentPosition(), mJL_MediaPlayer.getDuration());
            }
        }

        @Override
        public void onCurrentPositionChange(int current, int duration) {
            super.onCurrentPositionChange(current, duration);
            if (onFrontdesk && mPlayControlCallback != null) {
                mPlayControlCallback.onTimeChange(current, duration);
            }
        }

        @Override
        public void onMusicChanged(Music music) {
            refreshMusic(music);
        }

        @Override
        public void onMusicPlayMode(JL_PlayMode mode) {
            super.onMusicPlayMode(mode);
            PreferencesHelper.putIntValue(context.getApplicationContext(), SConstant.MEDIA_PLAY_MODE, mode.getValue());
            if (mPlayControlCallback == null || mJL_MediaPlayer == null) {
                return;
            }
            mPlayControlCallback.onPlayModeChange(mode);

        }

        @Override
        public void onError(int what, String msg) {
            super.onError(what, msg);
            if (mPlayControlCallback != null) {
                mPlayControlCallback.onFailed(msg);
            }
        }
    };


    protected void getLocalMusicCover(final Music music) {
        if (mPlayControlCallback == null || mJL_MediaPlayer == null || music == null) {
            return;
        }
        if (TextUtils.isEmpty(music.getCoverUrl()) && music.getLocal() != 0) {
            mPlayControlCallback.onCoverChange(null);
            return;
        }
        try {
//            mMediaMetadataRetriever.setDataSource(music.getUrl());
            if (TextUtils.isEmpty(music.getUrl())) {
                return;
            }
            mMediaMetadataRetriever.setDataSource(context, Uri.parse(music.getUrl()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        byte[] albumArt = mMediaMetadataRetriever.getEmbeddedPicture();
        if (null != albumArt && albumArt.length > 0) {
            final Bitmap bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
            mPlayControlCallback.onCoverChange(bitmap);
        } else {
            mPlayControlCallback.onCoverChange(null);
        }


    }


    private void refreshMusic(final Music music) {
        if (mPlayControlCallback == null || mJL_MediaPlayer == null) {
            return;
        }
        JL_Log.i(tag, "refreshMusic:" + music.toString());
        mPlayControlCallback.onTitleChange(music.getTitle());
        mPlayControlCallback.onArtistChange(music.getArtist());
        mPlayControlCallback.onDownloadStateChange(music.getDownload() == 1);
        if (mJL_MediaPlayer.getDuration() == 0) {
            mPlayControlCallback.onTimeChange(music.getPosition(), music.getDuration());
        } else if (mJL_MediaPlayer.getDuration() < 36000000) {
            mPlayControlCallback.onTimeChange(mJL_MediaPlayer.getCurrentPosition(), mJL_MediaPlayer.getDuration());
        }
        if (music.getLocal() == 1) {
            getNetMusicCover(music);
        } else {
            getLocalMusicCover(music);
        }
    }

    private void getNetMusicCover(Music music) {
        if (mPlayControlCallback == null) {
            return;
        }
        Glide.with(context).asBitmap().load(music.getCoverUrl()).listener(new RequestListener<Bitmap>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                mPlayControlCallback.onCoverChange(null);
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                mPlayControlCallback.onCoverChange(resource);
                return false;
            }

        }).submit();

    }


    private void updateVolume() {
        if (mPlayControlCallback == null) return;
        int max;
        int current;
        if (mRCSPController.isDeviceConnected()) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
            if (deviceInfo == null) return;
            max = deviceInfo.getMaxVol();
            current = deviceInfo.getVolume();
        } else {
            max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        mPlayControlCallback.onValumeChange(current, max);
    }


    private class VolumeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateVolume();
        }
    }

    @Override
    public void onStart() {
        JL_Log.e(tag, "------onStart--------");
        onFrontdesk = true;
        HandlerManager.getInstance().getMainHandler().removeCallbacks(requestMusicPositon);
        HandlerManager.getInstance().getMainHandler().post(requestMusicPositon);
    }

    @Override
    public void onPause() {
        JL_Log.d(tag, "------onPause--------");
        onFrontdesk = false;
        HandlerManager.getInstance().getMainHandler().removeCallbacks(requestMusicPositon);
    }

}
