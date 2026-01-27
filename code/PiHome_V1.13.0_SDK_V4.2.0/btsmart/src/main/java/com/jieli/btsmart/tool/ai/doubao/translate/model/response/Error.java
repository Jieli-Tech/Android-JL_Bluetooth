package com.jieli.btsmart.tool.ai.doubao.translate.model.response;

import android.os.Parcel;
import android.os.Parcelable;

import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * Error
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 错误信息
 * @since 2025/6/24
 */
public class Error implements Parcelable {
    /**
     * 错误码
     */
    private final int CodeN;
    /**
     * 错误描述
     */
    private final String Code;
    /**
     * 错误信息
     */
    private final String Message;

    public Error(int codeN, String code, String message) {
        CodeN = codeN;
        Code = code;
        Message = message;
    }

    protected Error(Parcel in) {
        CodeN = in.readInt();
        Code = in.readString();
        Message = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(CodeN);
        dest.writeString(Code);
        dest.writeString(Message);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Error> CREATOR = new Creator<Error>() {
        @Override
        public Error createFromParcel(Parcel in) {
            return new Error(in);
        }

        @Override
        public Error[] newArray(int size) {
            return new Error[size];
        }
    };

    public int getCodeN() {
        return CodeN;
    }

    public String getCode() {
        return Code;
    }

    public String getMessage() {
        return Message;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
