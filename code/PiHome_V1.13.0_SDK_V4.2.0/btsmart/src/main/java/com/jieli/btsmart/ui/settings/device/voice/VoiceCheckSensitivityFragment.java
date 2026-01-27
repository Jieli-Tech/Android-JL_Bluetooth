package com.jieli.btsmart.ui.settings.device.voice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jieli.bluetooth.bean.device.voice.SmartNoPick;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.FragmentVoiceCheckSensitivityBinding;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * 语音检测灵敏度设置
 */
public class VoiceCheckSensitivityFragment extends DeviceControlFragment {
    private FragmentVoiceCheckSensitivityBinding mBinding;
    private SmartNoPickViewModel mViewModel;
    private VoiceSettingAdapter mAdapter;

    public static VoiceCheckSensitivityFragment newInstance() {
        return new VoiceCheckSensitivityFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentVoiceCheckSensitivityBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(SmartNoPickViewModel.class);
        initUI();
        addObserver();
        SmartNoPick smartNoPick = mViewModel.getSmartNoPick();
        if (smartNoPick == null) {
            mViewModel.querySmartNoPick();
        } else {
            mAdapter.updateSelectedPosByVoiceId(smartNoPick.getSensitivity());
        }
    }

    private void initUI() {
        if (requireActivity() instanceof CommonActivity) {
            ((CommonActivity) requireActivity()).updateTopBar(getString(R.string.voice_check_sensitivity), R.drawable.ic_back_black, v -> requireActivity().finish(), 0, null);
        }
        mAdapter = new VoiceSettingAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            VoiceSetting setting = mAdapter.getItem(position);
            if (null == setting || mAdapter.isSelectedItem(setting)) return;
            SmartNoPick param = new SmartNoPick();
            param.setSensitivity(setting.getId());
            mViewModel.changeSmartNoPick(param);
        });
        mBinding.rvVoiceCheckSensitivityList.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.rvVoiceCheckSensitivityList.setAdapter(mAdapter);
        List<VoiceSetting> list = new ArrayList<>();
        String[] array = getResources().getStringArray(R.array.voice_check_sensitivity_list);
        for (int i = 0; i < array.length; i++) {
            list.add(new VoiceSetting(i, array[i]));
        }
        mAdapter.setList(list);
    }

    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnection -> {
            if (deviceConnection.getStatus() != StateCode.CONNECTION_OK) {
                requireActivity().finish();
            }
        });
        mViewModel.mSmartNoPickMLD.observe(getViewLifecycleOwner(), smartNoPick -> mAdapter.updateSelectedPosByVoiceId(smartNoPick.getSensitivity()));
    }

}