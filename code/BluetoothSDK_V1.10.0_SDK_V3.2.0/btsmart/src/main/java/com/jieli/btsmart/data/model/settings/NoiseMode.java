package com.jieli.btsmart.data.model.settings;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.jieli.bluetooth.bean.base.VoiceMode;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc
 * @since 2021/3/24
 */
public class NoiseMode implements MultiItemEntity {
    private int itemType;
    private VoiceMode mVoiceMode;

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }

    @Override
    public int getItemType() {
        return itemType;
    }
}
