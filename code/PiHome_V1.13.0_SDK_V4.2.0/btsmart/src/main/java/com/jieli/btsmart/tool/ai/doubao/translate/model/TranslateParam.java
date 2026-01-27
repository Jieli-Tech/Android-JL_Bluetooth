package com.jieli.btsmart.tool.ai.doubao.translate.model;

import androidx.annotation.NonNull;

import com.jieli.btsmart.data.model.translation.RoleInfo;
import com.jieli.btsmart.data.model.translation.TranslationRecord;

import java.util.Random;

/**
 * TranslationParam
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译参数
 * @since 2025/9/4
 */
public abstract class TranslateParam {

    /**
     * 翻译音频流方式
     */
    public static final int WAY_AUDIO_STREAM = 1;
    /**
     * 翻译文本方式
     */
    public static final int WAY_TEXT = 2;

    /**
     * 设备地址
     */
    @NonNull
    private final String mac;
    /**
     * 角色信息
     */
    @NonNull
    private final RoleInfo roleInfo;
    /**
     * 是都记录ID自动增加
     */
    private final boolean isIdAutoInc;
    /**
     * 记录ID
     */
    private int recordId;
    /**
     * 翻译记录
     */
    private TranslationRecord translationRecord;
    /**
     * 已保存记录ID
     */
    private int saveRecordId = -1;


    public TranslateParam(@NonNull String mac, @NonNull RoleInfo roleInfo, boolean isIdAutoInc, int recordId) {
        this.mac = mac;
        this.roleInfo = roleInfo;
        this.isIdAutoInc = isIdAutoInc;
        if (recordId < 0) {
            this.recordId = new Random().nextInt(256);
        } else {
            this.recordId = recordId;
        }
    }

    /**
     * 获取翻译方式
     *
     * @return int 翻译方式
     */
    public abstract int getTranslationWay();

    @NonNull
    public String getMac() {
        return mac;
    }

    @NonNull
    public RoleInfo getRoleInfo() {
        return roleInfo;
    }

    public boolean isIdAutoInc() {
        return isIdAutoInc;
    }

    public int getRecordId() {
        return recordId;
    }

    public int getNextRecordId() {
        int id = recordId;
        if (isIdAutoInc) {
            id++;
            id = id % 65536;
            recordId = id;
        }
        return id;
    }

    public TranslationRecord getTranslationRecord() {
        return translationRecord;
    }

    public void setTranslationRecord(TranslationRecord translationRecord) {
        this.translationRecord = translationRecord;
    }

    public int getSaveRecordId() {
        return saveRecordId;
    }

    public void setSaveRecordId(int saveRecordId) {
        this.saveRecordId = saveRecordId;
    }

    public boolean isSaveRecord() {
        if (null == translationRecord) return false;
        return translationRecord.getId() == saveRecordId;
    }

    @Override
    public String toString() {
        return "TranslationParam{" +
                "mac='" + mac + '\'' +
                ", roleInfo=" + roleInfo +
                ", translationWay=" + getTranslationWay() +
                ", isIdAutoInc=" + isIdAutoInc +
                ", recordId=" + recordId +
                ",\n translationRecord=" + translationRecord +
                ", saveRecordId=" + saveRecordId +
                '}';
    }
}
