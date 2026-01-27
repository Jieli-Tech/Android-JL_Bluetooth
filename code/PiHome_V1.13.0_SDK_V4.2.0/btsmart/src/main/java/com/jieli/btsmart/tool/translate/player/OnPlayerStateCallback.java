package com.jieli.btsmart.tool.translate.player;

import androidx.annotation.NonNull;

import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.data.model.translation.TranslationSessionRecord;

/**
 * OnPlayerStateCallback
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 播放器状态回调
 * @since 2025/8/19
 */
public interface OnPlayerStateCallback {
    /**
     * 回调会议记录改变
     *
     * @param record TranslationSessionRecord 会议记录
     */
    void onSessionRecordChange(@NonNull TranslationSessionRecord record);

    /**
     * 回调播放开始
     */
    void onStart();

    /**
     * 回调播放器状态改变
     *
     * @param state int 播放器状态
     */
    void onStateChange(int state);

    /**
     * 回调播放进度
     *
     * @param progress int 进度
     */
    void onProgress(int progress);

    /**
     * 回调当前播放的翻译记录
     *
     * @param index  int 索引号
     * @param record TranslationRecord 播放记录
     */
    void onTranslationRecord(int index, @NonNull TranslationRecord record);

    /**
     * 回调播放异常
     *
     * @param index   int 索引号
     * @param type    int 播放类型
     * @param code    int 错误码
     * @param message String 错误描述
     */
    void onError(int index, int type, int code, String message);

    /**
     * 回调播放结束
     *
     * @param result int 结果码
     */
    void onStop(int result);
}
