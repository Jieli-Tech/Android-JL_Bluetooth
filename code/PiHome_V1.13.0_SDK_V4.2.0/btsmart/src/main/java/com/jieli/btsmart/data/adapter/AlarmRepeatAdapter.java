package com.jieli.btsmart.data.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.alarm.RepeatBean;

import java.util.List;

/**
 * @author : chensenhua
 * @e-mail :
 * @date : 2020/6/24 4:21 PM
 * @desc : 闹钟重复模式选择适配器
 */
public  class AlarmRepeatAdapter extends BaseQuickAdapter<RepeatBean, BaseViewHolder> {
    public AlarmRepeatAdapter() {
        super(R.layout.item_alarm_repeat);
    }

    @Override
    protected void convert(BaseViewHolder holder, RepeatBean repeatBean) {
        holder.setText(R.id.tv_week_text, repeatBean.text/*.substring(1)*/);
        holder.getView(R.id.tv_week_text).setSelected(repeatBean.selected);
    }

    // 获取当前模式
    public byte getAlarmMode() {
        byte mode = 0;
        List<RepeatBean> list = getData();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).selected) {
                mode |= (0x01 << (i + 1));
            }
        }
        if (mode == (byte) 0xFE) {
            mode = 0x01;
        }
        return mode;
    }
}



