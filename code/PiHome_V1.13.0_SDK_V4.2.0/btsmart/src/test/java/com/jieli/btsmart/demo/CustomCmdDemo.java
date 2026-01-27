package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.command.custom.CustomCmd;
import com.jieli.bluetooth.bean.parameter.CustomParam;
import com.jieli.bluetooth.bean.response.CustomResponse;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.RcspCommandCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.tool.DeviceStatusManager;
import com.jieli.bluetooth.utils.CommandBuilder;

import org.junit.Test;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 自定义命令测试
 * @since 2021/12/2
 */
public class CustomCmdDemo {


    @Test
    void sendCustomCmd(byte[] data) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //创建自定义命令
        CommandBase customCmd = CommandBuilder.buildCustomCmd(data);
        //发送自定义命令并等待结果回调
        controller.sendRcspCommand(controller.getUsingDevice(), customCmd, new RcspCommandCallback() {
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                if (cmd.getStatus() != StateCode.STATUS_SUCCESS) { //固件回复失败状态
                    BaseError error = new BaseError(ErrorCode.SUB_ERR_RESPONSE_BAD_STATUS, "Device response an bad status : " + cmd.getStatus());
                    error.setOpCode(Command.CMD_EXTRA_CUSTOM);
                    onErrCode(device, error);
                    return;
                }
                //发送成功回调, 需要回复设备
                CustomCmd customCmd = (CustomCmd) cmd;
                CustomResponse response = customCmd.getResponse();
                if (null == response) return;
                byte[] data = response.getData(); //自定义回复数据
                //处理设备回复数据
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    @Test
    void receiveCustomCmd() {
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
                //此处将回调设备发送的命令
                if (cmd.getId() == Command.CMD_EXTRA_CUSTOM) { //只处理自定义命令数据
                    CustomCmd customCmd = (CustomCmd) cmd;
                    CustomParam param = customCmd.getParam();
                    boolean isNeedResponse = cmd.getType() == CommandBase.FLAG_HAVE_PARAMETER_AND_RESPONSE || cmd.getType() == CommandBase.FLAG_NO_PARAMETER_AND_RESPONSE;
                    if (null == param) {
                        if (isNeedResponse) { //需要回复
                            byte[] responseData = new byte[0]; //可以设置回复的数据
                            customCmd.setParam(new CustomParam(responseData));
                            customCmd.setStatus(StateCode.STATUS_SUCCESS);
                            controller.sendRcspResponse(device, customCmd); //发送命令回复
                        }
                        return;
                    }
                    byte[] data = param.getData(); //自定义数据
                    //parseCustomData(data);
                    if (isNeedResponse) { //需要回复
                        byte[] responseData = new byte[0]; //可以设置回复的数据
                        customCmd.setParam(new CustomParam(responseData));
                        customCmd.setStatus(StateCode.STATUS_SUCCESS);
                        controller.sendRcspResponse(device, customCmd); //发送命令回复
                    }
                }
            }
        });
    }

    public void calcCustomDataLimit() {
        //获取当前操作设备
        BluetoothDevice device = RCSPController.getInstance().getUsingDevice();
        //获取最大发送值
        int max = DeviceStatusManager.getInstance().getMaxReceiveMtu(device);
        //计算自定义数据最大长度
        int size = max - 20;
    }
}
