package com.jieli.btsmart.data.model.bluetooth;

import android.graphics.Color;

import com.jieli.bluetooth.utils.CHexConver;

public class LightControlInfo {
    private int switchState;
    private int lightMode;
    private int color;
    private int twinkleMode;
    private int twinkleFreq;
    private int sceneMode;
    private int hue;
    private int saturation;
    private int luminance;

    public int getSwitchState() {
        return switchState;
    }

    public void setSwitchState(int switchState) {
        this.switchState = switchState;
    }

    public int getLightMode() {
        return lightMode;
    }

    public void setLightMode(int lightMode) {
        this.lightMode = lightMode;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getTwinkleMode() {
        return twinkleMode;
    }

    public void setTwinkleMode(int twinkleMode) {
        this.twinkleMode = twinkleMode;
    }

    public int getTwinkleFreq() {
        return twinkleFreq;
    }

    public void setTwinkleFreq(int twinkleFreq) {
        this.twinkleFreq = twinkleFreq;
    }

    public int getSceneMode() {
        return sceneMode;
    }

    public void setSceneMode(int sceneMode) {
        this.sceneMode = sceneMode;
    }

    public int getHue() {
        return hue;
    }

    public void setHue(int hue) {
        this.hue = hue;
    }

    public int getSaturation() {
        return saturation;
    }

    public void setSaturation(int saturation) {
        this.saturation = saturation;
    }

    public int getLuminance() {
        return luminance;
    }

    public void setLuminance(int luminance) {
        this.luminance = luminance;
    }

    @Override
    public String toString() {
        return "LightControlInfo{" +
                "switchState=" + switchState +
                ", lightMode=" + lightMode +
                ", color=" + color +
                ", twinkleMode=" + twinkleMode +
                ", twinkleFreq=" + twinkleFreq +
                ", hue=" + hue +
                ", saturation=" + saturation +
                ", luminance=" + luminance +
                '}';
    }

    public byte[] toByteArray() {
        byte[] bytes = new byte[11];
        int byte0Value = switchState | (lightMode << 2);
        byte[] bytesHue = CHexConver.int2byte2(hue);
        bytes[0] = CHexConver.intToByte(byte0Value);
        bytes[1] = CHexConver.intToByte(Color.red(color));
        bytes[2] = CHexConver.intToByte(Color.green(color));
        bytes[3] = CHexConver.intToByte(Color.blue(color));
        bytes[4] = CHexConver.intToByte(twinkleMode);
        bytes[5] = CHexConver.intToByte(twinkleFreq);
        bytes[6] = CHexConver.intToByte(sceneMode);
        System.arraycopy(bytesHue, 0, bytes, 7, bytesHue.length);
        bytes[9] = CHexConver.intToByte(saturation);
        bytes[10] = CHexConver.intToByte(luminance);
        return bytes;
    }
}
