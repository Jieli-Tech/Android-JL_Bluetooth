package com.jieli.btsmart.tool.ai.doubao.translate.model.response;

import android.os.Parcel;
import android.os.Parcelable;

import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * TranslationResponse
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译结果回复
 * @since 2025/6/24
 */
public class TranslationResponse implements Parcelable {
    /**
     * 翻译结果
     */
    private final Subtitle Subtitle;
    /**
     * 回复元数据
     */
    private final ResponseMetaData ResponseMetaData;

    public TranslationResponse(Subtitle subtitle, ResponseMetaData responseMetaData) {
        Subtitle = subtitle;
        ResponseMetaData = responseMetaData;
    }

    protected TranslationResponse(Parcel in) {
        Subtitle = in.readParcelable(Subtitle.class.getClassLoader());
        ResponseMetaData = in.readParcelable(ResponseMetaData.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(Subtitle, flags);
        dest.writeParcelable(ResponseMetaData, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TranslationResponse> CREATOR = new Creator<TranslationResponse>() {
        @Override
        public TranslationResponse createFromParcel(Parcel in) {
            return new TranslationResponse(in);
        }

        @Override
        public TranslationResponse[] newArray(int size) {
            return new TranslationResponse[size];
        }
    };

    public Subtitle getSubtitle() {
        return Subtitle;
    }

    public ResponseMetaData getResponseMetaData() {
        return ResponseMetaData;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
