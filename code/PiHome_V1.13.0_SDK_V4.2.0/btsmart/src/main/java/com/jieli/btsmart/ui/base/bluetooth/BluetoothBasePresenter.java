package com.jieli.btsmart.ui.base.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.BluetoothOption;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.impl.BluetoothOperationImpl;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.IBluetoothOperation;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.component.utils.SystemUtil;

/**
 * 蓝牙控制基类逻辑
 *
 * @author zqjasonZhong
 * @since 2020/6/1
 */
public class BluetoothBasePresenter implements IBluetoothBase.IBluetoothPresenter {
    protected final String tag = getClass().getSimpleName();
    private final IBluetoothBase.IBluetoothView mView;

    protected final RCSPController mRCSPController = RCSPController.getInstance();

    public BluetoothBasePresenter(IBluetoothBase.IBluetoothView view) {
        mView = SystemUtil.checkNotNull(view);

        getRCSPController().addBTRcspEventCallback(mEventCallback);
    }

    @Override
    public RCSPController getRCSPController() {
        return mRCSPController;
    }

    @Override
    public JL_BluetoothManager getBtManager() {
        return mRCSPController.getBluetoothManager();
    }

    @Override
    public BluetoothOperationImpl getBtOp() {
        IBluetoothOperation operation = mRCSPController.getBtOperation();
        if(!(operation instanceof BluetoothOperationImpl)) return null;
        return (BluetoothOperationImpl) operation;
    }

    @Override
    public BluetoothOption getBluetoothOption() {
        return mRCSPController.getBluetoothOption();
    }

    @Override
    public void destroyRCSPController(BTRcspEventCallback callback) {
        if (mRCSPController != null) {
            if (callback != null) {
                mRCSPController.removeBTRcspEventCallback(callback);
            }
            mRCSPController.removeBTRcspEventCallback(mEventCallback);
        }
    }

    @Override
    public boolean isDevConnected() {
        return mRCSPController.isDeviceConnected();
    }

    @Override
    public boolean isConnectedDevice(BluetoothDevice device) {
        if(device == null) return false;
        boolean ret = mRCSPController.isDeviceConnected(device);
        if(!ret){
            String mappedAddr = getBtManager().getMappedDeviceAddress(device.getAddress());
            BluetoothDevice mappedDev = BluetoothUtil.getRemoteDevice(mappedAddr);
            ret = mRCSPController.isDeviceConnected(mappedDev);
        }
        return ret;
    }

    @Override
    public boolean isUsedDevice(BluetoothDevice device) {
        return mRCSPController.isUsingDevice(device);
    }

    @Override
    public BluetoothDevice getConnectedDevice() {
        return mRCSPController.getUsingDevice();
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        return mRCSPController.getDeviceInfo();
    }

    @Override
    public DeviceInfo getDeviceInfo(BluetoothDevice device) {
        return mRCSPController.getDeviceInfo(device);
    }

    @Override
    public boolean isDevConnecting() {
        return mRCSPController.isConnecting();
    }

    @Override
    public boolean isConnectingDevice(String addr) {
        boolean ret = false;
        if (getBtOp().getConnectingDevice() != null && BluetoothAdapter.checkBluetoothAddress(addr)) {
            ret = addr.equals(getBtOp().getConnectingDevice().getAddress());
        }
        return ret;
    }

    @Override
    public boolean isConnectedDevice(String addr) {
        BluetoothDevice device = BluetoothUtil.getRemoteDevice(addr);
        return device != null && isConnectedDevice(device);
    }

    @Override
    public BluetoothDevice getMappedEdrDevice(BluetoothDevice device) {
        return getBtOp().getCacheEdrDevice(device);
    }

    @Override
    public void switchConnectedDevice(BluetoothDevice device) {
        mRCSPController.switchUsingDevice(device);
    }

    @Override
    public void start() {

    }

    private final BTRcspEventCallback mEventCallback = new BTRcspEventCallback() {
        @Override
        public void onAdapterStatus(boolean bEnabled, boolean bHasBle) {
            mView.onBtAdapterStatus(bEnabled);
        }

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            mView.onDeviceConnection(device, status);
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            mView.onSwitchDevice(device);
        }
    };

}
