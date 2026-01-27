package com.jieli.btsmart.tool.bluetooth.rcsp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.device.alarm.AlarmBean;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.JL_DeviceType;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.BluetoothCallbackImpl;
import com.jieli.bluetooth.interfaces.bluetooth.IBluetoothOperation;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
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

    private final static String TAG = BTRcspHelper.class.getSimpleName();

    /**
     * 连接设备参数
     */
    private static ConnectDeviceParam connectDeviceParam;

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
     * @param device         待连接的设备
     * @param bleScanMessage 广播信息
     */
    public static void connectDeviceByMessage(RCSPController controller, BluetoothDevice device, BleScanMessage bleScanMessage) {
        int way = -1;
        final Context context = MainApplication.getApplication();
        BluetoothDevice targetDevice = device;
        if (bleScanMessage != null) {
            final int devType = bleScanMessage.getDeviceType();
            boolean isMandatoryUseBLE = SConstant.IS_USE_BLE_WAY
                    || devType == JL_DeviceType.JL_DEVICE_TYPE_CHARGING_BIN
                    || controller.getBluetoothOption().isMandatoryUseBLE();
            if (isMandatoryUseBLE) {
                way = BluetoothConstant.PROTOCOL_TYPE_BLE;
            } else {
                way = bleScanMessage.getConnectWay();
                boolean isSupportBrEdr = bleScanMessage.getEdrStatus() != StateCode.TWS_HEADSET_STATUS_NOT_SUPPORTED_BR_EDR;
                if (isSupportBrEdr) {
                    if (devType == JL_DeviceType.JL_DEVICE_TYPE_TWS_HEADSET_V1) { //TWS耳机
                        if (!bleScanMessage.isSupportLeAudio() || !bleScanMessage.isLeAudioConnected()) { //不支持LE Audio 或者 LE Audio未连接
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
                                    if (connectDeviceParam != null) { //正在连接
//                                        ToastUtil.showToastShort(context.getString(R.string.device_connecting_tips));
                                        JL_Log.i(TAG, "connectDeviceByMessage", "(Connect BR/EDR) Device is connecting. " + connectDeviceParam);
//                                        return;
                                    }
                                    final BluetoothCallbackImpl bluetoothCallback = new BluetoothCallbackImpl() {
                                        @Override
                                        public void onBrEdrConnection(BluetoothDevice device, int status) {
                                            JL_Log.d(TAG, "connectDeviceByMessage", "onBrEdrConnection ---> device : " + device + ", status : " + BluetoothUtil.printBtConnection(status));
                                            if (!isConnectingDevice(device)) return;//是否正在连接的设备
                                            postConnectionChange(device, convertConnectionState(status));
                                            if (status == BluetoothProfile.STATE_CONNECTING) return;
                                            final BluetoothDevice bleDevice = connectDeviceParam.getDevice();
                                            final BleScanMessage scanMessage = connectDeviceParam.getScanMessage();
                                            connectDeviceParam = null;
                                            bluetoothOperation.unregisterBluetoothCallback(this);
                                            if (status == BluetoothProfile.STATE_CONNECTED) {
                                                scanMessage.setAction(StateCode.TWS_HEADSET_STATUS_CONNECTED);
                                                connectDeviceByMessage(controller, bleDevice, scanMessage);
                                            }
                                        }
                                    };
                                    bluetoothOperation.registerBluetoothCallback(bluetoothCallback);
                                    connectDeviceParam = new ConnectDeviceParam(device, edrDevice, bleScanMessage);
                                    postConnectionChange(edrDevice, StateCode.CONNECTION_CONNECTING);
                                    if (bluetoothOperation.startConnectByBreProfiles(edrDevice) != ErrorCode.ERR_NONE) {
                                        connectDeviceParam = null;
                                        bluetoothOperation.unregisterBluetoothCallback(bluetoothCallback);
                                        //连接经典蓝牙失败
                                        postConnectionChange(edrDevice, StateCode.CONNECTION_DISCONNECT);
                                    }
                                    return;
                                }
                            }
                        }
                    } else if (devType == JL_DeviceType.JL_DEVICE_TYPE_SOUND_CARD) { //声卡设备，强制连接BLE
                        way = BluetoothConstant.PROTOCOL_TYPE_BLE;
                    }
                }
            }

            if (bleScanMessage.isSupportLeAudio() && bleScanMessage.isReuseLeAudioAddress() && bleScanMessage.isLeAudioConnected()) {
                way = BluetoothConstant.PROTOCOL_TYPE_BLE;
                BluetoothDevice temp = BluetoothUtil.getRemoteDevice(bleScanMessage.getEdrAddr());
                if (null != temp) {
                    targetDevice = temp;
                }
            }

            if(way == BluetoothConstant.PROTOCOL_TYPE_GATT_OVER_BR_EDR && MainApplication.isHarmonyOSNext()){
                //目前纯血鸿蒙暂不支持Gatt Over BR/EDR, 所以改成SPP方式连接
                way = BluetoothConstant.PROTOCOL_TYPE_SPP;
            }

            if (way == BluetoothConstant.PROTOCOL_TYPE_SPP || way == BluetoothConstant.PROTOCOL_TYPE_GATT_OVER_BR_EDR) {
                BluetoothDevice temp = BluetoothUtil.getRemoteDevice(bleScanMessage.getEdrAddr());
                JL_Log.d(TAG, "connectDeviceByMessage", "classicDevice : " + temp);
                if (null == temp) {
                    way = BluetoothConstant.PROTOCOL_TYPE_BLE;
                } else {
                    targetDevice = temp;
                }
            }
            JL_Log.d(TAG, "connectDeviceByMessage", "targetDevice : " + targetDevice + ", way : " + BluetoothConstant.printProtocolType(way));
            DeviceAddrManager.getInstance().saveDeviceConnectWay(targetDevice, way);
        } else {
            if (isSystemConnectedDevice(context, targetDevice)) { //系统已连接
                if (BluetoothUtil.deviceHasProfile(context, targetDevice, BluetoothConstant.UUID_JIE_LI_SPP)
                        && !MainApplication.isHarmonyOSNext()) {  //目前纯血鸿蒙暂不支持Gatt Over BR/EDR, 所以改成SPP方式连接
                    //具有私有AE00协议，判断连接Gatt Over BR/EDR
                    way = BluetoothConstant.PROTOCOL_TYPE_GATT_OVER_BR_EDR;
                } else if (BluetoothUtil.deviceHasProfile(context, targetDevice, BluetoothConstant.UUID_SPP)) {
                    //具有标准SPP通道，优先连接SPP
                    way = BluetoothConstant.PROTOCOL_TYPE_SPP;
                }
            }
        }
        if (checkCanConnectToDevice(controller, context, targetDevice)) {
            if (controller.isDeviceConnected(targetDevice)) return; //设备已连接
            if (connectDeviceParam != null) { //正在连接
                JL_Log.i(TAG, "connectDeviceByMessage", "Device is connecting. " + connectDeviceParam);
//                return;
            }
            //当没有设备连接的时候才暂停播放，有设备连接的情况下不暂停
            if (!controller.isDeviceConnected() && PlayControlImpl.getInstance().isPlay()) {
                PlayControlImpl.getInstance().pause();
            }
            final BTRcspEventCallback btRcspEventCallback = new BTRcspEventCallback() {
                @Override
                public void onConnection(BluetoothDevice device, int status) {
                    if (!isConnectingDevice(device) || status == StateCode.CONNECTION_CONNECTING)
                        return;
                    if (status == StateCode.CONNECTION_OK) {
                        final BleScanMessage scanMessage = connectDeviceParam.getScanMessage();
                        if (null != scanMessage) { //更新广播包信息
                            DeviceAddrManager.getInstance().updateHistoryBtDeviceInfo(device, scanMessage.getVersion(),
                                    scanMessage.getDeviceType());
                        }
                    }
                    controller.removeBTRcspEventCallback(this);
                    connectDeviceParam = null;
                }
            };
            controller.addBTRcspEventCallback(btRcspEventCallback);
            connectDeviceParam = new ConnectDeviceParam(device, targetDevice, bleScanMessage);
            if (way == -1) {
                controller.connectDevice(targetDevice);
            } else {
                controller.connectDevice(targetDevice, way);
            }
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

    private static boolean isConnectingDevice(BluetoothDevice device) {
        if (null == connectDeviceParam || null == device) return false;
        return BluetoothUtil.deviceEquals(device, connectDeviceParam.getConnectingDevice());
    }

    /**
     * 是否系统已连接设备
     *
     * @param context Context 上下文
     * @param device BluetoothDevice 蓝牙设备
     * @return boolean 结果
     */
    private static boolean isSystemConnectedDevice(@NonNull Context context, BluetoothDevice device) {
        if (null == device) return false;
        List<BluetoothDevice> systemConnectedDeviceList = BluetoothUtil.getSystemConnectedBtDeviceList(context);
        if (systemConnectedDeviceList.isEmpty()) return false;
        for (BluetoothDevice connectedDev : systemConnectedDeviceList) {
            if (BluetoothUtil.deviceEquals(connectedDev, device)) {
                return true;
            }
        }
        return false;
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

    private static class ConnectDeviceParam {
        /**
         * BLE设备
         */
        @NonNull
        private final BluetoothDevice device;
        /**
         * 需要连接设备
         */
        private BluetoothDevice connectingDevice;
        /**
         * 广播信息
         */
        private final BleScanMessage scanMessage;

        public ConnectDeviceParam(@NonNull BluetoothDevice device, BluetoothDevice connectingDevice, BleScanMessage scanMessage) {
            this.device = device;
            this.connectingDevice = connectingDevice;
            this.scanMessage = scanMessage;
        }

        @NonNull
        public BluetoothDevice getDevice() {
            return device;
        }

        public BleScanMessage getScanMessage() {
            return scanMessage;
        }

        public BluetoothDevice getConnectingDevice() {
            return connectingDevice;
        }

        public ConnectDeviceParam setConnectingDevice(BluetoothDevice connectingDevice) {
            this.connectingDevice = connectingDevice;
            return this;
        }

        @Override
        public String toString() {
            return "ConnectDeviceParam{" +
                    "device=" + device +
                    ", connectingDevice=" + connectingDevice +
                    ", scanMessage=" + scanMessage +
                    '}';
        }
    }
}
