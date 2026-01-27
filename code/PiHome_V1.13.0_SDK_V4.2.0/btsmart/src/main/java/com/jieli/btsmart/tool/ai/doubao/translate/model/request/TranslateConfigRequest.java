package com.jieli.btsmart.tool.ai.doubao.translate.model.request;

import android.os.Parcel;
import android.os.Parcelable;

import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * TranslateConfigRequest
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译配置指令
 * @since 2025/6/24
 */
public class TranslateConfigRequest implements Parcelable {
    /**
     * 配置
     */
    private final Configuration Configuration;

    public TranslateConfigRequest(Configuration configuration) {
        Configuration = configuration;
    }

    protected TranslateConfigRequest(Parcel in) {
        Configuration = in.readParcelable(Configuration.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(Configuration, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TranslateConfigRequest> CREATOR = new Creator<TranslateConfigRequest>() {
        @Override
        public TranslateConfigRequest createFromParcel(Parcel in) {
            return new TranslateConfigRequest(in);
        }

        @Override
        public TranslateConfigRequest[] newArray(int size) {
            return new TranslateConfigRequest[size];
        }
    };

    public Configuration getConfiguration() {
        return Configuration;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
