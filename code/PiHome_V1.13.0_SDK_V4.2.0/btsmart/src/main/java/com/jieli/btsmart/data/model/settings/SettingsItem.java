package com.jieli.btsmart.data.model.settings;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * 设置文字布局
 *
 * @author zqjasonZhong
 * @since 2020/5/15
 */
public class SettingsItem implements Parcelable {

    private int resId;
    private String name;
    private String value;
    private int type;
    private int valueId = 0;
    private boolean isShowIcon = true;

    public SettingsItem() {
        this(0, null, null, true);
    }

    public SettingsItem(int resId, String name, String value, boolean isShowIcon) {
        this(resId, name, value, 0, isShowIcon);
    }

    public SettingsItem(int resId, String name, String value, int valueId, boolean isShowIcon) {
        setResId(resId);
        setName(name);
        setValue(value);
        setValueId(valueId);
        setShowIcon(isShowIcon);
    }

    protected SettingsItem(Parcel in) {
        resId = in.readInt();
        name = in.readString();
        value = in.readString();
        type = in.readInt();
        valueId = in.readInt();
        isShowIcon = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(resId);
        dest.writeString(name);
        dest.writeString(value);
        dest.writeInt(type);
        dest.writeInt(valueId);
        dest.writeByte((byte) (isShowIcon ? 1 : 0));
    }

    public static final Creator<SettingsItem> CREATOR = new Creator<SettingsItem>() {
        @Override
        public SettingsItem createFromParcel(Parcel in) {
            return new SettingsItem(in);
        }

        @Override
        public SettingsItem[] newArray(int size) {
            return new SettingsItem[size];
        }
    };

    public int getResId() {
        return resId;
    }

    public SettingsItem setResId(int resId) {
        this.resId = resId;
        return this;
    }

    public String getName() {
        return name;
    }

    public SettingsItem setName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public SettingsItem setValue(String value) {
        this.value = value;
        return this;
    }

    public boolean isShowIcon() {
        return isShowIcon;
    }

    public SettingsItem setShowIcon(boolean showIcon) {
        isShowIcon = showIcon;
        return this;
    }

    public int getType() {
        return type;
    }

    public SettingsItem setType(int type) {
        this.type = type;
        return this;
    }

    public int getValueId() {
        return valueId;
    }

    public SettingsItem setValueId(int valueId) {
        this.valueId = valueId;
        return this;
    }
    @NonNull
    @Override
    public String toString() {
        return "SettingsItem{" +
                "resId=" + resId +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", type=" + type +
                ", valueId=" + valueId +
                ", isShowIcon=" + isShowIcon +
                '}';
    }
}
