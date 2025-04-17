package com.jieli.btsmart.tool.upgrade;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.base.CommonResponse;
import com.jieli.bluetooth.bean.command.ota.EnterUpdateModeCmd;
import com.jieli.bluetooth.bean.command.ota.ExitUpdateModeCmd;
import com.jieli.bluetooth.bean.command.ota.FirmwareUpdateBlockCmd;
import com.jieli.bluetooth.bean.command.ota.FirmwareUpdateStatusCmd;
import com.jieli.bluetooth.bean.command.ota.GetUpdateFileOffsetCmd;
import com.jieli.bluetooth.bean.command.ota.InquireUpdateCmd;
import com.jieli.bluetooth.bean.command.ota.NotifyUpdateContentSizeCmd;
import com.jieli.bluetooth.bean.command.ota.RebootDeviceCmd;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.parameter.FirmwareUpdateBlockParam;
import com.jieli.bluetooth.bean.parameter.FirmwareUpdateBlockResponseParam;
import com.jieli.bluetooth.bean.parameter.InquireUpdateParam;
import com.jieli.bluetooth.bean.parameter.NotifyUpdateContentSizeParam;
import com.jieli.bluetooth.bean.parameter.RebootDeviceParam;
import com.jieli.bluetooth.bean.response.EnterUpdateModeResponse;
import com.jieli.bluetooth.bean.response.ExitUpdateModeResponse;
import com.jieli.bluetooth.bean.response.FirmwareUpdateStatusResponse;
import com.jieli.bluetooth.bean.response.InquireUpdateResponse;
import com.jieli.bluetooth.bean.response.TargetInfoResponse;
import com.jieli.bluetooth.bean.response.UpdateFileOffsetResponse;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.IActionCallback;
import com.jieli.bluetooth.interfaces.bluetooth.BluetoothCallbackImpl;
import com.jieli.bluetooth.interfaces.bluetooth.RcspCommandCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.tool.DeviceStatusManager;
import com.jieli.bluetooth.tool.ParseHelper;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.FileUtil;
import com.jieli.jl_http.JL_HttpClient;
import com.jieli.jl_http.bean.OtaMessage;
import com.jieli.jl_http.interfaces.IActionListener;
import com.jieli.jl_http.interfaces.IDownloadListener;
import com.jieli.jl_http.util.Constant;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

/**
 * 升级管理器
 *
 * @author zqjasonZhong
 * @date 2018/12/24
 */
@Deprecated
public class FirmwareUpgradeManager implements IUpgradeManager {
    private static final String TAG = "FirmwareUpgradeManager";
    private static volatile FirmwareUpgradeManager instance;
    private final Handler mainHandler;

    private final RCSPController mRCSPController = RCSPController.getInstance();
    private ReadFileThread mReadFileThread;
    private IUpgradeCallback mCallback;

    private String upgradeFilePath; //固件升级文件路径
    private byte[] mUpgradeDataBuf;
    private boolean isOTA = false;
    private long timeout_ms = 30 * 1000; //接收命令超时时间
    private int mUpdateContentSize = 0;  //升级需要数据的总长度
    private int mCurrentSumFileSize = 0; //当前已发生数据的累计长度
    private long mStartTime = 0; //开始时间
    private long mTotalTime = 0; //总共花费时间
    private boolean isNeedBanDialog = false; //是否需要禁止弹窗

    /*------------------------------------------------------------------
     * Error Code
     *-----------------------------------------------------------------*/
    public static final int ERROR_BLUETOOTH_DEVICE_NOT_CONNECT = 0xff01;
    public static final int ERROR_FILE_NOT_EXIST = 0xff02;
    public static final int ERROR_FILE_NOT_FOUND = 0xff03;
    public static final int ERROR_IO_EXCEPTION = 0xff04;
    public static final int ERROR_RESPONSE_FAILED = 0xff05;
    public static final int ERROR_RESPONSE_IS_EMPTY = 0xff06;
    public static final int ERROR_PARAM_FAILED = 0xff07;
    public static final int ERROR_PARAM_IS_EMPTY = 0xff08;
    public static final int ERROR_OFFSET_OVER = 0xff09;
    public static final int ERROR_DEVICE_LOW_VOLTAGE = 0xff0a;
    public static final int ERROR_CHECK_UPGRADE_FILE = 0xff0b;
    public static final int ERROR_ENTER_UPDATE_MODE_FAILED = 0xff0c;
    public static final int ERROR_CHECK_RECEIVED_DATA_FAILED = 0xff0d;
    public static final int ERROR_UPGRADE_TYPE_NOT_MATCH = 0xff0e;
    public static final int ERROR_UPGRADE_FAILED = 0xff0f;
    public static final int ERROR_UPGRADE_KEY_NOT_MATCH = 0xff10;
    public static final int ERROR_RECEIVE_TIMEOUT = 0xff11;
    public static final int ERROR_NO_UPDATE_VERSION = 0xff12;
    public static final int ERROR_UPGRADE_FILE_VERSION_SAME = 0xff13;
    public static final int ERROR_DATA_LENGTH_INVALID = 0xff14;
    public static final int ERROR_FLASH_READ = 0xff15;
    public static final int ERROR_RECEIVE_CMD_TIMEOUT = 0xff16;
    public static final int ERROR_TWS_NOT_CONNECT = 0xff17;
    public static final int ERROR_HEADSET_NOT_IN_CHARGING_BIN = 0xff18;
    public static final int ERROR_OTA_IN_HANDLE = 0xFF19;

    public static final int ERROR_UNKNOWN = 0xfffe;


    public static final boolean IS_AUTO_SET_BLE_MODE = false;


    private FirmwareUpgradeManager() {

        mainHandler = new Handler(Looper.getMainLooper());

        mRCSPController.addBTRcspEventCallback(mBTRcspEventCallback);
        if (IS_AUTO_SET_BLE_MODE) {
            mRCSPController.getBtOperation().registerBluetoothCallback(mBtEventCallback);
        }
        upgradeFilePath = FileUtil.createFilePath(MainApplication.getApplication(), MainApplication.getApplication().getPackageName(), SConstant.DIR_UPDATE);
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        if (deviceInfo != null && deviceInfo.getSdkType() < JLChipFlag.JL_CHIP_FLAG_693X_TWS_HEADSET) {
            upgradeFilePath += "/" + SConstant.FIRMWARE_UPGRADE_FILE;
        } else {
            upgradeFilePath += "/" + SConstant.FIRMWARE_UPGRADE_FILE_AC693;
        }
    }

    public static FirmwareUpgradeManager getInstance() {
        if (instance == null) {
            synchronized (FirmwareUpgradeManager.class) {
                if (instance == null) {
                    instance = new FirmwareUpgradeManager();
                }
            }
        }
        return instance;
    }

    public boolean isOTA() {
        return isOTA;
    }

    private void setIsOTA(boolean ota) {
        this.isOTA = ota;
    }

    public void setUpgradeFilePath(String upgradeFilePath) {
        this.upgradeFilePath = upgradeFilePath;
    }

    public String getUpgradeFilePath() {
        return upgradeFilePath;
    }

    @Override
    public void configure() {

    }

    @Override
    public void startOTA(IUpgradeCallback callback) {
        if (mRCSPController.isDeviceConnected()) {
            if (!isOTA) {
                mCallback = callback;
//                checkServerOTAFile();
                callbackStartOTA();
                callbackProgress(mRCSPController.getUsingDevice(), 0f);
                startReadFileThread(upgradeFilePath);
                setIsOTA(true);
                if (IS_AUTO_SET_BLE_MODE) {
                    BluetoothGatt connectedGatt = mRCSPController.getBtOperation().getConnectedBluetoothGatt();
                    if (CommonUtil.checkHasConnectPermission(MainApplication.getApplication()) && connectedGatt != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        boolean ret = connectedGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                        JL_Log.w(TAG, "startOTA :: requestConnectionPriority :: ret : " + ret);
                    }
                }
            } else {
                callbackError(new BaseError(ERROR_OTA_IN_HANDLE, AppUtil.getContext().getString(R.string.ota_error_msg_ota_is_continuing)));
            }
        } else {
            callbackError(new BaseError(ERROR_BLUETOOTH_DEVICE_NOT_CONNECT, AppUtil.getContext().getString(R.string.ota_error_msg_device_not_connected)));
        }
    }

    @Override
    public void cancelOTA() {
        stopReadFileThread();
        exitUpdateMode(mRCSPController.getUsingDevice());
    }

    @Override
    public void release() {
        if (isOTA) {
            callbackCancelOTA();
        }
        mRCSPController.removeBTRcspEventCallback(mBTRcspEventCallback);
        if (IS_AUTO_SET_BLE_MODE) {
            mRCSPController.getBtOperation().unregisterBluetoothCallback(mBtEventCallback);
        }
        mCallback = null;
        mUpdateContentSize = 0;
        mCurrentSumFileSize = 0;
        setIsOTA(false);
        stopReadFileThread();
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        instance = null;
    }

    public boolean isNeedBanDialog() {
        return isNeedBanDialog;
    }

    public void setNeedBanDialog(boolean needBanDialog) {
        isNeedBanDialog = needBanDialog;
    }

    public long getTimeout_ms() {
        return timeout_ms;
    }

    public void setTimeout_ms(long timeout_ms) {
        this.timeout_ms = timeout_ms;
    }

    public void resetTotalTime() {
        mStartTime = 0;
        mTotalTime = 0;
    }

    public long getTotalTime() {
        return mTotalTime;
    }


    public boolean isUpgradeFileExist() {
        return FileUtil.checkFileExist(upgradeFilePath);
    }

    /*
     * 升级流程第一步骤
     */
    private void upgradeStep01() {
        if (!isOTA) {
            JL_Log.d(TAG, "upgradeStep01 : ota has exited.");
            return;
        }
        //Step01.发送询问升级文件信息偏移命令
        mRCSPController.sendRcspCommand(mRCSPController.getUsingDevice(), new GetUpdateFileOffsetCmd(), new RcspCommandCallback() {
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                GetUpdateFileOffsetCmd getUpdateFileOffsetCmd = (GetUpdateFileOffsetCmd) cmd;
                JL_Log.i(TAG, "Step01.获取升级文件信息的偏移地址, \n" + getUpdateFileOffsetCmd);
                if (cmd.getStatus() == StateCode.STATUS_SUCCESS) {
                    UpdateFileOffsetResponse response = getUpdateFileOffsetCmd.getResponse();
                    if (response != null) {
                        int len = response.getUpdateFileFlagLen();
                        int offset = response.getUpdateFileFlagOffset();
                        upgradeStep02(offset, len);
                    } else { //回复错误
                        callbackError(new BaseError(ERROR_RESPONSE_IS_EMPTY, AppUtil.getContext().getString(R.string.ota_error_msg_response_empty)));
                    }
                } else { //回复失败
                    callbackError(new BaseError(ERROR_RESPONSE_FAILED, AppUtil.getContext().getString(R.string.ota_error_msg_response_status_error) + cmd.getStatus()));
                }
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                JL_Log.e(TAG, "--->callbackError --0000");
                callbackError(error);
            }
        });
    }

    /*
     * 升级流程第二步骤
     *
     * @param offset 偏移地址
     * @param len 长度
     */
    private void upgradeStep02(int offset, int len) {
        if (!isOTA) {
            JL_Log.d(TAG, "upgradeStep02 : ota has exited.");
            return;
        }
        if (len >= 0 && offset >= 0) {
            //Step02.发送升级文件校验信息，确认是否可以升级
            InquireUpdateParam param = new InquireUpdateParam();
            if (len > 0) {
                param.setUpdateFileFlagData(readBlockData(offset, len));
            } else {
                int priority = mRCSPController.getBluetoothManager().getBluetoothOption().getPriority();
                byte[] data = new byte[1];
                data[0] = (byte) priority;
                param.setUpdateFileFlagData(data);
            }
            InquireUpdateCmd cmd = new InquireUpdateCmd(param);
            mRCSPController.sendRcspCommand(mRCSPController.getUsingDevice(), cmd, new RcspCommandCallback() {
                @Override
                public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                    InquireUpdateCmd inquireUpdateCmd = (InquireUpdateCmd) cmd;
                    JL_Log.i(TAG, "Step02.发送升级文件校验信息，确认是否可以升级, \n" + inquireUpdateCmd);
                    if (cmd.getStatus() == StateCode.STATUS_SUCCESS) {
                        InquireUpdateResponse response = inquireUpdateCmd.getResponse();
                        if (response != null) {
                            int canUpdateFlag = response.getCanUpdateFlag();
                            switch (canUpdateFlag) {
                                case StateCode.RESULT_CAN_UPDATE: //可以升级
                                    DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(device);
                                    if (deviceInfo != null && deviceInfo.isNeedBootLoader()) { //需要下载boot loader
                                        DeviceStatusManager.getInstance().updateDeviceMaxCommunicationMtu(device, Constants.OTA_PROTOCOL_MTU);
                                        startReceiveCmdTimeout();
                                    } else {
                                        upgradeStep03();
                                    }
                                    break;
                                case StateCode.RESULT_DEVICE_LOW_VOLTAGE_EQUIPMENT: //低电量
                                    callbackError(new BaseError(ERROR_DEVICE_LOW_VOLTAGE, AppUtil.getContext().getString(R.string.ota_error_msg_low_equipmeent)));
                                    break;
                                case StateCode.RESULT_FIRMWARE_INFO_ERROR: //文件信息错误
                                    callbackError(new BaseError(ERROR_CHECK_UPGRADE_FILE, AppUtil.getContext().getString(R.string.ota_error_msg_check_upgrade_file_failed)));
                                    break;
                                case StateCode.RESULT_FIRMWARE_VERSION_NO_CHANGE: //升级版本一致
                                    callbackError(new BaseError(ERROR_UPGRADE_FILE_VERSION_SAME, AppUtil.getContext().getString(R.string.ota_error_msg_upgrade_file_version_no_change)));
                                    break;
                                case StateCode.RESULT_TWS_NOT_CONNECT:
                                    callbackError(new BaseError(ERROR_TWS_NOT_CONNECT, AppUtil.getContext().getString(R.string.ota_error_msg_tws_is_not_connected)));
                                    break;
                                case StateCode.RESULT_HEADSET_NOT_IN_CHARGING_BIN:
                                    callbackError(new BaseError(ERROR_HEADSET_NOT_IN_CHARGING_BIN, AppUtil.getContext().getString(R.string.ota_error_msg_headset_not_in_charging)));
                                    break;
                                default:
                                    callbackError(new BaseError(ERROR_UNKNOWN, AppUtil.getContext().getString(R.string.ota_error_msg_error_unkonwn)));
                                    break;
                            }
                        } else { //回复错误
                            callbackError(new BaseError(ERROR_RESPONSE_IS_EMPTY, AppUtil.getContext().getString(R.string.ota_error_msg_response_empty)));
                        }
                    } else { //回复失败
                        callbackError(new BaseError(ERROR_RESPONSE_FAILED, AppUtil.getContext().getString(R.string.ota_error_msg_response_status_error) + cmd.getStatus()));
                    }
                }

                @Override
                public void onErrCode(BluetoothDevice device, BaseError error) {
                    JL_Log.e(TAG, "--->callbackError --11111");
                    callbackError(error);
                }
            });
        } else { //参数错误
            callbackError(new BaseError(ErrorCode.SUB_ERR_PARAMETER, AppUtil.getContext().getString(R.string.ota_error_msg_error_param)));
        }
    }

    /*
     * 升级流程第三步骤
     */
    private void upgradeStep03() {
        if (!isOTA) {
            JL_Log.d(TAG, "upgradeStep03 : ota has exited.");
            return;
        }
        //Step03.请求进入升级模式
        mRCSPController.sendRcspCommand(mRCSPController.getUsingDevice(), new EnterUpdateModeCmd(), new RcspCommandCallback() {
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                EnterUpdateModeCmd enterUpdateModeCmd = (EnterUpdateModeCmd) cmd;
                JL_Log.i(TAG, "Step03.请求进入升级模式, \n" + enterUpdateModeCmd);
                if (cmd.getStatus() == StateCode.STATUS_SUCCESS) {
                    EnterUpdateModeResponse response = enterUpdateModeCmd.getResponse();
                    if (response != null) {
                        int canUpdateFlag = response.getCanUpdateFlag();
                        if (canUpdateFlag == StateCode.RESULT_OK) { //进入升级模式成功
                            JL_Log.w(TAG, "enter upgrade mode success, waiting for device command.");
                            DeviceStatusManager.getInstance().updateDeviceMaxCommunicationMtu(device, Constants.OTA_PROTOCOL_MTU);
                            startReceiveCmdTimeout();
                        } else { //进入升级模式失败
                            callbackError(new BaseError(ERROR_ENTER_UPDATE_MODE_FAILED, AppUtil.getContext().getString(R.string.ota_error_msg_enter_update_mode_failed)));
                        }
                    } else { //回复错误
                        callbackError(new BaseError(ERROR_RESPONSE_IS_EMPTY, AppUtil.getContext().getString(R.string.ota_error_msg_response_empty)));
                    }
                } else { //回复失败
                    callbackError(new BaseError(ERROR_RESPONSE_FAILED, AppUtil.getContext().getString(R.string.ota_error_msg_response_status_error) + cmd.getStatus()));
                }
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                JL_Log.e(TAG, "--->callbackError --2222");
                callbackError(error);
            }
        });
    }

    /*
     * 升级流程第四步骤
     *
     * @param firmwareUpdateBlockCmd 发送升级数据命令
     * @param offset  偏移地址
     * @param length  长度
     */
    private void upgradeStep04(BluetoothDevice device, FirmwareUpdateBlockCmd firmwareUpdateBlockCmd, int offset, int length) {
        if (!isOTA) {
            JL_Log.d(TAG, "upgradeStep04 : ota has exited.");
            return;
        }
        //Step04.发送升级数据
        if (offset == 0 && length == 0) { //设备通知传输数据结束
            JL_Log.e(TAG, "upgradeStep04 ::::: over .....");
            firmwareUpdateBlockCmd.setParam(null);
            firmwareUpdateBlockCmd.setStatus(StateCode.STATUS_SUCCESS);
            mRCSPController.sendRcspResponse(device, firmwareUpdateBlockCmd);
            upgradeStep05(device);
        } else {
            byte[] data = readBlockData(offset, length);
            if (data != null && data.length > 0) {
//                callbackProgress();
                FirmwareUpdateBlockResponseParam responseParam = new FirmwareUpdateBlockResponseParam(data);
                firmwareUpdateBlockCmd.setParam(responseParam);
                firmwareUpdateBlockCmd.setStatus(StateCode.STATUS_SUCCESS);
                mRCSPController.sendRcspResponse(device, firmwareUpdateBlockCmd);
                startReceiveCmdTimeout();
                firmwareUpdateBlockCmd.setParam(null);
            } else {
                callbackError(new BaseError(ERROR_OFFSET_OVER, AppUtil.getContext().getString(R.string.ota_error_msg_offest_over_limit)));
            }
        }
    }

    /*
     * 升级流程第五步骤
     */
    private void upgradeStep05(BluetoothDevice device) {
        if (!isOTA) {
            JL_Log.d(TAG, "upgradeStep05 : ota has exited.");
            return;
        }
        //Step04.询问升级状态
        mRCSPController.sendRcspCommand(device, new FirmwareUpdateStatusCmd(), new RcspCommandCallback() {
            @Override
            public void onCommandResponse(BluetoothDevice device1, CommandBase cmd) {
                FirmwareUpdateStatusCmd firmwareUpdateStatusCmd = (FirmwareUpdateStatusCmd) cmd;
                JL_Log.i(TAG, "Step05.询问升级状态, \n" + firmwareUpdateStatusCmd);
                if (cmd.getStatus() == StateCode.STATUS_SUCCESS) {
                    FirmwareUpdateStatusResponse response = firmwareUpdateStatusCmd.getResponse();
                    if (response != null) {
                        int updateStatus = response.getResult();
                        switch (updateStatus) {
                            case StateCode.UPGRADE_RESULT_COMPLETE: //升级完成
                                callbackProgress(device1, 100f);
                                callbackStopOTA();
                                rebootDevice(device1);
                                break;
                            case StateCode.UPGRADE_RESULT_DATA_CHECK_ERROR: //数据校验失败
                                callbackError(new BaseError(ERROR_CHECK_RECEIVED_DATA_FAILED, AppUtil.getContext().getString(R.string.ota_error_msg_receive_error_data)));
                                break;
                            case StateCode.UPGRADE_RESULT_FAIL: //升级失败
                                callbackError(new BaseError(ERROR_UPGRADE_FAILED, AppUtil.getContext().getString(R.string.ota_error_msg_upgrade_failed)));
                                break;
                            case StateCode.UPGRADE_RESULT_ENCRYPTED_KEY_NOT_MATCH: //升级失败，加密Key不匹配
                                callbackError(new BaseError(ERROR_UPGRADE_KEY_NOT_MATCH, AppUtil.getContext().getString(R.string.ota_error_msg_key_not_match)));
                                break;
                            case StateCode.UPGRADE_RESULT_UPGRADE_FILE_ERROR://升级失败，升级文件出错
                                callbackError(new BaseError(ERROR_CHECK_UPGRADE_FILE, AppUtil.getContext().getString(R.string.ota_error_msg_check_file_error)));
                                break;
                            case StateCode.UPGRADE_RESULT_UPGRADE_TYPE_ERROR://升级失败，升级类型错误
                                callbackError(new BaseError(ERROR_UPGRADE_TYPE_NOT_MATCH, AppUtil.getContext().getString(R.string.ota_error_msg_type_not_match)));
                                break;
                            case StateCode.UPGRADE_RESULT_ERROR_LENGTH: //升级失败，数据长度错误
                                callbackError(new BaseError(ERROR_DATA_LENGTH_INVALID, AppUtil.getContext().getString(R.string.ota_error_msg_data_length_error)));
                                break;
                            case StateCode.UPGRADE_RESULT_FLASH_READ://升级失败，flash读写失败
                                callbackError(new BaseError(ERROR_FLASH_READ, AppUtil.getContext().getString(R.string.ota_error_msg_flash_read_error)));
                                break;
                            case StateCode.UPGRADE_RESULT_CMD_TIMEOUT://升级失败，命令处理超时
                                callbackError(new BaseError(ERROR_RECEIVE_CMD_TIMEOUT, AppUtil.getContext().getString(R.string.ota_error_msg_handler_cmd_timeout)));
                                break;
                            case StateCode.UPGRADE_RESULT_DOWNLOAD_BOOT_LOADER_SUCCESS: { //下载boot loader 成功
                                resetTotalTime();
                                upgradeStep03();
                                break;
                            }
                            default:
                                callbackError(new BaseError(ERROR_UNKNOWN, AppUtil.getContext().getString(R.string.ota_error_msg_response_status_error) + updateStatus));
                                break;
                        }
                    } else { //回复错误
                        callbackError(new BaseError(ERROR_RESPONSE_IS_EMPTY, AppUtil.getContext().getString(R.string.ota_error_msg_response_empty)));
                    }
                } else { //回复失败
                    callbackError(new BaseError(ERROR_RESPONSE_FAILED, AppUtil.getContext().getString(R.string.ota_error_msg_response_status_error) + cmd.getStatus()));
                }
            }

            @Override
            public void onErrCode(BluetoothDevice device1, BaseError error) {
                JL_Log.e(TAG, "--->callbackError --3333");
                callbackError(error);
            }
        });
    }

    private void exitUpdateMode(BluetoothDevice device) {
        if (!isOTA) {
            JL_Log.d(TAG, "exitUpdateMode : ota has exited.");
            return;
        }
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(device);
        if (deviceInfo != null && deviceInfo.isSupportDoubleBackup()) {
            setIsOTA(false); //先禁止其他命令的影响
            mRCSPController.sendRcspCommand(device, new ExitUpdateModeCmd(), new RcspCommandCallback() {
                @Override
                public void onCommandResponse(BluetoothDevice device1, CommandBase cmd) {
                    ExitUpdateModeCmd exitUpdateModeCmd = (ExitUpdateModeCmd) cmd;
                    if (cmd.getStatus() == StateCode.STATUS_SUCCESS) {
                        ExitUpdateModeResponse response = exitUpdateModeCmd.getResponse();
                        if (response != null) {
                            int result = response.getResult();
                            if (result == StateCode.RESULT_OK) {
                                callbackCancelOTA();
                            }
                        } else { //回复错误
                            callbackError(new BaseError(ERROR_RESPONSE_IS_EMPTY, AppUtil.getContext().getString(R.string.ota_error_msg_response_empty)));
                        }
                    } else { //回复失败
                        callbackError(new BaseError(ERROR_RESPONSE_FAILED, AppUtil.getContext().getString(R.string.ota_error_msg_response_status_error) + cmd.getStatus()));
                    }
                }

                @Override
                public void onErrCode(BluetoothDevice device1, BaseError error) {
                    JL_Log.e(TAG, "--->callbackError --444444");
                    callbackError(error);
                }
            });
        }
    }

    private void rebootDevice(BluetoothDevice device) {
        mRCSPController.sendRcspCommand(device, new RebootDeviceCmd(new RebootDeviceParam(Constants.DEVICE_REBOOT)), new RcspCommandCallback() {
            @Override
            public void onCommandResponse(BluetoothDevice device1, CommandBase cmd) {
                JL_Log.i(TAG, "-rebootDevice- " + cmd);
            }

            @Override
            public void onErrCode(BluetoothDevice device1, BaseError error) {
                JL_Log.w(TAG, "-rebootDevice- =onErrCode= " + error);
            }
        });
    }

    private byte[] readBlockData(int offset, int len) {
        if (mUpgradeDataBuf != null && mUpgradeDataBuf.length > 0) {
            byte[] value = new byte[len];
            if (offset + len <= mUpgradeDataBuf.length) {
                System.arraycopy(mUpgradeDataBuf, offset, value, 0, len);
                return value;
            }
        }
        return null;
    }

    private void callbackStartOTA() {
        resetTotalTime();
        if (mainHandler != null && mCallback != null) {
            mainHandler.post(() -> {
                if (mCallback != null) {
                    mCallback.onStartOTA();
                }
            });
        }
    }

    private void callbackProgress(BluetoothDevice device, float progress) {
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(device);
        int type = (deviceInfo != null && deviceInfo.isNeedBootLoader()) ? 0 : 1;
        callbackProgress(type, progress);
    }

    private void callbackProgress(final int type, final float progress) {
        if (mStartTime > 0) {
            mTotalTime = new Date().getTime() - mStartTime;
        }
        JL_Log.i("zzc_ota", "callbackProgress :: " + mCallback + ", " + mainHandler);
        if (mainHandler != null && mCallback != null) {
            mainHandler.post(() -> {
                if (mCallback != null) {
                    mCallback.onProgress(type, progress);
                }
            });
        }
    }

    private void callbackStopOTA() {
        if (mStartTime > 0) {
            mTotalTime = new Date().getTime() - mStartTime;
            mStartTime = 0;
        }
        setIsOTA(false);
        mCurrentSumFileSize = 0;
        mUpdateContentSize = 0;
        if (mainHandler != null && mCallback != null) {
            mainHandler.post(() -> {
                if (mCallback != null) {
                    mCallback.onStopOTA();
                }
            });
        }
    }

    private void callbackCancelOTA() {
        stopReceiveCmdTimeout();
        setIsOTA(false);
        mCurrentSumFileSize = 0;
        mUpdateContentSize = 0;
        if (mStartTime > 0) {
            mTotalTime = new Date().getTime() - mStartTime;
            mStartTime = 0;
        }
        if (mainHandler != null && mCallback != null) {
            mainHandler.post(() -> {
                if (mCallback != null) {
                    mCallback.onCancelOTA();
                }
            });
        }
    }

    private void callbackError(final BaseError error) {
        if (error == null) return;
        if (ERROR_OTA_IN_HANDLE != error.getSubCode()) {
            stopReceiveCmdTimeout();
            setIsOTA(false);
            mCurrentSumFileSize = 0;
            mUpdateContentSize = 0;
            if (mStartTime > 0) {
                mTotalTime = new Date().getTime() - mStartTime;
                mStartTime = 0;
            }
        }
        if (mainHandler != null && mCallback != null) {
            mainHandler.post(() -> {
                if (mCallback != null) {
                    mCallback.onError(error);
                }
            });
        }
    }

    /**
     * 获取服务器OTA最新文件
     *
     * @param callback 回调
     */
    public void requestServerOTAFile(BluetoothDevice device, IActionCallback<OtaMessage> callback) {
        //TODO：测试账号[authKey : hE9yfseX6UdK7rFh, projectCode : AI_SDK_test1]
        String authKey = "";
        String projectCode = "";
        if (mRCSPController.isDeviceConnected(device)) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(device);
            if (deviceInfo != null) {
                String devAuthKey = deviceInfo.getAuthKey();
                String devProjectCode = deviceInfo.getProjectCode();
                if (!TextUtils.isEmpty(devAuthKey) && !devAuthKey.equals(authKey)) {
                    authKey = deviceInfo.getAuthKey();
                }
                if (!TextUtils.isEmpty(devProjectCode) && !devProjectCode.equals(projectCode)) {
                    projectCode = deviceInfo.getProjectCode();
                }
            }
        }
        JL_HttpClient.getInstance().requestOTAFile(authKey, projectCode, Constant.PLATFORM_FIRMWARE, new IActionListener<OtaMessage>() {
            @Override
            public void onSuccess(final OtaMessage response) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(response));
                }
            }

            @Override
            public void onError(final int code, final String message) {
                JL_Log.w(TAG, "requestServerOTAFile:: onError : " + message);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(new BaseError(code, message)));
                }
            }
        });
    }

    public boolean judgeDeviceNeedToOta(BluetoothDevice device, OtaMessage message) {
        if (device == null || message == null) return false;
        TargetInfoResponse targetInfo = mRCSPController.getDeviceInfo(device);
        if (targetInfo == null) {
            callbackError(new BaseError(ERROR_BLUETOOTH_DEVICE_NOT_CONNECT, "device is not connected."));
            return false;
        }
        int versionCode = targetInfo.getVersionCode();
        int serverFirmware = ParseHelper.convertVersionByString(message.getVersion());
        JL_Log.i(TAG, "judgeDeviceNeedToOta:: versionCode : " + versionCode + ", sever firmware version : " + serverFirmware);
        return versionCode < serverFirmware || (serverFirmware == 0);
    }

    private void checkServerOTAFile(final BluetoothDevice device) {
        JL_Log.d(TAG, "checkServerOTAFile");
        callbackStartOTA();
        callbackProgress(device, 0f);
        requestServerOTAFile(device, new IActionCallback<OtaMessage>() {
            @Override
            public void onSuccess(OtaMessage message) {
                if (judgeDeviceNeedToOta(device, message)) {
                    JL_HttpClient.getInstance().downloadFile(message.getUrl(), upgradeFilePath, new IDownloadListener() {
                        @Override
                        public void onStart(String startPath) {
                            JL_Log.i(TAG, "checkServerOTAFile::onStart >> " + startPath);
                        }

                        @Override
                        public void onProgress(float progress) {
                            JL_Log.i(TAG, "checkServerOTAFile::onProgress >> " + progress);
                        }

                        @Override
                        public void onStop(String outputPath) {
                            JL_Log.i(TAG, "checkServerOTAFile::onStop >> " + outputPath);
                            startReadFileThread(outputPath);
                        }

                        @Override
                        public void onError(int code, String message) {
                            JL_Log.w(TAG, "checkServerOTAFile::onError >> " + message);
                            message = AppUtil.getContext().getString(R.string.ota_error_msg_download_ota_file_failed) + message;
                            callbackError(new BaseError(code, message));
                        }
                    });
                } else {
                    callbackError(new BaseError(ERROR_NO_UPDATE_VERSION, AppUtil.getContext().getString(R.string.ota_error_msg_no_update_version)));
                }
            }

            @Override
            public void onError(BaseError error) {
                JL_Log.e(TAG, "--->callbackError --55555");
                error.setMessage(AppUtil.getContext().getString(R.string.ota_error_msg_request_ota_file_failed) + error.getMessage());
                callbackError(error);
            }
        });
    }


    private void startReadFileThread(String filePath) {
        if (mReadFileThread == null) {
            mReadFileThread = new ReadFileThread(filePath, mReadFileCallback);
            mReadFileThread.start();
        }
    }

    private void stopReadFileThread() {
        if (mReadFileThread != null) {
            if (mReadFileThread.isAlive()) {
                mReadFileThread.interrupt();
                mReadFileThread.interrupt();
            }
            mReadFileThread = null;
        }
    }

    private class ReadFileThread extends Thread {
        private final String mUpgradeFilePath;
        private final IActionCallback<String> mReadFileCallback;


        private ReadFileThread(String filePath, IActionCallback<String> callback) {
            mUpgradeFilePath = filePath;
            mReadFileCallback = callback;
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        public void run() {
            if (mRCSPController.isDeviceConnected()) {
                if (!TextUtils.isEmpty(mUpgradeFilePath) && FileUtil.checkFileExist(mUpgradeFilePath)) {
                    // Step00.检查文件的合法性，缓存数据
                    FileInputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(mUpgradeFilePath);
                        mUpgradeDataBuf = new byte[inputStream.available()];
                        inputStream.read(mUpgradeDataBuf);
//                        inputStream.close();
                        notifySuccess();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        notifyError(new BaseError(ERROR_FILE_NOT_FOUND, AppUtil.getContext().getString(R.string.ota_error_msg_file_not_found)));
                    } catch (IOException e) {
                        e.printStackTrace();
                        notifyError(new BaseError(ERROR_IO_EXCEPTION, AppUtil.getContext().getString(R.string.ota_error_msg_file_read_fail)));
                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    notifyError(new BaseError(ERROR_FILE_NOT_EXIST, AppUtil.getContext().getString(R.string.ota_error_msg_file_not_exist)));
                }
            } else { //设备未连接
                notifyError(new BaseError(ERROR_BLUETOOTH_DEVICE_NOT_CONNECT, AppUtil.getContext().getString(R.string.ota_error_msg_bluetooth_not_connected)));

            }
            mReadFileThread = null;
        }

        private void notifySuccess() {
            mainHandler.post(() -> {
                if (mReadFileCallback != null) {
                    mReadFileCallback.onSuccess(mUpgradeFilePath);
                }
            });
        }

        private void notifyError(final BaseError error) {
            if (error != null) {
                mainHandler.post(() -> {
                    if (mReadFileCallback != null) {
                        mReadFileCallback.onError(error);
                    }
                });
            }
        }
    }

    private final IActionCallback<String> mReadFileCallback = new IActionCallback<String>() {
        @Override
        public void onSuccess(String message) {
           /* if(FileUtil.checkFileExist(upgradeFilePath)){
                FileUtil.deleteFile(new File(upgradeFilePath));
            }*/
            upgradeStep01();
        }

        @Override
        public void onError(BaseError error) {
            JL_Log.e(TAG, "--->callbackError --66666");
            callbackError(error);
        }
    };

    private final BluetoothCallbackImpl mBtEventCallback = new BluetoothCallbackImpl() {

        @Override
        public void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency, int timeout, int status) {
            JL_Log.e(TAG, "ota ==> onConnectionUpdated :: device :" + BluetoothUtil.printBtDeviceInfo(gatt.getDevice())
                    + " , interval:" + interval + " latency:" + latency + ",timeout = " + timeout + ", status = " + status);
            if (IS_AUTO_SET_BLE_MODE && isOTA && status == 0 && interval > 40) {
                if (CommonUtil.checkHasConnectPermission(MainApplication.getApplication()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mainHandler.post(() -> {
                        boolean ret = gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                        JL_Log.e(TAG, "requestConnectionPriority :: ret : " + ret);
                    });
                }
            }
        }
    };

    private final BTRcspEventCallback mBTRcspEventCallback = new BTRcspEventCallback() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
         /*   DeviceReConnectManager reConnectManager = DeviceReConnectManager.getInstance(mRCSPController.getBtOperation());
            boolean isWaitingForUpdate = reConnectManager.isDeviceReconnecting();
            boolean isReConnect = reConnectManager.checkIsReconnectDevice(device);
            boolean isMandatoryUpgrade = DeviceStatusManager.getInstance().isMandatoryUpgrade(device);
            if (status == StateCode.CONNECTION_FAILED || status == StateCode.CONNECTION_DISCONNECT) {
                if (!isWaitingForUpdate && isOTA) { //升级过程中异常断开
                    callbackError(new BaseError(ERROR_BLUETOOTH_DEVICE_NOT_CONNECT, AppUtil.getContext().getString(R.string.ota_error_msg_device_disconnected)));
                }
            } else if (status == StateCode.CONNECTION_OK) {
                if (!isReConnect && !isWaitingForUpdate && isOTA) {//清标志位
                    if (!isMandatoryUpgrade) {
                        setIsOTA(false);
                    }
//                    callbackError(new BaseError(ErrorCode.SUB_ERR_OTA_FAILED, "ota is aborted."));
                }
            }*/
        }

        @Override
        public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
            if (BluetoothUtil.deviceEquals(mRCSPController.getUsingDevice(), device) && cmd != null) { //确定设备
                int opCode = cmd.getId();
                JL_Log.i(TAG, "-onDeviceCommand- opcode : " + opCode);
                switch (opCode) {
                    case Command.CMD_OTA_SEND_FIRMWARE_UPDATE_BLOCK: {
                        FirmwareUpdateBlockCmd firmwareUpdateBlockCmd = (FirmwareUpdateBlockCmd) cmd;
                        JL_Log.i(TAG, "-onDeviceCommand- firmwareUpdateBlockCmd : " + firmwareUpdateBlockCmd.toString());
                        stopReceiveCmdTimeout();
                        if (isOTA) {
                            //关闭延时发数模式
                            DeviceStatusManager.getInstance().updateDeviceIsEnableLatencyMode(device, false);
                            FirmwareUpdateBlockParam param = firmwareUpdateBlockCmd.getParam();
                            if (param != null) {
                                int nextOffset = param.getNextUpdateBlockOffsetAddr();
                                int length = param.getNextUpdateBlockLen();
                                if (isStartSumProgress()) {
                                    mCurrentSumFileSize += length;
                                    callbackProgress(device, getCurrentProgress(mCurrentSumFileSize));
                                }
                                upgradeStep04(device, firmwareUpdateBlockCmd, nextOffset, length);
                            } else {
                                callbackError(new BaseError(ERROR_PARAM_IS_EMPTY, AppUtil.getContext().getString(R.string.ota_error_msg_param_null)));
                            }
                        } else {
                            JL_Log.w(TAG, "OTA is stop.skip ota message[E5].");
                            firmwareUpdateBlockCmd.setStatus(StateCode.STATUS_FAIL);
                            mRCSPController.sendRcspResponse(device, firmwareUpdateBlockCmd);
                        }
                        break;
                    }
                    case Command.CMD_OTA_NOTIFY_UPDATE_CONTENT_SIZE: {
                        NotifyUpdateContentSizeCmd notifyUpdateContentSizeCmd = (NotifyUpdateContentSizeCmd) cmd;
                        JL_Log.e(TAG, "-onDeviceCommand- notifyUpdateContentSizeCmd : " + notifyUpdateContentSizeCmd);
                        if (isOTA) {
                            NotifyUpdateContentSizeParam param = notifyUpdateContentSizeCmd.getParam();
                            if (param != null && param.getContentSize() > 0) { //开始统计升级进度
                                mStartTime = new Date().getTime();
                                mCurrentSumFileSize = param.getCurrentProgress();
                                mUpdateContentSize = param.getContentSize();
                                callbackProgress(device, getCurrentProgress(mCurrentSumFileSize));
                                notifyUpdateContentSizeCmd.setStatus(StateCode.STATUS_SUCCESS);
                                notifyUpdateContentSizeCmd.setResponse(new CommonResponse());
                                mRCSPController.sendRcspResponse(device, notifyUpdateContentSizeCmd);
                            } else if (param != null && param.getContentSize() == 0) {
                                mCurrentSumFileSize = 0;
                                mUpdateContentSize = 0;
                                JL_Log.w(TAG, "-onDeviceCommand- notifyUpdateContentSizeCmd : length is 0.");
                            } else {
                                BaseError error = new BaseError(ERROR_PARAM_FAILED, AppUtil.getContext().getString(R.string.ota_error_msg_error_param));
                                error.setOpCode(Command.CMD_OTA_NOTIFY_UPDATE_CONTENT_SIZE);
                                callbackError(error);
                            }
                        } else {
                            JL_Log.w(TAG, "OTA is stop.skip ota message[E8].");
                            notifyUpdateContentSizeCmd.setStatus(StateCode.STATUS_FAIL);
                            mRCSPController.sendRcspResponse(device, notifyUpdateContentSizeCmd);
                        }
                        break;
                    }
                }
            }
        }
    };

    private void startReceiveCmdTimeout() {
        if (mainHandler != null && timeout_ms > 0) {
            mainHandler.removeCallbacks(mReceiveCmdTimeout);
            mainHandler.postDelayed(mReceiveCmdTimeout, timeout_ms);
        }
    }

    private void stopReceiveCmdTimeout() {
        if (mainHandler != null) {
            mainHandler.removeCallbacks(mReceiveCmdTimeout);
        }
    }

    private final Runnable mReceiveCmdTimeout = () -> callbackError(new BaseError(ERROR_RECEIVE_TIMEOUT, AppUtil.getContext().getString(R.string.ota_error_msg_receive_cmd_timeout)));


    private boolean isStartSumProgress() {
        return mUpdateContentSize > 0;
    }

    private float getCurrentProgress(int size) {
        float progress = 0;
        if (isStartSumProgress()) {
            progress = ((float) size * 100 / (float) mUpdateContentSize);
            if (progress >= 100) {
                progress = 99.9f;
            }
        }
        return progress;
    }
}
