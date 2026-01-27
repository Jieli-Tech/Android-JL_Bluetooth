package com.jieli.btsmart.tool.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.utils.CommandBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/12 9:42 AM
 * @desc :
 */
public class StopAdvDeviceNotifyCmdHandler extends BTEventCallback {
    List<String> list = new ArrayList<>();

    public void init() {
        BluetoothHelper.getInstance().registerBTEventCallback(this);
    }

    @Override
    public void onConnection(BluetoothDevice device, int status) {
        list.remove(device.getAddress());
    }

    @Override
    public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
        super.onDeviceCommand(device, cmd);
        if (cmd.getId() == Command.CMD_ADV_DEVICE_NOTIFY) {
            if (list.contains(device.getAddress())) {
                return;
            }
            list.add(device.getAddress());
            BluetoothHelper.getInstance().sendCommand(device, CommandBuilder.buildStopDeviceNotifyADVInfoCmd(), null);
        }
//        else if (cmd.getId() == Command.CMD_ADV_DEV_REQUEST_OPERATION) {
//            //暂时没有使用这个功能
//            RequestAdvOpCmd requestAdvOpCmd = (RequestAdvOpCmd) cmd;
//            RequestAdvOpParam param = requestAdvOpCmd.getParam();
//            if (param.getOp() == Constants.ADV_REQUEST_OP_UPDATE_CONFIGURE) {
//                BluetoothHelper.getInstance().sendCommand(device, CommandBuilder.buildGetADVInfoCmdWithAll(), null);
//            }
//        }
    }

}
