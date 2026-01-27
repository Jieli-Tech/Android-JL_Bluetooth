package com.jieli.btsmart.data.model.chargingcase;

import android.os.Parcel;
import android.os.Parcelable;

import com.jieli.bluetooth.bean.charging_case.ChargingCaseInfo;

/**
 * ResourceMsg
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 资源信息
 * @since 2025/2/5
 */
public class ResourceMsg implements Parcelable {
    /**
     * 设备地址
     */
    private final String mac;
    /**
     * 芯片型号
     */
    private int chip;
    /**
     * 屏幕长度
     */
    private int screenWidth;
    /**
     * 屏幕宽度
     */
    private int screenHeight;
    /**
     * 资料类型
     */
    @ChargingCaseInfo.ResourceType
    private int resourceType;
    /**
     * 原文件路径
     */
    private String srcFilePath;
    /**
     * 二进制文件路径
     */
    private String binFilePath;
    /**
     * 二进制文件CRC
     */
    private short binFileCrc;

    public ResourceMsg(String mac) {
        this.mac = mac;
        resourceType = ChargingCaseInfo.TYPE_SCREEN_SAVER;
    }


    protected ResourceMsg(Parcel in) {
        mac = in.readString();
        chip = in.readInt();
        screenWidth = in.readInt();
        screenHeight = in.readInt();
        resourceType = in.readInt();
        srcFilePath = in.readString();
        binFilePath = in.readString();
        binFileCrc = (short) in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mac);
        dest.writeInt(chip);
        dest.writeInt(screenWidth);
        dest.writeInt(screenHeight);
        dest.writeInt(resourceType);
        dest.writeString(srcFilePath);
        dest.writeString(binFilePath);
        dest.writeInt(binFileCrc);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ResourceMsg> CREATOR = new Creator<ResourceMsg>() {
        @Override
        public ResourceMsg createFromParcel(Parcel in) {
            return new ResourceMsg(in);
        }

        @Override
        public ResourceMsg[] newArray(int size) {
            return new ResourceMsg[size];
        }
    };

    public String getMac() {
        return mac;
    }

    public int getChip() {
        return chip;
    }

    public ResourceMsg setChip(int chip) {
        this.chip = chip;
        return this;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public ResourceMsg setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
        return this;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public ResourceMsg setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
        return this;
    }

    @ChargingCaseInfo.ResourceType
    public int getResourceType() {
        return resourceType;
    }

    public ResourceMsg setResourceType(@ChargingCaseInfo.ResourceType int resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public String getSrcFilePath() {
        return srcFilePath;
    }

    public ResourceMsg setSrcFilePath(String srcFilePath) {
        this.srcFilePath = srcFilePath;
        return this;
    }

    public String getBinFilePath() {
        return binFilePath;
    }

    public ResourceMsg setBinFilePath(String binFilePath) {
        this.binFilePath = binFilePath;
        return this;
    }

    public short getBinFileCrc() {
        return binFileCrc;
    }

    public ResourceMsg setBinFileCrc(short binFileCrc) {
        this.binFileCrc = binFileCrc;
        return this;
    }

    @Override
    public String toString() {
        return "ResourceMsg{" +
                "mac='" + mac + '\'' +
                ", chip='" + chip + '\'' +
                ", screenWidth=" + screenWidth +
                ", screenHeight=" + screenHeight +
                ", resourceType=" + resourceType +
                ", srcFilePath='" + srcFilePath + '\'' +
                ", binFilePath='" + binFilePath + '\'' +
                ", binFileCrc=" + binFileCrc +
                '}';
    }
}
