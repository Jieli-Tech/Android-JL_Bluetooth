package com.jieli.btsmart.ui.search;

import com.amap.api.services.core.LatLonPoint;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/8/18 17:15
 * @desc :
 */
public class LocationDeviceInfo {
    public LatLonPoint location;
    public String locationString;
    private final HistoryBluetoothDevice historyBluetoothDevice;

    public LocationDeviceInfo(HistoryBluetoothDevice history){
        this.historyBluetoothDevice = history;
    }

    public HistoryBluetoothDevice getHistoryBluetoothDevice() {
        return historyBluetoothDevice;
    }
}
