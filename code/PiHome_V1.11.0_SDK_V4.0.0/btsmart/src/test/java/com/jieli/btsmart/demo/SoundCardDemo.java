package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.eq.EqInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;

import org.junit.Test;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 声卡功能测试
 * @since 2021/12/3
 */
public class SoundCardDemo {

    @Test
    void getSoundCardStatusInfo() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onSoundCardStatusChange(BluetoothDevice device, long mask, byte[] values) {
                //此处将会回调声卡状态
            }
        });
        //执行获取声卡状态信息功能并等待结果回调
        controller.getSoundCardStatusInfo(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onSoundCardStatusChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    @Test
    void getSoundCardEqInfo() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onSoundCardEqChange(BluetoothDevice device, EqInfo eqInfo) {
                //此处将会回调声卡效果信息
            }
        });
        //执行获取声卡效果信息功能并等待结果回调
        controller.getSoundCardEqInfo(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onSoundCardEqChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    @Test
    void setSoundCardEqInf(byte[] value) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行设置声卡效果功能并等待结果回调
        controller.setSoundCardEqInfo(controller.getUsingDevice(), value, new OnRcspActionCallback<Boolean>() {
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

    @Test
    void setSoundCardFunction(byte index, int value) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //设置声卡功能并等待结果回调
        controller.setSoundCardFunction(controller.getUsingDevice(), index, value, new OnRcspActionCallback<Boolean>() {
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
