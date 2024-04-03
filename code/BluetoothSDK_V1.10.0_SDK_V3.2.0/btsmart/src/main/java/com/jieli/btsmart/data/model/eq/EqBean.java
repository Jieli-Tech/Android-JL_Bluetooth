package com.jieli.btsmart.data.model.eq;

public class EqBean {
    private String name;
    private int mode;
    private byte [] value;
    private float [] freqs= new float[]{31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};

    public void setFreqs(float[] freqs) {
        this.freqs = freqs;
    }

    public float[] getFreqs() {
        return freqs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }
}
