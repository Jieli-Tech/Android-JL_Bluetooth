package com.jieli.btsmart.data.model.translation;

import com.jieli.bluetooth.bean.translation.TranslationMode;

/**
 * TranslationModeInfo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译模式信息
 * @since 2025/5/27
 */
public class TranslationModeInfo {
    private final int iconId;
    private final String title;
    private final String desc;
    private final boolean isShowIcon;
    private final TranslationMode mode;

    public TranslationModeInfo(int iconId, String title, String desc, TranslationMode mode) {
        this(iconId, title, desc, true, mode);
    }

    public TranslationModeInfo(int iconId, String title, String desc, boolean isShowIcon, TranslationMode mode) {
        this.iconId = iconId;
        this.title = title;
        this.desc = desc;
        this.isShowIcon = isShowIcon;
        this.mode = mode;
    }

    public int getIconId() {
        return iconId;
    }

    public String getTitle() {
        return title;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isShowIcon() {
        return isShowIcon;
    }

    public TranslationMode getMode() {
        return mode;
    }

    @Override
    public String toString() {
        return "TranslationModeInfo{" +
                "iconId=" + iconId +
                ", title='" + title + '\'' +
                ", desc='" + desc + '\'' +
                ", isShowIcon=" + isShowIcon +
                ", mode=" + mode +
                '}';
    }
}
