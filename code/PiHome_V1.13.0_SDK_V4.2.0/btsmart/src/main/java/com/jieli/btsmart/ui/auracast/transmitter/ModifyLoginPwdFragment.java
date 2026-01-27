package com.jieli.btsmart.ui.auracast.transmitter;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.auracast.AuracastLoginInfo;
import com.jieli.btsmart.data.model.auracast.AuracastQRCode;
import com.jieli.btsmart.databinding.FragmentModifyLoginPwdBinding;
import com.jieli.btsmart.tool.configure.ConfigureKit;
import com.jieli.btsmart.ui.auracast.AuracastAssistantViewModel;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.widget.dialog.SaveQRCodeDialog;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.jl_dialog.Jl_Dialog;

/**
 * ModifyLoginPwdFragment
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/24
 * note: 修改登录密码界面
 */
public class ModifyLoginPwdFragment extends DeviceControlFragment {

    private FragmentModifyLoginPwdBinding binding;
    private AuracastTransmitterViewModel viewModel;

    private boolean isShowPwd;
    /**
     * 保存二维码弹窗
     */
    private SaveQRCodeDialog saveQRCodeDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentModifyLoginPwdBinding.inflate(inflater, container, false);
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
        updatePwdState(isShowPwd);
        updateModifyPwdBtnUI();
    }

    private void initUI() {
        binding.ivPwdState.setOnClickListener(v -> {
            isShowPwd = !isShowPwd;
            updatePwdState(isShowPwd);
        });
        binding.etInputPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateModifyPwdBtnUI();
            }
        });
        binding.btnModifyPwd.setOnClickListener(v -> {
            String password = binding.etInputPassword.getText().toString().trim();
            if (!ConfigureKit.isValidPassword(password)) {
                showTips(getString(R.string.login_pwd_len_err_tips));
                return;
            }
            viewModel.modifyLoginPwd(password);
        });
        binding.btnCreateQrCode.setOnClickListener(v -> {
            BluetoothDevice device = viewModel.getDevice();
            String name = UIHelper.getDevName(device);
            String password = ConfigureKit.getInstance().getAuracastLoginPassword(viewModel.getMac());
            if (null == password) {
                password = "";
            }
            showSaveQrCodeDialog(name, password);
        });
    }

    private void addObserver() {
        viewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnection -> {
            if (!BluetoothUtil.deviceEquals(deviceConnection.getDevice(), viewModel.getDevice()))
                return;
            if (deviceConnection.getStatus() != StateCode.CONNECTION_OK) {
                finish();
            }
        });
        viewModel.opResultMLD.observe(getViewLifecycleOwner(), opResult -> {
            if (null == opResult || (opResult.getOp() != AuracastAssistantViewModel.OP_MODIFY_LOGIN_PWD &&
                    opResult.getOp() != AuracastAssistantViewModel.OP_REBOOT))
                return;
            if (opResult.isSuccess()) { //设置成功
                if (opResult.getOp() == AuracastAssistantViewModel.OP_REBOOT) {
                    viewModel.disconnectDevice();
                } else if (opResult.getOp() == AuracastAssistantViewModel.OP_MODIFY_LOGIN_PWD) {
                    showRebootTipsDialog();
                }
                return;
            }
            showTips(CommonUtil.formatString("%s\n%s : %s, %s", getString(R.string.operation_failed, viewModel.getOpString(opResult.getOp())),
                    getString(R.string.error_code), CommonUtil.formatInt(opResult.getCode()), opResult.getMessage()));
        });
    }

    private void updatePwdState(boolean isShowPwd) {
        if (isShowPwd) {
            binding.etInputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            binding.etInputPassword.setSelection(binding.etInputPassword.getText().length());
            binding.ivPwdState.setImageResource(R.drawable.ic_show_pwd_gray);
            return;
        }
        binding.etInputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        binding.etInputPassword.setSelection(binding.etInputPassword.getText().length());
        binding.ivPwdState.setImageResource(R.drawable.ic_hide_pwd_gray);
    }

    private void updateModifyPwdBtnUI() {
        String password = binding.etInputPassword.getText().toString().trim();
        byte[] buf = password.getBytes();
        boolean isValidPwd = buf.length >= 6 && buf.length <= 32;
        binding.btnModifyPwd.setClickable(isValidPwd);
        binding.btnModifyPwd.setBackgroundResource(isValidPwd ? R.drawable.bg_btn_c8_purple_gray_selector : R.drawable.bg_btn_c8_gray_shape);
    }

    private void showRebootTipsDialog() {
        if (isInvalid()) return;
        new Jl_Dialog.Builder()
                .title(getString(R.string.tips))
                .content(getString(R.string.modify_login_pwd_successfully))
                .left(getString(R.string.confirm))
                .leftColor(ContextCompat.getColor(requireContext(), R.color.blue_448EFF))
                .leftClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                    viewModel.reboot();
                }).cancel(false)
                .build().show(getChildFragmentManager(), "TipsDialog");
    }

    private void showSaveQrCodeDialog(String name, String password) {
        if (isInvalid()) return;
        dismissSaveQrCodeDialog();
        AuracastLoginInfo loginInfo = new AuracastLoginInfo()
                .setDeviceName(name)
                .setLoginPassword(password);
        new SaveQRCodeDialog.Builder(viewModel.getMac(),
                name, password, loginInfo.toString())
                .setTips(getString(R.string.tips_save_qr_code_file))
                .setCallback(result -> {

                }).build().show(getChildFragmentManager(), SaveQRCodeDialog.class.getSimpleName());
    }

    private void dismissSaveQrCodeDialog() {
        if (isInvalid() || null == saveQRCodeDialog) return;
        if (saveQRCodeDialog.isShow()) {
            saveQRCodeDialog.dismiss();
        }
        saveQRCodeDialog = null;
    }
}