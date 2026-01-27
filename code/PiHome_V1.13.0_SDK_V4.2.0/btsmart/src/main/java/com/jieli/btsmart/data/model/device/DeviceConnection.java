package com.jieli.btsmart.data.model.device;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 设备连接
 * @since 2023/8/23
 */
public class DeviceConnection {
    /**
     * 蓝牙设备
     */
    @NonNull
    final BluetoothDevice device;
    /**
     * 连接状态
     */
    final int status;

    public DeviceConnection(@NonNull BluetoothDevice device, int status) {
        this.device = device;
        this.status = status;
    }

    @NonNull
    public BluetoothDevice getDevice() {
        return device;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "DeviceConnection{" +
                "device=" + device +
                ", status=" + status +
                '}';
    }
}
