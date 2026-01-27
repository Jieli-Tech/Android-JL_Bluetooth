package com.jieli.btsmart.tool.translate.codec;

import com.jieli.bluetooth.annotation.AudioType;
import com.jieli.bluetooth.bean.translation.AudioData;

/**
 * AudioCodec
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 音频编解码
 * @since 2025/6/24
 */
public abstract class AudioCodec {
    /**
     * 上一包音频数据
     */
    protected AudioData lastAudioData;

    /**
     * 音频类型
     */
    @AudioType
    public abstract int getAudioType();

    public AudioData getLastAudioData() {
        return lastAudioData;
    }

    public abstract boolean isWorking();

    public abstract void startDecodeStream(OnAudioStreamCallback callback);

    public abstract void startDecodeStream(Object option, OnAudioStreamCallback callback);

    public abstract boolean writeAudioData(AudioData audioData);

    public abstract boolean stopDecodeStream();

    public abstract void release();

    public interface OnAudioStreamCallback {

        void onStart(@AudioType int type);

        void onStop(@AudioType int type, String result);

        void onError(@AudioType int type, int code, String message);

        void onStream(@AudioType int srcType, @AudioType int audioType, byte[] data);
    }
}
