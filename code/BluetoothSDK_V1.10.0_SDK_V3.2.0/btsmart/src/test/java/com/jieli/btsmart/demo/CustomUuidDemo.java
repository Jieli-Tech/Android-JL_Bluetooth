package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.BluetoothCallbackImpl;
import com.jieli.bluetooth.utils.JL_Log;

import org.junit.Test;

import java.util.List;
import java.util.UUID;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 自定义UUID使用示例
 * <p>
 * 适用情况：客户除了想使用RCSP的UUID，还想使用其他UUID来传输自定义协议数据
 * </p>
 * @since 2022/12/30
 */
public class CustomUuidDemo {
    private final String TAG = CustomUuidDemo.class.getSimpleName();
    private final RCSPController mController = RCSPController.getInstance(); //初始化RCSPController对象

    private final UUID serverUUID = UUID.fromString("自定义的服务UUID");
    private final UUID writeCharacteristicUUID = UUID.fromString("自定义的写特征UUID");
    private final UUID notifyCharacteristicUUID = UUID.fromString("自定义的通知特征UUID");

    private boolean isEnableOk;   //是否自定义通知使能成功

    private static final int DEFAULT_TIMEOUT = 6000;

    private static final int MSG_DISCOVERY_SERVER = 0x01;
    private static final int MSG_ENABLE_CUSTOM_UUID = 0x02;
    private final Handler mUIHandler = new Handler(Looper.getMainLooper(), msg -> {
        switch (msg.what) {
            case MSG_DISCOVERY_SERVER: {
                //发现服务超时， 视为发现服务失败情况处理
                if (msg.obj instanceof BluetoothGatt) {
                    BluetoothGatt gatt = (BluetoothGatt) msg.obj;
                    //TODO: 回调设备连接异常
//                    gatt.disconnect(); //可以判断异常BLE链接
                }
                break;
            }
            case MSG_ENABLE_CUSTOM_UUID: {
                //使能UUID超时，按照使能UUID失败处理
                if (msg.obj instanceof BluetoothGatt) {
                    BluetoothGatt gatt = (BluetoothGatt) msg.obj;
                    //TODO: 处理使能UUID失败的情况
                }
                break;
            }
        }
        return true;
    });

    @Test
    public void init() {
        //可以在配置RCSPController设置，也可以在连接设备前重新配置
//        mController.getBtOperation().getBluetoothOption().setUseDeviceAuth(false); //是否开启设备认证，根据项目需求
        mController.getBtOperation().registerBluetoothCallback(bluetoothCallback);   //注册蓝牙事件监听回调，对自定义UUID进行使能
    }

    @Test
    public boolean sendCustomData(byte[] data) {
        if (!mController.isDeviceConnected()) { //设备未连接
            return false;
        }
        //TODO: 视情况而定，如果需要设备回复数据的，必须等待使能成功。反之，则不用
        if (!isEnableOk) { //还没使能成功
            return false;
        }
        boolean ret = mController.getBtOperation().writeDataToBLEDevice(getConnectedDevice(), serverUUID, writeCharacteristicUUID, data);
        System.out.println("sendCustomData >>> " + ret);
        return ret;
    }

    @Test
    public void release() {
        isEnableOk = false;
        //断开设备
        mController.getBtOperation().disconnectBtDevice(getConnectedDevice());
        //注销回调
        mController.getBtOperation().unregisterBluetoothCallback(bluetoothCallback);
        //如果不需要用 RCSPController， 也可以释放
//        mController.destroy();
        mUIHandler.removeCallbacksAndMessages(null);
    }

    private BluetoothDevice getConnectedDevice() {
        return mController.getUsingDevice();
    }

    private final BluetoothCallbackImpl bluetoothCallback = new BluetoothCallbackImpl() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
            isEnableOk = false;
            if (status == StateCode.CONNECTION_OK) {
                //设备连接成功
                DeviceInfo deviceInfo = mController.getDeviceInfo(device);
                if (!deviceInfo.isMandatoryUpgrade()) { //设备不需要强制升级，正常模式
                    if (mController.getBtOperation().isConnectedBLEDevice(device)) { //是BLE连接成功
                        //使能自定义通知特征UUID
                        BluetoothGatt gatt = mController.getBtOperation().getDeviceGatt(device);
                        List<BluetoothGattService> services = gatt.getServices();
                        if (services != null && !services.isEmpty()) {
                            onBleServiceDiscovery(device, BluetoothGatt.GATT_SUCCESS, services);
                        } else {
                            if (!gatt.discoverServices()) {
                                //TODO: 发现服务失败, 可以考虑延时重试
                            } else {
                                mUIHandler.removeMessages(MSG_DISCOVERY_SERVER);
                                mUIHandler.sendMessageDelayed(mUIHandler.obtainMessage(MSG_DISCOVERY_SERVER, gatt), DEFAULT_TIMEOUT);
                            }
                        }
                        return;
                    }
                }
                //TODO: 可以在此处回调设备连接成功状态
            }
        }

        @Override
        public void onBleServiceDiscovery(BluetoothDevice device, int status, List<BluetoothGattService> services) {
            mUIHandler.removeMessages(MSG_DISCOVERY_SERVER);
            if (status != BluetoothGatt.GATT_SUCCESS || services == null) return;
            for (BluetoothGattService service : services) {
                if (serverUUID.equals(service.getUuid())) { //找到自定义ServerUUID
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(notifyCharacteristicUUID);
                    if (characteristic != null) { //找到自定义通知特征UUID
                        BluetoothGatt gatt = mController.getBtOperation().getDeviceGatt(device);
                        boolean ret = enableBLEDeviceNotification(gatt, serverUUID, notifyCharacteristicUUID);
                        System.out.printf("开始使能[%s]的操作结果:%s\n", notifyCharacteristicUUID, ret);
                        if (!ret) {
                            //TODO: 使能失败处理
                        } else {
                            //开启一个使能UUID回调超时任务
                            mUIHandler.removeMessages(MSG_ENABLE_CUSTOM_UUID);
                            mUIHandler.sendMessageDelayed(mUIHandler.obtainMessage(MSG_ENABLE_CUSTOM_UUID, gatt), DEFAULT_TIMEOUT);
                        }
                    }
                    break;
                }
            }
        }

        @Override
        public void onBleNotificationStatus(BluetoothDevice device, UUID serviceUuid, UUID characteristicUuid, boolean bEnabled) {
            //回调BLE的使能结果
            if (serverUUID.equals(serviceUuid) && notifyCharacteristicUUID.equals(characteristicUuid) && mUIHandler.hasMessages(MSG_ENABLE_CUSTOM_UUID)) {
                mUIHandler.removeMessages(MSG_ENABLE_CUSTOM_UUID);
                //自定义UUID的使能结果
                isEnableOk = bEnabled;
                //使能自定义UUID的流程完成，可以开始发数据到自定义通道
                //TODO: 可以在此处回调设备连接成功状态
            }
        }

        @Override
        public void onBleDataNotification(BluetoothDevice device, UUID serviceUuid, UUID characteristicsUuid, byte[] data) {
            if (serverUUID.equals(serviceUuid) && notifyCharacteristicUUID.equals(characteristicsUuid)) {
                //设备通知的数据，走自定义服务通道
                //TODO: 可以回调到上层使用
            }
        }
    };

    /**
     * 用于开启蓝牙BLE设备Notification服务
     *
     * @param gatt               Gatt对象
     * @param serviceUUID        服务UUID
     * @param characteristicUUID characteristic UUID
     * @return 结果 true 则等待系统回调BLE服务
     */
    private boolean enableBLEDeviceNotification(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID) {
        if (null == gatt) {
            JL_Log.w(TAG, "enableBLEDeviceNotification : bluetooth gatt is null....");
            return false;
        }
        BluetoothGattService gattService = gatt.getService(serviceUUID);
        if (null == gattService) {
            JL_Log.w(TAG, "enableBLEDeviceNotification : bluetooth gatt service is null....");
            return false;
        }
        BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(characteristicUUID);
        if (null == characteristic) {
            JL_Log.w(TAG, "enableBLEDeviceNotification : bluetooth characteristic is null....");
            return false;
        }
        boolean bRet = gatt.setCharacteristicNotification(characteristic, true);
        if (bRet) {
            List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
            if (descriptors == null || descriptors.size() == 0) {
                bRet = false;
            }
            if (descriptors != null) {
                JL_Log.d(TAG, "descriptors size = " + descriptors.size());
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    bRet = tryToWriteDescriptor(gatt, descriptor, 0, false);
                    if (!bRet) {
                        JL_Log.w(TAG, "tryToWriteDescriptor failed....");
                    }
                }
            }
        } else {
            JL_Log.w(TAG, "setCharacteristicNotification is failed....");
        }
        JL_Log.w(TAG, "enableBLEDeviceNotification ret : " + bRet);
        return bRet;
    }

    /**
     * 尝试使能BLE服务属性
     *
     * @param bluetoothGatt  BluetoothGatt对象
     * @param descriptor     属性
     * @param retryCount     失败次数
     * @param isSkipSetValue 是否跳过设值
     * @return 结果
     */
    private boolean tryToWriteDescriptor(BluetoothGatt bluetoothGatt, BluetoothGattDescriptor descriptor, int retryCount, boolean isSkipSetValue) {
        boolean ret = isSkipSetValue;
        if (!ret) {
            ret = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            JL_Log.i(TAG, "tryToWriteDescriptor : descriptor : setValue  ret : " + ret);
            if (!ret) {
                retryCount++;
                if (retryCount >= 3) {
                    return false;
                } else {
                    JL_Log.i(TAG, "-tryToWriteDescriptor- : retryCount : " + retryCount + ", isSkipSetValue :  false");
                    SystemClock.sleep(50);
                    tryToWriteDescriptor(bluetoothGatt, descriptor, retryCount, false);
                }
            } else {
                retryCount = 0;
            }
        }
        if (ret) {
            ret = bluetoothGatt.writeDescriptor(descriptor);
            JL_Log.i(TAG, "tryToWriteDescriptor : writeDescriptor  ret : " + ret);
            if (!ret) {
                retryCount++;
                if (retryCount >= 3) {
                    return false;
                } else {
                    JL_Log.i(TAG, "-tryToWriteDescriptor- 2222 : retryCount : " + retryCount + ", isSkipSetValue :  true");
                    SystemClock.sleep(50);
                    tryToWriteDescriptor(bluetoothGatt, descriptor, retryCount, true);
                }
            }
        }
        return ret;
    }
}
