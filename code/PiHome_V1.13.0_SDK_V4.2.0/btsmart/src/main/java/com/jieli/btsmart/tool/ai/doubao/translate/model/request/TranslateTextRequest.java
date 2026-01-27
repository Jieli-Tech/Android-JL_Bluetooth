package com.jieli.btsmart.tool.ai.doubao.translate.model.request;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

import java.util.List;

/**
 * TranslateTextRequest
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc  翻译文本请求
 * @since 2025/9/9
 */
public class TranslateTextRequest implements Parcelable {
    /**
     * 目标语言
     */
    @SerializedName("TargetLanguage")
    private final String targetLanguage;
    /**
     * 原文
     */
    @SerializedName("TextList")
    private final List<String> textList;

    public TranslateTextRequest(String targetLanguage, List<String> textList) {
        this.targetLanguage = targetLanguage;
        this.textList = textList;
    }

    protected TranslateTextRequest(Parcel in) {
        targetLanguage = in.readString();
        textList = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(targetLanguage);
        dest.writeStringList(textList);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TranslateTextRequest> CREATOR = new Creator<TranslateTextRequest>() {
        @Override
        public TranslateTextRequest createFromParcel(Parcel in) {
            return new TranslateTextRequest(in);
        }

        @Override
        public TranslateTextRequest[] newArray(int size) {
            return new TranslateTextRequest[size];
        }
    };

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public List<String> getTextList() {
        return textList;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
