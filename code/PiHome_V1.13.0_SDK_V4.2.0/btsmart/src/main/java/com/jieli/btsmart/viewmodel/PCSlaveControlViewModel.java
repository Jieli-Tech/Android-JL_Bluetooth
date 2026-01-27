package com.jieli.btsmart.viewmodel;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.pc_slave.PCSlavePlayStatusInfo;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

/**
 * @ClassName: SPDIFControlViewModel
 * @Description: java类作用描述
 * @Author: ZhangHuanMing
 * @CreateDate: 2024/9/25 10:59
 */
public class PCSlaveControlViewModel extends BtBasicVM {
    private PCSlavePlayStatusInfo mPCSlavePlayStatusInfo = new PCSlavePlayStatusInfo();
    public MutableLiveData<PCSlavePlayStatusInfo> pcSlavePlayStatusInfoLiveData = new MutableLiveData<>();

    public PCSlaveControlViewModel() {
        mRCSPController.addBTRcspEventCallback(callback);
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        PCSlavePlayStatusInfo pcSlavePlayStatusInfo = deviceInfo.getPcSlavePlayStatusInfo();
        if (pcSlavePlayStatusInfo != null) {
            mPCSlavePlayStatusInfo = pcSlavePlayStatusInfo;
            pcSlavePlayStatusInfoLiveData.postValue(pcSlavePlayStatusInfo);
        }
    }

    @Override
    protected void release() {
        mRCSPController.removeBTRcspEventCallback(callback);
        super.release();
    }

    public void getPCSlavePlayStatusInfo() {
        mRCSPController.getPCSlavePlayStatusInfo(mRCSPController.getUsingDevice(), null);
    }
    /**
     * 播放暂停
     */
    public void playOrPause() {
        PCSlavePlayStatusInfo pcSlavePlayStatusInfo = new PCSlavePlayStatusInfo();
        pcSlavePlayStatusInfo.setPlayStatus(mPCSlavePlayStatusInfo.getPlayStatus() == PCSlavePlayStatusInfo.PLAY_STATUS_PAUSE ? PCSlavePlayStatusInfo.PLAY_STATUS_PLAY : PCSlavePlayStatusInfo.PLAY_STATUS_PAUSE);
        mRCSPController.setPCSlavePlayStatusInfo(getConnectedDevice(), pcSlavePlayStatusInfo, null);
    }

    /**
     * 播放下一曲
     */
    public void playNextSong() {
        mRCSPController.pcSlavePlayNext(getConnectedDevice(), null);
    }

    /**
     * 播放上一曲
     */
    public void playPreSong() {
        mRCSPController.pcSlavePlayPrev(getConnectedDevice(), null);
    }

    private final BTRcspEventCallback callback = new BTRcspEventCallback() {
        @Override
        public void onPCSlavePlayStatusChange(BluetoothDevice device, PCSlavePlayStatusInfo pcSlavePlayStatusInfo) {
            mPCSlavePlayStatusInfo = pcSlavePlayStatusInfo;
            pcSlavePlayStatusInfoLiveData.postValue(pcSlavePlayStatusInfo);
        }
        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            super.onSwitchConnectedDevice(device);
            getPCSlavePlayStatusInfo();
        }
    };
}
