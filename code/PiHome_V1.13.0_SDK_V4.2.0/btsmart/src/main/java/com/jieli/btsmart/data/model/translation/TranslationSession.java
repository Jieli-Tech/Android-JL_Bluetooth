package com.jieli.btsmart.data.model.translation;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * TranslationSession
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译会话
 * @since 2025/6/12
 */
@Entity
public class TranslationSession implements Parcelable {
    /**
     * 会话ID
     */
    @PrimaryKey(autoGenerate = true)
    private int id;
    /**
     * 设备地址
     */
    private String mac;
    /**
     * 翻译模式
     */
    private int translationMode;
    /**
     * 标题
     */
    private String title;
    /**
     * 开始时间戳
     */
    private long startTime;
    /**
     * 结束时间戳
     */
    private long endTime;

    public TranslationSession() {

    }

    protected TranslationSession(Parcel in) {
        id = in.readInt();
        mac = in.readString();
        translationMode = in.readInt();
        title = in.readString();
        startTime = in.readLong();
        endTime = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(mac);
        dest.writeInt(translationMode);
        dest.writeString(title);
        dest.writeLong(startTime);
        dest.writeLong(endTime);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TranslationSession> CREATOR = new Creator<TranslationSession>() {
        @Override
        public TranslationSession createFromParcel(Parcel in) {
            return new TranslationSession(in);
        }

        @Override
        public TranslationSession[] newArray(int size) {
            return new TranslationSession[size];
        }
    };

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getTranslationMode() {
        return translationMode;
    }

    public void setTranslationMode(int translationMode) {
        this.translationMode = translationMode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "TranslationSession{" +
                "id=" + id +
                ", mac='" + mac + '\'' +
                ", translationMode=" + translationMode +
                ", title='" + title + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
