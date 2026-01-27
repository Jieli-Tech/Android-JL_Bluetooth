package com.jieli.btsmart.util;

import android.media.AudioFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * PcmKit
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/12/26
 * note: PCM工具栏
 */
public class PcmKit {


    /**
     * 分离立体声PCM数据(默认是PCM_16Bit)
     *
     * @param pcmData byte[] 立体声PCM数据
     * @return byte[] 立体声PCM数据
     * <p>
     *     固定为2个ByteArray的列表，第一个是左声道，第二个是右声道
     * </p>
     */
    public static byte[][] splitStereoPcmData(byte[] pcmData) {
        return splitStereoPcmData(pcmData, AudioFormat.ENCODING_PCM_16BIT);
    }

    /**
     * 分离立体声PCM数据
     *
     * @param pcmData byte[] 立体声PCM数据
     * @param bitRate int 编码深度
     * @return byte[][] 数据列表
     * <p>
     *     固定为2个ByteArray的列表，第一个是左声道，第二个是右声道
     * </p>
     */
    public static byte[][] splitStereoPcmData(byte[] pcmData, int bitRate) {
        if (null == pcmData || pcmData.length < bitRate * 2) return new byte[2][0];
        ByteArrayOutputStream leftData = new ByteArrayOutputStream();
        ByteArrayOutputStream rightData = new ByteArrayOutputStream();
        ByteBuffer dataBuf = ByteBuffer.wrap(pcmData);
        byte[] buf = new byte[bitRate * 2];
        try {
            while (dataBuf.remaining() >= buf.length) {
                dataBuf.get(buf);
                leftData.write(Arrays.copyOfRange(buf, 0, bitRate));
                rightData.write(Arrays.copyOfRange(buf, bitRate, bitRate * 2));
            }
        } catch (IOException ignore) {
        }
        return new byte[][]{
                leftData.toByteArray(),
                rightData.toByteArray()
        };
    }

}
