package com.jieli.btsmart.ui.device;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.Nullable;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.interfaces.IActionCallback;
import com.jieli.btsmart.ui.base.bluetooth.IBluetoothBase;

import java.util.List;

/**
 * 设备连接接口
 *
 * @author zqjasonZhong
 * @since 2020/5/14
 */
public interface IDeviceConnectContract {

    interface IDeviceConnectPresenter extends IBluetoothBase.IBluetoothPresenter {

        boolean isAppGrantPermissions();

        void checkAppPermissions();

        String getPermissionName(String permission);

        void checkNetworkAvailable();

        List<HistoryBluetoothDevice> getHistoryBtDeviceList();

        int getHistoryDeviceState(HistoryBluetoothDevice historyBtDevice);

        void removeHistoryBtDevice(HistoryBluetoothDevice historyBtDevice, IActionCallback<HistoryBluetoothDevice> callback);

        void removeHistoryBtDevice(HistoryBluetoothDevice historyBtDevice);

        boolean isScanning();

        void startScan();

        void stopScan();

        void connectHistoryDevice(HistoryBluetoothDevice device);

        void connectBtDevice(BluetoothDevice device);

        void connectBtDevice(BluetoothDevice device, BleScanMessage bleScanMessage);

        void disconnectBtDevice();

        void connectEdrDevice(String edrAddr);

        boolean isConnectingClassicDevice();

        boolean checkIsConnectingEdrDevice(String addr);

        void stopDeviceNotifyAdvInfo(BluetoothDevice device);

        void updateDeviceADVInfo(BluetoothDevice device);

        ADVInfoResponse getADVInfo(BluetoothDevice device);

        boolean isUpdateDevBQ();

        //开始更新设备电量信息
        void startUpdateDevBQ();

        //停止更新设备电量信息
        void stopUpdateDevBQ();

        void setBanShowDialog(boolean enable);

        void setBanScanBtDevice(boolean enable);

        //快速回连上一个设备
        void fastConnect();

        void destroy();
    }

    @SuppressWarnings("EmptyMethod")
    interface IDeviceConnectView extends IBluetoothBase.IBluetoothView {

        void onPermissionSuccess(String[] permissions);

        void onPermissionFailed(String permission);

        void onDiscovery(int state, @Nullable BluetoothDevice device, @Nullable BleScanMessage bleScanMessage);

        void onShowDialog(BluetoothDevice device, BleScanMessage scanMessage);

        void onRemoveHistoryDeviceSuccess(HistoryBluetoothDevice device);

        void onRemoveHistoryDeviceFailed(BaseError error);

        void onDevConnectionError(int code, String message);

        void onCommandSuccess(BluetoothDevice device, CommandBase cmd);

        void onCommandFailed(BluetoothDevice device, BaseError error);

        void onADVInfoUpdate(BluetoothDevice device, ADVInfoResponse advInfo);

        void onDeviceBQStatus(boolean isRunning);

        void onDeviceBQUpdate(BluetoothDevice device, ADVInfoResponse advInfo);

        void onNetworkState(boolean isAvailable);

        void onUpdateConfigureSuccess();

        void onUpdateImage();

        void onUpdateConfigureFailed(int code, String message);

    }
}
