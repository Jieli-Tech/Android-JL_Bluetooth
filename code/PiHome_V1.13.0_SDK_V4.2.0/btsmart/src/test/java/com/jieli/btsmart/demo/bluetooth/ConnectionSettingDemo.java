package com.jieli.btsmart.demo.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.BluetoothOption;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.BluetoothUtil;

import java.util.UUID;

/**
 * ConnectionSettingDemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 连接设备设置示例代码
 * @since 2024/12/9
 */
class ConnectionSettingDemo {

    void onlyConnectBle() {
        BluetoothOption option = BluetoothOption.createDefaultOption();
        option.setEnterLowPowerMode(true) //设置无过滤规则
                .setMandatoryUseBLE(true); //设置无过滤规则
        //配置SDK参数
//        Context context = null;
//        RCSPController.init(context, option);
        RCSPController.getInstance().configure(option);
    }

    void modifyBleUUID() {
        BluetoothOption option = BluetoothOption.createDefaultOption();
        option.setBleUUID(UUID.fromString("你的服务UUID"),
                UUID.fromString("你的写特征UUID"),
                UUID.fromString("你的通知特征UUID"));
        //配置SDK参数
//        Context context = null;
//        RCSPController.init(context, option);
        RCSPController.getInstance().configure(option);
    }

    void modifySppUUID() {
        BluetoothOption option = BluetoothOption.createDefaultOption();
        option.setSppUUID(UUID.fromString("你的自定义SPP UUID"));
        //配置SDK参数
//        Context context = null;
//        RCSPController.init(context, option);
        RCSPController.getInstance().configure(option);
    }


    void getClassicDeviceConnection() {
        BluetoothDevice device = BluetoothUtil.getRemoteDevice("设备的经典蓝牙地址");
        if (null == device) return;
        int state = RCSPController.getInstance().getBtOperation().isConnectedByProfile(device);
        //state有如下值:
//        BluetoothProfile.STATE_DISCONNECTED  --- 未连接
//        BluetoothProfile.STATE_CONNECTING    --- 连接中
//        BluetoothProfile.STATE_CONNECTED     --- 已连接
//        BluetoothProfile.STATE_DISCONNECTING --- 正在断开
    }

    void supportCTKDConnection() {
        BluetoothOption option = BluetoothOption.createDefaultOption();
        option.setSupportCTKD(true); //设置支持一键连接功能
        //配置SDK参数
//        Context context = null;
//        RCSPController.init(context, option);
        RCSPController.getInstance().configure(option);
    }
}
