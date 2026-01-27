package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 插入设备测试
 * @since 2021/12/2
 */
public class LineinDemo {

    public boolean isSupportAuxMode() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return false;
        DeviceInfo deviceInfo = controller.getDeviceInfo(usingDevice);
        if (null == deviceInfo) return false; //设备未初始化
        return deviceInfo.isAuxEnable();
    }

    public boolean isDeviceInAuxMode() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return false;
        DeviceInfo deviceInfo = controller.getDeviceInfo(usingDevice);
        if (null == deviceInfo) return false; //设备未初始化
        return deviceInfo.getCurFunction() == AttrAndFunCode.SYS_INFO_FUNCTION_AUX;
    }



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
