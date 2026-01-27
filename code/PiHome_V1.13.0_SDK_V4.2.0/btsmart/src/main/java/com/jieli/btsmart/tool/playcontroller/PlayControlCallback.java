package com.jieli.btsmart.tool.playcontroller;

import android.graphics.Bitmap;

import com.jieli.audio.media_player.JL_PlayMode;

/**
 * Created by chensenhua on 2018/1/17.
 */

public abstract class PlayControlCallback {

    public void onTitleChange(String title) {
    }

    public void onArtistChange(String name) {
    }


    public void onPlayStateChange(boolean isPlay) {
    }

    public void onPlayModeChange(JL_PlayMode mode) {
    }

    public void onTimeChange(int current, int total) {
    }

    public void onValumeChange(int current, int max) {
    }

    public void onCoverChange(Bitmap cover) {
    }

    public void onFailed(String msg) {
    }

    public void onDownloadStateChange(boolean state) {
    }

    /**
     * @param mode 0:local, 1:tfMode 3:linein
     */
    public void onModeChange(int mode) {
    }

}
