package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.record.RecordParam;
import com.jieli.bluetooth.bean.record.RecordState;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.impl.rcsp.record.RecordOpImpl;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.interfaces.rcsp.record.OnRecordStateCallback;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 录音示例代码
 * @since 2023/2/6
 */
public class RecordDemo {

    private RecordOpImpl mRecordOp;

    private final OnRecordStateCallback recordStateCallback = (device, state) -> {
        switch (state.getState()) {
            case RecordState.RECORD_STATE_IDLE:  //空闲或者结束
                int reason = state.getReason();  //结束原因
                if (reason == RecordState.REASON_NORMAL) { //录音完成
                    byte[] voiceData = state.getVoiceData(); //录音全部数据
                } else { //录音失败，错误码

                }
                break;
            case RecordState.RECORD_STATE_START: //开始录音
                RecordParam param = state.getRecordParam(); //录音参数
                //param.getVoiceType(); //数据格式
                //param.getSampleRate(); //采样率
                //param.getVadWay();   //断句方
                break;
            case RecordState.RECORD_STATE_WORKING: //正在录音
                RecordParam recordParam = state.getRecordParam(); //录音参数
                byte[] data = state.getVoiceDataBlock(); //录音数据
                break;
        }
    };

    public void init() {
        //获取录音功能操作对象
        mRecordOp = RecordOpImpl.getInstance(RCSPController.getInstance().getRcspOp());
        //注册录音事件回调
        mRecordOp.addOnRecordStateCallback(recordStateCallback);
    }

    public void startRecord() {
        if (null == mRecordOp) {
            System.out.println("还没有初始化！！！");
            return;
        }
        BluetoothDevice device = RCSPController.getInstance().getUsingDevice();
        if(mRecordOp.isRecording(device)){
            System.out.println("正在录音中");
            return;
        }
        //录音参数
        //voiceType --- 数据格式
        //sampleRate --- 采样率
        //vadWay --- 断句方
        RecordParam param = new RecordParam(RecordParam.VOICE_TYPE_OPUS, RecordParam.SAMPLE_RATE_16K, RecordParam.VAD_WAY_DEVICE);
        //执行录音功能
        mRecordOp.startRecord(device, param, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                //error --- 错误信息
            }
        });
    }

    public void stopRecord() {
        if (null == mRecordOp) {
            System.out.println("还没有初始化！！！");
            return;
        }
        mRecordOp.stopRecord(RCSPController.getInstance().getUsingDevice(), RecordState.REASON_NORMAL, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                //error --- 错误信息
            }
        });
    }


    public void release() {
        if (null != mRecordOp) {
            mRecordOp.removeOnRecordStateCallback(recordStateCallback);
            mRecordOp.destroy();
            mRecordOp = null;
        }
    }
}
