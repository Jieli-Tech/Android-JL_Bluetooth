package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.voice.AdaptiveData;
import com.jieli.bluetooth.bean.device.voice.SceneDenoising;
import com.jieli.bluetooth.bean.device.voice.SmartNoPick;
import com.jieli.bluetooth.bean.device.voice.VocalBooster;
import com.jieli.bluetooth.bean.device.voice.VoiceFunc;
import com.jieli.bluetooth.bean.device.voice.WindNoiseDetection;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnAdaptiveANCListener;
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

    /// 自适应ANC算法

    public boolean isSupportAdaptiveANC() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return false;
        //判断设备是否支持自适应ANC算法
        return controller.isSupportAdaptiveANC(usingDevice);
    }


    public void getAdaptiveANCData() {
        if (!isSupportAdaptiveANC()) return;
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        DeviceInfo deviceInfo = controller.getDeviceInfo(usingDevice);
        if (null == deviceInfo) return; //设备未初始化
        boolean isForceRead = true; //是否强制读取
        //获取缓存的自适应ANC状态信息
        AdaptiveData adaptiveData = deviceInfo.getAdaptiveData();
        if (!isForceRead && null != adaptiveData) return; //已有缓存信息
        final BTRcspEventCallback eventCallback = new BTRcspEventCallback() {
            @Override
            public void onVoiceFunctionChange(BluetoothDevice device, VoiceFunc voiceFunc) {
                if (null == device || null == voiceFunc) return;
                if (voiceFunc instanceof AdaptiveData) {
                    //回调自适应ANC数据
                    controller.removeBTRcspEventCallback(this);
                }
            }
        };
        //注册RCSP事件监听器
        controller.addBTRcspEventCallback(eventCallback);
        //执行获取自适应ANC数据的接口
        controller.getAdaptiveANCData(usingDevice, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
                //结果将在 BTRcspEventCallback#onVoiceFunctionChange 回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                controller.removeBTRcspEventCallback(eventCallback);
            }
        });
    }

    public void setAdaptiveANCData() {
        if (!isSupportAdaptiveANC()) return;
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        DeviceInfo deviceInfo = controller.getDeviceInfo(usingDevice);
        if (null == deviceInfo) return; //设备未初始化
        //获取缓存的自适应ANC状态信息
        AdaptiveData adaptiveData = deviceInfo.getAdaptiveData();
        if (null == adaptiveData) { //可以通过 RCSPController#getAdaptiveANCData 接口获取
            adaptiveData = new AdaptiveData();
        }
        //置反开关状态
        adaptiveData.setOn(!adaptiveData.isOn());
        //执行设置自适应ANC参数的接口
        controller.setAdaptiveANCData(usingDevice, adaptiveData, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作结果
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
            }
        });
    }

    public void startAdaptiveANCCheck() {
        if (!isSupportAdaptiveANC()) return;
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        //执行开始自适应ANC算法检测流程的接口
        controller.startAdaptiveANC(usingDevice, new OnAdaptiveANCListener() {
            @Override
            public void onStart() {
                //回调检测流程开始
            }

            @Override
            public void onFinish(int code) {
                //回调检测流程结束
                //code --- 结果码
            }
        });
    }

    /// 智能免摘

    public boolean isSupportSmartNoPick() {
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return false;
        return controller.isSupportSmartNoPick(usingDevice);
    }


    public void getSmartNoPick() {
        if(!isSupportSmartNoPick()) return;
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        DeviceInfo deviceInfo = controller.getDeviceInfo(usingDevice);
        if (null == deviceInfo) return; //设备未初始化
        boolean isForceRead = true; //是否强制读取
        SmartNoPick smartNoPick = deviceInfo.getSmartNoPick();
        if (!isForceRead && smartNoPick != null) return; //已有缓存信息
        final BTRcspEventCallback eventCallback = new BTRcspEventCallback() {
            @Override
            public void onVoiceFunctionChange(BluetoothDevice device, VoiceFunc voiceFunc) {
                if (null == device || null == voiceFunc) return;
                if (voiceFunc instanceof SmartNoPick) {
                    //回调智能免摘信息
                    controller.removeBTRcspEventCallback(this);
                }
            }
        };
        //注册RCSP事件监听器
        controller.addBTRcspEventCallback(eventCallback);
        //执行获取智能免摘信息的接口
        controller.getSmartNoPick(usingDevice, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
                //结果将在 BTRcspEventCallback#onVoiceFunctionChange 回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                controller.removeBTRcspEventCallback(eventCallback);
            }
        });
    }


    public void setSmartNoPickParam() {
        if(!isSupportSmartNoPick()) return;
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        DeviceInfo deviceInfo = controller.getDeviceInfo(usingDevice);
        if (null == deviceInfo) return; //设备未初始化
        //获取缓存的智能免摘信息
        SmartNoPick smartNoPick = deviceInfo.getSmartNoPick();
        if (null == smartNoPick) { //可以通过 RCSPController#getSmartNoPick 接口获取
            smartNoPick = new SmartNoPick();
        }
        //置反开关状态
        //更多属性，在文档上获取
        smartNoPick.setOn(!smartNoPick.isOn());
        final BTRcspEventCallback eventCallback = new BTRcspEventCallback() {
            @Override
            public void onVoiceFunctionChange(BluetoothDevice device, VoiceFunc voiceFunc) {
                if (null == device || null == voiceFunc) return;
                if (voiceFunc instanceof SmartNoPick) {
                    //回调智能免摘信息
                    controller.removeBTRcspEventCallback(this);
                }
            }
        };
        //注册RCSP事件监听器
        controller.addBTRcspEventCallback(eventCallback);
        //执行设置智能免摘参数的接口
        controller.setSmartNoPickParam(usingDevice, smartNoPick, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
                //结果将在 BTRcspEventCallback#onVoiceFunctionChange 回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                controller.removeBTRcspEventCallback(eventCallback);
            }
        });
    }

    /// 场景降噪

    public boolean isSupportSceneDenoising(){
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return false;
        return controller.isSupportSceneDenoising(usingDevice);
    }

    public void getSceneDenoising(){
        if(!isSupportSceneDenoising()) return;
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        DeviceInfo deviceInfo = controller.getDeviceInfo(usingDevice);
        if (null == deviceInfo) return; //设备未初始化
        boolean isForceRead = true; //是否强制读取
        SceneDenoising sceneDenoising = deviceInfo.getSceneDenoising();
        if (!isForceRead && sceneDenoising != null) return; //已有缓存信息
        final BTRcspEventCallback eventCallback = new BTRcspEventCallback() {
            @Override
            public void onVoiceFunctionChange(BluetoothDevice device, VoiceFunc voiceFunc) {
                if (null == device || null == voiceFunc) return;
                if (voiceFunc instanceof SceneDenoising) {
                    //回调场景降噪信息
                    controller.removeBTRcspEventCallback(this);
                }
            }
        };
        //注册RCSP事件监听器
        controller.addBTRcspEventCallback(eventCallback);
        //执行获取场景降噪信息的接口
        controller.getSceneDenoising(usingDevice, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
                //结果将在 BTRcspEventCallback#onVoiceFunctionChange 回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                controller.removeBTRcspEventCallback(eventCallback);
            }
        });
    }

    public void setSceneDenoising() {
        if(!isSupportSceneDenoising()) return;
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        DeviceInfo deviceInfo = controller.getDeviceInfo(usingDevice);
        if (null == deviceInfo) return; //设备未初始化
        //获取缓存的场景降噪信息
        SceneDenoising sceneDenoising = deviceInfo.getSceneDenoising();
        if (null == sceneDenoising) { //可以通过 RCSPController#getSceneDenoising 接口获取
            sceneDenoising = new SceneDenoising();
        }
        //设置智能模式
        sceneDenoising.setMode(SceneDenoising.MODE_SMART);
        final BTRcspEventCallback eventCallback = new BTRcspEventCallback() {
            @Override
            public void onVoiceFunctionChange(BluetoothDevice device, VoiceFunc voiceFunc) {
                if (null == device || null == voiceFunc) return;
                if (voiceFunc instanceof SceneDenoising) {
                    //回调场景降噪信息
                    controller.removeBTRcspEventCallback(this);
                }
            }
        };
        //注册RCSP事件监听器
        controller.addBTRcspEventCallback(eventCallback);
        //执行设置场景降噪参数的接口
        controller.setSceneDenoising(usingDevice, sceneDenoising, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
                //结果将在 BTRcspEventCallback#onVoiceFunctionChange 回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                controller.removeBTRcspEventCallback(eventCallback);
            }
        });
    }

    /// 风燥监测

    public boolean isSupportWindNoiseDetection(){
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return false;
        return controller.isSupportWindNoiseDetection(usingDevice);
    }

    public void getWindNoiseDetection(){
        if(!isSupportWindNoiseDetection()) return;
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        DeviceInfo deviceInfo = controller.getDeviceInfo(usingDevice);
        if (null == deviceInfo) return; //设备未初始化
        boolean isForceRead = true; //是否强制读取
        WindNoiseDetection windNoiseDetection = deviceInfo.getWindNoiseDetection();
        if (!isForceRead && windNoiseDetection != null) return; //已有缓存信息
        final BTRcspEventCallback eventCallback = new BTRcspEventCallback() {
            @Override
            public void onVoiceFunctionChange(BluetoothDevice device, VoiceFunc voiceFunc) {
                if (null == device || null == voiceFunc) return;
                if (voiceFunc instanceof WindNoiseDetection) {
                    //回调风噪监测信息
                    controller.removeBTRcspEventCallback(this);
                }
            }
        };
        //注册RCSP事件监听器
        controller.addBTRcspEventCallback(eventCallback);
        //执行获取风噪监测信息的接口
        controller.getWindNoiseDetection(usingDevice, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
                //结果将在 BTRcspEventCallback#onVoiceFunctionChange 回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                controller.removeBTRcspEventCallback(eventCallback);
            }
        });
    }

    public void setWindNoiseDetection() {
        if(!isSupportWindNoiseDetection()) return;
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        DeviceInfo deviceInfo = controller.getDeviceInfo(usingDevice);
        if (null == deviceInfo) return; //设备未初始化
        //获取缓存的风噪监测信息
        WindNoiseDetection windNoiseDetection = deviceInfo.getWindNoiseDetection();
        if (null == windNoiseDetection) { //可以通过 RCSPController#getWindNoiseDetection 接口获取
            windNoiseDetection = new WindNoiseDetection();
        }
        //置反开关状态
        windNoiseDetection.setOn(!windNoiseDetection.isOn());
        final BTRcspEventCallback eventCallback = new BTRcspEventCallback() {
            @Override
            public void onVoiceFunctionChange(BluetoothDevice device, VoiceFunc voiceFunc) {
                if (null == device || null == voiceFunc) return;
                if (voiceFunc instanceof WindNoiseDetection) {
                    //回调风噪监测信息
                    controller.removeBTRcspEventCallback(this);
                }
            }
        };
        //注册RCSP事件监听器
        controller.addBTRcspEventCallback(eventCallback);
        //执行设置风噪监测参数的接口
        controller.setWindNoiseDetection(usingDevice, windNoiseDetection, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
                //结果将在 BTRcspEventCallback#onVoiceFunctionChange 回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                controller.removeBTRcspEventCallback(eventCallback);
            }
        });
    }

    /// 人声增强

    public boolean isSupportVocalBooster(){
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return false;
        return controller.isSupportVocalBooster(usingDevice);
    }

    public void getVocalBooster(){
        if(!isSupportVocalBooster()) return;
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        DeviceInfo deviceInfo = controller.getDeviceInfo(usingDevice);
        if (null == deviceInfo) return; //设备未初始化
        boolean isForceRead = true; //是否强制读取
        VocalBooster vocalBooster = deviceInfo.getVocalBooster();
        if (!isForceRead && vocalBooster != null) return; //已有缓存信息
        final BTRcspEventCallback eventCallback = new BTRcspEventCallback() {
            @Override
            public void onVoiceFunctionChange(BluetoothDevice device, VoiceFunc voiceFunc) {
                if (null == device || null == voiceFunc) return;
                if (voiceFunc instanceof VocalBooster) {
                    //回调人声增强信息
                    controller.removeBTRcspEventCallback(this);
                }
            }
        };
        //注册RCSP事件监听器
        controller.addBTRcspEventCallback(eventCallback);
        //执行获取人声增强信息的接口
        controller.getVocalBooster(usingDevice, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
                //结果将在 BTRcspEventCallback#onVoiceFunctionChange 回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                controller.removeBTRcspEventCallback(eventCallback);
            }
        });
    }

    public void setVocalBooster() {
        if(!isSupportVocalBooster()) return;
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        DeviceInfo deviceInfo = controller.getDeviceInfo(usingDevice);
        if (null == deviceInfo) return; //设备未初始化
        //获取缓存的人声增强信息
        VocalBooster vocalBooster = deviceInfo.getVocalBooster();
        if (null == vocalBooster) { //可以通过 RCSPController#getVocalBooster 接口获取
            vocalBooster = new VocalBooster();
        }
        //置反开关状态
        vocalBooster.setOn(!vocalBooster.isOn());
        final BTRcspEventCallback eventCallback = new BTRcspEventCallback() {
            @Override
            public void onVoiceFunctionChange(BluetoothDevice device, VoiceFunc voiceFunc) {
                if (null == device || null == voiceFunc) return;
                if (voiceFunc instanceof VocalBooster) {
                    //回调人声增强信息
                    controller.removeBTRcspEventCallback(this);
                }
            }
        };
        //注册RCSP事件监听器
        controller.addBTRcspEventCallback(eventCallback);
        //执行设置人声增强参数的接口
        controller.setVocalBooster(usingDevice, vocalBooster, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
                //结果将在 BTRcspEventCallback#onVoiceFunctionChange 回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                controller.removeBTRcspEventCallback(eventCallback);
            }
        });
    }
}
