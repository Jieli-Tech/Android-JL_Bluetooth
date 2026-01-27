package com.jieli.btsmart.ui.settings.device.voice;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 声音设置
 * @since 2023/2/21
 */
public class VoiceSetting {
    private final int id;
    private final String name;
    private String desc;

    public VoiceSetting(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
