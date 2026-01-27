package com.jieli.btsmart.ui.auracast.transmitter;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.bean.auracast.transmitter.EncryptionSettings;
import com.jieli.bluetooth.bean.auracast.transmitter.TransmitterSettings;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.command.auracast.response.SetResult;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.interfaces.rcsp.auracast.transmitter.OnAuracastTransmitterCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.basic.OpResult;
import com.jieli.btsmart.tool.configure.ConfigureKit;
import com.jieli.btsmart.ui.auracast.AuracastAssistantViewModel;

import java.util.List;

/**
 * AuracastTransmitterViewModel
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/20
 * note: Auracast发射端逻辑实现
 */
public class AuracastTransmitterViewModel extends AuracastAssistantViewModel {

    /**
     * 回调登录状态
     */
    public final MutableLiveData<Boolean> loginStateMLD = new MutableLiveData<>();

    /**
     * 回调发射端设置信息
     */
    public final MutableLiveData<TransmitterSettings> transmitterSettingsMLD = new MutableLiveData<>();

    /**
     * 配置是否被更改
     */
    public boolean isSettingsChange;
    /**
     * 是否自动登录
     */
    private boolean isAutoLogin;

    public AuracastTransmitterViewModel(BluetoothDevice device) {
        super(device);
        auracastAssistant.addOnAuracastTransmitterCallback(auracastTransmitterCallback);
    }

    @Override
    public void release() {
        isSettingsChange = false;
        isAutoLogin = false;
        auracastAssistant.removeOnAuracastTransmitterCallback(auracastTransmitterCallback);
        super.release();
    }

    public boolean isLogin() {
        return auracastAssistant.isLogin();
    }

    public void login(String password) {
        auracastAssistant.login(password, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                if (message == Constants.RESULT_OK || message == Constants.RESULT_ALREADY_LOGGED_IN) {
                    final boolean autoLogin = isAutoLogin;
                    if (isAutoLogin) {
                        isAutoLogin = false;
                        JL_Log.i(tag, "login", "mac : " + getMac() + ", Automatic login successful.");
                    }
                    ConfigureKit.getInstance().saveAuracastLoginPassword(getMac(), password);
                    opResultMLD.postValue(new OpResult<>(OP_LOGIN)
                            .setCode(OpResult.RES_SUCCESS)
                            .setData(autoLogin));
                    return;
                }
                if (isAutoLogin && message == Constants.RESULT_BAD_PASSWORD) {
                    //密码被修改了，清除记录的密码
                    ConfigureKit.getInstance().deleteLoginPassword(getMac());
                }
                onError(device, new BaseError(ErrorCode.SUB_ERR_OP_FAILED, "Failed to login. reason : " + message)
                        .setReason(message));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                final boolean autoLogin = isAutoLogin;
                if (isAutoLogin) {
                    isAutoLogin = false;
                }
                opResultMLD.postValue(new OpResult<>(OP_LOGIN)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage())
                        .setData(autoLogin));
            }
        });
    }

    public void autoLogin() {
        String password = ConfigureKit.getInstance().getAuracastLoginPassword(getMac());
        if (ConfigureKit.isValidPassword(password)) {
            isAutoLogin = true;
            login(password);
        }
    }

    public void modifyLoginPwd(String newPwd) {
        String password = ConfigureKit.getInstance().getAuracastLoginPassword(getMac());
        if (!ConfigureKit.isValidPassword(password)) {
            opResultMLD.postValue(new OpResult<>(OP_MODIFY_LOGIN_PWD)
                    .setCode(ErrorCode.SUB_ERR_PARAMETER)
                    .setMessage("Invalid Old Password."));
            return;
        }
        auracastAssistant.modifyLoginPassword(password, newPwd, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                if (message == Constants.RESULT_OK) {
                    //重置密码成功，清除记录的登录密码
                    ConfigureKit.getInstance().deleteLoginPassword(getMac());
                    opResultMLD.postValue(new OpResult<>(OP_MODIFY_LOGIN_PWD)
                            .setCode(OpResult.RES_SUCCESS));
                    return;
                }
                onError(device, new BaseError(ErrorCode.SUB_ERR_OP_FAILED, "Failed to modify login password. reason : " + message)
                        .setReason(message));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                opResultMLD.postValue(new OpResult<>(OP_MODIFY_LOGIN_PWD)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public boolean isEncryption() {
        final TransmitterSettings settings = getSettingsInfo();
        if (null == settings) return false;
        return settings.isEncryption();
    }

    public TransmitterSettings getSettingsInfo() {
        return auracastAssistant.getTransmitterSettings();
    }

    public void readSettingsInfo() {
        readSettingsInfo(TransmitterSettings.TYPE_BROADCAST_NAME,
                TransmitterSettings.TYPE_AUDIO_FORMAT,
                TransmitterSettings.TYPE_ENCRYPTION_SETTINGS,
                TransmitterSettings.TYPE_TRANSMIT_POWER);
    }

    public void setBroadcastName(String name) {
        if (null == name || name.isEmpty()) return;
        auracastAssistant.setConfiguration(new TransmitterSettings()
                .setBroadcastName(name), new SetResultCallback(OP_SET_BROADCAST_NAME));
    }

    public void setEncryptionSettings(EncryptionSettings settings) {
        if (null == settings) return;
        auracastAssistant.setConfiguration(new TransmitterSettings()
                .setEncryptionSettings(settings), new SetResultCallback(OP_SET_ENCRYPTION_SETTINGS));
    }

    public void setAudioFormatID(int id) {
        auracastAssistant.setConfiguration(new TransmitterSettings()
                .setAudioFormat(id), new SetResultCallback(OP_SET_AUDIO_FORMAT));
    }

    public void adjustTransmitPower(int power) {
        auracastAssistant.setConfiguration(new TransmitterSettings()
                .setTransmitPower(power), new SetResultCallback(OP_SET_TRANSMIT_POWER));
    }

    public void reboot() {
        auracastAssistant.reboot(new OpResultCallback<>(OP_REBOOT));
    }

    public void disconnectDevice() {
        mRCSPController.disconnectDevice(getDevice());
    }

    private void readSettingsInfo(int... type) {
        auracastAssistant.readConfiguration(type, new OnRcspActionCallback<TransmitterSettings>() {
            @Override
            public void onSuccess(BluetoothDevice device, TransmitterSettings message) {
                opResultMLD.postValue(new OpResult<>(OP_READ_TRANSMITTER_SETTINGS)
                        .setCode(OpResult.RES_SUCCESS));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                opResultMLD.postValue(new OpResult<>(OP_READ_TRANSMITTER_SETTINGS)
                        .setCode(error.getSubCode()).setMessage(error.getMessage()));
            }
        });
    }

    private class SetResultCallback implements OnRcspActionCallback<List<SetResult>> {

        /**
         * 操作码
         */
        private final int op;

        public SetResultCallback(int op) {
            this.op = op;
        }

        @Override
        public void onSuccess(BluetoothDevice device, List<SetResult> message) {
            int type;
            switch (op) {
                case OP_SET_BROADCAST_NAME:
                    type = TransmitterSettings.TYPE_BROADCAST_NAME;
                    break;
                case OP_SET_AUDIO_FORMAT:
                    type = TransmitterSettings.TYPE_AUDIO_FORMAT;
                    break;
                case OP_SET_ENCRYPTION_SETTINGS:
                    type = TransmitterSettings.TYPE_ENCRYPTION_SETTINGS;
                    break;
                case OP_SET_TRANSMIT_POWER:
                    type = TransmitterSettings.TYPE_TRANSMIT_POWER;
                    break;
                default:
                    type = TransmitterSettings.TYPE_UNKNOWN;
                    break;
            }
            if (message != null) {
                for (SetResult result : message) {
                    if (result.getType() == type) {
                        if (result.getResult() == Constants.RESULT_OK) {
                            isSettingsChange = true;
                            opResultMLD.postValue(new OpResult<>(op)
                                    .setCode(OpResult.RES_SUCCESS));
                            if (type != TransmitterSettings.TYPE_UNKNOWN) {
                                readSettingsInfo();
                            }
                        }
                        break;
                    }
                }
            }

            onError(device, new BaseError(ErrorCode.SUB_ERR_OP_FAILED, CommonUtil.formatString("%s. reason : %s",
                    getContext().getString(R.string.operation_failed, getOpString(op)), message)));
        }

        @Override
        public void onError(BluetoothDevice device, BaseError error) {
            if (null == error) return;
            opResultMLD.postValue(new OpResult<>(op)
                    .setCode(error.getSubCode()).setMessage(error.getMessage()));
        }
    }

    private final OnAuracastTransmitterCallback auracastTransmitterCallback = new OnAuracastTransmitterCallback() {
        @Override
        public void onLoginChange(BluetoothDevice device, boolean isLogin) {
            if (!BluetoothUtil.deviceEquals(device, getDevice())) return;
            loginStateMLD.postValue(isLogin);
        }

        @Override
        public void onTransmitterSettings(BluetoothDevice device, int[] types, @NonNull TransmitterSettings settings) {
            if (!BluetoothUtil.deviceEquals(device, getDevice())) return;
            transmitterSettingsMLD.postValue(settings);
        }
    };

    public static class Factory implements ViewModelProvider.Factory {
        private final BluetoothDevice device;

        public Factory(BluetoothDevice device) {
            this.device = device;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new AuracastTransmitterViewModel(device);
        }
    }
}