package com.jieli.btsmart.data.adapter;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.settings.LedBean;

/**
 * 灯设置适配器
 *
 * @author zqjasonZhong
 * @since 2020/5/18
 */
public class LedSettingsAdapter extends BaseMultiItemQuickAdapter<LedBean, BaseViewHolder> {

    public LedSettingsAdapter() {
        addItemType(LedBean.ITEM_TYPE_ONE, R.layout.item_settings_one);
        addItemType(LedBean.ITEM_TYPE_TWO, R.layout.item_settings_two);
        addItemType(LedBean.ITEM_TYPE_THREE, R.layout.item_settings_three);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, LedBean item) {
        if (item == null) return;
        int itemType = getItemViewType(getItemPosition(item));
        switch (itemType) {
            case LedBean.ITEM_TYPE_ONE:
                helper.setText(R.id.tv_item_settings_one_name, item.getScene());
                helper.setText(R.id.tv_item_settings_one_value, item.getEffect());
                break;
            case LedBean.ITEM_TYPE_TWO:
                helper.setText(R.id.tv_item_settings_two_name, item.getScene());
                helper.setText(R.id.tv_item_settings_two_value, item.getEffect());
                break;
            case LedBean.ITEM_TYPE_THREE:
                helper.setText(R.id.tv_item_settings_three_name, item.getScene());
                helper.setText(R.id.tv_item_settings_three_value, item.getEffect());
                break;
        }
    }
}
