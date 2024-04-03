package com.jieli.btsmart.tool.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import java.util.Objects;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/9 5:39 PM
 * @desc : 定时扫描类
 */
public class ScanDeviceTask extends BTEventCallback {

    private final BluetoothHelper mBluetoothHelper = BluetoothHelper.getInstance();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final static int SCAN_BT_DEVICE_DELAY = 3 * 1000; //3s
    private Context context;
    private ScreenEventBroadcast broadcast;

    public void init(Context context) {
        this.context = context;
        mBluetoothHelper.registerBTEventCallback(this);
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        broadcast = new ScreenEventBroadcast();
        context.registerReceiver(broadcast, intentFilter);
    }

    private final Runnable scanTask = () -> {
        if (!mBluetoothHelper.getBtOperation().isScanning()) {
            mBluetoothHelper.startBleScan();
        }
    };

    @Override
    public void onAdapterStatus(boolean bEnabled, boolean bHasBle) {
        super.onAdapterStatus(bEnabled, bHasBle);
        if (bEnabled) {
            mHandler.removeCallbacks(scanTask);
            mHandler.post(scanTask);
        } else {
            mHandler.removeCallbacks(scanTask);
        }
    }

    @Override
    public void onDiscoveryStatus(boolean bBle, boolean bStart) {
        super.onDiscoveryStatus(bBle, bStart);
        if (bBle && !bStart) {
            mHandler.removeCallbacks(scanTask);
            PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            //亮屏时持续扫描
            if (manager.isInteractive()) {
                mHandler.postDelayed(scanTask, SCAN_BT_DEVICE_DELAY);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        context.unregisterReceiver(broadcast);
        super.finalize();
    }

    private class ScreenEventBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case Intent.ACTION_SCREEN_OFF:
                    mHandler.removeCallbacks(scanTask);
                    mBluetoothHelper.stopScan();
                    break;
                case Intent.ACTION_SCREEN_ON:
                    mHandler.post(scanTask);
                    break;
            }
        }
    }
}
