package com.jieli.btsmart.ui.settings;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.data.model.device.DeviceConnection;
import com.jieli.jl_bt_ota.constant.StateCode;

public class ModifyVoiceConfigViewModel extends ViewModel {
    public VoiceMode mVoiceMode;
    public boolean isLeftEdit;
    public boolean isRightEdit;
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private final BluetoothDevice mUseDevice;
    public final MutableLiveData<DeviceConnection> mDevConnectionMLD = new MutableLiveData<>();
    public final MutableLiveData<VoiceMode> mVoiceModeMLD = new MutableLiveData<>();
    public final MutableLiveData<Boolean> mSendResultMLD = new MutableLiveData<>();

    public ModifyVoiceConfigViewModel() {
        mRCSPController.addBTRcspEventCallback(mBTEventCallback);
        mUseDevice = mRCSPController.getUsingDevice();
        if (mUseDevice == null) {
            postConnectData(StateCode.CONNECTION_DISCONNECT);
        }
    }

    public RCSPController getRCSPController() {
        return mRCSPController;
    }

    public void getCurrentVoiceMode() {
        mRCSPController.getCurrentVoiceMode(mUseDevice, null);
    }

    public void setCurrentVoiceMode(VoiceMode voiceMode) {
        mRCSPController.setCurrentVoiceMode(mUseDevice, voiceMode, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                mSendResultMLD.setValue(true);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                mSendResultMLD.setValue(false);
            }
        });
    }

    public void release() {
        mRCSPController.removeBTRcspEventCallback(mBTEventCallback);
    }

    private void postConnectData(int status) {
        mDevConnectionMLD.postValue(new DeviceConnection(mUseDevice, status));
    }

    private void postCurrentVoiceMode(VoiceMode mode) {
        if (mVoiceMode != mode) {
            mVoiceMode = mode;
            mVoiceModeMLD.postValue(mode);
        }
    }

    private final BTRcspEventCallback mBTEventCallback = new BTRcspEventCallback() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (BluetoothUtil.deviceEquals(mUseDevice, device)) {
                postConnectData(status);
            }
        }

        @Override
        public void onCurrentVoiceMode(BluetoothDevice device, VoiceMode voiceMode) {
            if (BluetoothUtil.deviceEquals(mUseDevice, device)) {
                postCurrentVoiceMode(voiceMode);
            }
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            if (!BluetoothUtil.deviceEquals(device, mUseDevice)) {
                postConnectData(StateCode.CONNECTION_DISCONNECT);
            }
        }
    };
}