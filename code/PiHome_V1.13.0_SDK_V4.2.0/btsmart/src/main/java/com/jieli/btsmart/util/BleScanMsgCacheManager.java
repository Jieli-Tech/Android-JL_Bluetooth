package com.jieli.btsmart.util;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.JL_Log;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: BleScanMsgCacheUtil
 * @Description: java类作用描述
 * @Author: ZhangHuanMing
 * @CreateDate: 2022/5/19 11:35
 */
public class BleScanMsgCacheManager extends BTRcspEventCallback {
    private final String TAG = this.getClass().getSimpleName();
    private static BleScanMsgCacheManager instance;
    private final Map<String, BleScanMessage> sBleScanMessageMap = new HashMap<>();

    public static BleScanMsgCacheManager getInstance() {
        if (instance == null) {
            instance = new BleScanMsgCacheManager();
        }
        return instance;
    }

    /**
     * 发现设备回调
     *
     * @param device         蓝牙设备
     * @param bleScanMessage 设备广播包信息， 如果是经典蓝牙，没有Adv信息
     */
    @Override
    public void onDiscovery(BluetoothDevice device, BleScanMessage bleScanMessage) {
        super.onDiscovery(device, bleScanMessage);
        JL_Log.d(TAG, "onDiscovery: device:" + device + " bleScanMessage: " + bleScanMessage);
        sBleScanMessageMap.put(device.getAddress(), bleScanMessage);
    }

    public BleScanMessage getBleScanMessage(BluetoothDevice device) {
        return sBleScanMessageMap.get(device.getAddress());
    }

    public BleScanMessage getBleScanMessage(String address) {
        return sBleScanMessageMap.get(address);
    }
}
