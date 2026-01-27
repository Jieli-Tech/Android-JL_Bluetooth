package com.jieli.btsmart.demo.bluetooth.lea;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.BluetoothUtil;

/**
 * LEADemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc LE Audio连接状态处理示例
 * @since 2025/8/13
 */
class LEAudioDemo {
    private LEAReceiver mReceiver;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void registerLEAReceiver(@NonNull Context context) {
        BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager == null ? BluetoothAdapter.getDefaultAdapter() : btManager.getAdapter();
        if (btAdapter.isLeAudioSupported() != BluetoothStatusCodes.FEATURE_SUPPORTED)
            return; //不支持 LEA功能
        if (null != mReceiver) return; //广播已注册
        mReceiver = new LEAReceiver();
        context.registerReceiver(mReceiver,
                new IntentFilter(BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED));
    }

    private void unregisterLEAReceiver(@NonNull Context context) {
        if (null == mReceiver) return;
        context.unregisterReceiver(mReceiver);
        mReceiver = null;
    }

    private static String getLEAMac(BluetoothDevice device){
        //返回映射的通讯BLE地址
        return device.getAddress();
    }

    private static class LEAReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent) return;
            String action = intent.getAction();
            if (null == action) return;
            if (TextUtils.equals(action, BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (null == device) return;
                int previousState = intent.getIntExtra(BluetoothLeAudio.EXTRA_PREVIOUS_STATE, -1);
                int state = intent.getIntExtra(BluetoothLeAudio.EXTRA_STATE, -1);
                if (state == -1 || previousState == state) return; //状态没有更新
                switch (state) {
                    case BluetoothProfile.STATE_DISCONNECTED: { //已断开
                        break;
                    }
                    case BluetoothProfile.STATE_CONNECTING: { //连接中
                        break;
                    }
                    case BluetoothProfile.STATE_CONNECTED: { //已连接
                        //TODO: 处理通讯BLE回连情况
                        //Case1: LEA 地址与通讯BLE地址一样
                        RCSPController.getInstance().getBluetoothManager().connect(device, BluetoothConstant.PROTOCOL_TYPE_BLE);
                        //Case2: LEA 地址与通讯BLE地址不一样，需要第一次连接上时记录，或者通过记录广播包源地址
                        String mappedAddress = getLEAMac(device);
                        BluetoothDevice bleDevice = BluetoothUtil.getRemoteDevice(mappedAddress);
                        if(null == bleDevice) return;
                        RCSPController.getInstance().getBluetoothManager().connect(bleDevice, BluetoothConstant.PROTOCOL_TYPE_BLE);
                        break;
                    }
                    case BluetoothProfile.STATE_DISCONNECTING: { //正在断开
                        break;
                    }
                }

            }
        }
    }
}
