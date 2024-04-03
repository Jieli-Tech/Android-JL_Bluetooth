package com.jieli.btsmart.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.jieli.audio.media_player.JL_MediaPlayer;
import com.jieli.audio.media_player.JL_MediaPlayerService;
import com.jieli.audio.media_player.Music;
import com.jieli.audio.media_player.MusicObserver;
import com.jieli.bluetooth.utils.JL_Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chensenhua on 2018/5/28.
 */


public class JL_MediaPlayerServiceManager {

    private static volatile JL_MediaPlayerServiceManager mInstance;
    private MyServiceConnection mServiceConnection;
    private boolean bind;
    private JL_MediaPlayer mJl_mediaPlayer;
    private final List<OnBindStateChangeListener> listeners = new ArrayList<>();

    public JL_MediaPlayer getJl_mediaPlayer() {
        if (mJl_mediaPlayer == null) {
            mJl_mediaPlayer = JL_MediaPlayer.instantiate(AppUtil.getContext());
        }
        return mJl_mediaPlayer;
    }

    public void registerOnBindStateChangeListener(OnBindStateChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterOnBindStateChangeListener(OnBindStateChangeListener listener) {
        listeners.remove(listener);

    }

    private JL_MediaPlayerServiceManager() {
    }

    public static JL_MediaPlayerServiceManager getInstance() {
        if (mInstance == null) {
            synchronized (JL_MediaPlayerServiceManager.class) {
                if (mInstance == null) {
                    mInstance = new JL_MediaPlayerServiceManager();
                }
            }
        }
        return mInstance;
    }




    public void bindService() {
        if (!bind) {
            Intent intent = new Intent(AppUtil.getContext(), JL_MediaPlayerService.class);
            if (mServiceConnection == null) {
                mServiceConnection = new MyServiceConnection();
            }
            AppUtil.getContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void unbindService() {
        if (mServiceConnection != null && bind) {
            AppUtil.getContext().unbindService(mServiceConnection);
            mServiceConnection = null;
            bind = false;
        }
    }

    public void refreshLoadLocalMusic(){
          mJl_mediaPlayer.refreshLoadLocalMusic();
    }

    public List<Music> getLocalMusic(){
        return  mJl_mediaPlayer.getPhoneMusicList();
    }

    public Music getCurrentPlayMusic(){
        return mJl_mediaPlayer.getCurrentPlayMusic();
    }

    public void registerMusicObserver(MusicObserver musicObserver){
        mJl_mediaPlayer.registerMusicObserver(musicObserver);
    }

    public void unregisterMusicObserver(MusicObserver musicObserver){
        mJl_mediaPlayer.unregisterMusicObserver(musicObserver);
    }

    public void  play(int pos){
        mJl_mediaPlayer.play(pos);
    }



    private class MyServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            JL_Log.e("zzc", "-onServiceConnected- ok......");
            bind = true;
            mJl_mediaPlayer = ((JL_MediaPlayerService.LocalBinder) service).getService();
            for (OnBindStateChangeListener listener : listeners) {
                listener.onSuccess(mJl_mediaPlayer, componentName);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bind = false;
            for (OnBindStateChangeListener listener : listeners) {
                listener.onFailed(componentName);
            }
        }
    }

    public interface OnBindStateChangeListener {
        void onSuccess(JL_MediaPlayer jl_mediaPlayer, ComponentName componentName);

        void onFailed(ComponentName componentName);
    }
}
