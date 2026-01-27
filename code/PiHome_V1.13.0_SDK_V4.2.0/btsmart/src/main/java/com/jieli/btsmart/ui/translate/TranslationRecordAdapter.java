package com.jieli.btsmart.ui.translate;

import android.annotation.SuppressLint;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.translation.RoleInfo;
import com.jieli.btsmart.data.model.translation.TranslationRecord;

/**
 * TranslationRecordAdapter
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译记录适配器
 * @since 2025/8/5
 */
public class TranslationRecordAdapter extends BaseQuickAdapter<TranslationRecord, BaseViewHolder> {
    /**
     * 选中的翻译记录
     */
    private TranslationRecord selectedItem;
    /**
     * 选中的原文颜色
     */
    private int srcSelectedColor;
    /**
     * 选中的译文颜色
     */
    private int destSelectedColor;

    public TranslationRecordAdapter() {
        super(R.layout.item_translation_record);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder viewHolder, TranslationRecord record) {
        final TextView tvRole = viewHolder.getView(R.id.tv_role);
        tvRole.setText(record.getNikeName());
        int resId;
        if (record.getRole() == RoleInfo.ROLE_DEVICE) {
            resId = R.drawable.ic_red_dot;
        } else {
            resId = R.drawable.ic_green_dot;
        }
        tvRole.setCompoundDrawablesRelativeWithIntrinsicBounds(resId, 0, 0, 0);
        final TextView tvSrcContent = viewHolder.getView(R.id.tv_src_content);
        final TextView tvDestContent = viewHolder.getView(R.id.tv_dest_content);
        tvSrcContent.setText(record.getSrcText());
        tvDestContent.setText(record.getDestText());
        if (isSelectedItem(record)) { //选中的记录
            int srcColor = srcSelectedColor == 0 ?  R.color.black_E6000000: srcSelectedColor;
            tvSrcContent.setTextColor(ContextCompat.getColor(getContext(), srcColor));
            int destColor = destSelectedColor == 0 ? R.color.black_80000000 : destSelectedColor;
            tvDestContent.setTextColor(ContextCompat.getColor(getContext(), destColor));
        } else {
            tvSrcContent.setTextColor(ContextCompat.getColor(getContext(), R.color.black_E6000000));
            tvDestContent.setTextColor(ContextCompat.getColor(getContext(), R.color.black_80000000));
        }
    }

    public boolean isSelectedItem(TranslationRecord record) {
        if (null == record || null == selectedItem) return false;
        return selectedItem.equals(record);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSelectedItemColor(int srcColor, int destColor) {
        this.srcSelectedColor = srcColor;
        this.destSelectedColor = destColor;
        if (null != selectedItem) {
            notifyDataSetChanged();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateSelectedItem(TranslationRecord record) {
        if (null == record) return;
        if (isSelectedItem(record)) {
            selectedItem = null;
        } else {
            selectedItem = record;
        }
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void resetSelectedItem() {
        if (null == selectedItem) return;
        selectedItem = null;
        notifyDataSetChanged();
    }

}
