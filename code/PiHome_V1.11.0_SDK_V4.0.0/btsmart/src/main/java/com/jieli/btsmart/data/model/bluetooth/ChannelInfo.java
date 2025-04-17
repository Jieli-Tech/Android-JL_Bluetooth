package com.jieli.btsmart.data.model.bluetooth;

import androidx.annotation.NonNull;

public class ChannelInfo {
    /**
     * 序号
     */
    private int index;
    /**
     * 频点
     */
    private float freq;

    public ChannelInfo() {

    }

    public ChannelInfo(int index, float freq) {
        setIndex(index);
        setFreq(freq);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
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
        return "ChannelInfo{" +
                "index=" + index +
                ", freq=" + freq +
                '}';
    }
}
