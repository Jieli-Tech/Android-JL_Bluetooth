package com.jieli.btsmart.demo.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.BluetoothCallbackImpl;
import com.jieli.bluetooth.interfaces.bluetooth.IBluetoothOperation;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.tool.bluetooth.BTEventCallback;

/**
 * ConnectDeviceDemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 连接设备示例代码
 * @since 2024/12/7
 */
class ConnectDeviceDemo {


    void connectDevice() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        if (btManager.getBluetoothOperation().isConnecting()) return; //正在连接设备
        final BluetoothDevice device = BluetoothUtil.getRemoteDevice("目标设备地址");
        if (null == device) return;
        //蓝牙事件监听器
        final BTEventCallback btEventCallback = new BTEventCallback() {
            @Override
            public void onConnection(BluetoothDevice device, int status) {
                //回调蓝牙连接状态
            }
        };
        //注册蓝牙事件监听器
        btManager.addEventListener(btEventCallback);
        //执行连接设备
        btManager.connect(device);
        //执行断开设备
//        btManager.disconnect(device);
        //注销蓝牙事件监听器
//        btManager.removeEventListener(btEventCallback);
    }

    void connectBleDevice() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        if (btManager.getBluetoothOperation().isConnecting()) return; //正在连接设备
        final BluetoothDevice device = BluetoothUtil.getRemoteDevice("目标设备地址");
        if (null == device) return;
        //蓝牙事件监听器
        final BTEventCallback btEventCallback = new BTEventCallback() {
            @Override
            public void onConnection(BluetoothDevice device, int status) {
                //回调蓝牙连接状态
            }
        };
        //注册蓝牙事件监听器
        btManager.addEventListener(btEventCallback);
        //执行连接设备的BLE
        btManager.connect(device, BluetoothConstant.PROTOCOL_TYPE_BLE);
        //执行断开BLE
//        btManager.disconnect(device);
        //注销蓝牙事件监听器
//        btManager.removeEventListener(btEventCallback);
    }

    void connectBleDeviceV0() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        final BluetoothDevice device = BluetoothUtil.getRemoteDevice("目标设备地址");
        if (null == device) return;
        //获取蓝牙功能实现对象
        final IBluetoothOperation btOp = btManager.getBluetoothOperation();
        //蓝牙事件回调
        final BluetoothCallbackImpl bluetoothCallback = new BluetoothCallbackImpl() {
            @Override
            public void onBleConnection(BluetoothDevice device, int status) {
                //回调BLE设备连接成功
            }
        };
        //注册蓝牙事件回调
        btOp.registerBluetoothCallback(bluetoothCallback);
        //执行连接BLE操作
        //result --- 操作结果
        int result = btOp.connectBLEDevice(device);
        //执行断开BLE
//        btOp.disconnectBLEDevice(device);
        //注销蓝牙事件回调
//        btOp.unregisterBluetoothCallback(bluetoothCallback);
    }


    void connectSppDevice() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        final BluetoothDevice device = BluetoothUtil.getRemoteDevice("目标设备地址");
        if (null == device) return;
        //蓝牙事件监听器
        final BTEventCallback btEventCallback = new BTEventCallback() {
            @Override
            public void onConnection(BluetoothDevice device, int status) {
                //回调蓝牙连接状态
            }
        };
        //注册蓝牙事件监听器
        btManager.addEventListener(btEventCallback);
        //执行连接设备的SPP
        btManager.connect(device, BluetoothConstant.PROTOCOL_TYPE_SPP);
        //执行断开SPP
//        btManager.disconnect(device);
        //注销蓝牙事件监听器
//        btManager.removeEventListener(btEventCallback);
    }

    void connectSppDeviceV0() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        final BluetoothDevice device = BluetoothUtil.getRemoteDevice("目标设备地址");
        if (null == device) return;
        //获取蓝牙功能实现对象
        final IBluetoothOperation btOp = btManager.getBluetoothOperation();
        //蓝牙事件回调
        final BluetoothCallbackImpl bluetoothCallback = new BluetoothCallbackImpl() {
            @Override
            public void onSppStatus(BluetoothDevice device, int status) {
                //回调SPP设备连接成功
            }
        };
        //注册蓝牙事件回调
        btOp.registerBluetoothCallback(bluetoothCallback);
        //执行连接SPP操作
        //result --- 操作结果
        int result = btOp.connectSPPDevice(device);
        //执行断开SPP
//        btOp.disconnectSPPDevice(device);
        //注销蓝牙事件回调
//        btOp.unregisterBluetoothCallback(bluetoothCallback);
    }


    void connectClassicDevice() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        final BluetoothDevice device = BluetoothUtil.getRemoteDevice("目标设备地址");
        if (null == device) return;
        //蓝牙事件监听器
        final BTEventCallback btEventCallback = new BTEventCallback() {
            @Override
            public void onA2dpStatus(BluetoothDevice device, int status) {
                //回调A2DP连接状态
            }

            @Override
            public void onHfpStatus(BluetoothDevice device, int status) {
                //回调HFP连接状态
            }
        };
        //注册蓝牙事件监听器
        btManager.addEventListener(btEventCallback);
        //执行连接经典蓝牙设备
        btManager.startConnectByBreProfiles(device);
        //执行断开经典蓝牙
//        btManager.disconnectByProfiles(device);
        //注销蓝牙事件监听器
//        btManager.removeEventListener(btEventCallback);
    }
}
