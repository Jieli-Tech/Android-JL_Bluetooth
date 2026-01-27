package com.jieli.btsmart.ui.widget.dialog;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.audio.AudioFormat;
import com.jieli.btsmart.R;

/**
 * AudioFormatAdapter
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/24
 * note: 音频格式适配器
 */
public class AudioFormatAdapter extends BaseQuickAdapter<AudioFormat, BaseViewHolder> {

    /**
     * 已选择的算法ID
     */
    private int selectedId = 0;

    public AudioFormatAdapter() {
        super(R.layout.item_function_select);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, AudioFormat audioFormat) {
        if (null == audioFormat) return;
        holder.setText(R.id.tv_title, audioFormat.getName());
        holder.setVisible(R.id.iv_selected_state, isItemSelected(audioFormat));
    }

    public boolean isItemSelected(AudioFormat audioFormat) {
        if (null == audioFormat) return false;
        return audioFormat.getId() == selectedId;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateSelectedItem(int audioFormatID) {
        if(selectedId != audioFormatID){
            selectedId = audioFormatID;
            notifyDataSetChanged();
        }
    }
}
