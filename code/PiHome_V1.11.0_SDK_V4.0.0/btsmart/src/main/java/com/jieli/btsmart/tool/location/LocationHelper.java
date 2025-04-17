package com.jieli.btsmart.tool.location;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.DeviceFlag;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.tool.DeviceStatusManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.tool.bluetooth.rcsp.DeviceOpHandler;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.SystemUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 定位辅助类
 *
 * <p>
 * 使用高德定位SDK
 * </p>
 *
 * @author zqjasonZhong
 * @since 2020/7/8
 */
public class LocationHelper {
    private final static String TAG = LocationHelper.class.getSimpleName();
    private volatile static LocationHelper instance;
    private final AMapLocationClient mLocationClient;
    private AMapLocation mAMapLocation;
    private final RCSPController mRCSPController;
    private final GeocodeSearch mGeocodeSearch;
    private final DeviceAddrManager mDeviceAddrManager;
    private BluetoothDevice needUpdateGpsDev;
    private boolean isStopLocation;

    private final Set<OnLocationInfoListener> mListeners = new HashSet<>();
    private final static int UPDATE_DEVICE_LOCATION_INFO_INTERVAL = 30 * 1000;
    private final static int MSG_UPDATE_DEVICE_LOCATION = 6984;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (MSG_UPDATE_DEVICE_LOCATION == msg.what) {
                if (!mRCSPController.isDeviceConnected() && !isStopLocation) return false;
                final List<BluetoothDevice> deviceList = getDeviceListWithSearchFunc();
                if (null != mAMapLocation && (mAMapLocation.getLatitude() != 0 && mAMapLocation.getLongitude() != 0)) { //更新位置信息

                    for (BluetoothDevice device : deviceList) {
                        long currentTime = System.currentTimeMillis();
                        ADVInfoResponse advInfo = DeviceStatusManager.getInstance().getAdvInfo(device);
                        boolean isChangeGps;
                        if (advInfo != null) {
                            if (advInfo.getLeftDeviceQuantity() > 0 && advInfo.getRightDeviceQuantity() > 0) { //TWS设备连接
                                isChangeGps = mDeviceAddrManager.updateHistoryBtDeviceInfo(device, DeviceFlag.DEVICE_FLAG_MAIN,
                                        mAMapLocation.getLatitude(), mAMapLocation.getLongitude(), currentTime);
                                mDeviceAddrManager.updateHistoryBtDeviceInfo(device, DeviceFlag.DEVICE_FLAG_SUB,
                                        mAMapLocation.getLatitude(), mAMapLocation.getLongitude(), currentTime);
                                callbackUpdateGps(device, isChangeGps);
                                continue;
                            } else if (advInfo.getLeftDeviceQuantity() == 0 && advInfo.getRightDeviceQuantity() > 0) { //仅右设备连接
                                isChangeGps = mDeviceAddrManager.updateHistoryBtDeviceInfo(device, DeviceFlag.DEVICE_FLAG_SUB,
                                        mAMapLocation.getLatitude(), mAMapLocation.getLongitude(), currentTime);
                                callbackUpdateGps(device, isChangeGps);
                                continue;
                            }
                        }
                        isChangeGps = mDeviceAddrManager.updateHistoryBtDeviceInfo(device, DeviceFlag.DEVICE_FLAG_MAIN,
                                mAMapLocation.getLatitude(), mAMapLocation.getLongitude(), currentTime);
                        callbackUpdateGps(device, isChangeGps);
                    }
                } else if (needUpdateGpsDev != null) {
                    setNeedUpdateGpsDev(null);
                }
                if (!deviceList.isEmpty()) { //支持查找设备功能的设备列表不为空，继续更新设备位置信息
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEVICE_LOCATION, UPDATE_DEVICE_LOCATION_INFO_INTERVAL);
                }
            }
            return false;
        }
    });

    private LocationHelper(Context context) throws Exception {
        SystemUtil.checkNotNull(context);
        mLocationClient = new AMapLocationClient(context);
        if (!RCSPController.isInit())
            throw new RuntimeException("RCSPController has not been initialized yet.");
        mRCSPController = RCSPController.getInstance();
        AMapLocationClientOption locationClientOption = new AMapLocationClientOption();
        //设置为高精度定位模式
        locationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //是指定位间隔
        locationClientOption.setInterval(3000);
        //设置定位监听
        mLocationClient.setLocationListener(mMapLocationListener);
        //设置定位参数
        mLocationClient.setLocationOption(locationClientOption);
        //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
        JL_Log.i(TAG, "init", "clazz : " + this);
        stopLocation();
        mGeocodeSearch = new GeocodeSearch(AppUtil.getContext());
        /**
         * 地理编码查询回调
         */
        GeocodeSearch.OnGeocodeSearchListener onGeocodeSearchListener = new GeocodeSearch.OnGeocodeSearchListener() {
            /**
             * 逆地理编码回调
             */
            @Override
            public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int rCode) {
                notifyRegeocodeSearched(regeocodeResult, rCode);
            }

            /**
             * 地理编码查询回调
             */
            @Override
            public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {
                notifyGeocodeSearched(geocodeResult, rCode);
            }
        };
        mGeocodeSearch.setOnGeocodeSearchListener(onGeocodeSearchListener);

        mRCSPController.addBTRcspEventCallback(mBTRcspEventCallback);

        mDeviceAddrManager = DeviceAddrManager.getInstance();
        checkDeviceState();
    }

    public static boolean isInit() {
        return instance != null;
    }

    public static LocationHelper getInstance() {
        if (null == instance) {
            synchronized (LocationHelper.class) {
                if (null == instance) {
                    try {
                        instance = new LocationHelper(AppUtil.getContext());
                    } catch (Exception e) {
                        e.printStackTrace();
                        JL_Log.e(TAG, "LocationHelper : " + e.getMessage());
                        Thread.dumpStack();
                    }
                }
            }
        }
        return instance;
    }

    public void registerOnLocationInfoListener(OnLocationInfoListener listener) {
        if (null == listener) return;
        mListeners.add(listener);
        if (mAMapLocation != null) {
            listener.onLocationInfoChange(mAMapLocation, null);
        }
    }

    public void unregisterOnLocationInfoListener(OnLocationInfoListener listener) {
        if (null == listener) return;
        mListeners.remove(listener);
    }

    public void setLocationClientOption(AMapLocationClientOption option) {
        if (null == option) return;
        mLocationClient.setLocationOption(option);
    }

    public AMapLocationClient getLocationClient() {
        return mLocationClient;
    }

    public AMapLocation getLocationInfo() {
        return mAMapLocation;
    }

    public boolean isStartLocation() {
        return mLocationClient.isStarted();
    }

    public void startLocation() {
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        final boolean isStartLocation = isStartLocation();
        JL_Log.d(TAG, "startLocation", "isStartLocation : " + isStartLocation);
        if (isStartLocation) {
            stopLocation();
        }
        isStopLocation = false;
        mLocationClient.startLocation();
    }

    public void stopLocation() {
        mHandler.removeMessages(MSG_UPDATE_DEVICE_LOCATION);
        isStopLocation = true;
        mLocationClient.stopLocation();
        JL_Log.d(TAG, "stopLocation", "");
    }

    public void getFromLocation(double latitude, double longitude, int radius) {
        mGeocodeSearch.getFromLocationAsyn(new RegeocodeQuery(new LatLonPoint(latitude, longitude), radius, GeocodeSearch.AMAP));
    }

    public void destroy() {
        JL_Log.i(TAG, "destroy", "clazz : " + this);
        setNeedUpdateGpsDev(null);
        mLocationClient.unRegisterLocationListener(mMapLocationListener);
        stopLocation();
        mLocationClient.onDestroy();
        mListeners.clear();
        mHandler.removeCallbacksAndMessages(null);
        mGeocodeSearch.setOnGeocodeSearchListener(null);
        mRCSPController.removeBTRcspEventCallback(mBTRcspEventCallback);
        mAMapLocation = null;
        instance = null;
    }

    //获取具有查找设备功能的设备列表
    private List<BluetoothDevice> getDeviceListWithSearchFunc() {
        List<BluetoothDevice> devices = new ArrayList<>();
        if (!mRCSPController.isDeviceConnected()) return devices;
        List<BluetoothDevice> deviceList = mRCSPController.getConnectedDeviceList();
        for (BluetoothDevice device : deviceList) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(device);
            if (null == deviceInfo) continue;
            if (deviceInfo.isSupportSearchDevice()) {
                devices.add(device);
            }
        }
        return devices;
    }

    private void notifyLocationInfoChange(final AMapLocation location, final BluetoothDevice device) {
        if (location != null && !mListeners.isEmpty()) {
            mHandler.post(() -> {
                for (OnLocationInfoListener listener : new HashSet<>(mListeners)) {
                    listener.onLocationInfoChange(location, device);
                }
            });
        }
    }

    private void notifyRegeocodeSearched(final RegeocodeResult regeocodeResult, final int rCode) {
        if (regeocodeResult != null && !mListeners.isEmpty()) {
            mHandler.post(() -> {
                for (OnLocationInfoListener listener : new HashSet<>(mListeners)) {
                    listener.onRegeocodeSearched(regeocodeResult, rCode);
                }
            });
        }
    }

    private void notifyGeocodeSearched(final GeocodeResult geocodeResult, final int rCode) {
        if (geocodeResult != null && !mListeners.isEmpty()) {
            mHandler.post(() -> {
                for (OnLocationInfoListener listener : new HashSet<>(mListeners)) {
                    listener.onGeocodeSearched(geocodeResult, rCode);
                }
            });
        }
    }

    private void setNeedUpdateGpsDev(BluetoothDevice dev) {
        needUpdateGpsDev = dev;
    }

    private void callbackUpdateGps(BluetoothDevice device, boolean isChangeGps) {
        if (isChangeGps) {
            if (BluetoothUtil.deviceEquals(device, needUpdateGpsDev)) {
                setNeedUpdateGpsDev(null);
            }
            notifyLocationInfoChange(mAMapLocation, device);
        }
    }

    private void checkDeviceState() {
        final List<BluetoothDevice> deviceList = getDeviceListWithSearchFunc();
        JL_Log.d(TAG, "checkDeviceState", "deviceList : " + deviceList.size());
        if (!deviceList.isEmpty()) {
            startLocation();
        } else {
            stopLocation();
        }
    }

    private final AMapLocationListener mMapLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            if (null == aMapLocation) return;
            if (aMapLocation.getErrorCode() == 0) {
                JL_Log.d(TAG, "onLocationChanged", aMapLocation.toStr());
                mAMapLocation = aMapLocation;
                notifyLocationInfoChange(aMapLocation, null);

                if (mRCSPController.isDeviceConnected() && !isStopLocation &&
                        (!mHandler.hasMessages(MSG_UPDATE_DEVICE_LOCATION) || needUpdateGpsDev != null)) {
                    mHandler.sendEmptyMessage(MSG_UPDATE_DEVICE_LOCATION);
                }
            } else {
                JL_Log.i(TAG, "onLocationChanged", "location Error, ErrCode: " + aMapLocation.getErrorCode()
                        + ", errInfo : " + aMapLocation.getErrorInfo());
            }
        }
    };

    private final BTRcspEventCallback mBTRcspEventCallback = new BTRcspEventCallback() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (DeviceOpHandler.getInstance().isReconnecting()) return;
            if (device != null && status != StateCode.CONNECTION_CONNECTING) {
                setNeedUpdateGpsDev(device);
                checkDeviceState();
            }
        }

        @Override
        public void onTwsStatusChange(BluetoothDevice device, boolean isTwsConnected) {
            setNeedUpdateGpsDev(device);
            mHandler.removeMessages(MSG_UPDATE_DEVICE_LOCATION);
        }
    };
}
