package com.jieli.btsmart.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.bluetooth.utils.CommonUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * TranslateUtil
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译功能工具类
 * @since 2025/8/5
 */
public class TranslateUtil {
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
    public static final SimpleDateFormat RECORD_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
    public static final SimpleDateFormat YEAR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    public static final SimpleDateFormat MONTH_DATE_FORMAT = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH);

    public static final String PCM_SUFFIX = ".pcm";
    public static final String WAV_SUFFIX = ".wav";
    public static final String OPUS_SUFFIX = ".opus";
    public static final String JLA_SUFFIX = ".jla";

    public static long currentTime() {
        return Calendar.getInstance().getTimeInMillis();
    }

    public static String formatTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return SIMPLE_DATE_FORMAT.format(calendar.getTime());
    }

    public static String formatSessionTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return RECORD_DATE_FORMAT.format(calendar.getTime());
    }

    public static String getDateString() {
        return SIMPLE_DATE_FORMAT.format(Calendar.getInstance().getTime());
    }

    public static String formatYearDateString(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return YEAR_DATE_FORMAT.format(calendar.getTime());
    }

    public static String formatMonthDateString(@NonNull Date date) {
        return MONTH_DATE_FORMAT.format(date);
    }

    public static boolean isSameDay(Date date1, Date date2) {
        if (null == date1 || null == date2) return false;
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(date1);
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(date2);
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH) &&
                calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH);
    }

    public static boolean isSameMonth(Date date1, Date date2) {
        if (null == date1 || null == date2) return false;
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(date1);
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(date2);
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH);
    }

    public static long getDayStartTime(@NonNull Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long getDayEndTime(@NonNull Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return calendar.getTimeInMillis();
    }

    public static String formatDuration(int duration) {
        int hour = duration / 3600;
        int min = duration / 60 % 60;
        int sec = duration % 60;
        return CommonUtil.formatString("%02d:%02d:%02d", hour, min, sec);
    }

    public static String formatDurationToHm(int duration) {
        int min = duration / 60 % 60;
        int sec = duration % 60;
        return CommonUtil.formatString("%02d:%02d", min, sec);
    }

    /**
     * 获取映射后的模式
     *
     * @param mode TranslationMode 翻译模式信息
     * @return int 映射后的模式
     */
    public static int getTranslationMode(TranslationMode mode) {
        if (null == mode) return -1;
        return mode.getMode() == TranslationMode.MODE_CALL_TRANSLATION_WITH_STEREO ?
                TranslationMode.MODE_CALL_TRANSLATION : mode.getMode();
    }

    /**
     * 判断手机是否处于电话状态
     *
     * @param context Context 上下文
     * @return boolean 结果
     */
    public static boolean isPhoneInCall(@NonNull Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) return false;
        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                // 如果没有权限，无法准确判断通话状态
                // 可以根据应用逻辑决定返回值，这里返回false作为安全选择
                return false;
            }
        }
        int callState = tm.getCallState();
        return callState == TelephonyManager.CALL_STATE_OFFHOOK
                /*|| callState == TelephonyManager.CALL_STATE_RINGING*/;
    }
}
