package com.jieli.btsmart.demo.bluetooth.ble.interfaces;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;

import com.jieli.btsmart.demo.bluetooth.ble.model.BleScanInfo;

import java.util.List;
import java.util.UUID;

/**
 * Ble事件回调
 *
 * @author zqjasonZhong
 * @since 2020/7/16
 */
public interface IBleEventCallback {

    /**
     * 蓝牙适配器开关回调
     *
     * @param bEnabled 开关
     */
    void onAdapterChange(boolean bEnabled);

    /**
     * 搜索蓝牙设备的状态回调
     *
     * @param bStart 搜索状态
     */
    void onDiscoveryBleChange(boolean bStart);

    /**
     * 发现蓝牙设备的回调
     *
     * @param device      蓝牙设备对象
     * @param bleScanInfo BLE扫描数据
     */
    void onDiscoveryBle(BluetoothDevice device, BleScanInfo bleScanInfo);

    /**
     * BLE连接状态回调
     *
     * @param device 蓝牙设备对象
     * @param status 连接状态
     */
    void onBleConnection(BluetoothDevice device, int status);

    /**
     * BLE服务发现回调
     *
     * @param device   蓝牙设备对象
     * @param status   状态
     * @param services 服务列表
     */
    void onBleServiceDiscovery(BluetoothDevice device, int status, List<BluetoothGattService> services);

    /**
     * BLE特征状态回调
     *
     * @param device             蓝牙设备对象
     * @param serviceUuid        服务UUID
     * @param characteristicUuid 特征值UUID
     * @param status             状态
     */
    void onBleNotificationStatus(BluetoothDevice device, UUID serviceUuid, UUID characteristicUuid, int status);

    /**
     * BLE MTU改变回调
     *
     * @param device 蓝牙设备对象
     * @param block  协商后的MTU
     * @param status 状态
     */
    void onBleDataBlockChanged(BluetoothDevice device, int block, int status);

    /**
     * BLE数据回调
     *
     * @param device              蓝牙设备对象
     * @param serviceUuid         服务UUID
     * @param characteristicsUuid 特征值UUID
     * @param data                数据
     */
    void onBleDataNotification(BluetoothDevice device, UUID serviceUuid, UUID characteristicsUuid, byte[] data);

    /**
     * BLE 写数据回调
     *
     * @param device              蓝牙设备对象
     * @param serviceUuid         服务UUID
     * @param characteristicsUuid 特征值UUID
     * @param data                数据
     * @param status              状态
     */
    void onBleWriteStatus(BluetoothDevice device, UUID serviceUuid, UUID characteristicsUuid, byte[] data, int status);

    /**
     * BLE 连接参数回调
     *
     * @param device   蓝牙设备对象
     * @param interval 发数间隔
     * @param latency  延时参数
     * @param timeout  超时时间
     * @param status   状态
     */
    void onConnectionUpdated(BluetoothDevice device, int interval, int latency, int timeout, int status);
}
