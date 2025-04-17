package com.jieli.btsmart.tool.upgrade;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.response.TargetInfoResponse;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.BluetoothCallbackImpl;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.tool.ParseHelper;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.FileUtil;
import com.jieli.jl_bt_ota.constant.BluetoothConstant;
import com.jieli.jl_bt_ota.constant.ErrorCode;
import com.jieli.jl_bt_ota.constant.StateCode;
import com.jieli.jl_bt_ota.impl.BluetoothOTAManager;
import com.jieli.jl_bt_ota.interfaces.IUpgradeCallback;
import com.jieli.jl_bt_ota.model.BluetoothOTAConfigure;
import com.jieli.jl_bt_ota.model.base.BaseError;
import com.jieli.jl_http.bean.OtaMessage;

import java.util.UUID;

/**
 * OTA实现类
 *
 * @author zqjasonZhong
 * @since 2020/9/16
 */
public class OTAManager extends BluetoothOTAManager {

    private final RCSPController mRCSPController = RCSPController.getInstance();

    private BluetoothDevice mTargetDevice; //目标设备

    private final ReconnectAddress mReconnectAddress = new ReconnectAddress(); //回连设备信息

    public OTAManager(Context context) {
        super(context);
        mRCSPController.getBtOperation().registerBluetoothCallback(mBluetoothCallback);
        configureOTA();
        if (mRCSPController.isDeviceConnected()) {
            BluetoothDevice device = mRCSPController.getUsingDevice();
            setTargetDevice(device);
            onBtDeviceConnection(device, StateCode.CONNECTION_OK);
            if (mRCSPController.getBtOperation().isConnectedBLEDevice(device)) {
                onMtuChanged(mRCSPController.getBtOperation().getDeviceGatt(device), mRCSPController.getBtOperation().getBleMtu(device), BluetoothGatt.GATT_SUCCESS);
            }
        }
    }

    @Override
    public BluetoothDevice getConnectedDevice() {
        return mTargetDevice;
    }

    @Override
    public BluetoothGatt getConnectedBluetoothGatt() {
        return mRCSPController.getBtOperation().getConnectedBluetoothGatt();
    }

    @Override
    public void connectBluetoothDevice(BluetoothDevice device) {
        if (null == device) return;
        if (mReconnectAddress.isNeedRecordOtaAddress()) {
            mReconnectAddress.setOtaDeviceAddress(device.getAddress());
            DeviceAddrManager.getInstance().updateHistoryOtaAddress(mReconnectAddress.getSrcDeviceAddress(), device.getAddress());
        }
        mRCSPController.connectDevice(device);
    }

    @Override
    public void disconnectBluetoothDevice(BluetoothDevice device) {
        if (null == device) return;
        DevicePopDialogFilter.getInstance().addIgnoreDevice(device.getAddress());
        mRCSPController.disconnectDevice(device);
    }

    @Override
    public boolean sendDataToDevice(BluetoothDevice device, byte[] data) {
        return mRCSPController.sendDataToDevice(device, data);
    }

    @Override
    public void errorEventCallback(BaseError error) {

    }

    @Override
    public void startOTA(IUpgradeCallback callback) {
        setTargetDevice(mRCSPController.getUsingDevice());
        super.startOTA(new IUpgradeCallback() {
            @Override
            public void onStartOTA() {
                if (callback != null) callback.onStartOTA();
            }

            @Override
            public void onNeedReconnect(String s, boolean b) {
                mReconnectAddress.setNeedRecordOtaAddress(b).setSrcDeviceAddress(s)
                        .setMappedDeviceAddress(mRCSPController.getMappedDeviceAddress(s));
                if (callback != null) callback.onNeedReconnect(s, b);
            }

            @Override
            public void onProgress(int i, float v) {
                if (callback != null) callback.onProgress(i, v);
            }

            @Override
            public void onStopOTA() {
                if(mReconnectAddress.isNeedRecordOtaAddress()){ //还原历史记录
                    DeviceAddrManager.getInstance().putDeviceAddr(mReconnectAddress.getSrcDeviceAddress(), mReconnectAddress.getMappedDeviceAddress());
                    DeviceAddrManager.getInstance().updateHistoryOtaAddress(mReconnectAddress.getSrcDeviceAddress(), "");
                }
                mReconnectAddress.reset();
                if (callback != null) callback.onStopOTA();
            }

            @Override
            public void onCancelOTA() {
                mReconnectAddress.reset();
                if (callback != null) callback.onCancelOTA();
            }

            @Override
            public void onError(BaseError baseError) {
                if (callback != null) callback.onError(baseError);
            }
        });
    }

    @Override
    public void release() {
        mReconnectAddress.reset();
        super.release();
        mRCSPController.getBtOperation().unregisterBluetoothCallback(mBluetoothCallback);
    }

    public boolean judgeDeviceNeedToOta(BluetoothDevice device, OtaMessage message) {
        if (device == null || message == null) return false;
        TargetInfoResponse targetInfo = mRCSPController.getDeviceInfo(device);
        if (targetInfo == null) {
            errorEventCallback(new BaseError(ErrorCode.SUB_ERR_REMOTE_NOT_CONNECTED, "device is not connected."));
            return false;
        }
        int versionCode = targetInfo.getVersionCode();
        int serverFirmware = ParseHelper.convertVersionByString(message.getVersion());
        JL_Log.i(TAG, "judgeDeviceNeedToOta:: versionCode : " + versionCode + ", sever firmware version : " + serverFirmware);
        return versionCode < serverFirmware || (serverFirmware == 0);
    }

    private void configureOTA() {
        BluetoothOTAConfigure configure = BluetoothOTAConfigure.createDefault();
        configure.setPriority(BluetoothOTAConfigure.PREFER_SPP)
                .setMtu(BluetoothConstant.BLE_MTU_MIN)
                .setUseReconnect(false)
                .setUseAuthDevice(false);
        String upgradeFilePath = FileUtil.createFilePath(MainApplication.getApplication(), MainApplication.getApplication().getPackageName(), SConstant.DIR_UPDATE);
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        String filePath;
        if (deviceInfo != null && deviceInfo.getSdkType() < JLChipFlag.JL_CHIP_FLAG_693X_TWS_HEADSET) {
//            upgradeFilePath += "/" + SConstant.FIRMWARE_UPGRADE_FILE;
            filePath = AppUtil.obtainUpdateFilePath(upgradeFilePath, ".bfu");
            if (filePath == null) {
                upgradeFilePath += "/" + SConstant.FIRMWARE_UPGRADE_FILE;
            } else {
                upgradeFilePath = filePath;
            }
        } else {
//            upgradeFilePath += "/" + SConstant.FIRMWARE_UPGRADE_FILE_AC693;
            filePath = AppUtil.obtainUpdateFilePath(upgradeFilePath, ".ufw");
            if (filePath == null) {
                upgradeFilePath += "/" + SConstant.FIRMWARE_UPGRADE_FILE_AC693;
            } else {
                upgradeFilePath = filePath;
            }
        }
        configure.setFirmwareFilePath(upgradeFilePath);
        configure(configure);
    }

    private void setTargetDevice(BluetoothDevice device) {
        mTargetDevice = device;
        if (device != null && BluetoothUtil.deviceEquals(device, mRCSPController.getUsingDevice())) {
            mRCSPController.switchUsingDevice(device);
        }
    }

    private final BluetoothCallbackImpl mBluetoothCallback = new BluetoothCallbackImpl() {

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            JL_Log.i(TAG, "onConnection ::: device : " + BluetoothUtil.printBtDeviceInfo(device) + ", status : " + status);
            if (status == StateCode.CONNECTION_OK) {
                if (mTargetDevice == null) {
                    setTargetDevice(device);
                }
                if (mRCSPController.getBtOperation().isConnectedBLEDevice(device)) {
                    int mtu = mRCSPController.getBtOperation().getBleMtu(device);
                    if (mRCSPController.getBtOperation().getDeviceGatt(device) != null) {
                        onMtuChanged(mRCSPController.getBtOperation().getDeviceGatt(device), mtu + 3, BluetoothGatt.GATT_SUCCESS);
                    }
                }
            }
            if (mTargetDevice == null || BluetoothUtil.deviceEquals(device, mTargetDevice)) {
                onBtDeviceConnection(device, status);
                if (status == StateCode.CONNECTION_FAILED || status == StateCode.CONNECTION_DISCONNECT) {
                    if (BluetoothUtil.deviceEquals(device, mTargetDevice)) {
                        setTargetDevice(null);
                    }
                }
            }
        }

        @Override
        public void onBleDataBlockChanged(BluetoothDevice device, int block, int status) {
            onMtuChanged(mRCSPController.getBtOperation().getDeviceGatt(device), block + 3, status);
        }

        @Override
        public void onBleDataNotification(BluetoothDevice device, UUID serviceUuid, UUID characteristicsUuid, byte[] data) {
            JL_Log.d(TAG, "onBleDataNotification ::: device : " + BluetoothUtil.printBtDeviceInfo(device) + ", data : " + CHexConver.byte2HexStr(data));
            if (DeviceAddrManager.getInstance().isMatchDevice(mTargetDevice, device)) {
                onReceiveDeviceData(device, data);
            }
        }


        @Override
        public void onSppDataNotification(BluetoothDevice device, byte[] data) {
            JL_Log.d(TAG, "onSppDataNotification ::: device : " + BluetoothUtil.printBtDeviceInfo(device) + ", data : " + CHexConver.byte2HexStr(data));
            if (DeviceAddrManager.getInstance().isMatchDevice(mTargetDevice, device)) {
                onReceiveDeviceData(device, data);
            }
        }
    };

    private static class ReconnectAddress {
        /**
         * 是否需要记录OTA地址
         */
        private boolean isNeedRecordOtaAddress;
        /**
         * 原设备地址
         */
        private String srcDeviceAddress;
        /**
         * 原设备映射地址
         */
        private String mappedDeviceAddress;
        /**
         * OTA设备地址
         */
        private String otaDeviceAddress;

        public boolean isNeedRecordOtaAddress() {
            return isNeedRecordOtaAddress;
        }

        public ReconnectAddress setNeedRecordOtaAddress(boolean needRecordOtaAddress) {
            isNeedRecordOtaAddress = needRecordOtaAddress;
            return this;
        }

        public String getSrcDeviceAddress() {
            return srcDeviceAddress;
        }

        public ReconnectAddress setSrcDeviceAddress(String srcDeviceAddress) {
            this.srcDeviceAddress = srcDeviceAddress;
            return this;
        }

        public String getOtaDeviceAddress() {
            return otaDeviceAddress;
        }

        public ReconnectAddress setOtaDeviceAddress(String otaDeviceAddress) {
            this.otaDeviceAddress = otaDeviceAddress;
            return this;
        }

        public String getMappedDeviceAddress() {
            return mappedDeviceAddress;
        }

        public ReconnectAddress setMappedDeviceAddress(String mappedDeviceAddress) {
            this.mappedDeviceAddress = mappedDeviceAddress;
            return this;
        }

        public void reset() {
            setNeedRecordOtaAddress(false).setSrcDeviceAddress("").setOtaDeviceAddress("").setMappedDeviceAddress("");
        }
    }
}
