package com.jieli.btsmart.tool.playcontroller;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.jieli.audio.media_player.AudioFocusManager;
import com.jieli.audio.media_player.JL_PlayMode;
import com.jieli.audio.media_player.Music;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.util.AppUtil;

import java.util.List;

import static com.google.android.exoplayer2.Player.TIMELINE_CHANGE_REASON_DYNAMIC;
import static com.google.android.exoplayer2.Player.TIMELINE_CHANGE_REASON_PREPARED;
import static com.google.android.exoplayer2.Player.TIMELINE_CHANGE_REASON_RESET;


/**
 * Created by chensenhua on 2018/1/17.
 */

public class NetRadioPlayControlImpl implements PlayControl {
    private static final String tag = NetRadioPlayControlImpl.class.getSimpleName();
    private PlayControlCallback mPlayControlCallback;
    private final Context context;
    private SimpleExoPlayer player;
    private DefaultBandwidthMeter BANDWIDTH_METER;
    private List<Music> mPlayList;
    private Music mCurrentPlayMusic;
    private int mPlayPosition = -1;
    private static NetRadioPlayControlImpl mInstance;
    private boolean mIsBuffering = false;
    private boolean isPlay = false;

    public NetRadioPlayControlImpl(Context context) {
        JL_Log.i(tag, tag + "-NetRadioPlayControlImpl init-");
        this.context = context.getApplicationContext();
        TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this.context, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
        player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
        player.addListener(eventListener);
        AudioFocusManager.getInstance().registerOnAudioFocusChangeCallback(mOnAudioFocusChangeCallback);
    }

    public static NetRadioPlayControlImpl getInstance(Context context) {
        if (mInstance == null) {
            synchronized (NetRadioPlayControlImpl.class) {
                if (mInstance == null) {
                    mInstance = new NetRadioPlayControlImpl(context);
                }
            }
        }
        return mInstance;
    }

    @Override
    public void onStart() {
        JL_Log.i(tag, "------onStart--------");
    }

    @Override
    public void onPause() {
//        player.stop();
        JL_Log.i(tag, "------onPause--------");
    }

    public void setPlayList(List<Music> musicList) {
        mPlayList = musicList;
        if (mPlayList != null && mPlayList.size() == 0) {//播放列表为空的时候清空当前播放
            Music nullMusic = new Music();
            nullMusic.setTitle(null);
            nullMusic.setArtist(null);
            nullMusic.setDownload(1);
            refreshMusic(nullMusic);
            player.stop();
            mPlayPosition = -1;
            pause();
        }
    }

    public void play(int position) {
        playRadio(position);
    }

    /**
     * 设置当前播放器指向位置
     *
     * @param
     * @return
     * @description 描述一下方法的作用
     */
    public void setPlayPosition(int position) {
        if (position < 0 || (position >= mPlayList.size())) return;
        prepareMediaSource(position);
        refreshMusic(mPlayList.get(position));
        mPlayPosition = position;
    }

    private void playRadio(int position) {
        if (position < 0 || position >= mPlayList.size()) return;
        player.setPlayWhenReady(true);
        if (!AudioFocusManager.getInstance().isHasAudioFocus()) {
            AudioFocusManager.getInstance().requestAudioFocus();
        }
        setPlayPosition(position);
    }

    private void prepareMediaSource(int position) {
        if (mPlayList == null || mPlayList.size() == 0) return;
        MediaSource mediaSource;
        String userAgent = Util.getUserAgent(this.context, "ExoPlayerDemo");
        mCurrentPlayMusic = mPlayList.get(position);
        String fileName = mPlayList.get(position).getUrl();
        Uri uri = Uri.parse(fileName);
        if (fileName.endsWith(".m3u8")) {
            DefaultDataSourceFactory mediaDataSourceFactory = new DefaultDataSourceFactory(this.context, BANDWIDTH_METER,
                    new DefaultHttpDataSourceFactory(userAgent, BANDWIDTH_METER));
            mediaSource = new HlsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri);
        } else {
            DefaultDataSourceFactory mediaDataSourceFactory = new DefaultDataSourceFactory(this.context, userAgent);
            mediaSource = new ExtractorMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri);
        }
        player.prepare(mediaSource);
    }


    @Override
    public void playNext() {
        JL_Log.i(tag, "playNext");
        playNextMusic();
    }

    @Override
    public void playPre() {
        JL_Log.i(tag, "playPre");
        playPreMusic();
    }

    @Override
    public void play() {
        JL_Log.i(tag, tag + "-play-");
        if (player != null) {
            player.setPlayWhenReady(true);
            player.getPlaybackState();
            if (!AudioFocusManager.getInstance().isHasAudioFocus()) {
                AudioFocusManager.getInstance().requestAudioFocus();
            }
        }
    }

    @Override
    public void pause() {
        if (player != null) {
            JL_Log.i("zzc", tag + "-pause-");
            player.setPlayWhenReady(false);
            player.getPlaybackState();
        }
    }


    @Override
    public void playOrPause() {
        if (mPlayPosition == -1) return;
        player.setPlayWhenReady(!player.getPlayWhenReady());
    }

    @Override
    public void setVolume(final int value) {
    }

    @Override
    public void volumeUp() {
    }

    @Override
    public void volumeUpByHand() {
    }

    @Override
    public void volumeDown() {
    }

    @Override
    public void volumeDownByHand() {
    }

    @Override
    public void setPlaymode(JL_PlayMode playmode) {
    }

    @Override
    public void setNextPlaymode() {
    }

    @Override
    public void setPlayControlCallback(PlayControlCallback callback) {
        mPlayControlCallback = callback;
    }

    @Override
    public void refresh() {
        JL_Log.d(tag, "  refresh() ");
        if (player == null || mPlayControlCallback == null || mPlayList == null || mPlayList.size() == 0)
            return;
        Music music = mPlayList.get(mPlayPosition);
        if (music == null) {
            mPlayControlCallback.onFailed("没有可以播放的资源");
            if (player.getPlayWhenReady()) {//如果当前处于播放状态.则暂停
                pause();
            }
            return;
        }
        refreshMusic(music);
        mPlayControlCallback.onPlayStateChange(player.getPlayWhenReady());
    }

    @Override
    public void release() {
        if (player != null) {
            player.release();
        }
        AudioFocusManager.getInstance().unregisterOnAudioFocusChangeCallback(mOnAudioFocusChangeCallback);
        JL_Log.i(tag, tag + "-LocalPlayControlImpl release-");
    }

    @Override
    public void seekTo(int position) {
    }

    @Override
    public boolean isPlay() {
        return player.getPlayWhenReady();
    }

    public boolean isBuffering() {
        return mIsBuffering;
    }

    /**
     * 播放下一首
     *
     * @param
     * @return
     * @description
     */
    private void playNextMusic() {
        if (player != null && mPlayList != null) {
            int position;
            if (mPlayPosition == (mPlayList.size() - 1)) {
                position = 0;
            } else {
                position = mPlayPosition + 1;
            }
            if (mPlayPosition == position) return;
            playRadio(position);
        }
    }

    /**
     * 播放上一首
     *
     * @param
     * @return
     * @description 描述一下方法的作用
     */
    private void playPreMusic() {
        if (player != null && mPlayList != null) {
            int position;
            if (mPlayPosition == 0) {
                position = mPlayList.size() - 1;
            } else {
                position = mPlayPosition - 1;
            }
            if (mPlayPosition == position) return;
            playRadio(position);
        }
    }

    private void refreshMusic(final Music music) {
        if (mPlayControlCallback == null || player == null) {
            return;
        }
        JL_Log.i(tag, "refreshMusic:" + music.toString());
        mPlayControlCallback.onTitleChange(music.getTitle());
        mPlayControlCallback.onArtistChange(music.getArtist());
        mPlayControlCallback.onDownloadStateChange(music.getDownload() == 1);
/*        if (music.getLocal() == 4) {
            getNetMusicCover(music);
        }*/
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

    private final Player.EventListener eventListener = new Player.EventListener() {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
            JL_Log.i(tag, "onTimelineChanged  Timeline:  " + timeline + "   manifest:  " + manifest + "  reason:   " + reason);
            switch (reason) {
                case TIMELINE_CHANGE_REASON_PREPARED:
                    break;
                case TIMELINE_CHANGE_REASON_RESET:
                    break;
                case TIMELINE_CHANGE_REASON_DYNAMIC:
                    break;
            }
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            JL_Log.i(tag, "onTracksChanged:  TrackGroupArray:   " + trackGroups.toString() + "   TrackSelectionArray  " + trackSelections.toString());
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {//加载资源改变，开始或者停止加载资源
            JL_Log.i(tag, "onLoadingChanged: " + isLoading);
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            JL_Log.i(tag, "onPlayerStateChanged:  playWhenReady:  " + playWhenReady + "  playbackState:  " + playbackState);
            if (mPlayControlCallback != null) {
                if (isPlay != playWhenReady) {
                    isPlay = playWhenReady;
                    mPlayControlCallback.onPlayStateChange(playWhenReady);
                }
            }
            switch (playbackState) {
                case Player.STATE_IDLE://没有播放媒体
                    if (playWhenReady && mPlayPosition >= 0) {//播放的时候没有网络资源，需要加载
                        prepareMediaSource(mPlayPosition);
                    }
                    break;
                case Player.STATE_ENDED://已经完成播放
                    mIsBuffering = false;
                    break;
                case Player.STATE_BUFFERING://正在缓存
                    mIsBuffering = true;
                    break;
                case Player.STATE_READY://准备就绪
                    mIsBuffering = false;
                    break;
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            JL_Log.i(tag, "   onRepeatModeChanged  ：" + repeatMode);
        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {//是否启用随机播放
            JL_Log.i(tag, "onShuffleModeEnabledChanged:  shuffleModeEnabled:" + shuffleModeEnabled);
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            /**播放电台异常的时候*/
            JL_Log.e(tag, "onPlayerError: " + error.toString());
            if (isPlay) {
                player.setPlayWhenReady(false);
                if (mPlayControlCallback != null) {
                    mPlayControlCallback.onFailed(AppUtil.getContext().getString(R.string.resource_not_available));
                }
            }
        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            JL_Log.i(tag, "onPositionDiscontinuity:  reason:  " + reason);
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            JL_Log.i(tag, "onPlaybackParametersChanged:   PlaybackParameters:  " + playbackParameters);
        }

        @Override
        public void onSeekProcessed() {
            JL_Log.i(tag, "onSeekProcessed: ");
        }
    };

    private final AudioFocusManager.OnAudioFocusChangeCallback mOnAudioFocusChangeCallback = new AudioFocusManager.OnAudioFocusChangeCallback() {
        @Override
        public void onAudioFocusLossTransient() {

        }

        @Override
        public void onAudioFocusGain() {

        }

        @Override
        public void onAudioFocusLossTransientCanDuck() {

        }

        @Override
        public void onAudioLoss() {
            if (player.getPlayWhenReady() && mIsBuffering) {//焦点丢失的时候，正准备播放且还在缓存资源的过程中。这个时候耳机的MediaButton会被第三方的播放器响应从而被抢走焦点
                playRadio(mPlayPosition);
            } else {
                pause();
            }
        }
    };

}
