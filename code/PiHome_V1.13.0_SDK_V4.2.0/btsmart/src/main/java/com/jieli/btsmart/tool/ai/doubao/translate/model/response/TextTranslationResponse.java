package com.jieli.btsmart.tool.ai.doubao.translate.model.response;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

import java.util.List;

/**
 * TextTranslationResponse
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 文本翻译回复
 * @since 2025/9/9
 */
public class TextTranslationResponse implements Parcelable {

    /**
     * 翻译结果
     */
    @SerializedName("TranslationList")
    private final List<TextTranslation> translationList;
    /**
     * 回复元数据
     */
    @SerializedName("ResponseMetadata")
    private final ResponseMetaData responseMetadata;

    public TextTranslationResponse(List<TextTranslation> translationList, ResponseMetaData responseMetadata) {
        this.translationList = translationList;
        this.responseMetadata = responseMetadata;
    }

    protected TextTranslationResponse(Parcel in) {
        translationList = in.createTypedArrayList(TextTranslation.CREATOR);
        responseMetadata = in.readParcelable(ResponseMetaData.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(translationList);
        dest.writeParcelable(responseMetadata, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TextTranslationResponse> CREATOR = new Creator<TextTranslationResponse>() {
        @Override
        public TextTranslationResponse createFromParcel(Parcel in) {
            return new TextTranslationResponse(in);
        }

        @Override
        public TextTranslationResponse[] newArray(int size) {
            return new TextTranslationResponse[size];
        }
    };

    public List<TextTranslation> getTranslationList() {
        return translationList;
    }

    public ResponseMetaData getResponseMetadata() {
        return responseMetadata;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
