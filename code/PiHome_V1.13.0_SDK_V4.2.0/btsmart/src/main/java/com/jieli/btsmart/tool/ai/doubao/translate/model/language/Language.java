package com.jieli.btsmart.tool.ai.doubao.translate.model.language;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * Language
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 语言
 * @since 2025/6/24
 */
public class Language implements Parcelable {

    /**
     * 中文(简体)
     */
    public static final String LANG_ZH = "zh";
    /**
     * 中文(繁体)
     */
    public static final String LANG_ZH_HANT = "zh-Hant";
    /**
     * 英语
     */
    public static final String LANG_EN = "en";
    /**
     * 日语
     */
    public static final String LANG_JA = "ja";
    /**
     * 墨西哥语
     */
    public static final String LANG_ES = "es-mx";
    /**
     * 印尼语
     */
    public static final String LANG_ID = "id";
    /**
     * 巴葡语
     */
    public static final String LANG_PT_BR = "pt-br";

    /**
     * 获取面对面翻译提示语
     *
     * @param langCode String 语言标识
     * @return String 面对面翻译提示语
     */
    public static String getFaceToFaceTips(@NonNull String langCode) {
        switch (langCode) {
            case LANG_ZH:
            case LANG_ZH_HANT:
                return "请佩戴好耳机，单击下方按钮开始翻译";
            case LANG_JA:
                return "ヘッドフォンを装着して、下のボタンをクリックして翻訳を開始してください。";
            default:
                return "Please put on your headphones and click the button below to start translating.";
        }
    }

    /**
     * 获取通话翻译提示语
     *
     * @param langCode String 语言标识
     * @return String 通话翻译提示语
     */
    public static String getCallTranslationTips(@NonNull String langCode){
        switch (langCode) {
            case LANG_ZH:
            case LANG_ZH_HANT:
                return "你好，我的口语不好，会由翻译代我与你沟通，请见谅";
            case LANG_JA:
                return "こんにちは。私の話し言葉はあまり上手ではないので、代わりに通訳があなたとコミュニケーションをとることになります。ご容赦ください。";
            default:
                return "Hello,my spoken language is not very good,so a translator will be communicating with you on my behalf.Please excuse me.";
        }
    }


    /**
     * 语言代号
     */
    private final String code;
    /**
     * 语言资源ID
     */
    private final int nameID;

    public Language(String code, int nameID) {
        this.code = code;
        this.nameID = nameID;
    }

    protected Language(Parcel in) {
        code = in.readString();
        nameID = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(code);
        dest.writeInt(nameID);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Language> CREATOR = new Creator<Language>() {
        @Override
        public Language createFromParcel(Parcel in) {
            return new Language(in);
        }

        @Override
        public Language[] newArray(int size) {
            return new Language[size];
        }
    };

    public String getCode() {
        return code;
    }

    public int getNameID() {
        return nameID;
    }


    @Override
    public String toString() {
        return "Language{" +
                "code='" + code + '\'' +
                ", nameID=" + nameID +
                '}';
    }
}
