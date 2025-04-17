package com.jieli.btsmart.ui.device;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.command.tws.NotifyAdvInfoCmd;
import com.jieli.bluetooth.bean.command.tws.RequestAdvOpCmd;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.parameter.NotifyAdvInfoParam;
import com.jieli.bluetooth.bean.parameter.RequestAdvOpParam;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.IActionCallback;
import com.jieli.bluetooth.interfaces.IScreenEventListener;
import com.jieli.bluetooth.interfaces.OnReconnectHistoryRecordListener;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.bluetooth.utils.PreferencesHelper;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.device.HistoryDevice;
import com.jieli.btsmart.tool.bluetooth.rcsp.BTRcspHelper;
import com.jieli.btsmart.tool.network.NetworkHelper;
import com.jieli.btsmart.tool.permission.PermissionsHelper;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.ui.base.bluetooth.BluetoothBasePresenter;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.ScreenEventManager;
import com.jieli.btsmart.util.UIHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * 设备连接逻辑实现
 *
 * @author zqjasonZhong
 * @since 2020/5/14
 */
public class DeviceConnectPresenterImpl extends BluetoothBasePresenter implements IDeviceConnectContract.IDeviceConnectPresenter {
    private final static String TAG = DeviceConnectPresenterImpl.class.getSimpleName();
    private final IDeviceConnectContract.IDeviceConnectView mView;

    private final MainApplication mApplication;
    private final NetworkHelper mNetworkHelper;
    private final PermissionsHelper mPermissionsHelper;
    private final AppCompatActivity mActivity;
    private final ScreenEventManager mScreenEventManager;

    private BluetoothDevice mConnectingEdrDevice;

    private final HashMap<String, ADVInfoResponse> mADVInfoResponseMap = new HashMap<>();
    private volatile boolean isBqUpdating = false;
    private volatile boolean isBanShowDialog; //主动禁止弹窗
    private volatile boolean isBanBtScan;
    private volatile boolean isBanCmdShowDialog; //根据命令区分是否需要关闭广播命令推送
    private volatile int tempBanCmdShowDialog = -1;

    public final static int DISCOVERY_STATUS_IDLE = 0;
    public final static int DISCOVERY_STATUS_WORKING = 1;
    public final static int DISCOVERY_STATUS_FOUND = 2;

    private final static int UPDATE_TIME = 1000;
    private final static int FAIL_RETRY_TIME = 500;

    private final static int SCAN_BT_DEVICE_DELAY = 3 * 1000; //3s

    private final static int MSG_CONNECT_EDR_TIMEOUT = 0x1234;
    private final static int MSG_SCAN_BT_DEVICE = 0x1235;
    private final static int MSG_UPDATE_DEVICE_BATTERY = 0x1237;
    private final static int MSG_RESET_SHOW_DIALOG_FLAG = 0x1238;
    private final static int MSG_FAST_CONNECT = 0x1239;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_CONNECT_EDR_TIMEOUT:
                    JL_Log.w(TAG, "connect edr timeout.");
                    if (mView != null) {
                        mView.onDeviceConnection(mConnectingEdrDevice, StateCode.CONNECTION_FAILED);
                    }
                    setConnectingEdrDevice(null);
                    break;
                case MSG_SCAN_BT_DEVICE:
                    if (!isBanBtScan) {
                        startScan();
                    }
                    break;
                case MSG_UPDATE_DEVICE_BATTERY:
                    if (!isBqUpdating) {
                        isBqUpdating = true;
                        mView.onDeviceBQStatus(isBqUpdating);
                    }
                    updateDeviceBatteryQuantity();
                    break;
                case MSG_RESET_SHOW_DIALOG_FLAG:
                    resetShowDialogFlag();
                    break;
                case MSG_FAST_CONNECT:
                    getBtManager().fastConnect();
                    break;
            }
            return false;
        }
    });

    public DeviceConnectPresenterImpl(AppCompatActivity activity, IDeviceConnectContract.IDeviceConnectView view) {
        super(view);
        mView = CommonUtil.checkNotNull(view);

        mActivity = activity;
        mApplication = MainApplication.getApplication();
        mNetworkHelper = NetworkHelper.getInstance();
        mNetworkHelper.registerNetworkEventCallback(mNetworkEventCallback);
        getRCSPController().addBTRcspEventCallback(mEventCallback);
        mPermissionsHelper = new PermissionsHelper(mActivity);
        mPermissionsHelper.setOnPermissionListener(new PermissionsHelper.OnPermissionListener() {
            @Override
            public void onPermissionsSuccess(String[] permissions) {
                mView.onPermissionSuccess(permissions);
            }

            @Override
            public void onPermissionFailed(String permission) {
                mView.onPermissionFailed(permission);
            }
        });
        mScreenEventManager = ScreenEventManager.getInstance();
        mScreenEventManager.registerScreenEventListener(mIScreenEventListener);
    }

    @Override
    public boolean isAppGrantPermissions() {
        return PermissionsHelper.checkAppPermissionsIsAllow(mActivity);
    }

    @Override
    public void checkAppPermissions() {
        if (mPermissionsHelper != null) {
            mPermissionsHelper.checkAppRequestPermissions(PermissionsHelper.sPermissions);
        }
    }

    @Override
    public String getPermissionName(String permission) {
        if (mPermissionsHelper == null) return null;
        return mPermissionsHelper.getPermissionName(permission);
    }

    @Override
    public void checkNetworkAvailable() {
        if (mNetworkHelper != null) {
            mNetworkHelper.checkNetworkIsAvailable();
        }
    }

    @Override
    public List<HistoryBluetoothDevice> getHistoryBtDeviceList() {
        List<HistoryBluetoothDevice> list = getBtManager().getHistoryBluetoothDeviceList();
        if (list != null) {
            list = new ArrayList<>(list);
            Collections.reverse(list);
        }
        return list;
    }

    @Override
    public int getHistoryDeviceState(HistoryBluetoothDevice historyBtDevice) {
        int state = HistoryDevice.STATE_DISCONNECT;
        BluetoothDevice device = BluetoothUtil.getRemoteDevice(historyBtDevice.getAddress());
        if (null == device) return state;
        if (isConnectedDevice(device)) {
            state = HistoryDevice.STATE_CONNECTED;
            DeviceInfo deviceInfo = getDeviceInfo(device);
            if (deviceInfo != null && deviceInfo.isMandatoryUpgrade()) {
                state = HistoryDevice.STATE_NEED_OTA;
            }
        } else if (isConnectingDevice(device.getAddress())) {
            state = HistoryDevice.STATE_CONNECTING;
        }
        return state;
    }

    @Override
    public void removeHistoryBtDevice(HistoryBluetoothDevice historyBtDevice, IActionCallback<HistoryBluetoothDevice> callback) {
        getRCSPController().removeHistoryBtDevice(historyBtDevice, new IActionCallback<HistoryBluetoothDevice>() {
            @Override
            public void onSuccess(HistoryBluetoothDevice message) {
                mView.onRemoveHistoryDeviceSuccess(message);
                if (callback != null) callback.onSuccess(message);
            }

            @Override
            public void onError(BaseError error) {
                mView.onRemoveHistoryDeviceFailed(error);
                if (callback != null) callback.onError(error);
            }
        });
    }

    @Override
    public void removeHistoryBtDevice(HistoryBluetoothDevice historyBtDevice) {
        removeHistoryBtDevice(historyBtDevice, null);
    }

    @Override
    public boolean isScanning() {
        return getBtOp().isScanning();
    }

    @Override
    public void startScan() {
        if (!getBtManager().isBluetoothEnabled()) {
            getBtManager().openOrCloseBluetooth(true);
            return;
        }
        mHandler.removeMessages(MSG_SCAN_BT_DEVICE);
//        setBanShowDialog(isDevConnected());
        setBanScanBtDevice(false);
        getRCSPController().startBleScan(SConstant.SCAN_TIME);
    }

    @Override
    public void stopScan() {
        mHandler.removeMessages(MSG_SCAN_BT_DEVICE);
        getRCSPController().stopScan();
    }

    @Override
    public boolean isDevConnecting() {
        return super.isDevConnecting() || isConnectingClassicDevice();
    }

    @Override
    public void connectHistoryDevice(HistoryBluetoothDevice device) {
        if (device == null) return;
        mRCSPController.connectHistoryBtDevice(device, 15000, new OnReconnectHistoryRecordListener() {
            @Override
            public void onSuccess(HistoryBluetoothDevice history) {

            }

            @Override
            public void onFailed(HistoryBluetoothDevice history, BaseError error) {
                BluetoothDevice historyDevice = BluetoothUtil.getRemoteDevice(device.getAddress());
                if (historyDevice != null) {
                    mView.onDeviceConnection(historyDevice, StateCode.CONNECTION_FAILED);
                    mView.onDevConnectionError(error.getCode(), mActivity.getString(R.string.connect_history_failed_tips, UIHelper.getDevName(historyDevice)));
                }
            }
        });
        BluetoothDevice historyDevice = BluetoothUtil.getRemoteDevice(device.getAddress());
        if (historyDevice != null) {
            mView.onDeviceConnection(historyDevice, StateCode.CONNECTION_CONNECTING);
        }
        /*BluetoothDevice connectDevice = BluetoothUtil.getRemoteDevice(device.getAddress());
        if (device.getType() == BluetoothConstant.PROTOCOL_TYPE_BLE) {
            String bleAddr = UIHelper.getCacheBleAddr(device);
            connectDevice = BluetoothUtil.getRemoteDevice(bleAddr);
        }
        connectBtDeviceByWay(connectDevice, device.getType());*/
    }

    @Override
    public void connectBtDevice(BluetoothDevice device) {
        if (checkCanConnectToDevice(device)) {
            //todo 当没有设备连接的时候才暂停播放，有设备连接的情况下不暂停
            if (getConnectedDevice() == null && PlayControlImpl.getInstance().isPlay()) {
                PlayControlImpl.getInstance().pause();
            }
            getRCSPController().connectDevice(device);
        }
    }

    //检测是否可以去连接设备
    private boolean checkCanConnectToDevice(BluetoothDevice device) {
        if (device == null) {
            return false;
        } else if (!BluetoothUtil.isBluetoothEnable()) {
            mView.onDevConnectionError(SConstant.ERR_BLUETOOTH_NOT_ENABLE, (mActivity == null ?
                    "Bluetooth is not on." : mActivity.getString(R.string.bluetooth_not_enable)));
            return false;
        } else if (isDevConnecting()) {
            mView.onDevConnectionError(SConstant.ERR_DEV_CONNECTING, (mActivity == null ?
                    "Connecting device." : mActivity.getString(R.string.device_connecting_tips)));
            return false;
        } else if (checkConnectedEdrIsOverLimit(device)) { //连接设备已达到上限
            mView.onDevConnectionError(SConstant.ERR_EDR_MAX_CONNECTION, (mActivity == null ?
                    "The connection device has reached the upper limit." : mActivity.getString(R.string.connect_device_over_limit)));
            return false;
        }

        return true;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void connectBtDevice(BluetoothDevice device, BleScanMessage bleScanMessage) {
        BTRcspHelper.connectDeviceByMessage(mRCSPController, MainApplication.getApplication(), device, bleScanMessage);
    }

    @Override
    public void disconnectBtDevice() {
        final BluetoothDevice device = getConnectedDevice();
        if (null == device) return;
        DevicePopDialogFilter.getInstance().addIgnoreDevice(device.getAddress());
        getRCSPController().disconnectDevice(device);
    }

    @Override
    public void connectEdrDevice(String edrAddr) {
        if (BluetoothAdapter.checkBluetoothAddress(edrAddr)) {
            BluetoothDevice device = BluetoothUtil.getRemoteDevice(edrAddr);
            if (device != null) {
                startConnectEdrTimeoutTask(device);
                getBtOp().startConnectByBreProfiles(device);
                if (PlayControlImpl.getInstance().isPlay()) {
                    PlayControlImpl.getInstance().pause();
                }
            }
        }
    }


    @Override
    public boolean isConnectingClassicDevice() {
        return mConnectingEdrDevice != null;
    }

    @Override
    public boolean checkIsConnectingEdrDevice(String addr) {
        boolean ret = false;
        if (BluetoothAdapter.checkBluetoothAddress(addr)) {
            if (mConnectingEdrDevice != null) {
                ret = addr.equals(mConnectingEdrDevice.getAddress());
            }
            if (!ret) {
                BluetoothDevice edrDevice = BluetoothUtil.getRemoteDevice(addr);
                BluetoothDevice connectingEdr = getBtOp().getConnectingDevice();
                if (edrDevice != null) {
                    ret = BluetoothUtil.deviceEquals(edrDevice, connectingEdr);
                }
                if (!ret) {
                    ret = ProductUtil.checkEdrIsConnected(addr);
                    if (!ret) {//判断是否正在连接设备
                       /* ret = getBtOp().getConnectingDevice() != null &&
                                addr.equals(getBtOp().getConnectingDevice().getAddress());*/
                        ret = isDevConnecting();
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public void stopDeviceNotifyAdvInfo(BluetoothDevice device) {
        if (isCanUseTws(device)) {
            mRCSPController.controlAdvBroadcast(device, false, new OnRcspActionCallback<Boolean>() {
                @Override
                public void onSuccess(BluetoothDevice device, Boolean message) {
                    if (tempBanCmdShowDialog != -1) {
                        setTempBanCmdShowDialog(1);
                    }
                    setBanCmdShowDialog(true);
                }

                @Override
                public void onError(BluetoothDevice device, BaseError error) {

                }
            });
        }
    }

    @Override
    public void updateDeviceADVInfo(BluetoothDevice device) {
        if (isCanUseTws(device)) {
            mRCSPController.getDeviceSettingsInfo(device, 0xffffffff, new OnRcspActionCallback<ADVInfoResponse>() {
                @Override
                public void onSuccess(BluetoothDevice device, ADVInfoResponse message) {
                    mADVInfoResponseMap.put(device.getAddress(), message);
                    if (mNetworkHelper != null) {
                        mNetworkHelper.setADVInfo(message);
                    }
                    mView.onADVInfoUpdate(device, message);
                    if (!isUpdateDevBQ()) {
                        startUpdateDevBQ();
                    }
                }

                @Override
                public void onError(BluetoothDevice device, BaseError error) {

                }
            });
        }
    }

    @Override
    public ADVInfoResponse getADVInfo(BluetoothDevice device) {
        if (device == null) return null;
        return mADVInfoResponseMap.get(device.getAddress());
    }

    @Override
    public boolean isUpdateDevBQ() {
        return isBqUpdating;
    }

    @Override
    public void startUpdateDevBQ() {
        if (isBqUpdating) return;
        if (mHandler.hasMessages(MSG_UPDATE_DEVICE_BATTERY))
            mHandler.removeMessages(MSG_UPDATE_DEVICE_BATTERY);
        if (!isCanUseTws(getConnectedDevice())) return;
        if (getDeviceInfo() != null && getDeviceInfo().getSdkType() > JLChipFlag.JL_CHIP_FLAG_693X_TWS_HEADSET) {
            return;
        }
        mHandler.sendEmptyMessage(MSG_UPDATE_DEVICE_BATTERY);
    }

    @Override
    public void stopUpdateDevBQ() {
        isBqUpdating = mHandler.hasMessages(MSG_UPDATE_DEVICE_BATTERY);
        if (isBqUpdating) {
            isBqUpdating = false;
            mHandler.removeMessages(MSG_UPDATE_DEVICE_BATTERY);
            mView.onDeviceBQStatus(isBqUpdating);
        }
    }

    @Override
    public void setBanShowDialog(boolean enable) {
        if (mHandler.hasMessages(MSG_RESET_SHOW_DIALOG_FLAG)) return;
        if (mApplication.isOTA()) {
            isBanCmdShowDialog = true;
            return;
        }
        isBanShowDialog = enable;
    }

    @Override
    public void setBanScanBtDevice(boolean enable) {
        isBanBtScan = enable;
    }

    @Override
    public void fastConnect() {
        setBanShowDialog(true);
        if (!isBanCmdShowDialog) {
            setTempBanCmdShowDialog(0);
            setBanCmdShowDialog(true);
        }
        JL_Log.w(TAG, "fastConnect", "");
        mHandler.sendEmptyMessageDelayed(MSG_FAST_CONNECT, 3000);
        mHandler.removeMessages(MSG_RESET_SHOW_DIALOG_FLAG);
        mHandler.sendEmptyMessageDelayed(MSG_RESET_SHOW_DIALOG_FLAG, 33 * 1000);
    }

    @Override
    public void destroy() {
        destroyRCSPController(mEventCallback);
        mHandler.removeCallbacksAndMessages(null);
        if (mPermissionsHelper != null) {
            mPermissionsHelper.destroy();
        }
        if (mNetworkHelper != null) {
            mNetworkHelper.unregisterNetworkEventCallback(mNetworkEventCallback);
            mNetworkHelper.destroy();
        }
        if (mScreenEventManager != null) {
            mScreenEventManager.unregisterScreenEventListener(mIScreenEventListener);
        }
        mADVInfoResponseMap.clear();
    }

    @Override
    public void start() {
        mHandler.sendEmptyMessage(MSG_SCAN_BT_DEVICE);
    }

    /*
     * 判断是否能用TWS命令
     *
     * @return 结果
     */
    private boolean isCanUseTws(BluetoothDevice device) {
        return getDeviceInfo(device) != null && UIHelper.isCanUseTwsCmd(getDeviceInfo(device).getSdkType());
    }

    private void setConnectingEdrDevice(BluetoothDevice connectingEdrDevice) {
        mConnectingEdrDevice = connectingEdrDevice;
    }

    private void startConnectEdrTimeoutTask(BluetoothDevice device) {
        setConnectingEdrDevice(device);
        if (mHandler.hasMessages(MSG_CONNECT_EDR_TIMEOUT))
            mHandler.removeMessages(MSG_CONNECT_EDR_TIMEOUT);
        mHandler.sendEmptyMessageDelayed(MSG_CONNECT_EDR_TIMEOUT, BluetoothConstant.CONNECT_TIMEOUT);
    }

    private void stopConnectEdrTimeoutTask() {
        if (mHandler.hasMessages(MSG_CONNECT_EDR_TIMEOUT))
            mHandler.removeMessages(MSG_CONNECT_EDR_TIMEOUT);
        setConnectingEdrDevice(null);
    }

    private void updateDeviceBatteryQuantity() {
        if (!isDevConnected() || !isCanUseTws(getConnectedDevice())) {
            stopUpdateDevBQ();
            return;
        }
        mRCSPController.getDeviceSettingsInfo(getConnectedDevice(), 65, new OnRcspActionCallback<ADVInfoResponse>() {
            @Override
            public void onSuccess(BluetoothDevice device, ADVInfoResponse message) {
                mView.onDeviceBQUpdate(getConnectedDevice(), message);
                mHandler.removeMessages(MSG_UPDATE_DEVICE_BATTERY);
                mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEVICE_BATTERY, UPDATE_TIME);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                stopUpdateDevBQ();
                if (isDevConnected()) {
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEVICE_BATTERY, FAIL_RETRY_TIME);
                }
            }
        });
    }

    private void setBanCmdShowDialog(boolean enable) {
        if (mHandler.hasMessages(MSG_RESET_SHOW_DIALOG_FLAG)) return;
        if (mApplication.isOTA()) {
            isBanCmdShowDialog = true;
            return;
        }
        isBanCmdShowDialog = enable;
    }

    private void setTempBanCmdShowDialog(int temp) {
        tempBanCmdShowDialog = temp;
    }

    private void resetShowDialogFlag() {
        mHandler.removeMessages(MSG_RESET_SHOW_DIALOG_FLAG);
//        boolean isShowDeviceFragment = mActivity != null && ((HomeActivity) mActivity).getCurrentFragment() == 2;
//        setBanShowDialog(!isDevConnected() || isShowDeviceFragment);
        setBanShowDialog(false);
        if (tempBanCmdShowDialog != -1) {
            setBanCmdShowDialog(tempBanCmdShowDialog == 1);
            setTempBanCmdShowDialog(-1);
        }
    }

    @SuppressLint("MissingPermission")
    private boolean checkConnectedEdrIsOverLimit(BluetoothDevice device) {
        if (!PermissionUtil.checkHasConnectPermission(MainApplication.getApplication()))
            return true;
        boolean ret;
        List<BluetoothDevice> devices = BluetoothUtil.getSystemConnectedBtDeviceList();
        int count = 0;
        for (BluetoothDevice edrDevice : devices) {
            if (edrDevice.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC ||
                    edrDevice.getType() == BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
                //是相同设备，但是未连接通讯协议
                if (DeviceAddrManager.getInstance().isMatchDevice(edrDevice, device) && !isConnectedDevice(device)) {
                    return false;
                }
                count++;
            }
        }
        ret = count >= SConstant.MULTI_DEVICE_MAX_NUMBER;
        return ret;
    }

    private final BTRcspEventCallback mEventCallback = new BTRcspEventCallback() {

        @Override
        public void onAdapterStatus(boolean bEnabled, boolean bHasBle) {
            if (bEnabled) {
                setBanScanBtDevice(false);
                mHandler.sendEmptyMessage(MSG_SCAN_BT_DEVICE);
            } else {
                setBanScanBtDevice(true);
                mHandler.removeMessages(MSG_SCAN_BT_DEVICE);
            }
        }

        @Override
        public void onDiscoveryStatus(boolean bBle, boolean bStart) {
            mView.onDiscovery(bStart ? DISCOVERY_STATUS_WORKING : DISCOVERY_STATUS_IDLE, null, null);
            if (!bStart) {
                mHandler.removeMessages(MSG_SCAN_BT_DEVICE);
                if (!isBanBtScan) {
                    mHandler.sendEmptyMessageDelayed(MSG_SCAN_BT_DEVICE, SCAN_BT_DEVICE_DELAY);
                }
            }
        }

        @Override
        public void onDiscovery(BluetoothDevice device, BleScanMessage bleScanMessage) {
            mView.onDiscovery(DISCOVERY_STATUS_FOUND, device, bleScanMessage);
        }

        @Override
        public void onShowDialog(BluetoothDevice device, BleScanMessage bleScanMessage) {
            if (device != null && bleScanMessage != null && PreferencesHelper.getSharedPreferences(mActivity)
                    .getBoolean(SConstant.KEY_ALLOW_SHOW_BT_DIALOG, SConstant.ALLOW_SHOW_BT_DIALOG)
                    && !isBanShowDialog && !mApplication.isOTA()
                    && !mApplication.isNeedBanDialog()) {
                JL_Log.v(TAG, "onShowDialog", "device :  " + device + ", " + bleScanMessage);
                mView.onShowDialog(device, bleScanMessage);
            }
        }

        @Override
        public void onBondStatus(BluetoothDevice device, int status) {
            if (status == BluetoothDevice.BOND_NONE) { //更新历史记录
                mView.onRemoveHistoryDeviceSuccess(null);
            }
            if (BluetoothUtil.deviceEquals(mConnectingEdrDevice, device)) { //连接经典蓝牙
                switch (status) {
                    case BluetoothDevice.BOND_BONDING:
                        mView.onDeviceConnection(device, StateCode.CONNECTION_CONNECTING);
                        break;
                    case BluetoothDevice.BOND_NONE:
                        stopConnectEdrTimeoutTask();
                        mView.onDeviceConnection(device, StateCode.CONNECTION_FAILED);
                        HistoryBluetoothDevice history = mRCSPController.findHistoryBluetoothDevice(device.getAddress());
                        if (history != null) {
                            mView.onRemoveHistoryDeviceSuccess(history);
                        }
                        break;
                    /*case BluetoothDevice.BOND_BONDED:
                        stopConnectEdrTimeoutTask();
                        mView.onDeviceConnection(device, StateCode.CONNECTION_OK);
                        break;*/
                }
            }
        }

        @Override
        public void onA2dpStatus(BluetoothDevice device, int status) {
            if (BluetoothUtil.deviceEquals(mConnectingEdrDevice, device)) { //连接经典蓝牙
                if (status == BluetoothProfile.STATE_CONNECTING) {
                    mView.onDeviceConnection(device, StateCode.CONNECTION_CONNECTING);
                } else if (status == BluetoothProfile.STATE_CONNECTED) {
                    stopConnectEdrTimeoutTask();
                    mView.onDeviceConnection(device, StateCode.CONNECTION_OK);
                } else {
                    stopConnectEdrTimeoutTask();
                    mView.onDeviceConnection(device, StateCode.CONNECTION_DISCONNECT);
                }
            }
        }

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (status != StateCode.CONNECTION_CONNECTING) {
                if (status == StateCode.CONNECTION_DISCONNECT || status == StateCode.CONNECTION_FAILED) {
                    setBanCmdShowDialog(false);
                    if (device != null) {
                        mADVInfoResponseMap.remove(device.getAddress());
                    }
                    startScan();
                } else {
                    resetShowDialogFlag();
                }
            }
        }

        @Override
        public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
            int commandId = cmd.getId();
            switch (commandId) {
                case Command.CMD_ADV_DEVICE_NOTIFY:
                    NotifyAdvInfoCmd notifyAdvInfoCmd = (NotifyAdvInfoCmd) cmd;
                    NotifyAdvInfoParam advInfo = notifyAdvInfoCmd.getParam();
                    if (device != null && advInfo != null) {
                        BleScanMessage bleScanMessage = UIHelper.convertBleScanMsgFromNotifyADVInfo(advInfo);
                        bleScanMessage.setConnectWay(BluetoothUtil.getDeviceProtocol(getBluetoothOption(), getConnectedDevice()));
                        JL_Log.d(TAG, "CMD_ADV_DEVICE_NOTIFY", "isBanCmdShowDialog : " + isBanCmdShowDialog);
                        if (PreferencesHelper.getSharedPreferences(mActivity).getBoolean(SConstant.KEY_ALLOW_SHOW_BT_DIALOG, SConstant.ALLOW_SHOW_BT_DIALOG)
                                && !isBanCmdShowDialog && !mApplication.isOTA()
                                && !mApplication.isNeedBanDialog()) {
                            JL_Log.v(TAG, "onShowDialog", "from cmd:: " + BluetoothUtil.printBtDeviceInfo(device) + "\n" + bleScanMessage);
                            mView.onShowDialog(device, bleScanMessage);
                        }
                        if (getDeviceInfo() != null && UIHelper.isCanUseTwsCmd(getDeviceInfo().getSdkType())) {
                            mView.onDeviceBQUpdate(device, UIHelper.convertADVInfoFromBleScanMessage(bleScanMessage));
                        }
                    }
                    break;
                case Command.CMD_ADV_DEV_REQUEST_OPERATION:
                    RequestAdvOpCmd requestAdvOpCmd = (RequestAdvOpCmd) cmd;
                    RequestAdvOpParam param = requestAdvOpCmd.getParam();
                    if (param != null) {
                        switch (param.getOp()) {
                            case Constants.ADV_REQUEST_OP_UPDATE_CONFIGURE: {
                                updateDeviceADVInfo(device);
                                break;
                            }
                            case Constants.ADV_REQUEST_OP_UPDATE_AFTER_REBOOT: {
//                                mView.devNeedReboot();
                                break;
                            }
                        }
                    }
                    break;
            }
        }
    };

    private final NetworkHelper.OnNetworkEventCallback mNetworkEventCallback = new NetworkHelper.OnNetworkEventCallback() {
        @Override
        public void onNetworkState(boolean isAvailable) {
            mView.onNetworkState(isAvailable);
        }

        @Override
        public void onUpdateConfigureSuccess() {
            mView.onUpdateConfigureSuccess();
        }

        @Override
        public void onUpdateImage() {
            mView.onUpdateImage();
        }

        @Override
        public void onUpdateConfigureFailed(int code, String message) {
            mView.onUpdateConfigureFailed(code, message);
        }
    };

    private final IScreenEventListener mIScreenEventListener = new IScreenEventListener() {
        @Override
        public void onScreenOn() { //屏幕亮
            setBanScanBtDevice(false);
            if (getBtOp().isBluetoothEnabled() && !isScanning()) {
                startScan();
            }
        }

        @Override
        public void onScreenOff() {//屏幕灭
            if (isScanning()) {
                stopScan();
            }
            setBanScanBtDevice(true);
        }

        @Override
        public void onUserPresent() { //用户解锁

        }
    };
}
