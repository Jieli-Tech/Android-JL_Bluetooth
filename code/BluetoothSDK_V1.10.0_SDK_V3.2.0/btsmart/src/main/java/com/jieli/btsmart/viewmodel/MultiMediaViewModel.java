package com.jieli.btsmart.viewmodel;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.data.model.FunctionItemData;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

import java.util.ArrayList;

public class MultiMediaViewModel extends BtBasicVM {
    private ArrayList<FunctionItemData> mFunctionItemDataArrayList;
    private ArrayList<FunctionItemData> mAllFunctionItemDataArrayLists;
    private int mDeviceMask = -1;
    private PlayControlImpl mPlayControl;
    public final MutableLiveData<ArrayList<FunctionItemData>> mSupportFunctionItemDataListLiveData = new MutableLiveData<>();
    public final MutableLiveData<ConnectInfo> mConnectInfoLiveData = new MutableLiveData<>();

    public MultiMediaViewModel() {
        mRCSPController.addBTRcspEventCallback(mEventCallback);

        mFunctionItemDataArrayList = getAllFunctionItemDataArrayLists();
        getDeviceSupportFunctionList(isDevConnected());
        synConnectInfo(isDevConnected(), getDeviceInfo() == null ? 0 : getDeviceInfo().getCurFunction());
    }


    @Override
    protected void release() {
        super.release();
        mRCSPController.removeBTRcspEventCallback(mEventCallback);
    }

    public void getDeviceSupportFunctionList(boolean isConnected) {
        ArrayList<FunctionItemData> allList = getAllFunctionItemDataArrayLists();
        if (allList == null) return;
        if (isConnected) {
//            ArrayList<FunctionItemData> allList = mView.getFunctionItemDataList();
            ArrayList<FunctionItemData> removeList = new ArrayList<>();
            DeviceInfo deviceInfo = getDeviceInfo();
            if (!deviceInfo.isDevMusicEnable()) {
                removeList.add(allList.get(1));
                removeList.add(allList.get(2));
            } else {
                if (mDeviceMask != -1) {
                    if ((mDeviceMask & 0x01) != 0x01) {//u盘
                        removeList.add(allList.get(1));
                    }
                    if ((mDeviceMask & 0x02) != 0x02 && (mDeviceMask & 0x04) != 0x04) {//SD卡
                        removeList.add(allList.get(2));
                    }
                } else {
                    requestDeviceInfo();
                }
            }
//            if (!deviceInfo.isFmTxEnable()) {
//                removeList.add(allList.get(3));
//            }
//            if (!deviceInfo.isFmEnable()) {
//                removeList.add(allList.get(4));
//            }
            if (!deviceInfo.isAuxPlay()) {
                removeList.add(allList.get(5));
            }
            allList.removeAll(removeList);
            mFunctionItemDataArrayList = allList;
        } else {
//            mFunctionItemDataArrayList = mView.getFunctionItemDataList();
            mFunctionItemDataArrayList = getAllFunctionItemDataArrayLists();
        }
        mSupportFunctionItemDataListLiveData.setValue(mFunctionItemDataArrayList);
//        mView.updateFunctionItemDataList(mFunctionItemDataArrayList);
    }

    public void setAllFunctionItemDataArrayLists(ArrayList<FunctionItemData> list) {
        this.mAllFunctionItemDataArrayLists = list;
    }

    public MutableLiveData<ArrayList<FunctionItemData>> getSupportFunctionItemDataListLiveData() {
        return mSupportFunctionItemDataListLiveData;
    }

    public MutableLiveData<ConnectInfo> getConnectInfoLiveData() {
        return mConnectInfoLiveData;
    }

    private ArrayList<FunctionItemData> getAllFunctionItemDataArrayLists() {
        if (mAllFunctionItemDataArrayLists == null) return null;
        return new ArrayList<>(mAllFunctionItemDataArrayLists);
    }

    private void requestDeviceInfo() {
        mRCSPController.getDevStorageInfo(getConnectedDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                mDeviceMask = 0x01 << AttrAndFunCode.SYS_INFO_ATTR_MUSIC_DEV_STATUS;
                getDeviceSupportFunctionList(isDevConnected());
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                getDeviceSupportFunctionList(false);
            }
        });
    }

    private final BTRcspEventCallback mEventCallback = new BTRcspEventCallback() {

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            switch (status) {
                case StateCode.CONNECTION_DISCONNECT:
                    mDeviceMask = -1;
                    getDeviceSupportFunctionList(false);
                    synConnectInfo(false, (byte) 0x00);
//                    mView.switchTopControlFragment(false, (byte) 0x00);
                    break;
                case StateCode.CONNECTION_OK:
                    getDeviceSupportFunctionList(true);
                    getPlayControl().refresh();
                    DeviceInfo info = getDeviceInfo();
                    if (info != null) {
                        synConnectInfo(true, info.getCurFunction());
//                        mView.switchTopControlFragment(true, info.getCurFunction());
                    }
                    break;
            }
        }


        @Override
        public void onDeviceModeChange(BluetoothDevice device, int mode) {
            JL_Log.d("sen", "onDeviceFunChange--->" + mode);
//            mView.switchTopControlFragment(true, (byte) mode);
            synConnectInfo(true, (byte) mode);
        }

    };

    protected PlayControlImpl getPlayControl() {
        if (mPlayControl == null) {
            mPlayControl = PlayControlImpl.getInstance();
        }
        return mPlayControl;
    }

    private void synConnectInfo(boolean isConnect, byte fun) {
        JL_Log.d("ZHM", "synConnectInfo isConnect: " + isConnect + "fun: " + fun);
        ConnectInfo connectInfo = new ConnectInfo();
        connectInfo.setConnect(isConnect);
        connectInfo.setFun(fun);
        mConnectInfoLiveData.setValue(connectInfo);
    }

    public static class ConnectInfo {
        private boolean isConnect;
        private byte fun;

        public boolean isConnect() {
            return isConnect;
        }

        public void setConnect(boolean connect) {
            isConnect = connect;
        }

        public byte getFun() {
            return fun;
        }

        public void setFun(byte fun) {
            this.fun = fun;
        }
    }
}
