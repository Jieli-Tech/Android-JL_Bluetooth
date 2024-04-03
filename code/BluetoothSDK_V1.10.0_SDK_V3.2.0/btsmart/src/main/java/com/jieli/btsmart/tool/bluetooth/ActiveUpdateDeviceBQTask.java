package com.jieli.btsmart.tool.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.bluetooth.RcspCommandCallback;
import com.jieli.bluetooth.utils.CommandBuilder;
import com.jieli.btsmart.data.model.bluetooth.DeviceInfo;
import com.jieli.btsmart.util.UIHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: UpdateDeviceBQTask
 * @Description: 用于不会主动推送刷新电量的设备，实现刷新
 * @Author: ZhangHuanMing
 * @CreateDate: 2020/11/19 10:21
 */
public class ActiveUpdateDeviceBQTask extends BTEventCallback {
    private final BluetoothHelper mBluetoothHelper = BluetoothHelper.getInstance();
    private final Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_UPDATE_DEVICE_BATTERY) {
                refreshNeedUpdateDevicesBQ();
            }
            return false;
        }
    });
    private final List<BluetoothDevice> needActiveUpdateBQDevices = new ArrayList<>();
    private final static int UPDATE_TIME = 1000;
    private final static int FAIL_RETRY_TIME = 500;
    private final int MSG_UPDATE_DEVICE_BATTERY = 0x1237;

    //todo 要不要做息屏不同步电量或者onPause不同步电量
    public ActiveUpdateDeviceBQTask() {
        BluetoothHelper.getInstance().registerBTEventCallback(this);
    }

    @Override
    public void onConnection(BluetoothDevice device, int status) {
        super.onConnection(device, status);
        if (status == StateCode.CONNECTION_OK) {
            updateDeviceADVInfo(device);
            checkIsNeedActiveUpdateDevBQ(device);
        } else if (status == StateCode.CONNECTION_DISCONNECT) {
            stopNeedActiveUpdateDevBQ(device);
        }
    }

    private void updateDeviceADVInfo(BluetoothDevice device) {
        if (isCanUseTws(device)) {
            mBluetoothHelper.sendCommand(CommandBuilder.buildGetADVInfoCmdWithAll(), null);
        }
    }

    private void checkIsNeedActiveUpdateDevBQ(BluetoothDevice device) {
        if (!isCanUseTws(device)) return;
        if (getDeviceInfo(device) != null && getDeviceInfo(device).getSdkType() > JLChipFlag.JL_CHIP_FLAG_693X_TWS_HEADSET) {
            return;
        }
        if (needActiveUpdateBQDevices.contains(device)) return;
        needActiveUpdateBQDevices.add(device);
        boolean isStartUpdateDevBQ = mHandler.hasMessages(MSG_UPDATE_DEVICE_BATTERY);
        if (!isStartUpdateDevBQ)
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEVICE_BATTERY, UPDATE_TIME);
    }

    private void stopNeedActiveUpdateDevBQ(BluetoothDevice device) {
        needActiveUpdateBQDevices.remove(device);
        if (needActiveUpdateBQDevices.isEmpty())
            mHandler.removeMessages(MSG_UPDATE_DEVICE_BATTERY);
    }

    private boolean isCanUseTws(BluetoothDevice device) {
        return getDeviceInfo(device) != null && UIHelper.isCanUseTwsCmd(getDeviceInfo(device).getSdkType());
    }

    private DeviceInfo getDeviceInfo(BluetoothDevice device) {
        return mBluetoothHelper.getDeviceInfo(device);
    }

    private void refreshNeedUpdateDevicesBQ() {
        //todo 正在刷新的时候就不应该再刷新
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEVICE_BATTERY, UPDATE_TIME);
        if (needActiveUpdateBQDevices.isEmpty()) return;
        for (BluetoothDevice needUpdateDev : needActiveUpdateBQDevices) {
            activeUpdateDeviceBatteryQuantity(needUpdateDev);
        }
    }

    private void activeUpdateDeviceBatteryQuantity(BluetoothDevice bluetoothDevice) {
        if (!mBluetoothHelper.isConnectedDevice(bluetoothDevice) || !isCanUseTws(bluetoothDevice)) {
            stopNeedActiveUpdateDevBQ(bluetoothDevice);
            return;
        }
        mBluetoothHelper.sendCommand(bluetoothDevice, CommandBuilder.buildGetADVInfoCmd(0x41), new RcspCommandCallback() {
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
            }
        });
    }
}
