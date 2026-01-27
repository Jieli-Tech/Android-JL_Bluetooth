package com.jieli.btsmart.ui.settings.device.voice;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 声音设置适配器
 * @since 2023/2/21
 */
public class VoiceSettingAdapter extends BaseQuickAdapter<VoiceSetting, BaseViewHolder> {
    private int selectedPos = -1;

    public VoiceSettingAdapter() {
        super(R.layout.item_voice_setting);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder viewHolder, VoiceSetting voiceSetting) {
        if (null == voiceSetting) return;
        TextView textView = viewHolder.getView(R.id.tv_voice_setting);
        textView.setText(voiceSetting.getName());
        viewHolder.setText(R.id.tv_voice_setting, voiceSetting.getName());
        TextView tvDesc = viewHolder.getView(R.id.tv_voice_setting_desc);
        if (!TextUtils.isEmpty(voiceSetting.getDesc())) {
            tvDesc.setVisibility(View.VISIBLE);
            tvDesc.setText(voiceSetting.getDesc());
        } else {
            tvDesc.setVisibility(View.GONE);
            tvDesc.setText("");
        }
        viewHolder.setVisible(R.id.iv_voice_setting_icon, isSelectedItem(voiceSetting));
    }

    public boolean isSelectedItem(VoiceSetting setting) {
        if (null == setting) return false;
        int position = getItemPosition(setting);
        return position == selectedPos;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateSelectedPos(int position) {
        if (selectedPos != position) {
            selectedPos = position;
            notifyDataSetChanged();
        }
    }

    public void updateSelectedPosByVoiceId(int id) {
        VoiceSetting setting = getItemById(id);
        if (null != setting) {
            int position = getItemPosition(setting);
            updateSelectedPos(position);
        }
    }

    private VoiceSetting getItemById(int id) {
        if (getData().isEmpty()) return null;
        for (VoiceSetting setting : getData()) {
            if (setting.getId() == id) {
                return setting;
            }
        }
        return null;
    }
}
