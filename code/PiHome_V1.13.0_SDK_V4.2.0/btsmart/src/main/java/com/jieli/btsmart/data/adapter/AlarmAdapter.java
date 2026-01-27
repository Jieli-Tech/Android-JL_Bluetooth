package com.jieli.btsmart.data.adapter;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.device.alarm.AlarmBean;
import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.bluetooth.rcsp.BTRcspHelper;
import com.jieli.btsmart.util.AppUtil;
import com.kyleduo.switchbutton.SwitchButton;

import java.util.List;

/**
 * Created by chensenhua on 2017/12/28.
 * 闹钟适配器
 */

public class AlarmAdapter extends BaseQuickAdapter<AlarmBean, BaseViewHolder> {


    private OnAlarmEventListener mOnAlarmEventListener;

    public void setOnAlarmEventListener(OnAlarmEventListener mOnAlarmEventListener) {
        this.mOnAlarmEventListener = mOnAlarmEventListener;
    }

    public AlarmAdapter(@Nullable List<AlarmBean> data) {
        super(R.layout.item_alarm, data);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, AlarmBean alarmBean) {
        ((TextView) holder.getView(R.id.tv_alarm_name)).setText(TextUtils.isEmpty(alarmBean.getName()) ? getContext().getString(R.string.unnamed) : alarmBean.getName());
        ((TextView) holder.getView(R.id.tv_alarm_week)).setText(BTRcspHelper.getRepeatDescModify(getContext(), alarmBean));
        SwitchButton sw = holder.getView(R.id.sw_default_alarm);
        sw.setCheckedImmediatelyNoEvent(alarmBean.isOpen());

        sw.setOnCheckedChangeListener((v, isChecked) -> {
            alarmBean.setOpen(isChecked);
            if (mOnAlarmEventListener != null) {
                mOnAlarmEventListener.onAlarmOpen(v, alarmBean, alarmBean.isOpen());
            }
        });

        holder.getView(R.id.ll_alarm_bg).setOnClickListener(v -> {
            if (mOnAlarmEventListener != null) {
                mOnAlarmEventListener.onAlarmClick(v, alarmBean, getItemPosition(alarmBean));
            }
        });

        holder.getView(R.id.btn_del_alarm).setOnClickListener(v -> {
            if (mOnAlarmEventListener != null) {
                mOnAlarmEventListener.onAlarmDelete(v, alarmBean, getItemPosition(alarmBean));
            }
        });


        int hour = alarmBean.getHour();
        int min = alarmBean.getMin();
        String time = AppUtil.formatString("%02d:%02d", hour, min);
        ((TextView) holder.getView(R.id.tv_alarm_time)).setText(time);
    }


    public interface OnAlarmEventListener {
        void onAlarmOpen(View view, AlarmBean alarmBean, boolean isOpen);

        void onAlarmDelete(View view, AlarmBean alarmBean, int position);

        void onAlarmClick(View view, AlarmBean alarmBean, int position);

    }
}
