package com.jieli.btsmart.data.model.mydevice;

public class MyDeviceInfoItemData {
    private String mName;//设备名
    private int mDeviceInfoType;//sdkType
    private boolean mConnectState;//连接状态
    private int mProtocolType;//连接方式：ble，SPP
    private String address; //蓝牙设备的地址
    private int mAdvVersion;//广播包版本
    private int pid;
    private int vid;
    private int uid;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public int getmDeviceInfoType() {
        return mDeviceInfoType;
    }

    public void setmDeviceInfoType(int mDeviceInfoType) {
        this.mDeviceInfoType = mDeviceInfoType;
    }

    public int getAdvVersion() {
        return mAdvVersion;
    }

    public void setAdvVersion(int advVersion) {
        mAdvVersion = advVersion;
    }

    public boolean ismConnectState() {
        return mConnectState;
    }

    public void setmConnectState(boolean mConnectState) {
        this.mConnectState = mConnectState;
    }

    public int getmProtocolType() {
        return mProtocolType;
    }

    public void setmProtocolType(int mProtocolType) {
        this.mProtocolType = mProtocolType;
    }

    public int getPid() {
        return pid;
    }

    public int getUid() {
        return uid;
    }

    public int getVid() {
        return vid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public void setVid(int vid) {
        this.vid = vid;
    }
}
