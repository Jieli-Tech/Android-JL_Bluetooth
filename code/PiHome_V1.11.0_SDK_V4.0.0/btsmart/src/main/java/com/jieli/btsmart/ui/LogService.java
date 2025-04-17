package com.jieli.btsmart.ui;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.tool.location.LocationHelper;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.jl_http.JL_HttpClient;
import com.jieli.jl_http.bean.ProductMessage;
import com.jieli.jl_http.interfaces.IActionListener;

/**
 * 打点服务
 *
 * @author zqjasonZhong
 * @since 2020/6/15
 */
public class LogService extends Service {
    private static final String TAG = LogService.class.getSimpleName();

    public static final String ACTION_UPLOAD_DEVICE_MSG = "com.jieli.btsmart.action.upload_device_msg";
    private final RCSPController mRCSPController = RCSPController.getInstance();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        String action = intent.getAction();
        JL_Log.i(TAG, "action : " + action);
        if (!TextUtils.isEmpty(action)) {
            if (ACTION_UPLOAD_DEVICE_MSG.equals(action)) {
                if (mRCSPController.isDeviceConnected()) {
                    DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
                    if (deviceInfo != null) {
                        com.jieli.jl_http.bean.DeviceInfo device = new com.jieli.jl_http.bean.DeviceInfo();
                        device.setVid(deviceInfo.getUid());
                        device.setPid(deviceInfo.getPid());
                        device.setVersion(deviceInfo.getVersionName());
                        device.setPhoneUUID(MainApplication.OAID/*SystemUtil.getIMEI(getApplicationContext())*/);
                        device.setMac(deviceInfo.getEdrAddr());
                        ProductMessage.ChipBean chip = ProductUtil.getChipMessage(getApplicationContext(), deviceInfo.getVid(), deviceInfo.getUid(), deviceInfo.getPid());
                        if (chip != null) {
                            device.setSeries(chip.getSeries());
                            device.setName(chip.getName());
                            device.setType(chip.getCipType());
                        } else {
                            device.setSeries("");
                            device.setName("");
                            device.setType("");
                        }
                        JL_Log.d(TAG, "uploadDeviceInfo : " + device);
                        JL_HttpClient.getInstance().uploadDeviceInfo(device, new IActionListener<Boolean>() {
                            @Override
                            public void onSuccess(Boolean response) {
                                JL_Log.d(TAG, "uploadDeviceInfo :: onSuccess :  " + response);
                            }

                            @Override
                            public void onError(int code, String message) {
                                JL_Log.e(TAG, "uploadDeviceInfo :: onError :  " + code + ", " + message);
                            }
                        });
                    }
                }
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        stopSelf();
        super.onDestroy();
    }
}
