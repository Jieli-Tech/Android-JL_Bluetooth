package com.jieli.btsmart.tool.bluetooth.rcsp;

import android.bluetooth.BluetoothDevice;

import androidx.fragment.app.FragmentActivity;

import com.jieli.bluetooth.bean.device.alarm.AlarmBean;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.component.ActivityManager;
import com.jieli.jl_dialog.Jl_Dialog;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 闹钟操作处理器
 * @since 2021/12/13
 */
public class AlarmHandler extends BTRcspEventCallback {
    private final String tag = AlarmHandler.class.getSimpleName();
    private final RCSPController mRCSPController;
    private BluetoothDevice mDevice;

    public AlarmHandler(RCSPController controller){
        mRCSPController = controller;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    @Override
    public void onConnection(BluetoothDevice device, int status) {
        if (BluetoothUtil.deviceEquals(device, mDevice)) {
            setDevice(null);
            dismissNotifyDialog();
        }
    }

    @Override
    public void onAlarmNotify(BluetoothDevice device, AlarmBean alarmBean) {
        setDevice(device);
        showNotifyDialog();
    }

    @Override
    public void onAlarmStop(BluetoothDevice device, AlarmBean alarmBean) {
        setDevice(null);
        dismissNotifyDialog();
    }

    private void setDevice(BluetoothDevice device) {
        mDevice = device;
    }

    private void showNotifyDialog() {
        FragmentActivity activity = (FragmentActivity) ActivityManager.getInstance().getCurrentActivity();
        if (activity != null) {
            if (activity.getSupportFragmentManager().findFragmentByTag("alarm_notify") != null) {
                //如果对话框已显示则返回
                JL_Log.i(tag, "alarm dialog is showing");
                return;
            }
            Jl_Dialog jl_dialog = new Jl_Dialog.Builder()
                    .title(activity.getString(R.string.tips))
                    .content(activity.getString(R.string.alarm_running))
                    .right(activity.getString(R.string.confirm))
                    .width(0.8f)
                    .cancel(false)
                    .rightClickListener((v, dialogFragment) -> {
                        mRCSPController.stopAlarmBell(getDevice(), null);
                        dialogFragment.dismiss();
                    })
                    .build();
            jl_dialog.show(activity.getSupportFragmentManager(), "alarm_notify");
        }
    }

    private void dismissNotifyDialog() {
        FragmentActivity activity = (FragmentActivity) ActivityManager.getInstance().getCurrentActivity();
        if (activity != null && activity.getSupportFragmentManager().findFragmentByTag("alarm_notify") != null) {
            Jl_Dialog dialog = (Jl_Dialog) activity.getSupportFragmentManager().findFragmentByTag("alarm_notify");
            if (dialog != null && dialog.isShow()) {
                dialog.dismiss();
            }
        }
    }
}
