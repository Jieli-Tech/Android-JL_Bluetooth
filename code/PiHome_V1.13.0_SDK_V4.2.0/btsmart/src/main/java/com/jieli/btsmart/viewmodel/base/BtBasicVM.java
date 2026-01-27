package com.jieli.btsmart.viewmodel.base;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.BluetoothOption;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.BluetoothOperationImpl;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.IBluetoothOperation;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.data.model.device.DeviceConnection;
import com.jieli.btsmart.util.PermissionUtil;

import java.util.List;

/**
 * 蓝牙操作基础类
 */
public class BtBasicVM extends BaseViewModel {
    protected final RCSPController mRCSPController = RCSPController.getInstance();
    public final MutableLiveData<Boolean> btAdapterMLD = new MutableLiveData<>(false);
    public final MutableLiveData<DeviceConnection> deviceConnectionMLD = new MutableLiveData<>();
    public final MutableLiveData<BluetoothDevice> switchDeviceMLD = new MutableLiveData<>();

    public BtBasicVM() {
        JL_Log.d(tag, "init", "clazz : " + this);
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
        IBluetoothOperation operation = mRCSPController.getBtOperation();
        if (null == operation) return null;
        return (BluetoothOperationImpl) operation;
    }

    public BluetoothOption getBluetoothOption() {
        return mRCSPController.getBluetoothOption();
    }

    public BluetoothDevice getConnectedDevice() {
        return mRCSPController.getUsingDevice();
    }

    public DeviceInfo getDeviceInfo() {
        return mRCSPController.getDeviceInfo();
    }

    public DeviceInfo getDeviceInfo(BluetoothDevice device) {
        if (null == device) return null;
        return mRCSPController.getDeviceInfo(device);
    }

    public DeviceInfo getDeviceInfo(String address) {
        return getDeviceInfo(BluetoothUtil.getRemoteDevice(address));
    }

    public ADVInfoResponse getADVInfo(BluetoothDevice device) {
        return mRCSPController.getADVInfo(device);
    }

    public HistoryBluetoothDevice findHistory(BluetoothDevice device) {
        return mRCSPController.findHistoryBluetoothDevice(device);
    }

    public boolean isDevConnected() {
        return mRCSPController.isDeviceConnected();
    }

    public boolean isDevConnecting() {
        return mRCSPController.isConnecting();
    }

    public boolean isConnectedDevice(BluetoothDevice device) {
        if (null == device) return false;
        return isConnectedDevice(device.getAddress());
    }

    public boolean isConnectedDevice(String addr) {
        if (!BluetoothAdapter.checkBluetoothAddress(addr)) return false;
        List<BluetoothDevice> connectedDevices = getBtManager().getConnectedDeviceList();
        if (connectedDevices == null || connectedDevices.isEmpty()) return false;
        for (BluetoothDevice device : connectedDevices) {
            if (null == device) continue;
            if (getBtManager().isMatchDevice(device.getAddress(), addr)) {
                return true;
            }
        }
        return false;
    }

    public boolean isConnectingDevice(BluetoothDevice device) {
        if (null == device) return false;
        BluetoothOperationImpl operation = getBtOp();
        if (null == operation) return false;
        boolean ret = BluetoothUtil.deviceEquals(operation.getConnectingDevice(), device);
        if (!ret) {
            ret = BluetoothUtil.deviceEquals(operation.getConnectingBrEdrDevice(), device);
        }
        return ret;
    }

    public boolean isUsingDevice(String addr) {
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

    public String getEdrAddress(@NonNull BluetoothDevice device) {
       BluetoothDevice edrDevice = mRCSPController.getBtOperation().getCacheEdrDevice(device);
        return edrDevice == null ? device.getAddress(): edrDevice.getAddress();
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
        JL_Log.d(tag, "release", "clazz : " + this);
        if (mRCSPController != null) {
            mRCSPController.removeBTRcspEventCallback(mEventCallback);
        }
    }

    protected String printDeviceInfo(BluetoothDevice device) {
        return BluetoothUtil.printBtDeviceInfo(getContext(), device);
    }

    private final BTRcspEventCallback mEventCallback = new BTRcspEventCallback() {
        @Override
        public void onAdapterStatus(boolean bEnabled, boolean bHasBle) {
            btAdapterMLD.setValue(bEnabled);
        }

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (null == device) return;
            deviceConnectionMLD.setValue(new DeviceConnection(device, status));
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            switchDeviceMLD.setValue(device);
        }
    };
}
