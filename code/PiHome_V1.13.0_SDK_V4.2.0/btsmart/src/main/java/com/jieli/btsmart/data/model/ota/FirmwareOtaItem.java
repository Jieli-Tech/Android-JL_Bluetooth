package com.jieli.btsmart.data.model.ota;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.entity.MultiItemEntity;

/**
 * 固件升级数据模型
 *
 * @author zqjasonZhong
 * @since 2020/5/19
 */
public class FirmwareOtaItem implements MultiItemEntity {
    private int itemType;
    private String content;
    private String value;
    private OtaStageInfo otaStageInfo;
    private boolean isShowIcon;

    public final static int LAYOUT_ONE = 0;
    public final static int LAYOUT_TWO = 1;

    @Override
    public int getItemType() {
        return itemType;
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public OtaStageInfo getOtaStageInfo() {
        return otaStageInfo;
    }

    public void setOtaStageInfo(OtaStageInfo otaStageInfo) {
        this.otaStageInfo = otaStageInfo;
    }

    public boolean isShowIcon() {
        return isShowIcon;
    }

    public void setShowIcon(boolean showIcon) {
        isShowIcon = showIcon;
    }

    @NonNull
    @Override
    public String toString() {
        return "FirmwareOtaItem{" +
                "itemType=" + itemType +
                ", content='" + content + '\'' +
                ", value='" + value + '\'' +
                ", otaStageInfo=" + otaStageInfo +
                ", isShowIcon=" + isShowIcon +
                '}';
    }
}
