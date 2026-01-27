package com.jieli.btsmart.tool.ai.doubao.tts.model;

import com.jieli.bluetooth.interfaces.IDataOp;
import com.jieli.bluetooth.utils.CHexConver;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * TtsResponse
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc TTS回复数据
 * @since 2025/6/25
 */
public class TtsResponse implements IDataOp {
    /**
     * 协议版本
     */
    private int protocolVersion = 0;

    /**
     * 报头大小
     */
    private int headerSize = 0;

    /**
     * 消息类型
     */
    private int messageType = 0;

    /**
     * 消息类型标识
     */
    private int messageTypeSpecificFlags = 0;

    /**
     * 序列化方法
     */
    private int serializationMethod = 0;

    /**
     * 压缩方法
     */
    private int messageCompression = 0;

    /**
     * 保留字段
     */
    private int reserved = 0;

    /**
     * 序列号
     */
    private int sequenceNumber = 0;

    /**
     * 有效数据
     */
    private byte[] payload = new byte[0];

    /**
     * 错误码
     */
    private int code = -1;

    /**
     * 错误描述
     */
    private String message = "";

    public TtsResponse() {
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public TtsResponse setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
        return this;
    }

    public int getHeaderSize() {
        return headerSize;
    }

    public TtsResponse setHeaderSize(int headerSize) {
        this.headerSize = headerSize;
        return this;
    }

    public int getMessageType() {
        return messageType;
    }

    public TtsResponse setMessageType(int messageType) {
        this.messageType = messageType;
        return this;
    }

    public int getMessageTypeSpecificFlags() {
        return messageTypeSpecificFlags;
    }

    public TtsResponse setMessageTypeSpecificFlags(int messageTypeSpecificFlags) {
        this.messageTypeSpecificFlags = messageTypeSpecificFlags;
        return this;
    }

    public int getSerializationMethod() {
        return serializationMethod;
    }

    public TtsResponse setSerializationMethod(int serializationMethod) {
        this.serializationMethod = serializationMethod;
        return this;
    }

    public int getMessageCompression() {
        return messageCompression;
    }

    public TtsResponse setMessageCompression(int messageCompression) {
        this.messageCompression = messageCompression;
        return this;
    }

    public int getReserved() {
        return reserved;
    }

    public TtsResponse setReserved(int reserved) {
        this.reserved = reserved;
        return this;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public TtsResponse setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
        return this;
    }

    public byte[] getPayload() {
        return payload;
    }

    public TtsResponse setPayload(byte[] payload) {
        this.payload = payload;
        return this;
    }

    public int getCode() {
        return code;
    }

    public TtsResponse setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public TtsResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public byte[] toData() {
        return new byte[0];
    }

    @Override
    public int parseData(byte[] data) {
        if (null == data || data.length < 4) return 0;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int value = CHexConver.byteToInt(data[0]);
        protocolVersion = (value >> 4 & 0xFF);
        headerSize = (value & 0x0F);
        value = CHexConver.byteToInt(data[1]);
        messageType = (value >> 4 & 0xFF);
        messageTypeSpecificFlags = (value & 0x0F);
        value = CHexConver.byteToInt(data[2]);
        serializationMethod = (value >> 4 & 0xFF);
        messageCompression = (value & 0x0F);
        reserved = CHexConver.byteToInt(data[3]);
        buffer.position(headerSize * 4); //跳过协议头
        switch (messageType) {  // Audio-only server response
            case 11: {
                if (messageTypeSpecificFlags != 0) {
                    if (buffer.remaining() >= 4) {
                        sequenceNumber = buffer.getInt();
                    }
                    int payloadSize = 0;
                    if (buffer.remaining() >= 4) {
                        payloadSize = buffer.getInt();
                    }
                    if (payloadSize > 0 && buffer.remaining() >= payloadSize) {
                        payload = new byte[payloadSize];
                        buffer.get(payload);
                    }
                } else {
                    // Ack without audio data
                }
                break;
            }

            case 15: {  // Error message from server
                if (buffer.remaining() >= 4) {
                    code = buffer.getInt();
                }
                int messageSize = 0;
                if (buffer.remaining() >= 4) {
                    messageSize = buffer.getInt();
                }
                if (messageSize > 0 && buffer.remaining() >= messageSize) {
                    byte[] buf = new byte[messageSize];
                    buffer.get(buf);
                    try {
                        message = new String(buf, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
        return buffer.position();
    }

    @Override
    public String toString() {
        return "TtsResponse{" +
                "protocolVersion=" + protocolVersion +
                ", headerSize=" + headerSize +
                ", messageType=" + messageType +
                ", messageTypeSpecificFlags=" + messageTypeSpecificFlags +
                ", serializationMethod=" + serializationMethod +
                ", messageCompression=" + messageCompression +
                ", reserved=" + reserved +
                ", sequenceNumber=" + sequenceNumber +
                ", payload=" + CHexConver.byte2HexStr(payload) +
                ", code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
