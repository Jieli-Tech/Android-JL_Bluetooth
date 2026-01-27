package com.jieli.btsmart.tool.translate.player;

/**
 * OnMusicStateCallback
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 音乐状态回调
 * @since 2025/8/18
 */
public interface OnMusicStateCallback {

    /**
     * 回调播放开始
     *
     * @param url      String 唯一标识
     * @param duration int 播放时长
     */
    void onStart(String url, int duration);

    /**
     * 回调播放进度
     *
     * @param url      String 唯一标识
     * @param position int 进度
     */
    void onProgress(String url, int position);

    /**
     * 回调播放结束
     *
     * @param url String 唯一标识
     */
    void onCompletion(String url);

    /**
     * 回调播放异常
     *
     * @param url     String 唯一标识
     * @param code    int 错误码
     * @param message String 异常描述
     */
    void onError(String url, int code, String message);
}
