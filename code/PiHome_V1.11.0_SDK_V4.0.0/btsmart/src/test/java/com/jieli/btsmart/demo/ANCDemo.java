package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.CHexConver;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc ANC设置测试
 * @since 2021/12/2
 */
public class ANCDemo {



    public void getAllVoiceModes() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onVoiceModeList(BluetoothDevice device, List<VoiceMode> voiceModes) {
                //此处将会回调噪声处理信息列表
            }
        });
        //执行获取所有噪声处理信息功能并等待结果回调
        controller.getAllVoiceModes(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //数据将在BTRcspEventCallback#onVoiceModeList回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void getCurrentVoiceMode() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onCurrentVoiceMode(BluetoothDevice device, VoiceMode voiceMode) {
                //此处将会回调当前噪声处理模式信息
            }
        });
        //执行获取当前噪声处理模式信息功能并等待结果回调
        controller.getCurrentVoiceMode(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //数据将在BTRcspEventCallback#onCurrentVoiceMode回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void setCurrentVoiceMode(VoiceMode voiceMode) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //voiceMode - 噪声处理模式
        //执行设置当前噪声处理模式功能并等待结果回调
        controller.setCurrentVoiceMode(controller.getUsingDevice(), voiceMode, new OnRcspActionCallback<Boolean>() {
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


    public void changeVoiceModeList(int[] modes) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //噪声模式切换不能小于2个
        if (modes == null || modes.length < 2) return;
        //设置模式切换顺序
        int value = 0x00;
        for (int mode : modes) {
            byte bit = (byte) (mode & 0xff);
            value = value | (0x01 << bit);
        }
        //执行设置模式切换顺序功能并等待结果回调
        controller.modifyDeviceSettingsInfo(controller.getUsingDevice(), AttrAndFunCode.ADV_TYPE_ANC_MODE_LIST, CHexConver.intToBigBytes(value), new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                //成功回调
                //成功之后，可以获取结果
                controller.getDeviceSettingsInfo(device, 0x01 << AttrAndFunCode.ADV_TYPE_ANC_MODE_LIST, new OnRcspActionCallback<ADVInfoResponse>() {
                    @Override
                    public void onSuccess(BluetoothDevice device, ADVInfoResponse message) {
//                        message.getModes();//噪声模式切换顺序
                    }

                    @Override
                    public void onError(BluetoothDevice device, BaseError error) {
                        //失败回调
                        //error - 错误信息
                    }
                });
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void setKeyFunction() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        List<ADVInfoResponse.KeySettings> list = new ArrayList<>(); //获取设备设置信息得到
        list.get(0).setFunction(AttrAndFunCode.KEY_FUNC_ID_SWITCH_ANC_MODE); //anc
        //设置按键功能切换ANC设置功能并等待结果回调
        controller.configKeySettings(controller.getUsingDevice(), list, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
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
