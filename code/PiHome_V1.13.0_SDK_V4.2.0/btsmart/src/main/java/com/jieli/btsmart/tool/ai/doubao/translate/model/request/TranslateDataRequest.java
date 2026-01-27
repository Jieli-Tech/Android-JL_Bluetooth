package com.jieli.btsmart.tool.ai.doubao.translate.model.request;

import android.os.Parcel;
import android.os.Parcelable;

import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * TranslateDataRequest
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译数据指令
 * @since 2025/6/24
 */
public class TranslateDataRequest implements Parcelable {

    /**
     * 音频数据
     */
    private final String AudioData;

    public TranslateDataRequest(String audioData) {
        AudioData = audioData;
    }

    protected TranslateDataRequest(Parcel in) {
        AudioData = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(AudioData);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TranslateDataRequest> CREATOR = new Creator<TranslateDataRequest>() {
        @Override
        public TranslateDataRequest createFromParcel(Parcel in) {
            return new TranslateDataRequest(in);
        }

        @Override
        public TranslateDataRequest[] newArray(int size) {
            return new TranslateDataRequest[size];
        }
    };

    public String getAudioData() {
        return AudioData;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
