package com.jieli.btsmart.ui.ota;

import android.bluetooth.BluetoothDevice;

import com.jieli.btsmart.ui.base.bluetooth.IBluetoothBase;
import com.jieli.jl_http.bean.OtaMessage;

/**
 * OTA接口
 *
 * @author zqjasonZhong
 * @since 2020/5/19
 */
public interface IOtaContract {

    interface IOtaPresenter extends IBluetoothBase.IBluetoothPresenter {

        OtaMessage getOtaMessage();

        String getUpgradeFilePath();

        void checkFirmwareOtaService(String authKey, String projectCode);

        void checkFirmwareOtaService(String authKey, String projectCode, String md5);

        boolean judgeDeviceNeedToOta(BluetoothDevice device, OtaMessage message);

        void downloadFile(String url, String saveFilePath);

        boolean isFirmwareOta();

        void startFirmwareOta(String filePath);

        void cancelFirmwareOta();

        void disconnectDevice();

        void setUpgradeState(int state);

        void destroy();

    }

    interface IOtaView extends IBluetoothBase.IBluetoothView {

        void onOtaMessageChange(OtaMessage message);

        void onGetOtaMessageError(int code, String message);

        void onDownloadStart(String path);

        void onDownloadProgress(float progress);

        void onDownloadStop(String path);

        void onDownloadError(int code, String message);

        void onOtaStart();

        void onOtaProgress(int type, float progress);

        void onOtaStop();

        void onOtaCancel();

        void onOtaError(int code, String message);

    }
}
