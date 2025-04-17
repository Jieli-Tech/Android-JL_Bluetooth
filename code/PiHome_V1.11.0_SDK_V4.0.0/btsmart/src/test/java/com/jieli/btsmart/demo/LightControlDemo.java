package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.light.LightControlInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;

import org.junit.Test;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 灯光控制测试
 * @since 2021/12/31
 */
public class LightControlDemo {

    @Test
    public void getLightControlInfo() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onLightControlInfo(BluetoothDevice device, LightControlInfo lightControlInfo) {
                //此处将会回调灯光控制信息
            }
        });
        //执行获取灯光控制信息功能并等待结果回调
        controller.getLightControlInfo(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onLightControlInfo回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    @Test
    public void setLightControlInfo(LightControlInfo lightControlInfo) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行设置灯光控制功能并等待结果回调
        controller.setLightControlInfo(controller.getUsingDevice(), lightControlInfo, new OnRcspActionCallback<Boolean>() {
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
