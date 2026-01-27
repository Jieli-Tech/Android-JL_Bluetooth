package com.jieli.btsmart.data.model.translation;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.btsmart.R;

/**
 * RoleInfo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 角色信息
 * @since 2025/8/12
 */
public class RoleInfo implements Parcelable {

    /**
     * 手机端
     */
    public static final int ROLE_PHONE = 0;
    /**
     * 设备端
     */
    public static final int ROLE_DEVICE = 1;

    public static String printRoleName(int role) {
        switch (role) {
            case ROLE_PHONE:
                return "ROLE_PHONE(0)";
            case ROLE_DEVICE:
                return "ROLE_DEVICE(1)";
            default:
                return "UNKNOWN_ROLE(" + role + ")";
        }
    }

    /**
     * 翻译模式
     */
    @TranslationMode.Mode
    private final int translationMode;
    /**
     * 角色
     */
    private final int role;
    /**
     * 手机号码
     */
    private String mobileNumber = "";

    public RoleInfo(@TranslationMode.Mode int translationMode, int role) {
        this.translationMode = translationMode;
        this.role = role;
    }

    protected RoleInfo(Parcel in) {
        translationMode = in.readInt();
        role = in.readInt();
        mobileNumber = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(translationMode);
        dest.writeInt(role);
        dest.writeString(mobileNumber);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RoleInfo> CREATOR = new Creator<RoleInfo>() {
        @Override
        public RoleInfo createFromParcel(Parcel in) {
            return new RoleInfo(in);
        }

        @Override
        public RoleInfo[] newArray(int size) {
            return new RoleInfo[size];
        }
    };

    @TranslationMode.Mode
    public int getTranslationMode() {
        return translationMode;
    }

    public int getRole() {
        return role;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getRoleName(@NonNull Context context) {
        switch (translationMode) {
            case TranslationMode.MODE_CALL_TRANSLATION:
                String name = role == ROLE_DEVICE ? context.getString(R.string.other_side)
                        : context.getString(R.string.our_side);
                String flag = "";
                if (!TextUtils.isEmpty(mobileNumber)) {
                    flag = CommonUtil.formatString("(%s)", mobileNumber);
                }
                return name + flag;
            case TranslationMode.MODE_FACE_TO_FACE_TRANSLATION:
            case TranslationMode.MODE_RECORDING_TRANSLATION:
            case TranslationMode.MODE_AUDIO_TRANSLATION:
                return role == ROLE_DEVICE ? context.getString(R.string.headset_side)
                        : context.getString(R.string.mobile_side);
            default:
                return "";
        }
    }

    public boolean isUseSpeaker() {
        return translationMode == TranslationMode.MODE_FACE_TO_FACE_TRANSLATION && role == ROLE_DEVICE;
    }

    @Override
    public String toString() {
        return "RoleInfo{" +
                "translationMode=" + translationMode +
                ", role=" + printRoleName(role) +
                ", mobileNumber='" + mobileNumber + '\'' +
                '}';
    }
}
