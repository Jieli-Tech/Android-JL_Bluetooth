package com.jieli.btsmart.data.model.device;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.constant.StateCode;

/**
 * 扫描设备数据
 *
 * @author zqjasonZhong
 * @since 2020/5/20
 */
public class ScanBtDevice {
    private final int deviceType;
    private BleScanMessage mBleScanMessage;
    @NonNull
    private final BluetoothDevice device;
    private int connection = StateCode.CONNECTION_DISCONNECT;

    public ScanBtDevice(@NonNull BluetoothDevice device, int deviceType, BleScanMessage bleScanMessage) {
        this.device = device;
        this.deviceType = deviceType;
        setBleScanMessage(bleScanMessage);
    }

    public int getDeviceType() {
        return deviceType;
    }

    @NonNull
    public BluetoothDevice getDevice() {
        return device;
    }

    public BleScanMessage getBleScanMessage() {
        return mBleScanMessage;
    }

    public ScanBtDevice setBleScanMessage(BleScanMessage bleScanMessage) {
        mBleScanMessage = bleScanMessage;
        return this;
    }

    public int getConnection() {
        return connection;
    }

    public ScanBtDevice setConnection(int connection) {
        this.connection = connection;
        return this;
    }

    @Override
    public String toString() {
        return "ScanBtDevice{" +
                "deviceType=" + deviceType +
                ", mBleScanMessage=" + mBleScanMessage +
                ", device=" + device +
                ", connection=" + connection +
                '}';
    }
}
