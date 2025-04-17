package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.base.AttrBean;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.command.double_connect.QueryPhoneBtInfoCmd;
import com.jieli.bluetooth.bean.command.tws.GetADVInfoCmd;
import com.jieli.bluetooth.bean.device.DevBroadcastMsg;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.double_connect.ConnectedBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DeviceBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DoubleConnectionState;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.RcspCommandCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.CommandBuilder;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.configure.DoubleConnectionSp;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.UIHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc Tws功能测试
 * @since 2021/12/2
 */
public class TwsDemo {


    public void getTwsInfo(int mask) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //mask = 0xffffffff; //获取所有属性
        //执行获取设备设置信息功能并等待结果回调
        controller.getDeviceSettingsInfo(controller.getUsingDevice(), mask, new OnRcspActionCallback<ADVInfoResponse>() {
            @Override
            public void onSuccess(BluetoothDevice device, ADVInfoResponse message) {
                //成功回调
                //message - 设置信息
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void getTwsInfoV0(Context context, int mask) {
        //获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //构造功能命令
        //mask = 0xffffffff;  -- 获取所有属性
        CommandBase getTwsInfoCmd = CommandBuilder.buildGetADVInfoCmd(mask);
        //执行获取设备设置信息功能并等待结果回调
        manager.sendCommandAsync(manager.getConnectedDevice(), getTwsInfoCmd, manager.getBluetoothOption().getTimeoutMs(), new RcspCommandCallback() {
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                //检查设备状态
                if (cmd.getStatus() != StateCode.STATUS_SUCCESS) {
                    onErrCode(device, new BaseError(ErrorCode.SUB_ERR_RESPONSE_BAD_STATUS, "Device reply an bad status: " + cmd.getStatus()));
                    return;
                }
                //成功回调
                //获取对应的命令数据
                GetADVInfoCmd command = (GetADVInfoCmd) cmd;
                //获取回复数据
                //注意 - 如果是没有回复数据的命令，回复数据为null
                boolean isHasResponse = command.getType() == CommandBase.FLAG_HAVE_PARAMETER_AND_RESPONSE
                        || command.getType() == CommandBase.FLAG_NO_PARAMETER_AND_RESPONSE;
                if (isHasResponse) {
                    ADVInfoResponse response = command.getResponse();
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


    public void controlADVBroadcast(boolean enable) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onDeviceBroadcast(BluetoothDevice device, DevBroadcastMsg broadcast) {
                //此处将会回调设备广播信息
            }
        });
        //enable - 开关
        //执行控制设备广播信息功能并等待结果回调
        controller.controlAdvBroadcast(controller.getUsingDevice(), enable, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //enable = true, 开启设备广播信息，数据将在BTRcspEventCallback#onDeviceBroadcast回调
                //enable = false, 关闭设备广播信息
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void configFunctionSettings(int type, byte[] value) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //type - 类型
        //value - 数据
        //设置设备功能并等待结果回调
        controller.modifyDeviceSettingsInfo(controller.getUsingDevice(), type, value, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                //成功回调
                //message - 结果码， 0为成功，其他为错误码
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void requestDeviceOperation() {
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {

            @Override
            public void onTwsStatusChange(BluetoothDevice device, boolean isTwsConnected) {
                //此处将会回调TWS连接状态
            }

            @Override
            public void onDeviceRequestOp(BluetoothDevice device, int op) {
                //此处将会回调设备请求操作
                switch (op) {
                    case Constants.ADV_REQUEST_OP_UPDATE_CONFIGURE: //主动更新配置信息
//                        controller.getDeviceSettingsInfo(device, 0xffffffff, null);
                        break;
                    case Constants.ADV_REQUEST_OP_UPDATE_AFTER_REBOOT://更新配置信息，需要重启生效
//                        controller.getDeviceSettingsInfo(device, 0xffffffff, new OnRcspActionCallback<ADVInfoResponse>() {
//                            @Override
//                            public void onSuccess(BluetoothDevice device, ADVInfoResponse message) {
//                                controller.rebootDevice(device, null);
//                            }
//
//                            @Override
//                            public void onError(BluetoothDevice device, BaseError error) {
//
//                            }
//                        });
                        break;
                    case Constants.ADV_REQUEST_OP_SYNC_TIME: //请求同步连接时间
//                        int connectedTime = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
//                        controller.updateConnectedTime(device, connectedTime, null);
                        break;
                    case Constants.ADV_REQUEST_OP_RECONNECT_DEVICE://请求回连设备
                        break;
                    case Constants.ADV_REQUEST_OP_SYNC_DEVICE_INFO: //请求同步设备信息
//                        controller.requestDeviceInfo(device, 0xffffffff, null);
                        break;
                }
            }
        });
    }


    public void updateConnectedTime() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        int connectedTime = (int) (Calendar.getInstance().getTimeInMillis() / 1000); //接入时间
        //执行同步接入时间功能并等待结果回调
        controller.updateConnectedTime(controller.getUsingDevice(), connectedTime, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                //成功回调
                //message - 结果码， 0为成功，其他为错误码
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void configDeviceName(String name) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行设置设备名称功能并等待结果回调
        controller.configDeviceName(controller.getUsingDevice(), name, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                //成功回调
                //message - 结果码， 0为成功，其他为错误码
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void configKeySettings(List<ADVInfoResponse.KeySettings> keySettingsList) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行设置按键效果功能并等待结果回调
        controller.configKeySettings(controller.getUsingDevice(), keySettingsList, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                //成功回调
                //message - 结果码， 0为成功，其他为错误码
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void configLedSettings(List<ADVInfoResponse.LedSettings> ledSettingsList) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行设置闪灯效果功能并等待结果回调
        controller.configLedSettings(controller.getUsingDevice(), ledSettingsList, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                //成功回调
                //message - 结果码， 0为成功，其他为错误码
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public void updateFunctionValue(int type, byte value) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //type - 指定功能
        //value - 值
        //执行更新指定功能的值功能并等待结果回调
        controller.updateFunctionValue(controller.getUsingDevice(), type, value, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                //成功回调
                //message - 结果码， 0为成功，其他为错误码
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    public boolean checkBleScanMsgIsNeck(BleScanMessage bleScanMessage) {
        /** 设备类型是不是耳机类型(0x02,0x03) :
         0x00 -- 音箱类型
         0x01 -- 充电仓类型
         0x02 -- TWS耳机类型
         0x03 -- 耳机类型
         0x04--声卡类型
         0x05--手表类型 */
        boolean isHeadset = UIHelper.isHeadsetByDeviceType(bleScanMessage.getDeviceType());
        //广播包版本是不是挂脖版本
        boolean isNeck = isHeadset && bleScanMessage.getVersion() == SConstant.ADV_INFO_VERSION_NECK_HEADSET;
        return isNeck;
    }


    public boolean checkHistoryDeviceIsNeck(HistoryBluetoothDevice history) {
        /** 芯片类型是不是耳机类型(2,4):
         0:AI SDK (692X_AI智
         能音箱SDK)
         1:ST SDK (692X_标准
         音箱SDK)
         2:693x_TWS SDK
         3:695xSDK
         4:697x_TWS SDK
         5:696x_soundbox
         6:696x_tws
         7:695x_sound_card
         8:695x_sound_watch
         9:701x_sound_watch */
        boolean isHeadsetType = history != null && UIHelper.isHeadsetType(history.getChipType());
        boolean isNeck = isHeadsetType && history.getAdvVersion() != SConstant.ADV_INFO_VERSION_NECK_HEADSET;//广播包类型不为挂脖
        return isNeck;
    }

    public void isSupportDoubleConnection() {
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //查询是否支持设备双连功能
        final BluetoothDevice device = controller.getUsingDevice();
        boolean isSupportDoubleConnection = controller.isSupportDoubleConnection(device);
    }

    public void isSupportDoubleConnectionV0(Context context) {
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 获取设备信息
        final BluetoothDevice device = manager.getConnectedDevice();
        DeviceInfo deviceInfo = manager.getDeviceInfo(device);
        //Step2: 查询是否支持设备双连功能
        boolean isSupportDoubleConnection = deviceInfo != null && deviceInfo.isSupportDoubleConnection();
    }

    public void getDoubleConnectionState() {
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onDoubleConnectionChange(BluetoothDevice device, DoubleConnectionState state) {
                //回调设备双连状态
            }
        });
        //执行查询设备双连状态功能并等待结果回调
        final BluetoothDevice device = controller.getUsingDevice();
        controller.queryDoubleConnectionState(device, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //信息将在BTRcspEventCallback#onDoubleConnectionChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    public void getDoubleConnectionStateV0(Context context) {
        //Step0: 获取JL_BluetoothManager对象
        JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 构建命令 --- 查询设备双连状态
        CommandBase getDoubleConnectionStateCmd = CommandBuilder.buildGetSysInfoCmd(AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC,
                0x00000001 << AttrAndFunCode.SYS_INFO_ATTR_DOUBLE_CONNECT);

    }


    public void setDoubleConnectionState() {
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onDoubleConnectionChange(BluetoothDevice device, DoubleConnectionState state) {
                //回调设备双连状态
            }
        });
        //执行设置设备双连状态功能并等待结果回调
        final BluetoothDevice device = controller.getUsingDevice();
        final DoubleConnectionState state = controller.getDeviceInfo(device).getDoubleConnectionState();
        state.setOn(true);//设置开关
        controller.setDoubleConnectionState(device, state, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //可以等设备推送信息，也可以主动查询
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    public void setDoubleConnectionStateV0(Context context) {
        //Step0: 获取JL_BluetoothManager对象
        final JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        final BluetoothDevice device = manager.getConnectedDevice();
        //需要先调用【获取设备双连状态】功能获取，后通过缓存信息获取
        final DoubleConnectionState state = manager.getDeviceInfo(device).getDoubleConnectionState();
        state.setOn(true);//设置开关
        //构建设置设备双连状态命令
        List<AttrBean> list = new ArrayList<>();
        AttrBean attrBean = new AttrBean();
        attrBean.setType(AttrAndFunCode.SYS_INFO_ATTR_DOUBLE_CONNECT);
        attrBean.setAttrData(state.toData());
        list.add(attrBean);
        CommandBase setDoubleConnectionStateCmd = CommandBuilder.buildSetSysInfoCmd(AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC, list);
    }

    public void queryConnectedPhoneBtInfo() {
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onConnectedBtInfo(BluetoothDevice device, ConnectedBtInfo info) {
                //回调已连接设备的手机信息
            }
        });
        //执行查询已连接设备的手机信息功能并等待结果回调
        final BluetoothDevice device = controller.getUsingDevice();
        //查询数据库保存的本地信息
        DeviceBtInfo deviceBtInfo = DoubleConnectionSp.getInstance().getDeviceBtInfo(device.getAddress());
        if (null == deviceBtInfo) {
            String btName = AppUtil.getBtName(MainApplication.getApplication());
            deviceBtInfo = new DeviceBtInfo().setBtName(btName);
        }
        controller.queryConnectedPhoneBtInfo(device, deviceBtInfo, new OnRcspActionCallback<ConnectedBtInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, ConnectedBtInfo message) {
                //成功回调
                //信息将在BTRcspEventCallback#onConnectedBtInfo回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    public void queryConnectedPhoneBtInfoV0(Context context) {
        //Step0: 获取JL_BluetoothManager对象
        final JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //Step1: 获取当前正在使用的设备
        final BluetoothDevice device = manager.getConnectedDevice();
        if (null == device) return;
        //Step2: 执行查询已连接设备的手机信息功能的值功能并等待结果回调
        DeviceBtInfo deviceBtInfo = DoubleConnectionSp.getInstance().getDeviceBtInfo(device.getAddress());
        if (null == deviceBtInfo) {
            String btName = AppUtil.getBtName(MainApplication.getApplication());
            deviceBtInfo = new DeviceBtInfo().setBtName(btName);
        }
        CommandBase queryConnectedPhoneBtInfoCmd = CommandBuilder.buildQueryConnectedPhoneBtInfoCmd(deviceBtInfo);
        manager.sendRcspCommand(device, queryConnectedPhoneBtInfoCmd, new RcspCommandCallback() {
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                //Step3: 检查设备状态
                if (cmd.getStatus() != StateCode.STATUS_SUCCESS) { //设备状态异常，进行异常处理
                    onErrCode(device, new BaseError(ErrorCode.SUB_ERR_RESPONSE_BAD_STATUS, "Device reply an bad status: " + cmd.getStatus()));
                    return;
                }
                QueryPhoneBtInfoCmd queryPhoneBtInfoCmd = (QueryPhoneBtInfoCmd) cmd;
                QueryPhoneBtInfoCmd.Response resp = queryPhoneBtInfoCmd.getResponse();
                if(null == resp){
                    onErrCode(device, new BaseError(ErrorCode.SUB_ERR_DATA_FORMAT, "Device reply is error."));
                    return;
                }
                //解析数据
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }
}
