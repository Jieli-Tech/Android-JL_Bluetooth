package com.jieli.btsmart.ui.search;

import android.bluetooth.BluetoothDevice;

import com.amap.api.location.AMapLocation;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.btsmart.ui.base.bluetooth.IBluetoothBase;

import java.util.List;

/**
 * 搜索设备接口
 *
 * @author zqjasonZhong
 * @since 2020/7/8
 */
public interface ISearchDeviceContract {

    interface ISearchDevicePresenter extends IBluetoothBase.IBluetoothPresenter {

        List<HistoryBluetoothDevice> getHistoryBtDeviceList();

        HistoryBluetoothDevice getConnectedHistoryBtRecord();

        HistoryBluetoothDevice getHistoryBtRecordByAddress(String address);

        void setTargetDevAddress(String address);

        boolean isPlayingSound(String address);

        boolean isTwsConnected(String address);

        boolean isLocation();

        void startLocation();

        void stopLocation();

        void getFromLocation(double latitude, double longitude);

        void playSound(int way);

        void stopSound();

        void destroy();
    }

    interface ISearchDeviceView extends IBluetoothBase.IBluetoothView {

        void onRemoveHistoryDeviceSuccess(HistoryBluetoothDevice device);

        void onRemoveHistoryDeviceFailed(BaseError error);

        void onLocationChange(AMapLocation location, BluetoothDevice device);

        void onRegeocodeSearched(RegeocodeResult regeocodeResult);

        void onPlaySoundSuccess(BluetoothDevice device);

        void onPlaySoundFailed(BluetoothDevice device, BaseError error);

        void onTwsStatus(BluetoothDevice device, boolean isTwsConnected);

        void onPlaySoundStatus(BluetoothDevice device, boolean isPlaying);
    }
}
