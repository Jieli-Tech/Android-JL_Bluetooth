package com.jieli.btsmart.data.model.chargingcase;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.charging_case.ChargingCaseInfo;

/**
 * ChargingCaseInfoChange
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 彩屏仓信息改变参数
 * @since 2025/2/18
 */
public class ChargingCaseInfoChange {

    @ChargingCaseInfo.Function
    private final int func;
    @NonNull
    private final ChargingCaseInfo chargingCaseInfo;

    public ChargingCaseInfoChange(@ChargingCaseInfo.Function int func, @NonNull ChargingCaseInfo chargingCaseInfo) {
        this.func = func;
        this.chargingCaseInfo = chargingCaseInfo;
    }

    @ChargingCaseInfo.Function
    public int getFunc() {
        return func;
    }

    @NonNull
    public ChargingCaseInfo getChargingCaseInfo() {
        return chargingCaseInfo;
    }

    @Override
    public String toString() {
        return "ChargingCaseInfoChange{" +
                "func=" + ChargingCaseInfo.printFunction(func) +
                ", chargingCaseInfo=" + chargingCaseInfo +
                '}';
    }
}
