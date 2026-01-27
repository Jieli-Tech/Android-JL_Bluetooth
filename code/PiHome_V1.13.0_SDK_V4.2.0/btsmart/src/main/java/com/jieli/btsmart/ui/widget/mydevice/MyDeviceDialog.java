package com.jieli.btsmart.ui.widget.mydevice;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.OnReconnectHistoryRecordListener;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.adapter.MyDeviceInfoAdapter;
import com.jieli.btsmart.data.model.device.HistoryDevice;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.base.BaseDialogFragment;
import com.jieli.btsmart.ui.settings.device.DeviceSettingsFragment;
import com.jieli.component.utils.ToastUtil;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyDeviceDialog extends BaseDialogFragment {
    private RecyclerView rvMyDeviceList;
    private RelativeLayout llDiloagMyDevicebg;
    private LinearLayout llMyDeviceInfo;
    private TextView tvLoadOperate;

    private final RCSPController mRCSPController = RCSPController.getInstance();
    private MyDeviceInfoAdapter mAdapter;
    private List<HistoryDevice> mHistoryDeviceList;

    @NonNull
    @Contract(" -> new")
    public static MyDeviceDialog newInstance() {
        return new MyDeviceDialog();
    }

    public MyDeviceDialog() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Window window = requireDialog().getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.gravity = Gravity.TOP;

            window.getDecorView().setPadding(0, 0, 0, 0);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

            //去除刘海屏限制
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));//半透明
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);//允许在状态栏区域布局

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
            window.setWindowAnimations(R.style.TopToBottomAnim);
        }

        View view = inflater.inflate(R.layout.dialog_my_device, container, false);
        rvMyDeviceList = view.findViewById(R.id.rv_my_device_list);
        llDiloagMyDevicebg = view.findViewById(R.id.ll_dialog_my_device_bg);
        llMyDeviceInfo = view.findViewById(R.id.ll_my_device_info);
        tvLoadOperate = view.findViewById(R.id.tv_my_device_load_operate);
        View shapeView = view.findViewById(R.id.view_my_device_shape);
        llDiloagMyDevicebg.setOnClickListener(mOnClickListener);
        llMyDeviceInfo.setOnClickListener(mOnClickListener);
        tvLoadOperate.setOnClickListener(mOnClickListener);
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
            shapeView.setVisibility(View.VISIBLE);
        }
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
            setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        }
        mRCSPController.addBTRcspEventCallback(mBTRcspEventCallback);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRCSPController.removeBTRcspEventCallback(mBTRcspEventCallback);
    }

    private int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        llMyDeviceInfo.setPadding(0, getStatusBarHeight(requireContext()), 0, 0);
        initView();
        syncHistoryDevice();
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == llDiloagMyDevicebg) {
                dismissDialog();
            } else if (v == tvLoadOperate) {
                handleLoadButtonOnClick(tvLoadOperate.getText().equals(getString(R.string.more)));
            }
        }
    };

    public void initView() {
        mAdapter = new MyDeviceInfoAdapter(mRCSPController);
        rvMyDeviceList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyDeviceList.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener((adapter, view1, position) -> {
            HistoryDevice item = mAdapter.getItem(position);
            boolean isUsingDevice = mAdapter.isUsingDevice(item);
            if (isUsingDevice) {
//                mRCSPController.disconnectDevice(BluetoothUtil.getRemoteDevice(item.getDevice().getAddress()));
            } else {
                switch (item.getState()) {
                    case HistoryDevice.STATE_CONNECTED:
                        mRCSPController.switchUsingDevice(BluetoothUtil.getRemoteDevice(item.getDevice().getAddress()));
                        break;
                    case HistoryDevice.STATE_DISCONNECT:
                        item.setState(HistoryDevice.STATE_RECONNECT);
                        mRCSPController.connectHistoryBtDevice(item.getDevice(), 0, new OnReconnectHistoryRecordListener() {
                            @Override
                            public void onSuccess(HistoryBluetoothDevice history) {

                            }

                            @Override
                            public void onFailed(HistoryBluetoothDevice history, BaseError error) {
                                syncHistoryDevice();
                            }
                        });
                        mAdapter.notifyItemChanged(mAdapter.getItemPosition(item));
                        break;
                }
            }
        });
        mAdapter.setOnItemChildClickListener((adapter, view12, position) -> {
            HistoryDevice item = mAdapter.getItem(position);
            boolean isUsingDevice = mAdapter.isUsingDevice(item);
            if (isUsingDevice) {
                toDeviceSettings(BluetoothUtil.getRemoteDevice(item.getDevice().getAddress()));
            } else {
                switch (item.getState()) {
                    case HistoryDevice.STATE_CONNECTED:
                        ToastUtil.showToastShort(R.string.device_not_using);
                        break;
                    case HistoryDevice.STATE_DISCONNECT:
                        ToastUtil.showToastShort(getString(R.string.first_connect_device));
                        break;
                    case HistoryDevice.STATE_CONNECTING:
                        ToastUtil.showToastShort(getString(R.string.device_connecting_tips));
                        break;
                }
            }
        });
        requireDialog().setOnKeyListener((arg0, keyCode, arg2) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                dismissDialog();
                return true;
            }
            return false;
        });
    }

    private void dismissDialog() {
        if (getDialog() != null) {
            getDialog().dismiss();
        }
    }

    private void loadMoreHistoryListData() {
        updateDeviceList(mHistoryDeviceList);
    }

    private void packUpDataHistoryListData() {
        if (mHistoryDeviceList == null) return;
        List<HistoryDevice> list = new ArrayList<>();
        if (mHistoryDeviceList.size() > 3) {
            list.addAll(mHistoryDeviceList.subList(0, 2));
        } else {
            list.addAll(mHistoryDeviceList);
        }
        updateDeviceList(list);
    }

    private void handleLoadButtonOnClick(boolean toLoading) {
        if (!isAdded() || isDetached()) return;
        if (toLoading) {
            loadMoreHistoryListData();
        } else {
            packUpDataHistoryListData();
        }
        updateLoadButtonByCurrentState(toLoading);
    }

    public void syncHistoryDevice() {
        if (!isAdded() || isDetached() || mAdapter == null) return;
        List<HistoryBluetoothDevice> list = mRCSPController.getHistoryBluetoothDeviceList();
        if (list == null || list.isEmpty()) {
            handleDeviceList(new ArrayList<>());
            return;
        }
        List<HistoryDevice> historyDevices = new ArrayList<>();
        for (HistoryBluetoothDevice device : list) {
            HistoryDevice item = new HistoryDevice(device);
            item.setState(mAdapter.getHistoryDeviceState(device));
            boolean isUsingDevice = mAdapter.isUsingDevice(item);
            if (isUsingDevice) {
                historyDevices.add(0, item);
            } else {
                historyDevices.add(item);
            }
        }
        handleDeviceList(historyDevices);
    }

    private void handleDeviceList(List<HistoryDevice> deviceList) {
        if (!isAdded() || isDetached() || mAdapter == null) return;
        mHistoryDeviceList = deviceList;
        if (deviceList.size() > 3) {
            ArrayList<HistoryDevice> list = new ArrayList<>(deviceList.subList(0, 2));
            updateDeviceList(list);
            updateLoadButtonByCurrentState(false);
            tvLoadOperate.setVisibility(View.VISIBLE);
        } else {
            updateDeviceList(deviceList);
            tvLoadOperate.setVisibility(View.INVISIBLE);
        }
    }

    private void updateDeviceList(List<HistoryDevice> list) {
        if (!isAdded() || isDetached() || mAdapter == null) return;
        JL_Log.d(TAG, "updateDeviceList : " + list);
        ViewGroup.LayoutParams layoutParams = rvMyDeviceList.getLayoutParams();
        if (list.size() == 0) {
            layoutParams.height = 3 * (getPixelsFromDp(65));
        } else if (list.size() < 3) {
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else if (list.size() <= 6) {
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            layoutParams.height = 6 * (getPixelsFromDp(65));
        }
        mAdapter.setList(list);
    }

    private void updateLoadButtonByCurrentState(boolean isLoading) {
        if (!isAdded() || isDetached()) return;
        if (isLoading) {
            tvLoadOperate.setText(R.string.pack_up);
            @SuppressLint("UseCompatLoadingForDrawables") Drawable rightDrawable = getResources().getDrawable(R.drawable.ic_up_arrows_black);
            rightDrawable.setBounds(0, 0, rightDrawable.getMinimumWidth(), rightDrawable.getMinimumHeight());
            tvLoadOperate.setCompoundDrawables(null, null, rightDrawable, null);
        } else {
            tvLoadOperate.setText(R.string.more);
            @SuppressLint("UseCompatLoadingForDrawables") Drawable rightDrawable = getResources().getDrawable(R.drawable.ic_down_arrows_black);
            rightDrawable.setBounds(0, 0, rightDrawable.getMinimumWidth(), rightDrawable.getMinimumHeight());
            tvLoadOperate.setCompoundDrawables(null, null, rightDrawable, null);
        }
    }

    private int getPixelsFromDp(int size) {
        DisplayMetrics metrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return (size * metrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT;

    }

    private void toDeviceSettings(BluetoothDevice device) {
        if (!isAdded() || isDetached()) return;
        Bundle bundle = new Bundle();
        bundle.putParcelable(SConstant.KEY_DEVICE, device);
        CommonActivity.startCommonActivity(requireActivity(), DeviceSettingsFragment.class.getCanonicalName(), bundle);
        dismissDialog();
    }

    private final BTRcspEventCallback mBTRcspEventCallback = new BTRcspEventCallback() {
        @Override
        public void onBondStatus(BluetoothDevice device, int status) {
            if (status == BluetoothDevice.BOND_NONE) {
                syncHistoryDevice();
            }
        }

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            super.onConnection(device, status);
            if (status == StateCode.CONNECTION_OK) {
                syncHistoryDevice();
            } else {
                if (mAdapter != null && isAdded() && !isDetached()) {
                    HistoryDevice item = mAdapter.findHistoryDeviceByAddress(device.getAddress());
                    if (null != item) {
                        int newState = HistoryDevice.STATE_DISCONNECT;
                        if (status == StateCode.CONNECTION_CONNECTING) {
                            newState = HistoryDevice.STATE_CONNECTING;
                        }
                        if (item.getState() != newState) {
                            item.setState(newState);
                            mAdapter.notifyItemChanged(mAdapter.getItemPosition(item));
                        }
                    }
                }
            }
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            super.onSwitchConnectedDevice(device);
            syncHistoryDevice();
        }
    };
}
