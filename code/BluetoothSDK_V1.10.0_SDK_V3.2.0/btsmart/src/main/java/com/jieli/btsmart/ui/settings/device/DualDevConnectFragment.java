package com.jieli.btsmart.ui.settings.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.double_connect.ConnectedBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DeviceBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DoubleConnectionState;
import com.jieli.bluetooth.bean.device.voice.VoiceFunc;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.DualDevAdapter;
import com.jieli.btsmart.databinding.FragmentDualDevConnectBinding;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.component.base.BasePresenter;
import com.jieli.jl_dialog.Jl_Dialog;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: DualDevConnectFragment
 * @Description: 双设备连接
 * @Author: ZhangHuanMing
 * @CreateDate: 2023/8/16 10:30
 */
public class DualDevConnectFragment extends DeviceControlFragment implements IDeviceSettingsContract.IDeviceSettingsView {

    private FragmentDualDevConnectBinding binding;
    private IDeviceSettingsContract.IDeviceSettingsPresenter mPresenter;
    private DualDevAdapter mDualDevAdapter;
    private CommonActivity mActivity;

    private BluetoothDevice mUseDevice;
    private Jl_Dialog mTipsDialog;

    public static DualDevConnectFragment newInstance() {
        return new DualDevConnectFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDualDevConnectBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (mActivity == null && context instanceof CommonActivity) {
            mActivity = (CommonActivity) context;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPresenter = new DeviceSettingsPresenterImpl(mActivity, this);
        mUseDevice = mPresenter.getConnectedDevice();
        initUI();
        DoubleConnectionState state = mPresenter.getDeviceInfo() != null ? mPresenter.getDeviceInfo().getDoubleConnectionState() : null;
        if (state == null) {
            mPresenter.queryDoubleConnectionState();
        } else {
            updateDoubleConnectionState(state);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPresenter != null) {
            mPresenter.start();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mPresenter != null) {
            mPresenter.destroy();
        }
    }

    private void initUI() {
        if (mActivity == null && requireActivity() instanceof CommonActivity) {
            mActivity = (CommonActivity) requireActivity();
        }
        if (mActivity != null) {
            mActivity.updateTopBar(getString(R.string.double_connection), R.drawable.ic_back_black, v -> {
                if (mActivity != null) {
                    mActivity.onBackPressed();
                }
            }, 0, null);
        }

        mDualDevAdapter = new DualDevAdapter();
        binding.rvConnectedList.setAdapter(mDualDevAdapter);
        binding.rvConnectedList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.swDualDev.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DeviceInfo deviceInfo = mPresenter.getDeviceInfo();
            if (null == deviceInfo) {
                binding.swDualDev.setCheckedImmediatelyNoEvent(false);
                return;
            }
            DoubleConnectionState state = deviceInfo.getDoubleConnectionState();
            if (null == state) {
                binding.swDualDev.setCheckedImmediatelyNoEvent(false);
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
                        binding.swDualDev.setCheckedImmediatelyNoEvent(true);
                        showTipsDialog(state.getVersion(), otherDeviceBtName);
                    }
                    return;
                }
            }
            mPresenter.setDoubleConnectionState(new DoubleConnectionState().setVersion(state.getVersion()).setOn(isChecked));
        });
    }

    @Override
    public void onBtAdapterStatus(boolean enable) {

    }

    @Override
    public void onDeviceConnection(BluetoothDevice device, int status) {
        if (BluetoothUtil.deviceEquals(mUseDevice, device)
                && (status == StateCode.CONNECTION_FAILED || status == StateCode.CONNECTION_DISCONNECT)) {
            if (mActivity != null) {
                mActivity.onBackPressed();
            }
        }
    }

    @Override
    public void onSwitchDevice(BluetoothDevice device) {

    }

    @Override
    public void onADVInfoUpdate(ADVInfoResponse advInfo) {

    }

    @Override
    public void onGetADVInfoFailed(BaseError error) {

    }

    @Override
    public void onRebootSuccess() {

    }

    @Override
    public void onRebootFailed(BaseError error) {

    }

    @Override
    public void onSetNameSuccess(String name) {

    }

    @Override
    public void onSetNameFailed(BaseError error) {

    }

    @Override
    public void onConfigureSuccess(int position, int result) {

    }

    @Override
    public void onConfigureFailed(BaseError error) {

    }

    @Override
    public void onNetworkState(boolean isAvailable) {

    }

    @Override
    public void onUpdateConfigureSuccess() {

    }

    @Override
    public void onUpdateConfigureFailed(int code, String message) {

    }

    @Override
    public void onVoiceModeList(List<VoiceMode> list) {

    }

    @Override
    public void onCurrentVoiceMode(VoiceMode voiceMode) {

    }

    @Override
    public void onSelectedVoiceModes(byte[] selectedModes) {

    }

    @Override
    public void onVoiceFuncChange(VoiceFunc data) {

    }

    @Override
    public void onAdaptiveANCCheck(int state, int code) {

    }

    @Override
    public void onOpenSmartNoPickSetting(boolean isUser) {

    }

    @Override
    public void onDoubleConnectionStateChange(DoubleConnectionState state) {
        updateDoubleConnectionState(state);
    }

    @Override
    public void onConnectedBtInfo(ConnectedBtInfo info) {
        updateConnectedBtInfo(info);
    }

    @Override
    public void setPresenter(BasePresenter presenter) {
        if (mPresenter == null) {
            mPresenter = (DeviceSettingsPresenterImpl) presenter;
        }
    }

    private void updateDoubleConnectionState(DoubleConnectionState state) {
        if (state != null && isAdded() && !isDetached()) {
            binding.swDualDev.setCheckedImmediatelyNoEvent(state.isOn());
            if (state.isOn()) {
                ConnectedBtInfo connectedBtInfo = mPresenter.getDeviceInfo().getConnectedBtInfo();
                updateConnectedBtInfo(connectedBtInfo);
            } else {
                mDualDevAdapter.setList(new ArrayList<>());
            }
            mPresenter.queryConnectedBtInfo();
        }
    }

    private void updateConnectedBtInfo(ConnectedBtInfo info) {
        if (info != null && isAdded() && !isDetached()) {
            List<DeviceBtInfo> list = info.getDeviceBtInfoList();
            if (null == list) list = new ArrayList<>();
            mDualDevAdapter.setList(list);
        }
    }

    private void showTipsDialog(int version, String name) {
        if (isDetached() || !isAdded()) return;
        if (mTipsDialog == null) {
            mTipsDialog = new Jl_Dialog.Builder()
                    .title(getString(R.string.close_double_connection_tips))
                    .content(getString(R.string.close_double_connection_content, name))
                    .width(0.95f)
                    .cancel(false)
                    .left(getString(R.string.cancel))
                    .leftColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                    .leftClickListener((v, dialogFragment) -> {
                        dialogFragment.dismiss();
                        mTipsDialog = null;
                        binding.swDualDev.setCheckedImmediatelyNoEvent(true);
                    })
                    .right(getString(R.string.confirm))
                    .rightColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                    .rightClickListener((v, dialogFragment) -> {
                        dialogFragment.dismiss();
                        mTipsDialog = null;
                        mPresenter.setDoubleConnectionState(new DoubleConnectionState().setVersion(version).setOn(false));
                    })
                    .build();
        }
        if (!mTipsDialog.isShow()) {
            mTipsDialog.show(getChildFragmentManager(), "Tips Dialog");
        }
    }
}
