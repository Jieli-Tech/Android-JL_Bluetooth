package com.jieli.btsmart.demo.bluetooth.ble.model;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.demo.bluetooth.ble.SendBleDataThread;
import com.jieli.btsmart.demo.bluetooth.ble.interfaces.IBleOp;
import com.jieli.btsmart.demo.bluetooth.ble.interfaces.OnThreadStateListener;
import com.jieli.btsmart.demo.bluetooth.ble.interfaces.OnWriteDataCallback;
import com.jieli.jl_bt_ota.constant.BluetoothConstant;
import com.jieli.jl_bt_ota.util.CHexConver;

import java.util.UUID;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc BLE设备
 * @since 2022/12/5
 */
public class BleDevice {
    private final String tag = "BleManager";

    private final static int MAX_RETRY_CONNECT_COUNT = 1;//最大尝试连接次数

    /**
     * 上下文
     */
    @NonNull
    private final Context context;
    /**
     * 蓝牙设备类型
     */
    private final BluetoothDevice device;
    /**
     * 重连次数限制
     */
    private final int reconnectLimit;
    /**
     * 连接状态
     */
    private int connection = BluetoothProfile.STATE_DISCONNECTED;
    /**
     * GATT对象
     */
    private BluetoothGatt gatt;
    /**
     * 协商后的MTU值
     */
    private int mtu = BluetoothConstant.BLE_MTU_MIN;
    /**
     * 连接时间戳
     */
    private long connectedTime;
    /**
     * 发送数据线程
     */
    private SendBleDataThread sendDataThread;
    /**
     * 重连次数
     */
    private int reconnectCount = 0;
    /**
     * 是否需要主动回连
     */
    private boolean isNeedReconnect;

    public BleDevice(@NonNull Context context, @NonNull BluetoothDevice device) {
        this(context, device, MAX_RETRY_CONNECT_COUNT);
    }

    public BleDevice(@NonNull Context context, @NonNull BluetoothDevice device, int reconnectLimit) {
        this.context = context;
        this.device = device;
        this.reconnectLimit = reconnectLimit;
    }

    @NonNull
    public BluetoothDevice getDevice() {
        return device;
    }

    public int getConnection() {
        return connection;
    }

    public BleDevice setConnection(int connection) {
        this.connection = connection;
        return this;
    }

    public BluetoothGatt getGatt() {
        return gatt;
    }

    public BleDevice setGatt(BluetoothGatt gatt) {
        this.gatt = gatt;
        return this;
    }

    public int getMtu() {
        int realMtu = mtu;
        if (realMtu > 128) {
            realMtu -= 6;
        }
        return realMtu;
    }

    public BleDevice setMtu(int mtu) {
        this.mtu = mtu;
        return this;
    }

    public long getConnectedTime() {
        return connectedTime;
    }

    public BleDevice setConnectedTime(long connectedTime) {
        this.connectedTime = connectedTime;
        return this;
    }

    public boolean isOverReconnectLimit() {
        reconnectCount++;
        return reconnectCount > reconnectLimit;
    }

    public boolean isNeedReconnect() {
        return isNeedReconnect;
    }

    public BleDevice setNeedReconnect(boolean needReconnect) {
        isNeedReconnect = needReconnect;
        return this;
    }

    public void startSendDataThread() {
        if (sendDataThread == null || !sendDataThread.isRunning()) {
            sendDataThread = new SendBleDataThread(new IBleOp() {
                @Override
                public int getBleMtu() {
                    return getMtu();
                }

                @Override
                public boolean writeDataByBle(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID, byte[] data) {
                    return writeDataToDeviceByBle(gatt, serviceUUID, characteristicUUID, data);
                }
            }, new OnThreadStateListener() {
                @Override
                public void onStart(long id, String name) {

                }

                @Override
                public void onEnd(long id, String name) {
                    if (sendDataThread != null && sendDataThread.getId() == id) {
                        sendDataThread = null;
                    }
                }
            });
            sendDataThread.start();
        }
    }

    public void stopSendDataThread() {
        if (sendDataThread != null) {
            sendDataThread.stopThread();
        }
    }

    public void wakeupSendThread(SendBleDataThread.BleSendTask task) {
        if (sendDataThread != null && task != null && gatt.equals(task.getBleGatt())) {
            sendDataThread.wakeupSendThread(task);
        }
    }

    public boolean addSendTask(UUID serviceUUID, UUID characteristicUUID, byte[] data, OnWriteDataCallback callback) {
        boolean ret = false;
        if (sendDataThread != null && sendDataThread.isRunning()) {
            ret = sendDataThread.addSendTask(gatt, serviceUUID, characteristicUUID, data, callback);
        }
        return ret;
    }

    @SuppressLint("MissingPermission")
    private boolean writeDataToDeviceByBle(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID, byte[] data) {
        if (gatt == null || null == serviceUUID || null == characteristicUUID || null == data || data.length == 0
                || !CommonUtil.checkHasConnectPermission(this.context)) {
            JL_Log.d(tag, "writeDataByBle", "param is invalid.");
            return false;
        }
        BluetoothGattService gattService = gatt.getService(serviceUUID);
        if (null == gattService) {
            JL_Log.d(tag, "writeDataByBle", "service is null.");
            return false;
        }
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristicUUID);
        if (null == gattCharacteristic) {
            JL_Log.d(tag, "writeDataByBle", "characteristic is null");
            return false;
        }
        boolean ret = false;
        try {
            gattCharacteristic.setValue(data);
            ret = gatt.writeCharacteristic(gattCharacteristic);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JL_Log.d(tag, "writeDataByBle", ret + ", data = " + CHexConver.byte2HexStr(data));
        return ret;
    }

    @Override
    public String toString() {
        return "BleDevice{" +
                "device=" + device +
                ", reconnectLimit=" + reconnectLimit +
                ", connection=" + connection +
                ", gatt=" + gatt +
                ", mtu=" + mtu +
                ", connectedTime=" + connectedTime +
                ", sendDataThread=" + sendDataThread +
                ", reconnectCount=" + reconnectCount +
                ", isNeedReconnect=" + isNeedReconnect +
                '}';
    }
}
