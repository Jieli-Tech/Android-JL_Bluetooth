package com.jieli.btsmart.tool.ai.doubao.translate.model.request;

import android.os.Parcel;
import android.os.Parcelable;

import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * StopTranslateRequest
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 停止翻译指令
 * @since 2025/6/24
 */
public class StopTranslateRequest implements Parcelable {

    /**
     * 是否发完音频数据
     */
    private final boolean End;

    public StopTranslateRequest() {
        this(true);
    }

    public StopTranslateRequest(boolean end) {
        End = end;
    }

    protected StopTranslateRequest(Parcel in) {
        End = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (End ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<StopTranslateRequest> CREATOR = new Creator<StopTranslateRequest>() {
        @Override
        public StopTranslateRequest createFromParcel(Parcel in) {
            return new StopTranslateRequest(in);
        }

        @Override
        public StopTranslateRequest[] newArray(int size) {
            return new StopTranslateRequest[size];
        }
    };

    public boolean isEnd() {
        return End;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
