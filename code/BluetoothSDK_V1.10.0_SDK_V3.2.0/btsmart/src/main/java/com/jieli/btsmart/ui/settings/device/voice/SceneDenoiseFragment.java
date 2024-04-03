package com.jieli.btsmart.ui.settings.device.voice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jieli.bluetooth.bean.device.voice.SceneDenoising;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.FragmentSceneDenoiseBinding;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * 场景降噪界面
 */
public class SceneDenoiseFragment extends DeviceControlFragment {

    private FragmentSceneDenoiseBinding mBinding;
    private SceneDenoiseViewModel mViewModel;
    private VoiceSettingAdapter mAdapter;


    public static SceneDenoiseFragment newInstance() {
        return new SceneDenoiseFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentSceneDenoiseBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(SceneDenoiseViewModel.class);
        initUI();
        addObserver();
        SceneDenoising sceneDenoising = mViewModel.getSceneDenoising();
        if (null == sceneDenoising) {
            mViewModel.querySceneDenoising();
        } else {
            mAdapter.updateSelectedPosByVoiceId(sceneDenoising.getMode());
        }
    }

    private void initUI() {
        if (requireActivity() instanceof CommonActivity) {
            ((CommonActivity) requireActivity()).updateTopBar(getString(R.string.scene_denoising), R.drawable.ic_back_black, v -> requireActivity().finish(), 0, null);
        }
        mAdapter = new VoiceSettingAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            VoiceSetting setting = mAdapter.getItem(position);
            if (null == setting || mAdapter.isSelectedItem(setting)) return;
            SceneDenoising param = new SceneDenoising();
            param.setMode(setting.getId());
            mViewModel.changeSceneDenoising(param);
        });
        mBinding.rvSceneDenoiseList.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.rvSceneDenoiseList.setAdapter(mAdapter);
        List<VoiceSetting> list = new ArrayList<>();
        String[] array = getResources().getStringArray(R.array.scene_denoise_list);
        String[] descs = getResources().getStringArray(R.array.scene_denoise_desc_list);
        for (int i = 0; i < array.length; i++) {
            VoiceSetting setting = new VoiceSetting(i, array[i]);
            setting.setDesc(descs[i]);
            list.add(setting);
        }
        mAdapter.setList(list);
    }

    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnection -> {
            if (deviceConnection.getStatus() != StateCode.CONNECTION_OK) {
                requireActivity().finish();
            }
        });
        mViewModel.mSceneDenoisingMLD.observe(getViewLifecycleOwner(), sceneDenoising -> {
            JL_Log.d(TAG, ">>>>>>>>>>>>> " + sceneDenoising);
            mAdapter.updateSelectedPosByVoiceId(sceneDenoising.getMode());
        });
    }
}