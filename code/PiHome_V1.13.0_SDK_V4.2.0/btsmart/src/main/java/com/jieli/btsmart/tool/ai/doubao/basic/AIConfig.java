package com.jieli.btsmart.tool.ai.doubao.basic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jieli.btsmart.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * AIConfig
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc AI配置
 * @since 2025/6/24
 */
public class AIConfig {

    public static final Gson gson = new GsonBuilder().setLenient().create();

    public static final OkHttpClient httpClient = buildOKHttpClient();

    /* ---------------------------------------- *
     * 操作码
     * ---------------------------------------- */

    public static final int OP_TRANSLATE = 0x4010;
    public static final int OP_TTS = 0x4011;

    /* ---------------------------------------- *
     * 鉴权配置
     * ---------------------------------------- */

    public static final String KEY_ACTION = "Action";
    public static final String KEY_VERSION = "Version";

    public static final String KEY_HOST = "Host";
    public static final String KEY_X_DATE = "X-Date";
    public static final String KEY_X_CONTENT_SHA256 = "X-Content-Sha256";
    public static final String KEY_CONTENT_TYPE = "Content-Type";
    public static final String KEY_AUTHORIZATION = "Authorization";
    public static final String KEY_URL = "URL";

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";

    /* ---------------------------------------- *
     * 翻译配置
     * ---------------------------------------- */

    public static final String API = "SpeechTranslate";
    public static final String VERSION = "2020-06-01";
    public static final String PATH = "/api/translate/speech/v1/";
    public static final String HOST = "translate.volces.com";
    public static final String SERVICE = "translate";
    public static final String REGION = "cn-north-1";
    public static final String TRANSLATE_ACTION = "TranslateText";
    public static final String TRANSLATE_TEXT_HOST = "translate.volcengineapi.com";
    public static final String TRANSLATE_TEXT_PATH = "/";

    /* ---------------------------------------- *
     * 语音合成配置
     * ---------------------------------------- */

    public static final String TTS_API_URL = "wss://openspeech.bytedance.com/api/v1/tts/ws_binary";


    private static OkHttpClient buildOKHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS);
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BODY));
        }
        return builder.build();
    }
}
