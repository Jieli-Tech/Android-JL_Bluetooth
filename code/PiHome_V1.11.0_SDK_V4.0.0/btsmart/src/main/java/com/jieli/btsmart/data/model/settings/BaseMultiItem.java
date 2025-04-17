package com.jieli.btsmart.data.model.settings;

import com.chad.library.adapter.base.entity.MultiItemEntity;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 多项基础
 * @since 2023/12/7
 */
public class BaseMultiItem<T> implements MultiItemEntity {
    private final int itemType;
    private T data;

    public BaseMultiItem(int itemType) {
        this.itemType = itemType;
    }

    @Override
    public int getItemType() {
        return itemType;
    }

    public T getData() {
        return data;
    }

    public BaseMultiItem<T> setData(T data) {
        this.data = data;
        return this;
    }
}
