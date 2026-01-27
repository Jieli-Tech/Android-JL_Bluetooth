package com.jieli.btsmart.demo.bluetooth.ble.interfaces;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

/**
 * 写数据的结果回调
 *
 * @author zqjasonZhong
 * @date 2019/9/25
 */
public interface OnWriteDataCallback {

    /**
     * 回调BLE发送结果
     *
     * @param device             蓝牙设备
     * @param serviceUUID        服务UUID
     * @param characteristicUUID 特征值UUID
     * @param result             发送结果
     * @param data               发送数据
     */
    void onBleResult(BluetoothDevice device, UUID serviceUUID, UUID characteristicUUID, boolean result, byte[] data);
}
