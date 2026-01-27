package com.jieli.btsmart.tool.ai.doubao.basic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * WebSocketClient
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc WebSocket客户端
 * @since 2025/6/24
 */
public class WebSocketClient {
    private final String tag = getClass().getSimpleName();
    /**
     * WebSocket对象
     */
    private WebSocket mWebSocket;
    /**
     * 是否连接上服务器
     */
    private volatile boolean isConnected;

    public boolean isConnected() {
        return isConnected;
    }

    public void start(Request request, WebSocketListener listener) {
        stop();
        mWebSocket = AIConfig.httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                JL_Log.w(tag, "onClosed", CommonUtil.formatString("code : %s, reason : %s",
                        CommonUtil.formatInt(code), reason));
                isConnected = false; //连接已关闭
                if (webSocket.equals(mWebSocket)) {
                    mWebSocket = null;
                }
                if (null != listener) {
                    listener.onClosed(webSocket, code, reason);
                }
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                JL_Log.i(tag, "onClosing", CommonUtil.formatString("code : %s, reason : %s",
                        CommonUtil.formatInt(code), reason));
                // 当服务端发送关闭连接的请求时触发
                webSocket.close(code, reason);
                if (null != listener) {
                    listener.onClosing(webSocket, code, reason);
                }
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                JL_Log.w(tag, "onFailure", "throwable : " + t.getMessage());
                // 发生错误时的操作
                isConnected = false;
                if (webSocket.equals(mWebSocket)) {
                    mWebSocket = null;
                }
                if (null != listener) {
                    listener.onFailure(webSocket, t, response);
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                JL_Log.d(tag, "onMessage", "<<< text : " + text);
                // 收到消息时的操作
                if (null != listener) {
                    listener.onMessage(webSocket, text);
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                JL_Log.d(tag, "onMessage", CommonUtil.formatString("<<< data : [%s]", CHexConver.byte2HexStr(bytes.toByteArray())));
                // 收到二进制消息时的操作
                if (null != listener) {
                    listener.onMessage(webSocket, bytes);
                }
            }

            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                JL_Log.i(tag, "onOpen", "response : " + response);
                // 连接打开时的操作
                isConnected = true;
                if (null != listener) {
                    listener.onOpen(webSocket, response);
                }
            }
        });
    }

    public void stop() {
        if (mWebSocket != null) {
            mWebSocket.close(1000, "User Stop");
        }
        isConnected = false;
        mWebSocket = null;
    }

    public boolean sendMessage(String message) {
        if (!isConnected || null == mWebSocket || null == message) return false;
        boolean ret = mWebSocket.send(message);
        JL_Log.d(tag, "sendMessage", CommonUtil.formatString(">>> %s, message : %s", ret, message));
        return ret;
    }

    public boolean sendData(ByteString data) {
        if (!isConnected || null == mWebSocket || null == data) return false;
        boolean ret = mWebSocket.send(data);
        JL_Log.d(tag, "sendData", CommonUtil.formatString(">>> %s, data : [%s]", ret, CHexConver.byte2HexStr(data.toByteArray())));
        return ret;
    }
}
