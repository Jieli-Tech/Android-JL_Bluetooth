package com.jieli.btsmart.tool.ai.doubao.tts.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;

/**
 * Request
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 请求配置
 * @since 2025/6/24
 */
public class Request implements Parcelable {

    /**
     * 非流式
     * <p>
     * http 只能 query
     * </p>
     */
    public static final String OP_QUERY = "query";
    /**
     * 流式
     */
    public static final String OP_SUBMIT = "submit";

    /**
     * 请求ID
     *
     * <p>必要项</p>
     */
    @SerializedName("reqid")
    private final String reqID;
    /**
     * 合成文本
     *
     * <p>必要项</p>
     */
    private final String text;
    /**
     * 文本类型
     *
     * <p>可选项</p>
     */
    @SerializedName("text_type")
    private final String textType;
    /**
     * 操作
     *
     * <p>必要项</p>
     */
    private final String operation;
    /**
     * 是否使能尾句静音
     *
     * <p>
     * 可选项<br/>
     * 该字段为true时，[silenceDuration]才会生效
     * </p>
     */
    private final Boolean enableTrailingSilenceAudio;
    /**
     * 句尾静音
     *
     * <p>
     * 可选项<br/>
     * 取值范围: [[0, 30000]]ms
     * </p>
     */
    private final Integer silenceDuration;
    /**
     * 附加参数
     *
     * <p>可选项</p>
     */
    @SerializedName("extra_param")
    private final String extraParam;
    /**
     * 是否开启markdown解析过滤，
     *
     * <p>可选项</p>
     */
    @SerializedName("disable_markdown_filter")
    private final Boolean disableMarkdownFilter;

    public Request(String reqID, String text) {
        this(reqID, text, null, OP_SUBMIT);
    }

    public Request(String reqID, String text, String textType, String operation) {
        this(reqID, text, textType, operation, null, null, null, null);
    }

    public Request(String reqID, String text, String textType, String operation, Boolean enableTrailingSilenceAudio,
                   Integer silenceDuration, String extraParam, Boolean disableMarkdownFilter) {
        this.reqID = reqID;
        this.text = text;
        this.textType = textType;
        this.operation = operation;
        this.enableTrailingSilenceAudio = enableTrailingSilenceAudio;
        this.silenceDuration = silenceDuration;
        this.extraParam = extraParam;
        this.disableMarkdownFilter = disableMarkdownFilter;
    }

    protected Request(Parcel in) {
        reqID = in.readString();
        text = in.readString();
        textType = in.readString();
        operation = in.readString();
        byte tmpEnableTrailingSilenceAudio = in.readByte();
        enableTrailingSilenceAudio = tmpEnableTrailingSilenceAudio == 0 ? null : tmpEnableTrailingSilenceAudio == 1;
        if (in.readByte() == 0) {
            silenceDuration = null;
        } else {
            silenceDuration = in.readInt();
        }
        extraParam = in.readString();
        byte tmpDisableMarkdownFilter = in.readByte();
        disableMarkdownFilter = tmpDisableMarkdownFilter == 0 ? null : tmpDisableMarkdownFilter == 1;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(reqID);
        dest.writeString(text);
        dest.writeString(textType);
        dest.writeString(operation);
        dest.writeByte((byte) (enableTrailingSilenceAudio == null ? 0 : enableTrailingSilenceAudio ? 1 : 2));
        if (silenceDuration == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(silenceDuration);
        }
        dest.writeString(extraParam);
        dest.writeByte((byte) (disableMarkdownFilter == null ? 0 : disableMarkdownFilter ? 1 : 2));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Request> CREATOR = new Creator<Request>() {
        @Override
        public Request createFromParcel(Parcel in) {
            return new Request(in);
        }

        @Override
        public Request[] newArray(int size) {
            return new Request[size];
        }
    };

    public String getReqID() {
        return reqID;
    }

    public String getText() {
        return text;
    }

    public String getTextType() {
        return textType;
    }

    public String getOperation() {
        return operation;
    }

    public Boolean getEnableTrailingSilenceAudio() {
        return enableTrailingSilenceAudio;
    }

    public Integer getSilenceDuration() {
        return silenceDuration;
    }

    public String getExtraParam() {
        return extraParam;
    }

    public Boolean getDisableMarkdownFilter() {
        return disableMarkdownFilter;
    }

    @Override
    public String toString() {
        return AIConfig.gson.toJson(this);
    }
}
