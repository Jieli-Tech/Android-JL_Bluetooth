package com.jieli.btsmart.ui.widget.dialog;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;

/**
 * WeekAdapter
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 周末适配器
 * @since 2025/9/1
 */
public class WeekAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

    public WeekAdapter() {
        super(R.layout.item_week);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder viewHolder, String s) {
        viewHolder.setText(R.id.tv_week, s);
    }
}
