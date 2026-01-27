package com.jieli.btsmart.demo.bluetooth;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.IActionCallback;
import com.jieli.bluetooth.interfaces.OnReconnectHistoryRecordListener;

import java.util.List;

/**
 * HistoryManagerDemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 历史记录管理实例代码
 * @since 2024/12/7
 */
class HistoryManagerDemo {

    void getHistoryDeviceList() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        //获取历史记录列表
        final List<HistoryBluetoothDevice> historyDevices = btManager.getHistoryBluetoothDeviceList();
    }

    void reconnectHistory() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        HistoryBluetoothDevice history = null; //历史记录， 不能为空
        int timeout = 45 * 1000; //回连超时
        //执行回连历史记录操作
        btManager.reconnectHistoryBluetoothDevice(history, timeout, new OnReconnectHistoryRecordListener() {
            @Override
            public void onSuccess(HistoryBluetoothDevice history) {
                //回调回连历史记录成功
            }

            @Override
            public void onFailed(HistoryBluetoothDevice history, BaseError error) {
                //回调回连历史记录失败
                //history --- 历史记录
                //error   --- 错误信息
            }
        });
    }

    void removeHistory() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        HistoryBluetoothDevice history = null; //历史记录， 不能为空
        //执行删除历史记录操作
        btManager.removeHistoryDevice(history, new IActionCallback<HistoryBluetoothDevice>() {
            @Override
            public void onSuccess(HistoryBluetoothDevice message) {
                //回调删除历史记录成功
            }

            @Override
            public void onError(BaseError error) {
                //回调删除历史记录失败
                //error   --- 错误信息
            }
        });
    }


    void clearAllHistories() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        //执行清除所有历史记录操作
        btManager.clearHistoryDeviceRecord();
        //注意，清除完成后，需要重新更新历史记录列表
        final List<HistoryBluetoothDevice> historyDevices = btManager.getHistoryBluetoothDeviceList();
    }

    void stopReconnectFlow() {
        //获取蓝牙操作对象
        final JL_BluetoothManager btManager = RCSPController.getInstance().getBluetoothManager();
        if (null == btManager) return; //自定义蓝牙实现，btManager为空
        if (btManager.isReconnecting()) { //正在回连设备
            btManager.stopReconnect(); //停止回连流程
        }
    }
}
