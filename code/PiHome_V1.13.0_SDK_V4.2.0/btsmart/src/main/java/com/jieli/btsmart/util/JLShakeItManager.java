package com.jieli.btsmart.util;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.settings.AppSettingsItem;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: JLShakeItManager
 * @Description: java类作用描述
 * @Author: ZhangHuanMing
 * @CreateDate: 2021/1/30 20:58
 */
public class JLShakeItManager {
    public static final int SHAKE_IT_MODE_CUT_SONG = 1;
    public static final int SHAKE_IT_MODE_CUT_LIGHT_COLOR = 2;
    public static final int MODE_CUT_SONG_TYPE_DEFAULT = 1;
    public static final int MODE_CUT_SONG_TYPE_ID3 = 2;
    public static final int MODE_CUT_SONG_TYPE_FM = 3;

    private static final String PREFERENCE_SETTING_CONSTANT = "preference_setting_constant";

    private final String TAG = JLShakeItManager.class.getSimpleName();
    @SuppressLint("StaticFieldLeak")
    private volatile static JLShakeItManager mInstance;
    private final Context mContext;
    private final ShakeItUtil shakeItUtil;

    private boolean enableSupportCutSong;
    private boolean enableSupportCutLightColor;

    private int cutSongType = MODE_CUT_SONG_TYPE_DEFAULT;
    private int shakeItMode = SHAKE_IT_MODE_CUT_SONG;

    private final MutableLiveData<Integer> onShakeItStartLiveData = new MutableLiveData<>(SHAKE_IT_MODE_CUT_SONG);
    private final MutableLiveData<Integer> onShakeItEndLiveData = new MutableLiveData<>();

    private JLShakeItManager(Context context) {
        mContext = context;
        shakeItUtil = new ShakeItUtil(context);
        shakeItUtil.setShakeItListener(new ShakeItUtil.ShakeItListener() {
            @Override
            public void onShakeItStart() {
                if (!RCSPController.getInstance().isDeviceConnected()) {
                    ToastUtil.showToastShort(R.string.first_connect_device);
                    return;
                }
                if (shakeItMode == SHAKE_IT_MODE_CUT_SONG && enableSupportCutSong) {
                    onShakeItStartLiveData.postValue(shakeItMode);
                } else if (shakeItMode == SHAKE_IT_MODE_CUT_LIGHT_COLOR && enableSupportCutLightColor) {
                    onShakeItStartLiveData.postValue(shakeItMode);
                }
            }

            @Override
            public void onShakeItEnd() {
                onShakeItEndLiveData.postValue(shakeItMode);
            }
        });
    }

    public static JLShakeItManager getInstance() {
        if (mInstance == null) {
            synchronized (JLShakeItManager.class) {
                if (mInstance == null) {
                    mInstance = new JLShakeItManager(MainApplication.getApplication());
                }
            }
        }
        return mInstance;
    }

    public void release() {
        shakeItUtil.release();
        mInstance = null;
    }

    public MutableLiveData<Integer> getOnShakeItStartLiveData() {
        return onShakeItStartLiveData;
    }

    public MutableLiveData<Integer> getOnShakeItEndLiveData() {
        return onShakeItEndLiveData;
    }

    public void setShakeItMode(int shakeItMode) {
        if (this.shakeItMode == SHAKE_IT_MODE_CUT_SONG && shakeItMode == SHAKE_IT_MODE_CUT_LIGHT_COLOR) {
            shakeItUtil.startShakeIt();
        } else if (this.shakeItMode == SHAKE_IT_MODE_CUT_LIGHT_COLOR && shakeItMode == SHAKE_IT_MODE_CUT_SONG) {
            checkIsOpenShakeIt();
        }
        this.shakeItMode = shakeItMode;

    }

    public boolean isEnableSupportCutSong() {
        return enableSupportCutSong;
    }

    public boolean isEnableSupportCutLightColor() {
        return enableSupportCutLightColor;
    }

    public void setEnableSupportCutSong(boolean enable) {
        this.enableSupportCutSong = enable;
        checkIsOpenShakeIt();
    }

    public void setEnableSupportCutLightColor(boolean enable) {
        this.enableSupportCutLightColor = enable;
        checkIsOpenShakeIt();
    }

    public int getCutSongType() {
        return cutSongType;
    }

    public void setCutSongType(int cutSongType) {
        JL_Log.d(TAG, "setCutSongType: " + cutSongType);
        this.cutSongType = cutSongType;
    }

    public List<AppSettingsItem> getSettingList(BluetoothDevice device) {
        String cacheKey = getCacheKey(device);
        if (null == cacheKey) return null;
        String settingString = PreferencesHelper.getSharedPreferences(mContext).getString(cacheKey, null);
        if (null == settingString) return null;
        List<AppSettingsItem> result = new ArrayList<>();
        try {
            result = new Gson().fromJson(settingString, new TypeToken<List<AppSettingsItem>>() {
            }.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void saveSettingList(BluetoothDevice device, List<AppSettingsItem> settingsItems) {
        if (null == device || null == settingsItems) return;
        String settingString = new Gson().toJson(settingsItems, new TypeToken<List<AppSettingsItem>>() {
        }.getType());
        PreferencesHelper.putStringValue(mContext, getCacheKey(device), settingString);
    }

    private void checkIsOpenShakeIt() {
        if (enableSupportCutSong) {
            //打开了切换歌曲
            shakeItUtil.startShakeIt();
        } else {
            shakeItUtil.stopShakeIt();
        }
    }

    private String getCacheKey(BluetoothDevice device) {
        if (device == null) return null;
        return PREFERENCE_SETTING_CONSTANT + "_" + device.getAddress();
    }
}
