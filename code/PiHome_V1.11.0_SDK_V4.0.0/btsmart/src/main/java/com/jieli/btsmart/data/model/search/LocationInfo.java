package com.jieli.btsmart.data.model.search;

/**
 * 位置信息
 *
 * @author zqjasonZhong
 * @since 2020/11/3
 */
public class LocationInfo {
    private double latitude; //纬度
    private double longitude; //经度
    private long updateTime; //更新时间
    private int deviceFlag; //设备标识

    public final static int DEVICE_FLAG_NONE = 0; //无设备标识
    public final static int DEVICE_FLAG_LEFT = 1; //左设备标识
    public final static int DEVICE_FLAG_RIGHT = 2; //右设备标识

    public LocationInfo(){

    }

    public LocationInfo(double latitude, double longitude, long updateTime){
        this(latitude, longitude, updateTime, DEVICE_FLAG_NONE);
    }

    public LocationInfo(double latitude, double longitude, long updateTime, int deviceFlag){
        setLatitude(latitude).setLongitude(longitude).setUpdateTime(updateTime).setDeviceFlag(deviceFlag);
    }

    public double getLatitude() {
        return latitude;
    }

    public LocationInfo setLatitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public double getLongitude() {
        return longitude;
    }

    public LocationInfo setLongitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public LocationInfo setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public int getDeviceFlag() {
        return deviceFlag;
    }

    public LocationInfo setDeviceFlag(int deviceFlag) {
        this.deviceFlag = deviceFlag;
        return this;
    }

    @Override
    public String toString() {
        return "LocationInfo{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", updateTime=" + updateTime +
                ", deviceFlag=" + deviceFlag +
                '}';
    }
}
