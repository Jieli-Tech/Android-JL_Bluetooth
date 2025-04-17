package com.jieli.btsmart.tool.bluetooth.rcsp;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.util.PermissionUtil;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 定时扫描类
 * @since 2021/12/9
 */
public class ScanBleDeviceTask extends BTRcspEventCallback {
    private final Context mContext;
    private final RCSPController mRCSPController;
    private final PowerManager manager;
    private ScreenBroadcast mScreenBroadcast;
    private boolean isAutoScan = true;

    private final static int SCAN_BLE_DEVICE_DELAY = 15 * 1000; //15s
    private final static int MSG_START_SCAN = 0x6584;
    private final Handler mUIHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == MSG_START_SCAN) {
            startScan();
        }
        return true;
    });

    public ScanBleDeviceTask(Context context, RCSPController controller) {
        mContext = context;
        mRCSPController = controller;
        mRCSPController.addBTRcspEventCallback(this);
        manager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        registerBroadcast();
    }

    public void setAutoScan(boolean autoScan) {
        isAutoScan = autoScan;
    }

    @Override
    public void onAdapterStatus(boolean bEnabled, boolean bHasBle) {
        if (bEnabled) {
            startScan();
        } else {
            stopScan();
        }
    }

    @Override
    public void onDiscoveryStatus(boolean bBle, boolean bStart) {
        if (bBle && !bStart && isAutoScan) {
            reScan();
        }
    }

    @Override
    public void onDiscovery(BluetoothDevice device, BleScanMessage bleScanMessage) {
        if (MainApplication.getApplication().isOTA()) return;//OTA过程中，跳过回连
        final HistoryBluetoothDevice history = mRCSPController.findHistoryBluetoothDevice(device);
        if (null == history) return; //不是历史记录设备
        if (DevicePopDialogFilter.getInstance().isIgnoreDevice(mRCSPController, device.getAddress()))
            return;  //回连黑名单
        if (mRCSPController.isConnecting() || mRCSPController.getBluetoothManager() == null
                || mRCSPController.getBluetoothManager().isReconnecting()) return; //正在连接或者回连设备中
        if (!mRCSPController.getBluetoothOption().isUseMultiDevice() && mRCSPController.getUsingDevice() != null)
            return; //单设备管理 且 已有连接设备
        BTRcspHelper.connectDeviceByMessage(mRCSPController, mContext, device, bleScanMessage);
    }

    @Override
    public void onShowDialog(BluetoothDevice device, BleScanMessage bleScanMessage) {
        onDiscovery(device, bleScanMessage);
    }

    public void release() {
        unregisterBroadcast();
        mUIHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    private boolean isInteractive() {
        if (null == manager) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return manager.isInteractive();
        }
        return false;
    }

    private void startScan() {
        if (!isInteractive() || !PermissionUtil.hasBluetoothPermission(mContext) || mRCSPController.isScanning())
            return;
        if (!mRCSPController.startBleScan(SConstant.SCAN_TIME)) {
            reScan();
        }
    }

    private void stopScan() {
        mUIHandler.removeMessages(MSG_START_SCAN);
        mRCSPController.stopScan();
    }

    private void reScan() {
        mUIHandler.removeMessages(MSG_START_SCAN);
        mUIHandler.sendEmptyMessageDelayed(MSG_START_SCAN, SCAN_BLE_DEVICE_DELAY);
    }

    private void registerBroadcast() {
        if (mScreenBroadcast == null && mContext != null) {
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            intentFilter.addAction(Intent.ACTION_USER_PRESENT);
            mScreenBroadcast = new ScreenBroadcast();
            mContext.registerReceiver(mScreenBroadcast, intentFilter);
        }
    }

    private void unregisterBroadcast() {
        if (mContext != null && mScreenBroadcast != null) {
            mContext.unregisterReceiver(mScreenBroadcast);
            mScreenBroadcast = null;
        }
    }

    private class ScreenBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent || null == intent.getAction()) return;
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_OFF: //锁屏
                    stopScan();
                    break;
                case Intent.ACTION_SCREEN_ON:  //亮屏
                    startScan();
                    break;
            }
        }
    }
}
