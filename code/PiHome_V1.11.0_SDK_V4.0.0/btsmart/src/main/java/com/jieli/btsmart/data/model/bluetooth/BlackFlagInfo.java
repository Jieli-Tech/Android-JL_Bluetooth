package com.jieli.btsmart.data.model.bluetooth;

import java.util.Objects;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/9/30 4:40 PM
 * @desc :
 */
public class BlackFlagInfo {
    private String address;
    private int seq;
    private int repeatTime;

    public BlackFlagInfo(String address, int seq) {
        this.address = address;
        this.seq = seq;
    }

    public BlackFlagInfo() {
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public void setRepeatTime(int repeatTime) {
        this.repeatTime = repeatTime;
    }

    public int getRepeatTime() {
        return repeatTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlackFlagInfo that = (BlackFlagInfo) o;
        return Objects.equals(address, that.address) && seq == that.seq;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, seq);
    }

    @Override
    public String toString() {
        return "BlackFlagInfo{" +
                "address='" + address + '\'' +
                ", seq='" + seq + '\'' +
                ", repeatTime=" + repeatTime +
                '}';
    }
}
