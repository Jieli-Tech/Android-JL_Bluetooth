package com.jieli.btsmart.tool.bluetooth;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.interfaces.IActionCallback;
import com.jieli.bluetooth.tool.DeviceAddrManager;

/**
 * 移除历史记录监听
 *
 * @author zqjasonZhong
 * @since 2020/5/30
 */
public class OnHistoryBtDeviceCallback implements IActionCallback<HistoryBluetoothDevice> {
    private final BTEventCallbackManager mCallbackManager;
    private final IActionCallback<HistoryBluetoothDevice> mCallback;

    public OnHistoryBtDeviceCallback(BTEventCallbackManager callbackManager, IActionCallback<HistoryBluetoothDevice> callback) {
        mCallbackManager = callbackManager;
        mCallback = callback;
    }

    @Override
    public void onSuccess(HistoryBluetoothDevice message) {
        if (mCallback != null) {
            mCallback.onSuccess(message);
        }
        DeviceAddrManager deviceAddrManager = DeviceAddrManager.getInstance();
        String cacheBleAdr = deviceAddrManager.getCacheBleDeviceAddr();
        if (deviceAddrManager.isMatchDevice(cacheBleAdr, message.getAddress())) {
            deviceAddrManager.removeCacheBluetoothDeviceAddr();
        } else {
            deviceAddrManager.removeDeviceAddr(message.getAddress());
        }
        if (mCallbackManager != null) {
            mCallbackManager.onRemoveHistoryDeviceSuccess(message);
        }
    }

    @Override
    public void onError(BaseError error) {
        if (mCallback != null) {
            mCallback.onError(error);
        }
        if (mCallbackManager != null) {
            mCallbackManager.onRemoveHistoryDeviceFailed(error);
        }
    }
}
