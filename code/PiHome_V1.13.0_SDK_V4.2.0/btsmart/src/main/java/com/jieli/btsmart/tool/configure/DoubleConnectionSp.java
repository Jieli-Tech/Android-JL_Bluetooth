package com.jieli.btsmart.tool.configure;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.jieli.bluetooth.bean.device.double_connect.DeviceBtInfo;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 设备双连信息缓存
 * @since 2023/9/20
 */
public class DoubleConnectionSp {
    private static final String TAG = "DoubleConnectionSp";
    private static final DoubleConnectionSp ourInstance = new DoubleConnectionSp();
    private static final String KEY_CONNECTED_BT_INFO = "connected_bt_info";
    private final SharedPreferences sp;
    private final Gson gson = new GsonBuilder().setLenient().create();

    public static DoubleConnectionSp getInstance() {
        return ourInstance;
    }

    private DoubleConnectionSp() {
        sp = MainApplication.getApplication().getSharedPreferences("", Context.MODE_PRIVATE);
    }

    public DeviceBtInfo getDeviceBtInfo(String address) {
        String key = getKey(address);
        if (null == key) return null;
        String json = sp.getString(key, "");
        if (TextUtils.isEmpty(json)) return null;
        try {
            return gson.fromJson(json, DeviceBtInfo.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveDeviceBtInfo(String address, DeviceBtInfo info) {
        String key = getKey(address);
        if (null == key || null == info) return;
        info.setBind(true);
        JL_Log.i(TAG, "[saveDeviceBtInfo] >>> address = " + address + ", " + info);
        sp.edit().putString(key, toJson(info)).apply();
    }

    public void removeDeviceBtInfo(String address) {
        String key = getKey(address);
        if (null == key) return;
        sp.edit().remove(key).apply();
    }

    @Nullable
    private String getKey(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) return null;
        return KEY_CONNECTED_BT_INFO + "-" + address;
    }

    @NonNull
    private String toJson(DeviceBtInfo info) {
        return "{" + "\"isBind\":" + info.isBind() + "," +
                " \"address\":\"" + info.getAddress() + "\"," +
                " \"btName\":\"" + info.getBtName() + "\"}";
    }
}
