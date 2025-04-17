package com.jieli.btsmart.data.adapter;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.settings.AppSettingsItem;
import com.kyleduo.switchbutton.SwitchButton;

/**
 * App设置适配器
 *
 * @author zqjasonZhong
 * @since 2020/7/23
 */
public class AppSettingsAdapter extends BaseQuickAdapter<AppSettingsItem, BaseViewHolder> {
    private AppSettingClickListener listener;

    public AppSettingsAdapter() {
        super(R.layout.item_app_settings);
    }

    public void setListener(AppSettingClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, AppSettingsItem settingsItem) {
        if (settingsItem == null) return;
        TextView tvNote;
        setVisibility(baseViewHolder.getView(R.id.fl_app_setting), settingsItem.isVisible());
        switch (settingsItem.getSettingType()) {
            case AppSettingsItem.TYPE_SINGLE:
                baseViewHolder.setVisible(R.id.iv_arrow, true);
                tvNote = baseViewHolder.getView(R.id.tv_note);
                tvNote.setVisibility(View.GONE);
                baseViewHolder.setVisible(R.id.sw_setting, false);
                break;
            case AppSettingsItem.TYPE_SWITCH:
                baseViewHolder.setVisible(R.id.iv_arrow, false);
                baseViewHolder.setVisible(R.id.sw_setting, true);
                SwitchButton switchButton = baseViewHolder.getView(R.id.sw_setting);
                switchButton.setChecked(settingsItem.isEnableState());
                switchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (listener != null) {
                        listener.switchButtonClick(settingsItem, isChecked);
                    }
                });
                if (settingsItem.getSettingNoteSrc() != null) {
                    baseViewHolder.setText(R.id.tv_note, settingsItem.getSettingNoteSrc());
                    baseViewHolder.setVisible(R.id.tv_note, true);
                } else {
                    tvNote = baseViewHolder.getView(R.id.tv_note);
                    tvNote.setVisibility(View.GONE);
                }
                break;
        }
        baseViewHolder.setText(R.id.tv_item_app_settings, settingsItem.getSettingNameSrc());
        baseViewHolder.setText(R.id.tv_tail, settingsItem.getTailString());
    }

    public interface AppSettingClickListener {
        void switchButtonClick(AppSettingsItem appSettingsItem, boolean isChecked);
    }

    private void setVisibility(View itemView, boolean isVisible) {
        RecyclerView.LayoutParams param = (RecyclerView.LayoutParams) itemView.getLayoutParams();
        if (isVisible) {
            param.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            param.width = LinearLayout.LayoutParams.MATCH_PARENT;
        } else {
            param.height = 0;
            param.width = 0;
        }
        itemView.setLayoutParams(param);
    }
}
