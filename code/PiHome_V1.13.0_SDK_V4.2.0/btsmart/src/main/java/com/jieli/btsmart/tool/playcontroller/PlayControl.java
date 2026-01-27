package com.jieli.btsmart.tool.playcontroller;

import com.jieli.audio.media_player.JL_PlayMode;

/**
 * Created by chensenhua on 2018/1/17.
 * 播放控制接口
 */

public interface PlayControl {

    /**
     * 播放下一首
     */
    void playNext();

    /**
     * 播放上一首
     */
    void playPre();


    /**
     * 播放
     */
    void play();

    /**
     * 暂停
     */
    void pause();

    /**
     * 暂停或播放
     */
    void playOrPause();

    /**
     * 音量设置
     *
     * @param value：音量值
     */
    void setVolume(int value);

    /**
     * 增大音量，,语音控制
     */
    void volumeUp();


    /**
     * 减小音量 ,语音控制
     */
    void volumeDown();

    /**
     * 减小音量 ,手动控制
     */
    void volumeDownByHand();

    /**
     * 增大音量 ,手动控制
     */
    void volumeUpByHand();

    /**
     * 设置播放模式
     *
     * @param playmode
     */

    void setPlaymode(JL_PlayMode playmode);

    /**
     * 设置为下一个播放模式
     */
    void setNextPlaymode();

    /**
     * 设置状态回调
     *
     * @param callback
     */
    void setPlayControlCallback(PlayControlCallback callback);

    /**
     * 同步当前状态状态
     */
    void refresh();

    /**
     * 释放资源
     */
    void release();

    /**
     * 调节播放进度
     *
     * @param position
     */
    void seekTo(int position);

    /**
     * 判断播放状态
     *
     * @return
     */
    boolean isPlay();

    /**
     * 调用这个接口开始进度回调
     */

    void onStart();

    /**
     * 调用这个接口停止进度回调
     */

    void onPause();


}
