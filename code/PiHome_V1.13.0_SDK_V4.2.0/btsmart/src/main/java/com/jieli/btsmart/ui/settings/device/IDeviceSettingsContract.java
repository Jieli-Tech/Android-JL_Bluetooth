package com.jieli.btsmart.ui.settings.device;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.device.double_connect.ConnectedBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DoubleConnectionState;
import com.jieli.bluetooth.bean.device.voice.AdaptiveData;
import com.jieli.bluetooth.bean.device.voice.SceneDenoising;
import com.jieli.bluetooth.bean.device.voice.SmartNoPick;
import com.jieli.bluetooth.bean.device.voice.VocalBooster;
import com.jieli.bluetooth.bean.device.voice.VoiceFunc;
import com.jieli.bluetooth.bean.device.voice.WindNoiseDetection;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.btsmart.ui.base.bluetooth.IBluetoothBase;

import java.util.List;

/**
 * 设备设置接口
 *
 * @author zqjasonZhong
 * @since 2020/5/15
 */
@Deprecated
public interface IDeviceSettingsContract {

    interface IDeviceSettingsPresenter extends IBluetoothBase.IBluetoothPresenter {

        void checkNetworkAvailable();

        void disconnectBtDevice();

        void updateDeviceADVInfo();

        void changeDeviceName(String name, boolean isImmediately);

        void modifyHeadsetFunctions(int position, int type, byte[] value);

        void rebootDevice();

        List<VoiceMode> getNoiseModeMsg();

        VoiceMode getCurrentVoiceMode();

        void setCurrentVoiceMode(int modeID);

        boolean isCurrentVoiceMode(int modeID);

        boolean isSupportAnc();

        byte[] getSelectVoiceModes();

        void setSelectVoiceModeList(byte[] modes);

        void updateVoiceModeMsg();

        boolean isSupportAdaptiveANC();

        AdaptiveData getAdaptiveANCData();

        void updateAdaptiveANc();

        void setAdaptiveANC(AdaptiveData data);

        void startAdaptiveANCCheck();

        boolean isSupportSmartNoPick();

        void querySmartNoPick();

        void changeSmartNoPick(SmartNoPick param);

        boolean isSupportSceneDenoising();

        void querySceneDenoising();

        void changeSceneDenoising(SceneDenoising param);

        boolean isSupportWindNoiseDetection();

        void queryWindNoiseDetection();

        void changeWindNoiseDetection(WindNoiseDetection param);

        boolean isSupportVocalBooster();

        void queryVocalBooster();

        void changeVocalBooster(VocalBooster param);

        boolean isSupportDoubleConnection();

        void queryDoubleConnectionState();

        void setDoubleConnectionState(DoubleConnectionState state);

        void queryConnectedBtInfo();

        void destroy();

    }

    @SuppressWarnings("EmptyMethod")
    interface IDeviceSettingsView extends IBluetoothBase.IBluetoothView {

        void onADVInfoUpdate(ADVInfoResponse advInfo);

        void onGetADVInfoFailed(BaseError error);

        void onRebootSuccess();

        void onRebootFailed(BaseError error);

        void onSetNameSuccess(String name);

        void onSetNameFailed(BaseError error);

        void onConfigureSuccess(int position, int result);

        void onConfigureFailed(BaseError error);

        void onNetworkState(boolean isAvailable);

        void onUpdateConfigureSuccess();

        void onUpdateConfigureFailed(int code, String message);

        void onVoiceModeList(List<VoiceMode> list);

        void onCurrentVoiceMode(VoiceMode voiceMode);

        void onSelectedVoiceModes(byte[] selectedModes);

        void onVoiceFuncChange(VoiceFunc data);

        void onAdaptiveANCCheck(int state, int code);

        void onOpenSmartNoPickSetting(boolean isUser);

        void onDoubleConnectionStateChange(DoubleConnectionState state);

        void onConnectedBtInfo(ConnectedBtInfo info);
    }
}
