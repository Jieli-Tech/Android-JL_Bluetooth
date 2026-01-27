package com.jieli.btsmart.data.model;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 切换经典蓝牙参数
 * @since 2023/11/6
 */
public class SwitchEdrParam {

    private final String edrName;
    private final String targetEdeName;

    public SwitchEdrParam(String edrName, String targetEdeName) {
        this.edrName = edrName;
        this.targetEdeName = targetEdeName;
    }

    public String getEdrName() {
        return edrName;
    }

    public String getTargetEdeName() {
        return targetEdeName;
    }
}
