package com.jieli.btsmart.data.model.bluetooth;

import androidx.annotation.NonNull;

/**
 * 播放器模式信息
 */
public class PlayModeInfo {
    private int playMode; //播放模式

    public PlayModeInfo(int playMode) {
        setPlayMode(playMode);
    }

    public int getPlayMode() {
        return playMode;
    }

    public void setPlayMode(int playMode) {
        this.playMode = playMode;
    }
    @NonNull
    @Override
    public String toString() {
        return "PlayModeInfo{" +
                "playMode=" + playMode +
                '}';
    }
}
