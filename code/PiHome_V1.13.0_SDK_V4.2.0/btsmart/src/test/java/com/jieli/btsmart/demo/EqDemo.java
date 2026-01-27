package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.eq.DynamicLimiterParam;
import com.jieli.bluetooth.bean.device.eq.EqInfo;
import com.jieli.bluetooth.bean.device.eq.EqPresetInfo;
import com.jieli.bluetooth.bean.device.eq.ReverberationParam;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc EQ调节测试
 * @since 2021/12/2
 */
public class EqDemo {


    void getEqInfo() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {

            @Override
            public void onEqPresetChange(BluetoothDevice device, EqPresetInfo eqPresetInfo) {
                //此处将会回调均衡器预设值
            }

            @Override
            public void onEqChange(BluetoothDevice device, EqInfo eqInfo) {
                //此处将会回调均衡器效果信息
            }
        });
        //执行获取均衡器信息功能并等待结果回调
        controller.getEqInfo(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onEqPresetChange、BTRcspEventCallback#onEqChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void configEqInfo(EqInfo eqInfo) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {

            @Override
            public void onEqChange(BluetoothDevice device, EqInfo eqInfo) {
                //此处将会回调均衡器效果信息
            }
        });
        //执行配置均衡器效果功能并等待结果回调
        controller.configEqInfo(controller.getUsingDevice(), eqInfo, new OnRcspActionCallback<Boolean>() {
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


    void getExpandDataInfo() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {

            @Override
            public void onExpandFunction(BluetoothDevice device, int type, byte[] data) {
                //此处将会回调额外功能信息
            }
        });
        //执行获取额外功能信息功能并等待结果回调
        controller.getExpandDataInfo(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onExpandFunction回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void setExpandDataInfo(int mask, byte[] data) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //mask - 功能掩码
        //data - 参数数据
        //执行设置额外功能信息功能并等待结果回调
        controller.setExpandDataInfo(controller.getUsingDevice(), mask, data, new OnRcspActionCallback<Boolean>() {
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


    void setReverberationParameter(ReverberationParam param){
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行设置混响功能并等待结果回调
        controller.setReverberationParameter(controller.getUsingDevice(), param, new OnRcspActionCallback<Boolean>() {
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


    void setDynamicLimiterParameter(DynamicLimiterParam param){
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行设置动态限幅功能并等待结果回调
        controller.setDynamicLimiterParameter(controller.getUsingDevice(), param, new OnRcspActionCallback<Boolean>() {
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
