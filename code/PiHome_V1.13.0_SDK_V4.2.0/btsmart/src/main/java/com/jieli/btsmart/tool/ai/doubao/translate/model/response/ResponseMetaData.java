package com.jieli.btsmart.tool.ai.doubao.translate.model.response;

import android.os.Parcel;
import android.os.Parcelable;

import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * ResponseMetaData
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 回复元数据
 * @since 2025/6/24
 */
public class ResponseMetaData implements Parcelable {
    /**
     * 请求ID
     */
    private final String RequestId;
    /**
     * 操作行为
     */
    private final String Action;
    /**
     * 版本
     */
    private final String Version;
    /**
     * 服务类型
     */
    private final String Service;
    /**
     * 地区
     */
    private final String Region;
    /**
     * 错误信息
     */
    private final Error Error;

    public ResponseMetaData(String requestId, String action, String version, String service,
                            String region, Error error) {
        RequestId = requestId;
        Action = action;
        Version = version;
        Service = service;
        Region = region;
        Error = error;
    }

    protected ResponseMetaData(Parcel in) {
        RequestId = in.readString();
        Action = in.readString();
        Version = in.readString();
        Service = in.readString();
        Region = in.readString();
        Error = in.readParcelable(Error.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(RequestId);
        dest.writeString(Action);
        dest.writeString(Version);
        dest.writeString(Service);
        dest.writeString(Region);
        dest.writeParcelable(Error, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ResponseMetaData> CREATOR = new Creator<ResponseMetaData>() {
        @Override
        public ResponseMetaData createFromParcel(Parcel in) {
            return new ResponseMetaData(in);
        }

        @Override
        public ResponseMetaData[] newArray(int size) {
            return new ResponseMetaData[size];
        }
    };

    public String getRequestId() {
        return RequestId;
    }

    public String getAction() {
        return Action;
    }

    public String getVersion() {
        return Version;
    }

    public String getService() {
        return Service;
    }

    public String getRegion() {
        return Region;
    }

    public Error getError() {
        return Error;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
