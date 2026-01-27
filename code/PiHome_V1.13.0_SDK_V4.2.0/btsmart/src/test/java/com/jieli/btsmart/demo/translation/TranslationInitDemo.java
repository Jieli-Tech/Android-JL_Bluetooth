package com.jieli.btsmart.demo.translation;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.translation.AudioData;
import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.bluetooth.bean.translation.TranslationResult;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.impl.rcsp.RcspOpImpl;
import com.jieli.bluetooth.impl.rcsp.translation.TranslationImpl;
import com.jieli.bluetooth.interfaces.rcsp.translation.AITranslationCallback;
import com.jieli.bluetooth.interfaces.rcsp.translation.IAITranslationApi;
import com.jieli.bluetooth.interfaces.rcsp.translation.TranslationCallback;
import com.jieli.jl_audio_decode.callback.OnDecodeStreamCallback;
import com.jieli.jl_audio_decode.callback.OnEncodeStreamCallback;
import com.jieli.jl_audio_decode.exceptions.OpusException;
import com.jieli.jl_audio_decode.opus.OpusManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * TranslationInitDemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译功能初始化示例代码
 * @since 2025/1/5
 */
class TranslationInitDemo {

    private TranslationImpl translationImpl = null;

    void initByCustomAITranslation() {
        final RcspOpImpl rcspOp = RCSPController.getInstance().getRcspOp();
        translationImpl = new TranslationImpl(rcspOp,
                new CustomAITranslation());
        boolean isSupportTranslation = translationImpl.isSupportTranslation();
        if (!isSupportTranslation) { //不支持翻译功能
            translationImpl.destroy();
            translationImpl = null;
            return;
        }
        //添加翻译事件回调
        translationImpl.addTranslationCallback(new TranslationCallback() {
            @Override
            public void onModeChange(@NonNull BluetoothDevice device, @NonNull TranslationMode mode) {
                //回调翻译模式改变
            }

            @Override
            public void onReceiveAudioData(@NonNull BluetoothDevice device, @NonNull AudioData audioData) {
                //回调接收到的音频数据
            }

            @Override
            public void onError(BluetoothDevice device, int code, String message) {
                //回调错误事件
            }
        });
    }

    void initByCustomTranslationFlow() {
        final RcspOpImpl rcspOp = RCSPController.getInstance().getRcspOp();
        translationImpl = new TranslationImpl(rcspOp, null);
        boolean isSupportTranslation = translationImpl.isSupportTranslation();
        if (!isSupportTranslation) { //不支持翻译功能
            translationImpl.destroy();
            translationImpl = null;
            return;
        }
        //添加翻译事件回调
        translationImpl.addTranslationCallback(new TranslationCallback() {
            @Override
            public void onModeChange(@NonNull BluetoothDevice device, @NonNull TranslationMode mode) {
                //回调翻译模式改变
                //自行处理各种模式
                switch (mode.getMode()) {
                    case TranslationMode.MODE_IDLE: {
                        //空闲模式
                        break;
                    }
                    case TranslationMode.MODE_RECORD: {
                        //录音模式
                        break;
                    }
                    case TranslationMode.MODE_RECORDING_TRANSLATION: {
                        //录音翻译模式
                        break;
                    }
                    case TranslationMode.MODE_CALL_TRANSLATION: {
                        //通话翻译模式
                        break;
                    }
                    case TranslationMode.MODE_AUDIO_TRANSLATION: {
                        //音视频翻译模式
                        break;
                    }
                    case TranslationMode.MODE_FACE_TO_FACE_TRANSLATION: {
                        //面对面翻译模式
                        break;
                    }
                    case TranslationMode.MODE_CALL_TRANSLATION_WITH_STEREO: {
                        //通话翻译立体声模式
                        break;
                    }
                    case TranslationMode.MODE_CALL_RECORD: {
                        //通话录音模式
                        break;
                    }
                }
            }

            @Override
            public void onReceiveAudioData(@NonNull BluetoothDevice device, @NonNull AudioData audioData) {
                //回调接收到的音频数据
                final TranslationMode mode = translationImpl.getTranslationMode();
                if (null == mode || mode.getMode() == TranslationMode.MODE_IDLE) return;
                //自行处理各种模式
                switch (mode.getMode()) {
                    case TranslationMode.MODE_IDLE: {
                        //空闲模式
                        break;
                    }
                    case TranslationMode.MODE_RECORD: {
                        //录音模式
                        break;
                    }
                    case TranslationMode.MODE_RECORDING_TRANSLATION: {
                        //录音翻译模式
                        break;
                    }
                    case TranslationMode.MODE_CALL_TRANSLATION: {
                        //通话翻译模式
                        break;
                    }
                    case TranslationMode.MODE_AUDIO_TRANSLATION: {
                        //音视频翻译模式
                        break;
                    }
                    case TranslationMode.MODE_FACE_TO_FACE_TRANSLATION: {
                        //面对面翻译模式
                        break;
                    }
                    case TranslationMode.MODE_CALL_TRANSLATION_WITH_STEREO: {
                        //通话翻译立体声模式
                        break;
                    }
                    case TranslationMode.MODE_CALL_RECORD: {
                        //通话录音模式
                        break;
                    }
                }
            }

            @Override
            public void onError(BluetoothDevice device, int code, String message) {
                //回调错误事件
            }
        });
    }


    private void destroyTranslationImpl() {
        if (null != translationImpl) {
            translationImpl.destroy();
            translationImpl = null;
        }
    }

    /**
     * 自定义AI云翻译功能实现
     */
    public static class CustomAITranslation implements IAITranslationApi {

        private AITranslationCallback translationCallback;

        /**
         * OPUS解码器
         */
        private OpusManager opusDecoder;

        /**
         * OPUS编码器
         */
        private OpusManager opusEncoder;

        /**
         * 是否正在翻译
         */
        private boolean isWorking = false;

        /**
         * 当前翻译模式信息
         */
        private TranslationMode currentMode;

        @Override
        public boolean isWorking() {
            return isWorking;
        }

        @Override
        public void startTranslating(@NonNull TranslationMode mode, @NonNull AITranslationCallback callback) {
            //根据翻译模式信息，进行AI云翻译任务
            currentMode = mode;
            translationCallback = callback;
            isWorking = true;
            initOpusEncoder();
            startOpusDecode();
        }

        @Override
        public void stopTranslating() {
            //停止翻译
            destroyOpusEncoder();
            stopOpusDecode();
            currentMode = null;
            translationCallback = null;
            isWorking = false;
        }

        @Override
        public void writeAudio(@NonNull AudioData audioData) {
            //需要根据音频来源进行翻译任务
            //比如：通话翻译，有上行数据 和下行数据
            final TranslationMode mode = currentMode;
            if (mode == null) return;
            if (mode.getMode() == TranslationMode.MODE_CALL_TRANSLATION) {
                if (audioData.getSource() == AudioData.SOURCE_E_SCO_UP_LINK) { //上行数据
                    //解码上行数据
                } else if (audioData.getSource() == AudioData.SOURCE_E_SCO_DOWN_LINK) { //下行数据
                    //解码下行数据
                }
            } else {
                //解码数据
            }
        }

        private void startOpusDecode() {
            if (null == opusDecoder) {
                try {
                    opusDecoder = new OpusManager();
                } catch (OpusException e) {
                    e.printStackTrace();
                }
            }
            final OpusManager decoder = opusDecoder;
            if (decoder != null) {
                if (decoder.isDecodeStream()) {
                    decoder.stopDecodeStream();
                }
                decoder.startDecodeStream(new OnDecodeStreamCallback() {
                    @Override
                    public void onDecodeStream(byte[] bytes) {
                        //解码成功(流式)
                        //把流式的PCN数据上传的AI云端，进行语义分析和翻译
                        //把翻译结果，TTS音频编码成OPUS数据，再通过 AITranslationCallback#onTranslateResult(result) 回调到SDK
                    }

                    @Override
                    public void onStart() {
                        //解码开始
                    }

                    @Override
                    public void onComplete(String path) {
                        //解码结束
                    }

                    @Override
                    public void onError(int code, String message) {
                        //解码失败
                    }
                });
            }
        }

        private void stopOpusDecode() {
            final OpusManager decoder = opusDecoder;
            if (null == decoder) return;
            if (decoder.isDecodeStream()) {
                decoder.stopDecodeStream();
            }
            opusDecoder = null;
        }

        private void initOpusEncoder() {
            if (null == opusEncoder) {
                try {
                    opusEncoder = new OpusManager();
                } catch (OpusException e) {
                    e.printStackTrace();
                }
            }
        }

        private void destroyOpusEncoder() {
            if (null != opusDecoder) {
                opusEncoder.release();
            }
            opusEncoder = null;
        }

        private void pushTranslationResult(byte[] ttsData) {
            final OpusManager encoder = opusEncoder;
            if (null == encoder) return;
            //保存TTS数据为TTS文件，略
            encoder.encodeFile(
                    "你的TTS文件保存路径(xxx.pcm)", //只接受PCM文件
                    "编码输出文件路径(xxx.opus)",
                    new OnEncodeStreamCallback() {
                        @Override
                        public void onEncodeStream(byte[] bytes) {
                            //回调编码数据(流式)
                        }

                        @Override
                        public void onStart() {
                            //回调编码开始
                        }

                        @Override
                        public void onComplete(String path) {
                            //回调编码成功
                            if (null == path) return;
                            byte[] opusData = readFileData(path);
                            if (opusData.length == 0) return;
                            //构造翻译结果数据
                            AudioData audioData = new AudioData(
                                    AudioData.SOURCE_DEVICE_MIC,
                                    Constants.AUDIO_TYPE_OPUS,
                                    opusData
                            );
                            //通知SDK下发数据
                            if (null != translationCallback) {
                                translationCallback.onTranslateResult(
                                        new TranslationResult()
                                                .setId(1) //翻译任务序号
                                                .setTranslationTTSData(audioData)
                                );
                            }

                        }

                        @Override
                        public void onError(int code, String message) {
                            //回调编码失败
                        }
                    });
        }

        private byte[] readFileData(String filePath) {
            byte[] output = new byte[0];
            try {
                InputStream input = new FileInputStream(filePath);
                output = new byte[input.available()];
                int size = input.read(output);
                input.close();
                return Arrays.copyOf(output, size);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return output;
        }
    }


}