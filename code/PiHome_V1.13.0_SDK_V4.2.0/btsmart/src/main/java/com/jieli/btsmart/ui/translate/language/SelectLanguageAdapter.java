package com.jieli.btsmart.ui.translate.language;

import android.annotation.SuppressLint;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.language.LanguageInfo;

/**
 * SelectLanguageAdapter
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 选择语言适配器
 * @since 2025/5/27
 */
class SelectLanguageAdapter extends BaseQuickAdapter<LanguageInfo, BaseViewHolder> {
    private LanguageInfo selectedItem;

    public SelectLanguageAdapter() {
        super(R.layout.item_language);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder viewHolder, LanguageInfo languageInfo) {
        if (null == languageInfo) return;
        final TextView textView = viewHolder.getView(R.id.tv_language);
        textView.setText(languageInfo.getLanguage());
        textView.setTextColor(ContextCompat.getColor(getContext(), isSelectedItem(languageInfo)
                ? R.color.purple_7657EC : R.color.black_E6000000));
    }

    public boolean isSelectedItem(LanguageInfo item) {
        if (null == selectedItem) return false;
        return selectedItem.equals(item);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateSelectedItem(LanguageInfo item) {
        if (selectedItem == null || !selectedItem.equals(item)) {
            selectedItem = item;
            notifyDataSetChanged();
        }
    }

}
