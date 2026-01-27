package com.jieli.btsmart.tool.ai.doubao.tts.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * TtsRequest
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc TTS请求数据
 * @since 2025/6/25
 */
public class TtsRequest implements Parcelable {
    /**
     * 应用配置
     */
    private final App app;
    /**
     * 用户配置
     */
    private final User user;
    /**
     * 音频配置
     */
    private final Audio audio;
    /**
     * 请求参数
     */
    private final Request request;

    public TtsRequest(App app, User user, Audio audio, Request request) {
        this.app = app;
        this.user = user;
        this.audio = audio;
        this.request = request;
    }

    protected TtsRequest(Parcel in) {
        app = in.readParcelable(App.class.getClassLoader());
        user = in.readParcelable(User.class.getClassLoader());
        audio = in.readParcelable(Audio.class.getClassLoader());
        request = in.readParcelable(Request.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(app, flags);
        dest.writeParcelable(user, flags);
        dest.writeParcelable(audio, flags);
        dest.writeParcelable(request, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TtsRequest> CREATOR = new Creator<TtsRequest>() {
        @Override
        public TtsRequest createFromParcel(Parcel in) {
            return new TtsRequest(in);
        }

        @Override
        public TtsRequest[] newArray(int size) {
            return new TtsRequest[size];
        }
    };

    public App getApp() {
        return app;
    }

    public User getUser() {
        return user;
    }

    public Audio getAudio() {
        return audio;
    }

    public Request getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
