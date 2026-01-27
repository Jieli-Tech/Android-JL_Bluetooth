package com.jieli.btsmart.tool.translate.codec;

import com.jieli.bluetooth.bean.translation.AudioData;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.jl_audio_decode.callback.OnDecodeStreamCallback;
import com.jieli.jl_audio_decode.callback.OnStateCallback;
import com.jieli.jl_audio_decode.constant.ErrorCode;
import com.jieli.jl_audio_decode.exceptions.OpusException;
import com.jieli.jl_audio_decode.opus.OpusManager;
import com.jieli.jl_audio_decode.opus.model.OpusOption;

/**
 * OpusCodec
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc OPUS音频编解码
 * @since 2025/6/24
 */
public class OpusCodec extends AudioCodec {

    /**
     * 编码文件
     *
     * @param pcmFilePath  String PCM文件路径
     * @param opusFilePath String OPUS文件路径
     * @param callback     OnStateCallback 状态回调
     */
    public static void encodeFile(String pcmFilePath, String opusFilePath, OnStateCallback callback) {
        try {
            final OpusManager encoder = new OpusManager();
            encoder.encodeFile(pcmFilePath, opusFilePath, new OnStateCallback() {
                @Override
                public void onStart() {
                    if (null != callback) {
                        callback.onStart();
                    }
                }

                @Override
                public void onComplete(String s) {
                    encoder.release();
                    if (null != callback) {
                        callback.onComplete(s);
                    }
                }

                @Override
                public void onError(int i, String s) {
                    encoder.release();
                    if (null != callback) {
                        callback.onError(i, s);
                    }
                }
            });
        } catch (OpusException e) {
            if (null != callback) {
                callback.onError(ErrorCode.ERR_NONE_INIT, ErrorCode.getErrorMsg(ErrorCode.ERR_NONE_INIT));
            }
        }
    }

    /**
     * OPUS解码器
     */
    private OpusManager mDecoder;

    @Override
    public int getAudioType() {
        return Constants.AUDIO_TYPE_OPUS;
    }

    @Override
    public boolean isWorking() {
        if (null == mDecoder) return false;
        return mDecoder.isDecodeStream();
    }

    @Override
    public void startDecodeStream(OnAudioStreamCallback callback) {
        startDecodeStream(new OpusOption(), callback);
    }

    @Override
    public void startDecodeStream(Object option, OnAudioStreamCallback callback) {
        if (!(option instanceof OpusOption)) {
            option = new OpusOption();
        }
        OpusOption opusOption = (OpusOption) option;
        if (null == mDecoder) {
            try {
                mDecoder = new OpusManager();
            } catch (OpusException e) {
                String message = AppUtil.formatString("Failed to init opus manager.\n" +
                        "message : %s", e.getMessage());
                if (null != callback) {
                    callback.onError(getAudioType(), ErrorCode.ERR_NONE_INIT, message);
                }
                return;
            }
        }
        mDecoder.startDecodeStream(opusOption, new OnDecodeStreamCallback() {
            @Override
            public void onDecodeStream(byte[] bytes) {
                if (null != callback) {
                    callback.onStream(getAudioType(), Constants.AUDIO_TYPE_PCM, bytes);
                }
            }

            @Override
            public void onStart() {
                if (null != callback) {
                    callback.onStart(getAudioType());
                }
            }

            @Override
            public void onComplete(String s) {
                if (null != callback) {
                    callback.onStop(getAudioType(), s);
                }
            }

            @Override
            public void onError(int i, String s) {
                if (null != callback) {
                    callback.onError(getAudioType(), i, s);
                }
            }
        });
    }

    @Override
    public boolean writeAudioData(AudioData audioData) {
        if (null == audioData || !isWorking() || audioData.getType() != getAudioType())
            return false;
        lastAudioData = audioData;
        mDecoder.writeAudioStream(audioData.getAudioData());
        return true;
    }

    @Override
    public boolean stopDecodeStream() {
        if (!isWorking()) return false;
        mDecoder.stopDecodeStream();
        return true;
    }

    @Override
    public void release() {
        stopDecodeStream();
        if (null != mDecoder) {
            mDecoder.release();
            mDecoder = null;
        }
    }
}
