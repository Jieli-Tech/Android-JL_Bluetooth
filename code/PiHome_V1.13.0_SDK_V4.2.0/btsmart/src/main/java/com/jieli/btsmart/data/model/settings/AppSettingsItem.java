package com.jieli.btsmart.data.model.settings;

/**
 * @ClassName: AppSettingsItem
 * @Description: java类作用描述
 * @Author: ZhangHuanMing
 * @CreateDate: 2021/1/29 17:33
 */
public class AppSettingsItem extends SettingsItem {
    public static final int TYPE_SINGLE = 1;
    public static final int TYPE_SWITCH = 2;
    private int settingType = TYPE_SINGLE;
    private Integer settingNameSrc;
    private Integer settingNoteSrc;
    private boolean enableState = false;
    private boolean visible = true;
    private String tailString = null;

    public int getSettingType() {
        return settingType;
    }

    public void setSettingType(int settingType) {
        this.settingType = settingType;
    }

    public Integer getSettingNameSrc() {
        return settingNameSrc;
    }

    public void setSettingNameSrc(Integer settingNameSrc) {
        this.settingNameSrc = settingNameSrc;
    }

    public Integer getSettingNoteSrc() {
        return settingNoteSrc;
    }

    public void setSettingNoteSrc(Integer settingNoteSrc) {
        this.settingNoteSrc = settingNoteSrc;
    }

    public boolean isEnableState() {
        return enableState;
    }

    public void setEnableState(boolean enableState) {
        this.enableState = enableState;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getTailString() {
        return tailString;
    }

    public void setTailString(String tailString) {
        this.tailString = tailString;
    }

    @Override
    public String toString() {
        return "AppSettingsItem{" +
                "settingType=" + settingType +
                ", settingNameSrc=" + settingNameSrc +
                ", settingNoteSrc=" + settingNoteSrc +
                ", enableState=" + enableState +
                ", visible=" + visible +
                ", tailString='" + tailString + '\'' +
                "} ";
    }
}
