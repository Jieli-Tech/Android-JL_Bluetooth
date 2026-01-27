package com.jieli.btsmart.ui.widget.connect;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.constant.JL_DeviceType;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.ScanDeviceAdapter;
import com.jieli.btsmart.data.model.device.ScanBtDevice;
import com.jieli.btsmart.ui.device.DeviceConnectPresenterImpl;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.List;

/**
 * 扫描设备弹框
 *
 * @author zqjasonZhong
 * @since 2020/5/16
 */
@Deprecated
public class ScanDeviceDialog extends DialogFragment {

    private AVLoadingIndicatorView avLoading;
    private RecyclerView rvData;
    private SwipeRefreshLayout slRefresh;
    private RelativeLayout rlMain;

    private ScanDeviceAdapter mAdapter;

    private volatile boolean isShow = false;
    private volatile boolean isRefreshing;
    private OnScanDeviceOpListener mOnScanDeviceOpListener;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null) {
            //设置dialog的基本样式参数
            requireDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
            Window window = requireDialog().getWindow();
            if (window != null) {
                //去掉dialog默认的padding
                window.getDecorView().setPadding(0, 0, 0, 0);
                WindowManager.LayoutParams lp = window.getAttributes();
                lp.width = getScreenWidth() == 0 ? WindowManager.LayoutParams.MATCH_PARENT : getScreenWidth();
                lp.height = getScreenHeight() == 0 ? WindowManager.LayoutParams.WRAP_CONTENT : getScreenHeight();
                //设置dialog的位置在底部
                lp.gravity = Gravity.BOTTOM;
                //设置dialog的动画
                lp.windowAnimations = R.style.BottomToTopAnim;
                window.setAttributes(lp);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }

        View view = inflater.inflate(R.layout.dialog_scan_device, container, false);
        avLoading = view.findViewById(R.id.av_scan_device_loading);
        rvData = view.findViewById(R.id.rv_scan_device_data);
        slRefresh = view.findViewById(R.id.sl_scan_device_refresh);
        rlMain = view.findViewById(R.id.rl_scan_device_main);
        rlMain.setOnClickListener(mOnClickListener);
        initView(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mOnScanDeviceOpListener != null) {
            mOnScanDeviceOpListener.startScan();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isShow) setShow(true);
    }

    @Override
    public void onDestroyView() {
        setShow(false);
        super.onDestroyView();
    }

    public boolean isShow() {
        return isShow;
    }

    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
//        super.show(manager, tag);
        setShow(true);
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void dismiss() {
        setShow(false);
//        super.dismiss();
        super.dismissAllowingStateLoss();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        setShow(false);
        super.onDismiss(dialog);
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v == rlMain){
                dismiss();
            }
        }
    };

    public void setOnScanDeviceOpListener(OnScanDeviceOpListener onScanDeviceOpListener) {
        mOnScanDeviceOpListener = onScanDeviceOpListener;
    }

    public void onBtAdapterStatus(boolean enable) {
        if (!enable) {
            updateLoadingView(false);
            updateScanDeviceList(new ArrayList<>(), false);
        }
    }

    public void onDiscovery(int state, @Nullable BluetoothDevice device, @Nullable BleScanMessage bleScanMessage) {
        switch (state) {
            case DeviceConnectPresenterImpl.DISCOVERY_STATUS_IDLE:
//                updateLoadingView(false);
                break;
            case DeviceConnectPresenterImpl.DISCOVERY_STATUS_WORKING:
                updateLoadingView(true);
                updateScanDeviceList(new ArrayList<>(), false);
                break;
            case DeviceConnectPresenterImpl.DISCOVERY_STATUS_FOUND:
                if (device != null) {
                    updateScanDeviceList(getScanBtDevice(device, bleScanMessage));
                }
                break;
        }
    }

    public void onDeviceConnection(BluetoothDevice device, int status) {
        if (mAdapter != null) {
            mAdapter.updateDeviceState(device, status);
        }
    }

    private void initView(View view) {
        if (getContext() == null) return;
        rvData.setLayoutManager(new LinearLayoutManager(getContext()));

        slRefresh.setColorSchemeColors(getResources().getColor(android.R.color.black),
                getResources().getColor(android.R.color.darker_gray),
                getResources().getColor(android.R.color.black),
                getResources().getColor(android.R.color.background_light));
        slRefresh.setProgressBackgroundColorSchemeColor(Color.WHITE);
        slRefresh.setSize(SwipeRefreshLayout.DEFAULT);
        slRefresh.setOnRefreshListener(mOnRefreshListener);

        updateScanDeviceList(new ArrayList<>(), false);
    }

    private int getScreenWidth() {
        DisplayMetrics displayMetrics = getDisplayMetrics();
        return displayMetrics == null ? 0 : displayMetrics.widthPixels;
    }

    private int getScreenHeight() {
        DisplayMetrics displayMetrics = getDisplayMetrics();
        return displayMetrics == null ? 0 : displayMetrics.heightPixels;
    }

    private DisplayMetrics getDisplayMetrics() {
        if (getContext() == null) return null;
        if (getContext().getResources() == null) return null;
        return getContext().getResources().getDisplayMetrics();
    }

    private void setShow(boolean show) {
        isShow = show;
        MainApplication.getApplication().setNeedBanDialog(isShow);
    }

    private void setRefreshing(boolean refreshing) {
        isRefreshing = refreshing;
    }

    private void updateScanDeviceList(ScanBtDevice device) {
        if (device == null) return;
        List<ScanBtDevice> list = new ArrayList<>();
        list.add(device);
        updateScanDeviceList(list, true);
    }

    private void updateScanDeviceList(List<ScanBtDevice> list, boolean isAppend) {
        if (!isAdded() || isDetached()) return;
        if (list == null) {
            list = new ArrayList<>();
            isAppend = false;
        }
        if (mAdapter == null) {
            mAdapter = new ScanDeviceAdapter();
            mAdapter.setOnItemClickListener(mOnItemClickListener);
            mAdapter.setNewInstance(list);
        } else if (!isAppend) {
            mAdapter.setNewInstance(list);
        } else {
            mAdapter.addData(list);
        }
        if (rvData != null) {
            rvData.setAdapter(mAdapter);
        }
    }

    private void updateLoadingView(boolean isShow) {
        if (!isAdded() || isDetached()) return;
        if (avLoading != null) {
            avLoading.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }
    }

    private ScanBtDevice getScanBtDevice(BluetoothDevice device, BleScanMessage scanMessage) {
        if (device == null) return null;
        int deviceType = JL_DeviceType.JL_DEVICE_TYPE_SOUNDBOX;
        if (scanMessage != null) {
            deviceType = scanMessage.getDeviceType();
        }
        return new ScanBtDevice(device, deviceType, scanMessage);
    }

    private final SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            if (!isRefreshing) {
                setRefreshing(true);
                if (slRefresh != null && !isDetached()) {
                    slRefresh.setRefreshing(isRefreshing);
                }
                if (mOnScanDeviceOpListener != null) {
                    mOnScanDeviceOpListener.startScan();
                }
                mHandler.postDelayed(() -> {
                    if (isRefreshing) {
                        setRefreshing(false);
                        if (slRefresh != null && !isDetached()) {
                            slRefresh.setRefreshing(isRefreshing);
                        }
                    }
                }, 800);
            }
        }
    };

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(@NonNull BaseQuickAdapter adapter, @NonNull View view, int position) {
            if (mAdapter != null) {
                ScanBtDevice item = mAdapter.getItem(position);
                if (item == null) return;
                if (mOnScanDeviceOpListener != null) {
                    mOnScanDeviceOpListener.connectBtDevice(item.getDevice(), item.getBleScanMessage());
                }
            }
        }
    };

    public interface OnScanDeviceOpListener {

        void startScan();

        void stopScan();

        void connectBtDevice(BluetoothDevice device, BleScanMessage bleScanMessage);

        void disconnectBtDevice(BluetoothDevice device);
    }
}
