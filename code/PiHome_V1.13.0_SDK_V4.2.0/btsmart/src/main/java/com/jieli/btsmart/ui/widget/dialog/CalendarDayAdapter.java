package com.jieli.btsmart.ui.widget.dialog;

import android.annotation.SuppressLint;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;

/**
 * CalendarDayAdapter
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 日历日期适配器
 * @since 2025/9/1
 */
public class CalendarDayAdapter extends BaseQuickAdapter<CalendarDay, BaseViewHolder> {

    /**
     * 已选中的日期
     */
    private CalendarDay selectedItem;

    public CalendarDayAdapter() {
        super(R.layout.item_calendar_date);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder viewHolder, CalendarDay calendarDay) {
        final TextView tvDate = viewHolder.getView(R.id.tv_date);
        boolean hasData = calendarDay.hasData();
        boolean isSelectedItem = isSelectedItem(calendarDay);
        switch (calendarDay.getType()) {
            case CalendarDay.TYPE_PAST_TIME: {
                int color = hasData ? R.color.black_242424 : R.color.gray_4D000000;
                if (isSelectedItem) {
                    color = R.color.white_ffffff;
                    tvDate.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_oval_purple_shape));
                } else {
                    tvDate.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_transparent));
                }
                tvDate.setText(calendarDay.getDayString());
                tvDate.setTextColor(ContextCompat.getColor(getContext(), color));
                break;
            }
            case CalendarDay.TYPE_NOW_TIME: {
                int color = hasData ? R.color.black_242424 : R.color.gray_4D000000;
                if (isSelectedItem) {
                    color = R.color.white_ffffff;
                    tvDate.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_oval_purple_shape));
                } else {
                    tvDate.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_oval_gray_shape));
                }
                tvDate.setText(getContext().getString(R.string.today_ab));
                tvDate.setTextColor(ContextCompat.getColor(getContext(), color));
                break;
            }
            case CalendarDay.TYPE_FUTURE_TIME:
                tvDate.setText(calendarDay.getDayString());
                tvDate.setTextColor(ContextCompat.getColor(getContext(), R.color.gray_4D000000));
                tvDate.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_transparent));
                break;
            default:
                tvDate.setText("");
                tvDate.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_transparent));
                break;
        }
    }

    public boolean isSelectedItem(CalendarDay day) {
        if (null == day || null == selectedItem) return false;
        return selectedItem.equals(day);
    }

    public CalendarDay getSelectedItem() {
        return selectedItem;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateSelectedItem(CalendarDay day) {
        if (null == day || isSelectedItem(day)) return;
        selectedItem = day;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clearSelectedItem() {
        if (null == selectedItem) return;
        selectedItem = null;
        notifyDataSetChanged();
    }
}
