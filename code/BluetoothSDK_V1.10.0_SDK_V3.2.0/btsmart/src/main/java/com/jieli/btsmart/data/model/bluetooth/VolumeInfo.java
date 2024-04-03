package com.jieli.btsmart.data.model.bluetooth;

import androidx.annotation.NonNull;

/**
 * 音量信息
 */
public class VolumeInfo {
    private int maxVol;  //最大音量
    private int volume; //当前音量
    private boolean supportVolumeSync=false;

    public VolumeInfo() {
        this(0, 0);
    }

    public VolumeInfo(int volume) {
        this(0, volume);
    }

    public VolumeInfo(int max, int volume) {
        setMaxVol(max);
        setVolume(volume);
    }

    public VolumeInfo(int max, int volume,boolean supportVolumeSync) {
        setMaxVol(max);
        setVolume(volume);
        setSupportVolumeSync(supportVolumeSync);
    }

    public int getMaxVol() {
        return maxVol;
    }

    public void setMaxVol(int maxVol) {
        this.maxVol = maxVol;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public void setSupportVolumeSync(boolean supportVolumeSync) {
        this.supportVolumeSync = supportVolumeSync;
    }

    public boolean isSupportVolumeSync() {
        return supportVolumeSync;
    }

    @NonNull
    @Override
    public String toString() {
        return "VolumeInfo{" +
                "maxVol=" + maxVol +
                ", volume=" + volume +
                ", supportVolumeSync=" + supportVolumeSync +
                '}';
    }
}
