package com.jieli.btsmart.data.model.settings.item;

import com.chad.library.adapter.base.entity.MultiItemEntity;

/**
 * BaseItem
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc Item基类
 * @since 2025/2/28
 */
public abstract class BaseItem implements MultiItemEntity {

    public static final int ITEM_TEXT = 1;

    public static final int ITEM_SWITCH = 2;

    /**
     * 项目项类型
     */
    private final int itemType;
    /**
     * 数据
     */
    private Object data;

    public BaseItem(int itemType) {
        this.itemType = itemType;
    }

    @Override
    public int getItemType() {
        return itemType;
    }

    public Object getData() {
        return data;
    }

    public BaseItem setData(Object data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        return "BaseItem{" +
                "itemType=" + itemType +
                ", data=" + data +
                '}';
    }
}
