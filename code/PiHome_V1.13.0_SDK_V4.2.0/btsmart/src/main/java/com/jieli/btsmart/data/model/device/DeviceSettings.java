package com.jieli.btsmart.data.model.device;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * DeviceConfiguration
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 设备设置
 * @since 2025/2/25
 */
public class DeviceSettings implements Parcelable {
    /**
     * 设备标识
     */
    private final String mac;
    /**
     * 是否同步天气信息
     */
    private boolean isSyncWeather;
    /**
     * 是否同步信息
     */
    private boolean isSyncMessage;
    /**
     * 同步应用包名列表
     */
    private List<String> appPacketNameList;

    public DeviceSettings(String mac) {
        this.mac = mac;
    }

    protected DeviceSettings(Parcel in) {
        mac = in.readString();
        isSyncWeather = in.readByte() != 0;
        isSyncMessage = in.readByte() != 0;
        appPacketNameList = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mac);
        dest.writeByte((byte) (isSyncWeather ? 1 : 0));
        dest.writeByte((byte) (isSyncMessage ? 1 : 0));
        dest.writeStringList(appPacketNameList);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DeviceSettings> CREATOR = new Creator<DeviceSettings>() {
        @Override
        public DeviceSettings createFromParcel(Parcel in) {
            return new DeviceSettings(in);
        }

        @Override
        public DeviceSettings[] newArray(int size) {
            return new DeviceSettings[size];
        }
    };

    public String getMac() {
        return mac;
    }

    public boolean isSyncWeather() {
        return isSyncWeather;
    }

    public void setSyncWeather(boolean syncWeather) {
        isSyncWeather = syncWeather;
    }

    public boolean isSyncMessage() {
        return isSyncMessage;
    }

    public void setSyncMessage(boolean syncMessage) {
        isSyncMessage = syncMessage;
    }

    public List<String> getAppPacketNameList() {
        return appPacketNameList;
    }

    public void setAppPacketNameList(List<String> appPacketNameList) {
        this.appPacketNameList = appPacketNameList;
    }

    @Override
    public String toString() {
        return "DeviceSettings{" +
                "mac='" + mac + '\'' +
                ", isSyncWeather=" + isSyncWeather +
                ", isSyncMessage=" + isSyncMessage +
                ", syncAppNameList=" + appPacketNameList +
                '}';
    }
}
