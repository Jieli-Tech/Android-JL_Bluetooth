package com.jieli.btsmart.tool.configure;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.device.DeviceSettings;
import com.jieli.btsmart.data.model.translation.ai_auth.AIAuthMessage;
import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.SystemUtil;

import java.util.HashSet;
import java.util.Set;

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
    private static final String KEY_SAVE_TONE_FILE = "save_tone_file";
    private static final String KEY_DEVICE_CFG = "device_cfg_";
    private static final String KEY_RESOURCE_VERSION = "resource_version";
    private static final String KEY_BAN_CALL_TRANSLATION_INTRODUCTION = "ban_call_translation_introduction";
    private static final String KEY_AI_AUTH_MESSAGE = "ai_auth_message";
    private static final String KEY_AURACAST_TRANSMITTER_RECORD_LIST = "auracast_transmitter_record_list";
    private static final String KEY_AURACAST_LOGIN_PASSWORD = "auracast_login_password_";

    public static final Gson GSON = new GsonBuilder().setLenient().create();

    public static boolean isValidPassword(String password) {
        if (null == password) return false;
        return password.length() >= 6 && password.length() <= 32;
    }

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
        String key = AppUtil.formatString("%s_%s", KEY_ALLOW_SEARCH_DEVICE, mac);
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
        String key = AppUtil.formatString("%s_%s", KEY_ALLOW_SEARCH_DEVICE, mac);
        sp.edit().putBoolean(key, isAllow).apply();
    }

    public void removeAllowSearchDevice(String mac) {
        if (!BluetoothAdapter.checkBluetoothAddress(mac)) return;
        String key = AppUtil.formatString("%s_%s", KEY_ALLOW_SEARCH_DEVICE, mac);
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

    public boolean isNeedUpdateToneFile(Context context) {
        int currentVersion = SystemUtil.getVersion(context);
        int version = sp.getInt(KEY_SAVE_TONE_FILE, 0);
        return version < currentVersion;
    }

    public void updateUpdateToneFileVersion(Context context) {
        int currentVersion = SystemUtil.getVersion(context);
        sp.edit().putInt(KEY_SAVE_TONE_FILE, currentVersion).apply();
    }

    public DeviceSettings getDeviceSettings(@NonNull String mac) {
        String json = sp.getString(getDeviceCfgKey(mac), "");
        if (TextUtils.isEmpty(json)) return null;
        try {
            return GSON.fromJson(json, DeviceSettings.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveDeviceSettings(@NonNull String mac, DeviceSettings settings) {
        final String key = getDeviceCfgKey(mac);
        if (null == settings) {
            sp.edit().remove(key).apply();
            return;
        }
        String json = GSON.toJson(settings);
        sp.edit().putString(key, json).apply();
    }

    public boolean isNeedUpdateResource() {
        int version = sp.getInt(KEY_RESOURCE_VERSION, 0);
        if (version <= 0) return true;
        String content = MainApplication.getApplication().getString(R.string.update_resource_version);
        int latestUpdateResourceVersion = TextUtils.isDigitsOnly(content) ? Integer.parseInt(content) : 0;
        return version < latestUpdateResourceVersion;
    }

    public void updateResourceVersion() {
        int currentVersion = SystemUtil.getVersion(MainApplication.getApplication());
        sp.edit().putInt(KEY_RESOURCE_VERSION, currentVersion).apply();
    }

    public boolean isBanCallTranslationIntroduction() {
        return sp.getBoolean(KEY_BAN_CALL_TRANSLATION_INTRODUCTION, false);
    }

    public void setBanCallTranslationIntroduction(boolean isBan) {
        sp.edit().putBoolean(KEY_BAN_CALL_TRANSLATION_INTRODUCTION, isBan).apply();
    }

    public AIAuthMessage getAIAuthMessage() {
        String json = sp.getString(KEY_AI_AUTH_MESSAGE, "");
        if (json.isEmpty()) return null;
        try {
            return AIConfig.gson.fromJson(json, AIAuthMessage.class);
        } catch (JsonSyntaxException ignored) {

        }
        return null;
    }

    public void updateAIAuthMessage(AIAuthMessage authMessage) {
        if (null == authMessage) return;
        sp.edit().putString(KEY_AI_AUTH_MESSAGE, authMessage.toString()).apply();
    }

    public String getAuracastLoginPassword(@NonNull String mac) {
        return sp.getString(getAuracastLoginPwdKey(mac), "");
    }

    @SuppressLint("MutatingSharedPrefs")
    public void saveAuracastLoginPassword(@NonNull String mac, String password) {
        sp.edit().putString(getAuracastLoginPwdKey(mac), password).apply();
        Set<String> macRecords = sp.getStringSet(KEY_AURACAST_TRANSMITTER_RECORD_LIST, null);
        if (null == macRecords) {
            macRecords = new HashSet<>();
        }
        if (macRecords.add(mac)) {
            sp.edit().putStringSet(KEY_AURACAST_TRANSMITTER_RECORD_LIST, macRecords).apply();
        }
    }

    public void deleteLoginPassword(@NonNull String mac) {
        sp.edit().remove(getAuracastLoginPwdKey(mac)).apply();
    }

    public void clearAllAuracastTransmitterLoginPwd() {
        Set<String> macRecords = sp.getStringSet(KEY_AURACAST_TRANSMITTER_RECORD_LIST, null);
        if (null == macRecords || macRecords.isEmpty()) return;
        for (String mac : macRecords) {
            deleteLoginPassword(mac);
        }
        sp.edit().remove(KEY_AURACAST_TRANSMITTER_RECORD_LIST).apply();
    }

    private String getDeviceCfgKey(@NonNull String mac) {
        return KEY_DEVICE_CFG + mac;
    }

    private String getAuracastLoginPwdKey(@NonNull String mac) {
        return KEY_AURACAST_LOGIN_PASSWORD + mac.replaceAll(":", "");
    }
}
