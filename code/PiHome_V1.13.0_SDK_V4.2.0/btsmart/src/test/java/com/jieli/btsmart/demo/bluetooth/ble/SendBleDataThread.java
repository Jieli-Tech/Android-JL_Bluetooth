package com.jieli.btsmart.demo.bluetooth.ble;

import android.bluetooth.BluetoothGatt;

import com.jieli.btsmart.demo.bluetooth.ble.interfaces.IBleOp;
import com.jieli.btsmart.demo.bluetooth.ble.interfaces.OnThreadStateListener;
import com.jieli.btsmart.demo.bluetooth.ble.interfaces.OnWriteDataCallback;
import com.jieli.jl_bt_ota.util.JL_Log;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 发送BLE数据线程
 *
 * @author zqjasonZhong
 * @date 2019/10/8
 */
public class SendBleDataThread extends Thread {
    private final static String TAG = "SendBleDataThread";
    private final LinkedBlockingQueue<BleSendTask> mQueue = new LinkedBlockingQueue<>();
    private volatile boolean isDataSend = false;
    private volatile boolean isThreadWaiting = false;
    private volatile boolean isWaitingForCallback = false;
    private volatile int retryNum = 0;
    private final IBleOp mBleManager;
    private final OnThreadStateListener mListener;

    private BleSendTask mCurrentTask;

    public SendBleDataThread(IBleOp manager, OnThreadStateListener listener) {
        super(TAG);
        mBleManager = manager;
        mListener = listener;
    }

    public boolean isRunning() {
        return isDataSend;
    }

    public boolean addSendTask(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID, byte[] data, OnWriteDataCallback callback) {
        if (null == mBleManager || null == gatt || null == serviceUUID || null == characteristicUUID || null == data || data.length == 0) {
            return false;
        }
        int mtu = mBleManager.getBleMtu();
        JL_Log.d(TAG, "addSendTask : " + mtu);
        int dataLen = data.length;
        int blockCount = dataLen / mtu;
        boolean ret = false;
        for (int i = 0; i < blockCount; i++) {
            byte[] mBlockData = new byte[mtu];
            System.arraycopy(data, i * mtu, mBlockData, 0, mBlockData.length);
            ret = addSendData(gatt, serviceUUID, characteristicUUID, mBlockData, callback);
        }

        if (0 != dataLen % mtu) {
            byte[] noBlockData = new byte[dataLen % mtu];
            System.arraycopy(data, dataLen - dataLen % mtu, noBlockData, 0, noBlockData.length);
            ret = addSendData(gatt, serviceUUID, characteristicUUID, noBlockData, callback);
        }
        return ret;
    }

    public void wakeupSendThread(BleSendTask sendTask) {
        if (null == sendTask || mCurrentTask != null && mCurrentTask.equals(sendTask)) {
            if (sendTask != null) {
                sendTask.setCallback(mCurrentTask.getCallback());
                mCurrentTask = sendTask;
            }
            synchronized (mQueue) {
                if (isThreadWaiting) {
                    if (isWaitingForCallback) {
                        mQueue.notifyAll();
                    } else {
                        mQueue.notify();
                    }
                } else if (isWaitingForCallback) {
                    mQueue.notify();
                }
            }
        }
    }

    private boolean addSendData(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID, byte[] data, OnWriteDataCallback callback) {
        boolean ret = false;
        if (isDataSend) {
            BleSendTask sendTask = new BleSendTask(gatt, serviceUUID, characteristicUUID, data, callback);
            try {
                mQueue.put(sendTask);
                ret = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (ret && isThreadWaiting && !isWaitingForCallback) {
                isThreadWaiting = false;
                synchronized (mQueue) {
                    mQueue.notify();
                }
            }
        }
        return ret;
    }

    @Override
    public synchronized void start() {
        isDataSend = true;
        super.start();
    }

    public synchronized void stopThread() {
        isDataSend = false;
        wakeupSendThread(null);
    }

    private void callbackResult(BleSendTask task, boolean result) {
        if (task != null && task.getCallback() != null) {
            if (task.getBleGatt() == null) return;
            task.getCallback().onBleResult(task.getBleGatt().getDevice(), task.getServiceUUID(),
                    task.getCharacteristicUUID(), result, task.getData());
        } else {
            JL_Log.i(TAG, "getCallback is null.");
        }
    }

    @Override
    public void run() {
        JL_Log.d(TAG, "send ble data thread is started.");
        if (mListener != null) {
            mListener.onStart(getId(), getName());
        }
        if (mBleManager != null) {
            synchronized (mQueue) {
                while (isDataSend) {
                    mCurrentTask = null;
                    isThreadWaiting = false;
                    isWaitingForCallback = false;
                    if (mQueue.isEmpty()) {
                        isThreadWaiting = true;
                        JL_Log.d(TAG, "queue is empty, so waiting for data");
                        try {
                            mQueue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        mCurrentTask = mQueue.peek();
                        if (mCurrentTask != null) {
                            isWaitingForCallback = mBleManager.writeDataByBle(mCurrentTask.mGatt, mCurrentTask.getServiceUUID(),
                                    mCurrentTask.getCharacteristicUUID(), mCurrentTask.getData());
                            if (isWaitingForCallback) {
                                try {
                                    mQueue.wait(BleManager.SEND_DATA_MAX_TIMEOUT);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                mCurrentTask.setStatus(-1);
                            }
                            JL_Log.d(TAG, "data send ret :" + mCurrentTask.getStatus());
                            if (mCurrentTask.getStatus() != BluetoothGatt.GATT_SUCCESS) { //发送失败
                                retryNum++;
                                if (retryNum >= 3) { //重发次数超过限制
                                    callbackResult(mCurrentTask, false);
                                    mQueue.clear();
                                } else {
                                    if (mCurrentTask.getStatus() != -1) {
                                        mCurrentTask.setStatus(-1);
                                        try {
                                            sleep(10);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    continue;
                                }
                            } else { //发送成功
                                callbackResult(mCurrentTask, true);
                            }
                        }
                        retryNum = 0;
                        if (!mQueue.isEmpty()) mQueue.poll();
                    }
                }
            }

            isWaitingForCallback = false;
            isThreadWaiting = false;
            mQueue.clear();
            if (mListener != null) {
                mListener.onEnd(getId(), getName());
            }
            JL_Log.d(TAG, "send ble data thread exit.");
        }
    }


    public static class BleSendTask {
        private BluetoothGatt mGatt;
        private UUID serviceUUID;
        private UUID characteristicUUID;
        private byte[] data;
        private int status = -1;
        private OnWriteDataCallback mCallback;

        public BleSendTask(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID, byte[] data, OnWriteDataCallback callback) {
            this.mGatt = gatt;
            this.serviceUUID = serviceUUID;
            this.characteristicUUID = characteristicUUID;
            this.data = data;
            this.mCallback = callback;
        }

        public BluetoothGatt getBleGatt() {
            return mGatt;
        }

        public void setDevice(BluetoothGatt gatt) {
            mGatt = gatt;
        }

        public UUID getServiceUUID() {
            return serviceUUID;
        }

        public void setServiceUUID(UUID serviceUUID) {
            this.serviceUUID = serviceUUID;
        }

        public UUID getCharacteristicUUID() {
            return characteristicUUID;
        }

        public void setCharacteristicUUID(UUID characteristicUUID) {
            this.characteristicUUID = characteristicUUID;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public OnWriteDataCallback getCallback() {
            return mCallback;
        }

        public void setCallback(OnWriteDataCallback callback) {
            mCallback = callback;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "BleSendTask{" +
                    "mGatt=" + mGatt +
                    ", serviceUUID=" + serviceUUID +
                    ", characteristicUUID=" + characteristicUUID +
                    ", data=" + Arrays.toString(data) +
                    ", status=" + status +
                    ", mCallback=" + mCallback +
                    '}';
        }

        @Override
        public int hashCode() {
            if (mGatt != null && serviceUUID != null && characteristicUUID != null) {
                return mGatt.hashCode() + serviceUUID.hashCode() + characteristicUUID.hashCode();
            }
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BleSendTask) {
                BleSendTask other = (BleSendTask) obj;
                if (mGatt != null && serviceUUID != null && characteristicUUID != null) {
                    return mGatt.equals(other.getBleGatt()) && serviceUUID.equals(other.getServiceUUID())
                            && characteristicUUID.equals(other.getCharacteristicUUID());
                }
            }
            return false;
        }
    }
}
