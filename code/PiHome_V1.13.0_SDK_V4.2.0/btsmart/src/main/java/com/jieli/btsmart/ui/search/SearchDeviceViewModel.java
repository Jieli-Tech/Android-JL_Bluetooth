package com.jieli.btsmart.ui.search;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.MutableLiveData;

import com.amap.api.location.AMapLocation;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.parameter.SearchDevParam;
import com.jieli.bluetooth.bean.response.SearchDevStatusResponse;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.tool.DeviceStatusManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.data.model.basic.CombineData;
import com.jieli.btsmart.tool.bluetooth.RingHandler;
import com.jieli.btsmart.tool.configure.ConfigureKit;
import com.jieli.btsmart.tool.location.LocationHelper;
import com.jieli.btsmart.tool.location.OnLocationInfoListener;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 查找设备逻辑实现
 * @since 2023/8/31
 */
public class SearchDeviceViewModel extends BtBasicVM {

    public final MutableLiveData<HistoryBluetoothDevice> historyRemoveMLD = new MutableLiveData<>();
    public final MutableLiveData<CombineData<BluetoothDevice, AMapLocation>> locationInfoMLD = new MutableLiveData<>();
    public final MutableLiveData<RegeocodeResult> regeocodeResultMLD = new MutableLiveData<>();
    public final MutableLiveData<CombineData<BluetoothDevice, Boolean>> twsStatusMLD = new MutableLiveData<>();
    public final MutableLiveData<CombineData<String, Boolean>> playSoundStatusMLD = new MutableLiveData<>();
    public final MutableLiveData<CombineData<BluetoothDevice, SearchDevStatusResponse>> searchDeviceStatusMLD = new MutableLiveData<>();

    private final LocationHelper mLocationHelper = LocationHelper.getInstance();
    private final RingHandler mRingHandler = RingHandler.getInstance();

    private String mTargetDevAddress;
    private int playWay = Constants.RING_WAY_ALL;

    private final Map<LatLonPoint, Integer> mRetryTimesMap = new HashMap<>();

    public final static int DEFAULT_RADIUS = 16;
    private final static int PLAY_RING_TIMEOUT = 60;

    private final static int RETRY_MAX = 3;

    public SearchDeviceViewModel() {
        mRCSPController.addBTRcspEventCallback(mBTRcspEventCallback);
        mRingHandler.registerOnRingStatusListener(mOnRingStatusListener);
        mLocationHelper.registerOnLocationInfoListener(mOnLocationInfoListener);
    }

    @Override
    protected void release() {
        if (mRingHandler.isPlayAlarmRing()) {
            String address = mTargetDevAddress;
            if (!isUsingDevice(address)) {
                address = getConnectedDevice() != null ? getConnectedDevice().getAddress() : mTargetDevAddress;
            }
            mRingHandler.stopAlarmRing();
            stopSound(address);
        }
        mRingHandler.unregisterOnRingStatusListener(mOnRingStatusListener);
        mLocationHelper.unregisterOnLocationInfoListener(mOnLocationInfoListener);
        mRCSPController.removeBTRcspEventCallback(mBTRcspEventCallback);
        super.release();
    }

    public void setTargetDevAddress(String address) {
        mTargetDevAddress = address;
        if (isConnectedDevice(address)) {
            playSoundStatusMLD.postValue(new CombineData<>(address, mRingHandler.isPlayAlarmRing()));
        }
    }

    public String getTargetDevAddress() {
        return mTargetDevAddress;
    }

    public boolean isTargetDevice(String address) {
        return BluetoothAdapter.checkBluetoothAddress(mTargetDevAddress) && mTargetDevAddress.equals(address);
    }

    public boolean isTargetDevice(BluetoothDevice device) {
        return device != null && isTargetDevice(device.getAddress());
    }

    public boolean isTargetDevice(HistoryBluetoothDevice history) {
        if (null == history) return false;
        boolean ret = isTargetDevice(history.getAddress());
        if (!ret) {
            ret = isTargetDevice(mRCSPController.getMappedDeviceAddress(history.getAddress()));
        }
        return ret;
    }

    public boolean isPlayingSound(String address) {
        if (!isTargetDevice(address)) return false;
        CombineData<String, Boolean> data = playSoundStatusMLD.getValue();
        return data != null && data.getR();
    }

    public boolean isTwsConnected(String address) {
        if (isConnectedDevice(address)) {
            return DeviceStatusManager.getInstance().isTwsConnected(BluetoothUtil.getRemoteDevice(address));
        }
        return false;
    }

    public int getPlayWay() {
        return playWay;
    }

    public boolean isLocation() {
        return mLocationHelper.isStartLocation();
    }

    public void startLocation() {
        mLocationHelper.startLocation();
    }

    public void stopLocation() {
        if (isLocation()) mLocationHelper.stopLocation();
    }

    public void getFromLocation(double latitude, double longitude) {
        mLocationHelper.getFromLocation(latitude, longitude, DEFAULT_RADIUS);
    }

    public List<LocationDeviceInfo> getHistoryBtDeviceList() {
        List<HistoryBluetoothDevice> list = mRCSPController.getHistoryBluetoothDeviceList();
        ArrayList<LocationDeviceInfo> resultList = new ArrayList<>();
        if (list != null) {
            for (HistoryBluetoothDevice device : list) {
                LocationDeviceInfo deviceInfo = new LocationDeviceInfo(device);
                deviceInfo.location = new LatLonPoint(device.getLeftDevLatitude(), device.getLeftDevLongitude());
                if (ConfigureKit.getInstance().isAllowSearchDevice(device)) {
                    resultList.add(deviceInfo);
                }
            }
        }
        return resultList;
    }

    public HistoryBluetoothDevice getHistoryBtRecordByAddress() {
        return mRCSPController.findHistoryBluetoothDevice(mTargetDevAddress);
    }

    public void syncSearchDeviceStatus() {
        if (!isUsingDevice(mTargetDevAddress)) return;
        DeviceInfo deviceInfo = getDeviceInfo(mTargetDevAddress);
        boolean isSupportDoubleConnection = deviceInfo != null && deviceInfo.isSupportDoubleConnection();
        if (!isSupportDoubleConnection) return;
        mRCSPController.syncSearchDeviceStatus(BluetoothUtil.getRemoteDevice(mTargetDevAddress), new OnRcspActionCallback<SearchDevStatusResponse>() {
            @Override
            public void onSuccess(BluetoothDevice device, SearchDevStatusResponse message) {
                JL_Log.i(tag, "[syncSearchDeviceStatus][onSuccess] >>> " + message);
                playWay = message.getWay();
                playSoundStatusMLD.setValue(new CombineData<>(device.getAddress(), message.getOp() == Constants.RING_OP_OPEN));
                searchDeviceStatusMLD.postValue(new CombineData<>(device, message));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.w(tag, "[syncSearchDeviceStatus][onError] >>> error = " + error);
            }
        });
    }

    public void playSound(int way) {
        if (!isUsingDevice(mTargetDevAddress)) return;
        JL_Log.d(tag, "[playSound] >>> way = " + way);
        playWay = way;
        mRCSPController.searchDev(BluetoothUtil.getRemoteDevice(mTargetDevAddress), Constants.RING_OP_OPEN, PLAY_RING_TIMEOUT, way,
                Constants.RING_PLAYER_APP, new OnRcspActionCallback<Boolean>() {
                    @Override
                    public void onSuccess(BluetoothDevice device, Boolean message) {
                        JL_Log.d(tag, "[playSound][onSuccess] >>> way = " + way);
                        playSoundStatusMLD.setValue(new CombineData<>(mTargetDevAddress, true));
                        mRingHandler.playAlarmRing(Constants.SEARCH_TYPE_DEVICE, PLAY_RING_TIMEOUT * 1000);
                    }

                    @Override
                    public void onError(BluetoothDevice device, BaseError error) {
                        JL_Log.w(tag, "[playSound][onError] >>> error = " + error);
                    }
                });
    }

    public void stopSound() {
        stopSound(mTargetDevAddress);
    }

    public void stopSound(String address) {
        if (!isUsingDevice(address)) return;
        mRingHandler.stopAlarmRing();
        mRCSPController.stopSearchDevice(BluetoothUtil.getRemoteDevice(address), null);
    }

    private final OnLocationInfoListener mOnLocationInfoListener = new OnLocationInfoListener() {
        @Override
        public void onLocationInfoChange(AMapLocation location, BluetoothDevice device) {
            locationInfoMLD.setValue(new CombineData<>(device, location));
        }

        @Override
        public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int rCode) {
            LatLonPoint latLon = regeocodeResult.getRegeocodeQuery() != null ? regeocodeResult.getRegeocodeQuery().getPoint() : null;
            if (rCode == AMapException.CODE_AMAP_SUCCESS) {
                if (null != latLon && mRetryTimesMap.get(latLon) != null) {
                    mRetryTimesMap.put(latLon, 0);
                }
                regeocodeResultMLD.setValue(regeocodeResult);
            } else {
                if(null != latLon){
                    Integer retryTime = mRetryTimesMap.get(latLon);
                    if (retryTime == null || retryTime < RETRY_MAX) {
                        retryTime = retryTime == null ? 1 : retryTime + 1;
                        mRetryTimesMap.put(latLon, retryTime);
                        getFromLocation(latLon.getLatitude(), latLon.getLongitude());
                        return;
                    }
                }
                regeocodeResultMLD.setValue(regeocodeResult);
            }
        }

        @Override
        public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {

        }
    };

    private final RingHandler.OnRingStatusListener mOnRingStatusListener = new RingHandler.OnRingStatusListener() {
        @Override
        public void onRingStatusChange(boolean isPlay) {
            JL_Log.d(tag, "[onRingStatusChange] >>> isPlay = " + isPlay);
            playSoundStatusMLD.setValue(new CombineData<>(mTargetDevAddress, isPlay));
        }
    };

    private final BTRcspEventCallback mBTRcspEventCallback = new BTRcspEventCallback() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (!isTargetDevice(device)) return;
            if (status == StateCode.CONNECTION_FAILED || status == StateCode.CONNECTION_DISCONNECT) {
                stopSound();
            }
        }

        @Override
        public void onBondStatus(BluetoothDevice device, int status) {
            if (device != null && status == BluetoothDevice.BOND_NONE) { //更新历史记录
                HistoryBluetoothDevice history = mRCSPController.findHistoryBluetoothDevice(device.getAddress());
                if (history != null) historyRemoveMLD.postValue(history);
            }
        }

        @Override
        public void onTwsStatusChange(BluetoothDevice device, boolean isTwsConnected) {
            if (!isTargetDevice(device)) return;
            twsStatusMLD.setValue(new CombineData<>(device, isTwsConnected));
        }

        @Override
        public void onSearchDevice(BluetoothDevice device, SearchDevParam searchDevParam) {
            JL_Log.d(tag, "[onSearchDevice] >>> " + searchDevParam);
            if (!isTargetDevice(device) || searchDevParam == null) return;
            //查找设备的处理
            if (searchDevParam.getType() == Constants.SEARCH_TYPE_SYNC_STATUS) {
                syncSearchDeviceStatus();
            } else {
                playSoundStatusMLD.setValue(new CombineData<>(mTargetDevAddress, searchDevParam.getOp() == Constants.RING_OP_OPEN));
            }
        }
    };
}
