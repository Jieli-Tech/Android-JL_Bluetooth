package com.jieli.btsmart.data.model.eq;

public class EqSeekBarBean {
    private String freq;
    private int index;
    private int value;

    public EqSeekBarBean() {
    }

    public EqSeekBarBean(int index,String freq, int value) {
        this.freq = freq;
        this.value = value;
        this.index=index;
    }

    public String getFreq() {
        return freq;
    }

    public void setFreq(String freq) {
        this.freq = freq;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
