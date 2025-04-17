package com.jieli.btsmart.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

import com.jieli.btsmart.MainApplication;
import com.jieli.component.utils.PreferencesHelper;

import java.util.Locale;

/**
 * 多语言设置
 * 使用步骤：
 * 1、Application中onCreate添加registerActivityLifecycleCallbacks(MultiLanguageUtils.callbacks);
 *
 * @Override protected void attachBaseContext(Context base) {
 * //系统语言等设置发生改变时会调用此方法，需要要重置app语言
 * super.attachBaseContext(MultiLanguageUtils.attachBaseContext(base));
 * }
 * 2、改变应用语言调用MultiLanguageUtils.changeLanguage(activity,type,type);
 */
public class MultiLanguageUtils {
    //语言
    public final static String LANGUAGE_AUTO = "auto";
    public final static String LANGUAGE_EN = "en";
    public final static String LANGUAGE_ZH = "zh";
    public final static String LANGUAGE_JA = "ja";
    //语言地区
    public final static String AREA_AUTO = "AUTO";
    public final static String AREA_EN = "US";
    public final static String AREA_ZH = "CN";
    public final static String AREA_JA = "JP";
    //缓存Key
    public final static String SP_LANGUAGE = "SP_LANGUAGE";
    public final static String SP_COUNTRY = "SP_COUNTRY";

    /**
     * TODO 1、 修改应用内语言设置
     *
     * @param language 语言  zh/en
     * @param area     地区
     */
    public static void changeLanguage(Context context, String language, String area) {
        if (TextUtils.equals(LANGUAGE_AUTO, language) && TextUtils.equals(AREA_AUTO, area)) {
            //如果语言和地区都是空，那么跟随系统s
            PreferencesHelper.putStringValue(MainApplication.getApplication(), SP_LANGUAGE, LANGUAGE_AUTO);
            PreferencesHelper.putStringValue(MainApplication.getApplication(), SP_COUNTRY, AREA_AUTO);
        } else {
            //不为空，那么修改app语言，并true是把语言信息保存到sp中，false是不保存到sp中
            Locale newLocale = new Locale(language, area);
            changeAppLanguage(context, newLocale, true);
        }
    }

    /**
     * TODO 2、更改应用语言
     *
     * @param locale      语言地区
     * @param persistence 是否持久化
     */
    public static void changeAppLanguage(Context context, Locale locale, boolean persistence) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        Configuration configuration = resources.getConfiguration();
        setLanguage(context, locale, configuration);
        resources.updateConfiguration(configuration, metrics);
        if (persistence) {
            saveLanguageSetting(context, locale);
        }
    }

    private static void setLanguage(Context context, Locale locale, Configuration configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            configuration.setLocales(new LocaleList(locale));
            context.createConfigurationContext(configuration);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
        } else {
            configuration.locale = locale;
        }
    }

    /**
     * 判断sp中和app中的多语言信息是否相同
     */
    public static boolean isSameWithSetting(Context context) {
        Locale locale = getAppLocale(context);
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String sp_language = PreferencesHelper.getSharedPreferences(MainApplication.getApplication()).getString(SP_LANGUAGE,LANGUAGE_AUTO);
        String sp_country = PreferencesHelper.getSharedPreferences(MainApplication.getApplication()).getString(SP_COUNTRY, AREA_AUTO);
        if (sp_language.equals(LANGUAGE_AUTO) && sp_country.equals(AREA_AUTO)) {//缓存设置是auto
            Locale locale1 = getSystemLanguage().get(0);//系统语言
            sp_language = locale1.getLanguage();
            sp_country = locale1.getCountry();
        }
        if (language.equals(sp_language) && country.equals(sp_country)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 保存多语言信息到sp中
     */
    public static void saveLanguageSetting(Context context, Locale locale) {
        PreferencesHelper.putStringValue(MainApplication.getApplication(), SP_LANGUAGE, locale.getLanguage());
        PreferencesHelper.putStringValue(MainApplication.getApplication(), SP_COUNTRY, locale.getCountry());
    }

    /**
     * 获取应用语言
     */
    public static Locale getAppLocale(Context context) {
        Locale local;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            local = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            local = context.getResources().getConfiguration().locale;
        }
        return local;
    }

    /**
     * 获取系统语言
     */
    public static LocaleListCompat getSystemLanguage() {
        Configuration configuration = Resources.getSystem().getConfiguration();
        LocaleListCompat locales = ConfigurationCompat.getLocales(configuration);
        return locales;
    }

    //注册Activity生命周期监听回调，此部分一定加上，因为有些版本不加的话多语言切换不回来
    //registerActivityLifecycleCallbacks(callbacks);
    public static Application.ActivityLifecycleCallbacks callbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            String language = PreferencesHelper.getSharedPreferences(MainApplication.getApplication()).getString(SP_LANGUAGE,LANGUAGE_AUTO);
            String country = PreferencesHelper.getSharedPreferences(MainApplication.getApplication()).getString(SP_COUNTRY, AREA_AUTO);
            //强制修改应用语言  ，，相对于旧版的是默认也要强行切换，旧版的默认不会去进行切换
            if (!isSameWithSetting(activity)) {//如何区分auto
                if (TextUtils.equals(LANGUAGE_AUTO, language) && TextUtils.equals(AREA_AUTO, country)) {
                    Locale locale = getSystemLanguage().get(0);
                    language = locale.getLanguage();
                    country = locale.getCountry();
                }
                Locale locale = new Locale(language, country);
                changeAppLanguage(activity.getApplicationContext(), locale, false);
                changeAppLanguage(activity, locale, false);
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    };

}
