package com.jieli.btsmart.data.model;

public class FunctionItemData {
    private int supportIconResId;
    private int noSupportIconResId;
    private boolean isSupport;
    private String name;
    private int itemType;

    public int getSupportIconResId() {
        return supportIconResId;
    }

    public void setSupportIconResId(int supportIconResId) {
        this.supportIconResId = supportIconResId;
    }

    public int getNoSupportIconResId() {
        return noSupportIconResId;
    }

    public void setNoSupportIconResId(int noSupportIconResId) {
        this.noSupportIconResId = noSupportIconResId;
    }

    public boolean isSupport() {
        return isSupport;
    }

    public void setSupport(boolean support) {
        isSupport = support;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getItemType() {
        return itemType;
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }

    @Override
    public String toString() {
        return "FunctionItemData{" +
                "supportIconResId=" + supportIconResId +
                ", noSupportIconResId=" + noSupportIconResId +
                ", isSupport=" + isSupport +
                ", name='" + name + '\'' +
                ", itemType=" + itemType +
                '}';
    }

    public interface FunctionItemType {
        int FUNCTION_ITEM_TYPE_LOCAL = 0;
        int FUNCTION_ITEM_TYPE_USB = 1;
        int FUNCTION_ITEM_TYPE_SD_CARD = 2;
        int FUNCTION_ITEM_TYPE_FM_TX = 3;
        int FUNCTION_ITEM_TYPE_FM = 4;
        int FUNCTION_ITEM_TYPE_LINEIN = 5;
        int FUNCTION_ITEM_TYPE_LIGHT_SETTINGS = 6;
        int FUNCTION_ITEM_TYPE_ALARM = 7;
        int FUNCTION_ITEM_TYPE_SEARCH_DEVICE = 8;
        int FUNCTION_ITEM_TYPE_NET_RADIO = 9;
        int FUNCTION_ITEM_TYPE_SOUND_CARD = 10;
    }
}
