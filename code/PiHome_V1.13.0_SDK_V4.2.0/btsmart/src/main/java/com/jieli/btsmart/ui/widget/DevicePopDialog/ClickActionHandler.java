package com.jieli.btsmart.ui.widget.DevicePopDialog;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.bluetooth.rcsp.BTRcspHelper;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.home.HomeActivity;
import com.jieli.btsmart.ui.settings.device.DeviceSettingsFragment;
import com.jieli.component.ActivityManager;
import com.jieli.component.utils.SystemUtil;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/12 3:37 PM
 * @desc : 处理弹窗的按钮点击操作
 */
class ClickActionHandler {
    private final String tag = getClass().getSimpleName();
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private final BluetoothDevice device;
    private final BleScanMessage message;

    ClickActionHandler(BluetoothDevice device, BleScanMessage message) {
        this.device = device;
        this.message = message;
    }

    //点击完成
    void finish(Context context) {
        BluetoothDevice sppDevice = BluetoothUtil.getRemoteDevice(message.getEdrAddr());
        boolean isConnected = mRCSPController.isDeviceConnected(device) || mRCSPController.isDeviceConnected(sppDevice);
        if (isConnected) {
            boolean isAppInForeground = SystemUtil.isAppInForeground(context);
            if (!isAppInForeground) {
                if (ActivityManager.getInstance().getTopActivity() != null) {
                    ActivityManager.getInstance().getTopActivity().startActivity(new Intent(ActivityManager.getInstance().getTopActivity(), HomeActivity.class));
                } else {
                    context.getApplicationContext().startActivity(new Intent(context.getApplicationContext(), HomeActivity.class));
                }
            }
        } else {
            mRCSPController.addBTRcspEventCallback(new BTRcspEventCallback() {
                @Override
                public void onConnection(BluetoothDevice bluetoothDevice, int status) {
                    if (DeviceAddrManager.getInstance().isMatchDevice(bluetoothDevice, device)) {
                        if (status == StateCode.CONNECTION_OK) {
                            finish(context);
                            mRCSPController.removeBTRcspEventCallback(this);
                        } else if (status == StateCode.CONNECTION_FAILED || status == StateCode.CONNECTION_DISCONNECT) {
                            mRCSPController.removeBTRcspEventCallback(this);
                        }
                    }
                }
            });
            JL_Log.d(tag, "finish", "connectDeviceByMessage");
            BTRcspHelper.connectDeviceByMessage(mRCSPController, device, message);
        }
    }


    //查看设备信息
    void info(Context context) {
        BluetoothDevice sppDevice = BluetoothUtil.getRemoteDevice(message.getEdrAddr());
        boolean isConnected = mRCSPController.isDeviceConnected(device) || mRCSPController.isDeviceConnected(sppDevice);
        if (isConnected) {
            boolean isAppInForeground = SystemUtil.isAppInForeground(context);
            JL_Log.i(tag, "isAppInForeground >>>>> " + isAppInForeground);
            Bundle bundle = new Bundle();
            bundle.putParcelable(SConstant.KEY_DEVICE, device);
            CommonActivity.startCommonActivity(ActivityManager.getInstance().getTopActivity(), SConstant.REQUEST_CODE_DEVICE_SETTINGS,
                    DeviceSettingsFragment.class.getCanonicalName(), bundle);
        } else if (isAllowConnect()) {
            mRCSPController.addBTRcspEventCallback(new BTRcspEventCallback() {
                @Override
                public void onConnection(BluetoothDevice bluetoothDevice, int status) {
                    if (DeviceAddrManager.getInstance().isMatchDevice(bluetoothDevice, device)) {
                        if (status == StateCode.CONNECTION_OK) {
                            Bundle bundle = new Bundle();
                            JL_Log.e(tag, "============ 跳转到设置页面==========");
                            bundle.putParcelable(SConstant.KEY_DEVICE, device);
                            CommonActivity.startCommonActivity(ActivityManager.getInstance().getTopActivity(), SConstant.REQUEST_CODE_DEVICE_SETTINGS,
                                    DeviceSettingsFragment.class.getCanonicalName(), bundle);
                            mRCSPController.removeBTRcspEventCallback(this);
                        } else if (status == StateCode.CONNECTION_FAILED || status == StateCode.CONNECTION_DISCONNECT) {
                            mRCSPController.removeBTRcspEventCallback(this);
                        }
                    }
                }
            });
            JL_Log.d(tag, "info", "connectDeviceByMessage");
            BTRcspHelper.connectDeviceByMessage(mRCSPController, device, message);
        }
    }


    //连接设备
    void connect() {
        if (isAllowConnect()) {
            JL_Log.d(tag, "connect", "connectDeviceByMessage");
            BTRcspHelper.connectDeviceByMessage(mRCSPController, device, message);
        } else {
            BluetoothDevice edrDevice = BluetoothUtil.getRemoteDevice(message.getEdrAddr());
            mRCSPController.getBtOperation().startConnectByBreProfiles(edrDevice);
        }
    }


    private boolean isAllowConnect() {
        return message.isEnableConnect() || (message.isSupportLeAudio() && message.isLeAudioConnected())
                || message.getConnectWay() != BluetoothConstant.PROTOCOL_TYPE_BLE;
    }

}
