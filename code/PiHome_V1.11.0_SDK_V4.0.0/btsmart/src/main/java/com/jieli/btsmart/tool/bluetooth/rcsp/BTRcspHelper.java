package com.jieli.btsmart.tool.bluetooth.rcsp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.device.alarm.AlarmBean;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.JL_DeviceType;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.BluetoothCallbackImpl;
import com.jieli.bluetooth.interfaces.bluetooth.IBluetoothOperation;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.ToastUtil;

import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 辅助类
 * @since 2021/12/8
 */
public class BTRcspHelper {

    private static BluetoothDevice bluetoothDevice;

    /**
     * 音量调节
     *
     * @param controller 控制器
     * @param context    上下文
     * @param value      音量
     * @param callback   回调
     */
    public static void adjustVolume(RCSPController controller, Context context, int value, OnRcspActionCallback<Boolean> callback) {
        if (!controller.isDeviceConnected()) {
            if (context == null) return;
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, AudioManager.FLAG_SHOW_UI);
            }
            return;
        }
        controller.adjustVolume(controller.getUsingDevice(), value, callback);
    }

    /**
     * 通过广播信息连接设备
     *
     * @param controller     控制器
     * @param context        上下文
     * @param device         待连接的设备
     * @param bleScanMessage 广播信息
     */
    public static void connectDeviceByMessage(RCSPController controller, Context context, BluetoothDevice device, BleScanMessage bleScanMessage) {
        if (bleScanMessage != null) {
            final int devType = bleScanMessage.getDeviceType();
            boolean isMandatoryUseBLE = SConstant.IS_USE_BLE_WAY
                    || devType == JL_DeviceType.JL_DEVICE_TYPE_WATCH
                    || devType == JL_DeviceType.JL_DEVICE_TYPE_CHARGING_BIN
                    || controller.getBluetoothOption().isMandatoryUseBLE();
            int way;
            if (isMandatoryUseBLE) {
                way = BluetoothConstant.PROTOCOL_TYPE_BLE;
            } else {
                if (devType == JL_DeviceType.JL_DEVICE_TYPE_TWS_HEADSET_V1 || devType == JL_DeviceType.JL_DEVICE_TYPE_TWS_HEADSET_V2) { //TWS耳机
                    final int action = bleScanMessage.getAction();
                    if (action == StateCode.TWS_HEADSET_STATUS_DISCONNECTED || action == StateCode.TWS_HEADSET_STATUS_DIMISS) {
                    /*if (bleScanMessage.getAction() == StateCode.TWS_HEADSET_STATUS_CONNECTED) { //经典蓝牙被其他手机连接了
                        ToastUtil.showToastShort("设备已被连接");
                        return;
                    }*/
                        final IBluetoothOperation bluetoothOperation = controller.getBtOperation();
                        BluetoothDevice edrDevice = BluetoothUtil.getRemoteDevice(bleScanMessage.getEdrAddr());
                        boolean isEdrConnect = UIHelper.isEdrConnect(bleScanMessage.getEdrAddr());
                        if (null != bluetoothOperation && edrDevice != null && !isEdrConnect) {
                            if (bluetoothDevice != null) {
                                ToastUtil.showToastShort(context.getString(R.string.device_connecting_tips));
                                return;
                            }
                            final BluetoothDevice bleDevice = device;
                            final BluetoothCallbackImpl bluetoothCallback = new BluetoothCallbackImpl() {
                                @Override
                                public void onBrEdrConnection(BluetoothDevice device, int status) {
                                    if (BluetoothUtil.deviceEquals(bluetoothDevice, device)) {
                                        postConnectionChange(device, convertConnectionState(status));
                                        if (status != BluetoothProfile.STATE_CONNECTING) {
                                            bluetoothDevice = null;
                                            bluetoothOperation.unregisterBluetoothCallback(this);
                                            if (status == BluetoothProfile.STATE_CONNECTED) {
                                                bleScanMessage.setAction(StateCode.TWS_HEADSET_STATUS_CONNECTED);
                                                connectDeviceByMessage(controller, context, bleDevice, bleScanMessage);
                                            }
                                        }
                                    }
                                }
                            };
                            postConnectionChange(device, StateCode.CONNECTION_CONNECTING);
                            bluetoothOperation.registerBluetoothCallback(bluetoothCallback);
                            bluetoothDevice = edrDevice;
                            if (bluetoothOperation.startConnectByBreProfiles(edrDevice) != ErrorCode.ERR_NONE) {
                                bluetoothDevice = null;
                                bluetoothOperation.unregisterBluetoothCallback(bluetoothCallback);
                                postConnectionChange(device, StateCode.CONNECTION_DISCONNECT);
                            }
                            return;
                        }
                    }
                }
                way = bleScanMessage.getConnectWay();
                if (way == BluetoothConstant.PROTOCOL_TYPE_SPP) {
                    BluetoothDevice temp = BluetoothUtil.getRemoteDevice(bleScanMessage.getEdrAddr());
                    JL_Log.d("zzc", "connectDeviceByMessage", " classicDevice : " + temp );
                    if (null == temp) {
                        way = BluetoothConstant.PROTOCOL_TYPE_BLE;
                    } else {
                        device = temp;
                    }
                }
            }
            JL_Log.d("zzc", "connectDeviceByMessage", "device : " + device + ", way : " + way);
            DeviceAddrManager.getInstance().saveDeviceConnectWay(device, way);
        }
        if (checkCanConnectToDevice(controller, context, device)) {
            //todo 当没有设备连接的时候才暂停播放，有设备连接的情况下不暂停
            if (!controller.isDeviceConnected() && PlayControlImpl.getInstance().isPlay()) {
                PlayControlImpl.getInstance().pause();
            }
            controller.connectDevice(device);
        }
    }


    /**
     * 获取闹钟信息的重复模式描述
     *
     * @param alarmBean 闹钟信息
     * @return 重复模式描述
     */
    public static String getRepeatDescModify(Context context, AlarmBean alarmBean) {
        if (context == null) return null;
        StringBuilder sb = new StringBuilder();
        String result;
        int mode = alarmBean.getRepeatMode() & 0xff;
        if (mode == 0x00) {
            result = context.getString(R.string.alarm_repeat_single);
        } else if ((mode & 0x01) == 0x01) {
            result = context.getString(R.string.alarm_repeat_every_day);
        } else if (mode == 0x3e) {
            String[] dayOfWeek = context.getResources().getStringArray(R.array.alarm_weeks_workday);
            for (int i = 0; i < dayOfWeek.length; i++) {
                int temp = mode;
                temp = temp >> i + 1;
                temp = temp & 0x01;
                if (temp == 0x01) {
                    sb.append(dayOfWeek[i]);
                    sb.append(" ");
                }
            }
            result = sb.toString().trim().replaceAll(" ", "，");
        } else {
            String[] dayOfWeek = context.getResources().getStringArray(R.array.alarm_weeks);
            for (int i = 0; i < dayOfWeek.length; i++) {
                int temp = mode;
                temp = temp >> i + 1;
                temp = temp & 0x01;
                if (temp == 0x01) {
                    sb.append(dayOfWeek[i]);
                    sb.append(" ");
                }
            }
            result = sb.toString().trim().replaceAll(" ", "，");
        }
        return result;
    }

    @SuppressLint("MissingPermission")
    private static boolean checkConnectedEdrIsOverLimit(RCSPController controller, BluetoothDevice device) {
        if (!PermissionUtil.checkHasConnectPermission(MainApplication.getApplication()))
            return true;
        boolean ret;
        List<BluetoothDevice> devices = BluetoothUtil.getSystemConnectedBtDeviceList();
        int count = 0;
        for (BluetoothDevice edrDevice : devices) {
            if (edrDevice.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC ||
                    edrDevice.getType() == BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
                //是相同设备，但是未连接通讯协议
                if (DeviceAddrManager.getInstance().isMatchDevice(edrDevice, device) && !controller.isDeviceConnected(device)) {
                    return false;
                }
                count++;
            }
        }
        ret = count >= SConstant.MULTI_DEVICE_MAX_NUMBER;
        return ret;
    }

    //检测是否可以去连接设备
    private static boolean checkCanConnectToDevice(RCSPController controller, Context context, BluetoothDevice device) {
        if (device == null) {
            return false;
        } else if (!BluetoothUtil.isBluetoothEnable()) {
            ToastUtil.showToastShort(context.getString(R.string.bluetooth_not_enable));
            return false;
        } else if (controller.isConnecting()) {
            ToastUtil.showToastShort(context.getString(R.string.device_connecting_tips));
            return false;
        } else if (checkConnectedEdrIsOverLimit(controller, device)) { //连接设备已达到上限
            ToastUtil.showToastShort(context.getString(R.string.connect_device_over_limit));
            return false;
        }
        return true;
    }

    private static int convertConnectionState(int status) {
        switch (status) {
            case BluetoothProfile.STATE_CONNECTED:
                return StateCode.CONNECTION_OK;
            case BluetoothProfile.STATE_CONNECTING:
                return StateCode.CONNECTION_CONNECTING;
            default:
                return StateCode.CONNECTION_DISCONNECT;
        }
    }

    private static void postConnectionChange(BluetoothDevice device, int state) {
        Intent intent = new Intent(SConstant.ACTION_DEVICE_CONNECTION_CHANGE);
        intent.putExtra(SConstant.KEY_DEVICE, device);
        intent.putExtra(SConstant.KEY_CONNECTION, state);
        MainApplication.getApplication().sendBroadcast(intent);
    }
}
