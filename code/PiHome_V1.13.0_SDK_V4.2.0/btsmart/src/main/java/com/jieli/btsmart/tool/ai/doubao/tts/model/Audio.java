package com.jieli.btsmart.tool.ai.doubao.tts.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * Audio
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 音频配置
 * @since 2025/6/24
 */
public class Audio implements Parcelable {

    /**
     * PCM编码格式
     */
    public static final String ENCODING_PCM = "pcm";

    /**
     * WAV编码格式
     */
    public static final String ENCODING_WAV = "wav";

    /**
     * OPUS编码格式
     */
    public static final String ENCODING_OGG_OPUS = "ogg_opus";

    /**
     * MP3编码格式
     */
    public static final String ENCODING_MP3 = "mp3";

    /**
     * 音色类型
     *
     * <p>必要项</p>
     */
    @SerializedName("voice_type")
    private final String voiceType;
    /**
     * 开启音色情感
     *
     * <p>
     * 可选项 <br/>
     * 开启后，[emotion]字段生效
     * </p>
     */
    @SerializedName("enable_emotion")
    private final Boolean enableEmotion;
    /**
     * 音色情感
     *
     * <p>可选项</p>
     */
    private final String emotion;
    /**
     * 音频编码格式
     *
     * <p>可选项</p>
     */
    private final String encoding;
    /**
     * 语速
     *
     * <p>
     * 可选项<br/>
     * 取值范围: [[0.8, 2]]， 默认是1.0
     * </p>
     */
    @SerializedName("speed_ratio")
    private final Float speedRatio;
    /**
     * 音频采样率
     *
     * <p>
     * 可选项<br/>
     * 可选值: 8000, 16000, 24000。默认是 24000
     * </p>
     */
    private final Integer rate;
    /**
     * 比特率
     *
     * <p>
     * 可选项<br/>
     * 可选值: 16, 96, 128等
     * </p>
     */
    @SerializedName("BitRate")
    private final Integer bitRate;
    /**
     * 明确语种
     *
     * <p>可选项</p>
     */
    @SerializedName("explicit_language")
    private final String explicitLanguage;
    /**
     * 参考语种
     *
     * <p>可选项</p>
     */
    @SerializedName("context_language")
    private final String contextLanguage;
    /**
     * 音量调节
     *
     * <p>
     * 可选项<br/>
     * 取值范围: [[0.5, 2]]，默认为1。
     * </p>
     */
    @SerializedName("loudness_ratio")
    private final Float loudnessRatio;

    public Audio(String voiceType) {
        this(voiceType, null, null, null, null, null, null);
    }

    public Audio(String voiceType, Boolean enableEmotion, String emotion, Float speedRatio, String explicitLanguage,
                 String contextLanguage, Float loudnessRatio) {
        this(voiceType, enableEmotion, emotion, ENCODING_PCM, speedRatio, 16000, 16, explicitLanguage, contextLanguage, loudnessRatio);
    }

    public Audio(String voiceType, Boolean enableEmotion, String emotion, String encoding, Float speedRatio,
                 Integer rate, Integer bitRate, String explicitLanguage, String contextLanguage, Float loudnessRatio) {
        this.voiceType = voiceType;
        this.enableEmotion = enableEmotion;
        this.emotion = emotion;
        this.encoding = encoding;
        this.speedRatio = speedRatio;
        this.rate = rate;
        this.bitRate = bitRate;
        this.explicitLanguage = explicitLanguage;
        this.contextLanguage = contextLanguage;
        this.loudnessRatio = loudnessRatio;
    }

    protected Audio(Parcel in) {
        voiceType = in.readString();
        byte tmpEnableEmotion = in.readByte();
        enableEmotion = tmpEnableEmotion == 0 ? null : tmpEnableEmotion == 1;
        emotion = in.readString();
        encoding = in.readString();
        if (in.readByte() == 0) {
            speedRatio = null;
        } else {
            speedRatio = in.readFloat();
        }
        if (in.readByte() == 0) {
            rate = null;
        } else {
            rate = in.readInt();
        }
        if (in.readByte() == 0) {
            bitRate = null;
        } else {
            bitRate = in.readInt();
        }
        explicitLanguage = in.readString();
        contextLanguage = in.readString();
        if (in.readByte() == 0) {
            loudnessRatio = null;
        } else {
            loudnessRatio = in.readFloat();
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(voiceType);
        dest.writeByte((byte) (enableEmotion == null ? 0 : enableEmotion ? 1 : 2));
        dest.writeString(emotion);
        dest.writeString(encoding);
        if (speedRatio == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeFloat(speedRatio);
        }
        if (rate == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(rate);
        }
        if (bitRate == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(bitRate);
        }
        dest.writeString(explicitLanguage);
        dest.writeString(contextLanguage);
        if (loudnessRatio == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeFloat(loudnessRatio);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Audio> CREATOR = new Creator<Audio>() {
        @Override
        public Audio createFromParcel(Parcel in) {
            return new Audio(in);
        }

        @Override
        public Audio[] newArray(int size) {
            return new Audio[size];
        }
    };

    public String getVoiceType() {
        return voiceType;
    }

    public Boolean getEnableEmotion() {
        return enableEmotion;
    }

    public String getEmotion() {
        return emotion;
    }

    public String getEncoding() {
        return encoding;
    }

    public Float getSpeedRatio() {
        return speedRatio;
    }

    public Integer getRate() {
        return rate;
    }

    public Integer getBitRate() {
        return bitRate;
    }

    public String getExplicitLanguage() {
        return explicitLanguage;
    }

    public String getContextLanguage() {
        return contextLanguage;
    }

    public Float getLoudnessRatio() {
        return loudnessRatio;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
