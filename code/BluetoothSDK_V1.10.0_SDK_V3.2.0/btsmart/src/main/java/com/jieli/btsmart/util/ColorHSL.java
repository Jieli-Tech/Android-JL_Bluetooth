package com.jieli.btsmart.util;

public class ColorHSL {
    float hue;
    float saturation;
    float luminance;

    public ColorHSL() {

    }

    public ColorHSL(float hue, float saturation, float luminance) {
        this.hue = hue;
        this.saturation = saturation;
        this.luminance = luminance;
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
