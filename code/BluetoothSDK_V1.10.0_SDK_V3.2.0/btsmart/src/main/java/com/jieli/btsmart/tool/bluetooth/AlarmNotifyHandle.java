package com.jieli.btsmart.tool.bluetooth;

import androidx.fragment.app.FragmentActivity;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.bluetooth.AlarmBean;
import com.jieli.btsmart.data.model.bluetooth.AlarmListInfo;
import com.jieli.component.ActivityManager;
import com.jieli.jl_dialog.Jl_Dialog;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/7/7 9:30 AM
 * @desc :硬件闹钟
 */
public class AlarmNotifyHandle extends BTEventCallback {

    private final static String TAG = "AlarmNotifyHandle";
    private static AlarmNotifyHandle instance = null;

    private AlarmNotifyHandle() {
    }


    public static AlarmNotifyHandle getInstance() {
        if (instance == null) {
            synchronized (AlarmNotifyHandle.class) {
                instance = new AlarmNotifyHandle();
            }
        }
        return instance;
    }

    @Override
    public void onAlarmNotify(AlarmListInfo alarmListInfo) {
        super.onAlarmNotify(alarmListInfo);
        for (AlarmBean bean : alarmListInfo.getAlarmBeans()) {
            JL_Log.e(TAG, "onAlarm notify info-->" + bean);
        }
    }

    @Override
    public void onAlarmNotify(AlarmBean alarmBean) {
        super.onAlarmNotify(alarmBean);
        showNotifyDialog();
    }

    private void showNotifyDialog() {
        FragmentActivity activity = (FragmentActivity) ActivityManager.getInstance().getCurrentActivity();
        if (activity != null) {
            if (activity.getSupportFragmentManager().findFragmentByTag("alarm_notify") != null) {
                //如果对话框已显示则返回
                JL_Log.i("showNotifyDialog", "alarm dialog is showing");
                return;
            }
            Jl_Dialog jl_dialog = new Jl_Dialog.Builder()
                    .title(activity.getString(R.string.tips))
                    .content(activity.getString(R.string.alarm_running))
                    .right(activity.getString(R.string.confirm))
                    .width(0.8f)
                    .cancel(false)
                    .rightClickListener((v, dialogFragment) -> {
                        BluetoothHelper.getInstance().stopAlarm(null);
                        dialogFragment.dismiss();
                    })
                    .build();
            jl_dialog.show(activity.getSupportFragmentManager(), "alarm_notify");
        }
    }

    //todo   闹钟停止
    @Override
    public void onAlarmStop() {
        super.onAlarmStop();
        JL_Log.e(TAG, "onAlarm stop -->");
        FragmentActivity activity = (FragmentActivity) ActivityManager.getInstance().getCurrentActivity();
        if (activity != null && activity.getSupportFragmentManager().findFragmentByTag("alarm_notify") != null) {
            Jl_Dialog dialog = (Jl_Dialog) activity.getSupportFragmentManager().findFragmentByTag("alarm_notify");
            if (dialog.isShow()) {
                dialog.dismiss();
            }
        }
    }
}
