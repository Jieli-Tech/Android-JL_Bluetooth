package com.jieli.btsmart.tool.ai.doubao.translate.model;

import androidx.annotation.NonNull;

import com.jieli.btsmart.data.model.translation.RoleInfo;

import java.util.List;

/**
 * TranslationTextParam
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译文本参数
 * @since 2025/9/4
 */
public class TranslateTextParam extends TranslateParam {

    /**
     * 原文语言
     * <p>
     * 若不填，则为服务器自动推测
     * </p>
     */
    private final String srcLanguage;
    /**
     * 翻译语言
     */
    @NonNull
    private final String targetLanguage;
    /**
     * 原文
     */
    @NonNull
    private final List<String> srcTextList;


    public TranslateTextParam(@NonNull String mac, @NonNull RoleInfo roleInfo, int recordId,
                              String srcLanguage, @NonNull String targetLanguage,
                              @NonNull List<String> srcTextList) {
        super(mac, roleInfo, true, recordId);
        this.srcLanguage = srcLanguage;
        this.targetLanguage = targetLanguage;
        this.srcTextList = srcTextList;
    }

    @Override
    public int getTranslationWay() {
        return WAY_TEXT;
    }

    public String getSrcLanguage() {
        return srcLanguage;
    }

    @NonNull
    public String getTargetLanguage() {
        return targetLanguage;
    }

    @NonNull
    public List<String> getSrcTextList() {
        return srcTextList;
    }

    @Override
    public String toString() {
        return "TranslateTextParam{" +
                "mac='" + getMac() + '\'' +
                ", roleInfo=" + getRoleInfo() +
                ", isIdAutoInc=" + isIdAutoInc() +
                ", srcLanguage='" + srcLanguage + '\'' +
                ", targetLanguage='" + targetLanguage + '\'' +
                ", srcTextList=" + srcTextList +
                '}';
    }
}
