package com.jieli.btsmart.ui.base.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.BluetoothOption;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.impl.BluetoothOperationImpl;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.component.base.BasePresenter;
import com.jieli.component.base.BaseView;

/**
 * 蓝牙通用接口
 *
 * @author zqjasonZhong
 * @since 2020/6/1
 */
public interface IBluetoothBase {

    interface IBluetoothPresenter extends BasePresenter {

        RCSPController getRCSPController();

        JL_BluetoothManager getBtManager();

        BluetoothOperationImpl getBtOp();

        BluetoothOption getBluetoothOption();

        void destroyRCSPController(BTRcspEventCallback callback);

        boolean isDevConnected();

        boolean isConnectedDevice(BluetoothDevice device);

        boolean isUsedDevice(BluetoothDevice device);

        BluetoothDevice getConnectedDevice();

        DeviceInfo getDeviceInfo();

        DeviceInfo getDeviceInfo(BluetoothDevice device);

        boolean isDevConnecting();

        boolean isConnectingDevice(String addr);

        boolean isConnectedDevice(String addr);

        BluetoothDevice getMappedEdrDevice(BluetoothDevice device);

        void switchConnectedDevice(BluetoothDevice device);

    }

    interface IBluetoothView extends BaseView<BasePresenter> {

        void onBtAdapterStatus(boolean enable);

        void onDeviceConnection(BluetoothDevice device, int status);

        void onSwitchDevice(BluetoothDevice device);
    }

}
