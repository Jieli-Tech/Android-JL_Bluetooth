package com.jieli.btsmart.tool.translate;

import com.jieli.btsmart.tool.ai.doubao.translate.OnTranslateResultCallback;

/**
 * TranslationStateCallback
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译事件回调
 * @since 2025/7/31
 */
public interface TranslationStateCallback extends OnTranslateResultCallback {

    /**
     * 回调翻译状态
     *
     * @param state TranslateState 翻译状态
     */
    void onTranslateState(TranslateState state);

}
