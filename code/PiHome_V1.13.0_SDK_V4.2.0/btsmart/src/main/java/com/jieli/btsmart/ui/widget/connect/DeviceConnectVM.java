package com.jieli.btsmart.ui.widget.connect;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.JL_DeviceType;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.device.ScanBtDevice;
import com.jieli.btsmart.tool.bluetooth.rcsp.BTRcspHelper;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 设备连接逻辑实现
 * @since 2023/8/23
 */
public class DeviceConnectVM extends BtBasicVM {

    public final MutableLiveData<StateResult<List<ScanBtDevice>>> scanStateMLD = new MutableLiveData<>();

    private final List<ScanBtDevice> foundDeviceList = new ArrayList<>();

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
        JL_Log.d(tag, "connectDevice", "" + scanBtDevice);
        BTRcspHelper.connectDeviceByMessage(mRCSPController, scanBtDevice.getDevice(), scanBtDevice.getBleScanMessage());
    }

    private boolean isHasScanBtDevice(ScanBtDevice scanBtDevice) {
        if (foundDeviceList.isEmpty()) {
            foundDeviceList.add(scanBtDevice);
            return false;
        }
        String deviceAddress = scanBtDevice.getDevice().getAddress();
        BleScanMessage bleScanMessage = scanBtDevice.getBleScanMessage();
        String edrAddress = null == bleScanMessage ? null : bleScanMessage.getEdrAddr();
        int deleteItemPos = -1;
        for (int i = 0; i < foundDeviceList.size(); i++) {
            ScanBtDevice cache = foundDeviceList.get(i);
            String cacheAddress = cache.getDevice().getAddress();
            BleScanMessage cacheAdvMsg = cache.getBleScanMessage();
            String advEdrAddress = null == cacheAdvMsg ? null : cacheAdvMsg.getEdrAddr();
            if (TextUtils.equals(deviceAddress, cacheAddress) || TextUtils.equals(deviceAddress, advEdrAddress)
                    || TextUtils.equals(edrAddress, cacheAddress) || TextUtils.equals(edrAddress, advEdrAddress)) {
                if (cacheAdvMsg == null && bleScanMessage != null) { //优先有广播包的
                    deleteItemPos = i;
                    break;
                }
                return true;
            }
        }
        if (deleteItemPos != -1) {
            foundDeviceList.remove(deleteItemPos);
            foundDeviceList.add(deleteItemPos, scanBtDevice);
        } else {
            foundDeviceList.add(scanBtDevice);
        }
        return false;
    }

    private void postFoundDevice(@NonNull ScanBtDevice scanBtDevice) {
        BluetoothDevice device = scanBtDevice.getDevice();
        BleScanMessage bleScanMessage = scanBtDevice.getBleScanMessage();
        //过滤手表设备的广播包
        if (bleScanMessage != null && bleScanMessage.getDeviceType() == JL_DeviceType.JL_DEVICE_TYPE_WATCH) {
            JL_Log.d(tag, "postFoundDevice", "filter watch device...");
            return;
        }
        if (isConnectedDevice(device)) {
            JL_Log.d(tag, "postFoundDevice", "Device is connected.");
            return;
        }
        boolean ret = isHasScanBtDevice(scanBtDevice);
        JL_Log.d(tag, "postFoundDevice", ret + ", foundDeviceList size : " + foundDeviceList.size() + "\n" + scanBtDevice);
        if (ret) return; //有重复记录
        scanStateMLD.setValue(new StateResult<List<ScanBtDevice>>()
                .setState(StateResult.STATE_WORKING)
                .setCode(0)
                .setData(new ArrayList<>(foundDeviceList)));
    }

    private void syncSystemBtDevice() {
        if (!CommonUtil.checkHasConnectPermission(getContext())) return;
        List<BluetoothDevice> sysConnectedBtDeviceList = BluetoothUtil.getSystemConnectedBtDeviceList(getContext());
        if (sysConnectedBtDeviceList.isEmpty()) return;
        for (BluetoothDevice device : sysConnectedBtDeviceList) {
            if (!isConnectedDevice(device) && !isConnectingDevice(device)) {
                final ParcelUuid[] uuids = device.getUuids();
                if (null == uuids || uuids.length == 0) continue;
                boolean isTargetDevice = false;
                for (ParcelUuid parcelUuid : uuids) {
                    UUID uuid = parcelUuid.getUuid();
                    if (BluetoothConstant.UUID_SPP.equals(uuid) || BluetoothConstant.UUID_SERVICE.equals(uuid)) {
                        isTargetDevice = true;
                        break;
                    }
                }
                if (isTargetDevice) {
                    postFoundDevice(new ScanBtDevice(device, JL_DeviceType.JL_DEVICE_TYPE_SOUNDBOX, null));
                }
            }
        }
    }

    private final BTRcspEventCallback mBTRcspEventCallback = new BTRcspEventCallback() {
        @Override
        public void onDiscoveryStatus(boolean bBle, boolean bStart) {
            scanStateMLD.setValue(new StateResult<List<ScanBtDevice>>()
                    .setState(bStart ? StateResult.STATE_WORKING : StateResult.STATE_IDLE)
                    .setCode(0));
            if (bStart) {
                foundDeviceList.clear();
                syncSystemBtDevice();
            }
        }

        @Override
        public void onDiscovery(BluetoothDevice device, BleScanMessage bleScanMessage) {
            if (null == device) return;
            postFoundDevice(new ScanBtDevice(device, bleScanMessage == null ? JL_DeviceType.JL_DEVICE_TYPE_SOUNDBOX
                    : bleScanMessage.getDeviceType(), bleScanMessage));
        }
    };
}
