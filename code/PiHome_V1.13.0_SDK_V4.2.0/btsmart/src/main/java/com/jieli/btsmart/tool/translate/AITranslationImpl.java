package com.jieli.btsmart.tool.translate;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.annotation.AudioSource;
import com.jieli.bluetooth.annotation.AudioType;
import com.jieli.bluetooth.bean.translation.AudioData;
import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.bluetooth.bean.translation.TranslationResult;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.interfaces.rcsp.translation.AITranslationCallback;
import com.jieli.bluetooth.interfaces.rcsp.translation.IAITranslationApi;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.data.model.translation.RoleInfo;
import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.tool.ai.doubao.translate.MachineTranslation;
import com.jieli.btsmart.tool.ai.doubao.translate.OnTranslateResultCallback;
import com.jieli.btsmart.tool.ai.doubao.translate.model.TranslateAudioParam;
import com.jieli.btsmart.tool.ai.doubao.translate.model.TranslateTextParam;
import com.jieli.btsmart.tool.ai.doubao.translate.model.language.Language;
import com.jieli.btsmart.tool.ai.doubao.translate.model.request.Configuration;
import com.jieli.btsmart.tool.translate.codec.AudioCodec;
import com.jieli.btsmart.tool.translate.codec.JLAV2Codec;
import com.jieli.btsmart.tool.translate.codec.OpusCodec;
import com.jieli.btsmart.tool.translate.codec.PcmCodec;
import com.jieli.btsmart.tool.translate.player.AudioPlayer;
import com.jieli.btsmart.util.PcmKit;
import com.jieli.jl_audio_decode.opus.model.OpusOption;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AITranslationImpl
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc AI云翻译实现
 * @since 2025/6/24
 */
public class AITranslationImpl implements IAITranslationApi {

    private static Context getContext() {
        return MainApplication.getApplication();
    }

    private final String tag = getClass().getSimpleName();
    /**
     * 设备地址
     */
    @NonNull
    private final String mac;
    /**
     * 工作状态
     */
    private TranslateState workState = TranslateState.STATE_IDLE;
    /**
     * AI云翻译事件回调
     */
    private TranslationStateCallback mTranslateCallback;
    /**
     * AI云翻译事件回调
     */
    private Wrapper mCallback;
    /**
     * 音频解码器
     */
    private AudioCodec mDecoder;
    /**
     * 第二个音频解码器
     */
    private AudioCodec mSecondDecoder;
    /**
     * 源语言
     */
    @NonNull
    private String srcLanguage = Language.LANG_ZH;
    /**
     * 翻译语言
     */
    @NonNull
    private final List<String> destLanguage;
    /**
     * 会议ID
     */
    private int sessionId;
    /**
     * 是否使用A2DP播报
     */
    private boolean isUseA2DP;
    /**
     * 任务线程池
     */
    private volatile ExecutorService mThreadPool;
    /**
     * 倒计时阻塞
     */
    private volatile CountDownLatch mDownLatch;
    /**
     * 文本翻译器
     */
    private volatile MachineTranslation textTranslation;

    public AITranslationImpl(@NonNull String mac) {
        this.mac = mac;
        destLanguage = new ArrayList<>();
        destLanguage.add(Language.LANG_EN);
    }

    @Override
    public boolean isWorking() {
        return workState == TranslateState.STATE_WORKING;
    }

    public boolean isPaused() {
        return workState == TranslateState.STATE_PAUSE;
    }

    @Override
    public void startTranslating(@NonNull TranslationMode mode, @NonNull AITranslationCallback callback) {
        this.mCallback = new Wrapper(isUseA2DP, mode, mTranslateCallback, callback);
        SystemClock.sleep(80);
        initDecoder(mode, this.mCallback);
        postTranslateState(TranslateState.STATE_WORKING);
        this.mCallback.postStartEvent();
    }

    @Override
    public void stopTranslating() {
        //停止云翻译流程
        releaseDecoder();
        AudioPlayer.getInstance().release();
        postTranslateState(TranslateState.STATE_IDLE);
        final Wrapper callback = mCallback;
        if (callback != null) {
            callback.postStopEvent(ErrorCode.ERR_NONE, "Stop Translating.");
            mCallback = null;
        }
    }

    @Override
    public void writeAudio(@NonNull AudioData audioData) {
        if (!isWorking() || null == mCallback) return;
        final int mode = mCallback.getMode().getMode();
        if ((mode == TranslationMode.MODE_CALL_TRANSLATION && audioData.getSource() == AudioData.SOURCE_E_SCO_DOWN_LINK)
                || (mode == TranslationMode.MODE_FACE_TO_FACE_TRANSLATION && audioData.getType() != Constants.AUDIO_TYPE_PCM)) {
            if (mSecondDecoder != null) {
                mSecondDecoder.writeAudioData(audioData);
            }
            return;
        }
        if (mDecoder != null) {
            mDecoder.writeAudioData(audioData);
        }
    }

    public void setTranslateCallback(TranslationStateCallback callback) {
        this.mTranslateCallback = callback;
    }

    public void updateLanguage(@NonNull String srcLanguage, @NonNull List<String> destLanguage) {
        JL_Log.d(tag, "updateLanguage", "srcLanguage : " + srcLanguage + ", destLanguage : " + destLanguage);
        this.srcLanguage = srcLanguage;
        if (!destLanguage.isEmpty()) {
            this.destLanguage.clear();
            this.destLanguage.addAll(destLanguage);
        }
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public void setUseA2DP(boolean useA2DP) {
        isUseA2DP = useA2DP;
    }

    /**
     * 暂停翻译
     */
    public boolean pauseTranslate() {
        if (!isWorking()) return false;
        //停止云翻译流程
        releaseDecoder();
        postTranslateState(TranslateState.STATE_PAUSE);
        return true;
    }

    /**
     * 恢复翻译
     */
    public boolean resumeTranslate() {
        if (!isPaused()) return false;
        final Wrapper wrapper = mCallback;
        if (null == wrapper) return false;
        initDecoder(wrapper.getMode(), wrapper);
        postTranslateState(TranslateState.STATE_WORKING);
        return true;
    }

    /**
     * 写入音频文件(默认是麦克风输入，PCM音频)
     *
     * @param roleInfo    RoleInfo 角色
     * @param pcmFilePath String 音频文件路径
     */
    public void writeAudioFile(@NonNull RoleInfo roleInfo, String pcmFilePath) {
        writeAudioFile(roleInfo, AudioData.SOURCE_PHONE_MIC, Constants.AUDIO_TYPE_PCM, pcmFilePath);
    }

    /**
     * 写入音频文件
     *
     * @param roleInfo RoleInfo 角色
     * @param source   int 音频来源
     * @param type     int 音频类型
     * @param filePath String 音频文件路径
     */
    public void writeAudioFile(@NonNull RoleInfo roleInfo, @AudioSource int source, @AudioType int type, String filePath) {
        if (null == mThreadPool || mThreadPool.isShutdown() || !isWorking()) {
            JL_Log.w(tag, "writeAudioFile", "No working");
            return;
        }
        final AudioCodec codec = getAudioCodec(roleInfo, source, type);
        if (null == codec) {
            JL_Log.w(tag, "writeAudioFile", "Not found audio codec.");
            return;
        }
        JL_Log.d(tag, "writeAudioFile", "start. file Path : " + filePath);
        mThreadPool.submit(() -> {
            mDownLatch = new CountDownLatch(1);
            JL_Log.d(tag, "writeAudioFile", "startDecodeStream");
            String srcLang = getSrcLanguageByRole(roleInfo);
            List<String> destLang = new ArrayList<>();
            destLang.add(getDestLanguageByRole(roleInfo));
            codec.startDecodeStream(new CustomAudioStreamCallback(mac, codec, roleInfo, srcLang, destLang,
                    sessionId, isUseA2DP, false, (null == mCallback ? 0 : mCallback.getRecordId()),
                    new OnTranslateResultCallback() {
                        @Override
                        public void onTranslateRecord(@NonNull TranslationRecord record) {
                            if (null != mCallback) {
                                mCallback.onTranslateRecord(record);
                            }
                        }

                        @Override
                        public void onStart() {
                            writeFileData(codec, source, type, filePath);
                        }

                        @Override
                        public void onTranslateResult(@NonNull TranslationResult result) {
                            if (null != mCallback) {
                                mCallback.onTranslateResult(result);
                            }
                        }

                        @Override
                        public void onTranslateError(long id, int code, String message) {
                            JL_Log.i(tag, "writeAudioFile", "onTranslateError --> " + code + ", " + message);
                            if (null != mCallback) {
                                mCallback.onTranslateError(id, code, message);
                            }
                        }

                        @Override
                        public void onStop(int reason, String message) {
                            JL_Log.i(tag, "writeAudioFile", "onStop --> " + reason + ", " + message);
                            if (null != mCallback) {
                                mCallback.onStop(reason, message);
                            }
                            if (mDownLatch != null) {
                                mDownLatch.countDown();
                            }
                        }
                    }));
            if (mDownLatch != null && mDownLatch.getCount() > 0) {
                try {
                    mDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mDownLatch = null;
            }
            JL_Log.d(tag, "writeAudioFile", "stop");
        });
    }

    /**
     * 开始设备录音解码
     *
     * @param roleInfo RoleInfo 角色
     * @return boolean 结果
     */
    public boolean startDeviceRecordDecoder(@NonNull RoleInfo roleInfo) {
        if (mSecondDecoder == null) return false;
        if (stopDeviceRecordDecoder()) {
            SystemClock.sleep(100);
        }
        String srcLang = getSrcLanguageByRole(roleInfo);
        List<String> destLang = new ArrayList<>();
        destLang.add(getDestLanguageByRole(roleInfo));
        mSecondDecoder.startDecodeStream(new CustomAudioStreamCallback(mac, mSecondDecoder,
                roleInfo, srcLang, destLang, sessionId, isUseA2DP, false, (null == mCallback ? 0 : mCallback.getRecordId()), mCallback));
        return true;
    }

    /**
     * 停止设备录音解码
     *
     * @return boolean 结果
     */
    public boolean stopDeviceRecordDecoder() {
        if (mSecondDecoder == null) return false;
        if (mSecondDecoder.isWorking()) {
            mSecondDecoder.stopDecodeStream();
            return true;
        }
        return false;
    }

    /**
     * 是否正在翻译文本
     *
     * @return boolean 结果
     */
    public boolean isTextTranslating() {
        return textTranslation != null && textTranslation.isWorking();
    }

    /**
     * 翻译文本
     *
     * @param roleInfo RoleInfo 角色
     * @param texts    List\<String\> 文本信息
     * @return boolean 操作结果
     */
    public boolean translateText(@NonNull RoleInfo roleInfo, @NonNull List<String> texts) {
        if (null == textTranslation) {
            textTranslation = new MachineTranslation();
            textTranslation.setSessionId(sessionId);
            textTranslation.setUseA2DP(isUseA2DP);
        }
        if (textTranslation.isWorking()) return false;
        String srcLang = getSrcLanguageByRole(roleInfo);
        String destLang = getDestLanguageByRole(roleInfo);
        JL_Log.d(tag, "translateText", "srcLang : " + srcLang + ", destLang : " + destLang);
        textTranslation.start(new TranslateTextParam(mac, roleInfo,
                (null == mCallback ? 0 : mCallback.getRecordId()),
                srcLang, destLang, texts), mCallback);
        return true;
    }

    private String getSrcLanguageByRole(@NonNull RoleInfo roleInfo) {
        String targetLang = destLanguage.isEmpty() ? Language.LANG_EN : destLanguage.get(0);
        return roleInfo.getRole() == RoleInfo.ROLE_DEVICE ? targetLang : srcLanguage;
    }

    private String getDestLanguageByRole(@NonNull RoleInfo roleInfo) {
        String targetLang = destLanguage.isEmpty() ? Language.LANG_EN : destLanguage.get(0);
        return roleInfo.getRole() == RoleInfo.ROLE_DEVICE ? srcLanguage : targetLang;
    }

    private AudioCodec getAudioCodec(@NonNull RoleInfo roleInfo, int source, int type) {
        final AudioCodec codec;
        if (roleInfo.getTranslationMode() == TranslationMode.MODE_FACE_TO_FACE_TRANSLATION) {
            if (type != Constants.AUDIO_TYPE_PCM) {
                codec = mSecondDecoder;
            } else {
                codec = mDecoder;
            }
        } else {
            if (source == AudioData.SOURCE_E_SCO_DOWN_LINK) {
                codec = mSecondDecoder;
            } else {
                codec = mDecoder;
            }
        }
        return codec;
    }

    private void writeFileData(@NonNull AudioCodec codec, @AudioSource int source, @AudioType int type, String filePath) {
        if (!codec.isWorking()) return;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buf = new byte[200 * 32];
            int bytesLeft = fis.available();
            while (bytesLeft > 0) {
                int readSize = fis.read(buf);
                if (readSize == -1) break;
                byte[] data = Arrays.copyOfRange(buf, 0, readSize);
                codec.writeAudioData(new AudioData(source, type, data));
                bytesLeft -= readSize;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        codec.stopDecodeStream();
    }

    private void initDecoder(@NonNull TranslationMode mode, @NonNull TranslationStateCallback callback) {
        releaseDecoder();
        JL_Log.d(tag, "initDecoder", "" + mode);
        final int translationMode = mode.getMode();
        switch (translationMode) {
            case TranslationMode.MODE_IDLE:
            case TranslationMode.MODE_RECORD: //不处理
                break;
            case TranslationMode.MODE_CALL_TRANSLATION:
            case TranslationMode.MODE_CALL_RECORD: {
                if (mode.getAudioType() == Constants.AUDIO_TYPE_OPUS) {
                    mDecoder = new OpusCodec();
                    if (translationMode == TranslationMode.MODE_CALL_TRANSLATION) {
                        mSecondDecoder = new OpusCodec();
                    }
                } else if (mode.getAudioType() == Constants.AUDIO_TYPE_JLA_V2) {
                    mDecoder = new JLAV2Codec();
                    mSecondDecoder = new JLAV2Codec();
                }
                if (mDecoder != null) {
                    OpusOption option = translationMode == TranslationMode.MODE_CALL_RECORD ?
                            new OpusOption().setHasHead(false).setChannel(2).setPacketSize(80) : new OpusOption();
                    mDecoder.startDecodeStream(option, new CustomAudioStreamCallback(mac, mDecoder,
                            new RoleInfo(translationMode, RoleInfo.ROLE_PHONE), srcLanguage, destLanguage,
                            sessionId, isUseA2DP, true,
                            (null == mCallback ? 0 : mCallback.getRecordId()), callback));
                }
                if (mSecondDecoder != null) {
                    RoleInfo deviceRole = new RoleInfo(translationMode, RoleInfo.ROLE_DEVICE);
                    String srcLang = getSrcLanguageByRole(deviceRole);
                    List<String> destLang = new ArrayList<>();
                    destLang.add(getDestLanguageByRole(deviceRole));
                    mSecondDecoder.startDecodeStream(new CustomAudioStreamCallback(mac, mSecondDecoder,
                            deviceRole, srcLang, destLang, sessionId, isUseA2DP, true,
                            (null == mCallback ? 0 : mCallback.getRecordId()), callback));
                }
                break;
            }
            case TranslationMode.MODE_FACE_TO_FACE_TRANSLATION:
                //创建任务线程池
                initThreadPool();
                mDecoder = new PcmCodec();
                if (mode.getAudioType() == Constants.AUDIO_TYPE_OPUS) {
                    mSecondDecoder = new OpusCodec();
                } else if (mode.getAudioType() == Constants.AUDIO_TYPE_JLA_V2) {
                    mSecondDecoder = new JLAV2Codec();
                }
                break;
            case TranslationMode.MODE_AUDIO_TRANSLATION:
            case TranslationMode.MODE_RECORDING_TRANSLATION: {
                if (mode.getRecordingStrategy() == TranslationMode.STRATEGY_CUSTOM_RECORDING) {
                    JL_Log.d(tag, "initDecoder", "APP Record");
                    mDecoder = new PcmCodec();
                } else {
                    if (mode.getAudioType() == Constants.AUDIO_TYPE_OPUS) {
                        mDecoder = new OpusCodec();
                    } else if (mode.getAudioType() == Constants.AUDIO_TYPE_JLA_V2) {
                        mDecoder = new JLAV2Codec();
                    } else if (mode.getAudioType() == Constants.AUDIO_TYPE_PCM) {
                        mDecoder = new PcmCodec();
                    }
                }
                if (mDecoder != null) {
                    mDecoder.startDecodeStream(new CustomAudioStreamCallback(mac, mDecoder,
                            new RoleInfo(translationMode, RoleInfo.ROLE_PHONE), srcLanguage, destLanguage,
                            sessionId, isUseA2DP, true,
                            (null == mCallback ? 0 : mCallback.getRecordId()), callback));
                }
                break;
            }
            case TranslationMode.MODE_CALL_TRANSLATION_WITH_STEREO: {
                //暂时只支持立体声OPUS格式
                if (mode.getAudioType() == Constants.AUDIO_TYPE_OPUS) {
                    mDecoder = new OpusCodec();
                }
                if (mDecoder != null) {
                    mDecoder.startDecodeStream(new OpusOption()
                            .setHasHead(false)
                            .setChannel(2)
                            .setPacketSize(80), new CustomAudioStreamCallback(mac, mDecoder,
                            new RoleInfo(translationMode, RoleInfo.ROLE_PHONE), srcLanguage, destLanguage,
                            sessionId, isUseA2DP, true,
                            (null == mCallback ? 0 : mCallback.getRecordId()), callback));
                }
            }
        }
    }

    private void releaseDecoder() {
        JL_Log.i(tag, "releaseDecoder", "");
        if (mDecoder != null) {
            mDecoder.release();
            mDecoder = null;
        }
        if (mSecondDecoder != null) {
            mSecondDecoder.release();
            mSecondDecoder = null;
        }
        releaseThreadPool();
    }

    private void initThreadPool() {
        releaseThreadPool();
        mThreadPool = Executors.newSingleThreadExecutor();
    }

    private void releaseThreadPool() {
        if (mThreadPool != null) {
            if (!mThreadPool.isShutdown()) {
                mThreadPool.shutdownNow();
            }
            mThreadPool = null;
        }
    }

    private void postTranslateState(TranslateState state) {
        if (workState == state) return;
        workState = state;
        if (workState == TranslateState.STATE_IDLE) {
            if (null != textTranslation) {
                if (textTranslation.isWorking()) {
                    textTranslation.stop();
                }
                textTranslation = null;
            }
        }
        final Wrapper callback = mCallback;
        if (callback != null) {
            callback.onTranslateState(state);
        }
    }

    /**
     * 自定义编解码回调
     */
    private static class CustomAudioStreamCallback implements AudioCodec.OnAudioStreamCallback {
        /**
         * 设备地址
         */
        @NonNull
        private final String mac;
        /**
         * 解码器
         */
        @NonNull
        private final AudioCodec decoder;
        /**
         * 角色信息
         */
        @NonNull
        private final RoleInfo roleInfo;
        /**
         * 源语言
         */
        private final String srcLanguage;
        /**
         * 目标语言
         */
        private final List<String> destLanguage;
        /**
         * AI云翻译回调
         */
        private final OnTranslateResultCallback callback;
        /**
         * 记录ID是否自增
         */
        private final boolean isIdAutoInc;
        /**
         * 记录ID
         */
        private final int recordId;
        /**
         * 机器翻译
         */
        private MachineTranslation aiTranslation;
        /**
         * 第二声道机器翻译
         */
        private MachineTranslation secondAiTranslation;


        public CustomAudioStreamCallback(@NonNull String mac, @NonNull AudioCodec decoder, @NonNull RoleInfo roleInfo,
                                         String srcLanguage, List<String> destLanguage, int sessionId, boolean isUseA2DP,
                                         boolean isIdAutoInc, int recordId, OnTranslateResultCallback callback) {
            this.mac = mac;
            this.decoder = decoder;
            this.roleInfo = roleInfo;
            this.srcLanguage = srcLanguage;
            this.destLanguage = destLanguage;
            this.isIdAutoInc = isIdAutoInc;
            this.recordId = recordId;
            this.callback = callback;
            initAITranslation(sessionId, isUseA2DP);
        }

        @Override
        public void onStart(int type) {
            startAITranslate(type);
        }

        @Override
        public void onStop(int type, String result) {
            stopAiTranslation(ErrorCode.ERR_NONE, "");
        }

        @Override
        public void onError(int type, int code, String message) {
            stopAiTranslation(code, message);
        }

        @Override
        public void onStream(int srcType, int audioType, byte[] data) {
            if (srcType == getAudioType()) {
                handleDecodedData(audioType, data);
            }
        }

        @TranslationMode.Mode
        private int getTranslationMode() {
            return roleInfo.getTranslationMode();
        }

        private int getAudioType() {
            return decoder.getAudioType();
        }

        private void initAITranslation(int sessionId, boolean isUseA2DP) {
            final int mode = getTranslationMode();
            if (mode == TranslationMode.MODE_RECORD || mode == TranslationMode.MODE_CALL_RECORD) {
                //录音功能，不初始化AI翻译服务
                return;
            }
            aiTranslation = new MachineTranslation();
            aiTranslation.setSessionId(sessionId);
            aiTranslation.setUseA2DP(isUseA2DP);

            //通话翻译立体声模式，初始化第二声道AI翻译服务
            if (mode == TranslationMode.MODE_CALL_TRANSLATION_WITH_STEREO) {
                secondAiTranslation = new MachineTranslation();
                secondAiTranslation.setSessionId(sessionId);
                secondAiTranslation.setUseA2DP(isUseA2DP);
            }
        }

        private void startAITranslate(int audioType) {
            final OnTranslateResultCallback translateResultCallback = new OnTranslateResultCallback() {
                /**
                 * 失败重试次数
                 */
                private int count;

                @Override
                public void onTranslateRecord(@NonNull TranslationRecord record) {
                    if (null != callback) callback.onTranslateRecord(record);
                }

                @Override
                public void onStart() {
                    if (null != callback) callback.onStart();
                }

                @Override
                public void onTranslateResult(@NonNull TranslationResult result) {
                    if (null != callback) callback.onTranslateResult(result);
                }

                @Override
                public void onTranslateError(long id, int code, String message) {
                    if (null != callback) callback.onTranslateError(id, code, message);
                }

                @Override
                public void onStop(int reason, String message) {
                    if (decoder.isWorking()) {
                        count++;
                        if (count < 3) {
                            CustomAudioStreamCallback.this.onStart(getAudioType());
                            return;
                        }
                        if (reason == 0) {
                            CustomAudioStreamCallback.this.onStop(getAudioType(), message);
                        } else {
                            CustomAudioStreamCallback.this.onError(getAudioType(), reason, message);
                        }
                        return;
                    }
                    if (null != callback) callback.onStop(reason, message);
                }
            };
            if (aiTranslation != null && !aiTranslation.isWorking()) {
                aiTranslation.start(new TranslateAudioParam(mac, roleInfo, isIdAutoInc, recordId,
                        new Configuration(srcLanguage, destLanguage), audioType), translateResultCallback);
            }
            if (secondAiTranslation != null && !secondAiTranslation.isWorking()) {
                //第二个角色，就是第二声道的角色，下行数据，就是远端用户
                RoleInfo secondRole = new RoleInfo(roleInfo.getTranslationMode(), roleInfo.getRole() == RoleInfo.ROLE_DEVICE ?
                        RoleInfo.ROLE_PHONE : RoleInfo.ROLE_DEVICE);
                //角色对调，翻译语种也要对调
                String sourceLang = destLanguage.get(0);
                List<String> targetLang = new ArrayList<>();
                targetLang.add(srcLanguage);
                secondAiTranslation.start(new TranslateAudioParam(mac, secondRole, isIdAutoInc, recordId,
                        new Configuration(sourceLang, targetLang), audioType), translateResultCallback);
            }
        }

        private void stopAiTranslation(int code, String message) {
            if (aiTranslation != null) {
                if (code != ErrorCode.ERR_NONE) { //失败，就回调错误码
                    aiTranslation.callbackTranslationStop(code, message);
                }
                aiTranslation.stop();
                aiTranslation = null;
            }
            if (secondAiTranslation != null) {
                if (code != ErrorCode.ERR_NONE) { //失败，就回调错误码
                    secondAiTranslation.callbackTranslationStop(code, message);
                }
                secondAiTranslation.stop();
                secondAiTranslation = null;
            }
        }

        private void handleDecodedData(int audioType, byte[] data) {
            if (null == data || data.length == 0) return;
            final AudioData audioData = decoder.getLastAudioData();
            int source = audioData == null ? AudioData.SOURCE_PHONE_MIC : audioData.getSource();
            if (source == AudioData.SOURCE_E_SCO_MIX) { //如果是混合上下行的立体声，说明是通话翻译立体声模式
                byte[][] dataArray = PcmKit.splitStereoPcmData(data);
                //第一个是左声道，固定是上行数据
                byte[] leftPcm = dataArray[0];
                //第二个是右声道，固定是下行数据
                byte[] rightPcm = dataArray[1];
                if (leftPcm.length > 0) {
                    if (aiTranslation != null && aiTranslation.isWorking()) {
                        aiTranslation.writeAudio(AudioData.SOURCE_E_SCO_UP_LINK, leftPcm);
                    }
                }
                if (rightPcm.length > 0) {
                    if (secondAiTranslation != null && secondAiTranslation.isWorking()) {
                        secondAiTranslation.writeAudio(AudioData.SOURCE_E_SCO_DOWN_LINK, rightPcm);
                    }
                }
                return;
            }
            if (aiTranslation != null && aiTranslation.isWorking()) {
                aiTranslation.writeAudio(source, data);
            }
        }
    }

    private static class Wrapper implements TranslationStateCallback {
        private final boolean isUseA2DP;
        @NonNull
        private final TranslationMode mode;
        private final TranslationStateCallback globalCallback;
        private final AITranslationCallback callback;
        /**
         * 记录ID
         */
        private int recordId;

        public Wrapper(boolean isUseA2DP, @NonNull TranslationMode mode, TranslationStateCallback globalCallback,
                       AITranslationCallback callback) {
            this.isUseA2DP = isUseA2DP;
            this.mode = mode;
            this.globalCallback = globalCallback;
            this.callback = callback;
            recordId = new Random().nextInt(256);
        }

        @NonNull
        public TranslationMode getMode() {
            return mode;
        }

        public int getRecordId() {
            final int id = recordId;
            JL_Log.d(AITranslationImpl.class.getSimpleName(), "getRecordId", "" + id);
            autoInc();
            return id;
        }

        @Override
        public void onStart() {

        }

        @Override
        public void onTranslateResult(@NonNull TranslationResult result) {
            if (!isUseA2DP && result.getTranslationTTSData() != null && null != callback) {
                callback.onTranslateResult(result);
            }
            if (null != globalCallback) {
                globalCallback.onTranslateResult(result);
            }
        }

        @Override
        public void onTranslateError(long id, int code, String message) {
            autoInc();
            if (null != callback) {
                callback.onTranslateError(id, code, message);
            }
            if (null != globalCallback) {
                globalCallback.onTranslateError(id, code, message);
            }
        }

        @Override
        public void onStop(int reason, String message) {
            autoInc();
            if (reason != ErrorCode.ERR_NONE) {
                onTranslateError(recordId, reason, message);
            }
        }

        @Override
        public void onTranslateState(TranslateState state) {
            if (null != globalCallback) {
                globalCallback.onTranslateState(state);
            }
        }

        @Override
        public void onTranslateRecord(@NonNull TranslationRecord record) {
            autoInc();
            if (null != globalCallback) {
                globalCallback.onTranslateRecord(record);
            }
        }

        public void postStartEvent() {
            if (null != callback) {
                callback.onStart();
            }
            if (null != globalCallback) {
                globalCallback.onStart();
            }
        }

        public void postStopEvent(int reason, String message) {
            if (null != callback) {
                callback.onStop(reason, message);
            }
            if (null != globalCallback) {
                globalCallback.onStop(reason, message);
            }
        }

        private void autoInc() {
            int id = recordId;
            id++;
            id = id % 65536;
            recordId = id;
            JL_Log.d(AITranslationImpl.class.getSimpleName(), "autoInc", "" + recordId);
        }
    }
}
