package com.jieli.btsmart.tool.bluetooth.rcsp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.command.tws.NotifyAdvInfoCmd;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.alarm.AlarmBean;
import com.jieli.bluetooth.bean.device.double_connect.ConnectedBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DeviceBtInfo;
import com.jieli.bluetooth.bean.device.eq.EqInfo;
import com.jieli.bluetooth.bean.device.eq.EqPresetInfo;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.parameter.NotifyAdvInfoParam;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.bluetooth.RingHandler;
import com.jieli.btsmart.tool.configure.ConfigureKit;
import com.jieli.btsmart.tool.configure.DoubleConnectionSp;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.EqCacheUtil;
import com.jieli.component.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc Rcsp事件处理任务
 * @since 2021/12/9
 */
public class RcspEventHandleTask extends BTRcspEventCallback {
    private static final String TAG = RcspEventHandleTask.class.getSimpleName();
    private final RCSPController mController;
    private final StopAdvStreamHandler mStopAdvStreamHandler;
    private final AlarmHandler mAlarmHandler;
    private final DeviceOpHandler mDeviceOpHandler = DeviceOpHandler.getInstance();

    private BluetoothDevice mConnectingDev;

    private SyncWeatherTask mSyncWeatherTask;

    public RcspEventHandleTask(RCSPController controller) {
        mController = controller;
        mAlarmHandler = new AlarmHandler(controller);
        mStopAdvStreamHandler = new StopAdvStreamHandler(controller);
    }

    @Override
    public void onConnection(BluetoothDevice device, int status) {
        mDeviceOpHandler.onConnection(device, status);
        if (mDeviceOpHandler.isReconnecting()) return;
        handleConnectionEvent(device, status);
        if (status != StateCode.CONNECTION_OK) {
            EqCacheUtil.clear();//清除EQ缓存
        }
    }

    @Override
    public void onSwitchConnectedDevice(BluetoothDevice device) {
        handleConnectionEvent(device, StateCode.CONNECTION_OK);
    }

    @Override
    public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
        mStopAdvStreamHandler.onDeviceCommand(device, cmd);
        if (cmd.getId() == Command.CMD_ADV_DEVICE_NOTIFY) {
            NotifyAdvInfoCmd notifyAdvInfoCmd = (NotifyAdvInfoCmd) cmd;
            NotifyAdvInfoParam advInfo = notifyAdvInfoCmd.getParam();
            if (advInfo.isSupportChargingCase()) {
                //FIXME: 请求配对设备信息
            }
        }
    }

    @Override
    public void onDeviceRequestOp(BluetoothDevice device, int op) {
        mDeviceOpHandler.onDeviceRequestOp(device, op);
    }

    @Override
    public void onAlarmNotify(BluetoothDevice device, AlarmBean alarmBean) {
        mAlarmHandler.onAlarmNotify(device, alarmBean);
    }

    @Override
    public void onAlarmStop(BluetoothDevice device, AlarmBean alarmBean) {
        mAlarmHandler.onAlarmStop(device, alarmBean);
    }

    @Override
    public void onEqChange(BluetoothDevice device, EqInfo eqInfo) {
        if (mController.isUsingDevice(device)) {
            EqCacheUtil.saveEqValue(eqInfo);
            if (eqInfo.getMode() == EqInfo.MODE_CUSTOM && mController.getDeviceInfo() != null && mController.getDeviceInfo().getEqPresetInfo() != null) {
                //更新自定义eq预设值
                EqPresetInfo eqPresetInfo = mController.getDeviceInfo().getEqPresetInfo();
                eqPresetInfo.getEqInfos().remove(6);
                eqPresetInfo.getEqInfos().add(eqInfo);
                EqCacheUtil.savePresetEqInfo(eqPresetInfo);
            }
        }
    }

    @Override
    public void onEqPresetChange(BluetoothDevice device, EqPresetInfo eqPresetInfo) {
        if (mController.isUsingDevice(device)) {
            EqCacheUtil.savePresetEqInfo(eqPresetInfo);
        }
    }

    @Override
    public void onBondStatus(BluetoothDevice device, int status) {
        if (device != null && status == BluetoothDevice.BOND_NONE) { //更新历史记录
            DoubleConnectionSp.getInstance().removeDeviceBtInfo(getEdrAddress(device));
        }
    }

    @Override
    public void onConnectedBtInfo(BluetoothDevice device, ConnectedBtInfo info) {
        List<DeviceBtInfo> infoList = info.getDeviceBtInfoList();
        if (null != infoList) {
            final DoubleConnectionSp sp = DoubleConnectionSp.getInstance();
            String edrAddress = getEdrAddress(device);
            DeviceBtInfo cacheInfo = sp.getDeviceBtInfo(edrAddress);
            if (null == cacheInfo) {
                List<DeviceBtInfo> unBindList = new ArrayList<>();
                for (DeviceBtInfo btInfo : infoList) {
                    JL_Log.d(TAG, "[onConnectedBtInfo] >>> " + btInfo);
                    if (!btInfo.isBind()) {
                        unBindList.add(btInfo);
                    }
                }
                if (infoList.size() == 1 || unBindList.size() == 1) {
                    sp.saveDeviceBtInfo(edrAddress, unBindList.get(0));
                } else if (unBindList.size() > 1) {
                    String btName = AppUtil.getBtName(MainApplication.getApplication());
                    for (DeviceBtInfo btInfo : unBindList) {
                        if (btName != null && btInfo.getBtName() != null && btName.toLowerCase().startsWith(btInfo.getBtName().toLowerCase())) {
                            sp.saveDeviceBtInfo(edrAddress, btInfo);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void setConnectingDev(BluetoothDevice device) {
        mConnectingDev = device;
    }

    private String getEdrAddress(@NonNull BluetoothDevice device) {
        DeviceInfo deviceInfo = mController.getDeviceInfo(device);
        String address = deviceInfo != null ? deviceInfo.getEdrAddr() : null;
        if (null == deviceInfo) {
            HistoryBluetoothDevice history = mController.findHistoryBluetoothDevice(device.getAddress());
            if (null != history) {
                address = history.getType() == BluetoothConstant.PROTOCOL_TYPE_SPP ? history.getAddress()
                        : mController.getMappedDeviceAddress(history.getAddress());
            }
        }
        if (null == address) {
            address = device.getAddress();
        }
        return address;
    }

    private void handleConnectionEvent(BluetoothDevice device, int status) {
        mAlarmHandler.onConnection(device, status);
        mStopAdvStreamHandler.onConnection(device, status);
        if (status != StateCode.CONNECTION_OK && status != StateCode.CONNECTION_CONNECTED) {
            if (mSyncWeatherTask != null) {
                mSyncWeatherTask.stop();
                mSyncWeatherTask = null;
            }
        }
        switch (status) {
            case StateCode.CONNECTION_CONNECTING:
                setConnectingDev(device);
                break;
            case StateCode.CONNECTION_OK:
                if (BluetoothUtil.deviceEquals(device, mConnectingDev)) {
                    setConnectingDev(null);
                }
                final DevicePopDialogFilter filter = DevicePopDialogFilter.getInstance();
                if (filter.isIgnoreDevice(mController, device.getAddress())) { //如果是黑名单的设备
                    filter.removeIgnoreDevice(mController, device.getAddress()); 
                }
                if (mController.isUsingDevice(device)) {
                    mController.getCurrentDevModeInfo(device, null);
                }
                DeviceInfo deviceInfo = mController.getDeviceInfo(device);
                if (deviceInfo == null) break;
                String mac = deviceInfo.getEdrAddr();
                if (!BluetoothAdapter.checkBluetoothAddress(mac)) {
                    mac = device.getAddress();
                }
                ConfigureKit.getInstance().saveAllowSearchDevice(mac, deviceInfo.isSupportSearchDevice());
                if (deviceInfo.isSupportDoubleConnection()) {
                    String edrAddress = getEdrAddress(device);
                    DeviceBtInfo cacheInfo = DoubleConnectionSp.getInstance().getDeviceBtInfo(edrAddress);
                    if (null == cacheInfo) {
                        String btName = AppUtil.getBtName(MainApplication.getApplication());
                        mController.queryConnectedPhoneBtInfo(device, new DeviceBtInfo().setBtName(btName), null);
                    }
                }
                if (deviceInfo.getSdkType() == JLChipFlag.JL_COLOR_SCREEN_CHARGING_CASE) {
                    //FIXME: 请求配对设备信息

                    if (null == mSyncWeatherTask || !SyncWeatherTask.isRunning()) {
                        mSyncWeatherTask = new SyncWeatherTask(mController);
                        if (!mSyncWeatherTask.start(device)) {
                            mSyncWeatherTask = null;
                        }
                    }
                }
                break;
            case StateCode.CONNECTION_CONNECTED:
                if (BluetoothUtil.deviceEquals(device, mConnectingDev)) {
                    setConnectingDev(null);
                }
                break;
            case StateCode.CONNECTION_FAILED:
            case StateCode.CONNECTION_DISCONNECT:
                if (BluetoothUtil.deviceEquals(device, mConnectingDev)) {
                    ToastUtil.showToastShort(R.string.bt_connect_failed);
                    setConnectingDev(null);
                }
                if (!mController.isDeviceConnected() || BluetoothUtil.deviceEquals(device, mAlarmHandler.getDevice())) {
                    mAlarmHandler.onAlarmStop(device, null);
                }
                if (!mController.isDeviceConnected() || mController.isUsingDevice(device)) {
                    if (PlayControlImpl.getInstance().isPlay()) {
                        PlayControlImpl.getInstance().pause();
                    }
                    if (RingHandler.getInstance().isPlayAlarmRing()) {
                        RingHandler.getInstance().stopAlarmRing();
                    }
                }
                break;
        }
    }
}
