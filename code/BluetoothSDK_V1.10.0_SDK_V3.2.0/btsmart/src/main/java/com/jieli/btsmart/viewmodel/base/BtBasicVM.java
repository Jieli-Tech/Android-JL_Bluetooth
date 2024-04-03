package com.jieli.btsmart.viewmodel.base;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.BluetoothOption;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.BluetoothOperationImpl;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.util.PermissionUtil;

import java.util.List;

/**
 * 蓝牙操作基础类
 */
public class BtBasicVM extends BaseViewModel {
    protected final RCSPController mRCSPController = RCSPController.getInstance();
    public final MutableLiveData<Boolean> btAdapterMLD = new MutableLiveData<>(false);
    public final MutableLiveData<DeviceConnectionData> deviceConnectionMLD = new MutableLiveData<>();
    public final MutableLiveData<BluetoothDevice> switchDeviceMLD = new MutableLiveData<>();

    public BtBasicVM() {
        mRCSPController.addBTRcspEventCallback(mEventCallback);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        release();
    }

    public JL_BluetoothManager getBtManager() {
        return mRCSPController.getBluetoothManager();
    }

    public BluetoothOperationImpl getBtOp() {
        return (BluetoothOperationImpl) getBtManager().getBluetoothOperation();
    }

    public BluetoothOption getBluetoothOption() {
        return getBtManager().getBluetoothOption();
    }

    public BluetoothDevice getConnectedDevice() {
        return mRCSPController.getUsingDevice();
    }

    public DeviceInfo getDeviceInfo() {
        return mRCSPController.getDeviceInfo();
    }

    public DeviceInfo getDeviceInfo(String address){
        BluetoothDevice device = BluetoothUtil.getRemoteDevice(address);
        if(null == device) return null;
        return mRCSPController.getDeviceInfo(device);
    }

    public ADVInfoResponse getADVInfo(BluetoothDevice device){
        return mRCSPController.getADVInfo(device);
    }

    public boolean isDevConnected() {
        return mRCSPController.isDeviceConnected();
    }

    public boolean isDevConnecting() {
        return mRCSPController.isConnecting();
    }

    public boolean isConnectedDevice(String addr) {
        if(!BluetoothAdapter.checkBluetoothAddress(addr)) return false;
        List<BluetoothDevice> connectedDevices = getBtManager().getConnectedDeviceList();
        if(connectedDevices == null || connectedDevices.isEmpty()) return false;
        for (BluetoothDevice device : connectedDevices){
            if(null == device) continue;
            if(getBtManager().isMatchDevice(device.getAddress(), addr)){
                return true;
            }
        }
        return false;
    }

    public boolean isConnectingDevice(BluetoothDevice device) {
        if (null == device) return false;
        boolean ret = BluetoothUtil.deviceEquals(getBtOp().getConnectingDevice(), device);
        if (!ret) {
            ret = BluetoothUtil.deviceEquals(getBtOp().getConnectingBrEdrDevice(), device);
        }
        return ret;
    }

    public boolean isUsingDevice(String addr){
        boolean isConnected = false;
        if (addr != null && getConnectedDevice() != null) {
            isConnected = getBtManager().isMatchDevice(getConnectedDevice().getAddress(), addr);
        }
        return isConnected;
    }

    public int getDeviceConnection(BluetoothDevice device) {
        if (null == device) return -1;
        if (isConnectedDevice(device.getAddress())) {
            return StateCode.CONNECTION_OK;
        } else if (isConnectingDevice(device)) {
            return StateCode.CONNECTION_CONNECTING;
        } else {
            return StateCode.CONNECTION_DISCONNECT;
        }
    }

    public BluetoothDevice getMappedEdrDevice(BluetoothDevice device) {
        if (!PermissionUtil.checkHasConnectPermission(MainApplication.getApplication()))
            return null;
        if (null == device) return null;
        BluetoothDevice edrDevice;
        if (device.getType() != BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            String edrAddr = getBtManager().getMappedDeviceAddress(device.getAddress());
            if (BluetoothAdapter.checkBluetoothAddress(edrAddr)) {
                edrDevice = BluetoothUtil.getRemoteDevice(edrAddr);
            } else {
                edrDevice = null;
            }
        } else {
            edrDevice = device;
        }
        return edrDevice;
    }

    protected void release() {
        if (mRCSPController != null) {
            mRCSPController.removeBTRcspEventCallback(mEventCallback);
        }
    }

    private final BTRcspEventCallback mEventCallback = new BTRcspEventCallback() {
        @Override
        public void onAdapterStatus(boolean bEnabled, boolean bHasBle) {
            btAdapterMLD.setValue(bEnabled);
        }

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (null == device) return;
            deviceConnectionMLD.setValue(new DeviceConnectionData(device, status));
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            switchDeviceMLD.setValue(device);
        }
    };

    public static class DeviceConnectionData {
        @NonNull
        final BluetoothDevice device;
        final int status;

        public DeviceConnectionData(@NonNull BluetoothDevice device, int status) {
            this.device = device;
            this.status = status;
        }

        @NonNull
        public BluetoothDevice getDevice() {
            return device;
        }

        public int getStatus() {
            return status;
        }
    }
}
