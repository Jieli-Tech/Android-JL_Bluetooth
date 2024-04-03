package com.jieli.btsmart.demo.record;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.btsmart.demo.record.model.RecordParam;
import com.jieli.btsmart.demo.record.model.RecordState;

import org.junit.Test;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc
 * @since 2023/2/6
 */
class RecordDemo {

    private RecordOpImpl mRecordOp;

    @Test
    public void init() {
        mRecordOp = new RecordOpImpl(RCSPController.getInstance(), (device, state) -> {
            switch (state.getState()) {
                case RecordState.RECORD_STATE_IDLE:  //空闲或者结束
                    int reason = state.getReason();  //结束原因
                    break;
                case RecordState.RECORD_STATE_START: //开始录音
                    RecordParam param = state.getRecordParam(); //录音参数
                    break;
                case RecordState.RECORD_STATE_WORKING: //正在录音
                    RecordParam recordParam = state.getRecordParam(); //录音参数
                    byte[] data = state.getVoiceDataBlock(); //录音数据
                    break;
            }
        });
    }

    @Test
    public void startRecord(@NonNull RecordParam param) {
        if (null == mRecordOp) {
            System.out.println("还没有初始化！！！");
            return;
        }
        mRecordOp.startRecord(RCSPController.getInstance().getUsingDevice(), param, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
            }
        });
    }

    @Test
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
            }
        });
    }

    @Test
    public void release() {
        if (null != mRecordOp) {
            mRecordOp.destroy();
            mRecordOp = null;
        }
    }
}
