package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.command.AlarmExpandCmd;
import com.jieli.bluetooth.bean.device.alarm.AlarmBean;
import com.jieli.bluetooth.bean.device.alarm.AlarmListInfo;
import com.jieli.bluetooth.bean.device.alarm.AuditionParam;
import com.jieli.bluetooth.bean.device.alarm.DefaultAlarmBell;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;

import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 闹钟测试
 * @since 2021/12/2
 */
public class AlarmDemo {

    public void syncTime() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行同步时间功能并等待结果回调
        controller.syncTime(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
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


    public void readAlarmList() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onAlarmListChange(BluetoothDevice device, AlarmListInfo alarmListInfo) {
                //此处将会回调闹钟列表信息
            }
        });
        //执行获取闹钟列表功能并等待结果回调
        controller.readAlarmList(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会BTRcspEventCallback#onAlarmListChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void addOrModifyAlarm(AlarmBean alarm) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行添加或修改闹钟功能并等待结果回调
        controller.addOrModifyAlarm(controller.getUsingDevice(), alarm, new OnRcspActionCallback<Boolean>() {
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


    public void deleteAlarm(AlarmBean alarm) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行删除闹钟功能并等待结果回调
        controller.deleteAlarm(controller.getUsingDevice(), alarm, new OnRcspActionCallback<Boolean>() {
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


    public void readAlarmDefaultBellList() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {

            @Override
            public void onAlarmDefaultBellListChange(BluetoothDevice device, List<DefaultAlarmBell> bells) {
                //此处将会回调默认闹钟铃声列表信息
            }
        });
        //执行获取默认闹钟铃声列表功能并等待结果回调
        controller.readAlarmDefaultBellList(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会BTRcspEventCallback#onAlarmDefaultBellListChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void readAlarmBellArgs(byte alarmIndex) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //alarmIndex - 闹钟序号
        //执行获取指定闹钟的铃声参数功能并等待结果回调
        controller.readAlarmBellArgs(controller.getUsingDevice(), alarmIndex, new OnRcspActionCallback<List<AlarmExpandCmd.BellArg>>() {
            @Override
            public void onSuccess(BluetoothDevice device, List<AlarmExpandCmd.BellArg> message) {
                //成功回调
                //message - 铃声参数
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void setAlarmBellArg(AlarmExpandCmd.BellArg alarmBellArg) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //alarmBellArg - 通过读取闹钟铃声参数获得
        //执行设置指定闹钟的铃声参数功能并等待结果回调
        controller.setAlarmBellArg(controller.getUsingDevice(), alarmBellArg, new OnRcspActionCallback<Boolean>() {
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



    public void auditionAlarmBell(AuditionParam param) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行试听闹钟铃声功能并等待结果回调
        controller.auditionAlarmBell(controller.getUsingDevice(), param, new OnRcspActionCallback<Boolean>() {
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


    public void stopAlarmBell() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行停止试听闹钟铃声功能并等待结果回调
        controller.stopAlarmBell(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
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
