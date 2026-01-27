package com.jieli.btsmart.tool.ai.doubao.tts.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * User
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 用户信息
 * @since 2025/6/25
 */
public class User implements Parcelable {

    /**
     * 用户标识
     *
     * <p>必要项</p>
     */
    private final String uid;

    public User(String uid) {
        this.uid = uid;
    }

    protected User(Parcel in) {
        uid = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getUid() {
        return uid;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
