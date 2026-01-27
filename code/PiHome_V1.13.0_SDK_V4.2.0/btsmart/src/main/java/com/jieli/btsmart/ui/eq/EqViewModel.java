package com.jieli.btsmart.ui.eq;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.eq.EqInfo;
import com.jieli.bluetooth.bean.device.eq.EqPresetInfo;
import com.jieli.bluetooth.bean.device.voice.VolumeInfo;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspEventListener;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.data.model.device.DeviceConnection;
import com.jieli.btsmart.data.model.eq.VolumeCtrl;
import com.jieli.btsmart.util.EqCacheUtil;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

/**
 * EqViewModel
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc EQ控制逻辑实现
 * @since 2025/5/7
 */
public class EqViewModel extends BtBasicVM {

    public static final int OP_GET_EQ_INFO = 0x4010;
    public static final int OP_SET_EQ_INFO = 0x4011;

    /**
     * 是否禁止EQ调节回调
     */
    public final MutableLiveData<Boolean> isBanEqMLD = new MutableLiveData<>();
    /**
     * EQ信息回调
     */
    public final MutableLiveData<EqInfo> eqInfoMLD = new MutableLiveData<>();
    /**
     * 音量信息回调
     */
    public final MutableLiveData<VolumeInfo> volumeInfoMLD = new MutableLiveData<>();
    /**
     * 音量控制回调
     */
    public final MutableLiveData<VolumeCtrl> volumeCtrlMLD = new MutableLiveData<>();

    /**
     * 目标设备
     */
    private BluetoothDevice targetDev;

    private final Observer<DeviceConnection> connectionObserver = connection -> {
        final BluetoothDevice device = connection.getDevice();
        if (null == targetDev || BluetoothUtil.deviceEquals(device, targetDev)) {
            final int status = connection.getStatus();
            if (status == StateCode.CONNECTION_OK) {
                targetDev = device;
                syncEqInfo();
            } else if (status == StateCode.CONNECTION_DISCONNECT) {
                targetDev = null;
                isBanEqMLD.postValue(true);
                eqInfoMLD.postValue(EqCacheUtil.getCurrentCacheEqInfo());
            }
        }
    };
    private final Observer<BluetoothDevice> switchDeviceObserver = device -> {
        targetDev = device;
        syncEqInfo();
    };

    public EqViewModel() {
        targetDev = getConnectedDevice();
        mRCSPController.registerOnRcspEventListener(mRcspEventListener);
        deviceConnectionMLD.observeForever(connectionObserver);
        switchDeviceMLD.observeForever(switchDeviceObserver);
    }

    public boolean isSupportVolumeSync() {
        final DeviceInfo deviceInfo = getDeviceInfo(targetDev);
        if (null == deviceInfo) return false;
        return deviceInfo.isSupportVolumeSync();
    }

    public boolean isBanEq() {
        final DeviceInfo deviceInfo = getDeviceInfo(targetDev);
        if (null == deviceInfo) return true;
        return deviceInfo.isBanEq();
    }

    public BluetoothDevice getTargetDevice() {
        return targetDev;
    }

    public EqInfo getEqInfo() {
        return eqInfoMLD.getValue();
    }

    public void syncEqInfo() {
        syncVolumeInfo(getContext());
        if (!isConnectedDevice(targetDev)) {
            isBanEqMLD.postValue(true);
            eqInfoMLD.postValue(EqCacheUtil.getCurrentCacheEqInfo());
            return;
        }
        isBanEqMLD.postValue(isBanEq());
        mRCSPController.getEqInfo(targetDev, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                mRCSPController.getHighAndBassValue(device, null);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {

            }
        });
    }

    public void setEqInfo(EqInfo info) {
        if (null == info) return;
        if (isConnectedDevice(targetDev)) {
            mRCSPController.configEqInfo(targetDev, info, null);
            return;
        }
        //将自定义的的调节值保存到预设
        if (info.getMode() == EqInfo.MODE_CUSTOM) {
            EqPresetInfo eqPresetInfo = EqCacheUtil.getPresetEqInfo();
            eqPresetInfo.getEqInfos().get(info.getMode()).setValue(info.getValue());
            EqCacheUtil.savePresetEqInfo(eqPresetInfo);
        }
        EqCacheUtil.saveEqValue(info);
        eqInfoMLD.postValue(info);
    }

    public void setHighAndBass(int high, int bass) {
        if (!isConnectedDevice(targetDev)) return;
        JL_Log.d(tag, "setHighAndBass", "high : " + high + ", bass : " + bass);
        mRCSPController.setHighAndBassValue(targetDev, high, bass, null);
    }

    public void syncVolumeInfo(@NonNull Context context) {
        boolean isSupportVolumeSync = isSupportVolumeSync();
        if (!isConnectedDevice(targetDev) || isSupportVolumeSync) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            volumeInfoMLD.postValue(new VolumeInfo(max, current, isSupportVolumeSync));
            return;
        }
        mRCSPController.getCurrentVolume(targetDev, null);
    }

    public void setVolume(@NonNull Context context, int value) {
        if (!isConnectedDevice(targetDev)) return;
        boolean isSupportVolumeSync = isSupportVolumeSync();
        JL_Log.d(tag, "setVolume", "isSupportVolumeSync : " + isSupportVolumeSync + ", value : " + value);
        if (isSupportVolumeSync) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            return;
        }
        mRCSPController.adjustVolume(targetDev, value, null);
    }

    @Override
    protected void release() {
        deviceConnectionMLD.removeObserver(connectionObserver);
        switchDeviceMLD.removeObserver(switchDeviceObserver);
        mRCSPController.unregisterOnRcspEventListener(mRcspEventListener);
        super.release();
    }

    private int getMaxVol() {
        final DeviceInfo deviceInfo = getDeviceInfo(targetDev);
        if (null == deviceInfo) return 0;
        return deviceInfo.getMaxVol();
    }

    private final OnRcspEventListener mRcspEventListener = new OnRcspEventListener() {

        @Override
        public void onVolumeChange(BluetoothDevice device, VolumeInfo volume) {
            if (!BluetoothUtil.deviceEquals(device, targetDev)) return;
            volumeInfoMLD.postValue(volume);
        }

        @Override
        public void onEqChange(BluetoothDevice device, EqInfo eqInfo) {
            if (!BluetoothUtil.deviceEquals(device, targetDev)) return;
            eqInfoMLD.postValue(eqInfo);
        }

        @Override
        public void onHighAndBassChange(BluetoothDevice device, int high, int bass) {
            if (!BluetoothUtil.deviceEquals(device, targetDev)) return;
            volumeCtrlMLD.postValue(new VolumeCtrl(high, bass));
        }
    };
}
