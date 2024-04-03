package com.jieli.btsmart.ui.widget.DevicePopDialog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.bluetooth.rcsp.BTRcspHelper;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.home.HomeActivity;
import com.jieli.btsmart.ui.settings.device.DeviceSettingsFragment;
import com.jieli.component.ActivityManager;
import com.jieli.component.utils.SystemUtil;
import com.jieli.jl_bt_ota.util.BluetoothUtil;
import com.jieli.jl_bt_ota.util.JL_Log;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/12 3:37 PM
 * @desc : 处理弹窗的按钮点击操作
 */
class ClickActionHandler {
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private final BluetoothDevice device;
    private final BleScanMessage message;

    ClickActionHandler(BluetoothDevice device, BleScanMessage message) {
        this.device = device;
        this.message = message;
    }

    //点击完成
    void finish(Context context) {
        BluetoothDevice sppDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(message.getEdrAddr());
        boolean isConnected = mRCSPController.isDeviceConnected(device) || mRCSPController.isDeviceConnected(sppDevice);
        if (isConnected) {
            boolean isAppInForeground = SystemUtil.isAppInForeground(context);
            if (!isAppInForeground) {
                if (ActivityManager.getInstance().getTopActivity() != null) {
                    ActivityManager.getInstance().getTopActivity().startActivity(new Intent(ActivityManager.getInstance().getTopActivity(), HomeActivity.class));
                }else {
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
                        } else if (status == StateCode.CONNECTION_FAILED) {
                            mRCSPController.removeBTRcspEventCallback(this);
                        }
                    }
                }
            });
            BTRcspHelper.connectDeviceByMessage(mRCSPController, MainApplication.getApplication(), device, message);
        }
    }


    //查看设备信息
    void info(Context context) {
        BluetoothDevice sppDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(message.getEdrAddr());
        boolean isConnected = mRCSPController.isDeviceConnected(device) || mRCSPController.isDeviceConnected(sppDevice);
        if (isConnected) {
            boolean isAppInForeground = SystemUtil.isAppInForeground(context);
            JL_Log.i("sen_pop", "isAppInForeground >>>>> " + isAppInForeground);
            Bundle bundle = new Bundle();
            CommonActivity.startCommonActivity(ActivityManager.getInstance().getTopActivity(), SConstant.REQUEST_CODE_DEVICE_SETTINGS,
                    DeviceSettingsFragment.class.getCanonicalName(), bundle);
        } else if (message.isEnableConnect()) {
            mRCSPController.addBTRcspEventCallback(new BTRcspEventCallback() {
                @Override
                public void onConnection(BluetoothDevice bluetoothDevice, int status) {
                    if (DeviceAddrManager.getInstance().isMatchDevice(bluetoothDevice, device)) {
                        if (status == StateCode.CONNECTION_OK) {
                            Bundle bundle = new Bundle();
                            JL_Log.e("sen_pop", "============ 跳转到设置页面==========");
                            CommonActivity.startCommonActivity(ActivityManager.getInstance().getTopActivity(), SConstant.REQUEST_CODE_DEVICE_SETTINGS,
                                    DeviceSettingsFragment.class.getCanonicalName(), bundle);
                            mRCSPController.removeBTRcspEventCallback(this);
                        } else if (status == StateCode.CONNECTION_FAILED) {
                            mRCSPController.removeBTRcspEventCallback(this);
                        }
                    }
                }
            });
            BTRcspHelper.connectDeviceByMessage(mRCSPController, MainApplication.getApplication(), device, message);
        }
    }


    //连接设备
    void connect() {
        if (message.isEnableConnect()) {
            BTRcspHelper.connectDeviceByMessage(mRCSPController, MainApplication.getApplication(), device, message);
        } else {
            BluetoothDevice edrDevice = BluetoothUtil.getRemoteDevice(message.getEdrAddr());
            mRCSPController.getBtOperation().startConnectByBreProfiles(edrDevice);
        }
    }


}
