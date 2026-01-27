package com.jieli.btsmart.tool.ai.doubao.tts;

import com.jieli.bluetooth.bean.translation.TranslationResult;
import com.jieli.btsmart.tool.ai.doubao.tts.model.TtsRequest;

/**
 * OnTtsResultCallback
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc TTS结果回调
 * @since 2025/6/30
 */
public interface OnTtsResultCallback {

    /**
     * 回调合成语音开始
     */
    void onStart();

    /**
     * 回调合成语音结束
     *
     * @param result    TranslationResult 翻译结果
     * @param filePaths String[] 输出文件路径
     *                  <p>
     *                      String[0] --- 翻译文本WAV文件路径<br/>
     *                      String[1] --- 翻译文本BIN文件路径<br/>
     *                      String[2] --- 原始文本WAV文件路径
     *                      </p>
     */
    void onStop(TranslationResult result, String[] filePaths);

    /**
     * 回调合成语音失败
     *
     * @param result  TranslationResult 翻译结果
     * @param code    int 错误码
     * @param message String 错误描述
     */
    void onError(TranslationResult result, int code, String message);
}
