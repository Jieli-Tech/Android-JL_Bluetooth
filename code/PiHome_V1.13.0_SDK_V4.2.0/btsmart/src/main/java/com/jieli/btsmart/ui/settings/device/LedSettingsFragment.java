package com.jieli.btsmart.ui.settings.device;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.adapter.LedSettingsAdapter;
import com.jieli.btsmart.data.model.settings.LedBean;
import com.jieli.btsmart.databinding.FragmentLedSetingsBinding;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.UIHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 闪灯设置界面
 */
public class LedSettingsFragment extends DeviceControlFragment {

    private FragmentLedSetingsBinding mBinding;
    private DeviceSettingsViewModel mViewModel;
    private LedSettingsAdapter mAdapter;

    public static LedSettingsFragment newInstance() {
        return new LedSettingsFragment();
    }

    private final ActivityResultLauncher<Intent> mLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> mViewModel.syncDeviceSettings());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentLedSetingsBinding.inflate(inflater, container, false);
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
        final ADVInfoResponse advInfo = mViewModel.getDeviceSettingInfo();
        if (advInfo == null) {
            mViewModel.syncDeviceSettings();
        } else {
            updateLedSettingsFromADVInfo(advInfo);
        }
    }

    private void initUI() {
        if (requireActivity() instanceof CommonActivity) {
            ((CommonActivity) requireActivity()).updateTopBar(getString(R.string.led_settings),
                    R.drawable.ic_back_black, v -> finish(), 0, null);
        }
        mAdapter = new LedSettingsAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            final LedBean item = mAdapter.getItem(position);
            if (item == null) return;
            Bundle bundle = new Bundle();
            bundle.putParcelable(SConstant.KEY_DEVICE, mViewModel.mDevice);
            bundle.putParcelable(SConstant.KEY_DEV_LED_BEAN, item);
            CommonActivity.startActivityForRequest(LedSettingsFragment.this,
                    DevSettingsDetailsFragment.class.getCanonicalName(), bundle, mLauncher);
        });
        mBinding.rvLedSettingsFunc.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.rvLedSettingsFunc.setAdapter(mAdapter);
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
        mViewModel.mDeviceSettingInfoMLD.observe(getViewLifecycleOwner(), this::updateLedSettingsFromADVInfo);
    }

    private void updateLedSettingsFromADVInfo(ADVInfoResponse advInfo) {
        updateLedSettings(getLedListFromADVInfo(advInfo));
    }

    private List<LedBean> getLedListFromADVInfo(ADVInfoResponse advInfo) {
        if (advInfo == null) return new ArrayList<>();
        List<ADVInfoResponse.LedSettings> ledList = advInfo.getLedSettingsList();
        if (ledList == null) return new ArrayList<>();
        List<LedBean> list = new ArrayList<>();
        for (int i = 0; i < ledList.size(); i++) {
            ADVInfoResponse.LedSettings ledSettings = ledList.get(i);
            int sceneId = ledSettings.getScene();
            String scene = ProductUtil.getLedSettingsName(requireContext(), advInfo.getVid(),
                    advInfo.getUid(), advInfo.getPid(), SConstant.KEY_FIELD_LED_SCENE, sceneId);
            String effect = ProductUtil.getLedSettingsName(requireContext(), advInfo.getVid(),
                    advInfo.getUid(), advInfo.getPid(), SConstant.KEY_FIELD_LED_EFFECT, ledSettings.getEffect());
            LedBean ledBean = new LedBean(ledSettings.getScene(), scene, ledSettings.getEffect(), effect);
            if (sceneId == 1 || sceneId == 4 || sceneId == 6) {
                ledBean.setItemType(LedBean.ITEM_TYPE_ONE);
            } else if (sceneId == 3 || sceneId == 5 || sceneId == 7) {
                ledBean.setItemType(LedBean.ITEM_TYPE_THREE);
            } else {
                ledBean.setItemType(LedBean.ITEM_TYPE_TWO);
            }
            if (i == 0 && ledBean.getItemType() != LedBean.ITEM_TYPE_ONE) {
                ledBean.setItemType(LedBean.ITEM_TYPE_ONE);
            }
            list.add(ledBean);
        }
        return list;
    }

    private void updateLedSettings(List<LedBean> list) {
        if (isInvalid()) return;
        if (list == null) list = new ArrayList<>();
        if (list.isEmpty()) {
            UIHelper.gone(mBinding.rvLedSettingsFunc);
            return;
        }
        UIHelper.show(mBinding.rvLedSettingsFunc);
        mAdapter.setList(list);
    }
}
