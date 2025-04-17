package com.jieli.btsmart.ui.test;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.command.custom.CustomCmd;
import com.jieli.bluetooth.bean.parameter.CustomParam;
import com.jieli.bluetooth.bean.response.CustomResponse;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.RcspCommandCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.bluetooth.utils.CommandBuilder;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.ActivityTestBinding;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.base.Jl_BaseActivity;
import com.jieli.component.utils.ToastUtil;

public class TestCustomCmdActivity extends Jl_BaseActivity {
    private static final String TAG = TestCustomCmdActivity.class.getSimpleName();
    private ActivityTestBinding mBinding;

    //初始化RCSP控制器
    private final RCSPController mRCSPController = RCSPController.getInstance();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityTestBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        if (!mRCSPController.isDeviceConnected()) {
            ToastUtil.showToastShort(getString(R.string.first_connect_device));
            finish();
            return;
        }
        initUI();
        //监听RCSP监听器的事件
        mRCSPController.addBTRcspEventCallback(mBtEventCallback);
    }

    private void initUI() {
        mBinding.tvRecv.setMovementMethod(ScrollingMovementMethod.getInstance());
        mBinding.ivClearLog.setOnClickListener(v -> addLog("", false));
        mBinding.btnSendCustomCmd.setOnClickListener(v -> sendCustomCmd());
    }


    private void sendCustomCmd() {
        String customData = mBinding.etCustomData.getText().toString().trim();
        if (TextUtils.isEmpty(customData)) {
            mBinding.etCustomData.setError("自定义数据不允许为空");
            return;
        }
        //发送自定命令
        final byte[] data = CHexConver.hexStr2Bytes(customData);//自定义命令的附带数据
        CommandBase commandBase = CommandBuilder.buildCustomCmd(data);
        mRCSPController.sendCommandAsync(mRCSPController.getUsingDevice(), commandBase, 3000, new RcspCommandCallback() {
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                if (cmd.getStatus() != StateCode.STATUS_SUCCESS) { //固件回复失败状态
                    BaseError error = new BaseError(ErrorCode.SUB_ERR_RESPONSE_BAD_STATUS, "Device response an bad status : " + cmd.getStatus());
                    error.setOpCode(Command.CMD_EXTRA_CUSTOM);
                    onErrCode(device, error);
                    return;
                }
                //发送成功回调, 需要回复设备
                CustomCmd customCmd = (CustomCmd) cmd;
                CustomResponse response = customCmd.getResponse();
                if (null == response) {
                    addLog(AppUtil.formatString("发送自定义数据[%s]成功,没有回复数据!", CHexConver.byte2HexStr(data)));
                    return;
                }
                byte[] responseData = response.getData(); //自定义回复数据
                addLog(AppUtil.formatString("发送自定义数据[%s]成功！！！\n设备回复数据:[%s]",
                        CHexConver.byte2HexStr(data), CHexConver.byte2HexStr(responseData)));
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                addLog(AppUtil.formatString("发送自定义数据[%s]失败！！！\n错误原因: %s", CHexConver.byte2HexStr(data), error));
            }
        });
    }

    private void addLog(String text) {
        addLog(text, true);
    }

    private void addLog(String text, boolean isAppend) {
        if (isDestroyed() || null == text) return;
        JL_Log.d(TAG, "addLog:" + text);
        if (!isAppend) {
            mBinding.tvRecv.setText(text);
            mBinding.tvRecv.scrollTo(0, 0);
        } else {
            mBinding.tvRecv.append(text);
            mBinding.tvRecv.append("\n");
            int offset = getTextViewHeight(mBinding.tvRecv);
            if (offset > mBinding.tvRecv.getHeight()) {
                mBinding.tvRecv.scrollTo(0, offset - mBinding.tvRecv.getHeight());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRCSPController.removeBTRcspEventCallback(mBtEventCallback);
    }

    public static int getTextViewHeight(@NonNull TextView textView) {
        Layout layout = textView.getLayout();
        if (null == layout) return 0;
        int desired = layout.getLineTop(textView.getLineCount());
        int padding = textView.getCompoundPaddingTop() + textView.getCompoundPaddingBottom();
        return desired + padding;
    }

    private final BTRcspEventCallback mBtEventCallback = new BTRcspEventCallback() {

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (status != StateCode.CONNECTION_OK) {
                TestCustomCmdActivity.this.finish();
            }
        }

        //接收自定义命令
        @Override
        public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
            if (cmd.getId() == Command.CMD_EXTRA_CUSTOM) {
                JL_Log.e(TAG, "收到命令：" + cmd);
                //处理固件向App发送的自定义命令。
                CustomCmd customCmd = (CustomCmd) cmd;
                CustomParam param = customCmd.getParam();
                boolean isNeedResponse = cmd.getType() == CommandBase.FLAG_HAVE_PARAMETER_AND_RESPONSE
                        || cmd.getType() == CommandBase.FLAG_NO_PARAMETER_AND_RESPONSE;
                if (null == param) {
                    if (isNeedResponse) { //需要回复
                        byte[] responseData = new byte[0]; //可以设置回复的数据
                        customCmd.setParam(new CustomParam(responseData));
                        customCmd.setStatus(StateCode.STATUS_SUCCESS);
                        mRCSPController.sendCommandResponse(device, customCmd, null); //发送命令回复
                    }
                    addLog("接收到设备发送的自定义数据: 无参数。错误的命令数据格式");
                    return;
                }
                byte[] data = param.getData(); //自定义数据
                //parseCustomData(data);
                addLog(AppUtil.formatString("接收到设备发送的自定义数据:\n[%s]", CHexConver.byte2HexStr(data)));
                if (isNeedResponse) { //需要回复
                    byte[] responseData = new byte[0]; //可以设置回复的数据
                    if (responseData.length > 0) {
                        addLog(AppUtil.formatString("回复设备的自定义数据:\n[%s],\n回复数据:[%s]",
                                CHexConver.byte2HexStr(data), CHexConver.byte2HexStr(responseData)));
                    }
                    customCmd.setParam(new CustomParam(responseData));
                    customCmd.setStatus(StateCode.STATUS_SUCCESS);
                    mRCSPController.sendCommandResponse(device, customCmd, null); //发送命令回复
                }
            }
        }
    };
}
