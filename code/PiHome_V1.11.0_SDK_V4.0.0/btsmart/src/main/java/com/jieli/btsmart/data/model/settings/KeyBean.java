package com.jieli.btsmart.data.model.settings;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.entity.JSectionEntity;
import com.jieli.bluetooth.constant.AttrAndFunCode;

/**
 * 按键数据
 *
 * @author zqjasonZhong
 * @since 2020/5/15
 */
@SuppressWarnings("UnusedReturnValue")
public class KeyBean extends JSectionEntity implements Parcelable {
    private int keyId;
    private int actionId;
    private int funcId;
    private String keyName;// 真实按键名字
    private String key;//按键描述
    private String action;
    private String function;
    private int resId;
    private boolean isShowIcon;
    private boolean isHideLine;
    private int attrType = AttrAndFunCode.ADV_TYPE_KEY_SETTINGS;

    public final static int LAYOUT_ONE = 0;
    public final static int LAYOUT_TWO = 1;
    private boolean isHeader = false;

    public KeyBean() {

    }

    public KeyBean(int keyId, String keyName, int actionId, String action, int funcId, String function) {
        this(keyId, keyName, actionId, action, funcId, function, AttrAndFunCode.ADV_TYPE_KEY_SETTINGS);
    }

    public KeyBean(int keyId, String keyName, int actionId, String action, int funcId, String function, int attrType) {
        setKeyId(keyId);
        setKeyName(keyName);
        setActionId(actionId);
        setAction(action);
        setFuncId(funcId);
        setFunction(function);
        setAttrType(attrType);
    }

    protected KeyBean(Parcel in) {
        keyId = in.readInt();
        actionId = in.readInt();
        funcId = in.readInt();
        keyName = in.readString();
        key = in.readString();
        action = in.readString();
        function = in.readString();
        resId = in.readInt();
        isHeader = in.readByte() != 0;
        isShowIcon = in.readByte() != 0;
        isHideLine = in.readByte() != 0;
        attrType = in.readInt();
    }

    public static final Creator<KeyBean> CREATOR = new Creator<KeyBean>() {
        @Override
        public KeyBean createFromParcel(Parcel in) {
            return new KeyBean(in);
        }

        @Override
        public KeyBean[] newArray(int size) {
            return new KeyBean[size];
        }
    };

    public int getKeyId() {
        return keyId;
    }

    public KeyBean setKeyId(int keyId) {
        this.keyId = keyId;
        return this;
    }

    public int getActionId() {
        return actionId;
    }

    public KeyBean setActionId(int actionId) {
        this.actionId = actionId;
        return this;
    }

    public int getFuncId() {
        return funcId;
    }

    public KeyBean setFuncId(int funcId) {
        this.funcId = funcId;
        return this;
    }

    public String getKeyName() {
        return keyName;
    }

    public KeyBean setKeyName(String keyName) {
        this.keyName = keyName;
        return this;
    }

    public String getKey() {
        return key;
    }

    public KeyBean setKey(String key) {
        this.key = key;
        return this;
    }

    public String getAction() {
        return action;
    }

    public KeyBean setAction(String action) {
        this.action = action;
        return this;
    }

    public String getFunction() {
        return function;
    }

    public KeyBean setFunction(String function) {
        this.function = function;
        return this;
    }

    public int getResId() {
        return resId;
    }

    public KeyBean setResId(int resId) {
        this.resId = resId;
        return this;
    }

    public boolean isShowIcon() {
        return isShowIcon;
    }

    public KeyBean setShowIcon(boolean showIcon) {
        isShowIcon = showIcon;
        return this;
    }

    public boolean isHideLine() {
        return isHideLine;
    }

    public KeyBean setHideLine(boolean hideLine) {
        isHideLine = hideLine;
        return this;
    }

    public int getAttrType() {
        return attrType;
    }

    public KeyBean setAttrType(int attrType) {
        this.attrType = attrType;
        return this;
    }

    public KeyBean setHeader(boolean header) {
        isHeader = header;
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return "KeyBean{" +
                ", keyId=" + keyId +
                ", actionId=" + actionId +
                ", funcId=" + funcId +
                ", keyName='" + keyName + '\'' +
                ", action='" + action + '\'' +
                ", function='" + function + '\'' +
                ", resId=" + resId +
                ", isShowIcon=" + isShowIcon +
                ", isHideLine=" + isHideLine +
                ", attrType=" + attrType +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(keyId);
        dest.writeInt(actionId);
        dest.writeInt(funcId);
        dest.writeString(keyName);
        dest.writeString(key);
        dest.writeString(action);
        dest.writeString(function);
        dest.writeInt(resId);
        dest.writeByte((byte) (isHeader ? 1 : 0));
        dest.writeByte((byte) (isShowIcon ? 1 : 0));
        dest.writeByte((byte) (isHideLine ? 1 : 0));
        dest.writeInt(attrType);
    }

    @Override
    public boolean isHeader() {
        return isHeader;
    }
}
