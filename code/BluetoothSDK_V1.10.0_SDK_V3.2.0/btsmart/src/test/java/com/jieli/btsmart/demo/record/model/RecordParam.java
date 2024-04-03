package com.jieli.btsmart.demo.record.model;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc  录音参数
 * @since 2022/9/27
 */
public class RecordParam {
    /**
     * 音频类型 -- PCM格式
     */
    public static final int VOICE_TYPE_PCM = 0;
    /**
     * 音频类型 -- SPEEX格式
     */
    public static final int VOICE_TYPE_SPEEX = 1;
    /**
     * 音频类型 -- OPUS格式
     */
    public static final int VOICE_TYPE_OPUS = 2;

    /**
     * 采样率 -- 8K
     */
    public static final int SAMPLE_RATE_8K = 8;
    /**
     * 采样率 -- 16K
     */
    public static final int SAMPLE_RATE_16K = 16;

    /**
     * VAD方式 -- 设备端判断
     */
    public static final int VAD_WAY_DEVICE = 0;
    /**
     * VAD方式 -- SDK端判断
     */
    public static final int VAD_WAY_SDK = 1;

    private final int voiceType;      //音频类型
    private final int sampleRate;     //采样率
    private final int vadWay;         //VAD方式

    public RecordParam(int voiceType, int sampleRate, int vadWay) {
        this.voiceType = voiceType;
        this.sampleRate = sampleRate;
        this.vadWay = vadWay;
    }

    public int getVoiceType() {
        return voiceType;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getVadWay() {
        return vadWay;
    }

    @Override
    public String toString() {
        return "RecordParam{" +
                "voiceType=" + voiceType +
                ", sampleRate=" + sampleRate +
                ", vadWay=" + vadWay +
                '}';
    }
}
