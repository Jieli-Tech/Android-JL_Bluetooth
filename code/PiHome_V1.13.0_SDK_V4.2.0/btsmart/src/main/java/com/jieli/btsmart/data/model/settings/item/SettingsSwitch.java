package com.jieli.btsmart.data.model.settings.item;

import android.widget.CompoundButton;

/**
 * SettingsSwitch
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc  设置选择开关布局
 * @since 2025/2/28
 */
public class SettingsSwitch extends BaseItem {

    private String title;
    private int imgRes;
    private boolean isCheck;
    private CompoundButton.OnCheckedChangeListener listener = null;

    public SettingsSwitch(String title) {
        this(title, 0);
    }

    public SettingsSwitch(String title, int imgRes) {
        this(title, imgRes, false);
    }

    public SettingsSwitch(String title, int imgRes, boolean isCheck) {
        super(ITEM_SWITCH);
        this.title = title;
        this.imgRes = imgRes;
        this.isCheck = isCheck;
    }

    public String getTitle() {
        return title;
    }

    public SettingsSwitch setTitle(String title) {
        this.title = title;
        return this;
    }

    public int getImgRes() {
        return imgRes;
    }

    public SettingsSwitch setImgRes(int imgRes) {
        this.imgRes = imgRes;
        return this;
    }

    public boolean isCheck() {
        return isCheck;
    }

    public SettingsSwitch setCheck(boolean check) {
        isCheck = check;
        return this;
    }

    public CompoundButton.OnCheckedChangeListener getListener() {
        return listener;
    }

    public SettingsSwitch setListener(CompoundButton.OnCheckedChangeListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public String toString() {
        return "SettingsSwitch{" +
                "title='" + title + '\'' +
                ", imgRes=" + imgRes +
                ", isCheck=" + isCheck +
                ", listener=" + listener +
                '}';
    }
}
