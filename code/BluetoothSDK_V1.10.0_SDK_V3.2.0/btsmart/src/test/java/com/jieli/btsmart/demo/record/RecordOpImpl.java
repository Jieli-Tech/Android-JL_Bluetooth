package com.jieli.btsmart.demo.record;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.command.speech.StartSpeechCmd;
import com.jieli.bluetooth.bean.command.speech.StopSpeechCmd;
import com.jieli.bluetooth.bean.parameter.StartSpeechParam;
import com.jieli.bluetooth.bean.parameter.StopSpeechParam;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.BooleanRcspActionCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.demo.record.callback.OnRecordStateCallback;
import com.jieli.btsmart.demo.record.model.RecordParam;
import com.jieli.btsmart.demo.record.model.RecordState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc
 * @since 2023/2/6
 */
public class RecordOpImpl implements IRecordOp {
    private final String tag = RecordOpImpl.class.getSimpleName();

    private final RCSPController mRCSPController;
    private final OnRecordStateCallback mCallback;
    private final RecordState mRecordState;  //录音状态

    public RecordOpImpl(RCSPController controller, OnRecordStateCallback callback) {
        mRCSPController = controller;
        mCallback = callback;
        mRecordState = new RecordState();
        mRCSPController.addBTRcspEventCallback(mBTRcspEventCallback);
    }

    @Override
    public void startRecord(BluetoothDevice device, RecordParam param, OnRcspActionCallback<Boolean> callback) {
        if (null == param) { //参数为空
            if (null != callback)
                callback.onError(device, new BaseError(ErrorCode.SUB_ERR_PARAMETER, "RecordParam can not be null."));
            return;
        }
        if (isRecording()) { //正在录音
            if (null != callback)
                callback.onError(device, new BaseError(ErrorCode.SUB_ERR_OP_FAILED, "It is Recording!"));
            return;
        }
        cbRecordStart(device, param);
        mRCSPController.sendRcspCommand(device, new StartSpeechCmd(new StartSpeechParam(CHexConver.intToByte(param.getVoiceType()),
                        CHexConver.intToByte(param.getSampleRate()), CHexConver.intToByte(param.getVadWay()))),
                new BooleanRcspActionCallback(new OnRcspActionCallback<Boolean>() {
                    @Override
                    public void onSuccess(BluetoothDevice device, Boolean message) {
                        if (callback != null) callback.onSuccess(device, message);
                    }

                    @Override
                    public void onError(BluetoothDevice device, BaseError error) {
                        if (callback != null) callback.onError(device, error);
                        cbRecordError(device, error.getSubCode(), error.getMessage());
                    }
                }));
    }

    @Override
    public void stopRecord(BluetoothDevice device, int reason, OnRcspActionCallback<Boolean> callback) {
        if (!isRecording()) { //不在录音状态
            if (null != callback)
                callback.onError(device, new BaseError(ErrorCode.SUB_ERR_OP_FAILED, "Recording is not started."));
            return;
        }
        StopSpeechParam param = new StopSpeechParam();
        param.setReason(CHexConver.intToByte(reason));
        mRCSPController.sendRcspCommand(device, new StopSpeechCmd(param), new BooleanRcspActionCallback(new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                if (callback != null) callback.onSuccess(device, message);
                cbRecordStop(device, reason);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (callback != null) callback.onError(device, error);
                cbRecordError(device, error.getSubCode(), error.getMessage());
            }
        }));
    }

    public void destroy() {
        mRecordState.setState(RecordState.RECORD_STATE_IDLE)
                .setRecordParam(null)
                .setVoiceData(null)
                .setVoiceDataBlock(null)
                .setReason(RecordState.REASON_NORMAL)
                .setMessage("");
        mRCSPController.removeBTRcspEventCallback(mBTRcspEventCallback);
    }

    private boolean isRecording() {
        return mRecordState.getState() != RecordState.RECORD_STATE_IDLE;
    }

    private void cbRecordStart(BluetoothDevice device, RecordParam param) {
        if (null == param || isRecording()) {
            JL_Log.w(tag, "cbRecordStart : RecordParam is null or state error. " + mRecordState.getState());
            return;
        }
        mRecordState.setState(RecordState.RECORD_STATE_START)
                .setRecordParam(param)
                .setVoiceData(new byte[0]);
        if (null != mCallback) mCallback.onStateChange(device, mRecordState);
    }

    private void cbRecordWorking(BluetoothDevice device, byte[] data) {
        if (data == null || data.length == 0 || !isRecording()) {
            JL_Log.w(tag, "cbRecordWorking : data is null or state error. " + mRecordState.getState());
            return;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            if (mRecordState.getVoiceData() != null && mRecordState.getVoiceData().length > 0) {
                outputStream.write(mRecordState.getVoiceData());
            }
            outputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecordState.setState(RecordState.RECORD_STATE_WORKING)
                .setVoiceDataBlock(data)
                .setVoiceData(outputStream.toByteArray());
        if (null != mCallback) mCallback.onStateChange(device, mRecordState);
    }

    private void cbRecordStop(BluetoothDevice device, int reason) {
        if (!isRecording()) {
            JL_Log.w(tag, "cbRecordStop : state error. " + mRecordState.getState() + ", reason = " + reason);
            return;
        }
        mRecordState.setState(RecordState.RECORD_STATE_IDLE)
                .setVoiceDataBlock(new byte[0])
                .setReason(reason);
        if (null != mCallback) mCallback.onStateChange(device, mRecordState);
    }

    private void cbRecordError(BluetoothDevice device, int code, String explain) {
        if (!isRecording()) {
            JL_Log.w(tag, "cbRecordError : state error. " + mRecordState.getState() + ", code = " + code + ", explain = " + explain);
            return;
        }
        mRecordState.setState(RecordState.RECORD_STATE_IDLE)
                .setVoiceDataBlock(new byte[0])
                .setVoiceData(new byte[0])
                .setReason(code)
                .setMessage(String.format(Locale.getDefault(), "Code: %d, Error: %s", code, explain));
        if (null != mCallback) mCallback.onStateChange(device, mRecordState);
    }

    private final BTRcspEventCallback mBTRcspEventCallback = new BTRcspEventCallback() {
        @Override
        public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
            super.onDeviceCommand(device, cmd);
            if (null == cmd) return;
            switch (cmd.getId()) {
                case Command.CMD_RECEIVE_SPEECH_START: {
                    if (!isRecording()) {
                        StartSpeechCmd startSpeechCmd = (StartSpeechCmd) cmd;
                        StartSpeechParam param = startSpeechCmd.getParam();
                        if (null == param) return;
                        cbRecordStart(device, new RecordParam(param.getType(), param.getFreq(), param.getWay()));
                        startSpeechCmd.setParam(null);
                        startSpeechCmd.setStatus(StateCode.STATUS_SUCCESS);
                        mRCSPController.sendRcspResponse(device, startSpeechCmd);
                    }
                    break;
                }
                case Command.CMD_RECEIVE_SPEECH_STOP: {
                    if (isRecording()) {
                        StopSpeechCmd stopSpeechCmd = (StopSpeechCmd) cmd;
                        StopSpeechParam param = stopSpeechCmd.getParam();
                        if (null == param) return;
                        cbRecordStop(device, param.getReason());
                        stopSpeechCmd.setParam(null);
                        stopSpeechCmd.setStatus(StateCode.STATUS_SUCCESS);
                        mRCSPController.sendRcspResponse(device, stopSpeechCmd);
                    }
                    break;
                }
            }
        }

        @Override
        public void onDeviceVoiceData(BluetoothDevice device, byte[] data) {
            super.onDeviceVoiceData(device, data);
            if (isRecording()) {
                cbRecordWorking(device, data);
            }
        }
    };
}
