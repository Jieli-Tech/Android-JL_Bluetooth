package com.jieli.btsmart.data.model.bluetooth;

import java.util.Arrays;
import java.util.List;

/**
 * @author : zpc18-003
 * @e-mail :
 * @date : 2020/6/17 11:02 AM
 * @desc :
 */
public class EqPresetInfo {
    private int number;

    private List<EqInfo> eqInfos;
    private  int [] freqs;


    public void setFreqs(int[] freqs) {
        this.freqs = freqs;
    }

    public int[] getFreqs() {
        return freqs;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public List<EqInfo> getEqInfos() {
        return eqInfos;
    }

    public void setEqInfos(List<EqInfo> eqInfos) {
        this.eqInfos = eqInfos;
    }

    @Override
    public String toString() {
        return "EqPresetInfo{" +
                "number=" + number +
                ", eqInfos=" + eqInfos +
                ", freqs=" + Arrays.toString(freqs) +
                '}';
    }
}
