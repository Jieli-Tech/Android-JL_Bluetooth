package com.jieli.btsmart.data.model.settings;

import android.content.Context;

import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.btsmart.R;

import java.util.Objects;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 噪声处理模式
 * @since 2021/3/26
 */
public class VoiceModeItem {
    private int mode;
    private String name;
    private String desc;
    private int resource;

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getResource() {
        return resource;
    }

    public void setResource(int resource) {
        this.resource = resource;
    }

    public static String getVoiceModeName(Context context, int mode) {
        if (context == null) return null;
        String name = null;
        int res = -1;
        switch (mode) {
            case VoiceMode.VOICE_MODE_CLOSE:
                res = R.string.noise_mode_close;
                break;
            case VoiceMode.VOICE_MODE_DENOISE:
                res = R.string.noise_mode_denoise;
                break;
            case VoiceMode.VOICE_MODE_TRANSPARENT:
                res = R.string.noise_mode_transparent;
                break;
        }
        if (res != -1) {
            name = context.getString(res);
        }
        return name;
    }

    public static String getVoiceModeDesc(Context context, int mode) {
        if (context == null) return null;
        String desc = null;
        int res = -1;
        switch (mode) {
            case VoiceMode.VOICE_MODE_CLOSE:
                res = R.string.noise_mode_desc_close;
                break;
            case VoiceMode.VOICE_MODE_DENOISE:
                res = R.string.noise_mode_desc_denoise;
                break;
            case VoiceMode.VOICE_MODE_TRANSPARENT:
                res = R.string.noise_mode_desc_transparent;
                break;
        }
        if (res != -1) {
            desc = context.getString(res);
        }
        return desc;
    }

    public static int getVoiceModeResource(int mode, boolean isSelect) {
        int res = -1;
        switch (mode) {
            case VoiceMode.VOICE_MODE_CLOSE:
                res = isSelect ? R.drawable.ic_noise_close_white : R.drawable.ic_noise_close_gray;
                break;
            case VoiceMode.VOICE_MODE_DENOISE:
                res = isSelect ? R.drawable.ic_denoise_white : R.drawable.ic_denoise_gray;
                break;
            case VoiceMode.VOICE_MODE_TRANSPARENT:
                res = isSelect ? R.drawable.ic_transparent_white : R.drawable.ic_transparent_gray;
                break;
        }
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VoiceModeItem item = (VoiceModeItem) o;
        return mode == item.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode);
    }

    @Override
    public String toString() {
        return "VoiceModeItem{" +
                "mode=" + mode +
                ", name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", resource=" + resource +
                '}';
    }
}
