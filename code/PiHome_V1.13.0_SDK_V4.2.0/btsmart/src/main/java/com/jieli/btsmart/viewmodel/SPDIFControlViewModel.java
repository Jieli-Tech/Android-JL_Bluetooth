package com.jieli.btsmart.viewmodel;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.spdif.SPDIFAudioSourceInfo;
import com.jieli.bluetooth.bean.device.spdif.SPDIFPlayStatusInfo;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

/**
 * @ClassName: SPDIFControlViewModel
 * @Description: java类作用描述
 * @Author: ZhangHuanMing
 * @CreateDate: 2024/9/25 10:59
 */
public class SPDIFControlViewModel extends BtBasicVM {
    private SPDIFPlayStatusInfo mSPDIFPlayStatusInfo = new SPDIFPlayStatusInfo();
    private SPDIFAudioSourceInfo mSPDIFAudioSourceInfo = new SPDIFAudioSourceInfo();
    public MutableLiveData<SPDIFPlayStatusInfo> spdifPlayStatusInfoLiveData = new MutableLiveData<>();
    public MutableLiveData<SPDIFAudioSourceInfo> spdifAudioSourceInfoLiveData = new MutableLiveData<>();

    public SPDIFControlViewModel() {
        mRCSPController.addBTRcspEventCallback(callback);
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        SPDIFPlayStatusInfo spdifPlayStatusInfo = deviceInfo.getSPDIFPlayStatusInfo();
        if (spdifPlayStatusInfo != null) {
            mSPDIFPlayStatusInfo = spdifPlayStatusInfo;
            spdifPlayStatusInfoLiveData.postValue(spdifPlayStatusInfo);
        }
        SPDIFAudioSourceInfo spdifAudioSourceInfo = deviceInfo.getSPDIFAudioSourceInfo();
        if (spdifAudioSourceInfo != null) {
            mSPDIFAudioSourceInfo = spdifAudioSourceInfo;
            spdifAudioSourceInfoLiveData.postValue(spdifAudioSourceInfo);
        }
    }

    @Override
    protected void release() {
        mRCSPController.removeBTRcspEventCallback(callback);
        super.release();
    }

    /**
     * 播放暂停
     */
    public void playOrPause() {
        SPDIFPlayStatusInfo spdifPlayStatusInfo = new SPDIFPlayStatusInfo();
        spdifPlayStatusInfo.setPlayStatus(mSPDIFPlayStatusInfo.getPlayStatus() == SPDIFPlayStatusInfo.PLAY_STATUS_PAUSE ? SPDIFPlayStatusInfo.PLAY_STATUS_PLAY : SPDIFPlayStatusInfo.PLAY_STATUS_PAUSE);
        mRCSPController.setSPDIFPlayStatusInfo(getConnectedDevice(), spdifPlayStatusInfo, null);
    }
    public void getSPDIFInfo() {
        mRCSPController.getSPDIFInfo(mRCSPController.getUsingDevice(), null);
    }
    public void setAudioSourceHDMI() {
        SPDIFAudioSourceInfo spdifAudioSourceInfo = new SPDIFAudioSourceInfo();
        spdifAudioSourceInfo.setAudioSource(SPDIFAudioSourceInfo.AUDIO_SOURCE_HDMI);
        mRCSPController.setSPDIFSPDIFAudioSourceInfo(getConnectedDevice(), spdifAudioSourceInfo, null);
    }

    public void setAudioSourceOptical() {
        SPDIFAudioSourceInfo spdifAudioSourceInfo = new SPDIFAudioSourceInfo();
        spdifAudioSourceInfo.setAudioSource(SPDIFAudioSourceInfo.AUDIO_SOURCE_OPTICAL);
        mRCSPController.setSPDIFSPDIFAudioSourceInfo(getConnectedDevice(), spdifAudioSourceInfo, null);
    }

    public void setAudioSourceCoaxial() {
        SPDIFAudioSourceInfo spdifAudioSourceInfo = new SPDIFAudioSourceInfo();
        spdifAudioSourceInfo.setAudioSource(SPDIFAudioSourceInfo.AUDIO_SOURCE_COAXIAL);
        mRCSPController.setSPDIFSPDIFAudioSourceInfo(getConnectedDevice(), spdifAudioSourceInfo, null);
    }

    private final BTRcspEventCallback callback = new BTRcspEventCallback() {
        @Override
        public void onSPDIFPlayStatusChange(BluetoothDevice device, SPDIFPlayStatusInfo spdifPlayStatusInfo) {
            mSPDIFPlayStatusInfo = spdifPlayStatusInfo;
            spdifPlayStatusInfoLiveData.postValue(spdifPlayStatusInfo);
        }

        @Override
        public void onSPDIFAudioSourceInfoChange(BluetoothDevice device, SPDIFAudioSourceInfo spdifAudioSourceInfo) {
            mSPDIFAudioSourceInfo = spdifAudioSourceInfo;
            spdifAudioSourceInfoLiveData.postValue(spdifAudioSourceInfo);
        }
        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            super.onSwitchConnectedDevice(device);
            getSPDIFInfo();
        }
    };
}
