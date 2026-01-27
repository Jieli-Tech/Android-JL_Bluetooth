package com.jieli.btsmart.data.model.auracast;

import com.jieli.btsmart.tool.configure.ConfigureKit;

/**
 * AuracastLoginInfo
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/24
 * note: Auracast设备登录信息
 */
public class AuracastLoginInfo {
    /**
     * 设备名称
     */
    private String deviceName;
    /**
     * 登录密码
     */
    private String loginPassword;

    public String getDeviceName() {
        return deviceName;
    }

    public AuracastLoginInfo setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    public String getLoginPassword() {
        return loginPassword;
    }

    public AuracastLoginInfo setLoginPassword(String loginPassword) {
        this.loginPassword = loginPassword;
        return this;
    }

    @Override
    public String toString() {
        return ConfigureKit.GSON.toJson(this);
    }
}
