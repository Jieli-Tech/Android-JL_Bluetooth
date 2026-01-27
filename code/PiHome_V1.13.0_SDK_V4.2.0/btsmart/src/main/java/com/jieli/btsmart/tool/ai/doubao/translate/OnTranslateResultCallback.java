package com.jieli.btsmart.tool.ai.doubao.translate;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.interfaces.rcsp.translation.AITranslationCallback;
import com.jieli.btsmart.data.model.translation.TranslationRecord;

/**
 * OnTranslateResultCallback
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译结果回调
 * @since 2025/8/11
 */
public interface OnTranslateResultCallback extends AITranslationCallback {
    /**
     * 回调翻译记录
     *
     * @param record TranslationRecord 翻译记录
     */
    void onTranslateRecord(@NonNull TranslationRecord record);
}
