package com.jieli.btsmart.tool.ai.doubao.translate;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonSyntaxException;
import com.jieli.bluetooth.annotation.AudioSource;
import com.jieli.bluetooth.bean.translation.AudioData;
import com.jieli.bluetooth.bean.translation.TranslationResult;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.data.model.basic.OpResult;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.translation.RoleInfo;
import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.data.model.translation.ai_auth.AIAuthMessage;
import com.jieli.btsmart.data.model.translation.ai_auth.DoubaoTranslationMessage;
import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;
import com.jieli.btsmart.tool.ai.doubao.basic.WebSocketClient;
import com.jieli.btsmart.tool.ai.doubao.translate.auth.SignSpeechTranslate;
import com.jieli.btsmart.tool.ai.doubao.translate.model.TranslateAudioParam;
import com.jieli.btsmart.tool.ai.doubao.translate.model.TranslateParam;
import com.jieli.btsmart.tool.ai.doubao.translate.model.TranslateTextParam;
import com.jieli.btsmart.tool.ai.doubao.translate.model.request.Configuration;
import com.jieli.btsmart.tool.ai.doubao.translate.model.request.StopTranslateRequest;
import com.jieli.btsmart.tool.ai.doubao.translate.model.request.TranslateConfigRequest;
import com.jieli.btsmart.tool.ai.doubao.translate.model.request.TranslateDataRequest;
import com.jieli.btsmart.tool.ai.doubao.translate.model.request.TranslateTextRequest;
import com.jieli.btsmart.tool.ai.doubao.translate.model.response.Error;
import com.jieli.btsmart.tool.ai.doubao.translate.model.response.ResponseMetaData;
import com.jieli.btsmart.tool.ai.doubao.translate.model.response.Subtitle;
import com.jieli.btsmart.tool.ai.doubao.translate.model.response.TextTranslation;
import com.jieli.btsmart.tool.ai.doubao.translate.model.response.TextTranslationResponse;
import com.jieli.btsmart.tool.ai.doubao.translate.model.response.TranslationResponse;
import com.jieli.btsmart.tool.ai.doubao.tts.OnTtsResultCallback;
import com.jieli.btsmart.tool.ai.doubao.tts.TtsManager;
import com.jieli.btsmart.tool.configure.ConfigureKit;
import com.jieli.btsmart.tool.room.repository.TranslationRepository;
import com.jieli.btsmart.util.TranslateUtil;
import com.jieli.btsmart.util.WavUtil;
import com.jieli.component.utils.FileUtil;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * MachineTranslation
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 豆包机器翻译
 * @since 2025/6/24
 */
public class MachineTranslation {

    private final String tag = getClass().getSimpleName();

    /**
     * WebSocket客户端
     */
    private final WebSocketClient mWebSocketClient;
    /**
     * 工作状态
     */
    private final StateResult<Wrapper> mWorkState;
    /**
     * 工作线程池
     */
    private ExecutorService mWorkPool;
    /**
     * 音频来源
     */
    @AudioSource
    private int audioSource = AudioData.SOURCE_PHONE_MIC;
    /**
     * 翻译结果
     */
    private TranslationResult mTranslationResult;
    /**
     * 是否用户停止翻译
     */
    private boolean isUseStop;
    /**
     * 会议记录ID
     */
    private int sessionId;
    /**
     * 是否A2DP播报
     */
    private boolean isUseA2DP;
    /**
     * 倒计时阻塞
     */
    private CountDownLatch countDownLatch;
    /**
     * UI处理线程
     */
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();

    public MachineTranslation() {
        mWebSocketClient = new WebSocketClient();
        mWorkState = new StateResult<>(AIConfig.OP_TRANSLATE);
    }

    public boolean isWorking() {
        return mWorkState.getState() == StateResult.STATE_WORKING;
    }

    public void start(@NonNull TranslateParam param, @NonNull OnTranslateResultCallback callback) {
        if (isWorking()) {
            int code = ErrorCode.SUB_ERR_OPERATION_IN_PROGRESS;
            callbackTranslationStop(callback, code, ErrorCode.code2Msg(code));
            return;
        }
        final AIAuthMessage authMessage = ConfigureKit.getInstance().getAIAuthMessage();
        final DoubaoTranslationMessage translationMessage = null == authMessage ? null : authMessage.getDoubaoTranslationMessage();
        if (null == translationMessage || !authMessage.isValid()) {
            String msg = null == translationMessage ? "NO Doubao Translation Message" : "Auth Message is expired.";
            callbackTranslationStop(callback, ErrorCode.SUB_ERR_PARAMETER, msg);
            return;
        }
        String accessKey = translationMessage.getAccessKey();
        String secretKey = translationMessage.getSecretKey();
        if (TextUtils.isEmpty(accessKey) || TextUtils.isEmpty(secretKey)) {
            callbackTranslationStop(callback, ErrorCode.SUB_ERR_PARAMETER, "accessKey or secretKey is illegal.");
            return;
        }
        TtsManager.getInstance().setStateCallback(state -> {
            if (state == TtsManager.STATE_IDLE) {
                tryToSaveRecord();
            }
        });
        switch (param.getTranslationWay()) {
            case TranslateParam.WAY_AUDIO_STREAM: {
                translateAudioStream(accessKey, secretKey, (TranslateAudioParam) param, callback);
                return;
            }
            case TranslateParam.WAY_TEXT: {
                translateText(accessKey, secretKey, (TranslateTextParam) param, callback);
                return;
            }
            default: {
                callbackTranslationStop(callback, ErrorCode.SUB_ERR_UNSUPPORTED_FUNCTION, "unknown translation way : " + param.getTranslationWay());
            }
        }
    }

    public boolean writeAudio(int source, byte[] data) {
        if (!isWorking() || null == data || data.length == 0) return false;
        int translationWay = getTranslationWay();
        if (translationWay != TranslateParam.WAY_AUDIO_STREAM) return false;
        audioSource = source; //设置音频来源
        return mWebSocketClient.sendMessage(new TranslateDataRequest(Base64.encodeToString(data, Base64.DEFAULT)).toString());
    }

    public void stop() {
        if (isWorking()) {
            isUseStop = true;
        }
        int translationWay = getTranslationWay();
        JL_Log.d(tag, "stop", "translationWay : " + translationWay + ", isWorking : " + isWorking());
        if (translationWay == TranslateParam.WAY_AUDIO_STREAM) {
            if (mWebSocketClient.sendMessage(new StopTranslateRequest().toString())) {
                return;
            }
            mWebSocketClient.stop();
        } else if (translationWay == TranslateParam.WAY_TEXT) {
            callbackTranslationStop(getCallback(), ErrorCode.ERR_NONE, "User stops translating.");
        }
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public void setUseA2DP(boolean useA2DP) {
        isUseA2DP = useA2DP;
    }

    public void callbackTranslationStop(int code, String message) {
        callbackTranslationStop(getCallback(), code, message);
    }

    private void translateAudioStream(@NonNull String accessKey, @NonNull String secretKey, @NonNull TranslateAudioParam param,
                                      @NonNull OnTranslateResultCallback callback) {
        SignSpeechTranslate signSpeechTranslate = new SignSpeechTranslate(AIConfig.REGION,
                AIConfig.SERVICE, AIConfig.HOST, AIConfig.PATH, accessKey, secretKey);
        try {
            String signUrl = signSpeechTranslate.getSignUrl(AIConfig.API, AIConfig.VERSION, new byte[0]);
            URI url = new URI(CommonUtil.formatString("wss://%s%s?%s", AIConfig.HOST, AIConfig.PATH, signUrl));
            mWorkState.setData(new Wrapper(param, callback));
            isUseStop = false;
            mWebSocketClient.start(new Request.Builder().url(url.toString())
                    .header("Accept", "application/json")
                    .build(), new WebSocketListener() {
                @Override
                public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                    if (TtsManager.getInstance().isCreateTts()) return;
                    if (isUseStop || code == 1000) {
                        tryToSaveRecord();
                    }
                    callbackTranslationStop(getCallback(), code, reason);
                }

                @Override
                public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                    callbackTranslationStop(getCallback(), ErrorCode.SUB_ERR_IO_EXCEPTION, "IO Exception : " + t);
                }

                @Override
                public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                    mWebSocketClient.sendMessage(new TranslateConfigRequest(getConfiguration()).toString());
                    callbackWorking(getCallback());
                }

                @Override
                public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                    handleData(bytes.utf8());
                }
            });
        } catch (Exception e) {
            callbackTranslationStop(callback, ErrorCode.SUB_ERR_IO_EXCEPTION, "Exception : " + e);
        }
    }

    private void translateText(@NonNull String accessKey, @NonNull String secretKey, @NonNull TranslateTextParam param,
                               @NonNull OnTranslateResultCallback callback) {
        final List<String> textList = param.getSrcTextList();
        if (textList.isEmpty()) {
            callbackTranslationStop(callback, ErrorCode.SUB_ERR_PARAMETER, "Text is empty.");
            return;
        }
        mWorkState.setData(new Wrapper(param, callback));
        callbackWorking(callback);
        JL_Log.d(tag, "translateText", "callbackWorking");
        mWorkPool.submit(() -> {
            JL_Log.d(tag, "translateText", "start Task.");
            final OpResult<TextTranslationResponse> httpResult = translateTextByUrl(accessKey, secretKey, param);
            JL_Log.d(tag, "translateText", "httpResult : " + httpResult);
            final TextTranslationResponse response = httpResult.getData();
            if (!httpResult.isSuccess() || null == response) {
                callbackTranslationStop(callback, httpResult.getCode(), httpResult.getMessage());
                return;
            }
            final ResponseMetaData metaData = response.getResponseMetadata();
            if (null != metaData && null != metaData.getError()) {
                final Error error = metaData.getError();
                callbackTranslationStop(callback, ErrorCode.SUB_ERR_OP_FAILED, CommonUtil.formatString("code : %s, %s",
                        error.getCode(), error.getMessage()));
                return;
            }
            final List<TextTranslation> translations = response.getTranslationList();
            if (null == translations || translations.isEmpty()) {
                callbackTranslationStop(callback, ErrorCode.SUB_ERR_RESPONSE_BAD_RESULT, "No translation");
                return;
            }
            try {
                final Wrapper wrapper = mWorkState.getData();
                if (null == wrapper || !isWorking()) {
                    JL_Log.w(tag, "translateText", "It is idle state.");
                    return;
                }
                int count = translations.size();
                countDownLatch = new CountDownLatch(count);
                for (int i = 0; i < count; i++) {
                    String srcText = textList.get(i);
                    TextTranslation translation = translations.get(i);
                    TranslationResult translationResult = new TranslationResult()
                            .setId(param.getRoleInfo().getTranslationMode())
                            .setNlpText(srcText)
                            .setSrcLanguage(translation.getSourceLanguage())
                            .setDestLanguage(param.getTargetLanguage())
                            .setTranslationText(translation.getTranslation());
                    tryToTTS(wrapper, translationResult);
                }
                if (null != countDownLatch) {
                    if (countDownLatch.getCount() > 0) {
                        countDownLatch.await();
                    }
                    countDownLatch = null;
                }
                callbackTranslationStop(wrapper.getCallback(), ErrorCode.ERR_NONE, "Text translation completed.");
            } catch (Exception e) {
                callbackTranslationStop(getCallback(), ErrorCode.SUB_ERR_IO_EXCEPTION, "Exception : " + e);
            }
        });
    }

    private void callbackWorking(OnTranslateResultCallback callback) {
        if (null == callback) return;
        int state = mWorkState.getState();
        if (state == StateResult.STATE_WORKING) {
            JL_Log.d(tag, "callbackWorking", "same state : " + state);
            return;
        }
        mWorkState.setState(StateResult.STATE_WORKING).setCode(0).setProgress(0);
        final int translationWay = getTranslationWay();
        JL_Log.d(tag, "callbackWorking", "translationWay : " + translationWay);
        if (translationWay == TranslateParam.WAY_TEXT) {
            if (null == mWorkPool || mWorkPool.isShutdown()) {
                mWorkPool = Executors.newSingleThreadExecutor();
            }
        }
        callback.onStart();
    }

    private void callbackTranslationResult(OnTranslateResultCallback callback, TranslationResult result) {
        if (null == callback) return;
        int state = mWorkState.getState();
        if (state != StateResult.STATE_WORKING) return;
        callback.onTranslateResult(result);
    }

    private void callbackTranslationRecord(OnTranslateResultCallback callback, TranslationRecord record) {
        if (null == callback) return;
        int state = mWorkState.getState();
        if (state != StateResult.STATE_WORKING) return;
        callback.onTranslateRecord(record);
    }

    private void callbackTranslationError(OnTranslateResultCallback callback, long id, int code, String message) {
        if (null == callback) return;
        int state = mWorkState.getState();
        if (state != StateResult.STATE_WORKING) return;
        JL_Log.w(tag, "callbackTranslationError", "code : " + CommonUtil.formatInt(code) + ", " + message);
        callback.onTranslateError(id, code, message);
    }

    private void callbackTranslationStop(OnTranslateResultCallback callback, int code, String message) {
        int state = mWorkState.getState();
        if (state == StateResult.STATE_FINISH) return;
        if (isUseStop) {
            code = ErrorCode.ERR_NONE;
            message = "User stops translating.";
            isUseStop = false;
        }
        JL_Log.i(tag, "callbackTranslationStop", "code : " + CommonUtil.formatInt(code) + ", " + message);
        mWorkState.setState(StateResult.STATE_FINISH);
        TtsManager.getInstance().stop();
        final int translationWay = getTranslationWay();
        if (translationWay == TranslateParam.WAY_TEXT) {
            if (null != mWorkPool && !mWorkPool.isShutdown()) {
                mWorkPool.shutdownNow();
                mWorkPool = null;
            }
        }
        if (null != callback) {
            callback.onStop(code, message);
        }
    }

    private OnTranslateResultCallback getCallback() {
        final Wrapper wrapper = mWorkState.getData();
        if (null == wrapper) return null;
        return wrapper.getCallback();
    }

    private int getTranslationWay() {
        final Wrapper wrapper = mWorkState.getData();
        if (null == wrapper) return -1;
        return wrapper.getParam().getTranslationWay();
    }

    private Configuration getConfiguration() {
        final Wrapper wrapper = mWorkState.getData();
        if (null == wrapper) return null;
        TranslateParam param = wrapper.getParam();
        if (!(param instanceof TranslateAudioParam)) return null;
        return ((TranslateAudioParam) param).getConfiguration();
    }

    private void handleData(String data) {
        if (!isWorking() || null == data || data.isEmpty()) return;
        final Wrapper wrapper = mWorkState.getData();
        if (null == wrapper) return;
        TranslateParam param = wrapper.getParam();
        if (!(param instanceof TranslateAudioParam)) return;
        TranslateAudioParam audioParam = (TranslateAudioParam) param;
        final Configuration configuration = audioParam.getConfiguration();
        final OnTranslateResultCallback callback = wrapper.getCallback();
        final RoleInfo roleInfo = wrapper.getRoleInfo();
        try {
            TranslationResponse response = AIConfig.gson.fromJson(data, TranslationResponse.class);
            JL_Log.d(tag, "handleData", "roleInfo : " + roleInfo + ", json : " + data);
            ResponseMetaData responseMetaData = response.getResponseMetaData();
            final Error error = null == responseMetaData ? null : responseMetaData.getError();
            if (error != null) {
                //-500 INTERNAL_ERROR 没有翻译内容，跳过这个错误
                if (!TextUtils.equals(error.getCode(), "-500") || !TextUtils.equals(error.getMessage(), "INTERNAL_ERROR")) {
                    callbackTranslationError(callback, audioParam.getRecordId(), ErrorCode.SUB_ERR_OP_FAILED, error.toString());
                }
                return;
            }
            Subtitle subtitle = response.getSubtitle();
            if (subtitle == null) return;
            if (subtitle.getDefinite() == Boolean.TRUE) { //断句完成
                if (null == mTranslationResult) {
                    mTranslationResult = new TranslationResult();
                    mTranslationResult.setId(roleInfo.getTranslationMode());
                }
                if (TextUtils.equals(subtitle.getLanguage(), configuration.getSourceLanguage())) {
                    mTranslationResult.setSrcLanguage(subtitle.getLanguage())
                            .setNlpText(subtitle.getText());
                } else { //翻译文本
                    mTranslationResult.setDestLanguage(subtitle.getLanguage())
                            .setTranslationText(subtitle.getText());

                    mTranslationResult.setTranslationTTSData(new AudioData(audioSource, audioParam.getAudioType(), new byte[0]));
                    tryToTTS(wrapper, mTranslationResult.cloneObject());
                }
            }
        } catch (JsonSyntaxException e) {
            callbackTranslationError(callback, audioParam.getRecordId(), ErrorCode.SUB_ERR_PARSE_DATA, "Failed to parse json. " + e);
        }
    }

    private TranslationRecord buildRecord(@NonNull Wrapper wrapper, @NonNull TranslationResult result, String[] filePaths) {
        TranslationRecord record = new TranslationRecord();
        record.setMac(wrapper.getMac());
        record.setSessionId(sessionId);
        RoleInfo roleInfo = wrapper.getRoleInfo();
        record.setRole(roleInfo.getRole());
        record.setNikeName(roleInfo.getRoleName(MainApplication.getApplication()));
        record.setSrcText(result.getNlpText());
        record.setSrcLanguage(result.getSrcLanguage());
        if (null != filePaths && filePaths.length >= 2) {
            String srcFilePath = filePaths[1];
            record.setSrcFilePath(srcFilePath);
            record.setSrcFileDuration(WavUtil.getWavDuration(srcFilePath));
        }
        record.setDestText(result.getTranslationText());
        record.setDestLanguage(result.getDestLanguage());
        if (null != filePaths && filePaths.length >= 1) {
            String destFilePath = filePaths[0];
            record.setDestFilePath(destFilePath);
            record.setDestFileDuration(WavUtil.getWavDuration(destFilePath));
        }
        record.setUpdateTime(TranslateUtil.currentTime());
        return record;
    }

    private void tryToTTS(@NonNull Wrapper wrapper, @NonNull TranslationResult result) {
        final OnTtsResultCallback callback = new OnTtsResultCallback() {

            @Override
            public void onStart() {

            }

            @Override
            public void onStop(TranslationResult result, String[] filePaths) {
                if (!isWorking()) {
                    JL_Log.i(tag, "tryToTTS", "Not Working.");
                    handleTtsStopEvent();
                    return;
                }
                final Wrapper mWrapper = mWorkState.getData();
                if (null == mWrapper) return;
                final TranslateParam param = mWrapper.getParam();
                boolean isSameRecord = !param.isIdAutoInc();
                JL_Log.d(tag, "tryToTTS", "onStop --->  " + result);
                TranslationRecord record = buildRecord(mWrapper, result, filePaths);
                if (null == filePaths || filePaths.length == 0) {
                    record.setId(param.getNextRecordId());
                    if (isSameRecord && param.getTranslationRecord() != null) {
                        //合并翻译内容
                        record = mergeTranslationRecord(param.getTranslationRecord(), record, false);
                        JL_Log.d(tag, "tryToTTS", "mergeTranslationRecord --->  before. " + record);
                    }
                    callbackTranslationResult(getCallback(), result);
                    callbackTranslationRecord(getCallback(), record);
                    return;
                }

                if (isSameRecord && param.getTranslationRecord() != null) {
                    record.setId(param.getNextRecordId());
                    //合并翻译内容, 合并音频需要放在子线程操作
                    if (!threadPool.isShutdown()) {
                        final TranslationRecord newRecord = record;
                        threadPool.submit(() -> {
                            final Wrapper wrapper1 = mWorkState.getData();
                            if (null == wrapper1) return;
                            final TranslateParam param1 = wrapper1.getParam();
                            TranslationRecord target = mergeTranslationRecord(param1.getTranslationRecord(), newRecord, true);
                            JL_Log.d(tag, "tryToTTS", "mergeTranslationRecord --> " + target);
                            param1.setTranslationRecord(target);
                            uiHandler.post(() -> tryToSaveRecord());
                        });
                        return;
                    }
                    record = mergeTranslationRecord(param.getTranslationRecord(), record, true);
                    JL_Log.d(tag, "tryToTTS", "mergeTranslationRecord --> " + record);
                }
                param.setTranslationRecord(record);
                tryToSaveRecord();
            }

            @Override
            public void onError(TranslationResult result, int code, String message) {
                callbackTranslationError(getCallback(), wrapper.getParam().getRecordId(), code, message);
                handleTtsStopEvent();
            }
        };
        boolean ret = TtsManager.getInstance().addTask(wrapper.getMac(), wrapper.getRoleInfo(), isUseA2DP, result, callback);
        JL_Log.d(tag, "tryToTTS", "addTask ---> " + ret);
        if (!ret) {
            int code = ErrorCode.SUB_ERR_OP_FAILED;
            callback.onError(result, code, ErrorCode.code2Msg(code));
        }
    }

    private void tryToSaveRecord() {
        final Wrapper wrapper = mWorkState.getData();
        if (null == wrapper) return;
        final TranslateParam param = wrapper.getParam();
        final TranslationRecord record = param.getTranslationRecord();
        if (null == record || !record.isValidRecord() || param.isSaveRecord()) return;
        boolean isIdleState = TtsManager.getInstance().isIdleState();
        JL_Log.d(tag, "tryToSaveRecord", param
                + ", \n isConnected : " + mWebSocketClient.isConnected() + ", isIdleState : " + isIdleState);
        if (param.isIdAutoInc() || param.getTranslationWay() == TranslateParam.WAY_TEXT ||
                (!mWebSocketClient.isConnected() && isIdleState)) {
            param.setSaveRecordId(record.getId());
            TranslationRepository.getInstance().addTranslationRecord(record, id -> {
                JL_Log.d(tag, "tryToSaveRecord", "insert record success. id = " + id + ", sessionId : " + sessionId
                        + ", record id : " + record.getId());
                if (record.getId() != id) {
                    record.setId(id);
                }
                param.setSaveRecordId(id);
                handleTtsStopEvent();
            });
        }
    }

    private void handleTtsStopEvent() {
        final int translationWay = getTranslationWay();
        if (translationWay == TranslateParam.WAY_AUDIO_STREAM) {
            if (!mWebSocketClient.isConnected() && TtsManager.getInstance().isIdleState()) {
                callbackTranslationStop(getCallback(), ErrorCode.ERR_NONE, "Success");
            }
        } else if (translationWay == TranslateParam.WAY_TEXT) {
            if (countDownLatch != null && countDownLatch.getCount() > 0) {
                countDownLatch.countDown();
            }
        }
    }

    private TranslationRecord mergeTranslationRecord(@NonNull TranslationRecord cacheRecord, @NonNull TranslationRecord newRecord, boolean isMergeAudio) {
        JL_Log.d(tag, "mergeTranslationRecord", "cacheRecord : " + cacheRecord
                + ",\nnewRecord : " + newRecord
                + ",\nisMergeAudio : " + isMergeAudio + ", thread : " + Thread.currentThread().getName());
        TranslationRecord record = newRecord.cloneObject();
        String cacheSrcText = cacheRecord.getSrcText();
        String newSrcText = newRecord.getSrcText();
        if (!TextUtils.equals(cacheSrcText, newSrcText)) {
            record.setSrcText(cacheSrcText + " " + newSrcText);
            if (isMergeAudio) {
                String outputFilePath = cacheRecord.getSrcFilePath();
                String newWavFilePath = newRecord.getSrcFilePath();
                if (!TextUtils.isEmpty(outputFilePath) && !TextUtils.isEmpty(newWavFilePath)) {
                    if (WavUtil.mergeWavFiles(outputFilePath, newWavFilePath)) {
                        record.setSrcFilePath(outputFilePath);
                        record.setSrcFileDuration(WavUtil.getWavDuration(outputFilePath));
                        uiHandler.postDelayed(() -> { //过早删除文件会导致音频播放有问题
                            FileUtil.deleteFile(new File(newWavFilePath)); //合并完成后，删除多余的文件
                        }, newRecord.getDestFileDuration() * 1000L + 300L);
                    } else {
                        JL_Log.w(tag, "mergeTranslationRecord", "mergeWavFiles failed. src text");
                    }
                }
            }
        }
        String cacheDestText = cacheRecord.getDestText();
        String newDestText = newRecord.getDestText();
        if (!TextUtils.equals(cacheDestText, newDestText)) {
            record.setDestText(cacheDestText + " " + newDestText);
            if (isMergeAudio) {
                String outputFilePath = cacheRecord.getDestFilePath();
                String newWavFilePath = newRecord.getDestFilePath();
                if (!TextUtils.isEmpty(outputFilePath) && !TextUtils.isEmpty(newWavFilePath)) {
                    if (WavUtil.mergeWavFiles(outputFilePath, newWavFilePath)) {
                        record.setDestFilePath(outputFilePath);
                        record.setDestFileDuration(WavUtil.getWavDuration(outputFilePath));
                        uiHandler.postDelayed(() -> { //过早删除文件会导致音频播放有问题
                            FileUtil.deleteFile(new File(newWavFilePath)); //合并完成后，删除多余的文件
                        }, newRecord.getDestFileDuration() * 1000L + 300L);
                    } else {
                        JL_Log.w(tag, "mergeTranslationRecord", "mergeWavFiles failed. dest text");
                    }
                }
            }
        }
        JL_Log.d(tag, "mergeTranslationRecord", "record : " + record);
        return record;
    }

    private OpResult<TextTranslationResponse> translateTextByUrl(@NonNull String accessKey, @NonNull String secretKey, @NonNull TranslateTextParam param) {
        TranslateTextRequest request = new TranslateTextRequest(param.getTargetLanguage(), param.getSrcTextList());
        RequestBody body = RequestBody.create(request.toString(), MediaType.get("application/json; charset=utf-8"));
        SignSpeechTranslate signSpeechTranslate = new SignSpeechTranslate(AIConfig.REGION, AIConfig.SERVICE,
                AIConfig.TRANSLATE_TEXT_HOST, AIConfig.TRANSLATE_TEXT_PATH, accessKey, secretKey);
        try {
            JL_Log.d(tag, "translateTextByUrl", "body : " + request + ",\n, param : " + param);
            HashMap<String, String> signHeaders = signSpeechTranslate.getSignHeader(AIConfig.METHOD_POST, new HashMap<>(), request.toString().getBytes(),
                    new Date(), AIConfig.TRANSLATE_ACTION, AIConfig.VERSION);
            String url = signHeaders.get(AIConfig.KEY_URL);
            if (null == url || url.isEmpty()) {
                return new OpResult<TextTranslationResponse>()
                        .setCode(ErrorCode.SUB_ERR_PARAMETER)
                        .setMessage("No Url.");
            }
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .post(body);
            addHeader(builder, signHeaders, AIConfig.KEY_HOST);
            addHeader(builder, signHeaders, AIConfig.KEY_X_DATE);
            addHeader(builder, signHeaders, AIConfig.KEY_X_CONTENT_SHA256);
            addHeader(builder, signHeaders, AIConfig.KEY_CONTENT_TYPE);
            addHeader(builder, signHeaders, AIConfig.KEY_AUTHORIZATION);
            try (Response response = AIConfig.httpClient.newCall(builder.build()).execute()) {
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    String bodyString = null == responseBody ? null : responseBody.string();
                    if (null == bodyString || bodyString.isEmpty()) {
                        return new OpResult<TextTranslationResponse>()
                                .setCode(ErrorCode.SUB_ERR_NOT_FOUND_DATA)
                                .setMessage("No Response Body.");
                    }
                    try {
                        TextTranslationResponse translationResponse = AIConfig.gson.fromJson(bodyString, TextTranslationResponse.class);
                        return new OpResult<TextTranslationResponse>()
                                .setCode(ErrorCode.ERR_NONE)
                                .setData(translationResponse);
                    } catch (JsonSyntaxException e) {
                        return new OpResult<TextTranslationResponse>()
                                .setCode(ErrorCode.SUB_ERR_PARSE_DATA)
                                .setMessage("Failed to parse data. " + bodyString);
                    }
                } else {
                    int code = response.code();
                    return new OpResult<TextTranslationResponse>()
                            .setCode(ErrorCode.SUB_ERR_IO_EXCEPTION)
                            .setMessage(CommonUtil.formatString("Service reply an bad code(%d).", code));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new OpResult<TextTranslationResponse>()
                .setCode(ErrorCode.SUB_ERR_OP_FAILED)
                .setMessage("Failed to translate text.");
    }

    private void addHeader(@NonNull Request.Builder builder, @NonNull HashMap<String, String> map, String key) {
        String value = map.get(key);
        if (null == value || value.isEmpty()) return;
        builder.header(key, value);
    }


    private static class Wrapper {
        @NonNull
        private final TranslateParam param;
        @NonNull
        private final OnTranslateResultCallback callback;

        public Wrapper(@NonNull TranslateParam param, @NonNull OnTranslateResultCallback callback) {
            this.param = param;
            this.callback = callback;
        }

        @NonNull
        public TranslateParam getParam() {
            return param;
        }

        @NonNull
        public String getMac() {
            return param.getMac();
        }

        @NonNull
        public RoleInfo getRoleInfo() {
            return param.getRoleInfo();
        }


        @NonNull
        public OnTranslateResultCallback getCallback() {
            return callback;
        }

        @NonNull
        @Override
        public String toString() {
            return "Wrapper{" +
                    "param=" + param +
                    ", \ncallback=" + callback +
                    '}';
        }
    }
}
