package com.jieli.btsmart.data.model.bluetooth;

import com.jieli.bluetooth.constant.BluetoothConstant;

import java.util.Arrays;

/**
 * 均衡器信息
 */
public class EqInfo {

    private int mode;  //eq模式
    private byte[] value = new byte[10]; //db数值(10 bytes)
    private int[] freqs = BluetoothConstant.DEFAULT_EQ_FREQS;
    private boolean dynamic;
    private int count = 10;



    public EqInfo() {

    }

    public EqInfo(int mode, byte[] value) {
        setMode(mode);
        setValue(value);
    }

    public EqInfo(int mode, byte[] value, int[] freqs) {
        setMode(mode);
        setValue(value);
        setFreqs(freqs);
    }


    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public boolean isDynamic() {
        return dynamic;
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


    public void setFreqs(int[] freqs) {
        this.freqs = freqs;
        count = freqs.length;
    }

    public int[] getFreqs() {
        return freqs;
    }

    public EqInfo copy() {
        EqInfo eqInfo = new EqInfo(this.mode, this.value, this.getFreqs());
        eqInfo.count=this.count;
        eqInfo.setDynamic(this.dynamic);
        return eqInfo;
    }


    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "EqInfo{" +
                "mode=" + mode +
                ", isNew=" + dynamic +
                ", value=" + Arrays.toString(value) +
                ", freqs=" + Arrays.toString(freqs) +
                '}';
    }
}
