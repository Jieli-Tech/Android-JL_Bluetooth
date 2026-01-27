package com.jieli.btsmart.tool.ai.doubao.translate.model.response;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * Translation
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 文本翻译结果
 * @since 2025/9/9
 */
public class TextTranslation implements Parcelable {
    /**
     * 翻译文本
     */
    @SerializedName("Translation")
    private final String translation;
    /**
     * 检测到原文语言
     */
    @SerializedName("DetectedSourceLanguage")
    private final String sourceLanguage;

    public TextTranslation(String translation, String sourceLanguage) {
        this.translation = translation;
        this.sourceLanguage = sourceLanguage;
    }

    protected TextTranslation(Parcel in) {
        translation = in.readString();
        sourceLanguage = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(translation);
        dest.writeString(sourceLanguage);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TextTranslation> CREATOR = new Creator<TextTranslation>() {
        @Override
        public TextTranslation createFromParcel(Parcel in) {
            return new TextTranslation(in);
        }

        @Override
        public TextTranslation[] newArray(int size) {
            return new TextTranslation[size];
        }
    };

    public String getTranslation() {
        return translation;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
