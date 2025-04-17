package com.jieli.btsmart.viewmodel;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

import static com.jieli.btsmart.constant.SConstant.ALLOW_SWITCH_FUN_DISCONNECT;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/22 9:42
 * @desc :
 */
public class FMTXControlViewModel extends BtBasicVM {
    private final String TAG = this.getClass().getSimpleName();
    //时刻刷新的FM频点
    public MutableLiveData<Integer> realTimeFMTxFreqLiveData = new MutableLiveData<>(875);
    //小机的FM频点
    public MutableLiveData<Integer> deviceFMTxFreqLiveData = new MutableLiveData<>();

    public FMTXControlViewModel(){
        mRCSPController.addBTRcspEventCallback(btEventCallback);
    }

    @Override
    protected void release() {
        mRCSPController.removeBTRcspEventCallback(btEventCallback);
        super.release();
    }

    /**
     * FM发射获取频点信息
     */
    public void requestFMTXFreq() {
        mRCSPController.getFmFrequency(getConnectedDevice(), null);
    }

    /**
     * 设置FM发射频点
     */
    public void setFMTXFreq(float freq) {
        mRCSPController.setFmFrequency(getConnectedDevice(), freq, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                deviceFMTxFreqLiveData.setValue((int) (freq * 10));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.e(TAG, "setFrequencyValue : onErrCode >> " + error);
                requestFMTXFreq();
            }
        });
        if (ALLOW_SWITCH_FUN_DISCONNECT && !mRCSPController.isDeviceConnected()) {
            btEventCallback.onFrequencyTx(getConnectedDevice(), freq);
        }
    }

    private final BTRcspEventCallback btEventCallback = new BTRcspEventCallback() {

        @Override
        public void onFrequencyTx(BluetoothDevice device, float frequency) {
            deviceFMTxFreqLiveData.setValue((int) (frequency * 10));
        }
    };
}
