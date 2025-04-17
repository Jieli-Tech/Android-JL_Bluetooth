package com.jieli.btsmart.demo.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.BluetoothCallbackImpl;
import com.jieli.bluetooth.interfaces.bluetooth.IBluetoothOperation;
import com.jieli.bluetooth.utils.BluetoothUtil;

import java.util.UUID;

/**
 * BtIODemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 蓝牙读写数据示例代码
 * @since 2024/12/7
 */
class BtIODemo {


    void receiveData() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        //获取蓝牙功能实现对象
        final IBluetoothOperation btOp = btManager.getBluetoothOperation();
        //蓝牙事件回调
        final BluetoothCallbackImpl bluetoothCallback = new BluetoothCallbackImpl() {
            @Override
            public void onBleDataNotification(BluetoothDevice device, UUID serviceUuid, UUID characteristicsUuid, byte[] data) {
                //回调接收到的BLE数据
            }

            @Override
            public void onSppDataNotification(BluetoothDevice device, byte[] data) {
                //回调接收到的SPP数据
            }
        };
        //注册蓝牙事件回调
        btOp.registerBluetoothCallback(bluetoothCallback);
        //注销蓝牙事件回调
//        btOp.unregisterBluetoothCallback(bluetoothCallback);
    }


    void sendData() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        final BluetoothDevice device = BluetoothUtil.getRemoteDevice("目标设备地址");
        if (null == device) return;
        //data  --- 发送数据
        byte[] data = new byte[0];
        //执行发数操作
        //ret --- 操作结果
        boolean ret = btManager.sendDataToDevice(device, data);
    }

}
