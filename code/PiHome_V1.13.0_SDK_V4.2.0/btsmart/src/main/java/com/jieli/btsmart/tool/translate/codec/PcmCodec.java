package com.jieli.btsmart.tool.translate.codec;

import com.jieli.bluetooth.bean.translation.AudioData;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.ErrorCode;

/**
 * PcmCodec
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc PCM编解码
 * @since 2025/8/12
 */
public class PcmCodec extends AudioCodec {
    /**
     * 音频流回调
     */
    private OnAudioStreamCallback mCallback;
    /**
     * 是否工作中
     */
    private boolean isWorking;

    @Override
    public int getAudioType() {
        return Constants.AUDIO_TYPE_PCM;
    }

    @Override
    public boolean isWorking() {
        return isWorking;
    }

    @Override
    public void startDecodeStream(OnAudioStreamCallback callback) {
        startDecodeStream(null, callback);
    }

    @Override
    public void startDecodeStream(Object option, OnAudioStreamCallback callback) {
        if (isWorking()) {
            if (null != callback) {
                int code = ErrorCode.SUB_ERR_OPERATION_IN_PROGRESS;
                callback.onError(getAudioType(), code, ErrorCode.code2Msg(code));
            }
            return;
        }
        mCallback = callback;
        isWorking = true;
        if (null != callback) {
            callback.onStart(getAudioType());
        }
    }

    @Override
    public boolean writeAudioData(AudioData audioData) {
        if (null == audioData || !isWorking() || audioData.getType() != getAudioType())
            return false;
        lastAudioData = audioData;
        if (null != mCallback) {
            mCallback.onStream(getAudioType(), Constants.AUDIO_TYPE_PCM, audioData.getAudioData());
        }
        return true;
    }

    @Override
    public boolean stopDecodeStream() {
        if (!isWorking()) return false;
        isWorking = false;
        final OnAudioStreamCallback callback = mCallback;
        mCallback = null;
        if (null != callback) {
            callback.onStop(getAudioType(), "Success");
        }
        return true;
    }

    @Override
    public void release() {
        stopDecodeStream();
    }
}
