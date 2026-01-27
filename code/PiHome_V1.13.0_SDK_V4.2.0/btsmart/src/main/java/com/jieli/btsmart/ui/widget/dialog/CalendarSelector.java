package com.jieli.btsmart.ui.widget.dialog;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.translation.TranslationSessionRecord;
import com.jieli.btsmart.databinding.DialogCalendarSelectorBinding;
import com.jieli.btsmart.ui.translate.session.TranslationSessionViewModel;
import com.jieli.btsmart.util.TranslateUtil;
import com.jieli.btsmart.util.UIHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * CalendarSelector
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 日历选择器
 * @since 2025/9/1
 */
public class CalendarSelector extends CommonDialog {

    private DialogCalendarSelectorBinding mBinding;
    private CalendarDayAdapter mAdapter;

    protected CalendarSelector(@NonNull Builder builder) {
        super(builder);
    }

    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DialogCalendarSelectorBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI();
        addObserver();
    }

    private void initUI() {
        if (!(mBuilder instanceof Builder)) return;
        final Builder builder = (Builder) mBuilder;
        mBinding.btnClose.setOnClickListener(v -> dismiss());
        mBinding.btnPrevMonth.setOnClickListener(v -> {
            final Date date = builder.date;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.MONTH, -1);
            builder.date = calendar.getTime();
            updateCalendarDays(builder, null);
        });
        mBinding.btnNextMonth.setOnClickListener(v -> {
            final Date date = builder.date;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.MONTH, 1);
            builder.date = calendar.getTime();
            updateCalendarDays(builder, null);
        });

        WeekAdapter weekAdapter = new WeekAdapter();
        mBinding.rvWeek.setLayoutManager(new GridLayoutManager(requireContext(),7));
        mBinding.rvWeek.setAdapter(weekAdapter);

        mAdapter = new CalendarDayAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            final CalendarDay day = mAdapter.getItem(position);
            if (day.getType() != CalendarDay.TYPE_PAST_TIME && day.getType() != CalendarDay.TYPE_NOW_TIME
                    || mAdapter.isSelectedItem(day)) return;
            mAdapter.updateSelectedItem(day);
            builder.selectedDate = day.getDate();
            dismiss();
            final OnResultCallback<CalendarDay> callback = builder.callback;
            if (null != callback) {
                callback.onResult(day);
            }
        });
        mBinding.rvDate.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        mBinding.rvDate.setAdapter(mAdapter);

        String[] week = requireContext().getResources().getStringArray(R.array.week_array);
        weekAdapter.setList(Arrays.asList(week));

        updateCalendarDays(builder, null);
    }

    private void addObserver() {
        if (!(mBuilder instanceof Builder)) return;
        final Builder builder = (Builder) mBuilder;
        builder.viewModel.sessionRecordMLD.observe(getViewLifecycleOwner(), recordsResult -> {
            if (!isShow() || !isAdded() || isDetached() || null == recordsResult
                    || recordsResult.getOp() != TranslationSessionViewModel.OP_QUERY_BY_MONTH)
                return;
            if (recordsResult.getState() == StateResult.STATE_WORKING) {

                return;
            }
            if (recordsResult.getState() == StateResult.STATE_FINISH) {
                if (recordsResult.isSuccess()) {
                    updateCalendarDays(builder, recordsResult.getData());
                }else{
                    updateCalendarDays(builder, new ArrayList<>());
                }
            }
        });
    }

    private void updateCalendarDays(@NonNull Builder builder, List<TranslationSessionRecord> records) {
        final Date date = builder.date;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if (null == records) {
            builder.viewModel.querySessionRecordsByMonth(builder.translationMode, date);
            return;
        }

        List<CalendarDay> days = new ArrayList<>();

        Calendar current = Calendar.getInstance();
        int currentYear = current.get(Calendar.YEAR);
        int currentMonth = current.get(Calendar.MONTH);
        // 获取当天日期
        int currentDay = current.get(Calendar.DAY_OF_MONTH);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        boolean isCurrentMonth = year == currentYear && month == currentMonth;
        if (isCurrentMonth) {
            UIHelper.hide(mBinding.btnNextMonth);
        } else {
            UIHelper.show(mBinding.btnNextMonth);
        }
        mBinding.tvDate.setText(TranslateUtil.formatMonthDateString(date));

        calendar.set(Calendar.DAY_OF_MONTH, 1); //设置为月份第一天
        //获取当月第一天是星期几
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        // --- 计算需要在前面填充多少个上个月的日期 ---
        int prevMonthSize = (firstDayOfWeek - Calendar.SUNDAY) % 7;

        if (prevMonthSize > 0) { //需要添加上个月的日期
            // --- 创建上个月的 Calendar 实例 ---
            Calendar prevMonthCalendar = (Calendar) calendar.clone();
            prevMonthCalendar.add(Calendar.MONTH, -1); //切换到上个月

            // 获取上个月的总天数
            int prevMonthDays = prevMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

            // 添加上个月的日期 (从上个月的最后几天开始)
            for (int i = (prevMonthDays - prevMonthSize + 1); i <= prevMonthDays; i++) {
                Calendar dayCal = (Calendar) prevMonthCalendar.clone();
                dayCal.set(Calendar.DAY_OF_MONTH, i);
                days.add(new CalendarDay(dayCal.getTime(), CalendarDay.TYPE_PREVIOUS_MONTH));
            }
        }

        // --- 添加当月的日期 ---
        // 获取本月的总天数
        int maxDayInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        CalendarDay selectedItem = null;
        for (int i = 1; i <= maxDayInMonth; i++) {
            Calendar dayCal = (Calendar) calendar.clone();
            dayCal.set(Calendar.DAY_OF_MONTH, i);
            int type;
            if (isCurrentMonth) {
                if (i < currentDay) {
                    type = CalendarDay.TYPE_PAST_TIME;
                } else if (i == currentDay) {
                    type = CalendarDay.TYPE_NOW_TIME;
                } else {
                    type = CalendarDay.TYPE_FUTURE_TIME;
                }
            } else {
                type = CalendarDay.TYPE_PAST_TIME;
            }
            final Date time = dayCal.getTime();
            CalendarDay day = new CalendarDay(time, type);
            if (type == CalendarDay.TYPE_PAST_TIME || type == CalendarDay.TYPE_NOW_TIME) {
                final List<TranslationSessionRecord> list = findSessionRecordByDay(records, time);
                if (!list.isEmpty()) {
                    day.getRecords().clear();
                    day.getRecords().addAll(list);
                }
            }
            days.add(day);
            if (TranslateUtil.isSameDay(time, builder.selectedDate)) {
                selectedItem = day;
            }
        }

        // --- 填充下个月的日期  ---
        int nextMonthDaySize = (days.size() % 7 == 0) ? 0 : (7 - days.size() % 7);
        if (nextMonthDaySize > 0) {
            // --- 创建下个月的 Calendar 实例 ---
            Calendar nextMonthCalendar = (Calendar) calendar.clone();
            nextMonthCalendar.add(Calendar.MONTH, 1); //切换到下个月

            for (int i = 1; i <= nextMonthDaySize; i++) {
                Calendar dayCal = (Calendar) nextMonthCalendar.clone();
                dayCal.set(Calendar.DAY_OF_MONTH, i);
                days.add(new CalendarDay(dayCal.getTime(), CalendarDay.TYPE_NEXT_MONTH));
            }
        }

        mAdapter.setList(days);
        if (null == selectedItem) {
            mAdapter.clearSelectedItem();
        } else {
            mAdapter.updateSelectedItem(selectedItem);
        }
    }

    private List<TranslationSessionRecord> findSessionRecordByDay(List<TranslationSessionRecord> records, @NonNull Date date) {
        if (null == records || records.isEmpty()) return new ArrayList<>();
        List<TranslationSessionRecord> list = new ArrayList<>();
        long startTime = TranslateUtil.getDayStartTime(date);
        long endTime = TranslateUtil.getDayEndTime(date);
        for (TranslationSessionRecord sessionRecord : records) {
            long sessionStartTime = sessionRecord.getSession().getStartTime();
            long sessionEndTime = sessionRecord.getSession().getEndTime();
            if (sessionStartTime <= sessionEndTime && sessionStartTime >= startTime && sessionEndTime <= endTime) {
                list.add(sessionRecord);
            }
        }
        return list;
    }

    public static class Builder extends CommonDialog.Builder {
        /**
         * 翻译记录逻辑实现
         */
        @NonNull
        private final TranslationSessionViewModel viewModel;
        /**
         * 翻译模式
         */
        private final int translationMode;
        /**
         * 选中日期
         */
        private Date selectedDate;
        /**
         * 当前月份
         */
        @NonNull
        private Date date;
        /**
         * 结果回调
         */
        private OnResultCallback<CalendarDay> callback;

        public Builder(@NonNull TranslationSessionViewModel viewModel, int mode) {
            this.viewModel = viewModel;
            this.translationMode = mode;
            this.date = Calendar.getInstance().getTime();
            setGravity(Gravity.BOTTOM).setWidthRate(1.0f);
        }

        public Builder setDate(@NonNull Date date) {
            this.selectedDate = date;
            this.date = date;
            return this;
        }

        public Builder setCallback(OnResultCallback<CalendarDay> callback) {
            this.callback = callback;
            return this;
        }

        @Override
        public CalendarSelector build() {
            return new CalendarSelector(this);
        }
    }
}
