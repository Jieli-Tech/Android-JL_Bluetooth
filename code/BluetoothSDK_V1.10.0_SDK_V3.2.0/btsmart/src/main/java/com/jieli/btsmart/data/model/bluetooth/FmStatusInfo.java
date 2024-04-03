package com.jieli.btsmart.data.model.bluetooth;

import androidx.annotation.NonNull;

/**
 * FM状态信息
 */
public class FmStatusInfo {
    private boolean isPlay; //是否正在播放

    private int channel; //频点
    private float freq;   //频率

    private int mode;  //模式

    public FmStatusInfo() {

    }

    public FmStatusInfo(boolean isPlay, int channel, float freq, int mode) {
        setMode(mode);
        setChannel(channel);
        setFreq(freq);
        setPlay(isPlay);
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }

    public boolean isPlay() {
        return isPlay;
    }

    public void setPlay(boolean play) {
        isPlay = play;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public float getFreq() {
        return freq;
    }

    public void setFreq(float freq) {
        this.freq = freq;
    }

    @NonNull
    @Override
    public String toString() {
        return "FmStatusInfo{" +
                "isPlay=" + isPlay +
                ", channel=" + channel +
                ", freq=" + freq +
                ", mode=" + mode +
                '}';
    }
}
