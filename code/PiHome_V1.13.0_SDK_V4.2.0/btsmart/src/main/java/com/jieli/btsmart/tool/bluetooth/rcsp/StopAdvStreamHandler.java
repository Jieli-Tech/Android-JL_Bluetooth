package com.jieli.btsmart.tool.bluetooth.rcsp;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.impl.rcsp.RCSPController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 停止广播信息流处理
 * @since 2021/12/9
 */
public class StopAdvStreamHandler {
    private final RCSPController mController;
    private final List<String> blockList = new ArrayList<>();

    public StopAdvStreamHandler(RCSPController controller) {
        mController = controller;
    }


    public void onConnection(BluetoothDevice device, int status) {
        blockList.remove(device.getAddress());
    }

    public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
        if (cmd.getId() == Command.CMD_ADV_DEVICE_NOTIFY) {
            if (blockList.contains(device.getAddress())) {
                return;
            }
            blockList.add(device.getAddress());
            mController.controlAdvBroadcast(device, false, null);
        }
    }
}
