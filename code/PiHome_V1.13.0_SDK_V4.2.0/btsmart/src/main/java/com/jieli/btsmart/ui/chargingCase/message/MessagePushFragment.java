package com.jieli.btsmart.ui.chargingCase.message;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.device.DeviceSettings;
import com.jieli.btsmart.data.model.settings.item.SettingsSwitch;
import com.jieli.btsmart.databinding.FragmentMessagePushBinding;
import com.jieli.btsmart.ui.settings.SettingsAdapter;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.widget.CommonDecoration;
import com.jieli.btsmart.util.NotificationUtil;
import com.jieli.component.utils.ValueUtil;

import java.util.ArrayList;
import java.util.List;

public class MessagePushFragment extends DeviceControlFragment {

    private FragmentMessagePushBinding mBinding;
    private MessagePushViewModel mViewModel;
    private SettingsAdapter mAdapter;

    public static MessagePushFragment newInstance() {
        return new MessagePushFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mBinding = FragmentMessagePushBinding.inflate(inflater, container, false);
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
        mViewModel = new ViewModelProvider(this, new MessagePushViewModel.Factory(device)).get(MessagePushViewModel.class);
        initUI();
        addObserver();
    }

    private void initUI() {
        mAdapter = new SettingsAdapter();
        mBinding.rvAppName.setAdapter(mAdapter);
        CommonDecoration decoration = new CommonDecoration(requireContext(), RecyclerView.VERTICAL,
                ContextCompat.getColor(requireContext(), R.color.gray_0D000000), ValueUtil.dp2px(requireContext(), 1));
        mBinding.rvAppName.addItemDecoration(decoration);
    }

    private void addObserver() {
        mViewModel.switchDeviceMLD.observe(getViewLifecycleOwner(), device -> {
            if (!BluetoothUtil.deviceEquals(mViewModel.mDevice, device)) {
                finish();
            }
        });
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), connection -> {
            if (!BluetoothUtil.deviceEquals(mViewModel.mDevice, connection.getDevice())) return;
            if (connection.getStatus() != StateCode.CONNECTION_OK) {
                finish();
            }
        });
        mViewModel.mDeviceSettingsMLD.observe(getViewLifecycleOwner(), this::updateSyncMessage);
    }

    private boolean isExistPacketName(DeviceSettings settings, String packetName) {
        if (null == settings || null == packetName) return false;
        if (!settings.isSyncMessage()) return false;
        final List<String> list = settings.getAppPacketNameList();
        if (null == list || list.isEmpty()) return true;
        return list.contains(packetName);
    }

    private void updateSyncMessage(DeviceSettings settings) {
        if (null == settings || isInvalid()) return;
        SettingsAdapter.updateSettingSwitch(mBinding.viewMessagePush, new SettingsSwitch(getString(R.string.message_push), 0,
                settings.isSyncMessage()).setListener((buttonView, isChecked) -> mViewModel.enableSyncMessage(isChecked)));

        List<SettingsSwitch> list = new ArrayList<>();
        list.add(new SettingsSwitch(getString(R.string.app_sms), R.drawable.ic_message,
                isExistPacketName(settings, NotificationUtil.PACKAGE_NAME_SYS_MESSAGE))
                .setListener((buttonView, isChecked) -> mViewModel.handleAppPacketName(NotificationUtil.PACKAGE_NAME_SYS_MESSAGE, isChecked)));
        list.add(new SettingsSwitch(getString(R.string.app_wechat), R.drawable.ic_wechat,
                isExistPacketName(settings, NotificationUtil.PACKAGE_NAME_WECHAT))
                .setListener((buttonView, isChecked) -> mViewModel.handleAppPacketName(NotificationUtil.PACKAGE_NAME_WECHAT, isChecked)));
        list.add(new SettingsSwitch(getString(R.string.app_qq), R.drawable.ic_qq,
                isExistPacketName(settings, NotificationUtil.PACKAGE_NAME_QQ))
                .setListener((buttonView, isChecked) -> mViewModel.handleAppPacketName(NotificationUtil.PACKAGE_NAME_QQ, isChecked)));
        list.add(new SettingsSwitch(getString(R.string.app_ding_talk), R.drawable.ic_dingtalk,
                isExistPacketName(settings, NotificationUtil.PACKAGE_NAME_DING_TALK))
                .setListener((buttonView, isChecked) -> mViewModel.handleAppPacketName(NotificationUtil.PACKAGE_NAME_DING_TALK, isChecked)));
        list.add(new SettingsSwitch(getString(R.string.app_lark), R.drawable.ic_feishu,
                isExistPacketName(settings, NotificationUtil.PACKAGE_NAME_LARK))
                .setListener((buttonView, isChecked) -> mViewModel.handleAppPacketName(NotificationUtil.PACKAGE_NAME_LARK, isChecked)));

        mAdapter.setList(list);
    }

}