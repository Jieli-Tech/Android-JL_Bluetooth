package com.jieli.btsmart.data.model.translation.ai_auth;

import com.google.gson.JsonSyntaxException;
import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

import java.util.Calendar;

/**
 * AIAuthMessage
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc AI认证信息
 * @since 2025/9/15
 */
public class AIAuthMessage {

    /**
     * 豆包火山大模型
     */
    public static final String PLATFORM_DOUBAO = "doubao";
    /**
     * 有效期 --- 7天
     */
    public static final long VALIDITY = 3600 * 24 * 7L;

    /**
     * 平台
     */
    private String platform;
    /**
     * 翻译功能认证信息
     */
    private String translation;
    /**
     * TTS功能认证信息
     */
    private String tts;
    /**
     * 更新时间戳
     * <p>
     * 单位: 毫秒
     */
    private long updateTime;
    /**
     * 有效期
     * <p>
     * 单位: 秒
     */
    private long validity;

    public AIAuthMessage(String translation, String tts, long updateTime) {
        this(PLATFORM_DOUBAO, translation, tts, updateTime, VALIDITY);
    }

    public AIAuthMessage(String platform, String translation, String tts, long updateTime, long validity) {
        this.platform = platform;
        this.translation = translation;
        this.tts = tts;
        this.updateTime = updateTime;
        this.validity = validity;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public String getTts() {
        return tts;
    }

    public void setTts(String tts) {
        this.tts = tts;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public long getValidity() {
        return validity;
    }

    public void setValidity(long validity) {
        this.validity = validity;
    }

    public boolean isValid() {
        long validityTime = updateTime + validity * 1000;
        long currentTime = Calendar.getInstance().getTimeInMillis();
        return currentTime <= validityTime;
    }

    public DoubaoTranslationMessage getDoubaoTranslationMessage() {
        if (null == translation || translation.isEmpty() || !PLATFORM_DOUBAO.equals(platform))
            return null;
        try {
            return AIConfig.gson.fromJson(translation, DoubaoTranslationMessage.class);
        } catch (JsonSyntaxException ignored) {

        }
        return null;
    }

    public DoubaoTTSMessage getDoubaoTTSMessage() {
        if (null == tts || tts.isEmpty() || !PLATFORM_DOUBAO.equals(platform)) return null;
        try {
            return AIConfig.gson.fromJson(tts, DoubaoTTSMessage.class);
        } catch (JsonSyntaxException ignored) {

        }
        return null;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
