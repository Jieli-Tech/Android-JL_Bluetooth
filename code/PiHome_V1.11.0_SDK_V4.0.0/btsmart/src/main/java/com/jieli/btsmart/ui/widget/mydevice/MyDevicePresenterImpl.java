package com.jieli.btsmart.ui.widget.mydevice;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.OnReconnectHistoryRecordListener;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.mydevice.MyDeviceInfoItemData;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.ui.base.bluetooth.BluetoothBasePresenter;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.btsmart.util.UIHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Deprecated
public class MyDevicePresenterImpl extends BluetoothBasePresenter implements IMyDeviceContract.IMyDevicePresenter {
    private final IMyDeviceContract.IMyDeviceView mView;

    public MyDevicePresenterImpl(IMyDeviceContract.IMyDeviceView view) {
        super(view);
        // Required empty public constructor
        mView = CommonUtil.checkNotNull(view);
        getRCSPController().addBTRcspEventCallback(mEventCallback);
    }

    @Override
    public void start() {
    }

    @Override
    public ArrayList<MyDeviceInfoItemData> getHistoryMyDeviceInfoItemDataList() {
        List<HistoryBluetoothDevice> srcList = getHistoryBtDeviceList();
        if (srcList == null || srcList.size() == 0) return null;
        ArrayList<MyDeviceInfoItemData> targetList = new ArrayList<>();
        for (int i = 0; i < srcList.size(); i++) {
            HistoryBluetoothDevice item = srcList.get(i);
            MyDeviceInfoItemData targetItem = new MyDeviceInfoItemData();
            targetItem.setAddress(item.getAddress());
            targetItem.setmDeviceInfoType(item.getChipType());
            targetItem.setmName(item.getName());
            targetItem.setmProtocolType(item.getType());
            targetItem.setAdvVersion(item.getAdvVersion());
            targetItem.setmConnectState(false);
            targetItem.setVid(item.getVid());
            targetItem.setPid(item.getPid());
            targetItem.setUid(item.getUid());
            targetList.add(targetItem);
        }
        return targetList;
    }

    @Override
    public List<HistoryBluetoothDevice> getHistoryBtDeviceList() {
        List<HistoryBluetoothDevice> list = getBtManager().getHistoryBluetoothDeviceList();
        if (list != null) {
            list = new ArrayList<>(list);
            Collections.reverse(list);
        }
        return list;
    }


    @Override
    public BluetoothDevice getConnectingDevice() {
        return getBtOp().getConnectingDevice();
    }

    @Override
    public void connectBtDevice(BluetoothDevice device) {
        if (checkCanConnectToDevice(device)) {
            //todo 当没有设备连接的时候才暂停播放，有设备连接的情况下不暂停
            if (getConnectedDevice() == null && PlayControlImpl.getInstance().isPlay()) {
                PlayControlImpl.getInstance().pause();
            }
            getRCSPController().connectDevice(device);
        }
    }

    @Override
    public void connectHistoryDevice(MyDeviceInfoItemData device) {
        if (device == null) return;
        getRCSPController().getBluetoothManager().reconnectHistoryBluetoothDevice(DeviceAddrManager.getInstance().
                        findHistoryBluetoothDevice(device.getAddress()),
                5000,
                new OnReconnectHistoryRecordListener() {
                    @Override
                    public void onSuccess(HistoryBluetoothDevice history) {

                    }

                    @Override
                    public void onFailed(HistoryBluetoothDevice history, BaseError error) {
                        BluetoothDevice historyDevice = BluetoothUtil.getRemoteDevice(device.getAddress());
                        if (historyDevice != null) {
                            mView.onDeviceConnection(historyDevice, StateCode.CONNECTION_FAILED);
                            mView.onDevConnectionError(error.getCode(), MainApplication.getApplication()
                                    .getString(R.string.connect_history_failed_tips, UIHelper.getDevName(historyDevice)));
                        }
                    }
                });
        BluetoothDevice historyDevice = BluetoothUtil.getRemoteDevice(device.getAddress());
        if (historyDevice != null) {
            mView.onDeviceConnection(historyDevice, StateCode.CONNECTION_CONNECTING);
        }
        /*BluetoothDevice connectDevice = BluetoothUtil.getRemoteDevice(device.getAddress());
        if (device.getType() == BluetoothConstant.PROTOCOL_TYPE_BLE) {
            String bleAddr = UIHelper.getCacheBleAddr(device);
            connectDevice = BluetoothUtil.getRemoteDevice(bleAddr);
        }
        connectBtDeviceByWay(connectDevice, device.getType());*/
    }

    @Override
    public void disconnectDevice() {
        final BluetoothDevice device = getConnectedDevice();
        if(null == device) return;
        DevicePopDialogFilter.getInstance().addIgnoreDevice(device.getAddress());
        getRCSPController().disconnectDevice(device);
    }

    @Override
    public void removeHistoryBtDevice(HistoryBluetoothDevice historyBtDevice) {
        getRCSPController().removeHistoryBtDevice(historyBtDevice, null);
    }

    @Override
    public void destroy() {
        destroyRCSPController(mEventCallback);
    }

    @Override
    public void updateDeviceADVInfo(BluetoothDevice device) {
        if (getDeviceInfo(device) != null && UIHelper.isCanUseTwsCmd(getDeviceInfo(device).getSdkType())) {
            mRCSPController.getDeviceSettingsInfo(device, 0xffffffff, new OnRcspActionCallback<ADVInfoResponse>() {
                @Override
                public void onSuccess(BluetoothDevice device, ADVInfoResponse message) {
                    mView.onADVInfoUpdate(device, message);
                }

                @Override
                public void onError(BluetoothDevice device, BaseError error) {

                }
            });
        }
    }

    /*
     * 判断是否能用TWS命令
     *
     * @return 结果
     */
    @Override
    public boolean isCanUseTws() {
        return getDeviceInfo() != null && UIHelper.isCanUseTwsCmd(getDeviceInfo().getSdkType());
    }

    //检测是否可以去连接设备
    private boolean checkCanConnectToDevice(BluetoothDevice device) {
        if (device == null) {
            return false;
        } else if (!BluetoothUtil.isBluetoothEnable()) {
            mView.onDevConnectionError(SConstant.ERR_BLUETOOTH_NOT_ENABLE, MainApplication.getApplication().getString(R.string.bluetooth_not_enable));
            return false;
        } else if (isDevConnecting()) {
            mView.onDevConnectionError(SConstant.ERR_DEV_CONNECTING, MainApplication.getApplication().getString(R.string.device_connecting_tips));
            return false;
        } else if (checkConnectedEdrIsOverLimit(device)) { //连接设备已达到上限
            mView.onDevConnectionError(SConstant.ERR_EDR_MAX_CONNECTION, MainApplication.getApplication().getString(R.string.connect_device_over_limit));
            return false;
        }

        return true;
    }

   /* private String getCacheBleAddr(MyDeviceInfoItemData device) {
        String bleAddr = null;
        if (device != null) {
            if (device.getmProtocolType() == BluetoothOption.PREFER_BLE) {
                bleAddr = device.getAddress();
            } else {
                bleAddr = DeviceAddrManager.getInstance().getDeviceAddr(device.getAddress());
            }
        }
        return bleAddr;
    }

    private void connectBtDeviceByWay(BluetoothDevice device, int connectWay) {
        if (connectWay != getBluetoothOption().getPriority()) {
            JL_Log.i("zzc_2333", "connectBtDeviceByWay :: change priority : " + connectWay);
            DeviceAddrManager.getInstance().updateHistoryBtDeviceInfo(device, connectWay, device.getAddress());
        }
        connectBtDevice(device);
    }*/

    @SuppressLint("MissingPermission")
    private boolean checkConnectedEdrIsOverLimit(BluetoothDevice device) {
        if (!PermissionUtil.checkHasConnectPermission(MainApplication.getApplication()))
            return true;
        boolean ret;
        List<BluetoothDevice> devices = BluetoothUtil.getSystemConnectedBtDeviceList();
        int count = 0;
        for (BluetoothDevice edrDevice : devices) {
            if (edrDevice.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC ||
                    edrDevice.getType() == BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
                //是相同设备，但是未连接通讯协议
                if (DeviceAddrManager.getInstance().isMatchDevice(edrDevice, device) && !isConnectedDevice(device)) {
                    return false;
                }
                count++;
            }
        }
        ret = count >= SConstant.MULTI_DEVICE_MAX_NUMBER;
        return ret;
    }

    private final BTRcspEventCallback mEventCallback = new BTRcspEventCallback() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
            mView.updateItemDataConnectState((device == null ? null : device.getAddress()), status);
        }

        @Override
        public void onBondStatus(BluetoothDevice device, int status) {
            if (status != BluetoothDevice.BOND_NONE) {
                HistoryBluetoothDevice history = mRCSPController.findHistoryBluetoothDevice(device.getAddress());
                if (history != null) {
                    mView.onRemoveHistoryDeviceSuccess(history);
                }
            }
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            mView.updateItemDataConnectState((device == null ? null : device.getAddress()), StateCode.CONNECTION_OK);
        }
    };

}

