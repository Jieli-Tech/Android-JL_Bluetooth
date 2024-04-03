package com.jieli.btsmart.ui.widget.connect;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.constant.JL_DeviceType;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.device.ScanBtDevice;
import com.jieli.btsmart.tool.bluetooth.rcsp.BTRcspHelper;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 设备连接逻辑实现
 * @since 2023/8/23
 */
public class DeviceConnectVM extends BtBasicVM {

    public final MutableLiveData<StateResult<ScanBtDevice>> scanStateMLD = new MutableLiveData<>();

    public DeviceConnectVM() {
        mRCSPController.addBTRcspEventCallback(mBTRcspEventCallback);
    }

    @Override
    protected void release() {
        stopScan();
        super.release();
        mRCSPController.removeBTRcspEventCallback(mBTRcspEventCallback);
    }

    public boolean isScanning() {
        return mRCSPController.isScanning();
    }

    public boolean startScan() {
        return mRCSPController.startBleScan(SConstant.SCAN_TIME);
    }

    public void stopScan() {
        mRCSPController.stopScan();
    }

    public void connectDevice(ScanBtDevice scanBtDevice) {
        if (null == scanBtDevice) return;
        BTRcspHelper.connectDeviceByMessage(mRCSPController, MainApplication.getApplication(),
                scanBtDevice.getDevice(), scanBtDevice.getBleScanMessage());
    }

    private final BTRcspEventCallback mBTRcspEventCallback = new BTRcspEventCallback() {
        @Override
        public void onDiscoveryStatus(boolean bBle, boolean bStart) {
            scanStateMLD.postValue(new StateResult<ScanBtDevice>()
                    .setState(bStart ? StateResult.STATE_WORKING : StateResult.STATE_IDLE)
                    .setCode(0));
        }

        @Override
        public void onDiscovery(BluetoothDevice device, BleScanMessage bleScanMessage) {
            if (null == device) return;
            if (PermissionUtil.checkHasConnectPermission(MainApplication.getApplication())) {
                //过滤手表设备的广播包
                if (bleScanMessage != null && bleScanMessage.getDeviceType() == JL_DeviceType.JL_DEVICE_TYPE_WATCH) {
                    JL_Log.d(tag, "[onDiscovery] filter watch device...");
                    return;
                }
                if (getConnectedDevice() != null && getBtManager().isMatchDevice(device.getAddress(), getConnectedDevice().getAddress())) {
                    JL_Log.d(tag, "[onDiscovery] Device is connected.");
                    return;
                }
                scanStateMLD.setValue(new StateResult<ScanBtDevice>().setState(StateResult.STATE_WORKING)
                        .setCode(0)
                        .setData(new ScanBtDevice(device, bleScanMessage == null ?
                                JL_DeviceType.JL_DEVICE_TYPE_SOUNDBOX : bleScanMessage.getDeviceType(), bleScanMessage)));
            }
        }
    };
}
