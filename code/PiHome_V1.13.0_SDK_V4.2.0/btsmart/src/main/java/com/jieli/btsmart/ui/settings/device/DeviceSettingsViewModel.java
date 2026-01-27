package com.jieli.btsmart.ui.settings.device;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.charging_case.ChargingCaseInfo;
import com.jieli.bluetooth.bean.configuration.DeviceConfiguration;
import com.jieli.bluetooth.bean.configuration.DongleConfiguration;
import com.jieli.bluetooth.bean.configuration.SoundBoxConfiguration;
import com.jieli.bluetooth.bean.configuration.TwsHeadsetConfiguration;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.double_connect.ConnectedBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DeviceBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DoubleConnectionState;
import com.jieli.bluetooth.bean.device.voice.AdaptiveData;
import com.jieli.bluetooth.bean.device.voice.SceneDenoising;
import com.jieli.bluetooth.bean.device.voice.SmartNoPick;
import com.jieli.bluetooth.bean.device.voice.VocalBooster;
import com.jieli.bluetooth.bean.device.voice.VoiceFunc;
import com.jieli.bluetooth.bean.device.voice.WindNoiseDetection;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.bean.settings.v0.SettingFunction;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.charging_case.ChargingCaseOpImpl;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnAdaptiveANCListener;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.interfaces.rcsp.charging_case.OnChargingCaseListener;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.OpResult;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.device.DeviceSettings;
import com.jieli.btsmart.tool.configure.ConfigureKit;
import com.jieli.btsmart.tool.configure.DoubleConnectionSp;
import com.jieli.btsmart.tool.product.ProductCacheManager;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.NetworkStateHelper;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;
import com.jieli.jl_http.bean.ProductModel;

import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 设备设置逻辑实现
 * @since 2023/8/25
 */
public class DeviceSettingsViewModel extends BtBasicVM {

    private static final int ANC_SETTING_TIMEOUT = 20 * 1000; //ANC自适应超时时间 - 20s

    private static final int SMART_NO_PICK_TIMEOUT = 12 * 1000;

    /**
     * 开始自适应降噪流程超时消息
     */
    private static final int MSG_START_ADAPTIVE_ANC_FLOW_TIMEOUT = 0x0101;
    /**
     * 开启智能免摘超时消息
     */
    private static final int MSG_OPEN_SMART_NO_PICK_TIMEOUT = 0x0102;

    /**
     * 更新json行为
     */
    public static final int ACTION_UPDATE_JSON = 1;
    /**
     * 打开智能免摘功能成功
     */
    public static final int ACTION_OPEN_SMART_NO_PICK_SUCCESS = 2;


    /**
     * 同步设备配置
     */
    public static final int OP_SYNC_DEVICE_SETTINGS = 1;
    /**
     * 改变设备名称
     */
    public static final int OP_CHANGE_DEVICE_NAME = 2;
    /**
     * 修改设备功能
     */
    public static final int OP_CHANGE_DEVICE_FUNCTION = 3;
    /**
     * 获取降噪模式列表
     */
    public static final int OP_GET_VOICE_MODE_LIST = 4;
    /**
     * 获取当前降噪模式
     */
    public static final int OP_GET_CURRENT_VOICE_MODE = 5;
    /**
     * 设置当前降噪模式
     */
    public static final int OP_SET_CURRENT_VOICE_MODE = 6;
    /**
     * 获取自适应降噪数据
     */
    public static final int OP_GET_ADAPTIVE_ANC_DATA = 7;
    /**
     * 设置自适应降噪数据
     */
    public static final int OP_SET_ADAPTIVE_ANC_DATA = 8;
    /**
     * 开始自适应降噪流程
     */
    public static final int OP_START_ADAPTIVE_ANC_FLOW = 9;
    /**
     * 获取智能免摘参数
     */
    public static final int OP_GET_SMART_NO_PICK = 10;
    /**
     * 设置智能免摘参数
     */
    public static final int OP_SET_SMART_NO_PICK = 11;
    /**
     * 获取场景降噪参数
     */
    public static final int OP_GET_SCENE_DENOISING = 12;
    /**
     * 设置场景降噪参数
     */
    public static final int OP_SET_SCENE_DENOISING = 13;
    /**
     * 获取风噪监测参数
     */
    public static final int OP_GET_WIND_NOISE_DETECTION = 14;
    /**
     * 设置风噪监测参数
     */
    public static final int OP_SET_WIND_NOISE_DETECTION = 15;
    /**
     * 获取人声增强参数
     */
    public static final int OP_GET_VOCAL_BOOSTER = 16;
    /**
     * 设置人声增强参数
     */
    public static final int OP_SET_VOCAL_BOOSTER = 17;
    /**
     * 获取设备双连状态
     */
    public static final int OP_GET_DUAL_CONNECTION_STATE = 18;
    /**
     * 设置设备双连状态
     */
    public static final int OP_SET_DUAL_CONNECTION_STATE = 19;
    /**
     * 获取已连接的设备信息
     */
    public static final int OP_GET_CONNECTED_BT_INFO = 20;

    /**
     * 操作设备
     */
    @NonNull
    public final BluetoothDevice mDevice;
    /**
     * 网络助手
     */
    private final NetworkStateHelper mNetworkStateHelper = NetworkStateHelper.getInstance();
    /**
     * 产品缓存管理类
     */
    private final ProductCacheManager mProductCacheManager = ProductCacheManager.getInstance();

    /**
     * 网络状态回调
     */
    public final MutableLiveData<Boolean> mNetworkStateMLD = new MutableLiveData<>();
    /**
     * 行为事件回调
     */
    public final MutableLiveData<Integer> mActionEventMLD = new MutableLiveData<>();
    /**
     * 操作结果回调
     */
    public final MutableLiveData<OpResult<Object>> mOpResultMLD = new MutableLiveData<>();
    /**
     * 设备信息回调
     */
    public final MutableLiveData<ADVInfoResponse> mDeviceSettingInfoMLD = new MutableLiveData<>();
    /**
     * 降噪模式列表回调
     */
    public final MutableLiveData<List<VoiceMode>> mVoiceModeListMLD = new MutableLiveData<>();
    /**
     * 当前降噪模式回调
     */
    public final MutableLiveData<VoiceMode> mCurrentVoiceModeMLD = new MutableLiveData<>();
    /**
     * 声音功能回调
     */
    public final MutableLiveData<VoiceFunc> mVoiceFunctionMLD = new MutableLiveData<>();
    /**
     * 流程状态回调
     */
    public final MutableLiveData<StateResult<Boolean>> mFlowStateMLD = new MutableLiveData<>();
    /**
     * 设备双连状态回调
     */
    public final MutableLiveData<DoubleConnectionState> mDualConnectionStateMLD = new MutableLiveData<>();
    /**
     * 已连接设备信息回调
     */
    public final MutableLiveData<ConnectedBtInfo> mConnectedBtInfoMLD = new MutableLiveData<>();
    /**
     *
     */
    public final MutableLiveData<Integer> mChargingCaseInitMLD = new MutableLiveData<>();
    /**
     * 彩屏仓操作类
     */
    private ChargingCaseOpImpl mChargingCaseOp;

    /**
     * UI处理
     */
    private final Handler mUIHandler = new Handler(Looper.getMainLooper(), msg -> {
        switch (msg.what) {
            case MSG_START_ADAPTIVE_ANC_FLOW_TIMEOUT: {
                mFlowStateMLD.postValue(new StateResult<Boolean>(OP_START_ADAPTIVE_ANC_FLOW)
                        .setState(StateResult.STATE_FINISH)
                        .setCode(ErrorCode.SUB_ERR_OPERATION_TIMEOUT)
                        .setMessage(ErrorCode.code2Msg(ErrorCode.SUB_ERR_OPERATION_TIMEOUT)));
                break;
            }
            case MSG_OPEN_SMART_NO_PICK_TIMEOUT: {
                mFlowStateMLD.postValue(new StateResult<Boolean>(OP_SET_SMART_NO_PICK)
                        .setState(StateResult.STATE_FINISH)
                        .setCode(ErrorCode.SUB_ERR_OPERATION_TIMEOUT)
                        .setMessage(ErrorCode.code2Msg(ErrorCode.SUB_ERR_OPERATION_TIMEOUT)));
                break;
            }
        }
        return true;
    });


    public DeviceSettingsViewModel(@NonNull BluetoothDevice device) {
        mDevice = device;
        mNetworkStateHelper.registerListener(mNetworkListener);
        mProductCacheManager.registerListener(mOnUpdateListener);
        mRCSPController.addBTRcspEventCallback(mBTEventCallback);

        mNetworkStateMLD.postValue(mNetworkStateHelper.isNetworkIsAvailable());
        final DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(device);
        if (null != deviceInfo && deviceInfo.getSdkType() == JLChipFlag.JL_COLOR_SCREEN_CHARGING_CASE) {
            //彩屏仓
            mChargingCaseOp = ChargingCaseOpImpl.instance(mRCSPController.getRcspOp());
            mChargingCaseOp.addOnChargingCaseListener(mOnChargingCaseListener);
        }
        ADVInfoResponse advInfo = mRCSPController.getADVInfo(device);
        if (null != advInfo) {
            mDeviceSettingInfoMLD.postValue(advInfo);
        }
    }

    @Override
    protected void release() {
        super.release();
        if (null != mChargingCaseOp) {
            mChargingCaseOp.removeOnChargingCaseListener(mOnChargingCaseListener);
        }
        mNetworkStateHelper.unregisterListener(mNetworkListener);
        mProductCacheManager.unregisterListener(mOnUpdateListener);
        mRCSPController.removeBTRcspEventCallback(mBTEventCallback);
    }

    /**
     * 断开设备连接
     */
    public void disconnectDevice() {
        if (!isConnectedDevice(mDevice.getAddress())) return;
        DevicePopDialogFilter.getInstance().addIgnoreDevice(mDevice.getAddress());
        mRCSPController.disconnectDevice(mDevice);
    }

    /* ============================================================================= *
     *  设备设置功能实现
     * ============================================================================= */

    /**
     * 是否支持设备设置功能
     *
     * @return boolean 结果
     */
    public boolean isSupportTwsFunction() {
        return UIHelper.isSupportTws(mRCSPController, mDevice) || is707NChargingCase();
    }

    /**
     * 获取缓存的设备设置信息
     *
     * @return 设备设置信息
     */
    public ADVInfoResponse getDeviceSettingInfo() {
        return mDeviceSettingInfoMLD.getValue();
    }

    /**
     * 同步设备设置
     */
    public void syncDeviceSettings() {
        if (!isSupportTwsFunction()) return;
        mRCSPController.getDeviceSettingsInfo(mDevice, 0xffffffff, new OnRcspActionCallback<ADVInfoResponse>() {
            @Override
            public void onSuccess(BluetoothDevice device, ADVInfoResponse message) {
                mOpResultMLD.postValue(new OpResult<>(OP_SYNC_DEVICE_SETTINGS)
                        .setCode(0).setData(message));
                mDeviceSettingInfoMLD.postValue(message);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                mOpResultMLD.postValue(new OpResult<>(OP_SYNC_DEVICE_SETTINGS)
                        .setCode(error.getSubCode()).setMessage(error.getMessage()));
            }
        });
    }

    /**
     * 修改设备名称
     *
     * @param name        String 设备名称
     * @param isEffective boolean 是否立即生效
     */
    public void changeDeviceName(String name, boolean isEffective) {
        if (TextUtils.isEmpty(name) || !isSupportTwsFunction()) return;
        mRCSPController.configDeviceName(mDevice, name, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                if (message == null) return;
                if (message == 0) {
                    mOpResultMLD.postValue(new OpResult<>(OP_CHANGE_DEVICE_NAME).setCode(0));
                    if (isEffective) {
                        final ADVInfoResponse info = getDeviceSettingInfo();
                        if (info != null) {
                            info.setDeviceName(name);
                            mDeviceSettingInfoMLD.postValue(info);
                        }
                        mRCSPController.rebootDevice(device, null);
                        return;
                    }
                    syncDeviceSettings();
                    return;
                }
                String msg = getContext().getString(R.string.device_name_failure);
                if (message == StateCode.ADV_SETTINGS_ERROR_DEVICE_NAME_LENGTH_OVER_LIMIT) {
                    msg = getContext().getString(R.string.settings_failed_dev_name_len_over_limit);
                }
                BaseError error = BaseError.buildResponseBadResult(message, Command.CMD_ADV_SETTINGS);
                error.setMessage(msg);
                onError(device, error);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                mOpResultMLD.postValue(new OpResult<>(OP_CHANGE_DEVICE_NAME)
                        .setCode(error.getSubCode()).setMessage(error.getMessage()));
            }
        });
    }

    /**
     * 修改设备功能
     *
     * @param position int 位置
     * @param func     int 功能码
     * @param payload  byte[] 有效数据
     */
    public void modifyDeviceFunction(int position, int func, byte[] payload) {
        if (null == payload || !isSupportTwsFunction()) return;
        mRCSPController.modifyDeviceSettingsInfo(mDevice, func, payload, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                if (message == null) return;
                if (message == 0) {
                    mOpResultMLD.postValue(new OpResult<>(OP_CHANGE_DEVICE_FUNCTION).setCode(0).setData(position));
                    syncDeviceSettings();
                    return;
                }
                String msg = getContext().getString(R.string.settings_failed);
                switch (message) {
                    case StateCode.ADV_SETTINGS_ERROR_IN_GAME_MODE:
                        msg = getContext().getString(R.string.settings_failed_by_game_mode);
                        break;
                    case StateCode.ADV_SETTINGS_ERROR_DEVICE_NAME_LENGTH_OVER_LIMIT:
                        msg = getContext().getString(R.string.settings_failed_dev_name_len_over_limit);
                        break;
                    case StateCode.ADV_SETTINGS_ERROR_LED_SETTINGS_FAILED:
                        msg = getContext().getString(R.string.settings_failed_led_settings);
                        break;
                }
                BaseError error = BaseError.buildResponseBadResult(message, Command.CMD_ADV_SETTINGS);
                error.setMessage(msg);
                onError(device, error);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                mOpResultMLD.postValue(new OpResult<>(OP_CHANGE_DEVICE_FUNCTION).setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    /* ============================================================================= *
     *  降噪功能实现
     * ============================================================================= */

    /**
     * 是否支持降噪功能
     *
     * @return boolean 结果
     */
    public boolean isSupportAnc() {
        final DeviceInfo deviceInfo = getDeviceInfo(mDevice.getAddress());
        if (null == deviceInfo) return false;
        return deviceInfo.isSupportAnc();
    }

    /**
     * 获取降噪模式列表
     *
     * @return List\<VoiceMode\> 降噪模式列表
     */
    public List<VoiceMode> getVoiceModeList() {
        final DeviceInfo deviceInfo = getDeviceInfo(mDevice.getAddress());
        if (null == deviceInfo || !deviceInfo.isSupportAnc()) return null;
        return deviceInfo.getVoiceModeList();
    }

    /**
     * 获取当前的降噪模式
     *
     * @return 降噪模式
     */
    public VoiceMode getCurrentVoiceMode() {
        final DeviceInfo deviceInfo = getDeviceInfo(mDevice.getAddress());
        if (null == deviceInfo || !deviceInfo.isSupportAnc()) return null;
        return deviceInfo.getCurrentVoiceMode();
    }

    /**
     * 获取降噪模式切换列表
     *
     * @return byte[] 切换模式列表
     */
    public byte[] getSwitchVoiceModes() {
        final ADVInfoResponse deviceSetting = getDeviceSettingInfo();
        if (null == deviceSetting) return null;
        return deviceSetting.getModes();
    }

    /**
     * 同步降噪模式列表
     */
    public void syncVoiceModeList() {
        if (!isSupportAnc()) return;
        mRCSPController.getAllVoiceModes(mDevice, new CustomActionCallback(mOpResultMLD, OP_GET_VOICE_MODE_LIST));
    }

    /**
     * 同步当前降噪模式
     */
    public void syncCurrentVoiceMode() {
        if (!isSupportAnc()) return;
        mRCSPController.getCurrentVoiceMode(mDevice, new CustomActionCallback(mOpResultMLD, OP_GET_CURRENT_VOICE_MODE));
    }

    /**
     * 设置当前降噪模式
     *
     * @param modeID int 模式ID
     */
    public void setCurrentVoiceMode(int modeID) {
        if (!isSupportAnc()) return;
        VoiceMode currentVoiceMode = getCurrentVoiceMode();
        if (currentVoiceMode != null && currentVoiceMode.getMode() == modeID) {
            mCurrentVoiceModeMLD.postValue(currentVoiceMode);
            return;
        }
        VoiceMode voiceMode = getCacheVoiceModeByMode(modeID);
        if (voiceMode == null) return;
        mRCSPController.setCurrentVoiceMode(mDevice, voiceMode, new CustomActionCallback(mOpResultMLD, OP_SET_CURRENT_VOICE_MODE));
    }

    /**
     * 同步切换降噪模式列表
     */
    public void syncSwitchVoiceModes() {
        mRCSPController.getDeviceSettingsInfo(mDevice, 0x01 << AttrAndFunCode.ADV_TYPE_ANC_MODE_LIST,
                new OnRcspActionCallback<ADVInfoResponse>() {
                    @Override
                    public void onSuccess(BluetoothDevice device, ADVInfoResponse message) {
                        mOpResultMLD.postValue(new OpResult<>(OP_SYNC_DEVICE_SETTINGS)
                                .setCode(0).setData(message));
                        ADVInfoResponse cacheInfo = getDeviceSettingInfo();
                        if (null == cacheInfo) {
                            cacheInfo = message;
                        } else {
                            cacheInfo.setModes(message.getModes());
                        }
                        if (cacheInfo.getModes() == null || cacheInfo.getModes().length == 0) {
                            onError(device, new BaseError(ErrorCode.SUB_ERR_DATA_FORMAT));
                            return;
                        }
                        mDeviceSettingInfoMLD.postValue(cacheInfo);
                    }

                    @Override
                    public void onError(BluetoothDevice device, BaseError error) {
                        if (null == error) return;
                        mOpResultMLD.postValue(new OpResult<>(OP_SYNC_DEVICE_SETTINGS)
                                .setCode(error.getSubCode()).setMessage(error.getMessage()));
                    }
                });
    }

    /**
     * 修改切换降噪模式列表
     *
     * @param modes byte[] 切换降噪模式列表
     */
    public void changeSwitchVoiceModes(byte[] modes) {
        if (modes == null || modes.length < 2) return;
        int value = 0x00;
        for (byte bit : modes) {
            value = value | (0x01 << bit);
        }
        modifyDeviceFunction(0, AttrAndFunCode.ADV_TYPE_ANC_MODE_LIST, CHexConver.intToBigBytes(value));
    }

    /* ============================================================================= *
     *  自适应降噪功能实现
     * ============================================================================= */

    /**
     * 是否支持自适应降噪功能
     *
     * @return boolean 结果
     */
    public boolean isSupportAdaptiveANC() {
        final DeviceInfo deviceInfo = getDeviceInfo(mDevice.getAddress());
        if (null == deviceInfo) return false;
        return deviceInfo.isSupportAdaptiveANC();
    }

    /**
     * 获取缓存的自适应降噪信息
     *
     * @return AdaptiveData 自适应降噪信息
     */
    public AdaptiveData getAdaptiveANCData() {
        final DeviceInfo deviceInfo = getDeviceInfo(mDevice.getAddress());
        if (null == deviceInfo || !deviceInfo.isSupportAdaptiveANC()) return null;
        return deviceInfo.getAdaptiveData();
    }

    /**
     * 同步自适应降噪信息
     */
    public void syncAdaptiveANc() {
        if (!isSupportAdaptiveANC()) return;
        mRCSPController.getAdaptiveANCData(mDevice, new CustomActionCallback(mOpResultMLD, OP_GET_ADAPTIVE_ANC_DATA));
    }

    /**
     * 设置自适应降噪信息
     *
     * @param data AdaptiveData 自适应降噪信息
     */
    public void setAdaptiveANC(AdaptiveData data) {
        if (null == data || !isSupportAdaptiveANC()) return;
        mRCSPController.setAdaptiveANCData(mDevice, data, new CustomActionCallback(mOpResultMLD, OP_SET_ADAPTIVE_ANC_DATA));
    }

    /**
     * 开始自适应降噪流程
     */
    public void startAdaptiveANCCheck() {
        if (!isSupportAdaptiveANC()) return;
        final StateResult<Boolean> stateResult = mFlowStateMLD.getValue();
        if (stateResult != null && stateResult.getState() == StateResult.STATE_WORKING) {
            JL_Log.d(tag, "startAdaptiveANCCheck", "It is working.");
            return;
        }
        mFlowStateMLD.postValue(new StateResult<Boolean>(OP_START_ADAPTIVE_ANC_FLOW)
                .setState(StateResult.STATE_WORKING).setCode(0));
        mRCSPController.startAdaptiveANC(mDevice, new OnAdaptiveANCListener() {
            @Override
            public void onStart() {
                mUIHandler.removeMessages(MSG_START_ADAPTIVE_ANC_FLOW_TIMEOUT);
                mUIHandler.sendEmptyMessageDelayed(MSG_START_ADAPTIVE_ANC_FLOW_TIMEOUT, ANC_SETTING_TIMEOUT);
            }

            @Override
            public void onFinish(int code) {
                mUIHandler.removeMessages(MSG_START_ADAPTIVE_ANC_FLOW_TIMEOUT);
                mFlowStateMLD.postValue(new StateResult<Boolean>(OP_START_ADAPTIVE_ANC_FLOW)
                        .setState(StateResult.STATE_FINISH)
                        .setCode(code)
                        .setMessage(ErrorCode.code2Msg(code)));
            }
        });
    }

    /* ============================================================================= *
     *  智能免摘功能实现
     * ============================================================================= */

    /**
     * 是否支持智能免摘功能
     *
     * @return boolean 结果
     */
    public boolean isSupportSmartNoPick() {
        return mRCSPController.isSupportSmartNoPick(mDevice);
    }

    /**
     * 获取缓存的智能免摘信息
     *
     * @return SmartNoPick 智能免摘信息
     */
    public SmartNoPick getSmartNoPick() {
        final DeviceInfo deviceInfo = getDeviceInfo(mDevice.getAddress());
        if (null == deviceInfo || !deviceInfo.isSupportSmartNoPick()) return null;
        return deviceInfo.getSmartNoPick();
    }

    /**
     * 同步智能免摘信息
     */
    public void syncSmartNoPick() {
        if (!isSupportSmartNoPick()) return;
        mRCSPController.getSmartNoPick(mDevice, new CustomActionCallback(mOpResultMLD, OP_GET_SMART_NO_PICK));
    }

    /**
     * 修改智能免摘参数
     *
     * @param param SmartNoPick 智能免摘信息
     */
    public void changeSmartNoPick(SmartNoPick param) {
        if (null == param || !isSupportSmartNoPick()) return;
        final StateResult<Boolean> stateResult = mFlowStateMLD.getValue();
        if (stateResult != null && stateResult.getState() == StateResult.STATE_WORKING) {
            JL_Log.d(tag, "changeSmartNoPick", "It is working.");
            return;
        }
        mFlowStateMLD.postValue(new StateResult<Boolean>(OP_SET_SMART_NO_PICK)
                .setState(StateResult.STATE_WORKING).setCode(0));
        mUIHandler.removeMessages(MSG_OPEN_SMART_NO_PICK_TIMEOUT);
        mUIHandler.sendEmptyMessageDelayed(MSG_OPEN_SMART_NO_PICK_TIMEOUT, SMART_NO_PICK_TIMEOUT);
        mRCSPController.setSmartNoPickParam(mDevice, param, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {

            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                mUIHandler.removeMessages(MSG_OPEN_SMART_NO_PICK_TIMEOUT);
                mFlowStateMLD.postValue(new StateResult<Boolean>(OP_SET_SMART_NO_PICK)
                        .setState(StateResult.STATE_FINISH)
                        .setCode(error.getSubCode()).setMessage(error.getMessage()));
            }
        });
    }


    /* ============================================================================= *
     *  场景降噪功能实现
     * ============================================================================= */

    /**
     * 是否支持场景降噪功能
     *
     * @return boolean 结果
     */
    public boolean isSupportSceneDenoising() {
        return mRCSPController.isSupportSceneDenoising(mDevice);
    }

    /**
     * 获取场景降噪参数
     *
     * @return SceneDenoising 场景降噪参数
     */
    public SceneDenoising getSceneDenoising() {
        final DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(mDevice);
        if (null == deviceInfo || !deviceInfo.isSupportSceneDenoising()) return null;
        return deviceInfo.getSceneDenoising();
    }

    /**
     * 同步场景降噪信息
     */
    public void syncSceneDenoising() {
        if (!isSupportSceneDenoising()) return;
        mRCSPController.getSceneDenoising(mDevice, new CustomActionCallback(mOpResultMLD, OP_GET_SCENE_DENOISING));
    }

    /**
     * 修改场景降噪参数
     *
     * @param param SceneDenoising 场景降噪参数
     */
    public void changeSceneDenoising(SceneDenoising param) {
        if (null == param || !isSupportSceneDenoising()) return;
        mRCSPController.setSceneDenoising(mDevice, param, new CustomActionCallback(mOpResultMLD, OP_SET_SCENE_DENOISING));
    }

    /* ============================================================================= *
     *  风噪监测功能实现
     * ============================================================================= */

    /**
     * 是否支持风噪监测功能
     *
     * @return boolean 结果
     */
    public boolean isSupportWindNoiseDetection() {
        return mRCSPController.isSupportWindNoiseDetection(mDevice);
    }

    /**
     * 获取缓存的风噪监测参数
     *
     * @return WindNoiseDetection 风噪监测参数
     */
    public WindNoiseDetection getWindNoiseDetection() {
        final DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(mDevice);
        if (null == deviceInfo || !deviceInfo.isSupportWindNoiseDetection()) return null;
        return deviceInfo.getWindNoiseDetection();
    }

    /**
     * 同步风噪监测参数
     */
    public void syncWindNoiseDetection() {
        if (!isSupportWindNoiseDetection()) return;
        mRCSPController.getWindNoiseDetection(mDevice, new CustomActionCallback(mOpResultMLD, OP_GET_WIND_NOISE_DETECTION));
    }

    /**
     * 修改风噪监测参数
     *
     * @param param WindNoiseDetection 风噪监测参数
     */
    public void changeWindNoiseDetection(WindNoiseDetection param) {
        if (null == param || !isSupportWindNoiseDetection()) return;
        mRCSPController.setWindNoiseDetection(mDevice, param, new CustomActionCallback(mOpResultMLD, OP_SET_WIND_NOISE_DETECTION));
    }


    /* ============================================================================= *
     *  人声增强功能实现
     * ============================================================================= */

    /**
     * 是否支持人声增强功能
     *
     * @return boolean 结果
     */
    public boolean isSupportVocalBooster() {
        return mRCSPController.isSupportVocalBooster(mDevice);
    }

    /**
     * 获取人声增强参数
     *
     * @return VocalBooster 人声增强参数
     */
    public VocalBooster getVocalBooster() {
        final DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(mDevice);
        if (null == deviceInfo || !deviceInfo.isSupportVocalBooster()) return null;
        return deviceInfo.getVocalBooster();
    }

    /**
     * 同步人声增强参数
     */
    public void syncVocalBooster() {
        if (!isSupportVocalBooster()) return;
        mRCSPController.getVocalBooster(mDevice, new CustomActionCallback(mOpResultMLD, OP_GET_VOCAL_BOOSTER));
    }

    /**
     * 修改人声增强参数
     *
     * @param param VocalBooster 人声增强参数
     */
    public void changeVocalBooster(VocalBooster param) {
        if (null == param || !isSupportVocalBooster()) return;
        mRCSPController.setVocalBooster(mDevice, param, new CustomActionCallback(mOpResultMLD, OP_SET_VOCAL_BOOSTER));
    }

    /* ============================================================================= *
     *  设备双连功能实现
     * ============================================================================= */

    /**
     * 是否支持设备双连功能
     *
     * @return boolean 结果
     */
    public boolean isSupportDoubleConnection() {
        return mRCSPController.isSupportDoubleConnection(mDevice);
    }

    /**
     * 获取缓存的设备双连状态
     *
     * @return DoubleConnectionState 设备双连状态
     */
    public DoubleConnectionState getDoubleConnectionState() {
        final DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(mDevice);
        if (null == deviceInfo || !deviceInfo.isSupportDoubleConnection()) return null;
        return deviceInfo.getDoubleConnectionState();
    }

    /**
     * 同步设备双连状态
     */
    public void syncDoubleConnectionState() {
        if (!isSupportDoubleConnection()) return;
        mRCSPController.queryDoubleConnectionState(mDevice, new CustomActionCallback(mOpResultMLD, OP_GET_DUAL_CONNECTION_STATE));
    }

    /**
     * 修改设备双连状态
     *
     * @param state DoubleConnectionState 设备双连状态
     */
    public void changeDoubleConnectionState(DoubleConnectionState state) {
        if (null == state || !isSupportDoubleConnection()) return;
        mRCSPController.setDoubleConnectionState(mDevice, state, new CustomActionCallback(mOpResultMLD, OP_SET_DUAL_CONNECTION_STATE));
    }

    /**
     * 获取缓存的已连接设备信息
     *
     * @return ConnectedBtInfo 已连接设备信息
     */
    public ConnectedBtInfo getConnectedBtInfo() {
        final DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(mDevice);
        if (null == deviceInfo) return null;
        return deviceInfo.getConnectedBtInfo();
    }

    /**
     * 同步已连接设备信息
     */
    public void syncConnectedBtInfo() {
        if (!isSupportDoubleConnection()) return;
        String mac = getEdrAddress(mDevice);
        DeviceBtInfo deviceBtInfo = DoubleConnectionSp.getInstance().getDeviceBtInfo(mac);
        if (null == deviceBtInfo) {
            String btName = AppUtil.getBtName(getContext());
            deviceBtInfo = new DeviceBtInfo().setBtName(btName);
        }
        mRCSPController.queryConnectedPhoneBtInfo(mDevice, deviceBtInfo, new OnRcspActionCallback<ConnectedBtInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, ConnectedBtInfo message) {
                JL_Log.d(tag, "getConnectedBtInfo", "onSuccess ---> " + message);
                mOpResultMLD.postValue(new OpResult<>(OP_GET_CONNECTED_BT_INFO)
                        .setCode(0));
                mConnectedBtInfoMLD.postValue(message);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                JL_Log.i(tag, "getConnectedBtInfo", "onError ---> " + error);
                mOpResultMLD.postValue(new OpResult<>(OP_GET_CONNECTED_BT_INFO)
                        .setCode(error.getSubCode()).setMessage(error.getMessage()));
            }
        });
    }

    /* ============================================================================= *
     *  彩屏仓功能实现
     * ============================================================================= */

    public boolean is707NChargingCase() {
        final ChargingCaseInfo info = getChargingCaseInfo();
        return null != info && !info.isJL701N();
    }

    public boolean isSyncWeather() {
        final DeviceSettings settings = getDeviceSettings();
        return null != settings && settings.isSyncWeather();
    }

    public boolean isSyncMessage() {
        final DeviceSettings settings = getDeviceSettings();
        return null != settings && settings.isSyncMessage();
    }

    public void closeSyncMessage() {
        final String mac = getMac();
        if (null == mac) return;
        final DeviceSettings settings = ConfigureKit.getInstance().getDeviceSettings(mac);
        if (null == settings) return;
        settings.setSyncMessage(false);
        settings.setAppPacketNameList(null);
        ConfigureKit.getInstance().saveDeviceSettings(mac, settings);
    }

    public ChargingCaseInfo getChargingCaseInfo() {
        if (null == mChargingCaseOp) return null;
        return mChargingCaseOp.getChargingCaseInfo(mDevice);
    }

    public void changeSyncWeather(boolean isEnable) {
        final String mac = getMac();
        if (null == mac) return;
        final DeviceSettings settings = ConfigureKit.getInstance().getDeviceSettings(mac);
        if (null == settings) return;
        if (settings.isSyncWeather() != isEnable) {
            settings.setSyncWeather(isEnable);
            ConfigureKit.getInstance().saveDeviceSettings(mac, settings);
            getContext().sendBroadcast(new Intent(SConstant.ACTION_DEVICE_SETTINGS_CHANGE));
        }
    }

    /* ============================================================================= *
     *  Auracast功能
     * ============================================================================= */

    /**
     * 是否支持Auracast接收端功能
     *
     * @return boolean 结果
     */
    public boolean isSupportAuracastReceiver() {
        DeviceConfiguration configuration = mRCSPController.getStatusManager().getDeviceConfigure(mDevice);
        if (configuration instanceof TwsHeadsetConfiguration) {
            TwsHeadsetConfiguration twsHeadsetCfg = (TwsHeadsetConfiguration) configuration;
            return twsHeadsetCfg.isSupportAuracast() && twsHeadsetCfg.isSupportAuracastReceiver();
        } else if (configuration instanceof SoundBoxConfiguration) {
            SoundBoxConfiguration soundBoxCfg = (SoundBoxConfiguration) configuration;
            return soundBoxCfg.isSupportAuracast() && soundBoxCfg.isSupportAuracastReceiver();
        } else if (configuration instanceof DongleConfiguration) {
            DongleConfiguration dongleCfg = (DongleConfiguration) configuration;
            return dongleCfg.isSupportAuracast() && dongleCfg.isSupportAuracastReceiver();
        }
        return false;
    }

    /**
     * 是否支持Auracast发射端功能
     *
     * @return boolean 结果
     */
    public boolean isSupportAuracastTransmitter() {
        DeviceConfiguration configuration = mRCSPController.getStatusManager().getDeviceConfigure(mDevice);
        if (configuration instanceof TwsHeadsetConfiguration) {
            TwsHeadsetConfiguration twsHeadsetCfg = (TwsHeadsetConfiguration) configuration;
            return twsHeadsetCfg.isSupportAuracast() && twsHeadsetCfg.isSupportAuracastTransmitter();
        } else if (configuration instanceof SoundBoxConfiguration) {
            SoundBoxConfiguration soundBoxCfg = (SoundBoxConfiguration) configuration;
            return soundBoxCfg.isSupportAuracast() && soundBoxCfg.isSupportAuracastTransmitter();
        } else if (configuration instanceof DongleConfiguration) {
            DongleConfiguration dongleCfg = (DongleConfiguration) configuration;
            return dongleCfg.isSupportAuracast() && dongleCfg.isSupportAuracastTransmitter();
        }
        return false;
    }


    private String getMac() {
        final DeviceInfo deviceInfo = getDeviceInfo(mDevice.getAddress());
        if (null == deviceInfo) return null;
        final String edrAddr = deviceInfo.getEdrAddr();
        return null == edrAddr ? mDevice.getAddress() : edrAddr;
    }

    private DeviceSettings getDeviceSettings() {
        final String mac = getMac();
        if (null == mac) return null;
        return ConfigureKit.getInstance().getDeviceSettings(mac);
    }

    private VoiceMode getCacheVoiceModeByMode(int modeID) {
        final List<VoiceMode> list = getVoiceModeList();
        if (null == list || list.isEmpty()) return null;
        for (VoiceMode mode : list) {
            if (mode.getMode() == modeID) {
                return mode;
            }
        }
        return null;
    }

    private final NetworkStateHelper.Listener mNetworkListener = (type, available) -> {
        JL_Log.d(tag, "onNetworkStateChange", "type : " + type + ", available : " + available);
        mNetworkStateMLD.postValue(available);
    };

    private final ProductCacheManager.OnUpdateListener mOnUpdateListener = new ProductCacheManager.OnUpdateListener() {
        @Override
        public void onImageUrlUpdate(BleScanMessage bleScanMessage) {

        }

        @Override
        public void onJsonUpdate(BleScanMessage bleScanMessage, String path) {
            if (path.contains(ProductModel.MODEL_PRODUCT_MESSAGE.getValue())) {
                //更新json文档
                mActionEventMLD.postValue(ACTION_UPDATE_JSON);
            }
        }
    };

    private final BTRcspEventCallback mBTEventCallback = new BTRcspEventCallback() {
        @Override
        public void onCurrentVoiceMode(BluetoothDevice device, VoiceMode voiceMode) {
            if (!BluetoothUtil.deviceEquals(mDevice, device)) return;
            mCurrentVoiceModeMLD.postValue(voiceMode);
        }

        @Override
        public void onVoiceModeList(BluetoothDevice device, List<VoiceMode> voiceModes) {
            if (!BluetoothUtil.deviceEquals(mDevice, device)) return;
            mVoiceModeListMLD.postValue(voiceModes);
        }

        @Override
        public void onDeviceSettingsInfo(BluetoothDevice device, int mask, ADVInfoResponse dataInfo) {
            if (!BluetoothUtil.deviceEquals(mDevice, device)) return;
            if (mask == 0xffffffff) {
                mDeviceSettingInfoMLD.postValue(dataInfo);
            }
        }

        @Override
        public void onVoiceFunctionChange(BluetoothDevice device, VoiceFunc voiceFunc) {
            if (null == voiceFunc || !BluetoothUtil.deviceEquals(mDevice, device)) return;
            if (voiceFunc.getType() == VoiceFunc.FUNC_SMART_NO_PICK) {
                SmartNoPick pick = (SmartNoPick) voiceFunc;
                if (mUIHandler.hasMessages(MSG_OPEN_SMART_NO_PICK_TIMEOUT)) {
                    mUIHandler.removeMessages(MSG_OPEN_SMART_NO_PICK_TIMEOUT);
                    if (pick.isOn()) {
                        mActionEventMLD.postValue(ACTION_OPEN_SMART_NO_PICK_SUCCESS);
                    }
                }
            }
            mVoiceFunctionMLD.postValue(voiceFunc);
        }

        @Override
        public void onDoubleConnectionChange(BluetoothDevice device, DoubleConnectionState state) {
            if (!BluetoothUtil.deviceEquals(mDevice, device)) return;
            JL_Log.d(tag, "onDoubleConnectionChange", "" + state);
            mDualConnectionStateMLD.postValue(state);
        }

        @Override
        public void onConnectedBtInfo(BluetoothDevice device, ConnectedBtInfo info) {
            if (!BluetoothUtil.deviceEquals(mDevice, device)) return;
            mConnectedBtInfoMLD.postValue(info);
        }
    };

    private final OnChargingCaseListener mOnChargingCaseListener = new OnChargingCaseListener() {
        @Override
        public void onInit(BluetoothDevice device, int state) {
            if (!BluetoothUtil.deviceEquals(mDevice, device)) return;
            mChargingCaseInitMLD.postValue(state);
        }

        @Override
        public void onChargingCaseInfoChange(BluetoothDevice device, int func, ChargingCaseInfo info) {

        }

        @Override
        public void onChargingCaseEvent(BluetoothDevice device, SettingFunction function) {

        }
    };

    public static class CustomActionCallback implements OnRcspActionCallback<Boolean> {

        private final MutableLiveData<OpResult<Object>> mOpResultMLD;
        private final int mOp;

        public CustomActionCallback(MutableLiveData<OpResult<Object>> opResultMLD, int op) {
            mOpResultMLD = opResultMLD;
            mOp = op;
        }

        @Override
        public void onSuccess(BluetoothDevice device, Boolean message) {
            mOpResultMLD.postValue(new OpResult<>(mOp).setCode(0));
        }

        @Override
        public void onError(BluetoothDevice device, BaseError error) {
            if (null == error) return;
            mOpResultMLD.postValue(new OpResult<>(mOp)
                    .setCode(error.getSubCode()).setMessage(error.getMessage()));
        }
    }

    public static class Factory implements ViewModelProvider.Factory {
        @NonNull
        private final BluetoothDevice mDevice;

        public Factory(@NonNull BluetoothDevice device) {
            mDevice = device;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new DeviceSettingsViewModel(mDevice);
        }
    }
}
