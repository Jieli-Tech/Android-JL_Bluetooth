package com.jieli.btsmart.tool.bluetooth.rcsp;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.IBluetoothOperation;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;

import java.util.Calendar;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 设备请求操作处理器
 * @since 2021/12/13
 */
public class DeviceOpHandler extends BTRcspEventCallback {
    private static volatile DeviceOpHandler sHandler;
    private final RCSPController mRCSPController = RCSPController.getInstance();

    private BluetoothDevice mReConnectDevice = null;

    private final static int RECONNECT_TIMEOUT = 12 * 1000; //12s
    private final static int MSG_RECONNECT_DEVICE_TIMEOUT = 0x1212;
    private final Handler mUIHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == MSG_RECONNECT_DEVICE_TIMEOUT) {
            if (isReconnecting()) {
                if (!mRCSPController.isDeviceConnected(mReConnectDevice)) { //设备回连失败
                    mRCSPController.getCallbackManager().onConnection(mReConnectDevice, StateCode.CONNECTION_DISCONNECT);
                }
                setReConnectDevice(null);
            }
        }
        return true;
    });

    private DeviceOpHandler() {
    }

    public static DeviceOpHandler getInstance() {
        if (null == sHandler) {
            synchronized (DeviceOpHandler.class) {
                if (null == sHandler) {
                    sHandler = new DeviceOpHandler();
                }
            }
        }
        return sHandler;
    }

    public boolean isReconnecting() {
        return mReConnectDevice != null;
    }

    public void destroy() {
        mUIHandler.removeCallbacksAndMessages(null);
        setReConnectDevice(null);
        sHandler = null;
    }

    @Override
    public void onConnection(BluetoothDevice device, int status) {
        if (BluetoothUtil.deviceEquals(device, mReConnectDevice)) {
            if (status == StateCode.CONNECTION_DISCONNECT) {//判断回连设备断开
                mUIHandler.postDelayed(() -> {
                    if (!mUIHandler.hasMessages(MSG_RECONNECT_DEVICE_TIMEOUT)) { //未开始回连设备
                        //回连设备
                        mRCSPController.connectDevice(mReConnectDevice);
                        //开始回连设备超时任务
                        mUIHandler.sendEmptyMessageDelayed(MSG_RECONNECT_DEVICE_TIMEOUT, RECONNECT_TIMEOUT);
                    }
                }, 1000);
            } else if (status == StateCode.CONNECTION_OK) {
                setReConnectDevice(null);
                mUIHandler.removeMessages(MSG_RECONNECT_DEVICE_TIMEOUT);
            }
        }
    }

    @Override
    public void onDeviceRequestOp(BluetoothDevice device, int op) {
        switch (op) {
            case Constants.ADV_REQUEST_OP_UPDATE_CONFIGURE:
                mRCSPController.getDeviceSettingsInfo(device, 0xffffffff, null);
                break;
            case Constants.ADV_REQUEST_OP_UPDATE_AFTER_REBOOT:
                mRCSPController.getDeviceSettingsInfo(device, 0xffffffff, new OnRcspActionCallback<ADVInfoResponse>() {
                    @Override
                    public void onSuccess(BluetoothDevice device, ADVInfoResponse message) {
                        mRCSPController.rebootDevice(device, null);
                    }

                    @Override
                    public void onError(BluetoothDevice device, BaseError error) {

                    }
                });
                break;
            case Constants.ADV_REQUEST_OP_SYNC_TIME:
                int connectedTime = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
                mRCSPController.updateConnectedTime(device, connectedTime, null);
                break;
            case Constants.ADV_REQUEST_OP_RECONNECT_DEVICE:
                IBluetoothOperation operation = mRCSPController.getBtOperation();
                if (operation != null && operation.isConnectedBLEDevice(device) && !isReconnecting()) {//判断是走BLE方式才需要主从切换时回连
                    setReConnectDevice(device);
                }
                break;
            case Constants.ADV_REQUEST_OP_SYNC_DEVICE_INFO:
                mRCSPController.requestDeviceInfo(device, 0xffffffff, new OnRcspActionCallback<DeviceInfo>() {
                    @Override
                    public void onSuccess(BluetoothDevice device, DeviceInfo message) {
                        checkDeviceStatus(device, message);
                    }

                    @Override
                    public void onError(BluetoothDevice device, BaseError error) {

                    }
                });
                break;
        }
    }


    private void checkDeviceStatus(BluetoothDevice device, DeviceInfo deviceInfo) {
        if (deviceInfo == null) return;
        boolean isMandatoryUpdate = deviceInfo.isMandatoryUpgrade() || deviceInfo.getRequestOtaFlag() == Constants.FLAG_MANDATORY_UPGRADE;
        if (isMandatoryUpdate) {
            mRCSPController.getCallbackManager().onMandatoryUpgrade(device);
        }
    }

    private void setReConnectDevice(BluetoothDevice reConnectDevice) {
        mReConnectDevice = reConnectDevice;
    }
}
