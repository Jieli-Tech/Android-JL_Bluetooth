package com.jieli.btsmart.tool.bluetooth.rcsp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.tool.location.LocationHelper;
import com.jieli.btsmart.ui.LogService;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.jl_http.bean.LogResponse;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 上传用户信息任务
 * @since 2021/12/9
 */
public class UploadDeviceInfoTask extends BTRcspEventCallback {
    private PendingIntent mUploadPI;
    private int mUploadInterval = UPLOAD_DEV_INFO_INTERVAL;

    private final static int REQUEST_CODE_UPLOAD_DEV_INFO = 6111;
    private final static int UPLOAD_DEV_INFO_INTERVAL = 10 * 60 * 1000;
    private final static int MSG_UPLOAD_INFO = 0x6984;

    private final Handler mHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == MSG_UPLOAD_INFO) {
            uploadDevInfo();
        }
        return true;
    });


    @Override
    public void onConnection(BluetoothDevice device, int status) {
        if (status == StateCode.CONNECTION_OK) {
            startUploadDevInfo();
            if (!LocationHelper.isInit()) {
                LocationHelper.getInstance();
            }
        } else {
            stopUploadDevInfo();
        }
    }

    @SuppressLint("WrongConstant")
    private void uploadDevInfo() {
        MainApplication application = MainApplication.getApplication();
        if (application == null) return;
        if (mUploadPI == null) {
            Intent intent = new Intent(application, LogService.class);
            intent.setAction(LogService.ACTION_UPLOAD_DEVICE_MSG);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                flags = PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
            }
            mUploadPI = PendingIntent.getService(application, REQUEST_CODE_UPLOAD_DEV_INFO, intent, flags);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !PermissionUtil.isHasPermission(application, Manifest.permission.SCHEDULE_EXACT_ALARM)){
            JL_Log.i("UploadDeviceInfoTask", "uploadDevInfo : no alarm permission.");
            return;
        }
        AppUtil.startTimerTask(application, 0, mUploadPI);
        mHandler.removeMessages(MSG_UPLOAD_INFO);
        mHandler.sendEmptyMessageDelayed(MSG_UPLOAD_INFO, mUploadInterval);
    }

    private void startUploadDevInfo() {
        MainApplication application = MainApplication.getApplication();
        if (application == null) return;
        LogResponse logResponse = application.getLogResponse();
        mUploadInterval = logResponse == null ? UPLOAD_DEV_INFO_INTERVAL : logResponse.getDevUploadInterval() * 60 * 1000;
        mHandler.removeMessages(MSG_UPLOAD_INFO);
        mHandler.sendEmptyMessage(MSG_UPLOAD_INFO);
    }

    private void stopUploadDevInfo() {
        MainApplication application = MainApplication.getApplication();
        if (application == null) return;
        mHandler.removeMessages(MSG_UPLOAD_INFO);
        if (mUploadPI != null) {
            AppUtil.stopTimerTask(application, mUploadPI);
            mUploadPI = null;
        }
        application.stopService(new Intent(application, LogService.class));
    }
}
