package com.jieli.btsmart.tool.configure;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.btsmart.MainApplication;
import com.jieli.component.utils.SystemUtil;

import java.util.Locale;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 配置信息工具类
 * @since 2023/8/18
 */
public class ConfigureKit {
    private static final String TAG = "ConfigureKit";
    private static final ConfigureKit ourInstance = new ConfigureKit();

    private static final String KEY_AGREE_AGREEMENT = "user_agree_agreement";
    private static final String KEY_BAN_FLOATING_WINDOW = "ban_floating_window";
    private static final String KEY_ALLOW_SEARCH_DEVICE = "allow_search_device";

    public static ConfigureKit getInstance() {
        return ourInstance;
    }

    private final SharedPreferences sp;

    private ConfigureKit() {
        sp = MainApplication.getApplication().getSharedPreferences("configure_sp", Context.MODE_PRIVATE);
    }

    public boolean isAgreeAgreement() {
        return sp.getBoolean(KEY_AGREE_AGREEMENT, false);
    }

    public void setAgreeAgreement(boolean isAgree) {
        sp.edit().putBoolean(KEY_AGREE_AGREEMENT, isAgree).apply();
    }

    public boolean isBanRequestFloatingWindowPermission(@NonNull Context context) {
        int appVersion = SystemUtil.getVersion(context);
        int agreeVersion = sp.getInt(KEY_BAN_FLOATING_WINDOW, 0);
        return appVersion == agreeVersion;
    }

    public void setBanRequestFloatingWindowPermission(@NonNull Context context, boolean isBan) {
        int appVersion = SystemUtil.getVersion(context);
        sp.edit().putInt(KEY_BAN_FLOATING_WINDOW, isBan ? appVersion : 0).apply();
    }

    public boolean isAllowSearchDevice(String mac) {
        if (!BluetoothAdapter.checkBluetoothAddress(mac)) return false;
        String key = String.format(Locale.ENGLISH, "%s_%s", KEY_ALLOW_SEARCH_DEVICE, mac);
        return sp.getBoolean(key, false);
    }

    public boolean isAllowSearchDevice(@NonNull HistoryBluetoothDevice history) {
        String mac = history.getType() == BluetoothConstant.PROTOCOL_TYPE_SPP ? history.getAddress() : RCSPController.getInstance().getMappedDeviceAddress(history.getAddress());
        if (mac.isEmpty()) mac = history.getAddress();
        boolean ret = isAllowSearchDevice(mac);
        if (!ret && mac != null && !mac.equals(history.getAddress())) {
            ret = isAllowSearchDevice(history.getAddress());
        }
        return ret;
    }

    public void saveAllowSearchDevice(String mac, boolean isAllow) {
        if (!BluetoothAdapter.checkBluetoothAddress(mac)) return;
        String key = String.format(Locale.ENGLISH, "%s_%s", KEY_ALLOW_SEARCH_DEVICE, mac);
        sp.edit().putBoolean(key, isAllow).apply();
    }

    public void removeAllowSearchDevice(String mac) {
        if (!BluetoothAdapter.checkBluetoothAddress(mac)) return;
        String key = String.format(Locale.ENGLISH, "%s_%s", KEY_ALLOW_SEARCH_DEVICE, mac);
        sp.edit().remove(key).apply();
    }

    public void removeAllowSearchDevice(HistoryBluetoothDevice history) {
        String mac = history.getType() == BluetoothConstant.PROTOCOL_TYPE_SPP ? history.getAddress() : RCSPController.getInstance().getMappedDeviceAddress(history.getAddress());
        if (mac.isEmpty()) mac = history.getAddress();
        removeAllowSearchDevice(mac);
        if (mac != null && !mac.equals(history.getAddress())) {
            removeAllowSearchDevice(history.getAddress());
        }
    }
}
