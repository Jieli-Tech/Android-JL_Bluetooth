package com.jieli.btsmart.ui.settings;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.settings.item.BaseItem;
import com.jieli.btsmart.data.model.settings.item.SettingsSwitch;
import com.jieli.btsmart.databinding.ItemSettingsSwitchBinding;
import com.jieli.btsmart.util.UIHelper;

/**
 * SettingsAdapter
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 设置功能适配器
 * @since 2025/2/28
 */
public class SettingsAdapter extends BaseMultiItemQuickAdapter<BaseItem, BaseViewHolder> {


    public static void updateSettingSwitch(ItemSettingsSwitchBinding binding, SettingsSwitch item) {
        if (null == binding || null == item) return;
        final String title = item.getTitle();
        if (null != title) {
            binding.tvTitle.setText(title);
        }
        final int imgRes = item.getImgRes();
        if (imgRes == 0) {
            UIHelper.gone(binding.ivImage);
        } else {
            binding.ivImage.setImageResource(imgRes);
        }
        binding.switchBtn.setCheckedImmediatelyNoEvent(item.isCheck());
        if (null != item.getListener()) {
            binding.switchBtn.setOnCheckedChangeListener(item.getListener());
        }
    }

    public static boolean getSettingsSwitchValue(ItemSettingsSwitchBinding binding) {
        if (null == binding) return false;
        return binding.switchBtn.isChecked();
    }

    public SettingsAdapter() {
        addItemType(BaseItem.ITEM_TEXT, R.layout.item_key_settings_two);
        addItemType(BaseItem.ITEM_SWITCH, R.layout.item_settings_switch);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, BaseItem baseItem) {
        if (null == baseItem) return;
        switch (baseItem.getItemType()) {
            case BaseItem.ITEM_TEXT: {
                break;
            }
            case BaseItem.ITEM_SWITCH: {
                SettingsSwitch item = (SettingsSwitch) baseItem;
                ItemSettingsSwitchBinding binding = ItemSettingsSwitchBinding.bind(baseViewHolder.getView(R.id.cl_root));
                updateSettingSwitch(binding, item);
                break;
            }
        }
    }
}
