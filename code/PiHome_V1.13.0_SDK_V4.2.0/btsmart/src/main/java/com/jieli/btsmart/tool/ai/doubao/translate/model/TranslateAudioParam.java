package com.jieli.btsmart.tool.ai.doubao.translate.model;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.annotation.AudioType;
import com.jieli.btsmart.data.model.translation.RoleInfo;
import com.jieli.btsmart.tool.ai.doubao.translate.model.request.Configuration;

/**
 * TranslationAudioParam
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译音频流参数
 * @since 2025/9/4
 */
public class TranslateAudioParam extends TranslateParam {
    /**
     * 配置信息
     */
    @NonNull
    private final Configuration configuration;
    /**
     * 音频类型
     */
    @AudioType
    private final int audioType;

    public TranslateAudioParam(@NonNull String mac, @NonNull RoleInfo roleInfo,
                               @NonNull Configuration configuration, @AudioType int audioType) {
        this(mac, roleInfo, true, 0, configuration, audioType);
    }

    public TranslateAudioParam(@NonNull String mac, @NonNull RoleInfo roleInfo, boolean isIdAutoInc, int recordId,
                               @NonNull Configuration configuration, @AudioType int audioType) {
        super(mac, roleInfo, isIdAutoInc, recordId);
        this.configuration = configuration;
        this.audioType = audioType;
    }

    @Override
    public int getTranslationWay() {
        return WAY_AUDIO_STREAM;
    }

    @NonNull
    public Configuration getConfiguration() {
        return configuration;
    }

    @AudioType
    public int getAudioType() {
        return audioType;
    }

    @Override
    public String toString() {
        return "TranslationAudioParam{" +
                "mac='" + getMac() + '\'' +
                ", roleInfo=" + getRoleInfo() +
                ", isIdAutoInc=" + isIdAutoInc() +
                ", configuration=" + configuration +
                ", audioType=" + audioType +
                '}';
    }
}
