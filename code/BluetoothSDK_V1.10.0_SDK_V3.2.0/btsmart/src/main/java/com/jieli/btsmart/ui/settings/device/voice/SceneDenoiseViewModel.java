package com.jieli.btsmart.ui.settings.device.voice;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.voice.SceneDenoising;
import com.jieli.bluetooth.bean.device.voice.VoiceFunc;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 场景降噪逻辑处理
 * @since 2023/2/21
 */
public class SceneDenoiseViewModel extends BtBasicVM {
    MutableLiveData<SceneDenoising> mSceneDenoisingMLD = new MutableLiveData<>();

    private boolean isOpSetting;

    public SceneDenoiseViewModel() {
        mRCSPController.addBTRcspEventCallback(mBTRcspEventCallback);
    }


    @Override
    protected void release() {
        super.release();
        mRCSPController.removeBTRcspEventCallback(mBTRcspEventCallback);
    }

    public boolean isSupportSceneDenoising() {
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        return deviceInfo != null && deviceInfo.isSupportSceneDenoising();
    }

    public BluetoothDevice getUsingDevice() {
        return mRCSPController.getUsingDevice();
    }

    public SceneDenoising getSceneDenoising() {
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        if (null == deviceInfo) return null;
        return deviceInfo.getSceneDenoising();
    }

    public void querySceneDenoising() {
        if (!isSupportSceneDenoising()) return;
        mRCSPController.getSceneDenoising(getUsingDevice(), null);
    }

    public void changeSceneDenoising(SceneDenoising param) {
        if (!isSupportSceneDenoising() || null == param || isOpSetting) return;
        isOpSetting = true;
        mRCSPController.setSceneDenoising(getUsingDevice(), param, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                isOpSetting = false;
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                isOpSetting = false;
            }
        });
    }

    private final BTRcspEventCallback mBTRcspEventCallback = new BTRcspEventCallback() {

        @Override
        public void onVoiceFunctionChange(BluetoothDevice device, VoiceFunc voiceFunc) {
            if (voiceFunc != null && voiceFunc.getType() == VoiceFunc.FUNC_SCENE_DENOISING) {
                mSceneDenoisingMLD.setValue((SceneDenoising) voiceFunc);
            }
        }
    };
}
