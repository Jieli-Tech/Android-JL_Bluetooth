package com.jieli.btsmart.tool.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.jieli.audio.media_player.JL_PlayMode;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.BluetoothOption;
import com.jieli.bluetooth.bean.base.AttrBean;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.command.tws.RequestAdvOpCmd;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.parameter.RequestAdvOpParam;
import com.jieli.bluetooth.bean.response.TargetInfoResponse;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.BluetoothOperationImpl;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.interfaces.IActionCallback;
import com.jieli.bluetooth.interfaces.bluetooth.CommandCallback;
import com.jieli.bluetooth.interfaces.bluetooth.IBluetoothOperation;
import com.jieli.bluetooth.interfaces.bluetooth.RcspCommandCallback;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.bluetooth.utils.CommandBuilder;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.bluetooth.utils.ParseDataUtil;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.bluetooth.BatteryInfo;
import com.jieli.btsmart.data.model.bluetooth.DefaultAlarmBell;
import com.jieli.btsmart.data.model.bluetooth.DeviceInfo;
import com.jieli.btsmart.data.model.bluetooth.EqInfo;
import com.jieli.btsmart.data.model.bluetooth.EqPresetInfo;
import com.jieli.btsmart.data.model.bluetooth.FileFormatInfo;
import com.jieli.btsmart.data.model.bluetooth.LightControlInfo;
import com.jieli.btsmart.data.model.bluetooth.MusicNameInfo;
import com.jieli.btsmart.data.model.bluetooth.MusicStatusInfo;
import com.jieli.btsmart.data.model.bluetooth.VolumeInfo;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.ui.LogService;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.EqCacheUtil;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.ToastUtil;
import com.jieli.jl_http.bean.LogResponse;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 蓝牙辅助类
 * <p>
 * 作用:实现蓝牙流程的接口
 * </p>
 *
 * @author zqjasonZhong
 * @date 2020/5/12
 * @deprecated 不建议使用BluetoothHelper的方法，推荐使用{@link com.jieli.bluetooth.impl.rcsp.RCSPController}的方式实现
 */
@Deprecated
public class BluetoothHelper {
    private final static String TAG = BluetoothHelper.class.getSimpleName();
    private static volatile BluetoothHelper instance;
    private final BTEventCallbackManager mCallbackManager;

    private JL_BluetoothManager mJLBluetoothManager;

    private final Map<String, DeviceInfo> mDeviceInfoMap = new HashMap<>();
    private final Map<String, Integer> mBusyCmdMap = new HashMap<>();

    private final static int RESEND_MAX_COUNT = 3;
    private final static int SCAN_TIME = 30 * 1000;
    private final static int REQUEST_CODE_UPLOAD_DEV_INFO = 6111;
    private final static int UPLOAD_DEV_INFO_INTERVAL = 10 * 60 * 1000;

    private BluetoothDevice mConnectingDev;
    private PendingIntent mUploadPI;
    private int mUploadInterval = UPLOAD_DEV_INFO_INTERVAL;

    private final static int MSG_UPLOAD_INFO = 0x6984;

    private final Handler mHandler = new Handler(Looper.getMainLooper(), msg -> {
        switch (msg.what) {
            case MSG_UPLOAD_INFO:
                uploadDevInfo();
                break;
        }
        return false;
    });

    private BluetoothHelper() {
        mCallbackManager = new BTEventCallbackManager(this);
        configureBluetoothManager();
        registerBTEventCallback(mBTEventCallback);
        registerBTEventCallback(AlarmNotifyHandle.getInstance());//添加闹钟状态处理回调
    }

    public static BluetoothHelper getInstance() {
        if (instance == null) {
            synchronized (BluetoothHelper.class) {
                if (instance == null) {
                    instance = new BluetoothHelper();
                }
            }
        }
        return instance;
    }

    public JL_BluetoothManager getBtManager() {
        if (mJLBluetoothManager == null) {
            mJLBluetoothManager = JL_BluetoothManager.getInstance(MainApplication.getApplication());
        }
        return mJLBluetoothManager;
    }

    public IBluetoothOperation getBtOperation() {
        return getBtManager().getBluetoothOperation();
    }

    public BTEventCallbackManager getCallbackManager() {
        return mCallbackManager;
    }

    public DeviceInfo getDeviceInfo() {
        return getDeviceInfo(getConnectedDevice());
    }

    public DeviceInfo getDeviceInfo(BluetoothDevice device) {
        String address = device == null ? null : device.getAddress();
        return getDeviceInfo(address);
    }

    public DeviceInfo getDeviceInfo(String address) {
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            return mDeviceInfoMap.get(address);
        }
        return null;
    }

    /**
     * 设置当前设备信息
     * <p>
     * 多设备模式下错误设置设备信息，弃用
     * </p>
     *
     * @param targetInfo 设备信息
     */
    @Deprecated
    public void setDeviceInfo(TargetInfoResponse targetInfo) {
        addDeviceInfo(getConnectedDevice(), targetInfo);
    }

    public void addDeviceInfo(BluetoothDevice device, TargetInfoResponse targetInfo) {
        if (null == device) return;
        String address = device.getAddress();
        DeviceInfo deviceInfo = DeviceInfo.convertFromTargetInfo(targetInfo);
        DeviceInfo oldDevInfo = getDeviceInfo(address);
        if (oldDevInfo != null) {
            deviceInfo.setVoiceModeList(oldDevInfo.getVoiceModeList())
                    .setCurrentVoiceMode(oldDevInfo.getCurrentVoiceMode())
                    .setAlarmVersion(oldDevInfo.getAlarmVersion())
                    .setAlarmDefaultBells(oldDevInfo.getAlarmDefaultBells())
                    .setAlarmExpandFlag(oldDevInfo.getAlarmExpandFlag())
                    .setAlarmListInfo(oldDevInfo.getAlarmListInfo())
                    .setMusicNameInfo(oldDevInfo.getMusicNameInfo())
                    .setMusicStatusInfo(oldDevInfo.getMusicStatusInfo())
                    .setFatFsException(oldDevInfo.isFatFsException())
                    .setEqInfo(oldDevInfo.getEqInfo())
                    .setEqPresetInfo(oldDevInfo.getEqPresetInfo())
                    .setSoundCardEqInfo(oldDevInfo.getSoundCardEqInfo())
                    .setLightControlInfo(oldDevInfo.getLightControlInfo())
                    .setCluster(oldDevInfo.getCluster())
                    .setCurrentDevIndex(oldDevInfo.getCurrentDevIndex());
        }
        addDeviceInfo(address, deviceInfo);
    }

    public void addDeviceInfo(final String address, DeviceInfo deviceInfo) {
        if (BluetoothAdapter.checkBluetoothAddress(address) && deviceInfo != null) {
            mDeviceInfoMap.put(address, deviceInfo);
            if (deviceInfo.isRTCEnable()) {
                syncTime();
            }
            AppUtil.saveDeviceSupportSearchStatus(address, deviceInfo.isSupportSearchDev());
        }
    }

    private void removeDeviceInfo(BluetoothDevice device) {
        if (device != null) removeDeviceInfo(device.getAddress());
    }

    private void removeDeviceInfo(String address) {
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            mDeviceInfoMap.remove(address);
        }
    }

    public boolean isDevConnected() {
        TargetInfoResponse targetInfoResponse = getBtManager().getDeviceInfo(getConnectedDevice());
        boolean ret = getConnectedDevice() != null && targetInfoResponse != null;
        if (ret && getDeviceInfo(getConnectedDevice()) == null) {
            addDeviceInfo(getConnectedDevice(), targetInfoResponse);
        }
        return ret;
    }

    public BluetoothDevice getConnectedDevice() {
        return getBtManager().getConnectedDevice();
    }

    public void registerBTEventCallback(BTEventCallback eventCallback) {
        if (eventCallback != null && mCallbackManager != null) {
            mCallbackManager.registerBTEventCallback(eventCallback);
        }
    }

    public void unregisterBTEventCallback(BTEventCallback eventCallback) {
        if (eventCallback != null && mCallbackManager != null) {
            mCallbackManager.unregisterBTEventCallback(eventCallback);
        }
    }

    public void fastConnect() {
        getBtManager().fastConnect();
    }

    public void startBleScan() {
        startBtScan(BluetoothConstant.PROTOCOL_TYPE_BLE, SCAN_TIME);
    }

    public void startBtScan(int type, int timeout) {
        getBtManager().scan(type, timeout);
    }

    public void stopScan() {
        getBtManager().stopScan();
    }

    public boolean isDevConnecting() {
        return getBtOperation().isConnecting();
    }

    public void connectToDevice(BluetoothDevice device) {
        getBtManager().connect(device);
    }

    public void connectToDevice(BluetoothDevice device, BleScanMessage bleScanMessage) {
        if (!PermissionUtil.checkHasConnectPermission(MainApplication.getApplication())) return;
        int way = BluetoothConstant.PROTOCOL_TYPE_BLE;
        if (bleScanMessage != null) {
            way = bleScanMessage.getConnectWay();
            if (way == BluetoothConstant.PROTOCOL_TYPE_SPP) {
                device = BluetoothUtil.getRemoteDevice(bleScanMessage.getEdrAddr());
            }
        } else if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            way = BluetoothConstant.PROTOCOL_TYPE_SPP;
        }
        DeviceAddrManager.getInstance().saveDeviceConnectWay(device, way);

        if (checkCanConnectToDevice(device)) {
            //todo 当没有设备连接的时候才暂停播放，有设备连接的情况下不暂停
            if (getConnectedDevice() == null && PlayControlImpl.getInstance().isPlay()) {
                PlayControlImpl.getInstance().pause();
            }
            getBtManager().connect(device);
        }
    }

    private boolean checkConnectedEdrIsOverLimit(BluetoothDevice device) {
        if (!PermissionUtil.checkHasConnectPermission(MainApplication.getApplication())) return true;
        boolean ret;
        List<BluetoothDevice> devices = BluetoothUtil.getSystemConnectedBtDeviceList(MainApplication.getApplication());
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

    //检测是否可以去连接设备
    private boolean checkCanConnectToDevice(BluetoothDevice device) {
        if (device == null) {
            return false;
        } else if (!BluetoothUtil.isBluetoothEnable()) {
            ToastUtil.showToastShort(MainApplication.getApplication().getString(R.string.bluetooth_not_enable));
            return false;
        } else if (isDevConnecting()) {
            ToastUtil.showToastShort(MainApplication.getApplication().getString(R.string.device_connecting_tips));
            return false;
        } else if (checkConnectedEdrIsOverLimit(device)) { //连接设备已达到上限
            ToastUtil.showToastShort(MainApplication.getApplication().getString(R.string.connect_device_over_limit));
            return false;
        }
        return true;
    }

    public void disconnectDevice() {
        if (isDevConnected()) {
            getBtManager().disconnect();
        }
    }

    public void disconnectDeviceAndBanReConnect() {
        if (isDevConnected()) {
            getBtManager().disconnect();
            DeviceAddrManager.getInstance().removeCacheBluetoothDeviceAddr();
        }
    }

    public void removeHistoryBtDevice(HistoryBluetoothDevice historyBtDevice, IActionCallback<HistoryBluetoothDevice> callback) {
        getBtManager().removeHistoryDevice(historyBtDevice, new OnHistoryBtDeviceCallback(mCallbackManager, callback));
    }

    public List<BluetoothDevice> getConnectedDeviceList() {
        return getBtManager().getConnectedDeviceList();
    }

    public boolean isConnectedDevice(BluetoothDevice device) {
        return getBtManager().isConnectedBtDevice(device);
    }

    public boolean isUseDevice(BluetoothDevice device) {
        return getBtManager().isUseBtDevice(device);
    }

    public void switchConnectedDevice(BluetoothDevice device) {
        getBtManager().switchConnectedDevice(device);
    }

    public void destroy() {
        unregisterBTEventCallback(mBTEventCallback);
        stopUploadDevInfo();
        mHandler.removeCallbacksAndMessages(null);
        if (mJLBluetoothManager != null) {
            mJLBluetoothManager.destroy();
        }
        if (mCallbackManager != null) {
            mCallbackManager.destroy();
        }
        mDeviceInfoMap.clear();
        mBusyCmdMap.clear();
        instance = null;
    }

    /* --=---=--===----======-----=-=-=-=-----=----=--=-----=----====-=-=-=-------=--======-=-= *
     * 基本数据处理API
     * ---===---=====--------=-======-------=-=-=-=-=-=-=-=-=-=-=-=-=--------=-=-=-=-=---=----- */

    /**
     * 发送命令
     *
     * @param command  命令对象
     * @param callback 命令回调
     */
    public void sendCommand(CommandBase command, CommandCallback callback) {
        sendCommand(command, mJLBluetoothManager.getBluetoothOption().getTimeoutMs(), callback);
    }

    /**
     * 发送命令
     *
     * @param device   已连接设备
     * @param command  命令对象
     * @param callback 命令回调
     */
    public void sendCommand(BluetoothDevice device, CommandBase command, RcspCommandCallback callback) {
        sendCommand(device, command, mJLBluetoothManager.getBluetoothOption().getTimeoutMs(), callback);
    }

    /**
     * 发送命令
     *
     * @param command  命令对象
     * @param timeout  超时时间
     * @param callback 命令回调
     */
    @Deprecated
    public void sendCommand(CommandBase command, int timeout, CommandCallback callback) {
        getBtManager().sendCommandAsync(command, timeout, new CommandCallback() {
            @Override
            public void onCommandResponse(CommandBase cmd) {
                BluetoothDevice device = getConnectedDevice();
                if (StateCode.STATUS_BUSY == cmd.getStatus() && getBusyCmdReSendCount(device, cmd) < RESEND_MAX_COUNT) {
                    final CommandCallback oldCallback = this;
                    int reSendCount = getBusyCmdReSendCount(device, cmd) + 1;
                    addBusyCmd(device, cmd, reSendCount);
                    mHandler.postDelayed(() -> getBtManager().sendCommandAsync(command, timeout, oldCallback), 500);
                } else {
                    removeBusyCmdCount(device, cmd);
                    if (callback != null) {
                        callback.onCommandResponse(cmd);
                    }
                }
            }

            @Override
            public void onErrCode(BaseError error) {
                removeBusyCmdCount(getConnectedDevice(), command);
                if (callback != null) {
                    callback.onErrCode(error);
                }
            }
        });
    }

    /**
     * 发送命令
     *
     * @param device   已连接设备
     * @param command  命令对象
     * @param timeout  超时时间
     * @param callback 命令回调
     */
    public void sendCommand(final BluetoothDevice device, final CommandBase command, final int timeout, final RcspCommandCallback callback) {
        getBtManager().sendCommandAsync(device, command, timeout, new RcspCommandCallback() {
            @Override
            public void onCommandResponse(BluetoothDevice device1, CommandBase cmd) {
                if (StateCode.STATUS_BUSY == cmd.getStatus() && getBusyCmdReSendCount(device1, cmd) < RESEND_MAX_COUNT) {
                    final RcspCommandCallback oldCallback = this;
                    int reSendCount = getBusyCmdReSendCount(device1, cmd) + 1;
                    addBusyCmd(device1, cmd, reSendCount);
                    mHandler.postDelayed(() -> getBtManager().sendCommandAsync(device, command, timeout, oldCallback), 500);
                } else {
                    removeBusyCmdCount(device1, cmd);
                    if (callback != null) {
                        callback.onCommandResponse(device1, cmd);
                    }
                }
            }

            @Override
            public void onErrCode(BluetoothDevice device1, BaseError error) {
                removeBusyCmdCount(device1, command);
                if (callback != null) {
                    callback.onErrCode(device1, error);
                }
            }
        });
    }

    /**
     * 发送回复命令
     *
     * @param response 回复命令
     */
    public void sendCmdResponse(CommandBase response) {
        getBtManager().sendCommandResponse(response);
    }

    /**
     * 向已连接设备发送裸数据
     *
     * @param device 已连接设备对象
     * @param data   裸数据
     * @return 结果
     */
    public boolean sendRawDataToDevice(BluetoothDevice device, byte[] data) {
        return getBtManager().sendDataToDevice(device, data);
    }


    /* --=---=--===----======-----=-=-=-=-----=----=--=-----=----====-=-=-=-------=--======-=-= *
     * 命令快捷调用API
     * ---===---=====--------=-======-------=-=-=-=-=-=-=-=-=-=-=-=-=--------=-=-=-=-=---=----- */

    /**
     * 获取设备信息
     *
     * @param device   已连接设备
     * @param callback 命令回调
     */
    public void requestDeviceInfo(BluetoothDevice device, RcspCommandCallback callback) {
        sendCommand(device, CommandBuilder.buildGetTargetInfoCmd(0xffffffff), callback);
    }

    /* ------------ 切换模式-------------------------------------------------------->>>>
     */
    public void switchBTMode(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildSwitchBtModeCmd(), commandCallback);
    }

    public void switchMusicMode(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildSwitchMusicModeCmd(), commandCallback);
    }

    public void switchFMMode(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildSwitchFMModeCmd(), commandCallback);
    }

    public void switchLineInMode(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildSwitchLineInModeCmd(), commandCallback);
    }
    /* ------------ Get Device Mode Info-------------------------------------------------------->>>>
     */

    public void getFileFormat(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetBrowseFileTypeCmd(), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    public void getStorageInfo(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetPublicSysInfoCmd(0x01 << 2), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    public void getCurrentDevModeInfo(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetPublicSysInfoCmd(0x01 << 6), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    public void getDeviceMusicInfo(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetMusicSysInfoCmd(0x07), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    public void getDeviceMusicStatusInfo(final CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetMusicStatusInfoCmd(), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    public void getFmInfo(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetFmSysInfoCmd((byte) 0x3), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    public void getAuxStatusInfo(final CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetAuxPlayStatueCmd(), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    public void getEqInfo(final CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetEqValueCmd(), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    public void getSysPublicInfo(int mask, final CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetSysInfoCmd(AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC, mask), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    public void getFrequencyInfo(CommandCallback callback) {
        sendCommand(CommandBuilder.buildGetFrequencyTxInfoCmd(), new GetSysCommandCallback(mCallbackManager, callback));
    }

    public void setFrequencyInfo(float frequency, CommandCallback callback) {
        sendCommand(CommandBuilder.buildSetFrequencyTxInfoCmd(frequency), new GetSysCommandCallback(mCallbackManager, callback));
    }

    public void getFixedLenDataInfo(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetFixedLenDataCmd(), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    public void setFixedLenDataInfo(CommandCallback commandCallback, int mask, byte[] dataArray) {
        sendCommand(CommandBuilder.buildSetFixedLenDataCmd(mask, dataArray), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }


    public void getSoundCardEqInfo(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetSoundCardEqInfo(), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    public void getSoundCardStatusInfo(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetSoundCardStatusInfo(), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    public void setSoundCardEqInf(byte[] value, CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildSetSoundCardEqValue(value), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }


    public void getLightControlInfo(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetLightControlInfoCmd(), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    public void setLightControlInfo(CommandCallback commandCallback, LightControlInfo lightControlInfo) {
        sendCommand(CommandBuilder.buildSetLightControlInfoCmd(lightControlInfo.toByteArray()), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    public void getAllVoiceModes(CommandCallback callback) {
        sendCommand(CommandBuilder.buildGetAllVoiceModes(), new GetSysCommandCallback(mCallbackManager, callback));
    }

    public void getCurrentVoiceMode(CommandCallback callback) {
        sendCommand(CommandBuilder.buildGetCurrentVoiceMode(), new GetSysCommandCallback(mCallbackManager, callback));
    }

    public void setCurrentVoiceMode(VoiceMode voiceMode, CommandCallback callback) {
        sendCommand(CommandBuilder.buildSetCurrentVoiceMode(voiceMode), new GetSysCommandCallback(mCallbackManager, callback));
    }
    /* ------------ Player Op -------------------------------------------------------->>>>
     */

    public void deviceMusicPlayNext(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildPlayNextCmd(), commandCallback);

    }

    public void deviceMusicPlayPrev(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildPlayPrevCmd(), commandCallback);
    }

    public void deviceMusicPlay(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildPlayOrPauseCmd(), commandCallback);
    }

    public void deviceMusicPause(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildPlayOrPauseCmd(), commandCallback);
    }

    public void deviceMusicSetPlayMode(JL_PlayMode jl_playMode, CommandCallback commandCallback) {

    }

    public void deviceMusicNextPlayMode(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildNextPlaymodeCmd(), commandCallback);
    }

    public void ID3MusicPlayLast(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildID3PlayPrevCmd(), commandCallback);
    }

    public void ID3MusicPlayPlayOrPause(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildID3PlayOrPauseCmd(), commandCallback);
    }

    public void ID3MusicPlayNext(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildID3PlayNextCmd(), commandCallback);
    }

    public void ID3MusicPlayDataPushClose(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildID3DataPushSwitch((byte) 0x00), commandCallback);
    }

    public void ID3MusicPlayDataPushOpen(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildID3DataPushSwitch((byte) 0x01), commandCallback);
    }

    public void ID3MusicAllInfoData(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetBtSysInfoCmd(0x1ff), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    /* ------------ FM About -------------------------------------------------------->>>>
     */
    public void getFMInfo(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetFmStatueCmd(), commandCallback);
    }

    public void fmScanForward(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildFmScanCmd((byte) 0x01), commandCallback);
    }

    public void fmScanALll(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildFmScanCmd((byte) 0x00), commandCallback);
    }

    public void fmScanBackward(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildFmScanCmd((byte) 0x02), commandCallback);
    }

    public void fmScanStop(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildFmScanCmd((byte) 0x03), commandCallback);
    }

    public void fmPlayOrPause(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildFmPlayOrPauseCmd(), commandCallback);
    }

    public void fmPlayPrevChannel(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildFmPrevChannelCmd(), commandCallback);
    }

    public void fmPlayNextChannel(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildFmNextChannelCmd(), commandCallback);
    }

    public void fmPlayPrevFreq(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildFmPrevFreqCmd(), commandCallback);
    }

    public void fmPlayNextFreq(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildFmNextFreqCmd(), commandCallback);
    }

    public void fmPlaySelectFreq(float freq, CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildFmSelectFreqCmd(freq), commandCallback);
    }

    /**
     * 音量调节
     *
     * @param value 音量
     */
    public void adjustVolume(int value) {
        this.adjustVolume(value, null);
    }

    /**
     * 音量调节
     *
     * @param value    音量
     * @param callback 回调
     */
    public void adjustVolume(final int value, final CommandCallback callback) {
        if (!isDevConnected()) {
            AudioManager audioManager = (AudioManager) MainApplication.getApplication().getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, AudioManager.FLAG_SHOW_UI);
            }
            return;
        }
        sendCommand(CommandBuilder.buildSetVolumeCmd(value), new CommandCallback() {
            @Override
            public void onCommandResponse(CommandBase cmd) {
                if (callback != null) {
                    callback.onCommandResponse(cmd);
                }
                if (cmd.getStatus() == StateCode.STATUS_SUCCESS) {
                    //兼容AC692
                    VolumeInfo volumeInfo = new VolumeInfo(getDeviceInfo().getMaxVol(), value, getDeviceInfo().isSupportVolumeSync());
                    getDeviceInfo().setVolume(value);
                    mCallbackManager.onVolumeChange(volumeInfo);
                }
            }

            @Override
            public void onErrCode(BaseError error) {
                if (callback != null) {
                    callback.onErrCode(error);
                }
            }
        });
    }

    /* ------------ AUX Op -------------------------------------------------------->>>>
     */

    public void auxPlayOrPause(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildAuxPlayOrPauseCmd(), commandCallback);
    }

    /* ------------ RTC Op -------------------------------------------------------->>>>
     */
    public void syncTime() {
        JL_Log.e(TAG, "syncTime");
        sendCommand(CommandBuilder.buildSyncTimeCmd(), new CommandCallback() {
            @Override
            public void onCommandResponse(CommandBase cmd) {
                JL_Log.e(TAG, "syncTime success-->" + cmd);
                readAlarmList(null);
            }

            @Override
            public void onErrCode(BaseError error) {
                JL_Log.e(TAG, "-getSysInfoCmd- error : " + error.toString());
            }
        });
    }

    //读取闹钟列表
    public void readAlarmList(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetAlarmCmd(), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }

    //读取闹钟列表
    public void readAlarmDefaultBellList(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildGetDefaultBellList(), new GetSysCommandCallback(mCallbackManager, commandCallback));
    }


    //删除闹钟
    public void delAlarm(AttrBean attrBean, CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildDelAlarmCmd(attrBean), commandCallback);
    }

    //修改增加闹钟
    public void addAndChangeAlarm(AttrBean attrBean, CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildSetAlarmCmd(attrBean), commandCallback);
    }

    public void stopAlarm(CommandCallback commandCallback) {
        sendCommand(CommandBuilder.buildStopAlarmCmd(), commandCallback);
    }

    public boolean checkDeviceIsConnecting(BluetoothDevice device) {
        boolean isConnecting = false;
        BluetoothOperationImpl bluetoothOperation = (BluetoothOperationImpl) getBtOperation();
        if ((bluetoothOperation.isConnecting() && BluetoothUtil.deviceEquals(device, bluetoothOperation.getConnectingDevice()))
                || BluetoothUtil.deviceEquals(device, bluetoothOperation.getConnectingBrEdrDevice())) {
            isConnecting = true;
        }
        return isConnecting;
    }

    public void updateDevTime() {
        Calendar calendar = Calendar.getInstance();
        int time = (int) (calendar.getTimeInMillis() / 1000);
        updateDevTime(time);
    }


    public void updateDevTime(int time) {
        sendCommand(CommandBuilder.buildSetADVInfoCmd(ParseDataUtil.packLTVPacket(AttrAndFunCode.ADV_TYPE_CONNECTED_TIME,
                CHexConver.intToBigBytes(time))), new CommandCallback() {
            @Override
            public void onCommandResponse(CommandBase cmd) {
                JL_Log.i(TAG, "updateDevTime :: cmd = " + cmd);
            }

            @Override
            public void onErrCode(BaseError error) {
                JL_Log.e(TAG, "updateDevTime :: onErrCode = " + error);
            }
        });
    }

    /* ----------------------------------------------------------------------------------------- *
     * private way
     * ----------------------------------------------------------------------------------------- */

    private void configureBluetoothManager() {
        BluetoothOption bluetoothOption = new BluetoothOption()
                .setPriority(BluetoothOption.PREFER_BLE)
                .setReconnect(true)
                .setUseMultiDevice(true)
                .setBleIntervalMs(500)
                .setTimeoutMs(2000)
                .setMtu(BluetoothConstant.BLE_MTU_MAX)
                .setScanFilterData("JLAISDK")
                .setUseDeviceAuth(PreferencesHelper.getSharedPreferences(MainApplication.getApplication())
                        .getBoolean(SConstant.KEY_USE_DEVICE_AUTH, SConstant.IS_USE_DEVICE_AUTH));
        getBtManager().configure(bluetoothOption);
        if (!getBtManager().isBluetoothEnabled()) {
            getBtManager().openOrCloseBluetooth(true);
        } else {
            fastConnect();
        }
    }

    private String getBusyMapKey(BluetoothDevice device, CommandBase cmd) {
        if (cmd == null) return null;
        if (device == null) {
            return cmd.getId() + "_" + cmd.getOpCodeSn();
        } else {
            return device.getAddress() + "_" + cmd.getId() + "_" + cmd.getOpCodeSn();
        }
    }

    private void addBusyCmd(BluetoothDevice device, CommandBase cmd, int reSendCount) {
        String key = getBusyMapKey(device, cmd);
        if (null != key) {
            mBusyCmdMap.put(key, reSendCount);
        }
    }

    private int getBusyCmdReSendCount(BluetoothDevice device, CommandBase cmd) {
        String key = getBusyMapKey(device, cmd);
        if (null != key) {
            Integer value = mBusyCmdMap.get(key);
            if (value != null) {
                return value;
            }
        }
        return 0;
    }

    private void removeBusyCmdCount(BluetoothDevice device, CommandBase cmd) {
        String key = getBusyMapKey(device, cmd);
        if (null != key) {
            mBusyCmdMap.remove(key);
        }
    }

    private void handleDevConnectionEvent(BluetoothDevice device, int status) {
        switch (status) {
            case StateCode.CONNECTION_CONNECTING:
                mConnectingDev = device;
                break;
            case StateCode.CONNECTION_OK:
                startUploadDevInfo();
                mConnectingDev = null;
                if (device != null) {
                    //获取设备当前模式
                    getCurrentDevModeInfo(null);
                }
                break;
            case StateCode.CONNECTION_CONNECTED:
                mConnectingDev = null;
                break;
            case StateCode.CONNECTION_FAILED:
            case StateCode.CONNECTION_DISCONNECT:
                if (BluetoothUtil.deviceEquals(device, mConnectingDev)) {
                    ToastUtil.showToastShort(R.string.bt_connect_failed);
                    mConnectingDev = null;
                }
                removeDeviceInfo(device);
                stopUploadDevInfo();
                RingHandler.getInstance().stopAlarmRing();
                break;

        }
    }

    private void startUploadDevInfo() {
        MainApplication application = MainApplication.getApplication();
        if (application == null) return;
        LogResponse logResponse = application.getLogResponse();
        mUploadInterval = logResponse == null ? UPLOAD_DEV_INFO_INTERVAL : logResponse.getDevUploadInterval() * 60 * 1000;
        mHandler.removeMessages(MSG_UPLOAD_INFO);
        mHandler.sendEmptyMessage(MSG_UPLOAD_INFO);
    }

    @SuppressLint("WrongConstant")
    private void uploadDevInfo() {
        MainApplication application = MainApplication.getApplication();
        if (application == null) return;
        if (mUploadPI == null) {
            Intent intent = new Intent(application, LogService.class);
            intent.setAction(LogService.ACTION_UPLOAD_DEVICE_MSG);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags = PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
            }
            mUploadPI = PendingIntent.getService(application, REQUEST_CODE_UPLOAD_DEV_INFO, intent, flags);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !PermissionUtil.isHasPermission(application, Manifest.permission.SCHEDULE_EXACT_ALARM)) {
            JL_Log.i(TAG, "uploadDevInfo : no alarm permission.");
            return;
        }
        AppUtil.startTimerTask(application, 0, mUploadPI);
        mHandler.removeMessages(MSG_UPLOAD_INFO);
        mHandler.sendEmptyMessageDelayed(MSG_UPLOAD_INFO, mUploadInterval);
    }


    private void stopUploadDevInfo() {
        MainApplication application = MainApplication.getApplication();
        if (application == null) return;
        mHandler.removeMessages(MSG_UPLOAD_INFO);
        if (mUploadPI != null) {
            AppUtil.stopTimerTask(application, mUploadPI);
            mUploadPI = null;
        }
        application.stopService(new Intent(application, LogService.class));
    }

    /* ----------------------------------------------------------------------------------------- *
     * Callback or Listener
     * ----------------------------------------------------------------------------------------- */
    private final BTEventCallback mBTEventCallback = new BTEventCallback() {
        @Override
        public void onAdapterStatus(boolean bEnabled, boolean bHasBle) {
            if (!bEnabled) { //蓝牙适配器关闭
                mDeviceInfoMap.clear();
                mBusyCmdMap.clear();
            }
        }

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            handleDevConnectionEvent(device, status);
            EqCacheUtil.clear();//todo 清除eq缓存
        }

        @Override
        public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
            if (device == null || cmd == null) return;
            if (cmd.getId() == Command.CMD_ADV_DEV_REQUEST_OPERATION) {
                RequestAdvOpCmd requestAdvOpCmd = (RequestAdvOpCmd) cmd;
                //  JL_Log.i(TAG, "receive request op cmd : " + requestAdvOpCmd);
                RequestAdvOpParam param = requestAdvOpCmd.getParam();
                if (param != null && param.getOp() == Constants.ADV_REQUEST_OP_SYNC_TIME) {
                    updateDevTime();
                }
            }
        }

        @Override
        public void onMusicNameChange(MusicNameInfo nameInfo) {
            if (getDeviceInfo() != null) {
                getDeviceInfo().setMusicNameInfo(nameInfo);
            }
        }

        @Override
        public void onMusicStatusChange(MusicStatusInfo statusInfo) {
            if (getDeviceInfo() != null) {
                getDeviceInfo().setMusicStatusInfo(statusInfo);
                getDeviceInfo().setCurrentDevIndex((byte) statusInfo.getCurrentDev());
            }
        }

        @Override
        public void onDeviceModeChange(int mode) {
            if (getDeviceInfo() != null) {
                getDeviceInfo().setCurFunction(CHexConver.intToByte(mode));
            }
        }

        @Override
        public void onAuxStatusChange(boolean isPlay) {
            if (getDeviceInfo() != null) {
                getDeviceInfo().setAuxPlay(isPlay);
            }
        }

        @Override
        public void onBatteryChange(BatteryInfo batteryInfo) {
            if (getDeviceInfo() != null && batteryInfo != null) {
                getDeviceInfo().setQuantity(batteryInfo.getBattery());
            }
        }

        @Override
        public void onVolumeChange(VolumeInfo volume) {
            if (getDeviceInfo() != null && volume != null) {
                getDeviceInfo().setVolume(volume.getVolume());
            }
        }

        @Override
        public void onEqChange(EqInfo eqInfo) {
            super.onEqChange(eqInfo);
            /*EqCacheUtil.saveEqValue(eqInfo);
            if (eqInfo.getMode() == 6 && getDeviceInfo() != null && getDeviceInfo().getEqPresetInfo() != null) {
                //更新自定义eq预设值
                EqPresetInfo eqPresetInfo = getDeviceInfo().getEqPresetInfo();
                eqPresetInfo.getEqInfos().remove(6);
                eqPresetInfo.getEqInfos().add(eqInfo);
                EqCacheUtil.savePresetEqInfo(eqPresetInfo);
            }*/

            if (getDeviceInfo() != null) {
                getDeviceInfo().setEqInfo(eqInfo);
            }
        }

        @Override
        public void onEqPresetChange(EqPresetInfo eqPresetInfo) {
            super.onEqPresetChange(eqPresetInfo);
//            EqCacheUtil.savePresetEqInfo(eqPresetInfo);
            if (getDeviceInfo() != null) {
                getDeviceInfo().setEqPresetInfo(eqPresetInfo);
            }
        }

        @Override
        public void onFileFormatChange(FileFormatInfo fileFormatInfo) {
            if (getDeviceInfo() != null && fileFormatInfo != null) {
                getDeviceInfo().setPlayFileFormat(fileFormatInfo.getFormat());
            }
        }

        @Override
        public void onLightControlInfo(LightControlInfo lightControlInfo) {
            super.onLightControlInfo(lightControlInfo);
            if (getDeviceInfo() != null) {
                getDeviceInfo().setLightControlInfo(lightControlInfo);
            }
        }

        @Override
        public void onAlarmDefaultBellListChange(List<DefaultAlarmBell> bells) {
            super.onAlarmDefaultBellListChange(bells);
            if (getDeviceInfo() != null) {
                getDeviceInfo().setAlarmDefaultBells(bells);
            }
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
        }
    };

}
