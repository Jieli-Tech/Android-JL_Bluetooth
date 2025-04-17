package com.jieli.btsmart.demo.bluetooth.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.demo.bluetooth.ble.interfaces.BleEventCallback;
import com.jieli.btsmart.demo.bluetooth.ble.interfaces.OnWriteDataCallback;
import com.jieli.btsmart.demo.bluetooth.ble.model.BleDevice;
import com.jieli.btsmart.demo.bluetooth.ble.model.BleScanInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Ble连接管理类
 *
 * @author zqjasonZhong
 * @since 2020/7/16
 */
public class BleManager {
    private final static String TAG = BleManager.class.getSimpleName();

    private final static boolean IS_HID_DEVICE = false;
    private final static int REQUEST_MTU = BluetoothConstant.BLE_MTU_MAX;

    private final Context mContext;
    @SuppressLint("StaticFieldLeak")
    private volatile static BleManager instance;

    private BaseBtAdapterReceiver mAdapterReceiver;
    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;

    private volatile BluetoothDevice mUsingDevice;            //正在通讯的设备

    private final Map<String, BleDevice> mBleDeviceMap = new HashMap<>();
    private final List<BluetoothDevice> mDiscoveredBleDevices = new ArrayList<>();
    private final BleEventCallbackManager mCallbackManager = new BleEventCallbackManager();

    private volatile boolean isBleScanning;
    private NotifyCharacteristicRunnable mNotifyCharacteristicRunnable;


    private final static int MIN_CONNECT_TIME = 8 * 1000; //连接最小时间超时
    //BLE服务UUID
    public final static UUID BLE_UUID_SERVICE = BluetoothConstant.UUID_SERVICE;
    //BLE的写特征UUID
    public final static UUID BLE_UUID_WRITE = BluetoothConstant.UUID_WRITE;
    //BLE的通知特征UUID
    public final static UUID BLE_UUID_NOTIFICATION = BluetoothConstant.UUID_NOTIFICATION;
    //BLE的通知特征的描述符UUID
    public final static UUID BLE_UUID_NOTIFICATION_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    /**
     * 发送数据最大超时 - 8 秒
     */
    public final static int SEND_DATA_MAX_TIMEOUT = 8000; //8 s
    private final static int SCAN_BLE_TIMEOUT = 12 * 1000;  //建议搜索BLE最小时间
    private final static int CONNECT_BLE_TIMEOUT = 40 * 1000;

    private final static int CALLBACK_TIMEOUT = 6000;
    private final static int RECONNECT_BLE_DELAY = 2000;

    private final static int MSG_SCAN_BLE_TIMEOUT = 0x1010;
    private final static int MSG_CONNECT_BLE_TIMEOUT = 0x1011;
    private final static int MSG_SCAN_HID_DEVICE = 0X1012;
    private final static int MSG_NOTIFY_BLE_TIMEOUT = 0x1013;
    private final static int MSG_CHANGE_BLE_MTU_TIMEOUT = 0x1014;
    private final static int MSG_BLE_DISCOVER_SERVICES_CALLBACK_TIMEOUT = 0x1015;
    private final static int MSG_DISCONNECT_BLE_TIMEOUT = 0x1016;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_SCAN_BLE_TIMEOUT: {
                    JL_Log.i(TAG, "MSG_SCAN_BLE_TIMEOUT", "");
                    stopLeScan();
                    break;
                }
                case MSG_CONNECT_BLE_TIMEOUT: {
                    if (!(msg.obj instanceof BluetoothDevice)) return false;
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    JL_Log.i(TAG, "MSG_CONNECT_BLE_TIMEOUT", "device : " + printDeviceInfo(device));
                    disconnectBleDevice(device);
                    break;
                }
                case MSG_SCAN_HID_DEVICE: {
                    List<BluetoothDevice> lists = BluetoothUtil.getSystemConnectedBtDeviceList(mContext);
                    if (null != lists && CommonUtil.checkHasConnectPermission(mContext)) {
                        for (BluetoothDevice device : lists) {
                            if (device.getType() != BluetoothDevice.DEVICE_TYPE_CLASSIC &&
                                    device.getBondState() == BluetoothDevice.BOND_BONDED) {
                                handleDiscoveryBle(device, new BleScanInfo().setEnableConnect(true));
                            }
                        }
                    }
                    mHandler.sendEmptyMessageDelayed(MSG_SCAN_HID_DEVICE, 1000);
                    break;
                }
                case MSG_NOTIFY_BLE_TIMEOUT: {
                    if (!(msg.obj instanceof BluetoothDevice)) return false;
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    JL_Log.i(TAG, "MSG_NOTIFY_BLE_TIMEOUT", "device : " + printDeviceInfo(device));
                    disconnectBleDevice(device);
                    break;
                }
                case MSG_CHANGE_BLE_MTU_TIMEOUT: {
                    if (!(msg.obj instanceof BluetoothDevice)) return false;
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    BleDevice bleDevice = getBleDevice(device);
                    JL_Log.i(TAG, "MSG_CHANGE_BLE_MTU_TIMEOUT", "device : " + printDeviceInfo(device) + ", " + bleDevice);
                    if (null == bleDevice) return false;
                    handleBleConnection(device, BluetoothProfile.STATE_CONNECTED);
                    break;
                }
                case MSG_BLE_DISCOVER_SERVICES_CALLBACK_TIMEOUT: {
                    if (!(msg.obj instanceof BluetoothDevice)) return false;
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    BleDevice bleDevice = getBleDevice(device);
                    JL_Log.i(TAG, "MSG_BLE_DISCOVER_SERVICES_CALLBACK_TIMEOUT", "device : " + printDeviceInfo(device) + ", " + bleDevice);
                    if (null == bleDevice) return false;
                    final BluetoothGatt gatt = bleDevice.getGatt();
                    if (gatt != null) {
                        List<BluetoothGattService> services = gatt.getServices();
                        if (services != null && !services.isEmpty()) {
                            mBluetoothGattCallback.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS);
                            break;
                        }
                    }
                    JL_Log.i(TAG, "MSG_BLE_DISCOVER_SERVICES_CALLBACK_TIMEOUT", "disconnectBleDevice");
                    bleDevice.setNeedReconnect(true);
                    disconnectBleDevice(device);
                    break;
                }
                case MSG_DISCONNECT_BLE_TIMEOUT: {
                    if (!(msg.obj instanceof BluetoothDevice)) return false;
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    final BluetoothGatt gatt = getConnectedBtGatt(device);
                    JL_Log.d(TAG, "MSG_DISCONNECT_BLE_TIMEOUT", "device : " + printDeviceInfo(device) + ", gatt : " + gatt);
                    if (null == gatt) return false;
                    closeGatt(gatt);
                    handleBleConnection(device, BluetoothProfile.STATE_DISCONNECTED);
                    break;
                }
            }
            return true;
        }
    });


    private BleManager(Context context) {
        mContext = CommonUtil.checkNotNull(context);
        if (CommonUtil.getMainContext() == null) {
            CommonUtil.setMainContext(context);
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (Build.VERSION.SDK_INT >= LOLLIPOP && mBluetoothAdapter != null) {
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        registerReceiver();
    }

    public static BleManager getInstance() {
        if (instance == null) {
            synchronized (BleManager.class) {
                if (instance == null) {
                    instance = new BleManager(MainApplication.getApplication());
                    JL_Log.w(TAG, "init", "instance : " + instance);
                }
            }
        }
        return instance;
    }

    /**
     * 获取已连接的BLE设备列表
     *
     * @param context 上下文
     * @return 已连接的BLE设备列表
     */
    @SuppressLint("MissingPermission")
    public static List<BluetoothDevice> getConnectedBleDeviceList(Context context) {
        if (!CommonUtil.checkHasConnectPermission(context)) return null;
        BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            return mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        }
        return null;
    }

    public void destroy() {
        JL_Log.w(TAG, "destroy", "instance : " + instance);
        unregisterReceiver();
        stopConnectTimeout();
        clearConnectedBleDevices();

        if (isBleScanning()) stopLeScan();
        isBleScanning(false);
        mDiscoveredBleDevices.clear();

        mCallbackManager.release();
        mHandler.removeCallbacksAndMessages(null);
        instance = null;
    }

    public void registerBleEventCallback(BleEventCallback callback) {
        mCallbackManager.registerBleEventCallback(callback);
    }

    public void unregisterBleEventCallback(BleEventCallback callback) {
        mCallbackManager.unregisterBleEventCallback(callback);
    }

    public boolean isBluetoothEnable() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public boolean isBleScanning() {
        return isBleScanning;
    }

    @SuppressLint("MissingPermission")
    public boolean startLeScan(long timeout) {
        if (!checkScanEnv("startLeScan")) return false;
        if (!CommonUtil.isHasLocationPermission(mContext)) {
            JL_Log.w(TAG, "startLeScan", "Missing location permissions.");
            return false;
        }
        if (timeout <= 0) timeout = SCAN_BLE_TIMEOUT;
        if (isBleScanning()) {
            JL_Log.i(TAG, "startLeScan", "BLE is searching.");
            if (mBluetoothLeScanner != null && Build.VERSION.SDK_INT >= LOLLIPOP) {
                mBluetoothLeScanner.flushPendingScanResults(mScanCallback);
            }
            mDiscoveredBleDevices.clear();
            mHandler.removeMessages(MSG_SCAN_BLE_TIMEOUT);
            mHandler.sendEmptyMessageDelayed(MSG_SCAN_BLE_TIMEOUT, timeout);
            isBleScanning(true);
            syncSystemBleDevice();
            return true;
        }
        boolean ret;
        if (Build.VERSION.SDK_INT >= LOLLIPOP && mBluetoothLeScanner != null) {
            ScanSettings scanSettings;
            int scanMode = ScanSettings.SCAN_MODE_LOW_LATENCY; //修改搜索BLE模式 -- 均衡模式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ScanSettings.Builder builder = new ScanSettings.Builder()
                        .setScanMode(scanMode)
                        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                        .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    builder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED);
                }
                scanSettings = builder.build();
            } else {
                scanSettings = new ScanSettings.Builder()
                        .setScanMode(scanMode)
                        .build();
            }
            List<ScanFilter> filters = new ArrayList<>();
            mBluetoothLeScanner.startScan(filters, scanSettings, mScanCallback);
            ret = true;
        } else {
            ret = mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
        JL_Log.i(TAG, "startLeScan", CommonUtil.formatString("%s. timeout : %d.", ret, timeout));
        isBleScanning(ret);
        if (ret) {
            mDiscoveredBleDevices.clear();
            mHandler.removeMessages(MSG_SCAN_BLE_TIMEOUT);
            mHandler.sendEmptyMessageDelayed(MSG_SCAN_BLE_TIMEOUT, timeout);
            syncSystemBleDevice();
        }
        return ret;
    }

    @SuppressLint("MissingPermission")
    public boolean stopLeScan() {
        if (!checkScanEnv("stopLeScan")) return false;
        if (!isBleScanning()) return false;
        try {
            if (Build.VERSION.SDK_INT >= LOLLIPOP && mBluetoothLeScanner != null) {
                mBluetoothLeScanner.stopScan(mScanCallback);
            } else {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mHandler.removeMessages(MSG_SCAN_BLE_TIMEOUT);
        mHandler.removeMessages(MSG_SCAN_HID_DEVICE);
        isBleScanning(false);
        return true;
    }

    public BluetoothDevice getConnectedBtDevice() {
        return mUsingDevice;
    }

    public BluetoothGatt getConnectedBtGatt(BluetoothDevice device) {
        BleDevice bleDevice = getBleDevice(device);
        if (null == bleDevice) return null;
        return bleDevice.getGatt();
    }

    public BluetoothDevice getConnectedBLEDevice(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) return null;
        List<BluetoothDevice> devices = getConnectedDeviceList();
        if (devices.isEmpty()) return null;
        for (BluetoothDevice device : devices) {
            if (device.getAddress().equals(address)) {
                return device;
            }
        }
        return null;
    }

    /**
     * 获取已连接设备列表
     * <p>
     * 按照连接时间倒序
     * </p>
     *
     * @return 设备列表
     */
    public List<BluetoothDevice> getConnectedDeviceList() {
        if (mBleDeviceMap.isEmpty()) return new ArrayList<>();
        List<BleDevice> bleDevices = getSortList();
        List<BluetoothDevice> devices = new ArrayList<>();
        for (BleDevice bleDevice : bleDevices) {
            if (null == bleDevice || null == bleDevice.getGatt()
                    || bleDevice.getConnection() != BluetoothProfile.STATE_CONNECTED) continue;
            final BluetoothGatt gatt = bleDevice.getGatt();
            final BluetoothDevice device = gatt.getDevice();
            if (null != device) {
                devices.add(device);
            }
        }
        return devices;
    }


    public boolean isConnecting() {
        return getConnectingDevice() != null;
    }

    public boolean isConnectingDevice(BluetoothDevice device) {
        return BluetoothUtil.deviceEquals(getConnectingDevice(), device);
    }

    public boolean isConnectedDevice(BluetoothDevice device) {
        if (null == device) return false;
        return isConnectedDevice(device.getAddress());
    }

    public boolean isConnectedDevice(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) return false;
        List<BluetoothDevice> devices = getConnectedDeviceList();
        if (devices.isEmpty()) return false;
        for (BluetoothDevice device : devices) {
            if (device.getAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    public BluetoothDevice getConnectingDevice() {
        if (mBleDeviceMap.isEmpty()) return null;
        for (BleDevice bleDevice : mBleDeviceMap.values()) {
            if (bleDevice.getConnection() == BluetoothProfile.STATE_CONNECTING) {
                return bleDevice.getDevice();
            }
        }
        return null;
    }

    public int getBleMtu(BluetoothDevice device) {
        final BleDevice bleDevice = getBleDevice(device);
        if (null == bleDevice) return 0;
        return bleDevice.getMtu();
    }

    @SuppressLint("MissingPermission")
    public boolean connectBleDevice(BluetoothDevice device) {
        if (!checkConnectEnv("connectBleDevice", device) || !isBluetoothEnable())
            return false;
       /* if (mUsingDevice != null) {
            JL_Log.e(TAG, "BleDevice is connected, please call disconnectBleDevice method at first.");
            setReconnectDevAddr(null);
            return false;
        }*/
        if (isConnectedDevice(device)) {
            mCallbackManager.onBleConnection(device, BluetoothProfile.STATE_CONNECTED);
            return true;
        }
        final BluetoothDevice connectingDevice = getConnectingDevice();
        if (connectingDevice != null) {
            JL_Log.e(TAG, "connectBleDevice", CommonUtil.formatString("Device(%s) is connecting, please wait.", printDeviceInfo(connectingDevice)));
            return false;
        }
        if (isBleScanning()) {
            stopLeScan();
        }
        //回调BLE连接中状态
        final BleDevice bleDevice = addBleDevice(device);
        startConnectTimeout(device);
        handleBleConnection(device, BluetoothProfile.STATE_CONNECTING);

        BluetoothGatt gatt = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                gatt = device.connectGatt(
                        mContext,
                        false,
                        mBluetoothGattCallback,
                        BluetoothDevice.TRANSPORT_LE
                );
            } else {
                gatt = device.connectGatt(
                        mContext,
                        false,
                        mBluetoothGattCallback
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean ret = gatt != null;
        bleDevice.setGatt(gatt);
        JL_Log.d(TAG, "connectBleDevice", ret ? CommonUtil.formatString("Prepare to connect to BLE(%s)", printDeviceInfo(device))
                : CommonUtil.formatString("Failed to connect to BLE(%s)", printDeviceInfo(device)));
        if (!ret) {
            handleBleConnection(device, BluetoothProfile.STATE_CONNECTING);
        }
        return ret;
    }

    @SuppressLint("MissingPermission")
    public void disconnectBleDevice(BluetoothDevice device) {
        if (!checkConnectEnv("disconnectBleDevice", device)) return;
        BleDevice bleDevice = getBleDevice(device);
        JL_Log.d(TAG, "disconnectBleDevice", "device: " + printDeviceInfo(device) + ", " + bleDevice);
        if (null == bleDevice) return;
        final BluetoothGatt gatt = bleDevice.getGatt();
        if (!isBluetoothEnable()) {
            bleDevice.setConnection(BluetoothProfile.STATE_DISCONNECTED);
        }
        switch (bleDevice.getConnection()) {
            case BluetoothProfile.STATE_CONNECTED: {
                if (null != gatt) {
                    JL_Log.d(TAG, "disconnectBleDevice", "Gatt#disconnect");
                    mHandler.removeMessages(MSG_DISCONNECT_BLE_TIMEOUT);
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_DISCONNECT_BLE_TIMEOUT, device), CALLBACK_TIMEOUT);
                    gatt.disconnect();
                }
                break;
            }
            case BluetoothProfile.STATE_CONNECTING: {
                if (null != gatt) {
                    gatt.disconnect();
                    closeGatt(gatt);
                }
                handleBleConnection(device, BluetoothProfile.STATE_DISCONNECTED);
                break;
            }
            default:
                removeConnectedBle(device);
                mCallbackManager.onBleConnection(device, BluetoothProfile.STATE_DISCONNECTED);
                break;
        }
    }

    public void writeDataByBleAsync(BluetoothDevice device, UUID serviceUUID, UUID characteristicUUID, byte[] data, OnWriteDataCallback callback) {
        addSendTask(device, serviceUUID, characteristicUUID, data, callback);
    }

    private void isBleScanning(boolean isScanning) {
        JL_Log.d(TAG, "isBleScanning", "isScanning : " + isScanning);
        isBleScanning = isScanning;
        mCallbackManager.onDiscoveryBleChange(isScanning);
        if (isBleScanning && IS_HID_DEVICE) {
            mHandler.sendEmptyMessage(MSG_SCAN_HID_DEVICE);
        }
    }

    private boolean checkScanEnv(String method) {
        if (null == mBluetoothAdapter) {
            JL_Log.w(TAG, method, "No support for Bluetooth function.");
            return false;
        }
        if (!CommonUtil.checkHasScanPermission(mContext)) {
            JL_Log.w(TAG, method, "Missing permission to search Bluetooth.");
            return false;
        }
        if (!isBluetoothEnable()) {
            JL_Log.w(TAG, method, "Bluetooth is close.");
            return false;
        }
        return true;
    }

    private boolean checkConnectEnv(String method, BluetoothDevice device) {
        if (!CommonUtil.checkHasConnectPermission(mContext)) {
            JL_Log.w(TAG, method, "Missing permission to search Bluetooth.");
            return false;
        }
        if (null == device) {
            JL_Log.w(TAG, method, "Device is null.");
            return false;
        }
        /*if (!isBluetoothEnable()) {
            JL_Log.w(TAG, method, "Bluetooth is close.");
            return false;
        }*/
        return true;
    }

    private BleDevice getBleDevice(BluetoothDevice device) {
        if (null == device) return null;
        return mBleDeviceMap.get(device.getAddress());
    }

    private BleDevice addBleDevice(@NonNull BluetoothDevice device) {
        BleDevice cacheDevice = getBleDevice(device);
        if (null == cacheDevice) {
            cacheDevice = new BleDevice(mContext, device);
            mBleDeviceMap.put(device.getAddress(), cacheDevice);
        }
        return cacheDevice;
    }

    private BleDevice removeConnectedBle(BluetoothDevice device) {
        if (null == device) return null;
        return removeConnectedBle(device.getAddress());
    }

    private BleDevice removeConnectedBle(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) return null;
        BleDevice bleDevice = mBleDeviceMap.remove(address);
        JL_Log.i(TAG, "removeConnectedBle", "address : " + address + ", " + bleDevice);
        if (null == bleDevice) return null;
        bleDevice.stopSendDataThread();
        final List<BluetoothDevice> connectedDeviceList = getConnectedDeviceList();
        JL_Log.d(TAG, "removeConnectedBle", "connectedDeviceList size : " + connectedDeviceList.size());
        if (connectedDeviceList.isEmpty()) {
            setConnectedBtDevice(null);
        } else if (BluetoothUtil.deviceEquals(bleDevice.getDevice(), getConnectedBtDevice())) {
            setConnectedBtDevice(connectedDeviceList.get(0));
        }
        return bleDevice;
    }

    @NonNull
    private List<BleDevice> getSortList() {
        if (mBleDeviceMap.isEmpty()) return new ArrayList<>();
        List<BleDevice> bleDevices = new ArrayList<>(mBleDeviceMap.values());
        Collections.sort(bleDevices, (o1, o2) -> {
            if (null == o1 && null == o2) return 0;
            if (null == o1) return 1;
            if (null == o2) return -1;
            return Long.compare(o2.getConnectedTime(), o1.getConnectedTime());
        });
        return bleDevices;
    }

    private void clearConnectedBleDevices() {
        if (!mBleDeviceMap.isEmpty()) {
            Map<String, BleDevice> clone = new HashMap<>(mBleDeviceMap);
            for (String key : clone.keySet()) {
                BleDevice bleDevice = clone.get(key);
                if (null == bleDevice) continue;
                disconnectBleDevice(bleDevice.getDevice());
            }
            mBleDeviceMap.clear();
        }
    }

    @SuppressLint("MissingPermission")
    private void closeGatt(BluetoothGatt gatt) {
        if (null == gatt) return;
        JL_Log.i(TAG, "closeGatt", "gatt : " + gatt);
        //AppUtil.refreshBleDeviceCache(mContext, gatt); //强制更新缓存
        gatt.close();
    }

    private void setConnectedBtDevice(BluetoothDevice mConnectedBtDevice) {
        this.mUsingDevice = mConnectedBtDevice;
    }

    @SuppressLint("MissingPermission")
    private void filterDevice(BluetoothDevice device, int rssi, byte[] scanRecord,
                              boolean isBleEnableConnect) {
        if (CommonUtil.checkHasConnectPermission(mContext) && isBluetoothEnable() && !TextUtils.isEmpty(device.getName())
                && !mDiscoveredBleDevices.contains(device)) {
            JL_Log.d(TAG, "notify device : " + printDeviceInfo(device));
            mDiscoveredBleDevices.add(device);
            handleDiscoveryBle(device, new BleScanInfo().setRawData(scanRecord).setRssi(rssi).setEnableConnect(isBleEnableConnect));
        }
    }

    private void startConnectTimeout(BluetoothDevice device) {
        if (!mHandler.hasMessages(MSG_CONNECT_BLE_TIMEOUT)) {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CONNECT_BLE_TIMEOUT, device), CONNECT_BLE_TIMEOUT);
        }
    }

    private void stopConnectTimeout() {
        if (mHandler.hasMessages(MSG_CONNECT_BLE_TIMEOUT)) {
            mHandler.removeMessages(MSG_CONNECT_BLE_TIMEOUT);
        }
    }

    private void syncSystemBleDevice() {
        final List<BluetoothDevice> mSysConnectedBleList = getConnectedBleDeviceList(mContext);
        if (mSysConnectedBleList != null && !mSysConnectedBleList.isEmpty()) {
            for (BluetoothDevice bleDev : mSysConnectedBleList) {
                if (!BluetoothUtil.deviceEquals(bleDev, mUsingDevice)) {
                    if (!mDiscoveredBleDevices.contains(bleDev)) {
                        mDiscoveredBleDevices.add(bleDev);
                        handleDiscoveryBle(bleDev, new BleScanInfo().setEnableConnect(true));
                    }
                }
            }
        }
    }

    private void addSendTask(BluetoothDevice device, UUID serviceUUID, UUID characteristicUUID,
                             byte[] data, OnWriteDataCallback callback) {
        boolean ret = false;
        BleDevice bleDevice = getBleDevice(device);
        if (bleDevice != null) {
            ret = bleDevice.addSendTask(serviceUUID, characteristicUUID, data, callback);
        }
        if (!ret && callback != null) {
            callback.onBleResult(device, serviceUUID, characteristicUUID, false, data);
        }
    }

    private void wakeupSendThread(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID,
                                  int status, byte[] data) {
        final BleDevice bleDevice = getBleDevice(gatt.getDevice());
        if (bleDevice != null) {
            SendBleDataThread.BleSendTask task = new SendBleDataThread.BleSendTask(gatt, serviceUUID, characteristicUUID, data, null);
            task.setStatus(status);
            bleDevice.wakeupSendThread(task);
        }
    }

    private void handleDiscoveryBle(BluetoothDevice device, BleScanInfo bleScanInfo) {
        mCallbackManager.onDiscoveryBle(device, bleScanInfo);
    }

    private void handleBleConnection(BluetoothDevice device, int status) {
        final BleDevice bleDevice = getBleDevice(device);
        if (null == bleDevice) return;
        int prevState = bleDevice.getConnection();
        JL_Log.i(TAG, "handleBleConnection", CommonUtil.formatString("device : " + printDeviceInfo(device)
                + ", prevState : " + prevState + ", status : " + status));
        if (prevState == status) return; //相同连接状态
        bleDevice.setConnection(status);
        switch (status) {
            case BluetoothProfile.STATE_CONNECTED: {
                mHandler.removeMessages(MSG_NOTIFY_BLE_TIMEOUT);
                bleDevice.setConnectedTime(System.currentTimeMillis());
                bleDevice.startSendDataThread();
                if (getConnectedBtDevice() == null) {
                    setConnectedBtDevice(device);
                }
                break;
            }
            case BluetoothProfile.STATE_DISCONNECTED: {
                mHandler.removeMessages(MSG_NOTIFY_BLE_TIMEOUT);
                bleDevice.setConnectedTime(0);
                removeConnectedBle(device);
                break;
            }
            case BluetoothProfile.STATE_CONNECTING: {
                bleDevice.setConnectedTime(System.currentTimeMillis());
                break;
            }
        }
        mCallbackManager.onBleConnection(device, status);
    }

    /* ---- BroadcastReceiver Handler ---- */
    private void registerReceiver() {
        if (mAdapterReceiver == null) {
            mAdapterReceiver = new BaseBtAdapterReceiver();
            IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            mContext.registerReceiver(mAdapterReceiver, intentFilter);
        }
    }

    private void unregisterReceiver() {
        if (mAdapterReceiver != null) {
            mContext.unregisterReceiver(mAdapterReceiver);
            mAdapterReceiver = null;
        }
    }

    /**
     * 用于开启蓝牙BLE设备Notification服务
     *
     * @param gatt               被连接的ble Gatt服务对象
     * @param serviceUUID        服务UUID
     * @param characteristicUUID characteristic UUID
     * @return 结果 true 则等待系统回调BLE服务
     */
    @SuppressLint("MissingPermission")
    private boolean enableBLEDeviceNotification(BluetoothGatt gatt, UUID serviceUUID, UUID
            characteristicUUID) {
        if (null == gatt || !CommonUtil.checkHasConnectPermission(mContext)) {
            JL_Log.w(TAG, "enableBLEDeviceNotification", "Missing permission to connect Bluetooth.");
            return false;
        }
        if (!isBluetoothEnable()) {
            JL_Log.w(TAG, "enableBLEDeviceNotification", "Bluetooth is close.");
            return false;
        }
        BluetoothGattService gattService = gatt.getService(serviceUUID);
        if (null == gattService) {
            JL_Log.w(TAG, "enableBLEDeviceNotification", "BluetoothGattService is null. uuid = " + serviceUUID);
            return false;
        }
        BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(characteristicUUID);
        if (null == characteristic) {
            JL_Log.w(TAG, "enableBLEDeviceNotification", "BluetoothGattCharacteristic is null. uuid = " + characteristicUUID);
            return false;
        }
        boolean bRet = gatt.setCharacteristicNotification(characteristic, true);
        if (bRet) {
            bRet = false; //重置标识
            List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
            for (BluetoothGattDescriptor descriptor : descriptors) {
                if (!BLE_UUID_NOTIFICATION_DESCRIPTOR.equals(descriptor.getUuid()))
                    continue; //跳过不相关描述符
                bRet = tryToWriteDescriptor(gatt, descriptor, 0, false);
                if (!bRet) {
                    JL_Log.w(TAG, "enableBLEDeviceNotification", "(tryToWriteDescriptor) ---> failed");
                } else { //正常只有一个描述符，使能即可
                    break;
                }
            }
        } else {
            JL_Log.w(TAG, "enableBLEDeviceNotification", "(setCharacteristicNotification) ---> failed.");
        }
        JL_Log.w(TAG, "enableBLEDeviceNotification", bRet + ", serviceUUID : " + serviceUUID + ", characteristicUUID : " + characteristicUUID);
        return bRet;
    }

    @SuppressLint("MissingPermission")
    private boolean tryToWriteDescriptor(BluetoothGatt bluetoothGatt, BluetoothGattDescriptor
            descriptor, int retryCount, boolean isSkipSetValue) {
        if (!CommonUtil.checkHasConnectPermission(mContext)) return false;
        boolean ret = isSkipSetValue;
        if (!ret) {
            ret = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            JL_Log.i(TAG, "tryToWriteDescriptor", "setValue : " + ret);
            if (!ret) {
                retryCount++;
                if (retryCount >= 3) {
                    return false;
                } else {
                    JL_Log.i(TAG, "tryToWriteDescriptor", "setValue failed. retryCount : " + retryCount);
                    SystemClock.sleep(50);
                    tryToWriteDescriptor(bluetoothGatt, descriptor, retryCount, false);
                }
            } else {
                retryCount = 0;
            }
        }
        if (ret) {
            ret = bluetoothGatt.writeDescriptor(descriptor);
            JL_Log.i(TAG, "tryToWriteDescriptor", "writeDescriptor : " + ret);
            if (!ret) {
                retryCount++;
                if (retryCount >= 3) {
                    return false;
                } else {
                    JL_Log.i(TAG, "tryToWriteDescriptor", "writeDescriptor failed. retryCount : " + retryCount);
                    SystemClock.sleep(50);
                    tryToWriteDescriptor(bluetoothGatt, descriptor, retryCount, true);
                }
            }
        }
        return ret;
    }


    //开始调整BLE协议MTU
    @SuppressLint("MissingPermission")
    private void startChangeMtu(BluetoothGatt gatt, int mtu) {
        if (gatt == null || !CommonUtil.checkHasConnectPermission(mContext)) {
            JL_Log.w(TAG, "startChangeMtu", "-- param is error.");
            return;
        }
        if (mHandler.hasMessages(MSG_CHANGE_BLE_MTU_TIMEOUT)) {
            JL_Log.w(TAG, "startChangeMtu", "Adjusting the MTU for BLE");
            return;
        }
        final BluetoothDevice device = gatt.getDevice();
        if (device == null) {
            JL_Log.w(TAG, "startChangeMtu", "-startChangeMtu- device is null.");
            return;
        }
        boolean ret = false;
        if (mtu > BluetoothConstant.BLE_MTU_MIN) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ret = gatt.requestMtu(mtu + 3);
            }
        }
        JL_Log.d(TAG, "startChangeMtu", "requestMtu : " + ret + ", mtu : " + mtu);
        if (ret) { //调整成功，开始超时任务
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CHANGE_BLE_MTU_TIMEOUT, device), CALLBACK_TIMEOUT);
        } else {
            handleBleConnection(device, BluetoothProfile.STATE_CONNECTED);
        }
    }

    //回收调整MTU的超时任务
    private void stopChangeMtu() {
        mHandler.removeMessages(MSG_CHANGE_BLE_MTU_TIMEOUT);
    }

    private String printDeviceInfo(BluetoothDevice device) {
        return BluetoothUtil.printBtDeviceInfo(mContext, device);
    }

    private final BluetoothAdapter.LeScanCallback mLeScanCallback = (device, rssi, scanRecord) -> filterDevice(device, rssi, scanRecord, true);

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result != null && result.getScanRecord() != null) {
                BluetoothDevice device = result.getDevice();
                boolean isBleEnableConnect = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isBleEnableConnect = result.isConnectable();
                }
//                JL_Log.d("onScanResult", printDeviceInfo(device));
                filterDevice(device, result.getRssi(), result.getScanRecord().getBytes(), isBleEnableConnect);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {

        }

        @Override
        public void onScanFailed(int errorCode) {
            JL_Log.d(TAG, "onScanFailed", "code : " + errorCode);
            stopLeScan();
        }
    };

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

        public void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency, int timeout, int status) {
            if (null == gatt || !CommonUtil.checkHasConnectPermission(mContext)) return;
            BluetoothDevice device = gatt.getDevice();
            if (null == device) return;
            JL_Log.e(TAG, "onConnectionUpdated", "device : " + printDeviceInfo(device) + ", interval : "
                    + interval + ", latency : " + latency + ", timeout : " + timeout + ", status : " + status);
            mCallbackManager.onConnectionUpdated(device, interval, latency, timeout, status);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (null == gatt) return;
            final BluetoothDevice device = gatt.getDevice();
            if (null == device) return;
            final BleDevice bleDevice = getBleDevice(device);
            JL_Log.i(TAG, "onConnectionStateChange", CommonUtil.formatString("device : %s, status = %d, newState = %d.\n%s",
                    printDeviceInfo(device), status, newState, bleDevice));
            if (newState == BluetoothProfile.STATE_DISCONNECTED || newState == BluetoothProfile.STATE_DISCONNECTING
                    || newState == BluetoothProfile.STATE_CONNECTED) {
                stopConnectTimeout();
                if (newState == BluetoothProfile.STATE_CONNECTED) {  //BLE连接成功
                    bleDevice.setGatt(gatt);
                    boolean ret = gatt.discoverServices();
                    JL_Log.d(TAG, "onConnectionStateChange", "discoverServices : " + ret);
                    if (ret) {
                        mHandler.removeMessages(MSG_BLE_DISCOVER_SERVICES_CALLBACK_TIMEOUT);
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_BLE_DISCOVER_SERVICES_CALLBACK_TIMEOUT, device), CALLBACK_TIMEOUT);
                    } else {
                        disconnectBleDevice(device);
                    }
                    return;
                } else {
                    mHandler.removeMessages(MSG_DISCONNECT_BLE_TIMEOUT);
                    closeGatt(gatt);

                    long usedConnectTime = System.currentTimeMillis() - bleDevice.getConnectedTime(); //连接已耗费时间
                    boolean isNeedReconnect = bleDevice.isNeedReconnect() || (/*status == 133 && */usedConnectTime > 0 && usedConnectTime < MIN_CONNECT_TIME);
                    JL_Log.d(TAG, "onConnectionStateChange", "usedConnectTime = " + usedConnectTime + ", limit time = " + MIN_CONNECT_TIME
                            + ", isNeedReconnect : " + isNeedReconnect);
                    if (isNeedReconnect) { //Todo: 遇到了异常断开情况, 尝试重连设备
                        if (!bleDevice.isOverReconnectLimit()) {
                            JL_Log.i(TAG, "onConnectionStateChange", "Ready to reconnect device.");
                            bleDevice.setConnection(BluetoothProfile.STATE_DISCONNECTED)
                                    .setMtu(BluetoothConstant.BLE_MTU_MIN)
                                    .setConnectedTime(0L);
                            SystemClock.sleep(1000);
                            if (!connectBleDevice(device)) {
                                bleDevice.setConnection(BluetoothProfile.STATE_CONNECTING);
                                handleBleConnection(device, BluetoothProfile.STATE_DISCONNECTED);
                            }
                            return;
                        }
                    }
                }
            }
            handleBleConnection(device, newState);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (null == gatt) return;
            BluetoothDevice device = gatt.getDevice();
            if (null == device) return;
            final BleDevice bleDevice = getBleDevice(device);
            if (null == bleDevice) return;
            mHandler.removeMessages(MSG_BLE_DISCOVER_SERVICES_CALLBACK_TIMEOUT);
            mCallbackManager.onBleServiceDiscovery(device, status, gatt.getServices());
            boolean ret = false;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothUtil.printBleGattServices(device, gatt, status);
                for (BluetoothGattService service : gatt.getServices()) {
                    if (BLE_UUID_SERVICE.equals(service.getUuid()) && null != service.getCharacteristic(BLE_UUID_WRITE)
                            && null != service.getCharacteristic(BLE_UUID_NOTIFICATION)) {
                        JL_Log.i(TAG, "onServicesDiscovered", "start NotifyCharacteristicRunnable...");
                        mNotifyCharacteristicRunnable = new NotifyCharacteristicRunnable(gatt, BLE_UUID_SERVICE, BLE_UUID_NOTIFICATION);
                        mHandler.post(mNotifyCharacteristicRunnable);
                        ret = true;
                        break;
                    }
                }
            }
            JL_Log.i(TAG, "onServicesDiscovered", "" + ret);
            if (!ret) {
                disconnectBleDevice(device);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (null == gatt) return;
            BluetoothDevice device = gatt.getDevice();
            if (null == device || null == characteristic) return;
            UUID serviceUUID = null;
            UUID characteristicUUID = characteristic.getUuid();
            byte[] data = characteristic.getValue();
            BluetoothGattService gattService = characteristic.getService();
            if (gattService != null) {
                serviceUUID = gattService.getUuid();
            }
            JL_Log.d(TAG, CommonUtil.formatString("[onCharacteristicChanged] <<< deice : %s, serviceUuid = %s, characteristicUuid = %s, \ndata : [%s]",
                    printDeviceInfo(device), serviceUUID, characteristicUUID, CHexConver.byte2HexStr(data)));
            mCallbackManager.onBleDataNotification(device, serviceUUID, characteristicUUID, data);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (null == gatt || null == gatt.getDevice() || null == characteristic) return;
            BluetoothDevice device = gatt.getDevice();
            UUID serviceUUID = null;
            UUID characteristicUUID = characteristic.getUuid();
            BluetoothGattService gattService = characteristic.getService();
            if (gattService != null) serviceUUID = gattService.getUuid();
            byte[] data = characteristic.getValue();
            JL_Log.d(TAG, "onCharacteristicWrite", CommonUtil.formatString("device : %s, serviceUuid = %s, characteristicUuid = %s, status = %d, \ndata : [%s]",
                    printDeviceInfo(device), serviceUUID, characteristicUUID, status, CHexConver.byte2HexStr(data)));
            wakeupSendThread(gatt, serviceUUID, characteristicUUID, status, data);
            mCallbackManager.onBleWriteStatus(device, serviceUUID, characteristicUUID, data, status);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (null == gatt) return;
            BluetoothDevice device = gatt.getDevice();
            if (null == device || null == descriptor) return;
            UUID serviceUuid = null;
            UUID characteristicUuid = null;
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
            if (null != characteristic) {
                characteristicUuid = characteristic.getUuid();
                BluetoothGattService bluetoothGattService = characteristic.getService();
                if (null != bluetoothGattService) {
                    serviceUuid = bluetoothGattService.getUuid();
                }
            }
            JL_Log.i(TAG, "onDescriptorWrite", CommonUtil.formatString("device : %s, serviceUuid = %s, characteristicUuid = %s, descriptor = %s, status = %d",
                    printDeviceInfo(device), serviceUuid, characteristicUuid, descriptor.getUuid(), status));
            mCallbackManager.onBleNotificationStatus(device, serviceUuid, characteristicUuid, status);
            if (mNotifyCharacteristicRunnable != null && BluetoothUtil.deviceEquals(device, mNotifyCharacteristicRunnable.getBleDevice())
                    && serviceUuid != null && serviceUuid.equals(mNotifyCharacteristicRunnable.getServiceUUID())
                    && characteristicUuid != null && characteristicUuid.equals(mNotifyCharacteristicRunnable.getCharacteristicUUID())
                    && descriptor.getUuid() != null && descriptor.getUuid().equals(mNotifyCharacteristicRunnable.mDescriptorUUID)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    mNotifyCharacteristicRunnable = null;
                    // TODO: 2022/6/28 由于部分手机(小米11 Lite)的兼容性问题，MTU不能调整到最大，建议适当减小MTU
//                    startChangeMtu(gatt, 128); //调整MTU最大为128
                    int requestMTU = REQUEST_MTU;
                    if (requestMTU > 509) {
                        requestMTU = 509;
                    }
                    startChangeMtu(gatt, requestMTU);
//                    handleBleConnectedEvent(device);
                } else {
                    int num = mNotifyCharacteristicRunnable.getRetryNum();
                    if (num < 3) {
                        mNotifyCharacteristicRunnable.setRetryNum(++num);
                        mHandler.postDelayed(mNotifyCharacteristicRunnable, 100);
                    } else {
                        disconnectBleDevice(device);
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (null == gatt) return;
            BluetoothDevice device = gatt.getDevice();
            if (null == device) return;
            JL_Log.d(TAG, "onMtuChanged", CommonUtil.formatString("device : %s, mtu = %d, status = %d", printDeviceInfo(device), mtu, status));
            mCallbackManager.onBleDataBlockChanged(device, mtu, status);
            final BleDevice bleDevice = getBleDevice(device);
            if (null == bleDevice) return;
            if (mHandler.hasMessages(MSG_CHANGE_BLE_MTU_TIMEOUT)) { //调整MTU的回调
                // 需要减去3个字节的数据包头部信息
                int bleMtu = status == BluetoothGatt.GATT_SUCCESS ? mtu - 3 : BluetoothConstant.BLE_MTU_MIN;
                stopChangeMtu();
                bleDevice.setMtu(bleMtu);
                JL_Log.i(TAG, "onMtuChanged", "MTU modified successfully");
                handleBleConnection(device, BluetoothProfile.STATE_CONNECTED);
            }
        }
    };

    private class BaseBtAdapterReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
                    int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
                    if (mBluetoothAdapter != null && state == -1) {
                        state = mBluetoothAdapter.getState();
                    }
                    if (state == prevState) return;
                    if (state == BluetoothAdapter.STATE_OFF) {
                        isBleScanning(false);
                        mDiscoveredBleDevices.clear();
                        clearConnectedBleDevices();
                        mCallbackManager.onAdapterChange(false);
                    } else if (state == BluetoothAdapter.STATE_ON) {
                        mCallbackManager.onAdapterChange(true);
                    }
                    break;
                }
                case BluetoothDevice.ACTION_ACL_CONNECTED: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    JL_Log.i(TAG, "ACTION_ACL_CONNECTED", "device : "
                            + printDeviceInfo(device));
                    break;
                }
                case BluetoothDevice.ACTION_ACL_DISCONNECTED: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    JL_Log.i(TAG, "ACTION_ACL_DISCONNECTED", "device : "
                            + printDeviceInfo(device));
                    break;
                }

            }
        }
    }

    private class NotifyCharacteristicRunnable implements Runnable {
        private final BluetoothGatt mGatt;
        private final UUID mServiceUUID;
        private final UUID mCharacteristicUUID;
        public final UUID mDescriptorUUID = BLE_UUID_NOTIFICATION_DESCRIPTOR;
        private int retryNum = 0;

        private NotifyCharacteristicRunnable(BluetoothGatt gatt, UUID serviceUUID, UUID characteristicUUID) {
            this.mGatt = gatt;
            this.mServiceUUID = serviceUUID;
            this.mCharacteristicUUID = characteristicUUID;
        }

        private void setRetryNum(int retryNum) {
            this.retryNum = retryNum;
        }

        private int getRetryNum() {
            return retryNum;
        }

        private BluetoothDevice getBleDevice() {
            if (mGatt == null) return null;
            return mGatt.getDevice();
        }

        private UUID getServiceUUID() {
            return mServiceUUID;
        }

        private UUID getCharacteristicUUID() {
            return mCharacteristicUUID;
        }

        @Override
        public void run() {
            boolean ret = enableBLEDeviceNotification(mGatt, mServiceUUID, mCharacteristicUUID);
            JL_Log.w(TAG, "enableBLEDeviceNotification", CommonUtil.formatString("%s, service uuid = %s, characteristic uuid = %s",
                    ret, mServiceUUID, mCharacteristicUUID));
            if (!ret) {
                if (mGatt != null) {
                    disconnectBleDevice(mGatt.getDevice());
                }
            } else {
                mHandler.removeMessages(MSG_NOTIFY_BLE_TIMEOUT);
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_NOTIFY_BLE_TIMEOUT, mGatt.getDevice()), CALLBACK_TIMEOUT);
            }
        }
    }
}
