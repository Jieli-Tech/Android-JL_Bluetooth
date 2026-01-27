package com.jieli.btsmart.tool.translate.codec;

import com.jieli.bluetooth.bean.translation.AudioData;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.lib.audio.v2.JLAudioV2Codec;
import com.jieli.lib.audio.v2.callback.OnStateCallback;
import com.jieli.lib.audio.v2.callback.OnStreamStateCallback;
import com.jieli.lib.audio.v2.constant.ErrorCode;
import com.jieli.lib.audio.v2.model.CodecParam;

/**
 * JLAV2Codec
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc JLA_V2音频编解码
 * @since 2025/7/1
 */
public class JLAV2Codec extends AudioCodec {

    /**
     * 通话翻译的JLA_v2参数
     */
    private static final CodecParam codecParam = new CodecParam()
            .setSampleRate(16000L).setBitRate(16000L).setFrameLenIdx(11);

    /**
     * PCM文件编码成JLA_V2文件
     *
     * @param pcmFilePath String PCM文件路径
     * @param jlaFilePath String JLA_v2文件路径
     * @param callback    OnStateCallback 状态回调
     */
    public static void encodeFile(String pcmFilePath, String jlaFilePath, OnStateCallback callback) {
        try {
            final JLAudioV2Codec encoder = new JLAudioV2Codec();
            encoder.encodeFile(pcmFilePath, jlaFilePath, codecParam, new OnStateCallback() {
                @Override
                public void onStart() {
                    if (null != callback) {
                        callback.onStart();
                    }
                }

                @Override
                public void onStop(String s) {
                    encoder.release();
                    if (null != callback) {
                        callback.onStop(s);
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
        } catch (RuntimeException e) {
            if (null != callback) {
                callback.onError(ErrorCode.ERR_NOT_INIT, ErrorCode.printErrorCode(ErrorCode.ERR_NOT_INIT));
            }
        }
    }

    /**
     * JLA_v2解码器
     */
    private JLAudioV2Codec mDecoder;

    @Override
    public int getAudioType() {
        return Constants.AUDIO_TYPE_JLA_V2;
    }

    @Override
    public boolean isWorking() {
        if (null == mDecoder) return false;
        return mDecoder.isDecoding();
    }

    @Override
    public void startDecodeStream(OnAudioStreamCallback callback) {
        startDecodeStream(codecParam, callback);
    }

    @Override
    public void startDecodeStream(Object option, OnAudioStreamCallback callback) {
        if (!(option instanceof CodecParam)) {
            option = codecParam;
        }
        CodecParam param = (CodecParam) option;
        if (null == mDecoder) {
            try {
                mDecoder = new JLAudioV2Codec();
            } catch (RuntimeException e) {
                String message = AppUtil.formatString("Failed to init jla_v2 manager. " + e);
                if (null != callback) {
                    callback.onError(getAudioType(), ErrorCode.ERR_NOT_INIT, message);
                }
                return;
            }
        }
        mDecoder.startDecodeStream(param, new OnStreamStateCallback() {
            @Override
            public void onStreamData(byte[] bytes) {
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
            public void onStop(String s) {
                if (null != callback) {
                    callback.onStop(getAudioType(), s);
                }
            }

            @Override
            public void onError(int i, String s) {
                if (i == ErrorCode.ERR_SDK_RELEASE) {
                    if (null != callback) {
                        callback.onStop(getAudioType(), "");
                    }
                    return;
                }
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
        mDecoder.writeDecodeData(audioData.getAudioData());
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
        if (null != mDecoder) {
            mDecoder.release();
            mDecoder = null;
        }
    }
}
