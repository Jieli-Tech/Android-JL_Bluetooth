package com.jieli.btsmart.ui.settings.device.voice;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.voice.SmartNoPick;
import com.jieli.bluetooth.bean.device.voice.VoiceFunc;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 智能免摘逻辑实现
 * @since 2023/2/21
 */
public class SmartNoPickViewModel extends BtBasicVM {
    MutableLiveData<SmartNoPick> mSmartNoPickMLD = new MutableLiveData<>();

    private boolean isOpSetting;

    public SmartNoPickViewModel() {
        mRCSPController.addBTRcspEventCallback(mBTRcspEventCallback);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        release();
    }

    @Override
    protected void release() {
        super.release();
        mRCSPController.removeBTRcspEventCallback(mBTRcspEventCallback);
    }

    public boolean isSupportSmartNoPick() {
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        return deviceInfo != null && deviceInfo.isSupportSmartNoPick();
    }

    public BluetoothDevice getUsingDevice() {
        return mRCSPController.getUsingDevice();
    }

    public SmartNoPick getSmartNoPick() {
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        if (null == deviceInfo) return null;
        return deviceInfo.getSmartNoPick();
    }

    public void querySmartNoPick() {
        if (!isSupportSmartNoPick()) return;
        mRCSPController.getSmartNoPick(getUsingDevice(), null);
    }

    public void changeSmartNoPick(SmartNoPick param) {
        if (!isSupportSmartNoPick() || null == param || isOpSetting) return;
        isOpSetting = true;
        mRCSPController.setSmartNoPickParam(getUsingDevice(), param, new OnRcspActionCallback<Boolean>() {
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
            if (voiceFunc != null && voiceFunc.getType() == VoiceFunc.FUNC_SMART_NO_PICK) {
                mSmartNoPickMLD.setValue((SmartNoPick) voiceFunc);
            }
        }
    };
}
