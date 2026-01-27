package com.jieli.btsmart.tool.ai.doubao.tts;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.translation.ai_auth.AIAuthMessage;
import com.jieli.btsmart.data.model.translation.ai_auth.DoubaoTTSMessage;
import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;
import com.jieli.btsmart.tool.ai.doubao.basic.WebSocketClient;
import com.jieli.btsmart.tool.ai.doubao.tts.model.TtsRequest;
import com.jieli.btsmart.tool.ai.doubao.tts.model.TtsResponse;
import com.jieli.btsmart.tool.configure.ConfigureKit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * TtsGenerator
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc TTS生成器
 * @since 2025/6/24
 */
public class TtsGenerator {
    private final String tag = getClass().getSimpleName();

    /**
     * WebSocket客户端
     */
    private final WebSocketClient mWebSocketClient = new WebSocketClient();
    /**
     * 输出缓存BUF
     */
    private final ByteArrayOutputStream mOutputStream = new ByteArrayOutputStream();

    /**
     * 工作状态
     */
    private final StateResult<byte[]> mWorkState = new StateResult<>(AIConfig.OP_TTS);
    /**
     * 结果码
     */
    private int result = -1;
    /**
     * 描述信息
     */
    private String message = "";

    boolean isWorking() {
        return mWorkState.getState() == StateResult.STATE_WORKING;
    }

    public void start(@NonNull TtsRequest request, @NonNull Consumer<StateResult<byte[]>> callback) {
        if (isWorking()) {
            callbackFinish(callback, ErrorCode.SUB_ERR_OPERATION_IN_PROGRESS, ErrorCode.code2Msg(ErrorCode.SUB_ERR_OPERATION_IN_PROGRESS));
            return;
        }
        final AIAuthMessage authMessage = ConfigureKit.getInstance().getAIAuthMessage();
        final DoubaoTTSMessage ttsMessage = null == authMessage ? null : authMessage.getDoubaoTTSMessage();
        if (null == ttsMessage || !authMessage.isValid()) {
            String msg = null == ttsMessage ? "NO Doubao TTS Message" : "Auth Message is expired.";
            callbackFinish(callback, ErrorCode.SUB_ERR_PARAMETER, msg);
            return;
        }
        String accessToken = ttsMessage.getAccessToken();
        if (TextUtils.isEmpty(accessToken)) {
            callbackFinish(callback, ErrorCode.SUB_ERR_PARAMETER, "Missing access token.");
            return;
        }
        callbackWorking(callback);
        JL_Log.d(tag, "start", request + "");
        mWebSocketClient.start(new Request.Builder()
                .url(AIConfig.TTS_API_URL)
                .header("Authorization", "Bearer; " + accessToken)
                .build(), new WebSocketListener() {
            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                if (result == -1) {
                    result = ErrorCode.SUB_ERR_OP_FAILED;
                    message = CommonUtil.formatString("%s. code : %s, reason : %s.",
                            ErrorCode.code2Msg(result), CommonUtil.formatInt(code), reason);
                } else {
                    if (TextUtils.isEmpty(message)) {
                        message = ErrorCode.code2Msg(result);
                    }
                }
                JL_Log.i(tag, "onClosed", "code : " + CommonUtil.formatInt(result) + ", " + message);
                callbackFinish(callback, result, message);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                JL_Log.w(tag, "onFailure", "Throwable : " + t);
                callbackFinish(callback, ErrorCode.SUB_ERR_IO_EXCEPTION, "IO Exception : " + t);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                handleData(bytes.toByteArray());
            }

            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                configureTts(request);
            }
        });
    }

    public void stop() {
        if (isWorking() && result == -1) {
            result = ErrorCode.SUB_ERR_OPERATION_CANCEL;
        }
        mWebSocketClient.stop();
    }

    private StateResult<byte[]> cloneState() {
        return new StateResult<byte[]>(mWorkState.getOp())
                .setState(mWorkState.getState())
                .setProgress(mWorkState.getProgress())
                .setCode(mWorkState.getCode())
                .setMessage(mWorkState.getMessage())
                .setData(mWorkState.getData());
    }

    private void callbackWorking(Consumer<StateResult<byte[]>> callback) {
        int state = mWorkState.getState();
        if (state == StateResult.STATE_WORKING) return;
        mOutputStream.reset();
        result = -1;
        mWorkState.setState(StateResult.STATE_WORKING).setCode(0).setProgress(0);
        if(null != callback){
            callback.accept(cloneState());
        }
    }

    private void callbackFinish(Consumer<StateResult<byte[]>> callback, int code, String message) {
        int state = mWorkState.getState();
        if (state == StateResult.STATE_FINISH){
            JL_Log.w(tag, "callbackFinish", "state is finish.");
            return;
        }
        mWorkState.setState(StateResult.STATE_FINISH).setCode(code).setMessage(message);
        if (code == ErrorCode.ERR_NONE) { //结束数据成功
            byte[] data = mOutputStream.toByteArray();
            mWorkState.setData(data);
        }
        JL_Log.i(tag, "callbackFinish", "callback");
        if(null != callback){
            callback.accept(cloneState());
        }
        mOutputStream.reset();
    }

    private void configureTts(@NonNull TtsRequest request) {
        byte[] jsonBytes = request.toString().getBytes(StandardCharsets.UTF_8);
        byte[] header = new byte[]{0x11, 0x10, 0x10, 0x00};
        ByteBuffer requestBytes = ByteBuffer.allocate(8 + jsonBytes.length);
        requestBytes.put(header).putInt(jsonBytes.length).put(jsonBytes);
        mWebSocketClient.sendData(new ByteString(requestBytes.array()));
    }

    private void handleData(byte[] data) {
        TtsResponse ttsResponse = new TtsResponse();
        int size = ttsResponse.parseData(data);
        if (size <= 0) return;
//        JL_Log.d(tag, "handleData", ttsResponse.toString());
        switch (ttsResponse.getMessageType()) { // Audio-only server response
            case 11: {
//                JL_Log.d(tag, "handleData", "received audio-only response. flags : "
//                        + ttsResponse.getMessageTypeSpecificFlags());
                if (ttsResponse.getMessageTypeSpecificFlags() != 0) {
                    try {
                        mOutputStream.write(ttsResponse.getPayload());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (ttsResponse.getSequenceNumber() < 0) {
                        // received the last segment
                        JL_Log.i(tag, "handleData", "received all audio data.");
                        result = ErrorCode.ERR_NONE;
                        stop();
                    }
                } else { // Ack without audio data

                }
                break;
            }
            case 15: { // Error message from server
                result = ErrorCode.SUB_ERR_OP_FAILED;
                message = CommonUtil.formatString("%s. code : %s, reason : %s.",
                        ErrorCode.code2Msg(result), CommonUtil.formatInt(ttsResponse.getCode()), ttsResponse.getMessage());
                stop();
                break;
            }
            default:
                JL_Log.w(tag, "handleData",
                        "Received unknown response message type: " + ttsResponse.getMessageType());
                break;
        }
    }
}
