package com.jieli.btsmart.ui.multimedia;

import com.jieli.bluetooth.bean.device.music.ID3MusicInfo;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.btsmart.data.model.FunctionItemData;
import com.jieli.btsmart.ui.base.bluetooth.IBluetoothBase;

import java.util.ArrayList;

public interface IMultiMediaContract {
    interface IMultiMediaPresenter extends IBluetoothBase.IBluetoothPresenter {

        void getDeviceSupportFunctionList();

        void refreshDevMsg();

        void stopUpdateDevMsg();

        int showPlayerFlag();

        ID3MusicInfo getCurrentID3Info();

        void destroy();

        void switchToFMMode();

        void switchToBTMode();

        void switchToLineInMode();

        void switchToSPDIFMode();

        void switchToPCSlaveMode();

        void getCurrentModeInfo();

        void setDevStorage(int devHandler, OnRcspActionCallback<Boolean> callback);
    }

    interface IMultiMediaView extends IBluetoothBase.IBluetoothView {
        ArrayList<FunctionItemData> getFunctionItemDataList();

        void updateFunctionItemDataList(ArrayList<FunctionItemData> arrayList);

        void switchTopControlFragment(boolean isConnect, byte fun);

        void onChangePlayerFlag(int flag);
    }
}
