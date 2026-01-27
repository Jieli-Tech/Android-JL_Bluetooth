package com.jieli.btsmart.ui.search;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.amap.api.location.AMapLocation;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.parameter.SearchDevParam;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.tool.DeviceStatusManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.tool.bluetooth.RingHandler;
import com.jieli.btsmart.tool.location.LocationHelper;
import com.jieli.btsmart.tool.location.OnLocationInfoListener;
import com.jieli.btsmart.ui.base.bluetooth.BluetoothBasePresenter;
import com.jieli.component.utils.SystemUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 查找设备逻辑实现类
 *
 * @author zqjasonZhong
 * @since 2020/7/8
 */
@Deprecated
public class SearchDevicePresenter extends BluetoothBasePresenter implements ISearchDeviceContract.ISearchDevicePresenter {
    private final ISearchDeviceContract.ISearchDeviceView mView;
    private LocationHelper mLocationHelper;
    private GeocodeSearch mGeocodeSearch;
    private final RingHandler mRingHandler = RingHandler.getInstance();

    private String mTargetDevAddress;
    private boolean isPlayingSound;

    public final static int DEFAULT_RADIUS = 16;
    private final static int PLAY_RING_TIMEOUT = 60;

    public SearchDevicePresenter(ISearchDeviceContract.ISearchDeviceView view) {
        super(view);
        mView = SystemUtil.checkNotNull(view);
        getRCSPController().addBTRcspEventCallback(mEventCallback);
        mRingHandler.registerOnRingStatusListener(mOnRingStatusListener);

        isPlayingSound = mRingHandler.isPlayAlarmRing();
    }

    @Override
    public void start() {
        super.start();
        getLocationHelper().registerOnLocationInfoListener(mOnLocationInfoListener);
    }

    @Override
    public List<HistoryBluetoothDevice> getHistoryBtDeviceList() {
        return getRCSPController().getHistoryBluetoothDeviceList();
    }

    @Override
    public HistoryBluetoothDevice getConnectedHistoryBtRecord() {
        return DeviceAddrManager.getInstance().findHistoryBluetoothDevice(getConnectedDevice());
    }

    @Override
    public HistoryBluetoothDevice getHistoryBtRecordByAddress(String address) {
        return DeviceAddrManager.getInstance().findHistoryBluetoothDevice(address);
    }

    @Override
    public void setTargetDevAddress(String address) {
        mTargetDevAddress = address;
        if (isConnectedDevice(address)) {
            isPlayingSound = mRingHandler.isPlayAlarmRing();
            mView.onPlaySoundStatus(BluetoothUtil.getRemoteDevice(address), isPlayingSound);
        }
    }

    @Override
    public boolean isPlayingSound(String address) {
        if (!isTargetDevice(address)) return false;
        return isPlayingSound;
    }

    @Override
    public boolean isTwsConnected(String address) {
        if (isConnectedDevice(address)) {
            return DeviceStatusManager.getInstance().isTwsConnected(BluetoothUtil.getRemoteDevice(address));
        }
        return false;
    }

    @Override
    public boolean isLocation() {
        return getLocationHelper().isStartLocation();
    }

    @Override
    public void startLocation() {
        getLocationHelper().startLocation();
    }

    @Override
    public void stopLocation() {
        getLocationHelper().stopLocation();
    }

    @Override
    public void getFromLocation(double latitude, double longitude) {
        getLocationHelper().getFromLocation(latitude, longitude, DEFAULT_RADIUS);
    }

    @Override
    public void playSound(int way) {
        JL_Log.d("zzc_playSound", "playSound :: way = " + way);
        mRCSPController.searchDev(BluetoothUtil.getRemoteDevice(mTargetDevAddress), Constants.RING_OP_OPEN, PLAY_RING_TIMEOUT, way,
                Constants.RING_PLAYER_APP, new OnRcspActionCallback<Boolean>() {
                    @Override
                    public void onSuccess(BluetoothDevice device, Boolean message) {
                        mRingHandler.playAlarmRing(Constants.SEARCH_TYPE_DEVICE, PLAY_RING_TIMEOUT * 1000);
                        isPlayingSound = true;
                        mView.onPlaySoundStatus(device, true);
                        mView.onPlaySoundSuccess(device);
                    }

                    @Override
                    public void onError(BluetoothDevice device, BaseError error) {
                        isPlayingSound = false;
                        mView.onPlaySoundStatus(device, false);
                        mView.onPlaySoundFailed(device, error);
                    }
                });
    }

    @Override
    public void stopSound() {
        mRingHandler.stopAlarmRing();
        isPlayingSound = false;
        mView.onPlaySoundStatus(BluetoothUtil.getRemoteDevice(mTargetDevAddress), false);
        if (isConnectedDevice(mTargetDevAddress)) {
            mRCSPController.stopSearchDevice(BluetoothUtil.getRemoteDevice(mTargetDevAddress), null);
        }
    }

    @Override
    public void destroy() {
        if (mRingHandler.isPlayAlarmRing()) {
            stopSound();
        }
        mRingHandler.unregisterOnRingStatusListener(mOnRingStatusListener);
        if (mLocationHelper != null) {
            mLocationHelper.unregisterOnLocationInfoListener(mOnLocationInfoListener);
            mLocationHelper = null;
        }
        if (mGeocodeSearch != null) {
            mGeocodeSearch.setOnGeocodeSearchListener(null);
            mGeocodeSearch = null;
        }
        destroyRCSPController(mEventCallback);
    }

    private LocationHelper getLocationHelper() {
        if (null == mLocationHelper) {
            mLocationHelper = LocationHelper.getInstance();
        }
        return mLocationHelper;
    }

    private boolean isTargetDevice(String address) {
        return BluetoothAdapter.checkBluetoothAddress(mTargetDevAddress) && mTargetDevAddress.equals(address);
    }

    private boolean isTargetDevice(BluetoothDevice device) {
        return device != null && isTargetDevice(device.getAddress());
    }

    private final OnLocationInfoListener mOnLocationInfoListener = new OnLocationInfoListener() {
        @Override
        public void onLocationInfoChange(AMapLocation location, BluetoothDevice device) {
            mView.onLocationChange(location, device);
        }

        @Override
        public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int rCode) {
            if (rCode == AMapException.CODE_AMAP_SUCCESS) { //成功
                mView.onRegeocodeSearched(regeocodeResult);
            }
        }

        @Override
        public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {

        }
    };

    private final BTRcspEventCallback mEventCallback = new BTRcspEventCallback() {

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (status == StateCode.CONNECTION_FAILED || status == StateCode.CONNECTION_DISCONNECT) {
                stopSound();
            }
        }

        @Override
        public void onBondStatus(BluetoothDevice device, int status) {
            if (status == BluetoothDevice.BOND_NONE) { //更新历史记录
                mView.onRemoveHistoryDeviceSuccess(DeviceAddrManager.getInstance().findHistoryBluetoothDevice(device));
            }
        }

        @Override
        public void onTwsStatusChange(BluetoothDevice device, boolean isTwsConnected) {
            mView.onTwsStatus(device, isTwsConnected);
        }

        @Override
        public void onSearchDevice(BluetoothDevice device, SearchDevParam searchDevParam) {
            if (isTargetDevice(device) && searchDevParam != null) { //查找设备的处理
                JL_Log.d("zzc_search", "onSearchDevice >>> " + searchDevParam);
                if (searchDevParam.getType() == Constants.SEARCH_TYPE_SYNC_STATUS) {
                    mRCSPController.syncSearchDeviceStatus(device, null);
                } else {
                    isPlayingSound = searchDevParam.getOp() == Constants.RING_OP_OPEN;
                    mView.onPlaySoundStatus(device, isPlayingSound);
                }
            }
        }
    };

    private final RingHandler.OnRingStatusListener mOnRingStatusListener = new RingHandler.OnRingStatusListener() {
        @Override
        public void onRingStatusChange(boolean isPlay) {
            isPlayingSound = isPlay;
            mView.onPlaySoundStatus(BluetoothUtil.getRemoteDevice(mTargetDevAddress), isPlayingSound);
        }
    };
}
