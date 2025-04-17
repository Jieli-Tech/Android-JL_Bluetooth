package com.jieli.btsmart.demo.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.btsmart.tool.bluetooth.BTEventCallback;

/**
 * ScanDeviceDemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 扫描设备示例代码
 * @since 2024/12/7
 */
class ScanDeviceDemo {

    void scanDevice() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        //蓝牙事件监听器
        final BTEventCallback btEventCallback = new BTEventCallback() {
            @Override
            public void onDiscoveryStatus(boolean bBle, boolean bStart) {
                //回调发现设备状态
            }

            @Override
            public void onDiscovery(BluetoothDevice device, BleScanMessage bleScanMessage) {
                //回调发现的设备
            }
        };
        //注册蓝牙事件监听器
        btManager.addEventListener(btEventCallback);
        int scanType = BluetoothConstant.SCAN_TYPE_BLE; //扫描设备类型 --- BLE设备
        // BluetoothConstant.SCAN_TYPE_CLASSIC --- 经典蓝牙设备
        int timeout = 30 * 1000; //搜索超时 --- 30秒
        //执行扫描设备
        btManager.scan(scanType, timeout);
        //注销蓝牙事件监听器
//        btManager.removeEventListener(btEventCallback);
    }

    void stopScan() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        //执行停止扫描设备
        btManager.stopScan();
    }

}
