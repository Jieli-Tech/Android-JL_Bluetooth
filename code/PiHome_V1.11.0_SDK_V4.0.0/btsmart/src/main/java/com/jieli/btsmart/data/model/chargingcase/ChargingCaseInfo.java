package com.jieli.btsmart.data.model.chargingcase;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.settings.v0.ResourceInfo;
import com.jieli.bluetooth.bean.settings.v0.ScreenInfo;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 彩屏充电仓信息
 * @since 2023/12/12
 */
public class ChargingCaseInfo implements Parcelable {

    public static final int SCREEN_WIDTH = 320;
    public static final int SCREEN_HEIGHT = 172;

    /**
     * 设备地址
     * <p>
     * 唯一标识
     * </p>
     */
    private final String address;
    /**
     * 屏幕亮度
     */
    private int brightness;
    /**
     * 手电筒状态
     */
    private boolean isFlashlightOn;
    /**
     * 屏幕信息
     */
    private ScreenInfo screenInfo;
    /**
     * 当前屏幕保护程序信息
     */
    private ResourceInfo currentScreenSaver;
    /**
     * 当前开机动画信息
     */
    private ResourceInfo currentBootAnim;

    public ChargingCaseInfo(String address) {
        this.address = address;
    }

    protected ChargingCaseInfo(@NonNull Parcel in) {
        address = in.readString();
        brightness = in.readInt();
        isFlashlightOn = in.readByte() != 0;
        screenInfo = in.readParcelable(ScreenInfo.class.getClassLoader());
        currentScreenSaver = in.readParcelable(ResourceInfo.class.getClassLoader());
        currentBootAnim = in.readParcelable(ResourceInfo.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeInt(brightness);
        dest.writeByte((byte) (isFlashlightOn ? 1 : 0));
        dest.writeParcelable(screenInfo, flags);
        dest.writeParcelable(currentScreenSaver, flags);
        dest.writeParcelable(currentBootAnim, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ChargingCaseInfo> CREATOR = new Creator<ChargingCaseInfo>() {
        @Override
        public ChargingCaseInfo createFromParcel(Parcel in) {
            return new ChargingCaseInfo(in);
        }

        @Override
        public ChargingCaseInfo[] newArray(int size) {
            return new ChargingCaseInfo[size];
        }
    };

    public String getAddress() {
        return address;
    }

    public int getBrightness() {
        return brightness;
    }

    public ChargingCaseInfo setBrightness(int brightness) {
        this.brightness = brightness;
        return this;
    }

    public boolean isFlashlightOn() {
        return isFlashlightOn;
    }

    public ChargingCaseInfo setFlashlightOn(boolean flashlightOn) {
        isFlashlightOn = flashlightOn;
        return this;
    }

    public ScreenInfo getScreenInfo() {
        return screenInfo;
    }

    public ChargingCaseInfo setScreenInfo(ScreenInfo screenInfo) {
        this.screenInfo = screenInfo;
        return this;
    }

    public ResourceInfo getCurrentScreenSaver() {
        return currentScreenSaver;
    }

    public ChargingCaseInfo setCurrentScreenSaver(ResourceInfo currentScreenSaver) {
        this.currentScreenSaver = currentScreenSaver;
        return this;
    }

    public ResourceInfo getCurrentBootAnim() {
        return currentBootAnim;
    }

    public ChargingCaseInfo setCurrentBootAnim(ResourceInfo currentBootAnim) {
        this.currentBootAnim = currentBootAnim;
        return this;
    }

    public String getCurrentScreenSaverPath() {
        if(null == currentScreenSaver) return null;
        return currentScreenSaver.getPath();
    }

    public String getCurrentBootAnimPath() {
       if(null == currentBootAnim) return null;
        return currentBootAnim.getPath();
    }

    public int getScreenWidth() {
        return screenInfo == null ? SCREEN_WIDTH : screenInfo.getWidth();
    }

    public int getScreenHeight() {
        return screenInfo == null ? SCREEN_HEIGHT : screenInfo.getHeight();
    }

    @Override
    public String toString() {
        return "ChargingCaseInfo{" +
                "address='" + address + '\'' +
                ", brightness=" + brightness +
                ", isFlashlightOn=" + isFlashlightOn +
                ", screenInfo=" + screenInfo +
                ", currentScreenSaver=" + currentScreenSaver +
                ", currentBootAnim=" + currentBootAnim +
                '}';
    }
}
