package com.jieli.btsmart.util;

public class ColorHSB {
    float hue;
    float saturation;
    float brightness;

    public ColorHSB() {

    }

    public ColorHSB(float hue, float saturation, float brightness) {
        this.hue = hue;
        this.saturation = saturation;
        this.brightness = brightness;
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

    public float getBrightness() {
        return brightness;
    }

    public void setBrightness(float luminance) {
        this.brightness = luminance;
    }
}
