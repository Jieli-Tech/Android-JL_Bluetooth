package com.jieli.btsmart.tool.ai.doubao.translate.model.response;

import android.os.Parcel;
import android.os.Parcelable;

import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * Subtitle
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译结果
 * @since 2025/6/24
 */
public class Subtitle implements Parcelable {
    /**
     * 翻译结果
     */
    private final String Text;
    /**
     * 文本识别的开始时间
     */
    private final Integer BeginTime;
    /**
     * 文本识别的结束时间
     */
    private final Integer EndTime;
    /**
     * 文本是否确定
     */
    private final Boolean Definite;
    /**
     * 语言
     */
    private final String Language;
    /**
     * 序列号
     */
    private final Integer Sequence;

    public Subtitle(String text, Integer beginTime, Integer endTime, Boolean definite, String language, Integer sequence) {
        Text = text;
        BeginTime = beginTime;
        EndTime = endTime;
        Definite = definite;
        Language = language;
        Sequence = sequence;
    }

    protected Subtitle(Parcel in) {
        Text = in.readString();
        if (in.readByte() == 0) {
            BeginTime = null;
        } else {
            BeginTime = in.readInt();
        }
        if (in.readByte() == 0) {
            EndTime = null;
        } else {
            EndTime = in.readInt();
        }
        byte tmpDefinite = in.readByte();
        Definite = tmpDefinite == 0 ? null : tmpDefinite == 1;
        Language = in.readString();
        if (in.readByte() == 0) {
            Sequence = null;
        } else {
            Sequence = in.readInt();
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(Text);
        if (BeginTime == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(BeginTime);
        }
        if (EndTime == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(EndTime);
        }
        dest.writeByte((byte) (Definite == null ? 0 : Definite ? 1 : 2));
        dest.writeString(Language);
        if (Sequence == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(Sequence);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Subtitle> CREATOR = new Creator<Subtitle>() {
        @Override
        public Subtitle createFromParcel(Parcel in) {
            return new Subtitle(in);
        }

        @Override
        public Subtitle[] newArray(int size) {
            return new Subtitle[size];
        }
    };

    public String getText() {
        return Text;
    }

    public Integer getBeginTime() {
        return BeginTime;
    }

    public Integer getEndTime() {
        return EndTime;
    }

    public Boolean getDefinite() {
        return Definite;
    }

    public String getLanguage() {
        return Language;
    }

    public Integer getSequence() {
        return Sequence;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
