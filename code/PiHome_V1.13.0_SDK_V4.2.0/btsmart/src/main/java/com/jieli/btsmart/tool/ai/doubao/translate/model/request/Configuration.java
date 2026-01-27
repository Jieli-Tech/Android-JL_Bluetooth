package com.jieli.btsmart.tool.ai.doubao.translate.model.request;

import android.os.Parcel;
import android.os.Parcelable;

import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译配置
 * @since 2025/6/24
 */
public class Configuration implements Parcelable {
    /**
     * 源语言
     */
    private final String SourceLanguage;
    /**
     * 目标语言
     */
    private final List<String> TargetLanguages;
    /**
     * 热词
     */
    private final List<HotWord> HotWordList;

    public Configuration(String sourceLanguage, List<String> targetLanguages) {
        SourceLanguage = sourceLanguage;
        TargetLanguages = targetLanguages;
        HotWordList = new ArrayList<>();
    }

    protected Configuration(Parcel in) {
        SourceLanguage = in.readString();
        TargetLanguages = in.createStringArrayList();
        HotWordList = in.createTypedArrayList(HotWord.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(SourceLanguage);
        dest.writeStringList(TargetLanguages);
        dest.writeTypedList(HotWordList);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Configuration> CREATOR = new Creator<Configuration>() {
        @Override
        public Configuration createFromParcel(Parcel in) {
            return new Configuration(in);
        }

        @Override
        public Configuration[] newArray(int size) {
            return new Configuration[size];
        }
    };

    public String getSourceLanguage() {
        return SourceLanguage;
    }

    public List<String> getTargetLanguages() {
        return TargetLanguages;
    }

    public List<HotWord> getHotWordList() {
        return HotWordList;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
