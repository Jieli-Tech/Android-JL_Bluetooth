package com.jieli.btsmart.tool.location;

import android.bluetooth.BluetoothDevice;

import com.amap.api.location.AMapLocation;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.RegeocodeResult;

/**
 * 位置信息监听器
 *
 * @author zqjasonZhong
 * @since 2020/7/8
 */
public interface OnLocationInfoListener {

    /**
     * 地址位置改变回调
     *
     * @param location 位置信息
     * @param device 更新信息的设备
     */
    void onLocationInfoChange(AMapLocation location, BluetoothDevice device);

    /**
     * 逆地理编码回调
     */
    void onRegeocodeSearched(RegeocodeResult regeocodeResult, int rCode);

    /**
     * 地理编码查询回调
     */
    void onGeocodeSearched(GeocodeResult geocodeResult, int rCode);
}
