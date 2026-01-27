package com.jieli.btsmart.ui.widget.dialog;

import android.annotation.SuppressLint;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.translation.RecordType;

/**
 * RecordTypeAdapter
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 记录类型适配器
 * @since 2025/9/1
 */
public class RecordTypeAdapter extends BaseQuickAdapter<RecordType, BaseViewHolder> {

    /**
     * 选择项
     */
    private RecordType selectedItem;

    public RecordTypeAdapter() {
        super(R.layout.item_record_type);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder viewHolder, RecordType recordType) {
        final TextView tvContent = viewHolder.getView(R.id.tv_type);
        tvContent.setCompoundDrawablesRelativeWithIntrinsicBounds(recordType.getResId(), 0, 0, 0);
        tvContent.setText(recordType.getTitle());
        boolean isSelectedItem = isSelectedItem(recordType);
        final ConstraintLayout clMain = viewHolder.getView(R.id.main);
        clMain.setBackgroundResource(isSelectedItem ? R.color.gray_F5F7FA : R.color.white_ffffff);
        viewHolder.setVisible(R.id.iv_select_state, isSelectedItem);
    }

    public boolean isSelectedItem(RecordType type) {
        if (null == type || null == selectedItem) return false;
        return selectedItem.equals(type);
    }

    public RecordType getSelectedItem() {
        return selectedItem;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateSelectedItem(RecordType type) {
        if (null == type || isSelectedItem(type)) return;
        selectedItem = type;
        notifyDataSetChanged();
    }
}
