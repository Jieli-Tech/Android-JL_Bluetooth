package com.jieli.btsmart.data.model.translation;

import java.util.Objects;

/**
 * RecordType
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 记录类型
 * @since 2025/9/1
 */
public class RecordType {

    private int translationMode;
    private String title;
    private int resId;

    public int getTranslationMode() {
        return translationMode;
    }

    public RecordType setTranslationMode(int translationMode) {
        this.translationMode = translationMode;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public RecordType setTitle(String title) {
        this.title = title;
        return this;
    }

    public int getResId() {
        return resId;
    }

    public RecordType setResId(int resId) {
        this.resId = resId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RecordType type = (RecordType) o;
        return translationMode == type.translationMode;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(translationMode);
    }

    @Override
    public String toString() {
        return "RecordType{" +
                "translationMode=" + translationMode +
                ", title='" + title + '\'' +
                ", resId=" + resId +
                '}';
    }
}
