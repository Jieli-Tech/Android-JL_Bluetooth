package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;

import org.junit.Test;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 插入设备测试
 * @since 2021/12/2
 */
public class LineinDemo {

    @Test
    void getAuxStatusInfo() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onAuxStatusChange(BluetoothDevice device, boolean isPlay) {
                //此处回调外接设备播放状态
            }
        });
        //执行获取外接设备播放状态功能并等待结果回调
        controller.getAuxStatusInfo(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onAuxStatusChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    @Test
    void auxPlayOrPause() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行外接设备播放或暂停功能并等待结果回调
        controller.auxPlayOrPause(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

}
