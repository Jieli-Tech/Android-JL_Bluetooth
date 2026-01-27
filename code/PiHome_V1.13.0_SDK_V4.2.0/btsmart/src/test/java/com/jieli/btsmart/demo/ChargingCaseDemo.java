package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.charging_case.ChargingCaseInfo;
import com.jieli.bluetooth.bean.command.health.message.Weather;
import com.jieli.bluetooth.bean.message.MessageInfo;
import com.jieli.bluetooth.bean.settings.v0.BoundDeviceState;
import com.jieli.bluetooth.bean.settings.v0.ResourceInfo;
import com.jieli.bluetooth.bean.settings.v0.SDKMessage;
import com.jieli.bluetooth.bean.settings.v0.ScreenInfo;
import com.jieli.bluetooth.bean.settings.v0.SettingFunction;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.impl.rcsp.charging_case.ChargingCaseOpImpl;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.interfaces.rcsp.charging_case.OnChargingCaseListener;
import com.jieli.filebrowse.bean.RegFile;
import com.jieli.filebrowse.bean.SDCardBean;

import org.junit.Test;

import java.util.List;

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
            public void onInit(BluetoothDevice device, int state) {
                //回调初始化状态
                //0 --- 成功
                //其他数值为错误码， 参考【错误码】章节
            }

            @Override
            public void onChargingCaseInfoChange(BluetoothDevice device, @ChargingCaseInfo.Function int func, ChargingCaseInfo info) {
                //回调彩屏仓功能变化
                switch (func) {
                    case ChargingCaseInfo.FUNC_BRIGHTNESS:
                        info.getBrightness();
                        break;
                    case ChargingCaseInfo.FUNC_FLASHLIGHT:
                        info.isFlashlightOn();
                        break;
                    case ChargingCaseInfo.FUNC_SCREEN_SAVERS:
                        info.getScreenSavers();
                        break;
                    case ChargingCaseInfo.FUNC_CURRENT_SCREEN_SAVER:
                        info.getCurrentScreenSaver();
                        break;
                    case ChargingCaseInfo.FUNC_CURRENT_BOOT_ANIM:
                        info.getCurrentBootAnim();
                        break;
                    case ChargingCaseInfo.FUNC_WALLPAPERS:
                        info.getWallpapers();
                        break;
                    case ChargingCaseInfo.FUNC_CURRENT_WALLPAPER:
                        info.getCurrentWallpaper();
                        break;
                }
            }

            @Override
            public void onChargingCaseEvent(BluetoothDevice device, SettingFunction function) {
                //回调彩屏仓功能事件，也会已通过 onChargingCaseInfoChange 接口回调， 可以不处理
            }
        };
        chargingCaseOp.addOnChargingCaseListener(listener);
        //Step3. 当不需要监听时，记得释监听器
        //chargingCaseOp.removeListener(listener);
    }

    public void getChargingCaseInfo(){
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 获取彩屏仓缓存信息
        chargingCaseOp.getChargingCaseInfo(RCSPController.getInstance().getUsingDevice());
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


    public void readSDKMessage(){
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用获取设备屏幕信息的接口
        chargingCaseOp.readSDKMessage(RCSPController.getInstance().getUsingDevice(), new OnRcspActionCallback<SDKMessage>() {
            @Override
            public void onSuccess(BluetoothDevice device, SDKMessage message) {
                //回调操作成功 和 结果
                //message -- SDK信息
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
                //error.getSubCode() --- 错误码
                //error.getMessage() --- 错误描述
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

    public void browseScreenSavers() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 选择需要浏览的存储器
        final BluetoothDevice device = RCSPController.getInstance().getUsingDevice();
        List<SDCardBean> onlineStorages = chargingCaseOp.getOnlineStorages(device); //获取在线存储器列表
        if (onlineStorages.isEmpty()) return; //没有存储器。操作失败
        SDCardBean target = null;
        for (SDCardBean storage : onlineStorages) {
            //优先查找Flash 和 Flash2
            if (storage.getIndex() == SDCardBean.INDEX_FLASH || storage.getIndex() == SDCardBean.INDEX_FLASH2) {
                target = storage;
                break;
            }
        }
        if (target == null) {
            target = onlineStorages.get(0);
        }
        //Step3. 调用浏览屏幕保护程序的接口
        //获取存储器句柄
        int devHandler = target.getDevHandler();
        chargingCaseOp.browseScreenSavers(device, devHandler, new OnRcspActionCallback<List<RegFile>>() {
            @Override
            public void onSuccess(BluetoothDevice device, List<RegFile> message) {
                //回调操作成功
                //message --- 资源列表
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
                //error.getSubCode() --- 错误码
                //error.getMessage() --- 错误描述
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
        //Step2. 选择需要浏览的存储器
        final BluetoothDevice device = RCSPController.getInstance().getUsingDevice();
        List<SDCardBean> onlineStorages = chargingCaseOp.getOnlineStorages(device); //获取在线存储器列表
        if (onlineStorages.isEmpty()) return; //没有存储器。操作失败
        SDCardBean target = null;
        for (SDCardBean storage : onlineStorages) {
            //优先查找Flash 和 Flash2
            if (storage.getIndex() == SDCardBean.INDEX_FLASH || storage.getIndex() == SDCardBean.INDEX_FLASH2) {
                target = storage;
                break;
            }
        }
        if (target == null) {
            target = onlineStorages.get(0);
        }
        //Step3. 调用设置当前屏幕保护程序的接口
        int devHandle = target.getDevHandler(); //存储器句柄
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
        //Step2. 选择需要浏览的存储器
        final BluetoothDevice device = RCSPController.getInstance().getUsingDevice();
        List<SDCardBean> onlineStorages = chargingCaseOp.getOnlineStorages(device); //获取在线存储器列表
        if (onlineStorages.isEmpty()) return; //没有存储器。操作失败
        SDCardBean target = null;
        for (SDCardBean storage : onlineStorages) {
            //优先查找Flash 和 Flash2
            if (storage.getIndex() == SDCardBean.INDEX_FLASH || storage.getIndex() == SDCardBean.INDEX_FLASH2) {
                target = storage;
                break;
            }
        }
        if (target == null) {
            target = onlineStorages.get(0);
        }
        //Step3. 调用设置当前开机动画的接口
        int devHandle = target.getDevHandler(); //存储器句柄
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
        //Step2. 调用同步天气接口
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


    public void pushMessage() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用推送消息接口
        //messageInfo -- 消息信息
        MessageInfo messageInfo = new MessageInfo();
        chargingCaseOp.pushMessageInfo(RCSPController.getInstance().getUsingDevice(), messageInfo, new OnRcspActionCallback<Boolean>() {
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

    public void removeMessage() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用删除消息接口
        //messageInfo -- 消息信息
        //删除消息时，主要是看【包名】 + 【时间戳】
        MessageInfo messageInfo = new MessageInfo();
        chargingCaseOp.removeMessageInfo(RCSPController.getInstance().getUsingDevice(), messageInfo, new OnRcspActionCallback<Boolean>() {
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

    public void browseWallPapers(){
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 选择需要浏览的存储器
        final BluetoothDevice device = RCSPController.getInstance().getUsingDevice();
        List<SDCardBean> onlineStorages = chargingCaseOp.getOnlineStorages(device); //获取在线存储器列表
        if (onlineStorages.isEmpty()) return; //没有存储器。操作失败
        SDCardBean target = null;
        for (SDCardBean storage : onlineStorages) {
            //优先查找Flash 和 Flash2
            if (storage.getIndex() == SDCardBean.INDEX_FLASH || storage.getIndex() == SDCardBean.INDEX_FLASH2) {
                target = storage;
                break;
            }
        }
        if (target == null) {
            target = onlineStorages.get(0);
        }
        //Step3. 调用浏览墙纸资源的接口
        //获取存储器句柄
        int devHandler = target.getDevHandler();
        chargingCaseOp.browseWallPapers(device, devHandler, new OnRcspActionCallback<List<RegFile>>() {
            @Override
            public void onSuccess(BluetoothDevice device, List<RegFile> message) {
                //回调操作成功
                //message --- 资源列表
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
                //error.getSubCode() --- 错误码
                //error.getMessage() --- 错误描述
            }
        });
    }


    public void getCurrentWallPaper() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用获取当前屏幕保护程序信息的接口
        chargingCaseOp.getCurrentWallPaper(RCSPController.getInstance().getUsingDevice(), new OnRcspActionCallback<ResourceInfo>() {
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

    public void setCurrentWallPaper() {
        //Step1. 创建彩屏充电仓实现类
        ChargingCaseOpImpl chargingCaseOp = ChargingCaseOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 选择需要浏览的存储器
        final BluetoothDevice device = RCSPController.getInstance().getUsingDevice();
        List<SDCardBean> onlineStorages = chargingCaseOp.getOnlineStorages(device); //获取在线存储器列表
        if (onlineStorages.isEmpty()) return; //没有存储器。操作失败
        SDCardBean target = null;
        for (SDCardBean storage : onlineStorages) {
            //优先查找Flash 和 Flash2
            if (storage.getIndex() == SDCardBean.INDEX_FLASH || storage.getIndex() == SDCardBean.INDEX_FLASH2) {
                target = storage;
                break;
            }
        }
        if (target == null) {
            target = onlineStorages.get(0);
        }
        //Step3. 调用设置当前屏幕保护程序的接口
        //获取存储器句柄
        int devHandler = target.getDevHandler();
        String filePath = "设备存在的屏保路径";
        chargingCaseOp.setCurrentScreenSaver(RCSPController.getInstance().getUsingDevice(), devHandler, filePath, new OnRcspActionCallback<Boolean>() {
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
