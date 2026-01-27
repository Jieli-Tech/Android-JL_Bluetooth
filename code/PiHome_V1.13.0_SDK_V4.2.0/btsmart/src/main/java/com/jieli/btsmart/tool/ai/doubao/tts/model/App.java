package com.jieli.btsmart.tool.ai.doubao.tts.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * App
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 应用信息
 * @since 2025/6/24
 */
public class App implements Parcelable {

    /**
     * 应用标识
     *
     * <p>必要项</p>
     */
    @SerializedName("appid")
    private final String appId;
    /**
     * 应用令牌
     *
     * <p>
     * 必要项<br/>
     * 目前未生效，使用默认值即可
     * </p>
     */
    private final String token;
    /**
     * 业务集群
     * <p>
     * 必要项
     * </p>
     */
    private final String cluster;

    public App(String appId) {
        this(appId, "");
    }

    public App(String appId, String token) {
        this(appId, token, "volcano_tts");
    }

    public App(String appId, String token, String cluster) {
        this.appId = appId;
        this.token = token;
        this.cluster = cluster;
    }

    protected App(Parcel in) {
        appId = in.readString();
        token = in.readString();
        cluster = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(appId);
        dest.writeString(token);
        dest.writeString(cluster);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<App> CREATOR = new Creator<App>() {
        @Override
        public App createFromParcel(Parcel in) {
            return new App(in);
        }

        @Override
        public App[] newArray(int size) {
            return new App[size];
        }
    };

    public String getAppId() {
        return appId;
    }

    public String getToken() {
        return token;
    }

    public String getCluster() {
        return cluster;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
