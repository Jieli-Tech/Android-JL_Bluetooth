package com.jieli.btsmart.tool.network;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.component.network.NetWorkUtil;
import com.jieli.component.network.WifiHelper;
import com.jieli.component.utils.FileUtil;
import com.jieli.component.utils.ToastUtil;
import com.jieli.jl_dialog.Jl_Dialog;
import com.jieli.jl_http.JL_HttpClient;
import com.jieli.jl_http.bean.ProductDesignMessage;
import com.jieli.jl_http.bean.ProductMessage;
import com.jieli.jl_http.bean.ProductModel;
import com.jieli.jl_http.interfaces.IActionListener;
import com.jieli.jl_http.interfaces.IDownloadListener;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 网络检测辅助类
 *
 * @author zqjasonZhong
 * @since 2020/5/21
 */
public class NetworkHelper {
    private final static String TAG = NetworkHelper.class.getSimpleName();
    private static volatile NetworkHelper instance;
    private ADVInfoResponse mADVInfo;
    private Set<OnNetworkEventCallback> mNetworkEventCallbackSet;
    private final Set<String> interceptor = new HashSet<>();
    private boolean networkIsAvailable = true;
    private Jl_Dialog mOpenNetworkDialog;
    private OnNetworkDialogOpListener mOnNetworkDialogOpListener;
    private final RCSPController mRCSPController = RCSPController.getInstance();

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private NetworkHelper() {
        WifiHelper.getInstance(MainApplication.getApplication()).registerOnWifiCallback(mOnWifiCallBack);
    }

    public static NetworkHelper getInstance() {
        if (instance == null) {
            synchronized (NetworkHelper.class) {
                if (instance == null) {
                    instance = new NetworkHelper();
                }
            }
        }
        return instance;
    }


    public void destroy() {
        WifiHelper.getInstance(MainApplication.getApplication()).unregisterOnWifiCallback(mOnWifiCallBack);
        mADVInfo = null;
        if (mNetworkEventCallbackSet != null) {
            mNetworkEventCallbackSet.clear();
            mNetworkEventCallbackSet = null;
        }
        dismissNetworkDialog();
        interceptor.clear();
        mHandler.removeCallbacksAndMessages(null);
    }

    public void registerNetworkEventCallback(OnNetworkEventCallback networkEventCallback) {
        if (mNetworkEventCallbackSet == null) {
            mNetworkEventCallbackSet = new HashSet<>();
        }
        if (networkEventCallback != null) {
            mNetworkEventCallbackSet.add(networkEventCallback);
        }
    }

    public void unregisterNetworkEventCallback(OnNetworkEventCallback onNetworkEventCallback) {
        if (mNetworkEventCallbackSet != null && onNetworkEventCallback != null) {
            mNetworkEventCallbackSet.remove(onNetworkEventCallback);
        }
    }

    public boolean isNetworkIsAvailable() {
        return networkIsAvailable;
    }

    private void setNetworkIsAvailable(boolean networkIsAvailable) {
        this.networkIsAvailable = networkIsAvailable;
    }

    public void setADVInfo(ADVInfoResponse ADVInfo) {
        mADVInfo = ADVInfo;
        if (mADVInfo != null) {
            ProductMessage.DeviceBean deviceMsg = ProductUtil.getDeviceMessage(MainApplication.getApplication(), mADVInfo.getVid(), mADVInfo.getUid(), mADVInfo.getPid());
            if (deviceMsg == null) {
                checkNetworkIsAvailable();
            }
        }
    }

    public void checkNetworkIsAvailable() {
        NetWorkUtil.checkNetworkIsAvailable(this::callbackNetworkState);
    }

    public boolean checkNetworkAvailableAndToast() {
        boolean networkAvailable = true;
        if (!isNetworkIsAvailable()) {
            networkAvailable = false;
            ToastUtil.showToastShort(R.string.no_network);
            //网络不可用的时候就不要播放
        }
        return networkAvailable;
    }

    public void showOpenNetworkDialog(AppCompatActivity activity, OnNetworkDialogOpListener listener) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        setOnNetworkDialogOpListener(listener);
        if (mOpenNetworkDialog == null) {
            mOpenNetworkDialog = Jl_Dialog.builder()
                    .title(activity.getString(com.jieli.component.R.string.tips))
                    .content(activity.getString(com.jieli.component.R.string.none_network_tip))
                    .left(activity.getString(com.jieli.component.R.string.cancel))
                    .right(activity.getString(com.jieli.component.R.string.to_setting))
                    .leftClickListener((v, dialogFragment) -> {
                        if (dialogFragment != null) {
                            dialogFragment.dismiss();
                        }
                    })
                    .rightClickListener((v, dialogFragment) -> {
                        if (dialogFragment != null) {
                            dialogFragment.dismiss();
                        }
                        notifyDialogSettings();
                    })
                    .build();
        } else if (mOpenNetworkDialog.isShow()) {
            mOpenNetworkDialog.dismiss();
        }
        JL_Log.w("zzzz", "showOpenNetworkDialog :  " + mOpenNetworkDialog.isShow());
        if (!mOpenNetworkDialog.isShow()) {
            mOpenNetworkDialog.show(activity.getSupportFragmentManager(), "open_network_dialog");
            notifyDialogShow();
        }
    }

    public void dismissNetworkDialog() {
        if (mOpenNetworkDialog != null) {
            if (mOpenNetworkDialog.isShow()) {
                mOpenNetworkDialog.dismiss();
                notifyDialogDismiss();
            }
            mOpenNetworkDialog = null;
        }
    }

    private void checkHeadsetConfigureUpdate(ADVInfoResponse response) {
        if (response == null) {
            if (mRCSPController.isDeviceConnected()) {
                DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
                response = new ADVInfoResponse()
                        .setPid(deviceInfo.getPid())
                        .setVid(deviceInfo.getVid())
                        .setUid(deviceInfo.getUid());
            }
            if (response == null) {
                JL_Log.d(TAG, "checkHeadsetConfigureUpdate >> response is null.");
                return;
            }
        }
        setADVInfo(response);
        String url = ProductUtil.findCacheDesign(MainApplication.getApplication(), response.getVid(), response.getUid(),
                response.getPid(), ProductModel.MODEL_DEVICE_LEFT_IDLE.getValue());
        JL_Log.d(TAG, "checkHeadsetConfigureUpdate >> url : " + url);
        if (url == null) {
            updateDeviceMsgFromService(MainApplication.getApplication(), response.getVid(), response.getUid(), response.getPid());
        }
    }

    private void updateDeviceMsgFromService(final Context context, final int vid, final int uid, final int pid) {
        String key = getKeyForADV(vid, uid, pid);
        if (!isContainsInterceptor(key)) {
            interceptor.add(key);
            JL_HttpClient.getInstance().requestProductDesign(uid, pid, new IActionListener<List<ProductDesignMessage>>() {
                @Override
                public void onSuccess(List<ProductDesignMessage> response) {
                    if (response != null && response.size() > 0) {
                        for (final ProductDesignMessage message : response) {
                            final String url = message.getUrl();
                            if (!TextUtils.isEmpty(url)) {
                                String outPathUrl = ProductUtil.formatOutPath(context, vid, uid, pid,
                                        message.getScene(), message.getHash(), message.getType());
                                if (FileUtil.checkFileExist(outPathUrl)) {
                                    if (ProductModel.MODEL_PRODUCT_MESSAGE.getValue().equals(message.getScene())) {
                                        JL_Log.w(TAG, "updateDeviceMsgFromService :: update json: " + outPathUrl);
                                        byte[] data = FileUtil.getBytes(outPathUrl);
                                        String json = new String(data).trim();
                                        ProductUtil.saveProductMessage(context, vid, uid, pid, json);
                                        callbackUpdateConfigureSuccess();
                                    } else {
                                        callbackUpdateImage();
                                    }
                                } else {
                                    String fileUrl = ProductUtil.findCacheDesign(context, vid, uid, pid, message.getScene());
                                    if (!TextUtils.isEmpty(fileUrl) && FileUtil.checkFileExist(fileUrl)) { //删除旧产品图片
                                        JL_Log.w(TAG, "updateDeviceMsgFromService :: old file path : " + fileUrl);
                                        FileUtil.deleteFile(new File(fileUrl));
                                    }
                                    if (!isContainsInterceptor(getUrlKey(url, message.getScene()))) {
                                        interceptor.add(getUrlKey(url, message.getScene()));
                                        JL_HttpClient.getInstance().downloadFile(url, outPathUrl, new IDownloadListener() {
                                            @Override
                                            public void onStart(String startPath) {
                                                JL_Log.i(TAG, "updateDeviceMsgFromService :: downloadFile onStart. startPath = " + startPath);
                                            }

                                            @Override
                                            public void onProgress(float progress) {

                                            }

                                            @Override
                                            public void onStop(String outputPath) {
                                                JL_Log.w(TAG, "updateDeviceMsgFromService :: downloadFile onStop. outputPath = " + outputPath);
                                                if (ProductModel.MODEL_PRODUCT_MESSAGE.getValue().equals(message.getScene())) {
                                                    byte[] data = FileUtil.getBytes(outputPath);
                                                    String json = new String(data).trim();
//                                            JL_Log.w(TAG, "updateDeviceMsgFromService :: length = " + json.getBytes().length);
                                                    ProductUtil.saveProductMessage(context, vid, uid, pid, json);
                                                    callbackUpdateConfigureSuccess();
                                                } else {
                                                    callbackUpdateImage();
                                                    /*ImageCompress.getInstance().compress(context, outputPath, new IActionCallback<String>() {
                                                        @Override
                                                        public void onSuccess(String message) {
                                                            callbackUpdateImage();
                                                        }

                                                        @Override
                                                        public void onError(BaseError error) {
                                                            callbackUpdateImage();
                                                        }
                                                    });*/
                                                }
                                                interceptor.remove(getUrlKey(url, message.getScene()));
                                            }

                                            @Override
                                            public void onError(int code, String msg) {
                                                JL_Log.e(TAG, "updateDeviceMsgFromService :: download error : code = " + code + ",message = " + msg);
                                                callbackUpdateConfigureFailed(code, msg);
                                                interceptor.remove(getUrlKey(url, message.getScene()));
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    }
                    interceptor.remove(getKeyForADV(vid, uid, pid));
                }

                @Override
                public void onError(int code, String message) {
                    JL_Log.e(TAG, "updateDeviceMsgFromService :: requestProductDesign error : code = " + code + ",message = " + message);
                    callbackUpdateConfigureFailed(code, message);
                    interceptor.remove(getKeyForADV(vid, uid, pid));
                }
            });
        }
    }

    private void setOnNetworkDialogOpListener(OnNetworkDialogOpListener onNetworkDialogOpListener) {
        mOnNetworkDialogOpListener = onNetworkDialogOpListener;
    }

    private String getKeyForADV(int vid, int uid, int pid) {
        return "key_" + vid + "_" + uid + "_" + pid;
    }

    private String getUrlKey(String url, String scene) {
        return url + "_" + scene;
    }

    private boolean isContainsInterceptor(String address) {
        boolean ret = false;
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            ret = interceptor.contains(address);
        }
        return ret;
    }

    private void callbackNetworkState(final boolean isAvailable) {
        setNetworkIsAvailable(isAvailable);
        if (mNetworkEventCallbackSet != null) {
            mHandler.post(() -> {
                if (mNetworkEventCallbackSet != null) {
                    for (OnNetworkEventCallback callback : new HashSet<>(mNetworkEventCallbackSet)) {
                        callback.onNetworkState(isAvailable);
                    }
                }
            });
        }
        if (isAvailable) {
            dismissNetworkDialog();
            checkHeadsetConfigureUpdate(mADVInfo);
        }
    }

    private void callbackUpdateConfigureSuccess() {
        if (mNetworkEventCallbackSet != null) {
            mHandler.post(() -> {
                if (mNetworkEventCallbackSet != null) {
                    for (OnNetworkEventCallback callback : new HashSet<>(mNetworkEventCallbackSet)) {
                        callback.onUpdateConfigureSuccess();
                    }
                }
            });
        }
    }

    private void callbackUpdateImage() {
        if (mNetworkEventCallbackSet != null) {
            mHandler.post(() -> {
                if (mNetworkEventCallbackSet != null) {
                    for (OnNetworkEventCallback callback : new HashSet<>(mNetworkEventCallbackSet)) {
                        callback.onUpdateImage();
                    }
                }
            });
        }
    }

    private void callbackUpdateConfigureFailed(final int code, final String message) {
        if (mNetworkEventCallbackSet != null) {
            mHandler.post(() -> {
                if (mNetworkEventCallbackSet != null) {
                    for (OnNetworkEventCallback callback : new HashSet<>(mNetworkEventCallbackSet)) {
                        callback.onUpdateConfigureFailed(code, message);
                    }
                }
            });
        }
    }

    private void notifyDialogShow() {
        if (mOnNetworkDialogOpListener != null) {
            mHandler.post(() -> {
                if (mOnNetworkDialogOpListener != null) {
                    mOnNetworkDialogOpListener.onShow(mOpenNetworkDialog);
                }
            });
        }
    }

    private void notifyDialogDismiss() {
        if (mOnNetworkDialogOpListener != null) {
            mHandler.post(() -> {
                if (mOnNetworkDialogOpListener != null) {
                    mOnNetworkDialogOpListener.onDismiss(mOpenNetworkDialog);
                }
            });
        }
    }

    private void notifyDialogSettings() {
        if (mOnNetworkDialogOpListener != null) {
            mHandler.post(() -> {
                if (mOnNetworkDialogOpListener != null) {
                    mOnNetworkDialogOpListener.onSettings(mOpenNetworkDialog);
                }
            });
        }
    }

    private final WifiHelper.OnWifiCallBack mOnWifiCallBack = new WifiHelper.OnWifiCallBack() {
        @Override
        public void onConnected(WifiInfo info) {
            if (info != null) {
                //刚连上wifi有一段时间是处于无网状态
                mHandler.postDelayed(() -> {
                    checkNetworkIsAvailable();
                }, 300);

            }
        }

        @Override
        public void onState(int state) {
            switch (state) {
                case WifiHelper.STATE_NETWORK_NOT_OPEN:
                    callbackNetworkState(false);
                    break;
                case WifiHelper.STATE_NETWORK_TYPE_IS_MOBILE:
                    callbackNetworkState(true);
                    break;
                case WifiHelper.STATE_NETWORK_INFO_EMPTY: //wifi关闭或网络关闭
                case WifiHelper.STATE_WIFI_PWD_NOT_MATCH:
                case WifiHelper.STATE_WIFI_IS_CONNECTED:
                case WifiHelper.STATE_WIFI_INFO_EMPTY:
                    checkNetworkIsAvailable();
                    break;
            }
        }
    };

    public interface OnNetworkEventCallback {

        void onNetworkState(boolean isAvailable);

        void onUpdateConfigureSuccess();

        void onUpdateImage();

        void onUpdateConfigureFailed(int code, String message);
    }

    public interface OnNetworkDialogOpListener {

        void onShow(Jl_Dialog dialog);

        void onDismiss(Jl_Dialog dialog);

        void onSettings(Jl_Dialog dialog);

    }
}
