package com.jieli.btsmart.tool.ai.doubao.tts.model;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.translation.TranslationResult;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.data.model.translation.RoleInfo;
import com.jieli.btsmart.data.model.translation.ai_auth.AIAuthMessage;
import com.jieli.btsmart.data.model.translation.ai_auth.DoubaoTTSMessage;
import com.jieli.btsmart.tool.ai.doubao.translate.model.language.Language;
import com.jieli.btsmart.tool.ai.doubao.tts.OnTtsResultCallback;
import com.jieli.btsmart.tool.configure.ConfigureKit;

import java.util.UUID;

/**
 * TtsTask
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc TTS任务
 * @since 2025/6/30
 */
public class TtsTask {

    /**
     * 中文/英文的音色
     */
    public static final String DEFAULT_VOICE_TYPE = "zh_female_cancan_mars_bigtts";
    /**
     * 日语的音色
     */
    public static final String JP_VOICE_TYPE = "multi_zh_male_youyoujunzi_moon_bigtts";
    /**
     * 其他语种的音色
     */
    public static final String OTHER_VOICE_TYPE = "multi_female_maomao_conversation_wvae_bigtts";

    /**
     * 设备地址
     */
    @NonNull
    private final String mac;
    /**
     * 是否使用A2DP播报
     */
    private final boolean isUseA2DP;
    /**
     * 角色信息
     */
    @NonNull
    private final RoleInfo roleInfo;
    /**
     * 翻译结果
     */
    @NonNull
    private final TranslationResult translationResult;
    /**
     * 结果回调
     */
    private final OnTtsResultCallback callback;

    public TtsTask(@NonNull String mac, @NonNull RoleInfo roleInfo, boolean isUseA2DP, @NonNull TranslationResult translationResult, OnTtsResultCallback callback) {
        this.mac = mac;
        this.roleInfo = roleInfo;
        this.isUseA2DP = isUseA2DP;
        this.translationResult = translationResult;
        this.callback = callback;
    }

    @NonNull
    public String getMac() {
        return mac.replaceAll(":", "_");
    }

    public boolean isUseA2DP() {
        return isUseA2DP;
    }

    @NonNull
    public RoleInfo getRoleInfo() {
        return roleInfo;
    }

    @NonNull
    public TranslationResult getTranslationResult() {
        return translationResult;
    }

    public OnTtsResultCallback getCallback() {
        return callback;
    }

    public String getVoiceType(String language) {
        if (TextUtils.isEmpty(language)) return DEFAULT_VOICE_TYPE;
        if (language.equalsIgnoreCase(Language.LANG_JA)) {
            return JP_VOICE_TYPE;
        } else if (language.equalsIgnoreCase(Language.LANG_ES) || language.equalsIgnoreCase(Language.LANG_ID)
                || language.equalsIgnoreCase(Language.LANG_PT_BR)) {
            return OTHER_VOICE_TYPE;
        }
        return DEFAULT_VOICE_TYPE;
    }

    public TtsRequest buildTtsRequest(String language, String text) {
        final AIAuthMessage authMessage = ConfigureKit.getInstance().getAIAuthMessage();
        final DoubaoTTSMessage ttsMessage = null == authMessage ? null : authMessage.getDoubaoTTSMessage();
        if (null == ttsMessage || !authMessage.isValid()) {
            String msg = null == ttsMessage ? "NO Doubao TTS Message" : "Auth Message is expired.";
            JL_Log.w(TtsTask.class.getSimpleName(), "buildTtsRequest", msg);
            return null;
        }
        String appId = ttsMessage.getAppId();
        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(text)) return null;
        return new TtsRequest(new App(appId, ""),
                new User(mac), new Audio(getVoiceType(language)),
                new Request(UUID.randomUUID().toString(), text));
    }

    @Override
    public String toString() {
        return "TtsTask{" +
                "mac='" + mac + '\'' +
                ", isUseA2DP=" + isUseA2DP +
                ", translationResult=" + translationResult +
                ", callback=" + callback +
                '}';
    }
}
