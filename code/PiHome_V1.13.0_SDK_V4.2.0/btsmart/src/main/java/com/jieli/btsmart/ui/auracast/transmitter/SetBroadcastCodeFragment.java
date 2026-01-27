package com.jieli.btsmart.ui.auracast.transmitter;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.bluetooth.bean.auracast.transmitter.EncryptionSettings;
import com.jieli.bluetooth.bean.auracast.transmitter.TransmitterSettings;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.FragmentSetBroadcastCodeBinding;
import com.jieli.btsmart.ui.auracast.AuracastAssistantViewModel;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.widget.dialog.InputCodeDialog;

/**
 * SetBroadcastCodeFragment
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/24
 * note: 设置Auracast广播密钥界面
 */
public class SetBroadcastCodeFragment extends DeviceControlFragment {

    private FragmentSetBroadcastCodeBinding binding;
    private AuracastTransmitterViewModel viewModel;

    /**
     * 是否显示密钥
     */
    private boolean isShowPwd;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSetBroadcastCodeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = AuracastTransmitterFragment.viewModel;
        if (null == viewModel) {
            finish();
            return;
        }
        initUI();
        addObserver();
        final TransmitterSettings settings = viewModel.getSettingsInfo();
        if (null == settings || !settings.hasType(TransmitterSettings.TYPE_ENCRYPTION_SETTINGS)) {
            viewModel.readSettingsInfo();
        } else {
            updateEncryptionSettings(settings.getEncryptionSettings());
        }
    }

    private void initUI() {
        binding.ivPwdState.setOnClickListener(v -> {
            isShowPwd = !isShowPwd;
            updatePwdState(isShowPwd);
        });
        binding.btnSetCode.setOnClickListener(v -> {
            String code = binding.etInputBroadcastCode.getText().toString().trim();
            if (!InputCodeDialog.isValidBroadcastCode(code)) {
                showTips(getString(R.string.pwd_len_err_tips));
                return;
            }
            viewModel.setEncryptionSettings(new EncryptionSettings(true, code.getBytes()));
        });
        binding.etInputBroadcastCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSetCodeBtnUI();
            }
        });
        updatePwdState(isShowPwd);
        updateSetCodeBtnUI();
    }

    private void addObserver() {
        viewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnection -> {
            if (!BluetoothUtil.deviceEquals(deviceConnection.getDevice(), viewModel.getDevice()))
                return;
            if (deviceConnection.getStatus() != StateCode.CONNECTION_OK) {
                finish();
            }
        });
        viewModel.loginStateMLD.observe(getViewLifecycleOwner(), loginState -> {
            if (!loginState) {
                finish();
            }
        });
        viewModel.transmitterSettingsMLD.observe(getViewLifecycleOwner(), settings -> {
            if (null == settings) return;
            updateEncryptionSettings(settings.getEncryptionSettings());
        });
        viewModel.opResultMLD.observe(getViewLifecycleOwner(), opResult -> {
            if (null == opResult || opResult.getOp() != AuracastAssistantViewModel.OP_SET_ENCRYPTION_SETTINGS)
                return;
            if (opResult.isSuccess()) { //设置成功
                finish();
                return;
            }
            showTips(CommonUtil.formatString("%s\n%s : %s, %s", getString(R.string.operation_failed, viewModel.getOpString(opResult.getOp())),
                    getString(R.string.error_code), CommonUtil.formatInt(opResult.getCode()), opResult.getMessage()));
        });
    }

    private void updatePwdState(boolean isShowPwd) {
        if (isShowPwd) {
            binding.etInputBroadcastCode.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            binding.etInputBroadcastCode.setSelection(binding.etInputBroadcastCode.getText().length());
            binding.ivPwdState.setImageResource(R.drawable.ic_show_pwd_gray);
            return;
        }
        binding.etInputBroadcastCode.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        binding.etInputBroadcastCode.setSelection(binding.etInputBroadcastCode.getText().length());
        binding.ivPwdState.setImageResource(R.drawable.ic_hide_pwd_gray);
    }

    private void updateSetCodeBtnUI() {
        String code = binding.etInputBroadcastCode.getText().toString().trim();
        boolean isValidBroadcastCode = InputCodeDialog.isValidBroadcastCode(code);
        binding.btnSetCode.setClickable(isValidBroadcastCode);
        binding.btnSetCode.setBackgroundResource(isValidBroadcastCode ? R.drawable.bg_btn_c8_purple_gray_selector : R.drawable.bg_btn_c8_gray_shape);
    }

    private void updateEncryptionSettings(EncryptionSettings settings) {
        if (isInvalid() || null == settings) return;
        try {
            String code = settings.isInValidBroadcastCode() ? "" : new String(settings.getBroadcastCode());
            binding.etInputBroadcastCode.setText(code);
            binding.etInputBroadcastCode.setSelection(code.length());
            binding.btnSetCode.setText(getString(code.isEmpty() ? R.string.confirm : R.string.modify));
            updateSetCodeBtnUI();
        } catch (Exception ignore) {

        }
    }

    private void showTipsDialog() {
        if (isInvalid()) return;

    }
}