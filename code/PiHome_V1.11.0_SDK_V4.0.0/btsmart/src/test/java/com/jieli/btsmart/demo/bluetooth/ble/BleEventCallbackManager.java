package com.jieli.btsmart.demo.bluetooth.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;

import com.jieli.btsmart.demo.bluetooth.ble.interfaces.BleEventCallback;
import com.jieli.btsmart.demo.bluetooth.ble.model.BleScanInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ble事件回调管理器
 *
 * @author zqjasonZhong
 * @since 2020/12/24
 */
public class BleEventCallbackManager extends BleEventCallback {
    private final ArrayList<BleEventCallback> mCallbacks = new ArrayList<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public void registerBleEventCallback(BleEventCallback callback) {
        if (callback != null && !mCallbacks.contains(callback)) {
            mCallbacks.add(callback);
        }
    }

    public void unregisterBleEventCallback(BleEventCallback callback) {
        if (callback != null && !mCallbacks.isEmpty()) {
            mCallbacks.remove(callback);
        }
    }

    public void release() {
        mCallbacks.clear();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onAdapterChange(final boolean bEnabled) {
        callbackBleEvent(new BleEventCallbackImpl() {
            @Override
            public void onCallback(BleEventCallback callback) {
                callback.onAdapterChange(bEnabled);
            }
        });
    }

    @Override
    public void onDiscoveryBleChange(final boolean bStart) {
        callbackBleEvent(new BleEventCallbackImpl() {
            @Override
            public void onCallback(BleEventCallback callback) {
                callback.onDiscoveryBleChange(bStart);
            }
        });
    }

    @Override
    public void onDiscoveryBle(final BluetoothDevice device, final BleScanInfo bleScanMessage) {
        callbackBleEvent(new BleEventCallbackImpl() {
            @Override
            public void onCallback(BleEventCallback callback) {
                callback.onDiscoveryBle(device, bleScanMessage);
            }
        });
    }

    @Override
    public void onBleConnection(final BluetoothDevice device, final int status) {
        callbackBleEvent(new BleEventCallbackImpl() {
            @Override
            public void onCallback(BleEventCallback callback) {
                callback.onBleConnection(device, status);
            }
        });
    }

    @Override
    public void onBleServiceDiscovery(final BluetoothDevice device, final int status, final List<BluetoothGattService> services) {
        callbackBleEvent(new BleEventCallbackImpl() {
            @Override
            public void onCallback(BleEventCallback callback) {
                callback.onBleServiceDiscovery(device, status, services);
            }
        });
    }

    @Override
    public void onBleNotificationStatus(final BluetoothDevice device, final UUID serviceUuid, final UUID characteristicUuid, final int status) {
        callbackBleEvent(new BleEventCallbackImpl() {
            @Override
            public void onCallback(BleEventCallback callback) {
                callback.onBleNotificationStatus(device, serviceUuid, characteristicUuid, status);
            }
        });
    }

    @Override
    public void onBleDataBlockChanged(final BluetoothDevice device, final int block, final int status) {
        callbackBleEvent(new BleEventCallbackImpl() {
            @Override
            public void onCallback(BleEventCallback callback) {
                callback.onBleDataBlockChanged(device, block, status);
            }
        });
    }

    @Override
    public void onBleDataNotification(final BluetoothDevice device, final UUID serviceUuid, final UUID characteristicsUuid, final byte[] data) {
        callbackBleEvent(new BleEventCallbackImpl() {
            @Override
            public void onCallback(BleEventCallback callback) {
                callback.onBleDataNotification(device, serviceUuid, characteristicsUuid, data);
            }
        });
    }

    @Override
    public void onBleWriteStatus(final BluetoothDevice device, final UUID serviceUuid, final UUID characteristicsUuid, final byte[] data, final int status) {
        callbackBleEvent(new BleEventCallbackImpl() {
            @Override
            public void onCallback(BleEventCallback callback) {
                callback.onBleWriteStatus(device, serviceUuid, characteristicsUuid, data, status);
            }
        });
    }

    @Override
    public void onConnectionUpdated(final BluetoothDevice device, final int interval, final int latency, final int timeout, final int status) {
        callbackBleEvent(new BleEventCallbackImpl() {
            @Override
            public void onCallback(BleEventCallback callback) {
                callback.onConnectionUpdated(device, interval, latency, timeout, status);
            }
        });
    }

    private void callbackBleEvent(BleEventCallbackImpl impl) {
        if (null == impl) return;
        OnBleEventRunnable runnable = new OnBleEventRunnable(impl);
        if (Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId()) {
            runnable.run();
        } else {
            mHandler.post(runnable);
        }
    }

    private static abstract class BleEventCallbackImpl {

        public abstract void onCallback(BleEventCallback callback);
    }

    private class OnBleEventRunnable implements Runnable {
        private final BleEventCallbackImpl mImpl;

        public OnBleEventRunnable(BleEventCallbackImpl impl) {
            mImpl = impl;
        }

        @Override
        public void run() {
            if (!mCallbacks.isEmpty() && mImpl != null) {
                for (BleEventCallback callback : new ArrayList<>(mCallbacks)) {
                    if (callback != null) {
                        mImpl.onCallback(callback);
                    }
                }
            }
        }
    }
}
