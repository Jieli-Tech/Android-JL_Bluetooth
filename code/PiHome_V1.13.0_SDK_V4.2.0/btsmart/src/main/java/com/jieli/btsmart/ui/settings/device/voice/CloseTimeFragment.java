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
import com.jieli.btsmart.databinding.FragmentCloseTimeBinding;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动关闭时间设置界面
 */
public class CloseTimeFragment extends DeviceControlFragment {

    private FragmentCloseTimeBinding mBinding;
    private SmartNoPickViewModel mViewModel;
    private VoiceSettingAdapter mAdapter;

    public static CloseTimeFragment newInstance() {
        return new CloseTimeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentCloseTimeBinding.inflate(inflater, container, false);
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
            mAdapter.updateSelectedPosByVoiceId(smartNoPick.getCloseTime());
        }
    }

    private void initUI() {
        if (requireActivity() instanceof CommonActivity) {
            ((CommonActivity) requireActivity()).updateTopBar(getString(R.string.close_time), R.drawable.ic_back_black, v -> requireActivity().finish(), 0, null);
        }
        mAdapter = new VoiceSettingAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            VoiceSetting setting = mAdapter.getItem(position);
            if (null == setting || mAdapter.isSelectedItem(setting)) return;
            SmartNoPick param = new SmartNoPick();
            param.setCloseTime(setting.getId());
            mViewModel.changeSmartNoPick(param);
        });
        mBinding.rvCloseTimeList.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.rvCloseTimeList.setAdapter(mAdapter);
        List<VoiceSetting> list = new ArrayList<>();
        String[] array = getResources().getStringArray(R.array.close_time_list);
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
        mViewModel.mSmartNoPickMLD.observe(getViewLifecycleOwner(), smartNoPick -> mAdapter.updateSelectedPosByVoiceId(smartNoPick.getCloseTime()));
    }
}