package com.jieli.btsmart.util;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.component.utils.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * WavUtil
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc Wav工具类
 * @since 2025/7/1
 */
public class WavUtil {

    private static final String TAG = WavUtil.class.getSimpleName();

    private static final int RIFF_SIZE_OFFSET = 4;     // RIFF chunk size 位置
    private static final int DATA_SIZE_OFFSET = 40;    // data chunk size 位置
    private static final int WAV_HEADER_SIZE = 44;     // 标准WAV头部长度

    /**
     * 是否WAV文件
     *
     * @param path String 文件路径
     * @return boolean 结果
     */
    public static boolean isWavFile(String path) {
        if (TextUtils.isEmpty(path)) return false;
        return path.endsWith(".wav") || path.endsWith(".WAV");
    }

    /**
     * PCM转码成WAV
     *
     * @param pcmFilePath String PCM文件路径
     * @param wavFilePath String WAV文件路径
     * @return boolean 转码结果
     */
    public static boolean pcmToWav(String pcmFilePath, String wavFilePath) {
        return pcmToWav(pcmFilePath, wavFilePath, 16000, 1, 16);
    }

    /**
     * PCM转码成WAV
     *
     * @param pcmFilePath String PCM文件路径
     * @param wavFilePath String WAV文件路径
     * @param sampleRate  int 采样率
     * @param channels    int 声道数
     * @param bitDepth    int 位深
     * @return boolean 转码结果
     */
    public static boolean pcmToWav(String pcmFilePath, String wavFilePath, int sampleRate, int channels, int bitDepth) {
        return pcmToWav(FileUtil.getBytes(pcmFilePath), wavFilePath, sampleRate, channels, bitDepth);
    }

    /**
     * PCM转码成WAV
     *
     * @param pcmData     byte[] PCM数据
     * @param wavFilePath String WAV文件路径
     * @return boolean 转码结果
     */
    public static boolean pcmToWav(byte[] pcmData, String wavFilePath) {
        return pcmToWav(pcmData, wavFilePath, 16000, 1, 16);
    }

    /**
     * PCM转码成WAV
     *
     * @param pcmData     byte[] PCM数据
     * @param wavFilePath String WAV文件路径
     * @param sampleRate  int 采样率
     * @param channels    int 声道数
     * @param bitDepth    int 位深
     * @return boolean 转码结果
     */
    public static boolean pcmToWav(byte[] pcmData, String wavFilePath, int sampleRate, int channels, int bitDepth) {
        if (null == pcmData || pcmData.length == 0 || null == wavFilePath) return false;
        //计算WAV文件参数
        int byteRate = sampleRate * channels * bitDepth / 8;
        int blockAlign = channels * bitDepth / 8;
        int dataSize = pcmData.length;
        int totalSize = 36 + dataSize;

        FileOutputStream fos = null;
        boolean ret = false;
        try {
            fos = new FileOutputStream(wavFilePath);
            // RIFF Header
            writeString(fos, "RIFF"); // ChunkID
            writeInt(fos, totalSize);       // ChunkSize
            writeString(fos, "WAVE"); // Format

            // fmt Subchunk
            writeString(fos, "fmt ");      // Subchunk1ID
            writeInt(fos, 16);             // Subchunk1Size (固定为16)
            writeShort(fos, (short) 1);          // AudioFormat (1 表示 PCM)
            writeShort(fos, (short) channels);   // NumChannels
            writeInt(fos, sampleRate);           // SampleRate
            writeInt(fos, byteRate);             // ByteRate
            writeShort(fos, (short) blockAlign); // BlockAlign
            writeShort(fos, (short) bitDepth);   // BitsPerSample

            // data Subchunk
            writeString(fos, "data"); // Subchunk2ID
            writeInt(fos, dataSize);        // Subchunk2Size
            fos.write(pcmData);             // PCM Data
            ret = true;
        } catch (IOException e) {
            JL_Log.w(TAG, "pcmToWav", "IOException : " + e.getMessage());
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException ignored) {

                }
            }
        }
        return ret;
    }

    /**
     * 从WAV文件读取PCM数据
     *
     * @param wavFilePath String WAV文件路径
     * @return byte[] PCM数据
     */
    public static byte[] wavToPcm(String wavFilePath) {
        if (null == wavFilePath) return new byte[0];
        FileInputStream fis = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            fis = new FileInputStream(wavFilePath);
            // 跳过WAV文件头（前44字节）
            fis.skip(44);
            // 读取剩余数据（即PCM数据）
            byte[] data = new byte[1024];
            int readSize;
            while ((readSize = fis.read(data)) != -1) {
                output.write(data, 0, readSize);
            }
        } catch (IOException e) {
            JL_Log.w(TAG, "wavToPcm", "IOException : " + e.getMessage());
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException ignored) {

                }
            }
        }
        return output.toByteArray();
    }

    public static int getWavDuration(String filePath) {
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            // 检查RIFF标识
            byte[] riff = new byte[4];
            file.read(riff);
            if (!new String(riff, 0, 4).equals("RIFF")) {
                JL_Log.w(TAG, "getWavDuration", "Not a valid WAV file");
                return 0;
            }

            // 跳过文件大小
            file.seek(8);

            // 检查WAVE标识
            byte[] wave = new byte[4];
            file.read(wave);
            if (!new String(wave, 0, 4).equals("WAVE")) {
                JL_Log.w(TAG, "getWavDuration", "Not a valid WAV file");
                return 0;
            }

            // 查找 "fmt " 块
            while (file.getFilePointer() < file.length()) {
                byte[] chunkId = new byte[4];
                int size = file.read(chunkId);
                if (size != chunkId.length) break;
                String chunkName = new String(chunkId, 0, 4);

                int chunkSize = Integer.reverseBytes(file.readInt());

                if (chunkName.equals("fmt ")) {
                    // 读取fmt块数据
                    int audioFormat = Short.reverseBytes(file.readShort());
                    int numChannels = Short.reverseBytes(file.readShort());
                    int sampleRate = Integer.reverseBytes(file.readInt());
                    int byteRate = Integer.reverseBytes(file.readInt());
                    int blockAlign = Short.reverseBytes(file.readShort());
                    int bitsPerSample = Short.reverseBytes(file.readShort());

                    // 跳过可能的附加信息
                    file.seek(file.getFilePointer() + chunkSize - 16);

                    // 查找 "data" 块
                    while (file.getFilePointer() < file.length()) {
                        byte[] dataChunkId = new byte[4];
                        file.read(dataChunkId);
                        String dataChunkName = new String(dataChunkId, 0, 4);

                        int dataSize = Integer.reverseBytes(file.readInt());

                        if (dataChunkName.equals("data")) {
                            // 计算时长（秒）
                            float duration = (float) dataSize / byteRate;
                            return Math.round(duration);
                        } else {
                            // 跳过非数据块
                            file.seek(file.getFilePointer() + dataSize);
                        }
                    }
                    JL_Log.w(TAG, "getWavDuration", "Data chunk not found");
                } else {
                    // 跳过非fmt块
                    file.seek(file.getFilePointer() + chunkSize);
                }
            }
            JL_Log.w(TAG, "getWavDuration", "Fmt chunk not found");
        } catch (Exception ignored) {

        }
        return 0;
    }

    /**
     * 合并两个WAV文件
     *
     * @param wavFile1       String 第一个WAV文件路径
     * @param wavFile2       String 第二个WAV文件路径
     * @return boolean       结果
     */
    public static boolean mergeWavFiles(String wavFile1, String wavFile2) {
        RandomAccessFile targetRAF = null;
        FileInputStream sourceFIS = null;

        try {
            // 1. 以读写模式打开目标文件
            targetRAF = new RandomAccessFile(wavFile1, "rw");
            if (targetRAF.length() < 44) {
                JL_Log.w(TAG, "mergeWavFiles", "WavFile1 is not a valid WAV file.\nWavFile1 : " + wavFile1);
                return false;
            }
            sourceFIS = new FileInputStream(wavFile2);
            if (sourceFIS.available() < 44) {
                JL_Log.w(TAG, "mergeWavFiles", "WavFile2 is not a valid WAV file.\nWavFile2 : " + wavFile2);
                return false;
            }

            // 2. 读取两个文件的头部信息
            byte[] headData = new byte[44];
            targetRAF.read(headData);
            WavHeader targetHeader = readWavHeader(headData);
            sourceFIS.read(headData);
            WavHeader sourceHeader = readWavHeader(headData);

            // 3. 验证格式是否兼容
            if (!targetHeader.isCompatibleWith(sourceHeader)) {
                JL_Log.w(TAG, "mergeWavFiles", "The audio parameters of the two WAV files do not match and cannot be merged.\n" +
                        "WavFile1 : " + wavFile2 + ",\n WavFile2 : " + wavFile2);
                return false;
            }

            /// 4. 计算合并后的数据大小
            int newDataSize = targetHeader.dataSize + sourceHeader.dataSize;
            int newRiffSize = 36 + newDataSize; // 36 = RIFF头固定部分长度

            // 5. 将文件指针移动到目标文件末尾
            targetRAF.seek(targetRAF.length());

            // 6. 打开源文件，跳过其头部，将音频数据追加到目标文件末尾
            sourceFIS.skip(WAV_HEADER_SIZE); // 跳过源文件的WAV头部

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = sourceFIS.read(buffer)) != -1) {
                targetRAF.write(buffer, 0, bytesRead);
            }

            // 移动指针到 RIFF size 位置并写入新值
            targetRAF.seek(RIFF_SIZE_OFFSET);
            targetRAF.writeInt(Integer.reverseBytes(newRiffSize)); // RandomAccessFile.writeInt 是大端序，WAV是小端序

            // 移动指针到 data size 位置并写入新值
            targetRAF.seek(DATA_SIZE_OFFSET);
            targetRAF.writeInt(Integer.reverseBytes(newDataSize));

        } catch (IOException e) {
            JL_Log.w(TAG, "mergeWavFiles", "IOException : " + e.getMessage());
            return false;
        } finally {
            if (targetRAF != null) try {
                targetRAF.close();
            } catch (IOException ignored) {

            }
            if (sourceFIS != null) try {
                sourceFIS.close();
            } catch (IOException ignored) {

            }
        }
        return true;
    }

    private static void writeInt(@NonNull OutputStream output, int value) throws IOException {
        output.write(value & 0xFF);
        output.write((value >> 8) & 0xFF);
        output.write((value >> 16) & 0xFF);
        output.write((value >> 24) & 0xFF);
    }

    private static void writeShort(@NonNull OutputStream output, short value) throws IOException {
        output.write(value & 0xFF);
        output.write((value >> 8) & 0xFF);
    }

    private static void writeString(@NonNull OutputStream output, String value) throws IOException {
        if (null == value) return;
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 读取WAV文件头部信息
     */
    private static WavHeader readWavHeader(@NonNull byte[] headData) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(headData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        WavHeader header = new WavHeader();

        byte[] buf = new byte[4];
        buffer.get(buf);
        header.riffTag = new String(buf);
        header.riffSize = buffer.getInt(); // 小端序
        buffer.get(buf);
        header.waveTag = new String(buf);
        buffer.get(buf);
        header.fmtTag = new String(buf);
        header.fmtSize = buffer.getInt();
        header.audioFormat = buffer.getShort();
        header.numChannels = buffer.getShort();
        header.sampleRate = buffer.getInt();
        header.byteRate = buffer.getInt();
        header.blockAlign = buffer.getShort();
        header.bitsPerSample = buffer.getShort();
        buffer.get(buf);
        header.dataTag = new String(buf);
        header.dataSize = buffer.getInt();

        return header;
    }

    /**
     * WAV 头部信息类
     */
    private static class WavHeader {
        String riffTag;
        int riffSize;
        String waveTag;
        String fmtTag;
        String dataTag;
        int fmtSize;
        short audioFormat;
        short numChannels;
        int sampleRate;
        int byteRate;
        short blockAlign;
        short bitsPerSample;
        int dataSize;

        boolean isCompatibleWith(WavHeader other) {
            return this.audioFormat == other.audioFormat &&
                    this.numChannels == other.numChannels &&
                    this.sampleRate == other.sampleRate &&
                    this.bitsPerSample == other.bitsPerSample;
        }
    }
}
