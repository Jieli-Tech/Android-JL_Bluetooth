package com.jieli.btsmart.ui.settings.device.voice;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.bean.device.voice.SmartNoPick;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.FragmentSmartNoPickBinding;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;

import org.jetbrains.annotations.Contract;

/**
 * 智能免摘界面
 */
public class SmartNoPickFragment extends DeviceControlFragment {

    private static final int REQUEST_CODE = 0x1111;
    private FragmentSmartNoPickBinding mBinding;
    private SmartNoPickViewModel mViewModel;

    @NonNull
    @Contract(" -> new")
    public static SmartNoPickFragment newInstance() {
        return new SmartNoPickFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentSmartNoPickBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(SmartNoPickViewModel.class);
        initUI();
        addObserver();
        SmartNoPick smartNoPick = mViewModel.getSmartNoPick();
        if (null == smartNoPick) {
            mViewModel.querySmartNoPick();
        } else {
            updateSmartNoPick(smartNoPick);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            mViewModel.querySmartNoPick();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    private void initUI() {
        if (requireActivity() instanceof CommonActivity) {
            ((CommonActivity) requireActivity()).updateTopBar(getString(R.string.smart_no_pick), R.drawable.ic_back_black, v -> requireActivity().finish(), 0, null);
        }
        mBinding.tvVoiceCheckSensitivityValue.setOnClickListener(v -> CommonActivity.startCommonActivity(SmartNoPickFragment.this, REQUEST_CODE, VoiceCheckSensitivityFragment.class.getCanonicalName(), null));
        mBinding.tvCloseTimeValue.setOnClickListener(v -> CommonActivity.startCommonActivity(SmartNoPickFragment.this, REQUEST_CODE, CloseTimeFragment.class.getCanonicalName(), null));
    }

    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnection -> {
            if (deviceConnection.getStatus() != StateCode.CONNECTION_OK) {
                requireActivity().finish();
            }
        });
        mViewModel.mSmartNoPickMLD.observe(getViewLifecycleOwner(), this::updateSmartNoPick);
    }

    private void updateSmartNoPick(SmartNoPick param) {
        if (null == param || isDetached() || !isAdded()) return;
        mBinding.tvVoiceCheckSensitivityValue.setText(getVoiceCheckSensitivity(param.getSensitivity()));
        mBinding.tvCloseTimeValue.setText(getCloseTime(param.getCloseTime()));
    }

    @NonNull
    private String getVoiceCheckSensitivity(int value) {
        switch (value) {
            case SmartNoPick.SENSITIVITY_HIGH:
                return getString(R.string.high_level);
            case SmartNoPick.SENSITIVITY_LOW:
                return getString(R.string.low_level);
            default:
                return "";
        }
    }

    @NonNull
    private String getCloseTime(int value) {
        switch (value) {
            case SmartNoPick.CLOSE_TIME_SHORT:
                return getString(R.string.short_time);
            case SmartNoPick.CLOSE_TIME_STANDARD:
                return getString(R.string.standard_time);
            case SmartNoPick.CLOSE_TIME_LONG:
                return getString(R.string.long_time);
            case SmartNoPick.CLOSE_TIME_NONE:
                return getString(R.string.none_time);
            default:
                return "";
        }
    }
}