package com.jieli.btsmart.demo.bluetooth.ble.model;


import android.os.Parcel;
import android.os.Parcelable;

import com.jieli.jl_bt_ota.util.CHexConver;

/**
 * BLE 设备扫描信息
 * <p>
 * Created by zqjasonzhong on 2018/10/17.
 */

public class BleScanInfo implements Parcelable {
    /**
     * 原始数据
     */
    private byte[] rawData;
    /**
     * 信号强度
     */
    private int rssi;
    /**
     * 是否允许连接
     * <p>说明: 默认允许连接，特殊情况不允许</p>
     */
    private boolean isEnableConnect = true;

    public BleScanInfo() {

    }

    protected BleScanInfo(Parcel in) {
        rawData = in.createByteArray();
        rssi = in.readInt();
        isEnableConnect = in.readByte() != 0;
    }

    public static final Creator<BleScanInfo> CREATOR = new Creator<BleScanInfo>() {
        @Override
        public BleScanInfo createFromParcel(Parcel in) {
            return new BleScanInfo(in);
        }

        @Override
        public BleScanInfo[] newArray(int size) {
            return new BleScanInfo[size];
        }
    };

    public byte[] getRawData() {
        return rawData;
    }

    public BleScanInfo setRawData(byte[] rawData) {
        this.rawData = rawData;
        return this;
    }

    public int getRssi() {
        return rssi;
    }

    public BleScanInfo setRssi(int rssi) {
        this.rssi = rssi;
        return this;
    }

    public boolean isEnableConnect() {
        return isEnableConnect;
    }

    public BleScanInfo setEnableConnect(boolean enableConnect) {
        isEnableConnect = enableConnect;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(rawData);
        dest.writeInt(rssi);
        dest.writeByte((byte) (isEnableConnect ? 1 : 0));
    }

    @Override
    public String toString() {
        return "BleScanMessage{" +
                "rawData=" + CHexConver.byte2HexStr(rawData) +
                ", rssi=" + rssi +
                ", isEnableConnect=" + isEnableConnect +
                '}';
    }
}
