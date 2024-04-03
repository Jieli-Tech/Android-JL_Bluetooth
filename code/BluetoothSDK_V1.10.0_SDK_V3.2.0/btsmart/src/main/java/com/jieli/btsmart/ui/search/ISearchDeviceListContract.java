package com.jieli.btsmart.ui.search;

import com.amap.api.location.AMapLocation;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.btsmart.ui.base.bluetooth.IBluetoothBase;

import java.util.List;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/8/18 14:21
 * @desc :查找历史设备列表接口
 */
public interface ISearchDeviceListContract {
    interface ISearchDeviceListPresenter extends IBluetoothBase.IBluetoothPresenter {
        List<LocationDeviceInfo> getHistoryBtDeviceList();

        HistoryBluetoothDevice getConnectedHistoryBtRecord();

        HistoryBluetoothDevice getHistoryBtRecordByAddress(String address);

        void getHistoryDeviceListLocation(List<LocationDeviceInfo> deviceList);

        void destroy();
    }

    interface ISearchDeviceListView extends IBluetoothBase.IBluetoothView {
        void onLocationChange(AMapLocation location);

        void onRegeocodeSearched(RegeocodeResult regeocodeResult);

        void onRemoveHistoryDeviceSuccess(HistoryBluetoothDevice device);
    }
}
