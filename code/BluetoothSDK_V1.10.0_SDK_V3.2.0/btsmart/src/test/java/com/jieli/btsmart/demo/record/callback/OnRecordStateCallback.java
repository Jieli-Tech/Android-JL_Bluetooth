package com.jieli.btsmart.demo.record.callback;

import android.bluetooth.BluetoothDevice;

import com.jieli.btsmart.demo.record.model.RecordState;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 声音数据回调
 * @since 2022/9/27
 */
public interface OnRecordStateCallback {

    void onStateChange(BluetoothDevice device, RecordState state);
}
