package com.jieli.btsmart.data.model.bluetooth;

import androidx.annotation.NonNull;

/**
 *设备支持文件格式
 */
public class FileFormatInfo {
    private String format;  //设备支持的文件格式

    public FileFormatInfo(String format) {
        setFormat(format);
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @NonNull
    @Override
    public String toString() {
        return "FileFormatInfo{" +
                "format='" + format + '\'' +
                '}';
    }
}
