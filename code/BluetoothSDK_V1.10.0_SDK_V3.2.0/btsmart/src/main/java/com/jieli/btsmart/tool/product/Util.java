package com.jieli.btsmart.tool.product;

import android.content.Context;
import android.content.SharedPreferences;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.btsmart.MainApplication;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/25 3:06 PM
 * @desc :
 */
class Util {
    static String toDesignKey(BleScanMessage bleScanMessage) {
        return "ProductDesign_" + bleScanMessage.getUid() + "_" + bleScanMessage.getPid() + "_" + bleScanMessage.getVid();
    }

    static String toResKey(BleScanMessage bleScanMessage, String scene) {
        return toDesignKey(bleScanMessage) + "_" + scene;
    }

    static SharedPreferences sp = MainApplication.getApplication().getSharedPreferences(ProductCacheManager.class.getCanonicalName(), Context.MODE_PRIVATE);


    static void save(String key, String value) {
        sp.edit().putString(key, value).apply();
    }

    static String get(String key) {
        return sp.getString(key, "");
    }
}
