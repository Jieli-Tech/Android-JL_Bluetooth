package com.jieli.btsmart.data.model.translation.ai_auth;

import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * DoubaoTranslationMessage
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 豆包云翻译认证信息
 * @since 2025/9/15
 */
public class DoubaoTranslationMessage {
    /**
     * 访问密钥
     */
    private String accessKey;
    /**
     * 加密密钥
     */
    private String secretKey;

    public DoubaoTranslationMessage(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
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
