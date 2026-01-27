package com.jieli.btsmart.data.model.language;

import java.util.Objects;

/**
 * LanguageInfo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 语言信息
 * @since 2025/5/27
 */
public class LanguageInfo {

    /**
     * 语言标识
     */
    private final String flag;
    /**
     * 语言文本
     */
    private final String language;

    public LanguageInfo(String flag, String language) {
        this.flag = flag;
        this.language = language;
    }

    public String getFlag() {
        return flag;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LanguageInfo that = (LanguageInfo) o;
        return Objects.equals(flag, that.flag);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(flag);
    }

    @Override
    public String toString() {
        return "LanguageInfo{" +
                "flag='" + flag + '\'' +
                ", language='" + language + '\'' +
                '}';
    }
}
