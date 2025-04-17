package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.jieli.bluetooth.bean.base.AttrBean;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.base.CommonResponse;
import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.command.GetSysInfoCmd;
import com.jieli.bluetooth.bean.command.GetTargetInfoCmd;
import com.jieli.bluetooth.bean.command.UpdateSysInfoCmd;
import com.jieli.bluetooth.bean.command.data.DataCmd;
import com.jieli.bluetooth.bean.command.data.DataHasResponseCmd;
import com.jieli.bluetooth.bean.command.tws.RequestAdvOpCmd;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.alarm.AlarmBean;
import com.jieli.bluetooth.bean.device.alarm.AlarmListInfo;
import com.jieli.bluetooth.bean.device.alarm.DefaultAlarmBell;
import com.jieli.bluetooth.bean.device.double_connect.ConnectedBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DoubleConnectionState;
import com.jieli.bluetooth.bean.device.eq.DynamicLimiterParam;
import com.jieli.bluetooth.bean.device.eq.EqInfo;
import com.jieli.bluetooth.bean.device.eq.EqPresetInfo;
import com.jieli.bluetooth.bean.device.eq.ReverberationParam;
import com.jieli.bluetooth.bean.device.fm.ChannelInfo;
import com.jieli.bluetooth.bean.device.fm.FmStatusInfo;
import com.jieli.bluetooth.bean.device.hearing.HearingAssistInfo;
import com.jieli.bluetooth.bean.device.hearing.HearingChannelsStatus;
import com.jieli.bluetooth.bean.device.light.LightControlInfo;
import com.jieli.bluetooth.bean.device.music.ID3MusicInfo;
import com.jieli.bluetooth.bean.device.music.MusicNameInfo;
import com.jieli.bluetooth.bean.device.music.MusicStatusInfo;
import com.jieli.bluetooth.bean.device.music.PlayModeInfo;
import com.jieli.bluetooth.bean.device.status.BatteryInfo;
import com.jieli.bluetooth.bean.device.status.DevStorageInfo;
import com.jieli.bluetooth.bean.device.voice.VoiceFunc;
import com.jieli.bluetooth.bean.device.voice.VolumeInfo;
import com.jieli.bluetooth.bean.parameter.DataParam;
import com.jieli.bluetooth.bean.parameter.RequestAdvOpParam;
import com.jieli.bluetooth.bean.parameter.SearchDevParam;
import com.jieli.bluetooth.bean.parameter.UpdateSysInfoParam;
import com.jieli.bluetooth.bean.response.SysInfoResponse;
import com.jieli.bluetooth.bean.response.TargetInfoResponse;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.RcspCommandCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.CommandBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 蓝牙控制库测试
 * @since 2021/12/2
 */
public class BtRcspControlDemo {


    public void getDeviceInfo() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行请求设备功能并等待结果回调
        controller.requestDeviceInfo(controller.getUsingDevice(), 0xffffffff, new OnRcspActionCallback<DeviceInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, DeviceInfo message) {
                //成功回调
                //message - 设备信息
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
        //第二种方式，获取缓存的设备信息
        DeviceInfo deviceInfo = controller.getDeviceInfo();
    }


    public void getDeviceInfoV0(Context context, int mask) {
        //获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //构造功能命令
        //mask = 0xffffffff;  -- 获取所有属性
        CommandBase getDeviceInfoCmd = CommandBuilder.buildGetDeviceInfoCmd(mask);
        //执行获取设备信息功能并等待结果回调
        manager.sendCommandAsync(manager.getConnectedDevice(), getDeviceInfoCmd, manager.getBluetoothOption().getTimeoutMs(), new RcspCommandCallback() {
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                //检查设备状态
                if (cmd.getStatus() != StateCode.STATUS_SUCCESS) { //设备状态异常，进行异常处理
                    onErrCode(device, new BaseError(ErrorCode.SUB_ERR_RESPONSE_BAD_STATUS, "Device reply an bad status: " + cmd.getStatus()));
                    return;
                }
                //成功回调
                //获取对应的命令数据
                GetTargetInfoCmd command = (GetTargetInfoCmd) cmd;
                //获取回复数据
                //注意 - 如果是没有回复数据的命令，回复数据为null
                boolean isHasResponse = command.getType() == CommandBase.FLAG_HAVE_PARAMETER_AND_RESPONSE
                        || command.getType() == CommandBase.FLAG_NO_PARAMETER_AND_RESPONSE;
                if (isHasResponse) {
                    TargetInfoResponse response = command.getResponse();
                    if (null == response) {
                        onErrCode(device, new BaseError(ErrorCode.SUB_ERR_DATA_FORMAT, "Response data is error."));
                        return;
                    }
                    //处理设备回复数据
                }
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void getSysInfo() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {

            @Override
            public void onDevStorageInfoChange(BluetoothDevice device, DevStorageInfo storageInfo) {
                //此处将会回调设备存储信息
            }

            @Override
            public void onDeviceModeChange(BluetoothDevice device, int mode) {
                //此处将会回调设备模式
            }
        });
        int func = AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC; //公共属性
        //查询当前设备模式和设备存储信息
        int mask = 0x01 << AttrAndFunCode.SYS_INFO_ATTR_CUR_MODE_TYPE | 0x01 << AttrAndFunCode.SYS_INFO_ATTR_MUSIC_DEV_STATUS;
        //执行查询设备系统信息并等待结果回调
        controller.getDevSysInfo(controller.getUsingDevice(), func, mask, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onDeviceModeChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    public void getSysInfoV0(Context context) {
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 构造查询设备系统信息命令
        //功能码
        int function = AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC; //公共属性
        //查询当前设备模式和设备存储信息
        int mask = 0x01 << AttrAndFunCode.SYS_INFO_ATTR_CUR_MODE_TYPE | 0x01 << AttrAndFunCode.SYS_INFO_ATTR_MUSIC_DEV_STATUS;
        CommandBase getSysInfoCmd = CommandBuilder.buildGetSysInfoCmd((byte) function, mask);
        //Step2: 执行操作命令并等待结果回调
        manager.sendCommandAsync(manager.getConnectedDevice(), getSysInfoCmd, manager.getBluetoothOption().getTimeoutMs(), new RcspCommandCallback() {
            //回调回复命令
            //注意: 无回复命时，回复命令本身
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                //Step3: 检查设备状态
                if (cmd.getStatus() != StateCode.STATUS_SUCCESS) { //设备状态异常，进行异常处理
                    onErrCode(device, new BaseError(ErrorCode.SUB_ERR_RESPONSE_BAD_STATUS, "Device reply an bad status: " + cmd.getStatus()));
                    return;
                }
                //成功回调
                //Step4: 判断是否有回复数据
                //注意 - 如果是没有回复数据的命令，回复数据为null
                boolean isHasResponse = cmd.getType() == CommandBase.FLAG_HAVE_PARAMETER_AND_RESPONSE
                        || cmd.getType() == CommandBase.FLAG_NO_PARAMETER_AND_RESPONSE;
                if (isHasResponse) {
                    //Step5: 获取对应的命令数据
                    GetSysInfoCmd command = (GetSysInfoCmd) cmd;
                    //Step6: 获取回复数据
                    SysInfoResponse response = command.getResponse();
                    if (null == response) { //回复数据为空，证明设备回复的数据有问题，进行异常处理
                        onErrCode(device, new BaseError(ErrorCode.SUB_ERR_DATA_FORMAT, "Response data is error."));
                        return;
                    }
                    //处理设备回复数据
                    //response.getFunction(); //功能码
                    //response.getAttrs();    //功能属性
                }
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void getDeviceModeInfo() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onDeviceModeChange(BluetoothDevice device, int mode) {
                //回调设备模式
            }
        });
        //执行获取设备模式信息并等待结果回调
        controller.getCurrentDevModeInfo(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onDeviceModeChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    public void getDeviceModeInfoV0(Context context) {
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 构造查询设备系统信息命令
        CommandBase getCurrentModeCmd = CommandBuilder.buildGetSysInfoCmd(AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC,
                0x01 << AttrAndFunCode.SYS_INFO_ATTR_CUR_MODE_TYPE);
        //Step2: 执行操作命令并等待结果回调
        manager.sendCommandAsync(manager.getConnectedDevice(), getCurrentModeCmd, manager.getBluetoothOption().getTimeoutMs(), new RcspCommandCallback() {
            //回调回复命令
            //注意: 无回复命时，回复命令本身
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                //Step3: 检查设备状态
                if (cmd.getStatus() != StateCode.STATUS_SUCCESS) { //设备状态异常，进行异常处理
                    onErrCode(device, new BaseError(ErrorCode.SUB_ERR_RESPONSE_BAD_STATUS, "Device reply an bad status: " + cmd.getStatus()));
                    return;
                }
                //成功回调
                //Step4: 判断是否有回复数据
                //注意 - 如果是没有回复数据的命令，回复数据为null
                boolean isHasResponse = cmd.getType() == CommandBase.FLAG_HAVE_PARAMETER_AND_RESPONSE
                        || cmd.getType() == CommandBase.FLAG_NO_PARAMETER_AND_RESPONSE;
                if (isHasResponse) {
                    //Step5: 获取对应的命令数据
                    GetSysInfoCmd command = (GetSysInfoCmd) cmd;
                    //Step6: 获取回复数据
                    SysInfoResponse response = command.getResponse();
                    if (null == response) { //回复数据为空，证明设备回复的数据有问题，进行异常处理
                        onErrCode(device, new BaseError(ErrorCode.SUB_ERR_DATA_FORMAT, "Response data is error."));
                        return;
                    }
                    //处理设备回复数据
                    //response.getFunction(); //功能码
                    //response.getAttrs();    //功能属性
                }
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void getDevStorageInfo() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onDevStorageInfoChange(BluetoothDevice device, DevStorageInfo storageInfo) {
                //回调设备存储信息
            }
        });
        //执行获取设备存储信息并等待结果回调
        controller.getDevStorageInfo(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onDevStorageInfoChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void setSysInfo() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        int fun = AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC; //公共属性
        //设置设备音量 - 14
        AttrBean attrBean = new AttrBean();
        attrBean.setType(AttrAndFunCode.SYS_INFO_ATTR_VOLUME);
        attrBean.setAttrData(new byte[]{(byte) 0x0E});
        List<AttrBean> data = new ArrayList<>();
        data.add(attrBean);
        //执行设置设备系统属性并等待结果回调
        controller.setDevSysInfo(controller.getUsingDevice(), fun, data, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void setSysInfoV0(Context context) {
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 构造设置设备系统属性命令
        //功能码
        int function = AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC; //公共属性
        //数据列表
        AttrBean attrBean = new AttrBean();
        attrBean.setType(AttrAndFunCode.SYS_INFO_ATTR_VOLUME);  //系统音量
        attrBean.setAttrData(new byte[]{(byte) 0x0E}); //14等级
        List<AttrBean> data = new ArrayList<>();
        data.add(attrBean);
        CommandBase setSysInfoCmd = CommandBuilder.buildSetSysInfoCmd((byte) function, data);
        //Step2: 执行操作命令并等待结果回调
        manager.sendCommandAsync(manager.getConnectedDevice(), setSysInfoCmd, manager.getBluetoothOption().getTimeoutMs(), new RcspCommandCallback() {
            //回调回复命令
            //注意: 无回复命时，回复命令本身
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                //Step3: 检查设备状态
                if (cmd.getStatus() != StateCode.STATUS_SUCCESS) { //设备状态异常，进行异常处理
                    onErrCode(device, new BaseError(ErrorCode.SUB_ERR_RESPONSE_BAD_STATUS, "Device reply an bad status: " + cmd.getStatus()));
                    return;
                }
                //成功回调
                //等待系统调整成功后，通过更新设备系统信息命令通知
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void receiveSysInfo() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //定义蓝牙RCSP事件监听器
        final BTRcspEventCallback callback = new BTRcspEventCallback() {
            @Override
            public void onDeviceModeChange(BluetoothDevice device, int mode) {
                //回调设备模式
            }

            @Override
            public void onVolumeChange(BluetoothDevice device, VolumeInfo volume) {
                //回调设备音量信息
            }

            @Override
            public void onEqChange(BluetoothDevice device, EqInfo eqInfo) {
                //回调均衡器调节参数
            }

            @Override
            public void onDevStorageInfoChange(BluetoothDevice device, DevStorageInfo storageInfo) {
                //回调设备存储信息
            }

            @Override
            public void onFileFormatChange(BluetoothDevice device, String fileFormat) {
                //回调支持的播放格式
            }

            @Override
            public void onMusicNameChange(BluetoothDevice device, MusicNameInfo nameInfo) {
                //回调音乐名信息
            }

            @Override
            public void onMusicStatusChange(BluetoothDevice device, MusicStatusInfo statusInfo) {
                //回调音乐状态信息
            }

            @Override
            public void onPlayModeChange(BluetoothDevice device, PlayModeInfo playModeInfo) {
                //回调播放模式信息
            }

            @Override
            public void onAuxStatusChange(BluetoothDevice device, boolean isPlay) {
                //回调外接设备播放状态
            }

            @Override
            public void onFmChannelsChange(BluetoothDevice device, List<ChannelInfo> channels) {
                //回调FM频道列表信息
            }

            @Override
            public void onFmStatusChange(BluetoothDevice device, FmStatusInfo fmStatusInfo) {
                //回调FM状态信息
            }

            @Override
            public void onAlarmListChange(BluetoothDevice device, AlarmListInfo alarmListInfo) {
                //回调闹钟列表信息
            }

            @Override
            public void onAlarmDefaultBellListChange(BluetoothDevice device, List<DefaultAlarmBell> bells) {
                //回调闹钟默认铃声列表
            }

            @Override
            public void onAlarmNotify(BluetoothDevice device, AlarmBean alarmBean) {
                //回调闹钟提醒
            }

            @Override
            public void onAlarmStop(BluetoothDevice device, AlarmBean alarmBean) {
                //回调闹钟停止
            }

            @Override
            public void onID3MusicInfo(BluetoothDevice device, ID3MusicInfo id3MusicInfo) {
                //回调ID3音乐信息
            }

            @Override
            public void onHighAndBassChange(BluetoothDevice device, int high, int bass) {
                //回调高低音信息
            }

            @Override
            public void onEqPresetChange(BluetoothDevice device, EqPresetInfo eqPresetInfo) {
                //回调均衡器预设值信息
            }

            @Override
            public void onPhoneCallStatusChange(BluetoothDevice device, int status) {
                //回调电话状态
            }

            @Override
            public void onExpandFunction(BluetoothDevice device, int type, byte[] data) {
                //回调拓展功能信息
            }

            @Override
            public void onLightControlInfo(BluetoothDevice device, LightControlInfo lightControlInfo) {
                //回调灯光控制信息
            }

            @Override
            public void onSoundCardEqChange(BluetoothDevice device, EqInfo eqInfo) {
                //回调声卡信息
            }

            @Override
            public void onSoundCardStatusChange(BluetoothDevice device, long mask, byte[] values) {
                //回调声卡的状态信息
            }

            @Override
            public void onCurrentVoiceMode(BluetoothDevice device, VoiceMode voiceMode) {
                //回调当前噪声处理模式信息
            }

            @Override
            public void onVoiceModeList(BluetoothDevice device, List<VoiceMode> voiceModes) {
                //回调噪声处理列表信息
            }

            @Override
            public void onSearchDevice(BluetoothDevice device, SearchDevParam searchDevParam) {
                //回调查找设备参数
            }

            @Override
            public void onReverberation(BluetoothDevice device, ReverberationParam param) {
                //回调混响参数
            }

            @Override
            public void onDynamicLimiter(BluetoothDevice device, DynamicLimiterParam param) {
                //回调动态限幅参数
            }

            @Override
            public void onBatteryChange(BluetoothDevice device, BatteryInfo batteryInfo) {
                //回调电量信息
            }

            @Override
            public void onHearingAssistInfo(BluetoothDevice device, HearingAssistInfo hearingAssistInfo) {
                //回调辅听信息
            }

            @Override
            public void onHearingChannelsStatus(BluetoothDevice device, HearingChannelsStatus hearingChannelsStatus) {
                //回调辅听通道状态
            }

            @Override
            public void onVoiceFunctionChange(BluetoothDevice device, VoiceFunc voiceFunc) {
                //回调音效功能改变
            }

            @Override
            public void onDoubleConnectionChange(BluetoothDevice device, DoubleConnectionState state) {
                //回调设备双连状态
            }

            @Override
            public void onConnectedBtInfo(BluetoothDevice device, ConnectedBtInfo info) {
                //回调已连接设备的设备信息
            }
        };
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(callback);
        //移除蓝牙RCSP事件监听器
        //TODO: 注意每次添加事件监听器，不需要使用后，都需要移除监听器，避免多次回调数据。
//        controller.removeBTRcspEventCallback(callback);
    }


    public void receiveSysInfoV0(Context context) {
        //获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //定义蓝牙RCSP事件监听器
        final BTRcspEventCallback callback = new BTRcspEventCallback() {
            @Override
            public void onDeviceModeChange(BluetoothDevice device, int mode) {
                //回调设备模式
            }

            @Override
            public void onVolumeChange(BluetoothDevice device, VolumeInfo volume) {
                //回调设备音量信息
            }

            @Override
            public void onEqChange(BluetoothDevice device, EqInfo eqInfo) {
                //回调均衡器调节参数
            }

            @Override
            public void onDevStorageInfoChange(BluetoothDevice device, DevStorageInfo storageInfo) {
                //回调设备存储信息
            }

            @Override
            public void onFileFormatChange(BluetoothDevice device, String fileFormat) {
                //回调支持的播放格式
            }

            @Override
            public void onMusicNameChange(BluetoothDevice device, MusicNameInfo nameInfo) {
                //回调音乐名信息
            }

            @Override
            public void onMusicStatusChange(BluetoothDevice device, MusicStatusInfo statusInfo) {
                //回调音乐状态信息
            }

            @Override
            public void onPlayModeChange(BluetoothDevice device, PlayModeInfo playModeInfo) {
                //回调播放模式信息
            }

            @Override
            public void onAuxStatusChange(BluetoothDevice device, boolean isPlay) {
                //回调外接设备播放状态
            }

            @Override
            public void onFmChannelsChange(BluetoothDevice device, List<ChannelInfo> channels) {
                //回调FM频道列表信息
            }

            @Override
            public void onFmStatusChange(BluetoothDevice device, FmStatusInfo fmStatusInfo) {
                //回调FM状态信息
            }

            @Override
            public void onAlarmListChange(BluetoothDevice device, AlarmListInfo alarmListInfo) {
                //回调闹钟列表信息
            }

            @Override
            public void onAlarmDefaultBellListChange(BluetoothDevice device, List<DefaultAlarmBell> bells) {
                //回调闹钟默认铃声列表
            }

            @Override
            public void onAlarmNotify(BluetoothDevice device, AlarmBean alarmBean) {
                //回调闹钟提醒
            }

            @Override
            public void onAlarmStop(BluetoothDevice device, AlarmBean alarmBean) {
                //回调闹钟停止
            }

            @Override
            public void onID3MusicInfo(BluetoothDevice device, ID3MusicInfo id3MusicInfo) {
                //回调ID3音乐信息
            }

            @Override
            public void onHighAndBassChange(BluetoothDevice device, int high, int bass) {
                //回调高低音信息
            }

            @Override
            public void onEqPresetChange(BluetoothDevice device, EqPresetInfo eqPresetInfo) {
                //回调均衡器预设值信息
            }

            @Override
            public void onPhoneCallStatusChange(BluetoothDevice device, int status) {
                //回调电话状态
            }

            @Override
            public void onExpandFunction(BluetoothDevice device, int type, byte[] data) {
                //回调拓展功能信息
            }

            @Override
            public void onLightControlInfo(BluetoothDevice device, LightControlInfo lightControlInfo) {
                //回调灯光控制信息
            }

            @Override
            public void onSoundCardEqChange(BluetoothDevice device, EqInfo eqInfo) {
                //回调声卡信息
            }

            @Override
            public void onSoundCardStatusChange(BluetoothDevice device, long mask, byte[] values) {
                //回调声卡的状态信息
            }

            @Override
            public void onCurrentVoiceMode(BluetoothDevice device, VoiceMode voiceMode) {
                //回调当前噪声处理模式信息
            }

            @Override
            public void onVoiceModeList(BluetoothDevice device, List<VoiceMode> voiceModes) {
                //回调噪声处理列表信息
            }

            @Override
            public void onSearchDevice(BluetoothDevice device, SearchDevParam searchDevParam) {
                //回调查找设备参数
            }

            @Override
            public void onReverberation(BluetoothDevice device, ReverberationParam param) {
                //回调混响参数
            }

            @Override
            public void onDynamicLimiter(BluetoothDevice device, DynamicLimiterParam param) {
                //回调动态限幅参数
            }

            @Override
            public void onBatteryChange(BluetoothDevice device, BatteryInfo batteryInfo) {
                //回调电量信息
            }

            @Override
            public void onHearingAssistInfo(BluetoothDevice device, HearingAssistInfo hearingAssistInfo) {
                //回调辅听信息
            }

            @Override
            public void onHearingChannelsStatus(BluetoothDevice device, HearingChannelsStatus hearingChannelsStatus) {
                //回调辅听通道状态
            }

            @Override
            public void onVoiceFunctionChange(BluetoothDevice device, VoiceFunc voiceFunc) {
                //回调音效功能改变
            }

            @Override
            public void onDoubleConnectionChange(BluetoothDevice device, DoubleConnectionState state) {
                //回调设备双连状态
            }

            @Override
            public void onConnectedBtInfo(BluetoothDevice device, ConnectedBtInfo info) {
                //回调已连接设备的设备信息
            }
        };
        //添加蓝牙RCSP事件监听器
        manager.addEventListener(callback);
        //移除蓝牙RCSP事件监听器
        //TODO: 注意每次添加事件监听器，不需要使用后，都需要移除监听器，避免多次回调数据。
//        manager.removeEventListener(callback);
    }


    public void switchDeviceMode(byte mode) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onDeviceModeChange(BluetoothDevice device, int mode) {
                //回调设备模式
            }
        });
        //byte SYS_INFO_FUNCTION_BT :  0 //Bluetooth mode
        //byte SYS_INFO_FUNCTION_MUSIC : 1 //Music mode
        //byte SYS_INFO_FUNCTION_RTC : 2   //clock mode
        //byte SYS_INFO_FUNCTION_AUX : 3   //Linein mode
        //byte SYS_INFO_FUNCTION_FM  : 4   //FM mode
        //byte SYS_INFO_FUNCTION_LIGHT : 5 //Light mode
        //执行切换设备模式功能并等待结果回调
        controller.switchDeviceMode(controller.getUsingDevice(), mode, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onDeviceModeChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void switchDeviceModeV0(Context context) {
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 构建命令
        byte mode = AttrAndFunCode.SYS_INFO_FUNCTION_BT;   //-- 蓝牙模式
//                    AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC -- 设备音乐模式
//                    AttrAndFunCode.SYS_INFO_FUNCTION_RTC   -- 时钟模式(闹钟，秒表)
//                    AttrAndFunCode.SYS_INFO_FUNCTION_AUX   -- 外部输入控制模式
//                    AttrAndFunCode.SYS_INFO_FUNCTION_FM    -- FM控制模式
//                    AttrAndFunCode.SYS_INFO_FUNCTION_LIGHT -- 灯光模式
        CommandBase switchDevModeCmd = CommandBuilder.buildSwitchModeCmd(mode);
        //Step2: 执行操作命令并等待结果回调
        manager.sendCommandAsync(manager.getConnectedDevice(), switchDevModeCmd, manager.getBluetoothOption().getTimeoutMs(), new RcspCommandCallback() {
            //回调回复命令
            //注意: 无回复命时，回复命令本身
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                //Step3: 检查设备状态
                if (cmd.getStatus() != StateCode.STATUS_SUCCESS) { //设备状态异常，进行异常处理
                    onErrCode(device, new BaseError(ErrorCode.SUB_ERR_RESPONSE_BAD_STATUS, "Device reply an bad status: " + cmd.getStatus()));
                    return;
                }
                //成功回调
                //等待系统调整成功后，通过更新设备系统信息命令通知
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void rebootDevice() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行重启设备功能并等待结果回调
        controller.rebootDevice(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void rebootDeviceV0(Context context) {
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 构建重启命令
        CommandBase rebootCmd = CommandBuilder.buildRebootCmd();
        //Step2: 发送重启命令并等待结果回调
        manager.sendCommandAsync(manager.getConnectedDevice(), rebootCmd, manager.getBluetoothOption().getTimeoutMs(), new RcspCommandCallback() {
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                //Step3: 检查设备状态
                if (cmd.getStatus() != StateCode.STATUS_SUCCESS) { //设备状态异常，进行异常处理
                    onErrCode(device, new BaseError(ErrorCode.SUB_ERR_RESPONSE_BAD_STATUS, "Device reply an bad status: " + cmd.getStatus()));
                    return;
                }
                //成功回调
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void receiveCommandAndResponse(Context context) {
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 添加蓝牙RCSP事件回调器
        manager.addEventListener(new BTRcspEventCallback() {
            //回调设备发送的命令
            @Override
            public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
                super.onDeviceCommand(device, cmd);
                //Step2: 处理命令
                if (cmd.getId() != Command.CMD_ADV_DEV_REQUEST_OPERATION) {
                    //过滤命令
                    return;
                }
                //Step3: 判断是否需要回复命令
                //需要回复的命令，处理完后需要回复结果
                //不需要回复的命令，处理完就完成了
                boolean isHasResponse = cmd.getType() == CommandBase.FLAG_HAVE_PARAMETER_AND_RESPONSE
                        || cmd.getType() == CommandBase.FLAG_NO_PARAMETER_AND_RESPONSE;
                //只处理目标命令 - 请求操作
                RequestAdvOpCmd requestAdvOpCmd = (RequestAdvOpCmd) cmd;
                //Step4: 获取参数
                RequestAdvOpParam param = requestAdvOpCmd.getParam();
                if (null == param) return; //异常处理
                //Step5: 处理数据
                switch (param.getOp()) {
                    case Constants.ADV_REQUEST_OP_UPDATE_CONFIGURE: //更新配置
                        break;
                    case Constants.ADV_REQUEST_OP_SYNC_TIME: //同步时间
                        break;
                }
                //Step6. 回复结果
                if (isHasResponse) {
                    //组装结果
                    requestAdvOpCmd.setStatus(StateCode.STATUS_SUCCESS);
                    //组装参数
                    requestAdvOpCmd.setParam(null);
                    //发送回复命令
                    manager.sendCommandResponse(device, requestAdvOpCmd);
                }
            }
        });
    }


    public void sendDataHandle(Context context, int responseOpId, byte[] data) {
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 构造功能命令 - 比如, 构建命令包
        DataParam param = new DataParam(data);   //设置数据
        param.setXmOpCode(responseOpId);         //设置响应命令号
        CommandBase dataCmd = new DataCmd(param);
        //默认数据包没有回复, 如果需要数据包有回复
//        dataCmd = new DataHasResponseCmd(param);
        //Step2: 执行操作命令并等待结果回调
        manager.sendCommandAsync(manager.getConnectedDevice(), dataCmd, manager.getBluetoothOption().getTimeoutMs(), new RcspCommandCallback() {
            //回调回复命令
            //注意: 无回复命时，回复命令本身
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                //Step3: 检查设备状态
                if (cmd.getStatus() != StateCode.STATUS_SUCCESS) { //设备状态异常，进行异常处理
                    onErrCode(device, new BaseError(ErrorCode.SUB_ERR_RESPONSE_BAD_STATUS, "Device reply an bad status: " + cmd.getStatus()));
                    return;
                }
                //成功回调
                //Step4: 判断是否有回复数据
                //注意 - 如果是没有回复数据的命令，回复数据为null
                boolean isHasResponse = cmd.getType() == CommandBase.FLAG_HAVE_PARAMETER_AND_RESPONSE
                        || cmd.getType() == CommandBase.FLAG_NO_PARAMETER_AND_RESPONSE;
                if (isHasResponse) {
                    //Step5: 获取对应的命令数据
                    DataHasResponseCmd dataCmd = (DataHasResponseCmd) cmd;
                    //Step6: 获取回复数据
                    CommonResponse response = dataCmd.getResponse();
                    if (null == response) { //回复数据为空，证明设备回复的数据有问题，进行异常处理
                        onErrCode(device, new BaseError(ErrorCode.SUB_ERR_DATA_FORMAT, "Response data is error."));
                        return;
                    }
                    //处理设备回复数据
                    int responseOpID = dataCmd.getParam().getXmOpCode();
                    byte[] data = response.getRawData();
                    //parseData(data);
                }
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }



    public void receiveSysInfo(Context context) {
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 添加蓝牙RCSP事件回调器
        manager.addEventListener(new BTRcspEventCallback() {
            //回调设备发送的命令
            @Override
            public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
                super.onDeviceCommand(device, cmd);
                //Step2: 处理命令
                if (cmd.getId() != Command.CMD_SYS_INFO_AUTO_UPDATE) {
                    //过滤命令
                    return;
                }
                //只处理目标命令 - 通知设备系统信息
                UpdateSysInfoCmd updateSysInfoCmd = (UpdateSysInfoCmd) cmd;
                //Step4: 获取参数
                UpdateSysInfoParam param = updateSysInfoCmd.getParam();
                if (null == param) return; //异常处理
                //Step5: 处理数据
//                param.getFunction();  //功能码
//                param.getParamData(); //功能属性
            }
        });
    }


    public void setFunction(Context context) {
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 构造设置功能命令
        //功能码
        int function = AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC; //公共属性
        //标识
        int flag = 0;
        // 拓展参数
        byte[] extend = null;
        CommandBase setFunctionCmd = CommandBuilder.buildFunctionCmd((byte) function, (byte) flag, extend);
        //Step2: 执行操作命令并等待结果回调
        manager.sendCommandAsync(manager.getConnectedDevice(), setFunctionCmd, manager.getBluetoothOption().getTimeoutMs(), new RcspCommandCallback() {
            //回调回复命令
            //注意: 无回复命时，回复命令本身
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                //Step3: 检查设备状态
                if (cmd.getStatus() != StateCode.STATUS_SUCCESS) { //设备状态异常，进行异常处理
                    onErrCode(device, new BaseError(ErrorCode.SUB_ERR_RESPONSE_BAD_STATUS, "Device reply an bad status: " + cmd.getStatus()));
                    return;
                }
                //成功回调
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    public void buildCmd() {
        CommandBase getCurrentModeCmd = CommandBuilder.buildGetSysInfoCmd(AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC,
                0x01 << AttrAndFunCode.SYS_INFO_ATTR_CUR_MODE_TYPE);
        byte mode = AttrAndFunCode.SYS_INFO_FUNCTION_BT;   //-- 蓝牙模式
//                    AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC -- 设备音乐模式
//                    AttrAndFunCode.SYS_INFO_FUNCTION_RTC   -- 时钟模式(闹钟，秒表)
//                    AttrAndFunCode.SYS_INFO_FUNCTION_AUX   -- 外部输入控制模式
//                    AttrAndFunCode.SYS_INFO_FUNCTION_FM    -- FM控制模式
//                    AttrAndFunCode.SYS_INFO_FUNCTION_LIGHT -- 灯光模式
        CommandBase switchDevModeCmd = CommandBuilder.buildSwitchModeCmd(mode);
        CommandBase getMusicDevStatusCmd = CommandBuilder.buildGetSysInfoCmd(AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC,
                0x01 << AttrAndFunCode.SYS_INFO_ATTR_MUSIC_DEV_STATUS);
    }

}
