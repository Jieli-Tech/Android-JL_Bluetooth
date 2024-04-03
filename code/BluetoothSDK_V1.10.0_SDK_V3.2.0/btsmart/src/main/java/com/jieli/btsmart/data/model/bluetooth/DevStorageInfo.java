package com.jieli.btsmart.data.model.bluetooth;

import androidx.annotation.NonNull;

/**
 * 设备外接存储信息
 *
 * @author zqjasonZhong
 * @since 2020/5/13
 */
public class DevStorageInfo {
    private int ubsStatus;  //外部存储器状态( 0 -- 不在线  1 -- 在线)
    private int sd0Status;
    private int sd1Status;
    private int flashStatus;
    private int usbHandler; //usb句柄
    private int sd0Handler;  //sd0句柄
    private int sd1Handler;  //sd1句柄
    private int flashHandler; //flash句柄


    public int getUbsStatus() {
        return ubsStatus;
    }

    public void setUbsStatus(int ubsStatus) {
        this.ubsStatus = ubsStatus;
    }

    public int getSd0Status() {
        return sd0Status;
    }

    public void setSd0Status(int sd0Status) {
        this.sd0Status = sd0Status;
    }

    public int getSd1Status() {
        return sd1Status;
    }

    public void setSd1Status(int sd1Status) {
        this.sd1Status = sd1Status;
    }

    public int getFlashStatus() {
        return flashStatus;
    }

    public void setFlashStatus(int flashStatus) {
        this.flashStatus = flashStatus;
    }

    public int getUsbHandler() {
        return usbHandler;
    }

    public void setUsbHandler(int usbHandler) {
        this.usbHandler = usbHandler;
    }

    public int getSd0Handler() {
        return sd0Handler;
    }

    public void setSd0Handler(int sd0Handler) {
        this.sd0Handler = sd0Handler;
    }

    public int getSd1Handler() {
        return sd1Handler;
    }

    public void setSd1Handler(int sd1Handler) {
        this.sd1Handler = sd1Handler;
    }

    public int getFlashHandler() {
        return flashHandler;
    }

    public void setFlashHandler(int flashHandler) {
        this.flashHandler = flashHandler;
    }

    @NonNull
    @Override
    public String toString() {
        return "DevStorageInfo{" +
                "ubsStatus=" + ubsStatus +
                ", sd0Status=" + sd0Status +
                ", sd1Status=" + sd1Status +
                ", flashStatus=" + flashStatus +
                ", usbHandler=" + usbHandler +
                ", sd0Handler=" + sd0Handler +
                ", sd1Handler=" + sd1Handler +
                ", flashHandler=" + flashHandler +
                '}';
    }
}
