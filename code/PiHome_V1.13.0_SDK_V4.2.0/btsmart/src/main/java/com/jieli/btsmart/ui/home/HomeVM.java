package com.jieli.btsmart.ui.home;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.parameter.SearchDevParam;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.BluetoothOperationImpl;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.data.model.SwitchEdrParam;
import com.jieli.btsmart.tool.bluetooth.RingHandler;
import com.jieli.btsmart.tool.location.LocationHelper;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

import java.util.Calendar;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 主页逻辑实现
 * @since 2023/11/6
 */
public class HomeVM extends BtBasicVM {

    private final RingHandler mRingHandler = RingHandler.getInstance();
    private boolean isNeedCallback;

    public final MutableLiveData<Boolean> ringPlayStateMLD = new MutableLiveData<>();
    public final MutableLiveData<BluetoothDevice> mandatoryUpgradeMLD = new MutableLiveData<>();
    public final MutableLiveData<SwitchEdrParam> switchEdrParamMLD = new MutableLiveData<>();

    public HomeVM() {
        mRCSPController.addBTRcspEventCallback(mEventCallback);
        mRingHandler.registerOnRingStatusListener(mOnRingStatusListener);
    }

    public void stopRing() {
        mRingHandler.stopAlarmRing();
        if (isDevConnected()) {
            mRCSPController.stopSearchDevice(mRCSPController.getUsingDevice(), null);
        }
        isNeedCallback = false;
    }

    public void disconnect(BluetoothDevice device) {
        if(null == device) return;
        DevicePopDialogFilter.getInstance().addIgnoreDevice(device.getAddress());
        mRCSPController.disconnectDevice(device);
    }

    @Override
    public void release() {
        super.release();
        if (LocationHelper.isInit()) LocationHelper.getInstance().destroy();
        mRCSPController.removeBTRcspEventCallback(mEventCallback);
        mRingHandler.unregisterOnRingStatusListener(mOnRingStatusListener);
        mRingHandler.destroy();
    }

    private void syncConnectionTime() {
        mRCSPController.updateConnectedTime(getConnectedDevice(), (int) (Calendar.getInstance().getTimeInMillis() / 1000), null);
    }

    private void checkNeedShowSwitchEdrDialog(BluetoothDevice edrDevice) {
        if (null == edrDevice) return;
        BluetoothOperationImpl operation = getBtOp();
        if (null == operation) return;
        BluetoothDevice mUseDevEdr = operation.getCacheEdrDevice(getConnectedDevice());
        if (null == mUseDevEdr) return;
        BluetoothDevice mActiveDevice = operation.getActivityBluetoothDevice();
        if (mActiveDevice == null) {
            mActiveDevice = edrDevice;
        }
        if (!BluetoothUtil.deviceEquals(mActiveDevice, mUseDevEdr)) {
            switchEdrParamMLD.setValue(new SwitchEdrParam(UIHelper.getDevName(mActiveDevice), UIHelper.getDevName(mUseDevEdr)));
        }
    }

    private final RingHandler.OnRingStatusListener mOnRingStatusListener = new RingHandler.OnRingStatusListener() {
        @Override
        public void onRingStatusChange(boolean isPlay) {
            if (isNeedCallback && !isPlay) {
                ringPlayStateMLD.setValue(false);
                isNeedCallback = false;
            }
        }
    };

    public final BTRcspEventCallback mEventCallback = new BTRcspEventCallback() {

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (status != StateCode.CONNECTION_OK) return;
            DeviceInfo deviceInfo = getDeviceInfo();
            if (deviceInfo != null && UIHelper.isHeadsetType(deviceInfo.getSdkType())) {
                syncConnectionTime();
            }
            BluetoothOperationImpl operation = getBtOp();
            if (null != operation) {
                BluetoothDevice mCacheEdr = operation.getCacheEdrDevice(device);
                checkNeedShowSwitchEdrDialog(mCacheEdr);
            }
        }

        @Override
        public void onA2dpStatus(BluetoothDevice device, int status) {
            if (device != null && status == BluetoothProfile.STATE_CONNECTED && isDevConnected()) {
                checkNeedShowSwitchEdrDialog(device);
            }
        }

        @Override
        public void onMandatoryUpgrade(BluetoothDevice device) {
            if (!MainApplication.getApplication().isOTA() && BluetoothUtil.deviceEquals(getConnectedDevice(), device)) {
                mandatoryUpgradeMLD.postValue(device);
            }
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            BluetoothOperationImpl operation = getBtOp();
            if (null != operation) {
                BluetoothDevice mCacheEdr = operation.getCacheEdrDevice(device);
                checkNeedShowSwitchEdrDialog(mCacheEdr);
            }
            if (PlayControlImpl.getInstance().isPlay() && PlayControlImpl.getInstance().getMode() == PlayControlImpl.MODE_BT) {
                PlayControlImpl.getInstance().pause();
            }
        }

        @Override
        public void onSearchDevice(BluetoothDevice device, SearchDevParam searchDevParam) {
            if (searchDevParam.getType() != Constants.SEARCH_TYPE_SYNC_STATUS) {
                if (searchDevParam.getOp() == Constants.RING_OP_OPEN) {
                    mRingHandler.playAlarmRing(searchDevParam.getType(), searchDevParam.getTimeoutSec() * 1000L);
                    ringPlayStateMLD.setValue(true);
                    isNeedCallback = true;
                } else {
                    mRingHandler.stopAlarmRing();
                    ringPlayStateMLD.setValue(false);
                    isNeedCallback = false;
                }
            }
        }
    };
}
