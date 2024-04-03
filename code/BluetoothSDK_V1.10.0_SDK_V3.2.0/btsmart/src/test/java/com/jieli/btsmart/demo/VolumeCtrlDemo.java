package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.device.voice.VolumeInfo;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.RcspCommandCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.CommandBuilder;

import org.junit.Test;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 音量控制测试
 * @since 2021/12/2
 */
public class VolumeCtrlDemo {


    @Test
    public void getCurrentVolume() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onVolumeChange(BluetoothDevice device, VolumeInfo volume) {
                //此处将回调设备音量信息
            }
        });
        //执行获取当前音量功能并等待结果回调
        controller.getCurrentVolume(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onVolumeChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    public void getCurrentVolumeV0(Context context) {
        //参考【查询设备系统信息】的命令发送代码
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 构建命令 --- 查询设备当前音量
        CommandBase getCurrentVolumeCmd = CommandBuilder.buildGetSysInfoCmd(AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC,
                0x01 << AttrAndFunCode.SYS_INFO_ATTR_VOLUME);
        //Step2: 执行操作命令并等待结果回调
    }

    @Test
    public void setVolume(int volume) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onVolumeChange(BluetoothDevice device, VolumeInfo volume) {
                //此处将回调设备音量信息
            }
        });
        //执行调节音量功能并等待结果回调
        controller.adjustVolume(controller.getUsingDevice(), volume, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onVolumeChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    public void setVolumeV0(Context context, int volume) {
        //参考【设置设备系统信息】的命令发送代码
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 构建命令 --- 设置系统音量
        CommandBase setVolumeCmd = CommandBuilder.buildSetVolumeCmd(volume);
        //Step2: 执行操作命令并等待结果回调
        manager.sendCommandAsync(manager.getConnectedDevice(), setVolumeCmd, manager.getBluetoothOption().getTimeoutMs(), new RcspCommandCallback() {
            //回调回复命令
            //注意: 无回复命时，回复命令本身
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                //Step3: 检查设备状态
                if (cmd.getStatus() != StateCode.STATUS_SUCCESS) { //设备状态异常，进行异常处理
                    onErrCode(device, new BaseError(ErrorCode.SUB_ERR_RESPONSE_BAD_STATUS, "Device reply an bad status: " + cmd.getStatus()));
                    return;
                }
                //成功回调
                //等待系统调整成功后，通过更新设备系统信息命令通知
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    @Test
    public void getHighAndBassValue() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onHighAndBassChange(BluetoothDevice device, int high, int bass) {
                //此处将会回调高低音信息
            }
        });
        //执行获取高低音信息功能并等待结果回调
        controller.getHighAndBassValue(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onHighAndBassChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    public void getHighAndBassValueV0(Context context) {
        //参考【查询设备系统信息】的命令发送代码
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 构建命令 --- 查询高低音设置
        CommandBase getHighAndBassCmd = CommandBuilder.buildGetSysInfoCmd(AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC,
                0x01 << AttrAndFunCode.SYS_INFO_ATTR_HIGH_AND_BASS);
        //Step2: 执行操作命令并等待结果回调
    }

    @Test
    public void setHighAndBassValue(int high, int bass) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onHighAndBassChange(BluetoothDevice device, int high, int bass) {
                //此处将会回调高低音信息
            }
        });
        //执行设置高低音功能并等待结果回调
        controller.setHighAndBassValue(controller.getUsingDevice(), high, bass, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onHighAndBassChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    public void setHighAndBassValueV0(Context context, int high, int bass) {
        //参考【设置设备系统信息】的命令发送代码
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 构建命令 --- 设置高低音设置
        CommandBase setHighAndBassCmd = CommandBuilder.buildSetHighAndBassCmd(high, bass);
        //Step2: 执行操作命令并等待结果回调
    }

}
