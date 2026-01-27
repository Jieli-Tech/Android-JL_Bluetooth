package com.jieli.btsmart.ui.search;

import android.bluetooth.BluetoothDevice;

import com.amap.api.location.AMapLocation;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.btsmart.tool.configure.ConfigureKit;
import com.jieli.btsmart.tool.location.LocationHelper;
import com.jieli.btsmart.tool.location.OnLocationInfoListener;
import com.jieli.btsmart.ui.base.bluetooth.BluetoothBasePresenter;
import com.jieli.component.utils.SystemUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jieli.btsmart.ui.search.SearchDevicePresenter.DEFAULT_RADIUS;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/8/18 14:21
 * @desc :
 */
@Deprecated
public class SearchDeviceListPresenter extends BluetoothBasePresenter implements ISearchDeviceListContract.ISearchDeviceListPresenter {
    private final ISearchDeviceListContract.ISearchDeviceListView mView;
    private final Map<LatLonPoint, Integer> mRetryTimesMap = new HashMap<>();

    public SearchDeviceListPresenter(ISearchDeviceListContract.ISearchDeviceListView view) {
        super(view);
        mView = SystemUtil.checkNotNull(view);
        LocationHelper.getInstance().registerOnLocationInfoListener(mOnLocationInfoListener);
        mRCSPController.addBTRcspEventCallback(mEventCallback);
    }

    @Override
    public void start() {
    }

    @Override
    public List<LocationDeviceInfo> getHistoryBtDeviceList() {
        List<HistoryBluetoothDevice> list = mRCSPController.getHistoryBluetoothDeviceList();
        ArrayList<LocationDeviceInfo> resultList = new ArrayList<>();
        if (list != null) {
            for (HistoryBluetoothDevice device : list) {
                LocationDeviceInfo deviceInfo = new LocationDeviceInfo(device);
                deviceInfo.location = new LatLonPoint(device.getLeftDevLatitude(), device.getLeftDevLongitude());
                deviceInfo.locationString = "(" + device.getLeftDevLatitude() + "," + device.getLeftDevLongitude() + ")";
                if (ConfigureKit.getInstance().isAllowSearchDevice(device)) {
                    resultList.add(deviceInfo);
                }
            }
        }
        return resultList;
    }

    @Override
    public HistoryBluetoothDevice getConnectedHistoryBtRecord() {
        if (null == getConnectedDevice()) return null;
        return mRCSPController.findHistoryBluetoothDevice(getConnectedDevice().getAddress());
    }

    @Override
    public HistoryBluetoothDevice getHistoryBtRecordByAddress(String address) {
        return mRCSPController.findHistoryBluetoothDevice(address);
    }

    /*@Override
    public boolean isConnectedDevice(String addr) {
        boolean isConnected = false;
        if (addr != null && getConnectedDevice() != null) {
            isConnected = DeviceAddrManager.getInstance().isMatchDevice(getConnectedDevice().getAddress(), addr);
        }
        return isConnected;
    }*/

    @Override
    public void getHistoryDeviceListLocation(List<LocationDeviceInfo> deviceList) {
        for (LocationDeviceInfo device : deviceList) {
            LocationHelper.getInstance().getFromLocation(device.getHistoryBluetoothDevice().getLeftDevLatitude(), device.getHistoryBluetoothDevice().getLeftDevLongitude(), DEFAULT_RADIUS);
        }
    }

    @Override
    public void destroy() {
        LocationHelper.getInstance().unregisterOnLocationInfoListener(mOnLocationInfoListener);
        destroyRCSPController(mEventCallback);
    }

    private final OnLocationInfoListener mOnLocationInfoListener = new OnLocationInfoListener() {

        @Override
        public void onLocationInfoChange(AMapLocation location, BluetoothDevice device) {
            mView.onLocationChange(location);
        }

        /**
         * 逆地址编码
         * */
        @Override
        public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int rCode) {
            if (rCode == AMapException.CODE_AMAP_SUCCESS) { //成功
                mView.onRegeocodeSearched(regeocodeResult);
                LatLonPoint latLon = regeocodeResult.getRegeocodeQuery() != null ? regeocodeResult.getRegeocodeQuery().getPoint() : null;
                Integer retryTime = mRetryTimesMap.get(latLon);
                if (retryTime != null) {
                    mRetryTimesMap.put(latLon, 0);
                }
            } else {
                LatLonPoint latLon = regeocodeResult.getRegeocodeQuery() != null ? regeocodeResult.getRegeocodeQuery().getPoint() : null;
                if (latLon != null) {
                    Integer retryTime = mRetryTimesMap.get(latLon);
                    if (retryTime == null || retryTime < 3) {
                        retryTime = retryTime == null ? 1 : retryTime++;
                        mRetryTimesMap.put(latLon, retryTime);
                        LocationHelper.getInstance().getFromLocation(latLon.getLatitude(), latLon.getLongitude(), DEFAULT_RADIUS);
                    }
                }
            }
        }

        @Override
        public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {

        }
    };

    private final BTRcspEventCallback mEventCallback = new BTRcspEventCallback() {

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            mView.onDeviceConnection(device, status);
        }

        @Override
        public void onBondStatus(BluetoothDevice device, int status) {
            if (status != BluetoothDevice.BOND_NONE) {
                HistoryBluetoothDevice history = mRCSPController.findHistoryBluetoothDevice(device.getAddress());
                if (null != history) {
                    mView.onRemoveHistoryDeviceSuccess(history);
                }
            }
        }

    };
}
