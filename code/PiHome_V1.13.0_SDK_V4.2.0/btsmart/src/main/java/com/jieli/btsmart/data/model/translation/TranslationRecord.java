package com.jieli.btsmart.data.model.translation;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.jieli.bluetooth.utils.CHexConver;

import java.util.Objects;

/**
 * TranslationRecord
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译记录
 * @since 2025/6/9
 */
@Entity
public class TranslationRecord implements Parcelable {

    /**
     * 唯一码
     */
    @PrimaryKey(autoGenerate = true)
    private int id;
    /**
     * 设备地址
     */
    private String mac;
    /**
     * 会话ID
     */
    private int sessionId;
    /**
     * 角色
     */
    private int role;
    /**
     * 昵称
     */
    private String nikeName;
    /**
     * 原文
     */
    private String srcText;
    /**
     * 原文语言
     */
    private String srcLanguage;
    /**
     * 原文播放文件路径
     */
    private String srcFilePath;
    /**
     * 原文文件时长
     */
    private int srcFileDuration;
    /**
     * 译文
     */
    private String destText;
    /**
     * 目标语言
     */
    private String destLanguage;
    /**
     * 译文文件路径
     */
    private String destFilePath;
    /**
     * 译文文件时长
     */
    private int destFileDuration;
    /**
     * 更新时间戳
     */
    private long updateTime;

    public TranslationRecord() {
    }

    protected TranslationRecord(Parcel in) {
        id = in.readInt();
        mac = in.readString();
        sessionId = in.readInt();
        role = in.readInt();
        nikeName = in.readString();
        srcText = in.readString();
        srcLanguage = in.readString();
        srcFilePath = in.readString();
        srcFileDuration = in.readInt();
        destText = in.readString();
        destLanguage = in.readString();
        destFilePath = in.readString();
        destFileDuration = in.readInt();
        updateTime = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(mac);
        dest.writeInt(sessionId);
        dest.writeInt(role);
        dest.writeString(nikeName);
        dest.writeString(srcText);
        dest.writeString(srcLanguage);
        dest.writeString(srcFilePath);
        dest.writeInt(srcFileDuration);
        dest.writeString(destText);
        dest.writeString(destLanguage);
        dest.writeString(destFilePath);
        dest.writeInt(destFileDuration);
        dest.writeLong(updateTime);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TranslationRecord> CREATOR = new Creator<TranslationRecord>() {
        @Override
        public TranslationRecord createFromParcel(Parcel in) {
            return new TranslationRecord(in);
        }

        @Override
        public TranslationRecord[] newArray(int size) {
            return new TranslationRecord[size];
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

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public String getNikeName() {
        return nikeName;
    }

    public void setNikeName(String nikeName) {
        this.nikeName = nikeName;
    }

    public String getSrcText() {
        return srcText;
    }

    public void setSrcText(String srcText) {
        this.srcText = srcText;
    }

    public String getSrcLanguage() {
        return srcLanguage;
    }

    public void setSrcLanguage(String srcLanguage) {
        this.srcLanguage = srcLanguage;
    }

    public String getSrcFilePath() {
        return srcFilePath;
    }

    public void setSrcFilePath(String srcFilePath) {
        this.srcFilePath = srcFilePath;
    }

    public int getSrcFileDuration() {
        return srcFileDuration;
    }

    public void setSrcFileDuration(int srcFileDuration) {
        this.srcFileDuration = srcFileDuration;
    }

    public String getDestText() {
        return destText;
    }

    public void setDestText(String destText) {
        this.destText = destText;
    }

    public String getDestLanguage() {
        return destLanguage;
    }

    public void setDestLanguage(String destLanguage) {
        this.destLanguage = destLanguage;
    }

    public String getDestFilePath() {
        return destFilePath;
    }

    public void setDestFilePath(String destFilePath) {
        this.destFilePath = destFilePath;
    }

    public int getDestFileDuration() {
        return destFileDuration;
    }

    public void setDestFileDuration(int destFileDuration) {
        this.destFileDuration = destFileDuration;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public TranslationRecord cloneObject() {
        TranslationRecord record = new TranslationRecord();
        record.setId(id);
        record.setMac(mac);
        record.setSessionId(sessionId);
        record.setRole(role);
        record.setNikeName(nikeName);
        record.setSrcText(srcText);
        record.setSrcLanguage(srcLanguage);
        record.setSrcFilePath(srcFilePath);
        record.setSrcFileDuration(srcFileDuration);
        record.setDestText(destText);
        record.setDestLanguage(destLanguage);
        record.setDestFilePath(destFilePath);
        record.setDestFileDuration(destFileDuration);
        record.setUpdateTime(updateTime);
        return record;
    }

    public boolean isValidRecord() {
        return id >= 0 && CHexConver.checkBluetoothAddress(mac)
                && sessionId >= 0 && role >= 0 && !TextUtils.isEmpty(srcText)
                && !TextUtils.isEmpty(destText) && updateTime > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TranslationRecord record = (TranslationRecord) o;
        return id == record.id && sessionId == record.sessionId && Objects.equals(mac, record.mac);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, mac, sessionId);
    }

    @Override
    public String toString() {
        return "TranslationRecord{" +
                "id=" + id +
                ", mac='" + mac + '\'' +
                ", sessionId=" + sessionId +
                ", role=" + role +
                ", nikeName='" + nikeName + '\'' +
                ", srcText='" + srcText + '\'' +
                ", srcLanguage='" + srcLanguage + '\'' +
                ", srcFilePath='" + srcFilePath + '\'' +
                ", srcFileDuration=" + srcFileDuration +
                ", destText='" + destText + '\'' +
                ", destLanguage='" + destLanguage + '\'' +
                ", destFilePath='" + destFilePath + '\'' +
                ", destFileDuration=" + destFileDuration +
                ", updateTime=" + updateTime +
                '}';
    }
}
