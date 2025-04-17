package com.jieli.btsmart.ui.widget.DevicePopDialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.command.tws.NotifyAdvInfoCmd;
import com.jieli.bluetooth.bean.parameter.NotifyAdvInfoParam;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.ProductAction;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.bluetooth.rcsp.DeviceOpHandler;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.ActivityManager;
import com.jieli.component.utils.SystemUtil;

import java.util.List;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/4 3:44 PM
 * @desc :
 */
public class DevicePopDialog extends Service {

    public static final int FOREGROUND_ID = 135;
    private final String tag = getClass().getSimpleName();
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private DevicePopDialogView root;

    @Override
    public void onCreate() {
        super.onCreate();
        mRCSPController.addBTRcspEventCallback(mBtEventCallback);
        root = new DevicePopDialogView(this);
        root.setActivated(false);
        root.addOnAttachStateChangeListener(new ShowTimeOutTask(root));
        createNotification();
        if (PermissionUtil.hasBluetoothPermission(getApplicationContext()) && !mRCSPController.isScanning()) {
            mRCSPController.startBleScan(SConstant.SCAN_TIME);
        }
    }

    @Override
    public void onDestroy() {
        mRCSPController.removeBTRcspEventCallback(mBtEventCallback);
        clearNotification();
        stopSelf();
        super.onDestroy();
        mRCSPController.destroy();
        System.exit(0);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    public boolean isShowing() {
        return root.isActivated();
    }

    @SuppressLint("WrongConstant")
    private void createNotification() {
        String CHANNEL_ONE_ID = getPackageName();
        String CHANNEL_ONE_NAME = "Channel_Two";
        NotificationChannel notificationChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_LOW);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(notificationChannel);
            }
        }
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        Class<?> clazz = AppUtil.getConnectActivityClass();
        Intent nfIntent = new Intent(getApplicationContext(), clazz);
        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_MUTABLE;
        }
        builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, nfIntent, flags))
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_btsmart_logo)
                .setContentText(getString(R.string.app_is_running))
                .setWhen(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ONE_ID);
        }
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(FOREGROUND_ID, notification);
    }

    private void clearNotification() {
        stopForeground(true);
    }

    private void dismissDialog(BleScanMessage bleScanMessage) {
//        JL_Log.i(tag, "dismissDialog  showing = " + isShowing());
        if (bleScanMessage != null && bleScanMessage.baseEquals(root.bleScanMessage))
            root.dismiss();
    }

    private void showDialog(BluetoothDevice device, BleScanMessage bleScanMessage) {
        boolean canOverlays = SystemUtil.isCanDrawOverlays(getApplicationContext());
        if (isShowing() || shouldIgnore(bleScanMessage)) return;
        root.setActivated(true);
        root.bleScanMessage = bleScanMessage;
        root.device = device;
        WindowManager wm;
        if (canOverlays) {
            if (null == getApplication()) return;
            wm = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        } else {
            Activity activity = ActivityManager.getInstance().getTopActivity();
            if (null == activity || activity.isDestroyed() || activity.isFinishing()) {
                JL_Log.w(tag, "showDialog","none activity");
                return;
            }
            JL_Log.d(tag, "showDialog","activity = " + activity);
            wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        }
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        lp.format = PixelFormat.RGBA_8888;

        //window类型
        if (canOverlays) {
            lp.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        } else {
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        }
        if (wm.getDefaultDisplay() != null && wm.getDefaultDisplay().isValid()) {
            try {
                wm.addView(root, lp);
                root.setTag(wm);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    //定义处理过滤规则
    private boolean shouldIgnore(BleScanMessage bleScanMessage) {
        return DevicePopDialogFilter.getInstance().shouldIgnore(bleScanMessage);
    }

    private boolean isConnectedDevice(String addr) {
        if (!BluetoothAdapter.checkBluetoothAddress(addr)) return false;
        List<BluetoothDevice> connectedDevices = mRCSPController.getConnectedDeviceList();
        if (connectedDevices == null || connectedDevices.isEmpty()) return false;
        for (BluetoothDevice device : connectedDevices) {
            if (null == device) continue;
            if (mRCSPController.getBluetoothManager().isMatchDevice(device.getAddress(), addr)) {
                return true;
            }
        }
        return false;
    }

    private final BTRcspEventCallback mBtEventCallback = new BTRcspEventCallback() {
        @Override
        public void onAdapterStatus(boolean bEnabled, boolean bHasBle) {
            //关闭蓝牙时隐藏对话框
            if (!bEnabled) {
                root.dismiss();
            }
        }

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (DeviceOpHandler.getInstance().isReconnecting()) return;
            if (status != StateCode.CONNECTION_OK) return;//非连接成功状态返回

            if (isShowing() && BluetoothUtil.deviceEquals(device, root.device)) return;//已经在弹窗
            if (isShowing()) {
                dismissDialog(root.bleScanMessage);//如有已经显示，隐藏上一次显示的弹窗
            }

            BTRcspEventCallback btEventCallback = new BTRcspEventCallback() {
                @Override
                public void onDeviceCommand(BluetoothDevice targetDevice, CommandBase cmd) {
                    if (!BluetoothUtil.deviceEquals(targetDevice, device)) return;
                    if (cmd.getId() != Command.CMD_ADV_DEVICE_NOTIFY) return;
                    mRCSPController.removeBTRcspEventCallback(this);//收到第一条adv命令移除回调
                    NotifyAdvInfoCmd notifyAdvInfoCmd = (NotifyAdvInfoCmd) cmd;
                    NotifyAdvInfoParam advInfo = notifyAdvInfoCmd.getParam();
                    BleScanMessage message = UIHelper.convertBleScanMsgFromNotifyADVInfo(advInfo);
                    showDialog(device, message);
                    JL_Log.e(tag, "onShowDialog", "===========show by adv info==========");
                }

                @Override
                public void onConnection(BluetoothDevice targetDevice, int status) {
                    if (DeviceOpHandler.getInstance().isReconnecting()) return;
                    if (!BluetoothUtil.deviceEquals(targetDevice, device)) return;
                    mRCSPController.removeBTRcspEventCallback(this);//状态变化时移除回调
                }
            };
            mRCSPController.addBTRcspEventCallback(btEventCallback);
        }

        //只关注是否要显示弹窗
        @Override
        public void onShowDialog(BluetoothDevice device, BleScanMessage bleScanMessage) {
            super.onShowDialog(device, bleScanMessage);
            if (null == device || null == bleScanMessage) return;
            int action = bleScanMessage.getAction();
            switch (action) {
                case ProductAction.DEVICE_ACTION_DISMISS:
//                    JL_Log.d(tag, "dismiss -->" + BluetoothUtil.printBtDeviceInfo(device));
                    dismissDialog(bleScanMessage);
                    break;
                case ProductAction.DEVICE_ACTION_UNCONNECTED:
                    boolean isBleConnecting = mRCSPController.checkDeviceIsConnecting(device);
                    boolean isEdrConnecting = false;
                    String edrAddress = bleScanMessage.getEdrAddr();
                    if (!TextUtils.isEmpty(edrAddress) && BluetoothAdapter.checkBluetoothAddress(edrAddress)) {
                        BluetoothDevice edrDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(edrAddress);
                        isEdrConnecting = mRCSPController.checkDeviceIsConnecting(edrDevice);
                    }
                    if (isEdrConnecting || isBleConnecting) {
                        dismissDialog(bleScanMessage);
                        break;
                    }
                case ProductAction.DEVICE_ACTION_CONNECTED:
                case ProductAction.DEVICE_ACTION_CONNECTING:
                case ProductAction.DEVICE_ACTION_CONNECTIONLESS:
                    if (isConnectedDevice(device.getAddress())) { //如果设备已连接，就忽略广播包信息
                        return;
                    }
                    showDialog(device, bleScanMessage);
                    JL_Log.v(tag, "onShowDialog", "===========show by broadcast==========\n" + BluetoothUtil.printBtDeviceInfo(device) + "\n" + bleScanMessage + "\n=====================================");
                    break;
            }
        }
    };


}
