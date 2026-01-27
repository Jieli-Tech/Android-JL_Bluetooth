package com.jieli.btsmart.ui.widget.DevicePopDialog;

import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.view.WindowManager;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.btsmart.constant.SConstant;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/16 11:47 AM
 * @desc :弹窗超时检查,如超时则取消弹窗
 */
class ShowTimeOutTask extends BTRcspEventCallback implements View.OnAttachStateChangeListener {
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private final DevicePopDialogView root;


    public ShowTimeOutTask(DevicePopDialogView root) {
        this.root = root;
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        mRCSPController.addBTRcspEventCallback(this);
        v.postDelayed(task, SConstant.SHOW_DIALOG_TIMEOUT);
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        mRCSPController.removeBTRcspEventCallback(this);
        v.removeCallbacks(task);
    }

    @Override
    public void onShowDialog(BluetoothDevice device, BleScanMessage bleScanMessage) {
        super.onShowDialog(device, bleScanMessage);
        //是否同一个设备
        boolean sameDevice = bleScanMessage.baseEquals(root.bleScanMessage);
        if (sameDevice) {
            root.removeCallbacks(task);
            root.postDelayed(task, SConstant.SHOW_DIALOG_TIMEOUT);
        }
    }

    private final Runnable task = new Runnable() {
        @Override
        public void run() {
            WindowManager wm = (WindowManager) root.getTag();
            wm.removeViewImmediate(root);
            root.setTag(null);
        }
    };
}
