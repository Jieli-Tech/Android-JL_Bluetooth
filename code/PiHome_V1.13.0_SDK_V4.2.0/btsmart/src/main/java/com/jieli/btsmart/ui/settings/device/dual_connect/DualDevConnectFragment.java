package com.jieli.btsmart.ui.settings.device.dual_connect;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.double_connect.ConnectedBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DeviceBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DoubleConnectionState;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.adapter.DualDevAdapter;
import com.jieli.btsmart.databinding.FragmentDualDevConnectBinding;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.settings.device.DeviceSettingsViewModel;
import com.jieli.jl_dialog.Jl_Dialog;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: DualDevConnectFragment
 * @Description: 双设备连接
 * @Author: ZhangHuanMing
 * @CreateDate: 2023/8/16 10:30
 */
public class DualDevConnectFragment extends DeviceControlFragment {

    private FragmentDualDevConnectBinding mBinding;
    private DeviceSettingsViewModel mViewModel;
    private DualDevAdapter mDualDevAdapter;

    private Jl_Dialog mTipsDialog;

    public static DualDevConnectFragment newInstance() {
        return new DualDevConnectFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentDualDevConnectBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Bundle bundle = getArguments();
        if (null == bundle) {
            finish();
            return;
        }
        final BluetoothDevice device = bundle.getParcelable(SConstant.KEY_DEVICE);
        if (null == device) {
            finish();
            return;
        }
        mViewModel = new ViewModelProvider(this, new DeviceSettingsViewModel.Factory(device)).get(DeviceSettingsViewModel.class);
        initUI();
        addObserver();
        DoubleConnectionState state = mViewModel.getDoubleConnectionState();
        if (state == null) {
            mViewModel.syncDoubleConnectionState();
        } else {
            updateDoubleConnectionState(state);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissTipsDialog();
    }

    private void initUI() {
        if (requireActivity() instanceof CommonActivity) {
            ((CommonActivity) requireActivity()).updateTopBar(getString(R.string.double_connection), R.drawable.ic_back_black,
                    v -> finish(), 0, null);
        }

        mDualDevAdapter = new DualDevAdapter();
        mBinding.rvConnectedList.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.rvConnectedList.setAdapter(mDualDevAdapter);

        mBinding.swDualDev.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DeviceInfo deviceInfo = mViewModel.getDeviceInfo(mViewModel.mDevice.getAddress());
            if (null == deviceInfo) {
                mBinding.swDualDev.setCheckedImmediatelyNoEvent(false);
                return;
            }
            DoubleConnectionState state = deviceInfo.getDoubleConnectionState();
            if (null == state) {
                mBinding.swDualDev.setCheckedImmediatelyNoEvent(false);
                return;
            }
            if (!isChecked) {
                List<DeviceBtInfo> connectedBtInfos = mDualDevAdapter.getData();
                if (connectedBtInfos.size() > 1) {
                    String otherDeviceBtName = null;
                    for (DeviceBtInfo info : connectedBtInfos) {
                        if (DualDevAdapter.isOwn(requireContext(), info.getBtName())) continue;
                        otherDeviceBtName = info.getBtName();
                        break;
                    }
                    if (otherDeviceBtName != null) {
                        mBinding.swDualDev.setCheckedImmediatelyNoEvent(true);
                        showTipsDialog(state.getVersion(), otherDeviceBtName);
                    }
                    return;
                }
            }
            mViewModel.changeDoubleConnectionState(new DoubleConnectionState().setVersion(state.getVersion()).setOn(isChecked));
        });
    }

    private void addObserver() {
        mViewModel.switchDeviceMLD.observe(getViewLifecycleOwner(), device -> {
            if (!BluetoothUtil.deviceEquals(mViewModel.mDevice, device)) {
                finish();
            }
        });
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), connection -> {
            if (!BluetoothUtil.deviceEquals(mViewModel.mDevice, connection.getDevice())) return;
            if (connection.getStatus() != StateCode.CONNECTION_OK
                    || connection.getStatus() != StateCode.CONNECTION_CONNECTED) {
                finish();
            }
        });
        mViewModel.mDualConnectionStateMLD.observe(getViewLifecycleOwner(), this::updateDoubleConnectionState);
        mViewModel.mConnectedBtInfoMLD.observe(getViewLifecycleOwner(), this::updateConnectedBtInfo);
    }

    private void updateDoubleConnectionState(DoubleConnectionState state) {
        if (isInvalid() || null == state) return;
        mBinding.swDualDev.setCheckedImmediatelyNoEvent(state.isOn());
        if (state.isOn()) {
            ConnectedBtInfo connectedBtInfo = mViewModel.getConnectedBtInfo();
            updateConnectedBtInfo(connectedBtInfo);
        } else {
            mDualDevAdapter.setList(new ArrayList<>());
        }
        mViewModel.syncConnectedBtInfo();
    }

    private void updateConnectedBtInfo(ConnectedBtInfo info) {
        if(isInvalid() || null == info) return;
        List<DeviceBtInfo> list = info.getDeviceBtInfoList();
        if (null == list) list = new ArrayList<>();
        mDualDevAdapter.setList(list);
    }

    private void showTipsDialog(int version, String name) {
        if (isInvalid()) return;
        if (mTipsDialog == null) {
            mTipsDialog = new Jl_Dialog.Builder()
                    .title(getString(R.string.close_double_connection_tips))
                    .content(getString(R.string.close_double_connection_content, name))
                    .width(0.95f)
                    .cancel(false)
                    .left(getString(R.string.cancel))
                    .leftColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                    .leftClickListener((v, dialogFragment) -> {
                        dismissTipsDialog();
                        mBinding.swDualDev.setCheckedImmediatelyNoEvent(true);
                    })
                    .right(getString(R.string.confirm))
                    .rightColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                    .rightClickListener((v, dialogFragment) -> {
                        dismissTipsDialog();
                        mViewModel.changeDoubleConnectionState(new DoubleConnectionState().setVersion(version).setOn(false));
                    })
                    .build();
        }
        if (!mTipsDialog.isShow()) {
            mTipsDialog.show(getChildFragmentManager(), "Tips Dialog");
        }
    }

    private void dismissTipsDialog(){
        if(mTipsDialog != null){
            if(mTipsDialog.isShow() && !isInvalid()){
                mTipsDialog.dismiss();
            }
            mTipsDialog = null;
        }
    }
}
