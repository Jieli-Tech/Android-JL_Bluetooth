package com.jieli.btsmart.ui.widget.mydevice;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.btsmart.data.model.mydevice.MyDeviceInfoItemData;
import com.jieli.btsmart.ui.base.bluetooth.IBluetoothBase;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public interface IMyDeviceContract {
    interface IMyDevicePresenter extends IBluetoothBase.IBluetoothPresenter {

        ArrayList<MyDeviceInfoItemData> getHistoryMyDeviceInfoItemDataList();

        void connectBtDevice(BluetoothDevice device);

        void connectHistoryDevice(MyDeviceInfoItemData device);

        BluetoothDevice getConnectingDevice();

        void disconnectDevice();

        List<HistoryBluetoothDevice> getHistoryBtDeviceList();

        void removeHistoryBtDevice(HistoryBluetoothDevice historyBtDevice);

        void destroy();

        void updateDeviceADVInfo(BluetoothDevice device);

        /*
         * 判断是否能用TWS命令
         *
         * @return 结果
         */
        boolean isCanUseTws();
    }

    @SuppressWarnings("EmptyMethod")
    interface IMyDeviceView extends IBluetoothBase.IBluetoothView {

        void updateItemDataConnectState(String ssid, int status);

        void onDevConnectionError(int code, String message);

        void onADVInfoUpdate(BluetoothDevice device, ADVInfoResponse mADVInfo);

        void onRemoveHistoryDeviceSuccess(HistoryBluetoothDevice device);

        void onRemoveHistoryDeviceFailed(BaseError error);
    }
}
