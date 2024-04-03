package com.jieli.btsmart.tool.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.AttrBean;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.command.GetSysInfoCmd;
import com.jieli.bluetooth.bean.response.SysInfoResponse;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.bluetooth.CommandCallback;
import com.jieli.bluetooth.utils.JL_Log;

import java.util.List;

/**
 * 获取系统信息回调
 *
 * @author zqjasonZhong
 * @date 2019/10/29
 */
public class GetSysCommandCallback implements CommandCallback {
    private final CommandCallback mCallback;
    private final BTEventCallbackManager mCallbackManager;
    private final BluetoothDevice device;

    public GetSysCommandCallback(BTEventCallbackManager callbackManager, CommandCallback callback) {
        this.mCallback = callback;
        this.mCallbackManager = callbackManager;
        this.device = BluetoothHelper.getInstance().getConnectedDevice();
    }

    @Override
    public void onCommandResponse(CommandBase cmd) {
        if (mCallback != null) {
            mCallback.onCommandResponse(cmd);
        }
        if (cmd.getStatus() != StateCode.STATUS_SUCCESS) {
            return;
        }

        if (cmd.getId() == Command.CMD_GET_SYS_INFO) {
            GetSysInfoCmd getSysInfoCmd = (GetSysInfoCmd) cmd;
            if (getSysInfoCmd.getStatus() == StateCode.STATUS_SUCCESS) {
                SysInfoResponse sysInfoResponse = getSysInfoCmd.getResponse();
                List<AttrBean> list = sysInfoResponse.getAttrs();
                if (mCallbackManager != null) {
                    mCallbackManager.parseAttrMessage(device, sysInfoResponse.getFunction(), list);
                }
            } else {
                JL_Log.w("sen", cmd.toString());
            }
        }
    }

    @Override
    public void onErrCode(BaseError error) {
        if (mCallback != null) {
            mCallback.onErrCode(error);
        }
    }
}
