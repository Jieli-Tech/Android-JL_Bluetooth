package com.jieli.btsmart.data.model.device;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;

/**
 * 连接历史记录设备
 *
 * @author zqjasonZhong
 * @since 2020/9/28
 */
public class HistoryDevice {
    public static final int STATE_DISCONNECT = 0;    //设备未连接
    public static final int STATE_CONNECTING = 1;    //设备正在连接
    public static final int STATE_CONNECTED = 2;     //设备已连接
    public static final int STATE_NEED_OTA = 3;      //设备处于需要升级状态
    public static final int STATE_RECONNECT = 4;     //正在回连设备

    private final HistoryBluetoothDevice mDevice;
    private ADVInfoResponse mADVInfo;
    private int state;


    public HistoryDevice(@NonNull HistoryBluetoothDevice device) {
        this(device, null);
    }

    public HistoryDevice(@NonNull HistoryBluetoothDevice device, ADVInfoResponse advInfo) {
        this.mDevice = device;
        setADVInfo(advInfo);
    }

    @NonNull
    public HistoryBluetoothDevice getDevice() {
        return mDevice;
    }

    public ADVInfoResponse getADVInfo() {
        return mADVInfo;
    }

    public HistoryDevice setADVInfo(ADVInfoResponse ADVInfo) {
        mADVInfo = ADVInfo;
        return this;
    }

    public int getState() {
        return state;
    }

    public HistoryDevice setState(int state) {
        this.state = state;
        return this;
    }

    @Override
    public String toString() {
        return "HistoryDevice{" +
                "mDevice=" + mDevice +
                ", mADVInfo=" + mADVInfo +
                ", state=" + state +
                '}';
    }
}
