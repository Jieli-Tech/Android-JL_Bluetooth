package com.jieli.btsmart.data.model.bluetooth;

import androidx.annotation.NonNull;

/**
 * 音乐状态信息
 */
public class MusicStatusInfo {
    private boolean isPlay;  //是否播放
    private int currentTime; //当前播放时间(单位: 毫秒)
    private int totalTime; //时间总长(单位: 毫秒)
    private int currentDev; //设备句柄

    public MusicStatusInfo() {
    }

    public MusicStatusInfo(boolean isPlay, int currentTime, int totalTime, int currentDev) {
        setPlay(isPlay);
        setCurrentTime(currentTime);
        setTotalTime(totalTime);
        setCurrentDev(currentDev);
    }

    public boolean isPlay() {
        return isPlay;
    }

    public void setPlay(boolean play) {
        isPlay = play;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(int currentTime) {
        this.currentTime = currentTime;
    }

    public int getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(int totalTime) {
        this.totalTime = totalTime;
    }

    public int getCurrentDev() {
        return currentDev;
    }

    public void setCurrentDev(int currentDev) {
        this.currentDev = currentDev;
    }
    @NonNull
    @Override
    public String toString() {
        return "MusicStatusInfo{" +
                "isPlay=" + isPlay +
                ", currentTime=" + currentTime +
                ", totalTime=" + totalTime +
                ", currentDev=" + currentDev +
                '}';
    }
}
