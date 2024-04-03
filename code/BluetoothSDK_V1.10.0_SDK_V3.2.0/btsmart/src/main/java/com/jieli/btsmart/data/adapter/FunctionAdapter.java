package com.jieli.btsmart.data.adapter;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.settings.SettingsItem;

/**
 * 功能适配器
 *
 * @author zqjasonZhong
 * @since 2020/5/15
 */
public class FunctionAdapter extends BaseQuickAdapter<SettingsItem, BaseViewHolder> {

    public FunctionAdapter() {
        super(R.layout.item_key_settings_two);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, SettingsItem item) {
        if (item != null) {
            helper.setImageResource(R.id.iv_key_settings_two_img, item.getResId());
            helper.setText(R.id.tv_key_settings_two_key, item.getName());
            helper.setText(R.id.tv_key_settings_two_value, item.getValue());
            ImageView imageView = helper.getView(R.id.iv_key_settings_two_icon);
            imageView.setVisibility(item.isShowIcon() ? View.VISIBLE : View.GONE);
//            int position = getItemPosition(item);
//            View line = helper.getView(R.id.view_key_settings_two_line);
//            if (line != null) {
//                line.setVisibility(position == (getData().size() - 1) ? View.GONE : View.VISIBLE);
//            }
        }
    }
}
