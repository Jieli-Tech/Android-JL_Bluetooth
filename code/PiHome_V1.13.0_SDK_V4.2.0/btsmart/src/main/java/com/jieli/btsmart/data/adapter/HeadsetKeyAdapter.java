package com.jieli.btsmart.data.adapter;

import com.chad.library.adapter.base.BaseSectionQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.settings.KeyBean;

import org.jetbrains.annotations.NotNull;

/**
 * 按键设置适配器
 *
 * @author zqjasonZhong
 * @since 2020/5/15
 */
public class HeadsetKeyAdapter extends BaseSectionQuickAdapter<KeyBean, BaseViewHolder> {
    public HeadsetKeyAdapter() {
        super(R.layout.item_key_settings_header);
        setNormalLayout(R.layout.item_key_settings_two);
    }

    @Override
    protected void convertHeader(@NotNull BaseViewHolder baseViewHolder, @NotNull KeyBean keyBean) {
        baseViewHolder.setText(R.id.tv_key_settings_action, keyBean.getAction());
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, KeyBean keyBean) {
        baseViewHolder.setText(R.id.tv_key_settings_two_key, keyBean.getKey());
        baseViewHolder.setText(R.id.tv_key_settings_two_value, keyBean.getFunction());
        baseViewHolder.setImageResource(R.id.iv_key_settings_two_img, keyBean.getResId());
    }

}
