package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.command.health.message.Weather;
import com.jieli.bluetooth.bean.settings.v0.BoundDeviceState;
import com.jieli.bluetooth.bean.settings.v0.BrightnessSetting;
import com.jieli.bluetooth.bean.settings.v0.FlashlightSetting;
import com.jieli.bluetooth.bean.settings.v0.FunctionResource;
import com.jieli.bluetooth.bean.settings.v0.ResourceInfo;
import com.jieli.bluetooth.bean.settings.v0.ScreenInfo;
import com.jieli.bluetooth.bean.settings.v0.SettingFunction;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.impl.rcsp.charging_case.ChargingCaseOpImpl;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.interfaces.rcsp.charging_case.OnChargingCaseListener;

import org.junit.Test;

/**
 * ChargingCaseDemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 彩屏充电仓功能示例代码
 * @since 2024/6/21
 */
public class ChargingCaseDemo {

    @Test
    public void observeChargingCaseCallback() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 增加彩屏充电仓事件监听
        final OnChargingCaseListener listener = new OnChargingCaseListener() {
            @Override
            public void onChargingCaseEvent(BluetoothDevice device, SettingFunction function) {
                switch (function.getFunction()) {
                    case SettingFunction.FUNC_BRIGHTNESS: { //屏幕亮度
                        BrightnessSetting setting = (BrightnessSetting) function;
                        break;
                    }
                    case SettingFunction.FUNC_FLASHLIGHT: { //闪光灯状态
                        FlashlightSetting setting = (FlashlightSetting) function;
                        break;
                    }
                    case SettingFunction.FUNC_USING_RESOURCE: { //当前使用资源信息
                        FunctionResource state = (FunctionResource) function;
                        ResourceInfo resourceInfo = state.getResourceInfo();  //资源信息
                        switch (state.getSubFunction()) {
                            case FunctionResource.FUNC_SCREEN_SAVERS: { //屏幕保护程序(壁纸)
                                break;
                            }
                            case FunctionResource.FUNC_BOOT_ANIM: { //开机动画

                                break;
                            }
                        }
                        break;
                    }
                }
            }
        };
        chargingCaseOp.addOnChargingCaseListener(listener);
        //Step3. 当不需要监听时，记得释监听器
        //chargingCaseOp.removeListener(listener);
    }

    @Test
    public void getBoundDeviceState() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用获取配对设备信息的接口
        chargingCaseOp.getBoundDeviceState(RCSPController.getInstance().getUsingDevice(), new OnRcspActionCallback<BoundDeviceState>() {
            @Override
            public void onSuccess(BluetoothDevice device, BoundDeviceState message) {
                //回调操作成功 和 结果
                //message -- 配对设备的信息
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
            }
        });
    }

    @Test
    public void readScreenInfo() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用获取设备屏幕信息的接口
        chargingCaseOp.readScreenInfo(RCSPController.getInstance().getUsingDevice(), new OnRcspActionCallback<ScreenInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, ScreenInfo message) {
                //回调操作成功 和 结果
                //message -- 屏幕信息
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
            }
        });
    }

    @Test
    public void getBrightness() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用获取屏幕亮度的接口
        chargingCaseOp.getBrightness(RCSPController.getInstance().getUsingDevice(), new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                //回调操作成功 和 结果
                //message -- 亮度, 范围:0 - 100%
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
            }
        });
    }

    @Test
    public void setBrightness() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用设置屏幕亮度的接口
        //brightness -- 亮度, 范围:0 - 100%
        int brightness = 60;
        chargingCaseOp.setBrightness(RCSPController.getInstance().getUsingDevice(), brightness, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功 和 结果
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
            }
        });
    }

    @Test
    public void getFlashlightState() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用获取闪光灯状态的接口
        chargingCaseOp.getFlashlightState(RCSPController.getInstance().getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功 和 结果
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
            }
        });
    }

    @Test
    public void setFlashlightState() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用设置闪光灯的接口
        //isOn -- 开关
        boolean isOn = true;
        chargingCaseOp.setFlashlightState(RCSPController.getInstance().getUsingDevice(), isOn, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功 和 结果
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
            }
        });
    }

    @Test
    public void getCurrentScreenSaver() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用获取当前屏幕保护程序信息的接口
        chargingCaseOp.getCurrentScreenSaver(RCSPController.getInstance().getUsingDevice(), new OnRcspActionCallback<ResourceInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, ResourceInfo message) {
                //回调操作成功 和 结果
                //message -- 资源信息
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
            }
        });
    }

    @Test
    public void setCurrentScreenSaver() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用设置当前屏幕保护程序的接口
        int devHandle = 0; //存储器句柄
        String filePath = "设备存在的屏保路径";
        chargingCaseOp.setCurrentScreenSaver(RCSPController.getInstance().getUsingDevice(), devHandle, filePath, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功 和 结果
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
            }
        });
    }

    @Test
    public void getCurrentBootAnim() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用获取当前开机动画信息的接口
        chargingCaseOp.getCurrentBootAnim(RCSPController.getInstance().getUsingDevice(), new OnRcspActionCallback<ResourceInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, ResourceInfo message) {
                //回调操作成功 和 结果
                //message -- 资源信息
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
            }
        });
    }

    @Test
    public void setCurrentBootAnim() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用设置当前开机动画的接口
        int devHandle = 0; //存储器句柄
        String filePath = "设备存在的开机动画路径";
        chargingCaseOp.setCurrentBootAnim(RCSPController.getInstance().getUsingDevice(), devHandle, filePath, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功 和 结果
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
            }
        });
    }

    @Test
    public void syncWeatherInfo() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用设置当前屏幕保护程序信息的接口
        //weather -- 天气信息
        Weather weather = new Weather(new byte[0]);
        chargingCaseOp.syncWeatherInfo(RCSPController.getInstance().getUsingDevice(), weather, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功 和 结果
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
            }
        });
    }
}
