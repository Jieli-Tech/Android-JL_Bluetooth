package com.jieli.btsmart.demo.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.BluetoothOption;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.impl.rcsp.RcspOpImpl;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.demo.bluetooth.ble.BleManager;
import com.jieli.btsmart.demo.bluetooth.ble.interfaces.BleEventCallback;

import java.util.UUID;

/**
 * initDemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 初始化示例
 * @since 2024/12/4
 */
public class initDemo {

    /**
     * 通过SDK内置蓝牙实现初始化
     *
     * @param context Context 上下文
     */
    public void initByJLBtManager(Context context) {
        if (null == context || RCSPController.isInit()) return; //已初始化
        BluetoothOption option = BluetoothOption.createDefaultOption()
                .setUseMultiDevice(true)
                .setUseDeviceAuth(true)
                .setPriority(BluetoothOption.PREFER_BLE);
        //初始化和配置蓝牙库
        //RCSPController的默认实现为JL_BluetoothManager
        RCSPController.init(context, option);

        //RCSP库核心操作类
        RcspOpImpl rcspOp = RCSPController.getInstance().getRcspOp();
        //SDK内置蓝牙操作类
        JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
    }

    /**
     * 通过自行实现蓝牙代理初始化
     *
     * @param context Context 上下文
     */
    public void initByCustomBluetoothManager(Context context) {
        if (null == context || RCSPController.isInit()) return; //已初始化
        BluetoothOption option = BluetoothOption.createDefaultOption()
                .setUseMultiDevice(true)
                .setUseDeviceAuth(true)
                .setPriority(BluetoothOption.PREFER_BLE);
        //初始化和配置蓝牙库
        //可以自行管理蓝牙实现
        RCSPController.init(new CustomBluetoothManager(context, option));

        //RCSP库核心操作类
        RcspOpImpl rcspOp = RCSPController.getInstance().getRcspOp();
    }


    /**
     * 自定义蓝牙管理器
     */
    private static final class CustomBluetoothManager extends RcspOpImpl {

        //BLE服务UUID
        private final static UUID UUID_SERVICE = BleManager.BLE_UUID_SERVICE;
        //BLE的写特征UUID
        private final static UUID UUID_WRITE = BleManager.BLE_UUID_WRITE;
        //BLE的通知特征UUID
        private final static UUID UUID_NOTIFICATION = BleManager.BLE_UUID_NOTIFICATION;

        /**
         * 第三方蓝牙库实现
         */
        //FIXME: 替换成自定义蓝牙库实现
        private final BleManager bleManager = BleManager.getInstance();


        public CustomBluetoothManager(@NonNull Context context, @NonNull BluetoothOption option) {
            super(context, option);
            bleManager.registerBleEventCallback(mBleEventCallback);
        }

        @Override
        public boolean isDeviceConnected(BluetoothDevice device) {
            //FIXME: 需要返回设备是否已连接的结果
            return bleManager.isConnectedDevice(device);
        }

        @Override
        public boolean sendDataToDevice(BluetoothDevice device, byte[] data) {
            //FIXME: 需要实现蓝牙发数功能。
            //FIXME: 1. 若BLE方式发数，需要注意 MTU分包 和 队列式发数
            //FIXME: 2. 此处实现可以是阻塞实现，也可以是异步实现
            if (!isDeviceConnected(device)) {
                return false;
            }
            bleManager.writeDataByBleAsync(device, UUID_SERVICE, UUID_WRITE, data,
                    (device1, serviceUUID, characteristicUUID, result, data1) -> {
                        JL_Log.d(TAG, "sendDataToDevice", "send : " + result);
                    });
            return true;
        }

        public boolean connect(BluetoothDevice device) {
            return bleManager.connectBleDevice(device);
        }

        public void disconnect(BluetoothDevice device) {
            bleManager.disconnectBleDevice(device);
        }

        @Override
        public void release() {
            super.release();
            bleManager.unregisterBleEventCallback(mBleEventCallback);
        }

        private final BleEventCallback mBleEventCallback = new BleEventCallback() {
            @Override
            public void onBleConnection(BluetoothDevice device, int status) {
                super.onBleConnection(device, status);
                //转换连接状态
                int btState = StateCode.CONNECTION_DISCONNECT;
                switch (status) {
                    case BluetoothProfile.STATE_CONNECTED:
                        btState = StateCode.CONNECTION_OK;
                        break;
                    case BluetoothProfile.STATE_CONNECTING:
                        btState = StateCode.CONNECTION_CONNECTING;
                        break;
                }
                JL_Log.d(TAG, "onBleConnection", "device : " + device + ", status : " + status + ", convert state : " + btState);
                //TODO: 透传设备连接状态。需要避免相同设备的连接状态重复传入
                notifyBtDeviceConnection(device, btState);
            }

            @Override
            public void onBleDataNotification(BluetoothDevice device, UUID serviceUuid, UUID characteristicsUuid, byte[] data) {
                super.onBleDataNotification(device, serviceUuid, characteristicsUuid, data);
                if (device != null && UUID_SERVICE.equals(serviceUuid) && UUID_NOTIFICATION.equals(characteristicsUuid)) {
                    //TODO: 透传接收到的数据
                    notifyReceiveDeviceData(device, data);
                }
            }
        };
    }
}
