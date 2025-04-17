package com.jieli.btsmart.tool.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.VoiceData;
import com.jieli.bluetooth.bean.base.AttrBean;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.command.GetTargetInfoCmd;
import com.jieli.bluetooth.bean.command.UpdateSysInfoCmd;
import com.jieli.bluetooth.bean.command.tws.NotifyAdvInfoCmd;
import com.jieli.bluetooth.bean.command.tws.RequestAdvOpCmd;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.parameter.NotifyAdvInfoParam;
import com.jieli.bluetooth.bean.parameter.RequestAdvOpParam;
import com.jieli.bluetooth.bean.parameter.UpdateSysInfoParam;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.bean.response.ExternalFlashMsgResponse;
import com.jieli.bluetooth.bean.response.TargetInfoResponse;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.DeviceFlag;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.interfaces.bluetooth.IBluetoothEventListener;
import com.jieli.bluetooth.interfaces.bluetooth.RcspCommandCallback;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.tool.DeviceStatusManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.bluetooth.AlarmBean;
import com.jieli.btsmart.data.model.bluetooth.AlarmListInfo;
import com.jieli.btsmart.data.model.bluetooth.BatteryInfo;
import com.jieli.btsmart.data.model.bluetooth.ChannelInfo;
import com.jieli.btsmart.data.model.bluetooth.DefaultAlarmBell;
import com.jieli.btsmart.data.model.bluetooth.DevStorageInfo;
import com.jieli.btsmart.data.model.bluetooth.DeviceInfo;
import com.jieli.btsmart.data.model.bluetooth.EqInfo;
import com.jieli.btsmart.data.model.bluetooth.EqPresetInfo;
import com.jieli.btsmart.data.model.bluetooth.FileFormatInfo;
import com.jieli.btsmart.data.model.bluetooth.FmStatusInfo;
import com.jieli.btsmart.data.model.bluetooth.ID3MusicInfo;
import com.jieli.btsmart.data.model.bluetooth.LightControlInfo;
import com.jieli.btsmart.data.model.bluetooth.MusicNameInfo;
import com.jieli.btsmart.data.model.bluetooth.MusicStatusInfo;
import com.jieli.btsmart.data.model.bluetooth.PlayModeInfo;
import com.jieli.btsmart.data.model.bluetooth.VolumeInfo;
import com.jieli.btsmart.data.model.search.LocationInfo;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.PreferencesHelper;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.jieli.bluetooth.constant.AttrAndFunCode.FIXED_LEN_DATA_TYPE_DYNAMIC_LIMITER;
import static com.jieli.bluetooth.constant.AttrAndFunCode.FIXED_LEN_DATA_TYPE_REVERBERATION;
import static com.jieli.bluetooth.constant.AttrAndFunCode.SYS_INFO_ATTR_SOUND_CARD;
import static com.jieli.bluetooth.constant.AttrAndFunCode.SYS_INFO_ATTR_SOUND_CARD_EQ_FREQ;
import static com.jieli.bluetooth.constant.AttrAndFunCode.SYS_INFO_ATTR_SOUND_CARD_EQ_GAIN;

/**
 * 蓝牙事件回调管理器
 *
 * @author zqjasonZhong
 * @since 2020/5/13
 */
public class BTEventCallbackManager implements IBluetoothEventListener {
    private final static String TAG = BTEventCallbackManager.class.getSimpleName();
    private BluetoothHelper mBTHelper;
    private final List<BTEventCallback> mCallbackSet;

    private BluetoothDevice mReConnectDev = null;
    private boolean isReConnecting;

    private final static int DELAY_TIME = 1000;
    private final static int RECONNECT_TIMEOUT = 10 * 1000; //10s

    private final static int MSG_RECONNECT_DEVICE = 0x1248;
    private final static int MSG_RECONNECT_TIMEOUT = 0x1249;

    public static final int MASK_REVERBERATION = 1; // 混响
    public static final int MASK_DYNAMIC_LIMITER = 1 << 1; // 动态限幅器
    private final Handler mHandler = new Handler(Looper.getMainLooper(), this::handleMessage);

    BTEventCallbackManager(BluetoothHelper helper) {
        mBTHelper = helper;
        mCallbackSet = new ArrayList<>();
        getBluetoothManager().addEventListener(this);
    }

    private BluetoothHelper getBTHelper() {
        if (mBTHelper == null) {
            mBTHelper = BluetoothHelper.getInstance();
        }
        return mBTHelper;
    }

    private JL_BluetoothManager getBluetoothManager() {
        return getBTHelper().getBtManager();
    }

    public void registerBTEventCallback(BTEventCallback callback) {
        if (callback == null || mCallbackSet.contains(callback)) return;
        if (mCallbackSet.add(callback)) {
            TargetInfoResponse targetInfo = getBluetoothManager().getDeviceInfo(getBluetoothManager().getConnectedDevice());
            if (targetInfo != null) {
                boolean isMandatoryUpgrade = targetInfo.getMandatoryUpgradeFlag() == Constants.FLAG_MANDATORY_UPGRADE;
                if (!isMandatoryUpgrade) {
                    isMandatoryUpgrade = targetInfo.getRequestOtaFlag() == Constants.FLAG_MANDATORY_UPGRADE;
                }
                if (isMandatoryUpgrade) {
                    callback.onMandatoryUpgrade();
                    callback.onMandatoryUpgrade(getBluetoothManager().getConnectedDevice());
                }
            }
            ExternalFlashMsgResponse flashMsg = DeviceStatusManager.getInstance().getExtFlashMsg(getBluetoothManager().getConnectedDevice());
            if (flashMsg != null && flashMsg.getSysStatus() != 0) {
                DeviceInfo deviceInfo = getBTHelper().getDeviceInfo(getBluetoothManager().getConnectedDevice());
                if (deviceInfo != null) deviceInfo.setFatFsException(true);
                callback.onExternalFlashSysException(getBluetoothManager().getConnectedDevice(), flashMsg.getSysStatus());
            }
        }
    }

    public void unregisterBTEventCallback(BTEventCallback callback) {
        if (callback != null) {
            mCallbackSet.remove(callback);
        }
    }

    public void handleBtCallback(BtCallback callback) {
        mHandler.post(new BTCallbackRunnable(mCallbackSet, callback));
    }

    public void destroy() {
        mHandler.removeCallbacksAndMessages(null);
        getBluetoothManager().removeEventListener(this);
        mCallbackSet.clear();
    }

    @Override
    public void onAdapterStatus(final boolean bEnabled, final boolean bHasBle) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onAdapterStatus(bEnabled, bHasBle);
            }
        });
    }

    @Override
    public void onDiscoveryStatus(final boolean bBle, final boolean bStart) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onDiscoveryStatus(bBle, bStart);
            }
        });
    }

    @Override
    public void onDiscovery(final BluetoothDevice device) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onDiscovery(device);
            }
        });
    }

    @Override
    public void onDiscovery(final BluetoothDevice device, final BleScanMessage bleScanMessage) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onDiscovery(device, bleScanMessage);
            }
        });
    }

    @Override
    public void onShowDialog(final BluetoothDevice device, final BleScanMessage bleScanMessage) {
        boolean isAllowShowDialog = PreferencesHelper.getSharedPreferences(MainApplication.getApplication()).getBoolean(SConstant.KEY_ALLOW_SHOW_BT_DIALOG, SConstant.ALLOW_SHOW_BT_DIALOG);
        if (bleScanMessage != null && bleScanMessage.getRssi() > PreferencesHelper.getSharedPreferences(MainApplication.getApplication()).
                getInt(SConstant.KEY_BLE_ADV_RSSI_LIMIT, SConstant.BLE_ADV_RSSI_LIMIT)
                && isAllowShowDialog) {
            handleBtCallback(new BtCallback() {
                @Override
                public void onCallback(BTEventCallback callback) {
                    callback.onShowDialog(device, bleScanMessage);
                }
            });
        }
    }

    @Override
    public void onBondStatus(final BluetoothDevice device, final int status) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onBondStatus(device, status);
            }
        });
    }

    @Override
    public void onConnection(final BluetoothDevice device, final int status) {
        handleConnectEvent(device, status);
    }

    @Override
    public void onSwitchConnectedDevice(final BluetoothDevice device) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onSwitchConnectedDevice(device);
            }
        });
        if (DeviceStatusManager.getInstance().isMandatoryUpgrade(device)) {
            onMandatoryUpgrade(device);
        }
    }

    @Override
    public void onA2dpStatus(final BluetoothDevice device, final int status) {
        if (BluetoothUtil.deviceEquals(device, mReConnectDev) && status == BluetoothProfile.STATE_DISCONNECTED) {
            mHandler.sendEmptyMessage(MSG_RECONNECT_TIMEOUT);
        }
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onA2dpStatus(device, status);
            }
        });
    }

    @Override
    public void onHfpStatus(final BluetoothDevice device, final int status) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onHfpStatus(device, status);
            }
        });
    }

    @Override
    public void onSppStatus(final BluetoothDevice device, final int status) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onSppStatus(device, status);
            }
        });
    }

    @Override
    public void onDeviceCommand(final BluetoothDevice device, final CommandBase cmd) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onDeviceCommand(device, cmd);
            }
        });
        int cmdId = cmd.getId();
        switch (cmdId) {
            case Command.CMD_SYS_INFO_AUTO_UPDATE:
                UpdateSysInfoCmd updateSysInfoCmd = (UpdateSysInfoCmd) cmd;
                UpdateSysInfoParam param = updateSysInfoCmd.getParam();
                List<AttrBean> list = param.getAttrBeanList();
                parseAttrMessage(device, param.getFunction(), list);
                break;
            case Command.CMD_ADV_DEV_REQUEST_OPERATION:
                RequestAdvOpCmd requestAdvOpCmd = (RequestAdvOpCmd) cmd;
                RequestAdvOpParam opParam = requestAdvOpCmd.getParam();
                if (opParam != null) {
                    switch (opParam.getOp()) {
                        case Constants.ADV_REQUEST_OP_RECONNECT_DEVICE://回连设备
                            HistoryBluetoothDevice historyDevice = DeviceAddrManager.getInstance().findHistoryBluetoothDevice(device);
                            if ((historyDevice != null && historyDevice.getType() == BluetoothConstant.PROTOCOL_TYPE_BLE) ||
                                    getBTHelper().getBtOperation().getDeviceGatt(device) != null) { //判断是走BLE方式才需要主从切换时回连
                                JL_Log.i(TAG, "reconnect device : " + BluetoothUtil.printBtDeviceInfo(device));
                                setReConnectDev(device);
                            }
                            break;
                        case Constants.ADV_REQUEST_OP_SYNC_DEVICE_INFO: //请求同步设备信息
                            getBTHelper().requestDeviceInfo(device, new RcspCommandCallback() {
                                @Override
                                public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                                    JL_Log.d(TAG, "requestDeviceInfo response : " + cmd);
                                    if (cmd.getStatus() == StateCode.STATUS_SUCCESS) {
                                        TargetInfoResponse targetInfo = ((GetTargetInfoCmd) cmd).getResponse();
                                        if (targetInfo != null) {
                                            getBTHelper().addDeviceInfo(device, targetInfo);
                                            if (targetInfo.getMandatoryUpgradeFlag() == Constants.FLAG_MANDATORY_UPGRADE
                                                    || targetInfo.getRequestOtaFlag() == Constants.FLAG_MANDATORY_UPGRADE) { //需要强制升级
                                                onMandatoryUpgrade(device);
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onErrCode(BluetoothDevice device, BaseError error) {
                                    JL_Log.w(TAG, "requestDeviceInfo error : " + error);
                                }
                            });
                            break;
                    }
                }
                requestAdvOpCmd.setStatus(StateCode.STATUS_SUCCESS);
                getBTHelper().sendCmdResponse(requestAdvOpCmd);
                break;
            case Command.CMD_ADV_DEVICE_NOTIFY:
                NotifyAdvInfoCmd notifyAdvInfoCmd = (NotifyAdvInfoCmd) cmd;
                NotifyAdvInfoParam advInfo = notifyAdvInfoCmd.getParam();
                if (advInfo != null) {
                    ADVInfoResponse notifyAdvInfo = UIHelper.convertADVInfoFromBleScanMessage(UIHelper.convertBleScanMsgFromNotifyADVInfo(advInfo));
                    if (notifyAdvInfo != null) {
                        ADVInfoResponse cacheAdvInfo = DeviceStatusManager.getInstance().getAdvInfo(device);
                        boolean isCacheTwsConnected = cacheAdvInfo != null && cacheAdvInfo.getLeftDeviceQuantity() > 0 && cacheAdvInfo.getRightDeviceQuantity() > 0;
                        if (!notifyAdvInfo.equals(cacheAdvInfo)) {
                            DeviceStatusManager.getInstance().updateDeviceAdvInfo(device, notifyAdvInfo);
                            boolean isTwsConnected = notifyAdvInfo.getLeftDeviceQuantity() > 0 && notifyAdvInfo.getRightDeviceQuantity() > 0;
                            JL_Log.d(TAG, "-onTwsStatus- isCacheTwsConnected : " + isCacheTwsConnected + ", isTwsConnected : " + isTwsConnected);
                            if (isCacheTwsConnected != isTwsConnected) {
                                if (isTwsConnected) { //如果TWS已连接，同步左右设备的经纬度
                                    List<LocationInfo> locationInfos = UIHelper.getLocationInfosByHistoryDevice(DeviceAddrManager.getInstance().findHistoryBluetoothDevice(device));
                                    if (locationInfos.size() > 0) {
                                        DeviceAddrManager.getInstance().updateHistoryBtDeviceInfo(device, DeviceFlag.DEVICE_FLAG_MAIN,
                                                locationInfos.get(0).getLatitude(),
                                                locationInfos.get(0).getLongitude(),
                                                locationInfos.get(0).getUpdateTime());
                                        DeviceAddrManager.getInstance().updateHistoryBtDeviceInfo(device, DeviceFlag.DEVICE_FLAG_SUB,
                                                locationInfos.get(0).getLatitude(),
                                                locationInfos.get(0).getLongitude(),
                                                locationInfos.get(0).getUpdateTime());
                                    }
                                }
                                onTwsStatusChange(device, isTwsConnected);
                            }
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onDeviceData(final BluetoothDevice device, final byte[] data) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onDeviceData(device, data);
            }
        });
    }

    @Override
    public void onDeviceVoiceData(final BluetoothDevice device, final byte[] data) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onDeviceVoiceData(device, data);
            }
        });
    }

    @Override
    public void onDeviceVoiceData(final BluetoothDevice device, final VoiceData data) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onDeviceVoiceData(device, data);
            }
        });
    }

    @Override
    public void onDeviceVadEnd(final BluetoothDevice device) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onDeviceVadEnd(device);
            }
        });
    }

    @Override
    public void onDeviceResponse(BluetoothDevice device, CommandBase response) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onDeviceResponse(device, response);
            }
        });
    }

    @Override
    public void onError(final BaseError error) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onError(error);
            }
        });
    }

    public void onDeviceModeChange(final int mode) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onDeviceModeChange(mode);
            }
        });
    }

    public void onVolumeChange(final VolumeInfo volume) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onVolumeChange(volume);
            }
        });
    }

    public void onEqChange(final EqInfo eqInfo) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onEqChange(eqInfo);
            }
        });
    }

    public void onSoundCardEqChange(final EqInfo eqInfo) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onSoundCardEqChange(eqInfo);
            }
        });
    }

    private void onSoundCardStatusChange(long mask, byte[] values) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onSoundCardStatusChange(mask, values);
            }
        });
    }

    public void onEqPresetChange(final EqPresetInfo eqPresetInfo) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onEqPresetChange(eqPresetInfo);
            }
        });
    }

    public void onPhoneCallStatusChange(final int status) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onPhoneCallStatusChange(status);
            }
        });
    }

    public void onDevStorageInfoChange(final DevStorageInfo storageInfo) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onDevStorageInfoChange(storageInfo);
            }
        });
    }

    public void onFileFormatChange(final FileFormatInfo fileFormatInfo) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onFileFormatChange(fileFormatInfo);
            }
        });
    }

    public void onMusicNameChange(final MusicNameInfo nameInfo) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onMusicNameChange(nameInfo);
            }
        });
    }

    public void onMusicStatusChange(final MusicStatusInfo statusInfo) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onMusicStatusChange(statusInfo);
            }
        });
    }

    public void onPlayModeChange(final PlayModeInfo playModeInfo) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onPlayModeChange(playModeInfo);
            }
        });
    }

    public void onBatteryChange(final BatteryInfo batteryInfo) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onBatteryChange(batteryInfo);
            }
        });
    }

    public void onAuxStatusChange(final boolean isPlay) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onAuxStatusChange(isPlay);
            }
        });
    }

    public void onID3MusicInfoChange(final ID3MusicInfo id3MusicInfo) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onID3MusicInfo(id3MusicInfo);
            }
        });
    }

    public void onFixedLenData(final int type, final byte[] data) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onFixedLenData(type, data);
            }
        });
    }

    @Deprecated
    public void onMandatoryUpgrade() {
        if (MainApplication.getApplication().isOTA()) return;
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onMandatoryUpgrade();
            }
        });
    }

    public void onMandatoryUpgrade(final BluetoothDevice device) {
        if (MainApplication.getApplication().isOTA()) return;
        if (BluetoothUtil.deviceEquals(device, getBTHelper().getConnectedDevice())) {
            onMandatoryUpgrade();
        }
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onMandatoryUpgrade(device);
            }
        });
    }

    public void onFmChannelsChange(final List<ChannelInfo> channels) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onFmChannelsChange(channels);
            }
        });
    }

    public void onFmStatusChange(final FmStatusInfo fmStatusInfo) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onFmStatusChange(fmStatusInfo);
            }
        });
    }

    public void onAlarmListChange(final AlarmListInfo alarmListInfo) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onAlarmListChange(alarmListInfo);
            }
        });
    }

    private void onAlarmDefaultBellListChange(List<DefaultAlarmBell> bells) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onAlarmDefaultBellListChange(bells);
            }
        });
    }

    @Deprecated
    public void onAlarmNotify(final AlarmListInfo alarmListInfo) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onAlarmNotify(alarmListInfo);
            }
        });
    }

    public void onAlarmNotify(final AlarmBean alarmBean) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onAlarmNotify(alarmBean);
            }
        });
    }

    public void onAlarmStop() {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onAlarmStop();
            }
        });
    }

    public void onLightControlInfo(final LightControlInfo lightControlInfo) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onLightControlInfo(lightControlInfo);
            }
        });
    }

    public void onFrequencyTx(final float frequency) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onFrequencyTx(frequency);
            }
        });
    }

    public void onPeripheralsModeChange(final int mode) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onPeripheralsModeChange(mode);
            }
        });
    }

    public void onPeripheralsConnectStatusChange(final boolean connect, final String mac) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onPeripheralsConnectStatusChange(connect, mac);
            }
        });
    }

    public void onHandlerHighAndBassChange(final int high, final int value) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onHighAndBassChange(high, value);
            }
        });
    }

    public void onRemoveHistoryDeviceSuccess(final HistoryBluetoothDevice device) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onRemoveHistoryDeviceSuccess(device);
            }
        });
    }

    public void onRemoveHistoryDeviceFailed(final BaseError error) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onRemoveHistoryDeviceFailed(error);
            }
        });
    }

    public void onTwsStatusChange(final BluetoothDevice device, final boolean isTwsConnected) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onTwsStatusChange(device, isTwsConnected);
            }
        });
    }

    /**
     * 当前噪声处理模式信息
     *
     * @param device    使用设备
     * @param voiceMode 噪声处理模式
     */
    public void onCurrentVoiceMode(final BluetoothDevice device, final VoiceMode voiceMode) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onCurrentVoiceMode(device, voiceMode);
            }
        });
    }

    /**
     * 设备所有噪声处理模式信息
     *
     * @param device     使用设备
     * @param voiceModes 噪声处理模式信息列表
     */
    public void onVoiceModeList(final BluetoothDevice device, final List<VoiceMode> voiceModes) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onVoiceModeList(device, voiceModes);
            }
        });
    }

    public void parseAttrMessage(BluetoothDevice device, byte function, List<AttrBean> list) {
        if (list == null || getBTHelper().getDeviceInfo(device) == null) {
            return;
        }
        switch (function) {
            case AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC:
                parsePublicData(device, list);
                break;
            case AttrAndFunCode.SYS_INFO_FUNCTION_BT:
                parseBtData(device, list);
                break;
            case AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC:
                parseMusicData(device, list);
                break;
            case AttrAndFunCode.SYS_INFO_FUNCTION_RTC:
                parseRTCData(device, list);
                break;
            case AttrAndFunCode.SYS_INFO_FUNCTION_AUX:
                parseAUXData(device, list);
                break;
            case AttrAndFunCode.SYS_INFO_FUNCTION_FM:
                parseFMData(device, list);
                break;
        }
    }

    public void cleanID3MusicInfo() {
        id3MusicInfo = null;
    }

    public void onExternalFlashSysException(final BluetoothDevice device, final int status) {
        DeviceInfo deviceInfo = getBTHelper().getDeviceInfo(device);
        if (deviceInfo != null) deviceInfo.setFatFsException(status != 0);
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onExternalFlashSysException(device, status);
            }
        });
    }

    private void setReConnectDev(BluetoothDevice reConnectDev) {
        mReConnectDev = reConnectDev;
        if (reConnectDev != null) {
            mHandler.removeMessages(MSG_RECONNECT_DEVICE);
        } else {
            isReConnecting = false;
            mHandler.removeMessages(MSG_RECONNECT_TIMEOUT);
        }
    }

    private void parsePublicData(BluetoothDevice device, List<AttrBean> list) {
        if (list == null) return;
        for (AttrBean attr : list) {
            byte[] data = attr.getAttrData();
            if (data == null || data.length == 0) continue;
            switch (attr.getType()) {
                case AttrAndFunCode.SYS_INFO_ATTR_BATTERY:
                    onBatteryChange(new BatteryInfo(CHexConver.byteToInt(data[0])));
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_VOLUME:
                    int maxVol = getBTHelper().getDeviceInfo(device) == null ? 0 : getBTHelper().getDeviceInfo(device).getMaxVol();
                    boolean supportVolumeSync = getBTHelper().getDeviceInfo(device) != null && getBTHelper().getDeviceInfo(device).isSupportVolumeSync();
                    onVolumeChange(new VolumeInfo(maxVol, CHexConver.byteToInt(data[0]), supportVolumeSync));
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_MUSIC_DEV_STATUS:
                    byte status = data[0];
                    int usb = status & 0x01;
                    int sd0 = status & (0x01 << 1);
                    int sd1 = status & (0x01 << 2);
                    int flash = status & (0x01 << 3);
                    int usbHandle = 0;
                    int sd0Handle = 0;
                    int sd1Handle = 0;
                    int flashHandle = 0;
                    int offset = 1;
                    byte[] buf = new byte[4];
                    if (data.length > 4) {
                        System.arraycopy(data, offset, buf, 0, buf.length);
                        offset += buf.length;
                        usbHandle = CHexConver.bytesToInt(buf);
                    }
                    if (data.length > 8) {
                        System.arraycopy(data, offset, buf, 0, buf.length);
                        offset += buf.length;
                        sd0Handle = CHexConver.bytesToInt(buf);
                    }
                    if (data.length > 12) {
                        System.arraycopy(data, offset, buf, 0, buf.length);
                        offset += buf.length;
                        sd1Handle = CHexConver.bytesToInt(buf);
                    }
                    if (data.length > 16) {
                        System.arraycopy(data, offset, buf, 0, buf.length);
                        flashHandle = CHexConver.bytesToInt(buf);
                    }
                    DevStorageInfo devStorageInfo = new DevStorageInfo();
                    devStorageInfo.setUbsStatus(usb);
                    devStorageInfo.setSd0Status(sd0);
                    devStorageInfo.setSd1Status(sd1);
                    devStorageInfo.setFlashStatus(flash);
                    devStorageInfo.setUsbHandler(usbHandle);
                    devStorageInfo.setSd0Handler(sd0Handle);
                    devStorageInfo.setSd1Handler(sd1Handle);
                    devStorageInfo.setFlashHandler(flashHandle);
                    onDevStorageInfoChange(devStorageInfo);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_EQ:
                    boolean isDynamic = (data[0] & 0x80) == 0x80;
                    int eqMode = CHexConver.byteToInt(data[0]) & 0x7f;
                    EqInfo eqInfo = new EqInfo();
                    eqInfo.setMode(eqMode);
                    eqInfo.setDynamic(isDynamic);
                    if (isDynamic) {
                        int count = data[1];
                        byte[] gain = new byte[count];
                        System.arraycopy(data, 2, gain, 0, gain.length);
                        eqInfo.setValue(gain);
                        //统一EqInfo的输出格式，
                        EqPresetInfo eqPresetInfo = checkAndGetEqPresetInfo(list);
                        if (eqPresetInfo == null) {
                            eqPresetInfo = getBTHelper().getDeviceInfo(device).getEqPresetInfo();
                        }
                        if (eqPresetInfo != null) {
                            eqInfo.setFreqs(eqPresetInfo.getFreqs());
                        }
                        JL_Log.d(TAG, "eq   data  freq-->" + eqPresetInfo);
                    } else {
                        byte[] values = new byte[0];
                        if (data.length > 10) {
                            values = new byte[10];
                            System.arraycopy(data, 1, values, 0, values.length);
                        }
                        eqInfo.setValue(values);
                    }

                    onEqChange(eqInfo);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_EQ_PRESET_VALUE:
                    JL_Log.d(TAG, "eq preset data-->" + CHexConver.byte2HexStr(data, data.length));
                    EqPresetInfo eqPresetInfo = parseEqPresetInfo(attr);
                    onEqPresetChange(eqPresetInfo);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_FILE_TYPE:
                    onFileFormatChange(new FileFormatInfo(new String(data)));
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_CUR_MODE_TYPE:
                    JL_Log.i(TAG, "onDeviceModeChange >> " + CHexConver.byteToInt(data[0]));
                    onDeviceModeChange(CHexConver.byteToInt(data[0]));
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_LIGHT:
                    Log.e(TAG, "parsePublicData: SYS_INFO_ATTR_LIGHT");
                    int byte0Value = CHexConver.byteToInt(data[0]);
                    int switchState = byte0Value & 0x03;
                    int mode = (byte0Value & 0x0c) >>> 2;
                    int red = CHexConver.byteToInt(data[1]);
                    int green = CHexConver.byteToInt(data[2]);
                    int blue = CHexConver.byteToInt(data[3]);
                    int twinkleMode = CHexConver.byteToInt(data[4]);
                    int twinkleFreq = CHexConver.byteToInt(data[5]);
                    int sceneMode = CHexConver.byteToInt(data[6]);
                    int hue = CHexConver.bytesToInt(data[7], data[8]);
                    int saturation = CHexConver.byteToInt(data[9]);
                    int luminance = CHexConver.byteToInt(data[10]);
                    int color = Color.rgb(red, green, blue);
                    LightControlInfo lightControlInfo = new LightControlInfo();
                    lightControlInfo.setSwitchState(switchState);
                    lightControlInfo.setLightMode(mode);
                    lightControlInfo.setColor(color);
                    lightControlInfo.setTwinkleMode(twinkleMode);
                    lightControlInfo.setTwinkleFreq(twinkleFreq);
                    lightControlInfo.setSceneMode(sceneMode);
                    lightControlInfo.setHue(hue);
                    lightControlInfo.setSaturation(saturation);
                    lightControlInfo.setLuminance(luminance);
                    onLightControlInfo(lightControlInfo);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_FM_TX:
                    int value = 0;
                    byte[] temp;
                    if (data.length >= 2 && data.length < 4) {
                        temp = new byte[2];
                        System.arraycopy(data, 0, temp, 0, temp.length);
                        value = CHexConver.bytesToInt(temp[0], temp[1]);
                    } else if (data.length >= 4) {
                        temp = new byte[4];
                        System.arraycopy(data, 0, temp, 0, temp.length);
                        value = CHexConver.bytesToInt(temp);
                    }
                    float frequency = value / 10.0f;
                    onFrequencyTx(frequency);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_EMITTER_MODE:
                    onPeripheralsModeChange(CHexConver.byteToInt(data[0]));
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_EMITTER_CONNECT_STATUS:
                    byte[] mac = new byte[0];
                    boolean connect = data[0] == 0x01;
                    if (connect && data.length > 6) {
                        mac = new byte[6];
                        System.arraycopy(data, 1, mac, 0, mac.length);
                    }
                    onPeripheralsConnectStatusChange(connect, BluetoothUtil.hexDataCovetToAddress(mac));
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_HIGH_AND_BASS:
                    if (data.length >= 8) {
                        int bass = CHexConver.bytesToInt(data, 0, 4);
                        int high = CHexConver.bytesToInt(data, 4, 4);
                        onHandlerHighAndBassChange(high, bass);
                    } /*else {
                        throw new RuntimeException("长度错误");
                    }*/
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_CURRENT_NOISE_MODE:
                    if (data.length >= 9) {
                        byte[] noiseMode = new byte[9];
                        System.arraycopy(data, 0, noiseMode, 0, 9);
                        VoiceMode voiceMode = VoiceMode.parse(noiseMode);
                        DeviceInfo deviceInfo = getBTHelper().getDeviceInfo(device);
                        if (deviceInfo != null) deviceInfo.setCurrentVoiceMode(voiceMode);
                        onCurrentVoiceMode(device, voiceMode);
                    }
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_ALL_NOISE_MODE:
                    int index = 0;
                    int num = data[index];
                    index++;
                    if (num > 0 && data.length >= (index + num * 9)) {
                        byte[] vMode = new byte[9];
                        List<VoiceMode> voiceModes = new ArrayList<>();
                        for (int i = 0; i < num; i++) {
                            System.arraycopy(data, index, vMode, 0, vMode.length);
                            index += vMode.length;
                            voiceModes.add(VoiceMode.parse(vMode));
                        }
                        DeviceInfo deviceInfo = getBTHelper().getDeviceInfo(device);
                        if (deviceInfo != null) deviceInfo.setVoiceModeList(voiceModes);
                        onVoiceModeList(device, voiceModes);
                    }
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_PHONE_STATUS:
                    JL_Log.e("sen", "通话状态变化：" + CHexConver.byte2HexStr(data, data.length));
                    onPhoneCallStatusChange(data[0]);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_FIXED_LEN_DATA_FUN:
                    byte[] tempByteArray;
                    tempByteArray = new byte[data.length];
                    System.arraycopy(data, 0, tempByteArray, 0, tempByteArray.length);
                    parseFixedLenData(tempByteArray);
                    break;

                case SYS_INFO_ATTR_SOUND_CARD_EQ_FREQ: {
                    EqInfo micEqInfo = new EqInfo();
                    micEqInfo.setDynamic(true);
                    micEqInfo.setMode(0);
                    int total = data[0];
                    int[] freqs = new int[total];

                    for (int i = 0, j = 1; i < total; i++, j += 2) {
                        int freq = CHexConver.bytesToInt(data, j, 2);
                        freqs[i] = freq;
                    }
                    micEqInfo.setFreqs(freqs);
                    DeviceInfo deviceInfo = getBTHelper().getDeviceInfo(device);
                    deviceInfo.setSoundCardEqInfo(micEqInfo);
                }
                break;
                case SYS_INFO_ATTR_SOUND_CARD_EQ_GAIN: {
                    DeviceInfo deviceInfo = getBTHelper().getDeviceInfo(device);
                    if (deviceInfo == null) {
                        break;
                    }
                    EqInfo micEqInfo = deviceInfo.getSoundCardEqInfo();
                    if (micEqInfo == null) {
                        break;
                    }
                    int total = data[0];
                    byte[] gains = new byte[total];
                    System.arraycopy(data, 1, gains, 0, total);
                    micEqInfo.setValue(gains);
                    onSoundCardEqChange(micEqInfo);
                    break;
                }
                case SYS_INFO_ATTR_SOUND_CARD:
                    JL_Log.d(TAG, "声卡功能状态变化：" + CHexConver.byte2HexStr(data, data.length));
                    long mask = CHexConver.bytesToLong(data, 0, 8);
                    JL_Log.d(TAG, "声卡功能状态变化 ： mask  ->  " + CHexConver.byte2HexStr(data, 8));
                    byte[] values = new byte[data.length - 8];
                    System.arraycopy(data, 8, values, 0, values.length);
                    JL_Log.d(TAG, "声卡功能状态变化 ： values  ->  " + CHexConver.byte2HexStr(values, values.length));
                    onSoundCardStatusChange(mask, values);
                    break;
            }
        }
    }


    private ID3MusicInfo id3MusicInfo;
    private final int ID3_DATA_MAX_LEN = 64;

    private void parseBtData(BluetoothDevice device, List<AttrBean> list) {
        if (list == null) return;
        if (id3MusicInfo == null) {
            id3MusicInfo = new ID3MusicInfo();
        }
        boolean isID3MusicData = false;
        for (AttrBean attr : list) {
            byte[] data = attr.getAttrData();
            if (data == null || data.length == 0) continue;
            switch (attr.getType()) {
                case AttrAndFunCode.SYS_INFO_ATTR_ID3_TITLE:
                    isID3MusicData = true;
                    String title = null;
                    try {
                        title = new String(data, 0, data.length, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    JL_Log.i(TAG, "parseBtData id3 title: [" + CHexConver.byte2HexStr(data, data.length) + "], " + title);
                    if (title != null && data.length == ID3_DATA_MAX_LEN) {
                        title = title.substring(0, title.length() - 1) + "...";
                    }
                    if (!CHexConver.byte2HexStr(data, data.length).equals("00")) {
                        id3MusicInfo.setTitle(title);
                    } else {
                        id3MusicInfo.setTitle(null);
                    }
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_ID3_ARTIST:
                    isID3MusicData = true;
                    String artist = null;
                    try {
                        artist = new String(data, 0, data.length, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    JL_Log.i(TAG, "parseBtData id3 artist: " + "[" + CHexConver.byte2HexStr(data, data.length) + "], " + artist);
                    if (artist != null && data.length == ID3_DATA_MAX_LEN) {
                        artist = artist.substring(0, artist.length() - 1) + "...";
                    }
                    if (!CHexConver.byte2HexStr(data, data.length).equals("00")) {
                        id3MusicInfo.setArtist(artist);
                    } else {
                        id3MusicInfo.setArtist(null);
                    }
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_ID3_ALBUM:
                    isID3MusicData = true;
                    String album = null;
                    try {
                        album = new String(data, 0, data.length, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    JL_Log.i(TAG, "parseBtData id3 album: " + "[" + CHexConver.byte2HexStr(data, data.length) + "], " + album);
                    if (album != null && data.length == ID3_DATA_MAX_LEN) {
                        album = album.substring(0, album.length() - 1) + "...";
                    }
                    if (!CHexConver.byte2HexStr(data, data.length).equals("00")) {
                        id3MusicInfo.setAlbum(album);
                    } else {
                        id3MusicInfo.setAlbum(null);
                    }
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_ID3_NUMBER:
                    isID3MusicData = true;
                    int number = CHexConver.byteToInt(data[0]);
                    JL_Log.i(TAG, "parseBtData id3 number: " + number);
                    id3MusicInfo.setNumber(number);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_ID3_TOTAL:
                    isID3MusicData = true;
                    int allNum = 0;
                    if (data.length >= 2) {
                        allNum = CHexConver.bytesToInt(data, 0, 2);
                    }
                    JL_Log.i(TAG, "parseBtData id3 allNum: " + allNum);
                    id3MusicInfo.setTotal(allNum);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_ID3_GENRE:
                    isID3MusicData = true;
                    String genre = null;
                    try {
                        genre = new String(data, 0, data.length, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    JL_Log.i(TAG, "parseBtData id3 genre: " + genre);
                    id3MusicInfo.setGenre(genre);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_ID3_TOTAL_TIME:
                    isID3MusicData = true;
                    int allTime = 0;
                    JL_Log.i(TAG, "id3 data: [" + CHexConver.byte2HexStr(data, data.length) + "]");
                    if (data.length >= 2) {
                        allTime = CHexConver.bytesToInt(data, 0, 2);
                    }
                    JL_Log.i(TAG, "parseBtData id3 allTime: " + allTime);
                    id3MusicInfo.setTotalTime(allTime);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_ID3_PLAY_STATUS:
                    isID3MusicData = true;
                    boolean isPlay = (data[0] & 0x01) == 0x01;
                    JL_Log.i(TAG, "parseBtData id3 isPlay: " + isPlay);
                    id3MusicInfo.setPlayStatus(isPlay);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_ID3_CURRENT_TIME:
                    isID3MusicData = true;
                    int currentTime = 0;
                    if (data.length >= 4) {
                        currentTime = CHexConver.bytesToInt(data, 0, 4) / 1000;
                    }
                    JL_Log.i(TAG, "parseBtData id3 currentTime: " + currentTime);
                    id3MusicInfo.setCurrentTime(currentTime);
                    break;
            }
        }
        if (isID3MusicData) {
            onID3MusicInfoChange(id3MusicInfo);
        }
    }

    private void parseMusicData(BluetoothDevice device, List<AttrBean> list) {
        if (list == null) return;
        for (AttrBean attr : list) {
            byte[] data = attr.getAttrData();
            if (data == null || data.length == 0) continue;
            switch (attr.getType()) {
                case AttrAndFunCode.SYS_INFO_ATTR_MUSIC_STATUS_INFO:
                    boolean isPlay = (data[0] & 0x01) == 0x01;
                    int currentTime = 0;
                    int totalTime = 0;
                    int devIndex = 0;
                    int offset = 1;
                    if (data.length > 4) {
                        byte[] temp = new byte[4];
                        System.arraycopy(data, offset, temp, 0, temp.length);
                        offset += temp.length;
                        currentTime = CHexConver.bytesToInt(temp) * 1000; //秒转换成毫秒
                        if (data.length > 8) {
                            System.arraycopy(data, offset, temp, 0, temp.length);
                            offset += temp.length;
                            totalTime = CHexConver.bytesToInt(temp) * 1000; //秒转换成毫秒
                            if (data.length > 9) {
                                devIndex = CHexConver.byteToInt(data[offset]);
                            }
                        }
                    }
                    onMusicStatusChange(new MusicStatusInfo(isPlay, currentTime, totalTime, devIndex));
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_MUSIC_FILE_NAME_INFO:
                    if (data.length > 3) {
                        int index = 0;
                        byte[] buf = new byte[4];
                        System.arraycopy(data, index, buf, 0, buf.length);
                        index += buf.length;
                        int cluster = CHexConver.bytesToInt(buf);
                        String name = null;
                        if (data.length > 4) {
                            boolean isAsni = (data[index] & 0xff) == 0x01;
                            index++;
                            if (data.length > 5) {
                                try {
                                    name = new String(data, index, (data.length - index), isAsni ? "gbk" : "utf-16le");
                                    /*byte[] nameBuf = new byte[data.length - index];
                                    System.arraycopy(data, index, nameBuf, 0, nameBuf.length);
                                    JL_Log.d(TAG, "parseMusicData :: music name : " + name + ", raw data : " + CHexConver.byte2HexStr(nameBuf) +
                                            ", cluster : " + cluster + ", isAsni : " + isAsni);*/
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        onMusicNameChange(new MusicNameInfo(cluster, name));
                    }
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_MUSIC_PLAY_MODE:
                    int mode = CHexConver.byteToInt(data[0]);
                    JL_Log.d(TAG, "parseMusicData :: music play mode : " + mode);
                    onPlayModeChange(new PlayModeInfo(mode));
                    break;
            }
        }
    }

    private void parseRTCData(BluetoothDevice device, List<AttrBean> list) {
        if (list == null) return;
        int alarmVersion = getAlarmVersion(list);
        for (AttrBean attr : list) {
            byte[] data = attr.getAttrData();
            if (attr.getType() == AttrAndFunCode.SYS_INFO_ATTR_RTC_STOP_ALARM) {
                onAlarmStop();
                continue;
            }
            if (data == null || data.length == 0) continue;
            JL_Log.d(TAG, "parseRTCData-->" + CHexConver.byte2HexStr(data, data.length));
            switch (attr.getType()) {
                case AttrAndFunCode.SYS_INFO_ATTR_RTC_ALARM:
                    int counts = CHexConver.byteToInt(data[0]);
                    List<AlarmBean> alarmBeans = new ArrayList<>();
                    if (counts > 0) {
                        int offset = 1;
                        int start = 0;
                        for (int i = 0; i < counts; i++) {
                            start = offset;
                            if (data.length - offset > 4) {
                                byte index = data[offset++]; //索引号
                                boolean on = CHexConver.byteToInt(data[offset++]) == 0x01; // 开关
                                byte mode = data[offset++]; //模式
                                byte hour = data[offset++]; //时
                                byte min = data[offset++]; //分
                                String name = null;
                                int len = CHexConver.byteToInt(data[offset++]); //名称长度
                                if (len > 0) { //判断剩余数据是否足够
                                    try {
                                        name = new String(data, offset, len); //闹钟名称
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                offset += len;
                                byte bellType = 0;
                                String bellName = "";
                                int bellCluster = 0;
                                byte devIndex = 0;
                                //闹钟版本为0的时候固件可能没有发送版本过来，使用缓存的版本号
                                if (alarmVersion == 0 && getBTHelper().getDeviceInfo(device) != null) {
                                    alarmVersion = getBTHelper().getDeviceInfo(device).getAlarmVersion();
                                }
                                //闹钟结构版本为1时
                                if (alarmVersion == 1) {
                                    bellType = data[offset++];
                                    devIndex = data[offset++];
                                    byte[] cluster = new byte[4];
                                    System.arraycopy(data, offset, cluster, 0, 4);
                                    bellCluster = CHexConver.bytesToInt(cluster);
                                    offset += 4;
                                    int bellNameLen = data[offset++] & 0xff;
                                    bellName = new String(data, offset, bellNameLen); //闹钟名称
                                    offset += bellNameLen;
                                }
                                AlarmBean alarmBean = new AlarmBean()
                                        .setVersion(alarmVersion)
                                        .setIndex(index)
                                        .setRepeatMode(mode)
                                        .setName(name)
                                        .setDevIndex(devIndex)
                                        .setHour(hour)
                                        .setMin(min)
                                        .setBellCluster(bellCluster)
                                        .setBellName(bellName)
                                        .setBellType(bellType)
                                        .setOpen(on);
                                alarmBeans.add(alarmBean);
                            } else { //头数据不足
                                break;
                            }
                        }
                    }
                    AlarmListInfo alarmListInfo = new AlarmListInfo(alarmBeans);
                    alarmListInfo.setVersion(alarmVersion);
                    getBTHelper().getDeviceInfo(device).setAlarmListInfo(alarmListInfo);
                    onAlarmListChange(alarmListInfo);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_RTC_CURRENT_ALARM_INDEX:
                    int index = CHexConver.byteToInt(data[0]);
                    AlarmBean alarmBean = new AlarmBean();
                    alarmBean.setIndex((byte) index);
                    alarmBean.setName(AppUtil.getContext().getString(R.string.default_alarm_name));
                    if (getBTHelper().getDeviceInfo(device) != null && getBTHelper().getDeviceInfo(device).getAlarmListInfo() != null) {
                        List<AlarmBean> cacheList = getBTHelper().getDeviceInfo(device).getAlarmListInfo().getAlarmBeans();
                        AlarmListInfo alarmList = new AlarmListInfo(null);
                        List<AlarmBean> ret = new ArrayList<>();
                        if (cacheList != null) {
                            for (AlarmBean alarm : cacheList) {
                                if (((index >> alarm.getIndex()) & 0x01) == 0x01) {
                                    ret.add(alarm.copy());
                                }
                            }
                            alarmList.setAlarmBeans(ret);
                        }
                        onAlarmNotify(alarmList);
                        if (ret.size() > 0) {
                            alarmBean = ret.get(0);
                        }
                    }
                    onAlarmNotify(alarmBean);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_RTC_ALARM_VER:
                    JL_Log.d("sen", "parse alarm ver " + CHexConver.byte2HexStr(data));
                    getBTHelper().getDeviceInfo(device).setAlarmVersion(alarmVersion);
                    if (getBTHelper().getDeviceInfo(device).getAlarmListInfo() == null) {
                        getBTHelper().getDeviceInfo(device).setAlarmListInfo(new AlarmListInfo(alarmVersion, new ArrayList<>()));
                    } else {
                        getBTHelper().getDeviceInfo(device).getAlarmListInfo().setVersion(alarmVersion);
                    }
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_RTC_ALARM_DEFAULT_BELL_LIST:
                    JL_Log.e("sen", "parse alarm default bell " + CHexConver.byte2HexStr(data));
                    int total = data[0] & 0x0f;
                    List<DefaultAlarmBell> bells = new ArrayList<DefaultAlarmBell>();
                    for (int i = 1; i < data.length && bells.size() < total; ) {
                        int bellIndex = data[i++];
                        int bellNameLen = data[i++];
                        byte[] bellNameData = new byte[bellNameLen];
                        System.arraycopy(data, i, bellNameData, 0, bellNameLen);
                        i += bellNameLen;
                        DefaultAlarmBell bell = new DefaultAlarmBell(bellIndex, new String(bellNameData), false);
                        bells.add(bell);
                    }
                    onAlarmDefaultBellListChange(bells);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_RTC_ALARM_EXPAND_FLAG: {
                    if (getBTHelper().getDeviceInfo(device) != null) {
                        getBTHelper().getDeviceInfo(device).setAlarmExpandFlag(data[0]);
                    }
                }
            }
        }
    }

    private void parseAUXData(BluetoothDevice device, List<AttrBean> list) {
        if (list == null) return;
        for (AttrBean attr : list) {
            byte[] data = attr.getAttrData();
            if (data == null || data.length == 0) continue;
            if (attr.getType() == AttrAndFunCode.SYS_INFO_ATTR_AUX_STATU) {
                boolean isPlay = CHexConver.byteToInt(data[0]) == 1;
                JL_Log.i(TAG, "onAuxStatusChange >> " + isPlay);
                onAuxStatusChange(isPlay);
            }
        }
    }

    private void parseFMData(BluetoothDevice device, List<AttrBean> list) {
        if (list == null) return;
        for (AttrBean attr : list) {
            byte[] data = attr.getAttrData();
            if (data == null || data.length == 0) continue;
            switch (attr.getType()) {
                case AttrAndFunCode.SYS_INFO_ATTR_FM_FRE_INFO:
                    List<ChannelInfo> channelInfos = new ArrayList<>();
                    int offset = 1;
                    int dataLen = data.length;
                    while (dataLen - offset >= 3) {
                        int index = CHexConver.byteToInt(data[offset]);
                        offset++;
                        float freq = CHexConver.bytesToInt(data[offset], data[offset + 1]) / 10.0f;
                        offset += 2;
                        channelInfos.add(new ChannelInfo(index, freq));
                    }
                    onFmChannelsChange(channelInfos);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_FM_STATU:
                    boolean isPlay = CHexConver.byteToInt(data[0]) == 1;
                    int channel = 0;
                    float freq = 0.0f;
                    int mode = 0;
                    if (data.length > 1) {
                        channel = CHexConver.byteToInt(data[1]);
                        if (data.length > 3) {
                            freq = CHexConver.bytesToInt(data[2], data[3]) / 10.0f;
                            if (data.length > 4) {
                                mode = CHexConver.byteToInt(data[4]);
                            }
                        }
                    }
                    onFmStatusChange(new FmStatusInfo(isPlay, channel, freq, mode));
                    break;
            }
        }
    }

    private void parseFixedLenData(byte[] dataArray) {
        JL_Log.d(TAG, "parseFixedLenData-->" + CHexConver.byte2HexStr(dataArray));
        int mask = CHexConver.bytesToInt(dataArray, 0, 4);
        int position = 4;
        if ((MASK_REVERBERATION & mask) == MASK_REVERBERATION) {
            byte[] data = new byte[5];
            System.arraycopy(dataArray, position, data, 0, data.length);
            position = position + 5;
            onFixedLenData(FIXED_LEN_DATA_TYPE_REVERBERATION/*, opcode*/, data);
        }
        if ((MASK_DYNAMIC_LIMITER & mask) == MASK_DYNAMIC_LIMITER) {
            byte[] data = new byte[2];
            System.arraycopy(dataArray, position, data, 0, data.length);
            position = position + 2;
            onFixedLenData(FIXED_LEN_DATA_TYPE_DYNAMIC_LIMITER/*, opcode*/, data);
        }


    }

    private void notifyConnectStatus(final BluetoothDevice device, final int status) {
        handleBtCallback(new BtCallback() {
            @Override
            public void onCallback(BTEventCallback callback) {
                callback.onConnection(device, status);
            }
        });
    }

    private void handleConnectEvent(final BluetoothDevice device, final int status) {
        if (status != StateCode.CONNECTION_OK) {
            if (BluetoothUtil.deviceEquals(device, mReConnectDev)) {
                if (status == StateCode.CONNECTION_CONNECTING || status == StateCode.CONNECTION_DISCONNECT || status == StateCode.CONNECTION_FAILED) {
                    if (!mHandler.hasMessages(MSG_RECONNECT_DEVICE) && !isReConnecting) {
                        //开始延时回连
                        mHandler.sendEmptyMessageDelayed(MSG_RECONNECT_DEVICE, DELAY_TIME);
                    }
                    return; //10s内禁止回调失败状态
                } else if (status == StateCode.CONNECTION_CONNECTED) {
                    setReConnectDev(null);
                }
            } else if (status == StateCode.CONNECTION_DISCONNECT || status == StateCode.CONNECTION_FAILED) {
                id3MusicInfo = null;
                if ((getBTHelper().getConnectedDevice() == null || getBTHelper().isUseDevice(device))
                        && PlayControlImpl.getInstance().isPlay()) { //todo 设备断开连接，且是当前设备时，暂停本地音乐播放
                    PlayControlImpl.getInstance().pause();
                }

                //设备连接断开,隐藏闹钟对话框
                if (getBTHelper().getConnectedDevice() == null || getBTHelper().isUseDevice(device)) {
                    AlarmNotifyHandle.getInstance().onAlarmStop();
                }
            }
            notifyConnectStatus(device, status);
        } else {
            if (BluetoothUtil.deviceEquals(device, mReConnectDev)) {
                setReConnectDev(null);
            }
            TargetInfoResponse targetInfo = getBluetoothManager().getDeviceInfo(device);
            if (targetInfo == null) {
                getBTHelper().requestDeviceInfo(device, new RcspCommandCallback() {
                    @Override
                    public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                        if (cmd.getStatus() == StateCode.STATUS_SUCCESS) {
                            TargetInfoResponse targetInfo = ((GetTargetInfoCmd) cmd).getResponse();
                            if (targetInfo != null) {
                                getBTHelper().addDeviceInfo(device, targetInfo);
                                if (targetInfo.getMandatoryUpgradeFlag() == Constants.FLAG_MANDATORY_UPGRADE
                                        || targetInfo.getRequestOtaFlag() == Constants.FLAG_MANDATORY_UPGRADE) { //需要强制升级
                                    onMandatoryUpgrade(device);
                                }
                            }
                        }
                        notifyConnectStatus(device, status);
                    }

                    @Override
                    public void onErrCode(BluetoothDevice device, BaseError error) {
                        notifyConnectStatus(device, StateCode.CONNECTION_FAILED);
                    }
                });
            } else {
                getBTHelper().addDeviceInfo(device, targetInfo);
                notifyConnectStatus(device, status);
                if (targetInfo.getMandatoryUpgradeFlag() == Constants.FLAG_MANDATORY_UPGRADE
                        || targetInfo.getRequestOtaFlag() == Constants.FLAG_MANDATORY_UPGRADE) { //需要强制升级
                    onMandatoryUpgrade(device);
                }
            }
        }
    }

    private boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_RECONNECT_DEVICE:
                JL_Log.i(TAG, "handleMessage :: reconnect device start.");
                mHandler.removeMessages(MSG_RECONNECT_TIMEOUT);
                if (mReConnectDev != null) {
                    getBTHelper().connectToDevice(mReConnectDev); //开始回连
                    isReConnecting = true;
                    mHandler.sendEmptyMessageDelayed(MSG_RECONNECT_TIMEOUT, RECONNECT_TIMEOUT); //开始回连超时任务
                }
                break;
            case MSG_RECONNECT_TIMEOUT:
                JL_Log.i(TAG, "handleMessage :: reconnect timeout.");
                if (mReConnectDev != null) {
                    isReConnecting = false;
                    notifyConnectStatus(mReConnectDev, StateCode.CONNECTION_DISCONNECT);
                    setReConnectDev(null);
                }
                break;
        }
        return false;
    }


    public EqPresetInfo checkAndGetEqPresetInfo(List<AttrBean> list) {
        AttrBean attrBean = null;
        for (AttrBean bean : list) {
            if (bean.getType() == AttrAndFunCode.SYS_INFO_ATTR_EQ_PRESET_VALUE) {
                attrBean = bean;
                break;
            }
        }
        if (attrBean == null) {
            return null;
        }

        return parseEqPresetInfo(attrBean);
    }


    private EqPresetInfo parseEqPresetInfo(AttrBean attrBean) {
        EqPresetInfo eqPresetInfo = new EqPresetInfo();
        byte[] data = attrBean.getAttrData();
        List<EqInfo> eqInfos = new ArrayList<>();
        int count = data[0];
        int[] freqs = new int[count];
        int index = 1;
        for (int j = 0; j < count; index += 2, j++) {
            freqs[j] = ((data[index] & 0xff) << 8) | (data[index + 1] & 0xff);
        }

        for (int j = 0; j < 7; index += (count + 1), j++) {
            byte[] gain = new byte[count];
            byte mode = (byte) (data[index] & 0x7f);
            System.arraycopy(data, index + 1, gain, 0, gain.length);
            EqInfo preset = new EqInfo(mode, gain, freqs);
            preset.setDynamic((data[index] & 0x80) == 0x80);
//            preset.setDynamic(true);
            eqInfos.add(preset);
        }
        eqPresetInfo.setNumber(count);
        eqPresetInfo.setFreqs(freqs);
        eqPresetInfo.setEqInfos(eqInfos);
        JL_Log.e("sen", "eq--->" + eqPresetInfo.toString());
        return eqPresetInfo;
    }

    //获取闹钟结构版本
    private int getAlarmVersion(List<AttrBean> attrBeans) {
        for (AttrBean attrBean : attrBeans) {
            if (attrBean.getType() == AttrAndFunCode.SYS_INFO_ATTR_RTC_ALARM_VER) {
                int ver = attrBean.getAttrData()[0] & 0x07;
                return ver;
            }
        }
        return 0;
    }
}
