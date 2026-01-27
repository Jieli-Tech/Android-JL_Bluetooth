package com.jieli.btsmart.ui.auracast;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.impl.rcsp.auracast.AuracastAssistant;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.basic.OpResult;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

/**
 * AuracastAssistantViewModel
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/19
 * note: Auracast助手逻辑实现
 */
public class AuracastAssistantViewModel extends BtBasicVM {

    /// 接收端操作码

    /**
     * 开始搜索操作
     */
    public static final int OP_START_SCAN = 1;

    /**
     * 停止搜索操作
     */
    public static final int OP_STOP_SCAN = 2;

    /**
     * 添加音源操作
     */
    public static final int OP_ADD_SOURCE = 3;

    /**
     * 修改音源参数操作
     */
    public static final int OP_MODIFY_SOURCE = 4;

    /**
     * 移除音源操作
     */
    public static final int OP_REMOVE_SOURCE = 5;

    /**
     * 同步广播音源
     */
    public static final int OP_SYNC_BROADCAST = 6;

    /**
     * 同步广播状态
     */
    public static final int OP_SYNC_BROADCAST_STATE = 7;

    /// 发射端操作码

    /**
     * 登录操作
     */
    public static final int OP_LOGIN = 40;

    /**
     * 修改登录密码
     */
    public static final int OP_MODIFY_LOGIN_PWD = 41;

    /**
     * 读取发射端配置信息
     */
    public static final int OP_READ_TRANSMITTER_SETTINGS = 42;

    /**
     * 设置广播名称
     */
    public static final int OP_SET_BROADCAST_NAME = 43;

    /**
     * 设置音频格式
     */
    public static final int OP_SET_AUDIO_FORMAT = 44;

    /**
     * 设置加密设置
     */
    public static final int OP_SET_ENCRYPTION_SETTINGS = 45;

    /**
     * 设置发射功率
     */
    public static final int OP_SET_TRANSMIT_POWER = 46;

    /**
     * 重启设备
     */
    public static final int OP_REBOOT = 47;

    /**
     * Auracast助手功能实现
     */
    protected final AuracastAssistant auracastAssistant;

    /**
     * 操作结果回调
     */
    public final MutableLiveData<OpResult<Object>> opResultMLD = new MutableLiveData<>();

    public AuracastAssistantViewModel(BluetoothDevice device) {
        auracastAssistant = new AuracastAssistant(getContext(), mRCSPController.getRcspOp(), device);
    }


    @Override
    public void release() {
        auracastAssistant.destroy();
        super.release();
    }

    public boolean isSupportAuracastReceiver() {
        return auracastAssistant.isSupportAuracastReceiver();
    }

    public boolean isSupportAuracastTransmitter() {
        return auracastAssistant.isSupportAuracastTransmitter();
    }

    public BluetoothDevice getDevice() {
        return auracastAssistant.getDevice();
    }

    public String getOpString(int op) {
        switch (op) {
            case OP_START_SCAN:
                return getContext().getString(R.string.op_start_scan);
            case OP_STOP_SCAN:
                return getContext().getString(R.string.op_stop_scan);
            case OP_ADD_SOURCE:
                return getContext().getString(R.string.op_add_source);
            case OP_MODIFY_SOURCE:
                return getContext().getString(R.string.op_modify_source);
            case OP_REMOVE_SOURCE:
                return getContext().getString(R.string.op_remove_source);
            case OP_SYNC_BROADCAST:
                return getContext().getString(R.string.op_resynchronize_broadcast);
            case OP_SYNC_BROADCAST_STATE:
                return getContext().getString(R.string.op_sync_broadcast_state);

            case OP_LOGIN:
                return getContext().getString(R.string.op_login);
            case OP_MODIFY_LOGIN_PWD:
                return getContext().getString(R.string.op_modify_login_pwd);
            case OP_READ_TRANSMITTER_SETTINGS:
                return getContext().getString(R.string.op_read_transmitter_settings);
            case OP_SET_BROADCAST_NAME:
                return getContext().getString(R.string.op_set_broadcast_name);
            case OP_SET_AUDIO_FORMAT:
                return getContext().getString(R.string.op_set_audio_format);
            case OP_SET_ENCRYPTION_SETTINGS:
                return getContext().getString(R.string.op_set_encryption_settings);
            case OP_SET_TRANSMIT_POWER:
                return getContext().getString(R.string.op_set_transmit_power);
            case OP_REBOOT:
                return getContext().getString(R.string.op_reboot);
            default:
                return "Unknown Operation : " + CommonUtil.formatInt(op);
        }
    }

    public String getMac() {
        BluetoothDevice device = getDevice();
        if (null == device) return "";
        return device.getAddress();
    }

    protected class OpResultCallback<T> implements OnRcspActionCallback<Boolean> {
        private final int op;
        private final T data;

        public OpResultCallback(int op) {
            this(op, null);
        }

        public OpResultCallback(int op, T data) {
            this.op = op;
            this.data = data;
        }

        @Override
        public void onSuccess(BluetoothDevice device, Boolean message) {
            opResultMLD.postValue(new OpResult<>(op)
                    .setCode(OpResult.RES_SUCCESS)
                    .setData(data));
        }

        @Override
        public void onError(BluetoothDevice device, BaseError error) {
            if (null == error) return;
            opResultMLD.postValue(new OpResult<>(op)
                    .setCode(error.getSubCode())
                    .setMessage(error.getMessage()));
        }
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final BluetoothDevice device;

        public Factory(BluetoothDevice device) {
            this.device = device;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new AuracastAssistantViewModel(device);
        }
    }
}
