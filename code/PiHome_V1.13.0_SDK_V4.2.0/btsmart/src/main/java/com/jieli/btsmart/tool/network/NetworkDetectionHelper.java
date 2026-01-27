package com.jieli.btsmart.tool.network;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.utils.JL_Log;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/24 2:37 PM
 * @desc : 网络监测工具类
 */
public class NetworkDetectionHelper {
    private final static String TAG = NetworkDetectionHelper.class.getSimpleName();
    @SuppressLint("StaticFieldLeak")
    private volatile static NetworkDetectionHelper sHelper;
    @SuppressLint("StaticFieldLeak")
    private static Context sContext;
    private final ArrayList<OnNetworkDetectionListener> mOnNetworkDetectionListenerList = new ArrayList<>();
    private final Handler mUIHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mThreadPool = Executors.newSingleThreadExecutor();

    private final ConnectivityManager mConnectivityManager;
    private NetworkStateReceiver mNetworkStateReceiver;
    private TimeOutTask timeOutTask;
    private int networkType;
    private boolean isNetworkAvailable = false;
    private boolean isFirstSync = false;

    private NetworkDetectionHelper(@NonNull Context context) {
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        detectNetworkStatus();
    }

    public static boolean isInit() {
        return sHelper != null || sContext != null;
    }

    public static void init(Context context) {
        sContext = context;
    }

    public static NetworkDetectionHelper getInstance() {
        if (sHelper == null) {
            synchronized (NetworkDetectionHelper.class) {
                if (sHelper == null) {
                    if (sContext == null) {
                        throw new RuntimeException("Please call 'init' method at first.");
                    }
                    sHelper = new NetworkDetectionHelper(sContext);
                }
            }
        }
        return sHelper;
    }

    public void addOnNetworkDetectionListener(OnNetworkDetectionListener listener) {
        if (listener == null || mOnNetworkDetectionListenerList.contains(listener)) return;
        boolean ret = mOnNetworkDetectionListenerList.add(listener);
        if (ret && isFirstSync) {
            listener.onNetworkStateChange(networkType, isNetworkAvailable);
        }
    }

    public void removeOnNetworkDetectionListener(OnNetworkDetectionListener listener) {
        if (listener == null) return;
        mOnNetworkDetectionListenerList.remove(listener);
    }

    public void destroy() {
        if (!mThreadPool.isShutdown()) {
            mThreadPool.shutdownNow();
        }
        unregisterNetworkStateReceiver();
        mOnNetworkDetectionListenerList.clear();
        mUIHandler.removeCallbacksAndMessages(null);
        isFirstSync = false;
        sContext = null;
        sHelper = null;
    }

    private void setNetworkType(int networkType) {
        this.networkType = networkType;
    }

    private void setNetworkAvailable(boolean available) {
        isNetworkAvailable = available;
    }

    private void detectNetworkStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                    .build();
            JL_Log.d(TAG, "-detectNetworkStatus- ");
            mConnectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) { //网络连接
                    super.onAvailable(network);
                    NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
                    if (networkInfo == null) return;
                    JL_Log.i(TAG, "--connected-- handle from ConnectivityManager = " + networkInfo.getClass() + "\tthread==" + Thread.currentThread().getName());
                    checkNetworkAvailable(networkInfo);
                }


                @Override
                public void onLost(@NonNull Network network) { //网络断开
                    super.onLost(network);
                    JL_Log.d(TAG, "onLost");
                    NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
                    int type = networkInfo == null ? ConnectivityManager.TYPE_MOBILE : networkInfo.getType();
                    notifyNetworkStatus(type, false);
                }
            });
        } else {
            registerNetworkStateReceiver();
//            syncNetworkStatus();
        }
    }

    /**
     * 测试网络是否连通外网
     *
     * <strong>阻塞方法，不能在主线程使用</strong>
     *
     * @param ip 测试IP地址
     * @return 结果
     */
    private boolean checkNetworkIsAvailable(String ip) {
        int timeOut = 3000;
        Process process = null;
        boolean ret = false;
        try {
            process = Runtime.getRuntime().exec("/system/bin/ping  -c 1 -w 1000 " + ip);
            long time = System.currentTimeMillis();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ret = process.waitFor(timeOut, TimeUnit.MILLISECONDS);
            } else {
                if (timeOutTask == null) timeOutTask = new TimeOutTask();
                JL_Log.d(TAG, "-checkNetworkIsAvailable- thread = " + Thread.currentThread().toString());
                timeOutTask.setThread(Thread.currentThread());
                mUIHandler.postDelayed(timeOutTask, timeOut);
                ret = process.waitFor() == 0;
            }
            JL_Log.d(TAG, "-checkNetworkIsAvailable- address=" + ip + "\ttake time=" + (System.currentTimeMillis() - time) + "\tstate:" + ret);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mUIHandler.removeCallbacks(timeOutTask);
            if (process != null) {
                process.destroy();
            }
        }
        return ret;
    }

    private void checkNetworkAvailable(@NonNull NetworkInfo networkInfo) {
        int type = networkInfo.getType();
        long currentTime = System.currentTimeMillis();
        JL_Log.d(TAG, "-checkNetworkAvailable- >>> type = " + type + "\tthread==" + Thread.currentThread().getName() + "\tstart = " + currentTime);
        //测试网络是否连接外网
        boolean available = checkNetworkIsAvailable("www.baidu.com")
                || checkNetworkIsAvailable("www.aliyun.com")
                || checkNetworkIsAvailable("www.qq.com");
        JL_Log.d(TAG, "-checkNetworkAvailable- >>> type = " + type + "\tthread==" + Thread.currentThread().getName() + "\tavailable = " + available + "\tused : " + (System.currentTimeMillis() - currentTime));
        networkInfo = mConnectivityManager.getNetworkInfo(type);
        if (networkInfo == null)
            return;//网络检测的过程中，该网络已失效
        notifyNetworkStatus(type, available);
    }

    private void notifyNetworkStatus(final int type, final boolean available) {
        if (!isFirstSync) isFirstSync = true;
        setNetworkType(type);
        setNetworkAvailable(available);
        mUIHandler.post(() -> {
            if (mOnNetworkDetectionListenerList.isEmpty()) return;
            for (OnNetworkDetectionListener listener : new ArrayList<>(mOnNetworkDetectionListenerList)) {
                listener.onNetworkStateChange(type, available);
            }
        });
    }

    private void registerNetworkStateReceiver() {
        if (null == mNetworkStateReceiver) {
            mNetworkStateReceiver = new NetworkStateReceiver();
            sContext.registerReceiver(mNetworkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    private void unregisterNetworkStateReceiver() {
        if (null != mNetworkStateReceiver) {
            sContext.unregisterReceiver(mNetworkStateReceiver);
            mNetworkStateReceiver = null;
        }
    }

    /**
     * 网络状态监听器
     */
    public interface OnNetworkDetectionListener {

        void onNetworkStateChange(int type, boolean available);
    }

    private static class TimeOutTask implements Runnable {
        private Thread thread;


        public void setThread(Thread thread) {
            this.thread = thread;
        }

        @Override
        public void run() {
            JL_Log.i(TAG, "--TimeOutTask-- thread = " + thread);
            if (!thread.isInterrupted()) {
                thread.interrupt();
            }
        }
    }

    /*
     * 网络状态监听
     */
    public class NetworkStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent) return;
            int networkType = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, 0);
            final NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(networkType);
            if (null == networkInfo) return;
            if (networkInfo.isConnected()) {
                JL_Log.i(TAG, "--connected-- handle from broadcast = ");
                if (!mThreadPool.isShutdown()) {
                    mThreadPool.submit(() -> checkNetworkAvailable(networkInfo));
                }
            } else {
                notifyNetworkStatus(networkType, false);
            }
        }
    }

}
