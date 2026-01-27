package com.jieli.btsmart.data.adapter;

import android.annotation.SuppressLint;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.settings.VoiceModeItem;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 噪声处理适配器
 * @since 2021/3/26
 */
public class VoiceModeAdapter extends BaseQuickAdapter<VoiceModeItem, BaseViewHolder> {
    private final List<VoiceModeItem> selectList = new ArrayList<>();

    public VoiceModeAdapter() {
        super(R.layout.item_voice_mode);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder viewHolder, VoiceModeItem voiceModeItem) {
        if (null == voiceModeItem) return;
        viewHolder.setText(R.id.tv_voice_mode_name, voiceModeItem.getName());
        viewHolder.setText(R.id.tv_voice_mode_desc, voiceModeItem.getDesc());
        boolean isSelected = isSelectMode(voiceModeItem.getMode());
        viewHolder.setImageResource(R.id.iv_voice_mode_state, isSelected ? R.drawable.ic_check_purple : R.drawable.ic_check_gray);
        View line = viewHolder.getView(R.id.view_voice_mode_line);
        int position = getItemPosition(voiceModeItem);
        line.setVisibility(position == getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);
    }

    public List<VoiceModeItem> getSelectList() {
        return selectList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSelectList(byte[] array) {
        selectList.clear();
        if (array != null) {
            for (byte mode : array) {
                VoiceModeItem item = findVoiceModeItem(mode);
                if (item != null && !selectList.contains(item)) {
                    selectList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void handleVoiceModeItem(VoiceModeItem item) {
        if (item != null) {
            if (!selectList.contains(item)) {//不在列表内，添加
                selectList.add(item);
            } else {
                if (selectList.size() > 2) { //2个以上才能删除降噪模式
                    selectList.remove(item);
                }
            }
            notifyDataSetChanged();
        }
    }

    private VoiceModeItem findVoiceModeItem(byte mode) {
        List<VoiceModeItem> list = getData();
        VoiceModeItem item = null;
        if (!list.isEmpty()) {
            for (VoiceModeItem modeItem : list) {
                if (modeItem.getMode() == CHexConver.byteToInt(mode)) {
                    item = modeItem;
                    break;
                }
            }
        }
        return item;
    }

    private boolean isSelectMode(int mode) {
        if (selectList.isEmpty()) return false;
        for (VoiceModeItem select : selectList) {
            if (select.getMode() == mode) {
                return true;
            }
        }
        return false;
    }
}
