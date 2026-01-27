package com.jieli.btsmart.tool.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.VoiceData;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.interfaces.bluetooth.IBluetoothEventListener;
import com.jieli.btsmart.data.model.bluetooth.AlarmBean;
import com.jieli.btsmart.data.model.bluetooth.AlarmListInfo;
import com.jieli.btsmart.data.model.bluetooth.BatteryInfo;
import com.jieli.btsmart.data.model.bluetooth.ChannelInfo;
import com.jieli.btsmart.data.model.bluetooth.DefaultAlarmBell;
import com.jieli.btsmart.data.model.bluetooth.DevStorageInfo;
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

import java.util.List;

/**
 * 蓝牙事件回调抽象类
 *
 * @author zqjasonZhong
 * @version V1.0
 * @since 2020/5/13
 */
@SuppressWarnings("EmptyMethod")
public abstract class BTEventCallback implements IBluetoothEventListener {

    @Override
    public void onAdapterStatus(boolean bEnabled, boolean bHasBle) {

    }

    @Override
    public void onDiscoveryStatus(boolean bBle, boolean bStart) {

    }

    @Override
    public void onDiscovery(BluetoothDevice device) {

    }

    @Override
    public void onDiscovery(BluetoothDevice device, BleScanMessage bleScanMessage) {

    }

    @Override
    public void onShowDialog(BluetoothDevice device, BleScanMessage bleScanMessage) {

    }

    @Override
    public void onBondStatus(BluetoothDevice device, int status) {

    }

    @Override
    public void onConnection(BluetoothDevice device, int status) {

    }

    @Override
    public void onSwitchConnectedDevice(BluetoothDevice device) {

    }

    @Override
    public void onA2dpStatus(BluetoothDevice device, int status) {

    }

    @Override
    public void onHfpStatus(BluetoothDevice device, int status) {

    }

    @Override
    public void onSppStatus(BluetoothDevice device, int status) {

    }

    @Override
    public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {

    }

    @Override
    public void onDeviceData(BluetoothDevice device, byte[] data) {

    }

    @Override
    public void onDeviceVoiceData(BluetoothDevice device, byte[] data) {

    }

    @Override
    public void onDeviceVoiceData(BluetoothDevice device, VoiceData data) {

    }

    @Override
    public void onDeviceVadEnd(BluetoothDevice device) {

    }

    @Override
    public void onError(BaseError error) {

    }

    @Override
    public void onDeviceResponse(BluetoothDevice device, CommandBase response) {

    }

    /* =====================================================================================
     * 后续添加回调
     * ===================================================================================== */

    /**
     * 设备模式改变回调
     *
     * @param mode 设备当前模式
     */
    public void onDeviceModeChange(int mode) {

    }

    /**
     * 设备系统音量改变回调
     *
     * @param volume 设备系统音量
     */
    public void onVolumeChange(VolumeInfo volume) {

    }

    /**
     * 设备EQ信息改变回调
     *
     * @param eqInfo EQ信息
     */
    public void onEqChange(EqInfo eqInfo) {

    }

    /**
     * 设备外接存储器改变回调
     *
     * @param storageInfo 设备信息
     */
    public void onDevStorageInfoChange(DevStorageInfo storageInfo) {

    }

    /**
     * 设备支持文件格式回调
     *
     * @param fileFormatInfo 设备支持文件格式
     */
    public void onFileFormatChange(FileFormatInfo fileFormatInfo) {

    }

    /**
     * 音乐歌曲改变回调
     *
     * @param nameInfo 音乐信息
     */
    public void onMusicNameChange(MusicNameInfo nameInfo) {

    }

    /**
     * 音乐播放状态回调
     *
     * @param statusInfo 音乐状态
     */
    public void onMusicStatusChange(MusicStatusInfo statusInfo) {

    }

    /**
     * 播放模式改变回调
     *
     * @param playModeInfo 播放模式信息
     */
    public void onPlayModeChange(PlayModeInfo playModeInfo) {

    }

    /**
     * 设备电量改变回调
     *
     * @param batteryInfo 电量信息
     */
    public void onBatteryChange(BatteryInfo batteryInfo) {

    }

    /**
     * 外接设备的状态回调
     *
     * @param isPlay 是否播放
     */
    public void onAuxStatusChange(boolean isPlay) {

    }

    /**
     * 强制升级回调
     */
    @Deprecated
    public void onMandatoryUpgrade() {

    }

    /**
     * 强制升级回调
     *
     * @param device 需要强制升级的设备
     */
    public void onMandatoryUpgrade(BluetoothDevice device){

    }

    /**
     * FM频道改变回调
     *
     * @param channels 频道信息列表
     */
    public void onFmChannelsChange(List<ChannelInfo> channels) {

    }

    /**
     * FM状态信息改变回调
     *
     * @param fmStatusInfo FM状态信息
     */
    public void onFmStatusChange(FmStatusInfo fmStatusInfo) {

    }

    /**
     * 闹钟列表更新回调
     *
     * @param alarmListInfo 闹钟列表
     */
    public void onAlarmListChange(AlarmListInfo alarmListInfo) {

    }
    /**
     * 默认闹钟铃声列表
     *
     * @param bells 默认闹钟铃声列表
     */

    public void onAlarmDefaultBellListChange(List<DefaultAlarmBell> bells) {

    }


    /**
     * 当前闹钟信息回调
     *
     * @param alarmListInfo 闹钟信息
     */
    @Deprecated
    public void onAlarmNotify(AlarmListInfo alarmListInfo) {

    }

    /**
     * 当前闹钟信息回调
     *
     * @param alarmBean 闹钟信息
     */
    public void onAlarmNotify(AlarmBean alarmBean) {

    }

    /**
     * 闹钟停止回调
     */
    public void onAlarmStop() {

    }

    /**
     * 频道发射回调
     *
     * @param frequency 频点
     */
    public void onFrequencyTx(float frequency) {

    }

    /**
     * 外设模式改变回调
     *
     * @param mode 外设模式
     */
    public void onPeripheralsModeChange(int mode) {

    }

    /**
     * 外设连接状态改变回调
     *
     * @param connect 是否连接
     * @param mac     设备mac地址
     */
    public void onPeripheralsConnectStatusChange(boolean connect, String mac) {

    }

    /**
     * 删除连接记录成功回调
     *
     * @param device 连接记录
     */
    public void onRemoveHistoryDeviceSuccess(HistoryBluetoothDevice device) {

    }

    /**
     * 删除连接记录失败回调
     *
     * @param error 失败原因
     */
    public void onRemoveHistoryDeviceFailed(BaseError error) {

    }

    /**
     * ID3歌曲info回调
     *
     * @param id3MusicInfo id3歌曲信息
     */
    public void onID3MusicInfo(ID3MusicInfo id3MusicInfo) {

    }

    /**
     * 高低音变化回调
     *
     * @param high 高音
     * @param bass 低音
     */
    public void onHighAndBassChange(int high, int bass) {
    }

    /**
     * Eq预设值变化
     *
     * @param eqPresetInfo 预设值
     */
    public void onEqPresetChange(EqPresetInfo eqPresetInfo) {

    }

    public void onPhoneCallStatusChange(int status) {

    }

    public void onFixedLenData(int type, byte[] data) {

    }

    public void onLightControlInfo(LightControlInfo lightControlInfo) {

    }

    /**
     * Tws连接状态回调
     *
     * @param device 蓝牙设备
     * @param isTwsConnected 是否连接TWS
     */
    public void onTwsStatusChange(BluetoothDevice device, boolean isTwsConnected){

    }

    public void onSoundCardEqChange(EqInfo eqInfo) {
    }


    public void onSoundCardStatusChange(long mask, byte[] values) {
    }

    /**
     * 外挂Flash系统异常回调
     *
     * @param device 设备对象
     */
    public void onExternalFlashSysException(BluetoothDevice device, int state){

    }

    /**
     * 当前噪声处理模式信息
     *
     * @param device 使用设备
     * @param voiceMode 噪声处理模式
     */
    public void onCurrentVoiceMode(BluetoothDevice device, VoiceMode voiceMode){

    }

    /**
     * 设备所有噪声处理模式信息
     *
     * @param device 使用设备
     * @param voiceModes 噪声处理模式信息列表
     */
    public void onVoiceModeList(BluetoothDevice device, List<VoiceMode> voiceModes){

    }

}
