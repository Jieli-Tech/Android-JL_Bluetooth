package com.jieli.btsmart.ui.widget.dialog;

import androidx.annotation.NonNull;

import com.jieli.btsmart.data.model.translation.TranslationSessionRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * CalendarDay
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 日历日期
 * @since 2025/9/1
 */
public class CalendarDay {

    /**
     * 已过去的日期
     */
    public static final int TYPE_PAST_TIME = 0;
    /**
     * 当月当天
     */
    public static final int TYPE_NOW_TIME = 1;
    /**
     * 当月未来日期
     */
    public static final int TYPE_FUTURE_TIME = 2;
    /**
     * 上一个月的日期
     */
    public static final int TYPE_PREVIOUS_MONTH = 3;
    /**
     * 下一个月的日期
     */
    public static final int TYPE_NEXT_MONTH = 4;

    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("d", Locale.ENGLISH);

    /**
     * 时间戳
     */
    @NonNull
    private final Date date;
    /**
     * 日期类型
     */
    private final int type;
    /**
     * 会议记录列表
     */
    @NonNull
    private final List<TranslationSessionRecord> records = new ArrayList<>();

    public CalendarDay(@NonNull Date date, int type) {
        this.date = date;
        this.type = type;
    }

    @NonNull
    public Date getDate() {
        return date;
    }

    public int getType() {
        return type;
    }

    @NonNull
    public List<TranslationSessionRecord> getRecords() {
        return records;
    }

    public boolean hasData() {
        return !records.isEmpty();
    }

    public String getDayString() {
        return DAY_FORMAT.format(date);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CalendarDay that = (CalendarDay) o;
        return Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(date);
    }

    @Override
    public String toString() {
        return "CalendarDay{" +
                "date=" + date +
                ", type=" + type +
                ", hasData=" + hasData() +
                '}';
    }
}
