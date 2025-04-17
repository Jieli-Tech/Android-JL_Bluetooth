package com.jieli.btsmart.ui.ota;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.tool.upgrade.OTAManager;
import com.jieli.btsmart.ui.base.bluetooth.BluetoothBasePresenter;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.FileUtil;
import com.jieli.component.utils.SystemUtil;
import com.jieli.jl_bt_ota.constant.BluetoothConstant;
import com.jieli.jl_bt_ota.constant.ErrorCode;
import com.jieli.jl_bt_ota.interfaces.IUpgradeCallback;
import com.jieli.jl_bt_ota.model.base.BaseError;
import com.jieli.jl_http.JL_HttpClient;
import com.jieli.jl_http.bean.OtaMessage;
import com.jieli.jl_http.interfaces.IActionListener;
import com.jieli.jl_http.interfaces.IDownloadListener;
import com.jieli.jl_http.util.Constant;

/**
 * OTA逻辑实现
 *
 * @author zqjasonZhong
 * @since 2020/5/19
 */
public class OtaPresenterImpl extends BluetoothBasePresenter implements IOtaContract.IOtaPresenter, DevicePopDialogFilter.IgnoreFilter {
    private final static String TAG = "zzc_ota";
    private final IOtaContract.IOtaView mView;
    private final DeviceAddrManager mDeviceAddrManager;

    private OtaMessage mOtaMessage;
    private String upgradeFilePath;
    private int mUpgradeState = -1;
    private int sdkType = 0;

    private final OTAManager mOTAManager;
    private final MainApplication mApplication;
    private final Handler mHandler;

    public OtaPresenterImpl(IOtaContract.IOtaView view) {
        super(view);
        mView = SystemUtil.checkNotNull(view);
        mApplication = MainApplication.getApplication();
        if (!DeviceAddrManager.isInit()) {
            DeviceAddrManager.init(mApplication);
        }
        mDeviceAddrManager = DeviceAddrManager.getInstance();
        mOTAManager = new OTAManager(mApplication);
        mHandler = new Handler(Looper.getMainLooper());
    }


    private void destroyOTAManager() {
        mOTAManager.release();
    }

    @Override
    public OtaMessage getOtaMessage() {
        return mOtaMessage;
    }

    @Override
    public String getUpgradeFilePath() {
        if (upgradeFilePath == null) {
            upgradeFilePath = mOTAManager.getBluetoothOption().getFirmwareFilePath();
        }
        return upgradeFilePath;
    }

    @Override
    public void checkFirmwareOtaService(String authKey, String projectCode) {
        checkFirmwareOtaService(authKey, projectCode, null);
    }

    @Override
    public void checkFirmwareOtaService(String authKey, String projectCode, String md5) {
        JL_Log.i(TAG, "checkFirmwareOtaService >> authKey : " + authKey + ", projectCode : " + projectCode + ", md5 : " + md5);
        if (!TextUtils.isEmpty(md5)) {
            JL_HttpClient.getInstance().requestOTAFileByV2(authKey, projectCode, Constant.PLATFORM_FIRMWARE, md5, mOtaMessageListener);
        } else {
            JL_HttpClient.getInstance().requestOTAFile(authKey, projectCode, Constant.PLATFORM_FIRMWARE, mOtaMessageListener);
        }
    }

    @Override
    public boolean judgeDeviceNeedToOta(BluetoothDevice device, OtaMessage message) {
        return mOTAManager.judgeDeviceNeedToOta(device, message);
    }

    @Override
    public void downloadFile(String url, String saveFilePath) {
        if (url == null || saveFilePath == null) return;
        JL_HttpClient.getInstance().downloadFile(url, saveFilePath, mDownloadListener);
    }

    @Override
    public boolean isFirmwareOta() {
        return mOTAManager.isOTA();
    }

    @Override
    public void startFirmwareOta(String filePath) {
        if (filePath == null || !FileUtil.checkFileExist(filePath)) {
            mView.onOtaError(ErrorCode.SUB_ERR_FILE_NOT_FOUND, AppUtil.getContext().getString(R.string.ota_error_msg_file_not_exist));
            return;
        }
        upgradeFilePath = filePath;
        JL_Log.i(TAG, "startFirmwareOta >> " + filePath);
        mOTAManager.getBluetoothOption().setFirmwareFilePath(filePath);

        mOTAManager.startOTA(mIUpgradeCallback);
    }

    @Override
    public void cancelFirmwareOta() {
        mOTAManager.cancelOTA();
    }

    @Override
    public void disconnectDevice() {
        final BluetoothDevice device = getConnectedDevice();
        if(null == device) return;
        DevicePopDialogFilter.getInstance().addIgnoreDevice(device.getAddress());
        getRCSPController().disconnectDevice(device);
    }

    @Override
    public void setUpgradeState(int state) {
        mUpgradeState = state;
    }

    @Override
    public void destroy() {
        sdkType = 0;
        mApplication.setNeedBanDialog(false);
        mApplication.setOTA(false);
        destroyRCSPController(null);
        destroyOTAManager();
        mHandler.removeCallbacksAndMessages(null);
        DevicePopDialogFilter.getInstance().removeIgnoreFilter(this);
    }

    @Override
    public void start() {
        DevicePopDialogFilter.getInstance().addIgnoreFilter(this);
        mApplication.setNeedBanDialog(true);
    }


    private void changeDeviceStatus() {
        if (mRCSPController.isDeviceConnected()) {
            BluetoothDevice connectedDevice = getConnectedDevice();
            DeviceInfo deviceInfo = getDeviceInfo(connectedDevice);
            if (deviceInfo != null && !deviceInfo.isSupportDoubleBackup()) {
                int otaWay = deviceInfo.getSingleBackupOtaWay();
                if (BluetoothUtil.isUseBle(getBtOp().getBluetoothOption(), connectedDevice)) {
                    if (otaWay == Constants.SINGLE_BACKUP_OTA_WAY_SPP) {
                        if (BluetoothAdapter.checkBluetoothAddress(deviceInfo.getEdrAddr())) {
                            mDeviceAddrManager.updateHistoryBtDeviceInfo(connectedDevice, BluetoothConstant.PROTOCOL_TYPE_SPP, deviceInfo.getEdrAddr());
                        }
                    }
                } else {
                    if (otaWay == Constants.SINGLE_BACKUP_OTA_WAY_BLE) {
                        String bleAddr = mDeviceAddrManager.getDeviceAddr(connectedDevice.getAddress());
                        if (!BluetoothAdapter.checkBluetoothAddress(bleAddr)) {
                            bleAddr = deviceInfo.getBleAddr();
                        }
                        if (BluetoothAdapter.checkBluetoothAddress(bleAddr)) {
                            mDeviceAddrManager.updateHistoryBtDeviceInfo(connectedDevice, BluetoothConstant.PROTOCOL_TYPE_BLE, bleAddr);
                        }
                    }
                }
            }
        }
    }

    private final IActionListener<OtaMessage> mOtaMessageListener = new IActionListener<OtaMessage>() {
        @Override
        public void onSuccess(OtaMessage response) {
            JL_Log.i(TAG, "onSuccess >> " + response);
            mOtaMessage = response;
            mView.onOtaMessageChange(response);
        }

        @Override
        public void onError(int code, String message) {
            JL_Log.e(TAG, "onError >> " + code + ", " + message);
            mView.onGetOtaMessageError(code, message);
        }
    };

    private final IDownloadListener mDownloadListener = new IDownloadListener() {
        @Override
        public void onStart(String startPath) {
            mView.onDownloadStart(startPath);
        }

        @Override
        public void onProgress(float progress) {
            mView.onDownloadProgress(progress);
        }

        @Override
        public void onStop(String outputPath) {
            upgradeFilePath = outputPath;
            mView.onDownloadStop(outputPath);
        }

        @Override
        public void onError(int code, String message) {
            mView.onDownloadError(code, message);
        }
    };

    private final IUpgradeCallback mIUpgradeCallback = new IUpgradeCallback() {
        @Override
        public void onStartOTA() {
            mApplication.setOTA(true);
            if (getDeviceInfo() != null) {
                sdkType = getDeviceInfo().getSdkType();
            }
            if (PlayControlImpl.getInstance().isPlay()) {
                PlayControlImpl.getInstance().pause();
            }
            changeDeviceStatus();
            mView.onOtaStart();
        }

        @Override
        public void onNeedReconnect(String addr, boolean isNewReconnectWay) {
            JL_Log.i(TAG, "-onNeedReconnect- addr : " + addr);
        }

        @Override
        public void onProgress(int type, float progress) {
            mView.onOtaProgress(type, progress);
        }

        @Override
        public void onStopOTA() {
            mApplication.setOTA(false);
            upgradeFilePath = null;
            mView.onOtaStop();
            if (mUpgradeState == 0 && sdkType == JLChipFlag.JL_CHIP_FLAG_697X_TWS_HEADSET) {
                AppUtil.getContext().sendBroadcast(new Intent(SConstant.ACTION_FAST_CONNECT));
                mHandler.postDelayed(() -> {
                    sdkType = 0;
                    mUpgradeState = -1;
                }, 500);
            }
        }

        @Override
        public void onCancelOTA() {
            mApplication.setOTA(false);
            upgradeFilePath = null;
            sdkType = 0;
            mUpgradeState = -1;
            mView.onOtaCancel();
        }

        @Override
        public void onError(BaseError error) {
            if (error == null) return;
            if (ErrorCode.SUB_ERR_OTA_IN_HANDLE != error.getSubCode()) {
                sdkType = 0;
                mUpgradeState = -1;
                mApplication.setOTA(false);
                mView.onOtaError(error.getSubCode(), error.getMessage());
            }
        }
    };

    @Override
    public boolean shouldIgnore(BleScanMessage bleScanMessage) {
        return true;
    }
}
