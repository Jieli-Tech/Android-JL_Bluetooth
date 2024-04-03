package com.jieli.btsmart.demo.record;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.btsmart.demo.record.model.RecordParam;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc
 * @since 2023/2/6
 */
interface IRecordOp {

    /**
     * 开始录音
     *
     * @param device   操作设备
     * @param param    录音参数
     * @param callback 结果回调
     */
    void startRecord(BluetoothDevice device, RecordParam param, OnRcspActionCallback<Boolean> callback);

    /**
     * 结束录音
     *
     * @param device   操作设备
     * @param reason   结束原因
     * @param callback 结果回调
     */
    void stopRecord(BluetoothDevice device, int reason, OnRcspActionCallback<Boolean> callback);
}
