package com.jieli.btsmart.ui.base;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.network.NetworkDetectionHelper;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.home.HomeActivity;
import com.jieli.component.ActivityManager;
import com.jieli.component.base.Jl_BaseActivity;
import com.jieli.component.utils.ToastUtil;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/8/11 7:44 PM
 * @desc :
 */
public class BaseActivity extends Jl_BaseActivity {
    protected final RCSPController mRCSPController = RCSPController.getInstance();
    protected final NetworkDetectionHelper mNetworkDetectionHelper = NetworkDetectionHelper.getInstance();

    private int callStatus = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRCSPController.addBTRcspEventCallback(btEventCallback);
        mNetworkDetectionHelper.addOnNetworkDetectionListener(mDetectionListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRCSPController.removeBTRcspEventCallback(btEventCallback);
        mNetworkDetectionHelper.removeOnNetworkDetectionListener(mDetectionListener);
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() != MotionEvent.ACTION_DOWN) {
            return super.dispatchTouchEvent(ev);
        }

        if (callStatus > 0) {
            ToastUtil.showToastShort(R.string.msg_call_tip);
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }


    private final BTRcspEventCallback btEventCallback = new BTRcspEventCallback() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
            callStatus = 0;
        }

        @Override
        public void onPhoneCallStatusChange(BluetoothDevice device, int status) {
            callStatus = status;
        }
    };

    private final NetworkDetectionHelper.OnNetworkDetectionListener mDetectionListener = (type, available) -> {
        if(available && type == ConnectivityManager.TYPE_MOBILE){
            Activity currentActivity = ActivityManager.getInstance().getCurrentActivity();
            if (currentActivity instanceof CommonActivity || currentActivity instanceof HomeActivity) {
                ToastUtil.showToastShort(getString(R.string.mobile_network_tip));
            }
        }
    };
}
