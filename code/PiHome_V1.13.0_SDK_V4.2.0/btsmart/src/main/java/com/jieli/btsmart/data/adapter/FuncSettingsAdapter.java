package com.jieli.btsmart.data.adapter;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;


import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.settings.FunctionBean;

/**
 * 功能设置适配器
 *
 * @author zqjasonZhong
 * @since 2020/5/16
 */
public class FuncSettingsAdapter extends BaseQuickAdapter<FunctionBean, BaseViewHolder> {
    private int selectPos = -1;

    public FuncSettingsAdapter() {
        super(R.layout.item_func_settings);
    }

    public int getSelectPos() {
        return selectPos;
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, FunctionBean item) {
        if (item == null) return;
        helper.setText(R.id.tv_func_settings_func, item.getFunction());
        ImageView ivCheck = helper.getView(R.id.iv_func_settings_check);
        ivCheck.setImageResource(item.isSelected() ? R.drawable.ic_check_purple : R.drawable.ic_check_gray);
        View line = helper.getView(R.id.view_func_settings_line);
        int position = getItemPosition(item);
        if (item.isSelected()) selectPos = position;
        line.setVisibility(position == (getData().size() - 1) ? View.GONE : View.VISIBLE);
    }
}
