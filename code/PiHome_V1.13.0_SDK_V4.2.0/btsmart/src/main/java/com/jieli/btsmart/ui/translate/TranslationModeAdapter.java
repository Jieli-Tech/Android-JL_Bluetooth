package com.jieli.btsmart.ui.translate;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.translation.TranslationModeInfo;

/**
 * TranslationModeAdapter
 *
 * @author zqjasonZhong
 * @since 2025/5/27
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译模式适配器
 */
public class TranslationModeAdapter extends BaseQuickAdapter<TranslationModeInfo, BaseViewHolder> {

    public TranslationModeAdapter() {
        super(R.layout.item_translation_mode);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder viewHolder, TranslationModeInfo modeInfo) {
        if (null == modeInfo) return;
        viewHolder.setImageResource(R.id.iv_icon, modeInfo.getIconId());
        viewHolder.setText(R.id.tv_title, modeInfo.getTitle());
        viewHolder.setText(R.id.tv_desc, modeInfo.getDesc());
    }
}
