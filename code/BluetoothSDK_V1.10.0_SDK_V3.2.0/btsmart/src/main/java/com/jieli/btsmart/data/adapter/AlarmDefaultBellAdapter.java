package com.jieli.btsmart.data.adapter;

import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.device.alarm.DefaultAlarmBell;
import com.jieli.btsmart.R;

import java.util.List;

/**
 * Created by chensenhua on 2017/12/28.
 * 闹钟默认铃声适配器
 */

public class AlarmDefaultBellAdapter extends BaseQuickAdapter<DefaultAlarmBell, BaseViewHolder> {


    public AlarmDefaultBellAdapter(@Nullable List<DefaultAlarmBell> data) {
        super(R.layout.item_alarm_bell, data);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, final DefaultAlarmBell ring) {
        ((TextView) holder.getView(R.id.tv_bell_name)).setText(TextUtils.isEmpty(ring.getName()) ? getContext().getString(R.string.unnamed) : ring.getName());
        holder.getView(R.id.tv_bell_name).setSelected(ring.isSelected());
        holder.getView(R.id.iv_bell_state).setSelected(ring.isSelected());
        holder.setVisible(R.id.view_bell_line, getItemPosition(ring) < getData().size() - 1);
    }


}
