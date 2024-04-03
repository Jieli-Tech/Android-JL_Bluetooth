package com.jieli.btsmart.data.model.light;

/**
 * @author : chensenhua
 * @e-mail :
 * @date : 2020/6/29 11:51 AM
 * @desc :
 */
public class ColorCollect {
    private Integer color = null;
    private long time;
    float hue;
    float saturation;
    float luminance;

    public ColorCollect() {
    }

    public ColorCollect(int color) {
        this.color = color;
    }

    public Integer getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public float getHue() {
        return hue;
    }

    public void setHue(float hue) {
        this.hue = hue;
    }

    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public float getLuminance() {
        return luminance;
    }

    public void setLuminance(float luminance) {
        this.luminance = luminance;
    }
}
