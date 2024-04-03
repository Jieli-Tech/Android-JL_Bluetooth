package com.jieli.btsmart.tool.bluetooth.rcsp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.btsmart.constant.SConstant;
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

    private final static int SCAN_BLE_DEVICE_DELAY = 6 * 1000; //6s
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

    public void release() {
        unregisterBroadcast();
        mUIHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    private void startScan() {
        if (!manager.isInteractive() || !PermissionUtil.hasBluetoothPermission(mContext) || mRCSPController.isScanning())
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
