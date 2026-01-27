package com.jieli.btsmart.data.model.translation.ai_auth;

import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * DoubaoTTSMessage
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 豆包语音合成认证信息
 * @since 2025/9/15
 */
public class DoubaoTTSMessage {

    /**
     * 应用标识
     */
    private String appId;
    /**
     * 访问凭证
     */
    private String accessToken;
    /**
     * 加密密钥
     */
    private String secretKey;

    public DoubaoTTSMessage(String appId, String accessToken, String secretKey) {
        this.appId = appId;
        this.accessToken = accessToken;
        this.secretKey = secretKey;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
