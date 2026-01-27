package com.jieli.btsmart.ui.chargingCase.message;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.btsmart.data.model.device.DeviceSettings;
import com.jieli.btsmart.tool.configure.ConfigureKit;
import com.jieli.btsmart.util.NotificationUtil;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

import java.util.ArrayList;
import java.util.List;

public class MessagePushViewModel extends BtBasicVM {
    /**
     * 操作设备
     */
    @NonNull
    public final BluetoothDevice mDevice;
    private final ConfigureKit mConfigureKit = ConfigureKit.getInstance();

    public final MutableLiveData<DeviceSettings> mDeviceSettingsMLD = new MutableLiveData<>();

    public MessagePushViewModel(@NonNull BluetoothDevice device) {
        mDevice = device;
        final String mac = getMac();
        if (mac != null) {
            final DeviceSettings settings = mConfigureKit.getDeviceSettings(mac);
            if (settings != null) {
                mDeviceSettingsMLD.postValue(settings);
            }
        }
    }

    public boolean isSyncMessage() {
        final String mac = getMac();
        if (null == mac) return false;
        final DeviceSettings settings = mConfigureKit.getDeviceSettings(mac);
        if (null == settings) return false;
        return settings.isSyncMessage();
    }

    public void enableSyncMessage(boolean isEnable) {
        final String mac = getMac();
        if (null == mac) return;
        final DeviceSettings settings = mConfigureKit.getDeviceSettings(mac);
        if (null == settings) return;
        if (settings.isSyncMessage() != isEnable) {
            settings.setSyncMessage(isEnable);
            List<String> list = null;
            if (isEnable) {
                list = new ArrayList<>();
                list.add(NotificationUtil.PACKAGE_NAME_SYS_MESSAGE);
                list.add(NotificationUtil.PACKAGE_NAME_WECHAT);
                list.add(NotificationUtil.PACKAGE_NAME_QQ);
                list.add(NotificationUtil.PACKAGE_NAME_DING_TALK);
                list.add(NotificationUtil.PACKAGE_NAME_LARK);
            }
            settings.setAppPacketNameList(list);
            mConfigureKit.saveDeviceSettings(mac, settings);
            mDeviceSettingsMLD.postValue(settings);
        }
    }

    public void handleAppPacketName(String packetName, boolean isAllow) {
        if (TextUtils.isEmpty(packetName)) return;
        final String mac = getMac();
        if (null == mac) return;
        final DeviceSettings settings = mConfigureKit.getDeviceSettings(mac);
        if (null == settings || !settings.isSyncMessage()) return;
        List<String> appPacketNameList = settings.getAppPacketNameList();
        if (null == appPacketNameList) {
            appPacketNameList = new ArrayList<>();
        }
        boolean isChange = false;
        if (isAllow) {
            if (!appPacketNameList.contains(packetName)) {
                appPacketNameList.add(packetName);
                isChange = true;
            }
        } else {
            isChange = appPacketNameList.remove(packetName);
        }
        if (isChange) {
            settings.setAppPacketNameList(appPacketNameList);
            mConfigureKit.saveDeviceSettings(mac, settings);
            mDeviceSettingsMLD.postValue(settings);
        }
    }

    private String getMac() {
        final DeviceInfo deviceInfo = getDeviceInfo(mDevice.getAddress());
        if (null == deviceInfo) return null;
        final String edrAddr = deviceInfo.getEdrAddr();
        return BluetoothAdapter.checkBluetoothAddress(edrAddr) ? edrAddr : mDevice.getAddress();
    }

    public static class Factory implements ViewModelProvider.Factory {
        @NonNull
        private final BluetoothDevice mDevice;

        public Factory(@NonNull BluetoothDevice device) {
            mDevice = device;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new MessagePushViewModel(mDevice);
        }
    }
}