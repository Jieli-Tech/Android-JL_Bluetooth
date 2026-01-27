package com.jieli.btsmart.data.model.eq;

/**
 * VolumeCtrl
 * @author zqjasonZhong
 * @since 2025/5/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 音量调节
 */
public class VolumeCtrl {
    private int high;
    private int bass;

    public VolumeCtrl() {
        setHigh(0).setBass(0);
    }

    public VolumeCtrl(int high, int bass) {
        this.high = high;
        this.bass = bass;
    }

    public int getHigh() {
        return high;
    }

    public int getBass() {
        return bass;
    }

    public VolumeCtrl setHigh(int high) {
        this.high = high;
        return this;
    }

    public VolumeCtrl setBass(int bass) {
        this.bass = bass;
        return this;
    }

    @Override
    public String toString() {
        return "VolumeCtrl{" +
                "high=" + high +
                ", bass=" + bass +
                '}';
    }
}
