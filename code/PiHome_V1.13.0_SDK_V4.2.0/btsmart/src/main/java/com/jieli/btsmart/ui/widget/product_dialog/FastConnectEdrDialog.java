package com.jieli.btsmart.ui.widget.product_dialog;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.ProductAction;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.SystemUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 快连EDR弹窗
 *
 * @author zqjasonZhong
 * @date 2019/4/10
 */
@Deprecated
public class FastConnectEdrDialog {
    private final static String TAG = FastConnectEdrDialog.class.getSimpleName();
    private final Context mContext;
    private PopupWindow mPopupWindow;
    //    private ConstraintLayout mainLayout;
    private TextView mTitleTv;
    private RecyclerView mContentView;
    private GridLayoutManager mGridLayoutManager;
    private TextView mConnectDeviceButton;
    private TextView mTipsTv;
    private Group mFinishLayout;

    private ProductAdapter mAdapter;

    private BleScanMessageHandler mScanMessageHandler;
    private IBTOp mBTOp;
    private long startTime;
    private int mPackCount;

    private final Builder mBuilder;

    private final static int SHOW_TYPE_CONNECT = 0;
    private final static int SHOW_TYPE_TIPS = 1;
    private final static int SHOW_TYPE_FINISH = 2;

    private final static int MSG_DISMISS_WINDOW = 0x1234;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == MSG_DISMISS_WINDOW) {
            JL_Log.w(TAG, "onShowDialog :: MSG_DISMISS_WINDOW  over time ");
            dismissDialog(msg.arg1 == 1);
        }
        return false;
    });

    public FastConnectEdrDialog(@NonNull Context context, BluetoothDevice device, BleScanMessage bleScanMessage, OnFastConnectListener listener) {
        mContext = CommonUtil.checkNotNull(context, "context can not be empty.");
        mBuilder = new Builder(context);
        mBuilder.setBluetoothDevice(device);
        mBuilder.setBleScanMessage(bleScanMessage);
        mBuilder.setFastConnectListener(listener);
        init(mBuilder);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(Builder builder) {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_fast_connect_edr, null);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.getBackground().setAlpha(150);
        RelativeLayout mainLayout = view.findViewById(R.id.rl_fast_connect_dialog_main);
        mTitleTv = view.findViewById(R.id.product_message_title);
        ImageView closeBtn = view.findViewById(R.id.product_message_close);
        mConnectDeviceButton = view.findViewById(R.id.product_message_connect_btn);
        mContentView = view.findViewById(R.id.product_message_content_layout);
        mGridLayoutManager = new GridLayoutManager(mContext, 2);
        mContentView.setLayoutManager(mGridLayoutManager);
        mTipsTv = view.findViewById(R.id.product_message_tips_tv);
        mFinishLayout = view.findViewById(R.id.product_message_finish_layout);
        TextView checkTv = view.findViewById(R.id.product_message_check_tv);
        TextView finishTv = view.findViewById(R.id.product_message_finish_tv);

        closeBtn.setOnClickListener(mOnClickListener);
        mConnectDeviceButton.setOnClickListener(mOnClickListener);
        checkTv.setOnClickListener(mOnClickListener);
        finishTv.setOnClickListener(mOnClickListener);

        String title = mBuilder.getBluetoothDevice() == null ? "" : UIHelper.getCacheDeviceName(mBuilder.getBluetoothDevice());
        updateTitle(title);
//        updateBleScanMessage(mBuilder.getBleScanMessage());

        int screenHeight = (int) SystemUtil.getScreenResolution(mBuilder.getContext())[1];
        mPopupWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, (screenHeight == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : screenHeight));
        mPopupWindow.setClippingEnabled(false);
        mPopupWindow.setFocusable(true);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setAnimationStyle(R.style.FloatingViewAnim);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mPopupWindow.setTouchable(true);
        mPopupWindow.setOnDismissListener(() -> {
            JL_Log.w(TAG, "onDismiss :: dismissDialog");
            dismissDialog(true);
        });
        view.setOnKeyListener((view12, i, keyEvent) -> {
            if (i == KeyEvent.KEYCODE_BACK) {
                JL_Log.w(TAG, "key back :: dismissDialog");
                dismissDialog(true);
                return true;
            }
            return false;
        });
        mainLayout.setOnClickListener(v -> {
            JL_Log.w(TAG, "onClick :: dismissDialog");
            dismissDialog(true);
        });
       /* view.setOnTouchListener((view1, motionEvent) -> {
            if (mainLayout == null) return false;
            int action = motionEvent.getAction() & MotionEvent.ACTION_MASK;
            float y = motionEvent.getRawY();
            int viewY = view1.getHeight() - mainLayout.getHeight();
            JL_Log.i(TAG, "onTouch : screenHeight : " + view1.getHeight() + ", viewHeight : " + mainLayout.getHeight());
            if (action == MotionEvent.ACTION_DOWN) {
                JL_Log.i(TAG, "onTouch : y : " + y + ",viewY : " + viewY);
                if (y < viewY) {
                    JL_Log.w(TAG, "onTouch :: dismissDialog");
                    dismissDialog(true);
                    return true;
                }
            }
            return false;
        });*/
    }

    public Builder getBuilder() {
        return mBuilder;
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

    public void resetTimeout(BleScanMessage bleScanMessage) {
        if (ProductUtil.equalScanMessage(bleScanMessage, mBuilder.getBleScanMessage())) {
            long currentTime = Calendar.getInstance().getTimeInMillis();
            JL_Log.d(TAG, "onShowDialog :: updateBleScanMessage :: startTime : " + startTime + ", currentTime : " + currentTime);
            if (currentTime - startTime >= 500) {
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

    private BleScanMessageHandler getScanMessageHandler() {
        if (mScanMessageHandler == null) {
            mScanMessageHandler = BleScanMessageHandler.getInstance();
        }
        return mScanMessageHandler;
    }

    public void updateBleScanMessage(BleScanMessage bleScanMessage) {
        if (bleScanMessage != null) {
            boolean isSameBleMsg = UIHelper.compareBleScanMessage(mBuilder.getBleScanMessage(), bleScanMessage);
            mBuilder.setBleScanMessage(bleScanMessage);
            if (bleScanMessage.getAction() == ProductAction.DEVICE_ACTION_DISMISS) {
                JL_Log.w(TAG, "updateBleScanMessage :: dismissDialog");
                dismissDialog(false);
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
                return;
            }
            if (bleScanMessage.getAction() == ProductAction.DEVICE_ACTION_UNCONNECTED && mBTOp != null && mBTOp.checkIsConnectingEdrDevice(bleScanMessage.getEdrAddr())) {
                boolean isPaired = UIHelper.isContainsBoundedEdrList(mBuilder.getContext(), bleScanMessage.getEdrAddr());
                if (!isPaired) {
                    JL_Log.w(TAG, "onShowDialog :: edr is pairing :: dismissDialog");
                    dismissDialog(false);
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
                    boolean isPaired = UIHelper.isContainsBoundedEdrList(mBuilder.getContext(), bleScanMessage.getEdrAddr());
                    if (isPhoneConnected) {
                        updateBottomBarUI(SHOW_TYPE_FINISH, null);
                    } else {
                        if (!isPaired) {
                            showNotConnectBottomTips(bleScanMessage);
                        } else {
                            updateBottomBarUI(SHOW_TYPE_TIPS, null);
                        }
                    }
                    boolean isShowQuantity = isPhoneConnected || isPaired;
                    handlerDeviceQuantity(bleScanMessage, isShowQuantity ? ProductDesign.ACTION_SHOW_QUANTITY : ProductDesign.ACTION_HIDE_QUANTITY);
                    break;
                }
                case ProductAction.DEVICE_ACTION_CONNECTING: {
                    boolean isPaired = UIHelper.isContainsBoundedEdrList(mBuilder.getContext(), bleScanMessage.getEdrAddr()) || bleScanMessage.getVid() == BluetoothConstant.APPLE_VID;
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
                    updateBottomBarUI(SHOW_TYPE_TIPS, mContext.getString(R.string.device_connectionless_tips));
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
        JL_Log.i(TAG, "updateContentView :: length : " + data.size());
        mGridLayoutManager.setSpanCount(mAdapter.getData().size());
        mContentView.setAdapter(mAdapter);
        if (mBuilder.getBleScanMessage() != null) {
            getScanMessageHandler().requestProductDesignFromService(mBuilder.getBleScanMessage(), data, new BleScanMessageHandler.OnServiceUpdateCallback() {
                @Override
                public void onUpdate(ProductDesign design) {
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onError(int code, String message) {
                    JL_Log.e(TAG, "updateContentView :: error ==> code : " + code + ", " + message);
                }
            });
        }
    }

    private void showNotConnectBottomTips(BleScanMessage bleScanMessage) {
        if (null == bleScanMessage || null == mContext) return;
        boolean isSupportCancelPair = ProductUtil.isSupportCancelPair(mContext, bleScanMessage.getVid(),
                bleScanMessage.getUid(), bleScanMessage.getPid());
        boolean isHeadsetProduct = ProductUtil.isHeadsetTypeByADV(mContext, bleScanMessage);
        String tips;
        if (isSupportCancelPair) {
            tips = isHeadsetProduct ? mContext.getString(R.string.long_click_cancel_pair) :
                    mContext.getString(R.string.long_click_cancel_pair_soundbox);
        } else {
            tips = isHeadsetProduct ? mContext.getString(R.string.make_sure_headset_paired) :
                    mContext.getString(R.string.make_sure_soundbox_paired);
        }
        updateBottomBarUI(SHOW_TYPE_TIPS, tips);
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view != null) {
                final int viewId = view.getId();
                if (viewId == R.id.product_message_close) { //关闭
                    JL_Log.w(TAG, "click :: dismiss.");
                    dismissDialog(true);
                } else if (viewId == R.id.product_message_connect_btn) { //连接
                    callbackConnect(mBuilder.getBluetoothDevice(), mBuilder.getBleScanMessage());
                } else if (viewId == R.id.product_message_check_tv) { //设置
                    callbackSettings(mBuilder.getBluetoothDevice(), mBuilder.getBleScanMessage());
                } else if (viewId == R.id.product_message_finish_tv) { //完成
                    callbackFinish(mBuilder.getBluetoothDevice(), mBuilder.getBleScanMessage());
                }
            }
        }
    };

    public boolean isShowing() {
        return mPopupWindow != null && mPopupWindow.isShowing();
    }

    public void showDialog(View parentView) {
        if (mPopupWindow != null && !isShowing() && parentView != null && parentView.getWindowToken() != null) {
            mPopupWindow.showAtLocation(parentView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            mPopupWindow.update();
        }
    }

    public void dismissDialog(boolean isUser) {
        JL_Log.i(TAG, "dismissDialog >> " + isUser);
        if (mPopupWindow != null) {
            if (isUser) {
                if (mBuilder.getBleScanMessage() != null) {
                    BlacklistHandler.getInstance().addData(mBuilder.getBleScanMessage().getEdrAddr(), mBuilder.getBleScanMessage().getSeq());
                }
            }
            if (mPopupWindow.isShowing()) {
                mPopupWindow.dismiss();
            }
            startTime = 0;
            mPackCount = 0;
            callbackOnDismiss(mBuilder.getBluetoothDevice(), isUser);
            mHandler.removeMessages(MSG_DISMISS_WINDOW);
            mPopupWindow = null;
        }
        if (mScanMessageHandler != null) {
            mScanMessageHandler = null;
        }
    }

    private void callbackOnDismiss(final BluetoothDevice device, final boolean isUser) {
        if (mBuilder.getFastConnectListener() != null) {
            mHandler.post(() -> {
                if (mBuilder.getFastConnectListener() != null && device != null) {
                    mBuilder.getFastConnectListener().onDismiss(device, isUser);
                }
            });
        }
    }

    private void callbackConnect(final BluetoothDevice device, final BleScanMessage bleScanMessage) {
        if (mBuilder.getFastConnectListener() != null && bleScanMessage != null) {
            mHandler.post(() -> {
                if (mBuilder.getFastConnectListener() != null && device != null) {
                    mBuilder.getFastConnectListener().onConnect(device, bleScanMessage);
                }
            });
        }
    }

    private void callbackFinish(final BluetoothDevice device, final BleScanMessage bleScanMessage) {
        if (mBuilder.getFastConnectListener() != null && bleScanMessage != null) {
            mHandler.post(() -> {
                if (mBuilder.getFastConnectListener() != null && device != null) {
                    mBuilder.getFastConnectListener().onFinish(device, bleScanMessage);
                }
            });
        }
    }

    private void callbackSettings(final BluetoothDevice device, final BleScanMessage bleScanMessage) {
        if (mBuilder.getFastConnectListener() != null && bleScanMessage != null) {
            mHandler.post(() -> {
                if (mBuilder.getFastConnectListener() != null && device != null) {
                    mBuilder.getFastConnectListener().onSettings(device, bleScanMessage);
                }
            });
        }
    }

    public interface OnFastConnectListener {

        void onDismiss(BluetoothDevice device, boolean isUser);

        void onConnect(BluetoothDevice device, BleScanMessage bleScanMessage);

        void onFinish(BluetoothDevice device, BleScanMessage bleScanMessage);

        void onSettings(BluetoothDevice device, BleScanMessage bleScanMessage);
    }

    public static class Builder {
        private final Context mContext;
        private BluetoothDevice mDevice;
        private BleScanMessage mBleScanMessage;
        private OnFastConnectListener mFastConnectListener;

        public Builder(Context context) {
            mContext = context;
        }

        public FastConnectEdrDialog create() {
            return new FastConnectEdrDialog(mContext, mDevice, mBleScanMessage, mFastConnectListener);
        }

        public Context getContext() {
            return mContext;
        }

        public Builder setBluetoothDevice(BluetoothDevice device) {
            this.mDevice = device;
            return this;
        }

        public BluetoothDevice getBluetoothDevice() {
            return mDevice;
        }

        public Builder setBleScanMessage(BleScanMessage bleScanMessage) {
            mBleScanMessage = bleScanMessage;
            return this;
        }

        public BleScanMessage getBleScanMessage() {
            return mBleScanMessage;
        }

        public Builder setFastConnectListener(OnFastConnectListener fastConnectListener) {
            mFastConnectListener = fastConnectListener;
            return this;
        }

        public OnFastConnectListener getFastConnectListener() {
            return mFastConnectListener;
        }
    }

}
