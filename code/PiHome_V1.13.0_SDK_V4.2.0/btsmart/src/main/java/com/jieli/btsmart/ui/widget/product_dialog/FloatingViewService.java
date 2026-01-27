package com.jieli.btsmart.ui.widget.product_dialog;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.ProductAction;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.UIHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Deprecated
public class FloatingViewService extends Service {
    private final static String TAG = "FloatingViewService";
    private WindowManager mWindowManager;
    private View mFloatingView;

    private ViewGroup mainLayout;
    private TextView mTitleTv;
    private RecyclerView mContentView;
    private GridLayoutManager mGridLayoutManager;
    private TextView mConnectDeviceButton;
    private TextView mTipsTv;
    private View mFinishLayout;

    private ProductAdapter mAdapter;

    private BluetoothDevice mScanDevice;
    private BleScanMessage mBleScanMessage;
    private BleScanMessageHandler mScanMessageHandler;
    private FastConnectEdrDialog.OnFastConnectListener mListener;
    private IBTOp mBTOp;
    private long startTime;
    private int mPackCount;

    private final FloatingBinder mFloatingBinder = new FloatingBinder();

    public final static int NOTIFICATION_ID = 135;
    private final static int SHOW_TYPE_CONNECT = 0;
    private final static int SHOW_TYPE_TIPS = 1;
    private final static int SHOW_TYPE_FINISH = 2;

    private final static int MSG_DISMISS_WINDOW = 0x1234;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == MSG_DISMISS_WINDOW) {
            JL_Log.w(TAG, "onShowDialog :: MSG_DISMISS_WINDOW  over time ");
            dismissWindow(msg.arg1 == 1);
        }
        return false;
    });

    public FloatingViewService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 获取WindowManager服务
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        createNotification();
    }


    @SuppressLint("WrongConstant")
    private void createNotification() {
        String CHANNEL_ONE_ID = "com.jieli.btsmart";
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
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY_COMPATIBILITY;
        BluetoothDevice device = intent.getParcelableExtra(SConstant.KEY_BLUETOOTH_DEVICE);
        BleScanMessage message = (BleScanMessage) intent.getSerializableExtra(SConstant.KEY_BLE_SCAN_MESSAGE);
        showFloatingWindow(device, message);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mFloatingBinder;
    }

    @Override
    public void onDestroy() {
        if (mListener != null) {
            mListener = null;
        }
        JL_Log.w(TAG, "onShowDialog :: onDestroy");
        dismissWindow(false);
        mFloatingView = null;
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }

    public boolean isFloatingWindowShowing() {
        return mFloatingView != null && mFloatingView.isActivated();
    }

    public BleScanMessage getBleScanMessage() {
        return mBleScanMessage;
    }

    private void setBleScanMessage(BleScanMessage scanMessage) {
        this.mBleScanMessage = scanMessage;
    }

    public BluetoothDevice getScanDevice() {
        return mScanDevice;
    }

    public void setOnFastConnectListener(FastConnectEdrDialog.OnFastConnectListener listener) {
        this.mListener = listener;
    }

    private BleScanMessageHandler getScanMessageHandler() {
        if (mScanMessageHandler == null) {
            mScanMessageHandler = BleScanMessageHandler.getInstance();
        }
        return mScanMessageHandler;
    }

    @SuppressLint("InflateParams")
    private void showFloatingWindow(BluetoothDevice device, BleScanMessage scanMessage) {
        if (device == null || scanMessage == null) return;
        boolean canDrawOverlays = ProductUtil.isCanDrawOverlays(getApplicationContext());
        if (canDrawOverlays) {
            if (mFloatingView == null || !mFloatingView.isActivated()) {
                mFloatingView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.view_product_message1, null);
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                } else {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
                }
                //去除刘海屏限制
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                }
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
                layoutParams.format = PixelFormat.RGBA_8888;
                layoutParams.gravity = Gravity.BOTTOM;
                layoutParams.windowAnimations = R.style.FloatingViewAnim;
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_FULLSCREEN;
                mWindowManager.addView(mFloatingView, layoutParams);
                mFloatingView.setActivated(true);
                initView(mFloatingView);
                this.mScanDevice = device;
                updateTitle(UIHelper.getCacheDeviceName(device));
                updateBleScanMessage(scanMessage);
            } else if (ProductUtil.equalScanMessage(mBleScanMessage, scanMessage)) {
                updateBleScanMessage(scanMessage);
//                mWindowManager.updateViewLayout(mFloatingView, mLayoutParams);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView(View view) {
        mainLayout = view.findViewById(R.id.product_message_main_layout);
        mainLayout.setBackgroundColor(getResources().getColor(R.color.color_transparent));
        mTitleTv = view.findViewById(R.id.product_message_title);
        ImageView closeBtn = view.findViewById(R.id.product_message_close);
        mConnectDeviceButton = view.findViewById(R.id.product_message_connect_btn);
        mContentView = view.findViewById(R.id.product_message_content_layout);
        mGridLayoutManager = new GridLayoutManager(getApplicationContext(), 2);
        mContentView.setLayoutManager(mGridLayoutManager);
        mTipsTv = view.findViewById(R.id.product_message_tips_tv);
        mFinishLayout = view.findViewById(R.id.product_message_finish_layout);
        TextView checkTv = view.findViewById(R.id.product_message_check_tv);
        TextView finishTv = view.findViewById(R.id.product_message_finish_tv);

        closeBtn.setOnClickListener(mOnClickListener);
        mConnectDeviceButton.setOnClickListener(mOnClickListener);
        checkTv.setOnClickListener(mOnClickListener);
        finishTv.setOnClickListener(mOnClickListener);
        mainLayout.setOnClickListener(v -> {
            JL_Log.w(TAG, "onClick :: dismissDialog");
            dismissWindow(true);
        });
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mainLayout != null) {
                    mainLayout.setBackgroundColor(getResources().getColor(R.color.half_transparent_1));
                }
            }
        }, 500);
        /*view.setOnTouchListener((v, event) -> {
            if (mainLayout == null) return false;
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            float y = event.getRawY();
            int viewY = v.getHeight() - mainLayout.getHeight();
//                JL_Log.i(TAG, "onTouch : screenHeight : " + v.getHeight() + ", viewHeight : " + mainLayout.getHeight());
            if (action == MotionEvent.ACTION_DOWN) {
                if (y < viewY) {
                    JL_Log.w(TAG, "onTouch :: dismissDialog");
                    dismissWindow(true);
                    return true;
                }
            }
            return false;
        });*/
    }

    public void updateTitle(String title) {
        if (TextUtils.isEmpty(title)) {
            title = "";
        }
        if (mTitleTv != null) {
            mTitleTv.setText(title);
        }
    }

    public void setBTOp(IBTOp BTOp) {
        mBTOp = BTOp;
    }

    public void resetTimeout(BleScanMessage message) {
        if (ProductUtil.equalScanMessage(mBleScanMessage, message)) {
            long currentTime = Calendar.getInstance().getTimeInMillis();
            long usedTime = currentTime - startTime;
            JL_Log.i(TAG, "onShowDialog :: updateBleScanMessage :: startTime : " + startTime + ", currentTime : " + currentTime + ", usedTime : " + usedTime);
            if (usedTime >= 500) {
                mHandler.removeMessages(MSG_DISMISS_WINDOW);
                mHandler.sendEmptyMessageDelayed(MSG_DISMISS_WINDOW, SConstant.SHOW_DIALOG_TIMEOUT);
                startTime = currentTime;
            }
        }
    }

    private void updateBottomBarUI(int showType, String title) {
        switch (showType) {
            case SHOW_TYPE_CONNECT: //Connect
                updateTipsTv(null, View.GONE);
                updateFinishLayout(View.GONE);
                updateConnectBtn(View.VISIBLE);
                break;
            case SHOW_TYPE_TIPS: //tips
                updateFinishLayout(View.GONE);
                updateConnectBtn(View.GONE);
                updateTipsTv(title, View.VISIBLE);
                break;
            case SHOW_TYPE_FINISH: //finish
                updateTipsTv(null, View.GONE);
                updateConnectBtn(View.GONE);
                updateFinishLayout(View.VISIBLE);
                break;
        }
    }

    private void updateConnectBtn(int visibility) {
        if (mConnectDeviceButton != null) {
            mConnectDeviceButton.setVisibility(visibility);
        }
    }

    private void updateTipsTv(String tips, int visibility) {
        if (mTipsTv != null) {
            mTipsTv.setVisibility(visibility);
            if (visibility == View.VISIBLE && !TextUtils.isEmpty(tips)) {
                mTipsTv.setText(tips);
            }
        }
    }

    private void updateFinishLayout(int visibility) {
        if (mFinishLayout != null) {
            mFinishLayout.setVisibility(visibility);
        }
    }

    public void updateBleScanMessage(BleScanMessage bleScanMessage) {
        if (bleScanMessage != null) {
            boolean isSameBleMsg = UIHelper.compareBleScanMessage(mBleScanMessage, bleScanMessage);
            setBleScanMessage(bleScanMessage);
            if (bleScanMessage.getAction() == ProductAction.DEVICE_ACTION_DISMISS) {
                JL_Log.w(TAG, "onShowDialog :: updateBleScanMessage :: dismissDialog");
                dismissWindow(false);
                return;
            } else {
                resetTimeout(bleScanMessage);
            }
            if (isSameBleMsg) { //因为数据相同，不再更新UI
                if (bleScanMessage.getAction() == ProductAction.DEVICE_ACTION_CONNECTED) {
                    mPackCount++;
                    if (mPackCount > 200) {
                        mPackCount = 0;
                    }
                } else {
                    mPackCount = 0;
                }
                if (mPackCount % 5 == 0) {
                    //                JL_Log.d(TAG, "onShowDialog :: updateBleScanMessage :: isSameBleMsg");
                    return;
                }
            }
            if (bleScanMessage.getAction() == ProductAction.DEVICE_ACTION_UNCONNECTED && mBTOp != null && mBTOp.checkIsConnectingEdrDevice(bleScanMessage.getEdrAddr())) {
                boolean isPaired = UIHelper.isContainsBoundedEdrList(getApplicationContext(), bleScanMessage.getEdrAddr());
                if (!isPaired) {
                    JL_Log.w(TAG, "onShowDialog :: edr is pairing :: dismissDialog");
                    dismissWindow(false);
                    return;
                }
                //TODO:经典蓝牙正在连接，不处理。
                JL_Log.w("zzc_product", "onShowDialog : edr is connecting...");
                bleScanMessage.setAction(ProductAction.DEVICE_ACTION_CONNECTING);
            }
            switch (bleScanMessage.getAction()) {
                case ProductAction.DEVICE_ACTION_UNCONNECTED: {
                    updateBottomBarUI(SHOW_TYPE_CONNECT, null);
                    updateProductDesign(bleScanMessage);
                    break;
                }
                case ProductAction.DEVICE_ACTION_CONNECTED: {
                    boolean isPhoneConnected = ProductUtil.checkEdrIsConnected(bleScanMessage.getEdrAddr());
                    boolean isPaired = UIHelper.isContainsBoundedEdrList(getApplicationContext(), bleScanMessage.getEdrAddr());
                    if (isPhoneConnected) {
                        updateBottomBarUI(SHOW_TYPE_FINISH, null);
                    } else {
                        if (!isPaired) {
                            showNotConnectBottomTips(bleScanMessage);
                        } else {
                            updateBottomBarUI(SHOW_TYPE_TIPS, null);
                        }
                    }
                    JL_Log.w("zzc", "onShowDialog :: DEVICE_ACTION_CONNECTED : isPhoneConnected : " + isPhoneConnected + ", isPaired : " + isPaired);
                    boolean isShowQuantity = isPhoneConnected || isPaired;
                    handlerDeviceQuantity(bleScanMessage, isShowQuantity ? ProductDesign.ACTION_SHOW_QUANTITY : ProductDesign.ACTION_HIDE_QUANTITY);
                    break;
                }
                case ProductAction.DEVICE_ACTION_CONNECTING: {
                    boolean isPaired = UIHelper.isContainsBoundedEdrList(getApplicationContext(), bleScanMessage.getEdrAddr()) || bleScanMessage.getVid() == BluetoothConstant.APPLE_VID;
                    if (isPaired) {
                        updateBottomBarUI(SHOW_TYPE_TIPS, null);
                        handlerDeviceQuantity(bleScanMessage, ProductDesign.ACTION_SHOW_QUANTITY);
                    } else {
                        showNotConnectBottomTips(bleScanMessage);
                        updateProductDesign(bleScanMessage);
                    }
                    break;
                }
                case ProductAction.DEVICE_ACTION_CONNECTIONLESS: {
                    updateBottomBarUI(SHOW_TYPE_TIPS, getApplicationContext().getString(R.string.device_connectionless_tips));
                    updateProductDesign(bleScanMessage);
                    break;
                }
            }
        }
    }

    private void updateProductDesign(BleScanMessage bleScanMessage) {
        List<ProductDesignItem> data = getScanMessageHandler().handlerProductDesign(bleScanMessage);
        updateContentView(data, false);
    }

    private void handlerDeviceQuantity(BleScanMessage bleScanMessage, int action) {
        List<ProductDesignItem> data = getScanMessageHandler().handlerConnectedDeviceStatus(bleScanMessage, action);
        updateContentView(data, false);
    }

    private void updateContentView(List<ProductDesignItem> data, boolean isAppend) {
        if (mContentView == null) return;
        if (data == null) {
            data = new ArrayList<>();
            isAppend = false;
        }
        if (mAdapter == null) {
            mAdapter = new ProductAdapter(data);
        } else {
            if (isAppend) {
                mAdapter.addData(data);
            } else {
                mAdapter.setNewInstance(data);
            }
        }
        JL_Log.d("zzc_product", "updateContentView : " + data.size());
        mGridLayoutManager.setSpanCount(mAdapter.getData().size());
        mContentView.setAdapter(mAdapter);
        if (mBleScanMessage != null) {
            getScanMessageHandler().requestProductDesignFromService(mBleScanMessage, data, new BleScanMessageHandler.OnServiceUpdateCallback() {
                @Override
                public void onUpdate(ProductDesign design) {
                    JL_Log.d("zzc_product", "design : " + design);
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onError(int code, String message) {
                    JL_Log.e("zzc_product", "updateContentView :: error ==> code : " + code + ", " + message);
                }
            });
        }
    }

    private void showNotConnectBottomTips(BleScanMessage bleScanMessage) {
        if (null == bleScanMessage) return;
        boolean isSupportCancelPair = ProductUtil.isSupportCancelPair(getApplicationContext(), bleScanMessage.getVid(),
                bleScanMessage.getUid(), bleScanMessage.getPid());
        boolean isHeadsetProduct = ProductUtil.isHeadsetTypeByADV(getApplicationContext(), bleScanMessage);
        String tips;
        if (isSupportCancelPair) {
            tips = isHeadsetProduct ? getApplicationContext().getString(R.string.long_click_cancel_pair) :
                    getApplicationContext().getString(R.string.long_click_cancel_pair_soundbox);
        } else {
            tips = isHeadsetProduct ? getApplicationContext().getString(R.string.make_sure_headset_paired) :
                    getApplicationContext().getString(R.string.make_sure_soundbox_paired);
        }
        updateBottomBarUI(SHOW_TYPE_TIPS, tips);
    }

    public void dismissWindow(boolean isUser) {
        if (mWindowManager != null && mFloatingView != null && mFloatingView.isActivated()) {
            if (isUser) {
                if (mBleScanMessage != null) {
                    JL_Log.w(TAG, "dismissWindow :: add Blacklist");
                    BlacklistHandler.getInstance().addData(mBleScanMessage.getEdrAddr(), mBleScanMessage.getSeq());
                }
            }
            JL_Log.w(TAG, "onShowDialog :: dismissWindow >> " + isUser);
            mFloatingView.setActivated(false);
            mWindowManager.removeViewImmediate(mFloatingView);
            mFloatingView = null;
        }
        startTime = 0;
        mPackCount = 0;
        mHandler.removeMessages(MSG_DISMISS_WINDOW);
        callbackOnDismiss(mScanDevice, isUser);
        setBleScanMessage(null);
        if (mScanMessageHandler != null) {
            mScanMessageHandler = null;
        }
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view != null) {
                final int viewId = view.getId();
                if (viewId == R.id.product_message_close) { //关闭
                    JL_Log.w(TAG, "click :: dismiss.");
                    dismissWindow(true);
                } else if (viewId == R.id.product_message_connect_btn) { //连接
                    callbackConnect(mScanDevice, mBleScanMessage);
                } else if (viewId == R.id.product_message_check_tv) { //查看
                    callbackSettings(mScanDevice, mBleScanMessage);
                } else if (viewId == R.id.product_message_finish_tv) { //完成
                    callbackFinish(mScanDevice, mBleScanMessage);
                }
            }
        }
    };


    private void callbackOnDismiss(final BluetoothDevice device, final boolean isUser) {
        if (mListener != null) {
            mHandler.post(() -> {
                JL_Log.i(TAG, "dismiss : " + device);
                if (mListener != null && device != null) {
                    mListener.onDismiss(device, isUser);
                }
            });
        }
    }

    private void callbackConnect(final BluetoothDevice device, final BleScanMessage bleScanMessage) {
        if (mListener != null && bleScanMessage != null) {
            mHandler.post(() -> {
                if (mListener != null && device != null) {
                    mListener.onConnect(device, bleScanMessage);
                }
            });
        }
    }

    private void callbackFinish(final BluetoothDevice device, final BleScanMessage bleScanMessage) {
        if (mListener != null && bleScanMessage != null) {
            mHandler.post(() -> {
                if (mListener != null && device != null) {
                    mListener.onFinish(device, bleScanMessage);
                }
            });
        }
    }

    private void callbackSettings(final BluetoothDevice device, final BleScanMessage bleScanMessage) {
        if (mListener != null && bleScanMessage != null) {
            mHandler.post(() -> {
                if (mListener != null && device != null) {
                    mListener.onSettings(device, bleScanMessage);
                }
            });
        }
    }

    public class FloatingBinder extends Binder {

        public FloatingViewService getService() {
            return FloatingViewService.this;
        }
    }
}
