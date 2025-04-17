package com.jieli.btsmart.ui.widget.connect;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.adapter.ScanDeviceAdapter;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.device.ScanBtDevice;
import com.jieli.btsmart.databinding.DialogScanDeviceBinding;
import com.jieli.btsmart.tool.bluetooth.rcsp.DeviceOpHandler;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.jl_dialog.Jl_Dialog;

import java.util.ArrayList;

/**
 * 扫描设备弹框
 *
 * @author chensenhua
 * @since 2020/5/16
 */
public class ScanDeviceDialogModify extends DialogFragment implements DevicePopDialogFilter.IgnoreFilter {
    private DialogScanDeviceBinding binding;
    private DeviceConnectVM viewModel;
    private ScanDeviceAdapter mAdapter;

    private Jl_Dialog notifyGpsDialog;

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
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                //设置dialog的位置在底部
                lp.gravity = Gravity.BOTTOM;
                //设置dialog的动画
                lp.windowAnimations = R.style.BottomToTopAnim;
                window.setAttributes(lp);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
        binding = DialogScanDeviceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DeviceConnectVM.class);
        initView();
        addObserver();
        updateScanState(viewModel.isScanning(), false);
        tryToStartScan();
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        assert window != null;
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(null);
        DevicePopDialogFilter.getInstance().addIgnoreFilter(this);
    }

    @Override
    public void onStop() {
        DevicePopDialogFilter.getInstance().removeIgnoreFilter(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }


    public boolean isShow() {
        return getDialog() != null && getDialog().isShowing();
    }

    private void initView() {
        binding.slScanDeviceRefresh.setColorSchemeColors(getResources().getColor(android.R.color.black),
                getResources().getColor(android.R.color.darker_gray),
                getResources().getColor(android.R.color.black),
                getResources().getColor(android.R.color.background_light));
        binding.slScanDeviceRefresh.setProgressBackgroundColorSchemeColor(Color.WHITE);
        binding.slScanDeviceRefresh.setSize(SwipeRefreshLayout.DEFAULT);
        binding.slScanDeviceRefresh.setOnRefreshListener(this::tryToStartScan);

        mAdapter = new ScanDeviceAdapter();
        binding.rvScanDeviceData.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvScanDeviceData.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener((adapter, view1, position) -> {
            ScanBtDevice item = mAdapter.getItem(position);
            if (null == item || item.getConnection() != StateCode.CONNECTION_DISCONNECT) return;
            viewModel.connectDevice(item);
        });
        binding.rlScanDeviceMain.setOnClickListener(v -> dismiss());
    }

    private void addObserver() {
        viewModel.btAdapterMLD.observe(getViewLifecycleOwner(), enable -> {
            binding.avScanDeviceLoading.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
            if (!enable) {
                updateScanState(false, true);
            } else if (!viewModel.isScanning()) {
                tryToStartScan();
            }
        });
        viewModel.scanStateMLD.observe(getViewLifecycleOwner(), stateResult -> {
            if (!stateResult.isSuccess()) return;
            updateScanState(stateResult.getState() == StateResult.STATE_WORKING, false);
            switch (stateResult.getState()) {
                case StateResult.STATE_IDLE: {
                    break;
                }
                case StateResult.STATE_WORKING: {
                    if (stateResult.getData() == null) {
                        mAdapter.setList(new ArrayList<>());
                        return;
                    }
                    mAdapter.addData(stateResult.getData());
                }
            }
        });
        viewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnection -> {
            if (DeviceOpHandler.getInstance().isReconnecting()) return;
            mAdapter.updateDeviceState(deviceConnection.getDevice(), deviceConnection.getStatus());
        });
    }

    private void tryToStartScan() {
        if (BluetoothUtil.isBluetoothEnable()) {
            if (!PermissionUtil.checkGpsProviderEnable(requireContext())) {
                showNotifyGPSDialog();
                return;
            }
            updateScanState(viewModel.startScan(), true);
        } else {
            BluetoothUtil.enableBluetooth(requireContext());
            updateScanState(false, true);
        }
    }

    private void updateScanState(boolean isScanning, boolean isReset) {
        binding.avScanDeviceLoading.setVisibility(isScanning ? View.VISIBLE : View.INVISIBLE);
        binding.slScanDeviceRefresh.setRefreshing(false);
        if (isReset) mAdapter.setList(new ArrayList<>());
    }

    private void showNotifyGPSDialog() {
        if (isDetached() || !isAdded()) return;
        if (notifyGpsDialog == null) {
            notifyGpsDialog = new Jl_Dialog.Builder()
                    .title(getString(R.string.tips))
                    .content(getString(R.string.open_gpg_tip))
                    .cancel(false)
                    .left(getString(R.string.cancel))
                    .leftColor(getResources().getColor(R.color.gray_text_444444))
                    .right(getString(R.string.to_setting))
                    .rightColor(getResources().getColor(R.color.red_FF688C))
                    .leftClickListener((v, dialogFragment) -> dismissNotifyGPSDialog())
                    .rightClickListener((v, dialogFragment) -> {
                        dismissNotifyGPSDialog();
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), SConstant.REQUEST_CODE_CHECK_GPS);
                    })
                    .build();
        }
        if (!notifyGpsDialog.isShow()) {
            notifyGpsDialog.show(getChildFragmentManager(), "notify_gps_dialog");
        }
    }

    private void dismissNotifyGPSDialog() {
        if (notifyGpsDialog != null) {
            if (notifyGpsDialog.isShow() && !isDetached()) {
                notifyGpsDialog.dismiss();
            }
            notifyGpsDialog = null;
        }
    }

    @Override
    public boolean shouldIgnore(BleScanMessage bleScanMessage) {
        return true;
    }
}
