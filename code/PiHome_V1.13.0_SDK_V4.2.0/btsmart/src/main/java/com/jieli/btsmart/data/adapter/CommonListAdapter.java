package com.jieli.btsmart.data.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.settings.SettingsItem;

import java.util.List;

/**
 * Des:
 * Author: Bob
 * Date:20-5-16
 * UpdateRemark:
 */
public final class CommonListAdapter extends BaseQuickAdapter<SettingsItem, BaseViewHolder> {

    public final static class ValueType {
        public static final int IMAGE = 0;
        public static final int TEXT = 1;
    }

    public CommonListAdapter(List<SettingsItem> list) {
        super(R.layout.item_common_list, list);

    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, SettingsItem item) {
        helper.setText(R.id.item_title, item.getName());
        int position = getItemPosition(item);
        if (getItemCount() > 0 && position == getItemCount() - 1) {//不显示最后一条
            helper.getView(R.id.item_divider).setVisibility(View.GONE);
        } else {
            helper.getView(R.id.item_divider).setVisibility(View.VISIBLE);
        }

        switch (item.getType()) {
            case ValueType.IMAGE:
                helper.setVisible(R.id.item_value, false);
            default:
                break;
            case ValueType.TEXT:
                TextView tvText = helper.getView(R.id.item_value);
                tvText.setText(item.getValue());
                helper.setVisible(R.id.item_next_logo, false);
//                tvText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                break;
        }
    }
}
