package com.jieli.btsmart.ui.settings.device.assistivelistening;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jieli.bluetooth.bean.device.hearing.HearingAssistInfo;
import com.jieli.bluetooth.bean.device.hearing.HearingChannelsStatus;
import com.jieli.bluetooth.bean.device.hearing.HearingFrequenciesInfo;
import com.jieli.bluetooth.bean.device.hearing.HearingFrequencyInfo;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;

/**
 * @ClassName: FittingViewModel
 * @Description: 辅听
 * @Author: ZhangHuanMing
 * @CreateDate: 2022/7/1 17:49
 */
public class HearingAssitstViewModel extends ViewModel {
    public final MutableLiveData<Boolean> mDeviceDisconnectMLD = new MutableLiveData<>();
    public final MutableLiveData<HearingAssistInfo> mHearingAssistInfoMLD = new MutableLiveData<>();
    public final MutableLiveData<HearingChannelsStatus> mHearingChannelsStatusMLD = new MutableLiveData<>();
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private final BluetoothDevice mTargetDevice;

    public HearingAssitstViewModel() {
        mTargetDevice = getRCSPController().getUsingDevice();
        getRCSPController().addBTRcspEventCallback(mBTRcspEventCallback);
    }

    public BluetoothDevice getTargetDevice() {
        return mTargetDevice;
    }

    public void getFittingConfigure(OnRcspActionCallback callback) {
        BluetoothDevice device = mRCSPController.getUsingDevice();
        mRCSPController.getHearingAssistInfo(device, callback);
    }

    public void setHearingAssistFrequency(HearingFrequencyInfo hearingFrequencyInfo, OnRcspActionCallback callback) {
        BluetoothDevice device = mRCSPController.getUsingDevice();
        mRCSPController.setHearingAssistFrequency(device, hearingFrequencyInfo, callback);
    }

    public void setHearingAssistFrequencies(HearingFrequenciesInfo hearingFrequenciesInfo, OnRcspActionCallback callback) {
        BluetoothDevice device = mRCSPController.getUsingDevice();
        mRCSPController.setHearingAssistFrequencies(device, hearingFrequenciesInfo, callback);
    }

    public void stopHearingAssistFitting(OnRcspActionCallback callback) {
        BluetoothDevice device = mRCSPController.getUsingDevice();
        mRCSPController.stopHearingAssistFitting(device, callback);
    }

    protected RCSPController getRCSPController() {
        return mRCSPController;
    }

    private BTRcspEventCallback mBTRcspEventCallback = new BTRcspEventCallback() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
            super.onConnection(device, status);
            if (BluetoothUtil.deviceEquals(device, mTargetDevice)
                    && (status == StateCode.CONNECTION_FAILED || status == StateCode.CONNECTION_DISCONNECT)) {
                mDeviceDisconnectMLD.postValue(true);
            }
        }

        @Override
        public void onHearingAssistInfo(BluetoothDevice device, HearingAssistInfo hearingAssistInfo) {
            super.onHearingAssistInfo(device, hearingAssistInfo);
            if (hearingAssistInfo == null) return;
            mHearingAssistInfoMLD.postValue(hearingAssistInfo);
        }

        @Override
        public void onHearingChannelsStatus(BluetoothDevice device, HearingChannelsStatus hearingChannelsStatus) {
            super.onHearingChannelsStatus(device, hearingChannelsStatus);
            if (hearingChannelsStatus == null) return;
            mHearingChannelsStatusMLD.postValue(hearingChannelsStatus);
        }
    };
}
