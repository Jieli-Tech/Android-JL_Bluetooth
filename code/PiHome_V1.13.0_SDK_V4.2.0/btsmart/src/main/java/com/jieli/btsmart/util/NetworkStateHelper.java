package com.jieli.btsmart.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.tool.callback.CbBasicHelper;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.component.network.NetWorkUtil;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/24 2:37 PM
 * @desc : 网络状态监测助手
 */
public class NetworkStateHelper {
    private static final NetworkStateHelper instance = new NetworkStateHelper();
    private final String tag = getClass().getSimpleName();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final CallbackManager mCallbackManager = new CallbackManager(mHandler);

    private boolean networkIsAvailable = false;

    public static NetworkStateHelper getInstance() {
        return instance;
    }

    private NetworkStateHelper() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            ConnectivityManager cm = (ConnectivityManager) MainApplication.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                    .build();

            cm.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    final NetworkInfo networkInfo = cm.getNetworkInfo(network);
                    if (networkInfo == null) return;
                    JL_Log.d(tag, "onAvailable  " + networkInfo.getClass() + "\tthread==" + Thread.currentThread().getName());

                    int type = networkInfo.getType();
                    boolean available = checkNetworkIsAvailable("www.baidu.com", 1) || checkNetworkIsAvailable("www.baidu.com", 2)
                            || checkNetworkIsAvailable("www.aliyun.com", 1) || checkNetworkIsAvailable("www.aliyun.com", 2)
                            || checkNetworkIsAvailable("www.qq.com", 1) || checkNetworkIsAvailable("www.qq.com", 2);
                    JL_Log.d(tag, "onAvailable  " + type + "\tthread==" + Thread.currentThread().getName() + "\tavailabe=" + available);
                    if (cm.getNetworkInfo(network) == null) return;//网络检测的过程中，该网络已失效
                    setNetworkIsAvailable(available);
                    mCallbackManager.onNetworkStateChange(type, available);
                }


                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    JL_Log.d(tag, "onLost");
                    final NetworkInfo networkInfo = cm.getNetworkInfo(network);
                    int type = networkInfo == null ? ConnectivityManager.TYPE_MOBILE : networkInfo.getType();
                    setNetworkIsAvailable(false);
                    mCallbackManager.onNetworkStateChange(type, false);

                }

            });
        } else {
            NetWorkUtil.checkNetworkIsAvailable(isAvailable -> {
                setNetworkIsAvailable(isAvailable);
                mCallbackManager.onNetworkStateChange(ConnectivityManager.TYPE_MOBILE, isAvailable);
            });
        }

    }


    public boolean isNetworkIsAvailable() {
        return networkIsAvailable;
    }

    private void setNetworkIsAvailable(boolean networkIsAvailable) {
        this.networkIsAvailable = networkIsAvailable;
    }

    public void registerListener(Listener listener) {
        mCallbackManager.addCallback(listener);
    }

    public void unregisterListener(Listener listener) {
        mCallbackManager.removeCallback(listener);
    }

    public interface Listener {
        void onNetworkStateChange(int type, boolean available);
    }

    public static class CallbackManager extends CbBasicHelper<Listener> implements Listener {

        public CallbackManager(Handler uiHandler) {
            super(uiHandler);
        }

        @Override
        public void onNetworkStateChange(int type, boolean available) {
            postCbEvent(callback -> callback.onNetworkStateChange(type, available));
        }
    }

    private static class TimeOutTask implements Runnable {
        private Thread thread;


        public void setThread(Thread thread) {
            this.thread = thread;
        }

        @Override
        public void run() {
            thread.interrupt();
        }
    }

    private TimeOutTask timeOutTask;

    private boolean checkNetworkIsAvailable(String ip, int pingWay) {
        int timeOut = 3000;
        Process process = null;
        boolean ret = false;
        try {
            String command;
            switch (pingWay) {
                case 1://输出数据不进行ip与主机名反查
                    command = "/system/bin/ping  -n 1 -w 1000 " + ip;
                    break;
                case 2://ping的次数
                default:
                    command = "/system/bin/ping  -c 1 -w 1000 " + ip;
                    break;
            }
            process = Runtime.getRuntime().exec(command);
            long time = new Date().getTime();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ret = process.waitFor(timeOut, TimeUnit.MILLISECONDS);
            } else {
                if (timeOutTask == null) {
                    timeOutTask = new TimeOutTask();
                }
                timeOutTask.setThread(Thread.currentThread());
                mHandler.postDelayed(timeOutTask, timeOut);
                ret = process.waitFor() == 0;
            }
            JL_Log.d(tag, "-checkNetworkIsAvailable-->address=" + ip + "\ttake time=" + (new Date().getTime() - time) + "\tstate:" + ret);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != timeOutTask) {
                mHandler.removeCallbacks(timeOutTask);
            }
            if (process != null) {
                process.destroy();
            }
        }
        return ret;
    }
}
