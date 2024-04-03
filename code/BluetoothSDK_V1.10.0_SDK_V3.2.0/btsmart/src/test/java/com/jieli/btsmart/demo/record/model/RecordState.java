package com.jieli.btsmart.demo.record.model;

import com.jieli.bluetooth.utils.CHexConver;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 录音状态
 * @since 2022/9/27
 */
public class RecordState {
    /**
     * 录音状态 -- 空闲状态
     */
    public static final int RECORD_STATE_IDLE = 0;
    /**
     * 录音状态 -- 开始状态
     */
    public static final int RECORD_STATE_START = 1;
    /**
     * 录音状态 -- 工作状态
     */
    public static final int RECORD_STATE_WORKING = 2;

    /**
     * 正常结束
     */
    public static final int REASON_NORMAL = 0;
    /**
     * 主动结束
     */
    public static final int REASON_STOP = 1;

    private int state = RECORD_STATE_IDLE;  //状态
    private RecordParam recordParam;        //录音参数
    private byte[] voiceDataBlock;          //音频内容块
    private int reason = REASON_NORMAL;     //结束原因
    private byte[] voiceData;               //音频内容
    private String message;                 //错误描述

    public int getState() {
        return state;
    }

    public RecordState setState(int state) {
        this.state = state;
        return this;
    }

    public RecordParam getRecordParam() {
        return recordParam;
    }

    public RecordState setRecordParam(RecordParam recordParam) {
        this.recordParam = recordParam;
        return this;
    }

    public byte[] getVoiceDataBlock() {
        return voiceDataBlock;
    }

    public RecordState setVoiceDataBlock(byte[] voiceDataBlock) {
        this.voiceDataBlock = voiceDataBlock;
        return this;
    }

    public byte[] getVoiceData() {
        return voiceData;
    }

    public RecordState setVoiceData(byte[] voiceData) {
        this.voiceData = voiceData;
        return this;
    }

    public int getReason() {
        return reason;
    }

    public RecordState setReason(int reason) {
        this.reason = reason;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public RecordState setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public String toString() {
        return "RecordState{" +
                "state=" + state +
                ", recordParam=" + recordParam +
                ", voiceDataBlock=" + CHexConver.byte2HexStr(voiceDataBlock) +
                ", reason=" + reason +
                ", voiceData=" + CHexConver.byte2HexStr(voiceData) +
                ", message='" + message + '\'' +
                '}';
    }
}
