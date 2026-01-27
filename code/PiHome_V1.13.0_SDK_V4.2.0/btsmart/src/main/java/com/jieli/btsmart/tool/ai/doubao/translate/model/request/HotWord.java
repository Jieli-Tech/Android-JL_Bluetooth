package com.jieli.btsmart.tool.ai.doubao.translate.model.request;

import android.os.Parcel;
import android.os.Parcelable;

import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * HotWord
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 热词
 * @since 2025/6/24
 */
public class HotWord implements Parcelable {
    /**
     * 热词
     */
    private final String Word;
    /**
     * 占比
     */
    private final int Scale;

    public HotWord(String Word, int Scale) {
        this.Word = Word;
        this.Scale = Scale;
    }

    protected HotWord(Parcel in) {
        Word = in.readString();
        Scale = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(Word);
        dest.writeInt(Scale);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HotWord> CREATOR = new Creator<HotWord>() {
        @Override
        public HotWord createFromParcel(Parcel in) {
            return new HotWord(in);
        }

        @Override
        public HotWord[] newArray(int size) {
            return new HotWord[size];
        }
    };

    public String getWord() {
        return Word;
    }

    public int getScale() {
        return Scale;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
