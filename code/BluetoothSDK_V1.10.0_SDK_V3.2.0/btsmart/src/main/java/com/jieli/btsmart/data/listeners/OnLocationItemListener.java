package com.jieli.btsmart.data.listeners;

import android.view.View;

import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;

/**
 * 定位标志触发监听器
 *
 * @author zqjasonZhong
 * @since 2020/9/28
 */
public interface OnLocationItemListener {

    void onItemClick(View view, int position, HistoryBluetoothDevice device);
}
