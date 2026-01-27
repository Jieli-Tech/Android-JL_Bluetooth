package com.jieli.btsmart.data.model.bluetooth;

import androidx.annotation.NonNull;

/**
 * 电量信息
 */
public class BatteryInfo {
    private int battery;

    public BatteryInfo() {

    }

    public BatteryInfo(int battery) {
        setBattery(battery);
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    @NonNull
    @Override
    public String toString() {
        return "BatteryInfo{" +
                "battery=" + battery +
                '}';
    }
}
