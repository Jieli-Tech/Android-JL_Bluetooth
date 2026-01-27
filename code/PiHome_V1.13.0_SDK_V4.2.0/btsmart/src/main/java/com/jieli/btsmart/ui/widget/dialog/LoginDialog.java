package com.jieli.btsmart.ui.widget.dialog;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.JsonSyntaxException;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.auracast.AuracastLoginInfo;
import com.jieli.btsmart.databinding.DialogLoginBinding;
import com.jieli.btsmart.tool.configure.ConfigureKit;
import com.jieli.btsmart.ui.auracast.AuracastAssistantViewModel;
import com.jieli.btsmart.ui.auracast.transmitter.AuracastTransmitterViewModel;
import com.jieli.btsmart.ui.qrcode.QRCodeScanActivity;
import com.jieli.btsmart.util.UIHelper;
import com.king.camera.scan.CameraScan;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.PermissionUtils;
import permissions.dispatcher.RuntimePermissions;

/**
 * LoginDialog
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/24
 * note: 登录弹窗
 */
@RuntimePermissions
public class LoginDialog extends CommonDialog {

    private DialogLoginBinding binding;
    private AuracastTransmitterViewModel viewModel;
    private boolean isShowPwd = false;

    private final ActivityResultLauncher<Intent> qrCodeLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK) return;
                Intent intent = result.getData();
                if (null == intent) return;
                String content = intent.getStringExtra(CameraScan.SCAN_RESULT);
                try {
                    AuracastLoginInfo loginInfo = ConfigureKit.GSON.fromJson(content, AuracastLoginInfo.class);
                    if (null != loginInfo && TextUtils.equals(loginInfo.getDeviceName(), UIHelper.getDevName(viewModel.getDevice()))) {
                        tryToLogin(loginInfo.getLoginPassword());
                        return;
                    }
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
                showTips(getString(R.string.wrong_pwd));
            });

    protected LoginDialog(@NonNull Builder builder) {
        super(builder);
    }

    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!(mBuilder instanceof Builder)) {
            dismiss();
            return;
        }
        Builder builder = (Builder) mBuilder;
        BluetoothDevice device = builder.getDevice();
        if (null == device) {
            dismiss();
            return;
        }
        viewModel = new ViewModelProvider(this, new AuracastTransmitterViewModel.Factory(device))
                .get(AuracastTransmitterViewModel.class);
        initUI();
        addObserver();
        if (viewModel.isLogin()) {
            final OnResultCallback<Boolean> callback = builder.getCallback();
            if (null != callback) {
                callback.onResult(true);
            }
            dismiss();
            return;
        }
        viewModel.autoLogin();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LoginDialogPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission({Manifest.permission.CAMERA})
    public void onCameraPermissionGrant() {
        disPermissionTipsDialog();
        qrCodeLauncher.launch(new Intent(requireContext(), QRCodeScanActivity.class));
    }

    @NeedsPermission({Manifest.permission.CAMERA})
    public void onCameraPermissionShowRationale(PermissionRequest request) {
        disPermissionTipsDialog();
        if (null != request) request.proceed();
    }

    @NeedsPermission({Manifest.permission.CAMERA})
    public void onCameraPermissionDenied() {
        disPermissionTipsDialog();
        UIHelper.showAppSettingDialog(LoginDialog.this, getString(R.string.camera_permission_denied_tips));
    }

    private void initUI() {
        binding.etInputPwd.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLoginBtnUI();
            }
        });
        binding.btnPwdState.setOnClickListener(v -> {
            isShowPwd = !isShowPwd;
            updatePwdState(isShowPwd);
        });
        binding.tvLoginScanCode.setOnClickListener(v -> tryToScanQRCode());
        binding.btnLogin.setOnClickListener(v -> {
            String password = binding.etInputPwd.getText().toString().trim();
            tryToLogin(password);
        });

        updatePwdState(isShowPwd);
        updateLoginBtnUI();
    }

    private void addObserver() {
        if (!(mBuilder instanceof Builder)) return;
        final Builder builder = (Builder) mBuilder;
        final OnResultCallback<Boolean> callback = builder.getCallback();
        viewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnection -> {
            if (!BluetoothUtil.deviceEquals(deviceConnection.getDevice(), viewModel.getDevice()))
                return;
            if (deviceConnection.getStatus() != StateCode.CONNECTION_OK) {
                if (null != callback) callback.onResult(false);
                dismiss();
            }
        });
        viewModel.loginStateMLD.observe(getViewLifecycleOwner(), loginState -> {
            if (null != callback) callback.onResult(loginState);
            dismiss();
        });
        viewModel.opResultMLD.observe(getViewLifecycleOwner(), opResult -> {
            if (null == opResult || opResult.getOp() != AuracastAssistantViewModel.OP_LOGIN) return;
            boolean isAutoLogin = (boolean) opResult.getData();
            if (isAutoLogin && opResult.isSuccess()) {
                showTips(getString(R.string.auto_login_successful));
                return;
            }
            if (!isAutoLogin && !opResult.isSuccess()) {
                showTips(CommonUtil.formatString("%s\n%s : %s, %s", getString(R.string.operation_failed, viewModel.getOpString(opResult.getOp())),
                        getString(R.string.error_code), CommonUtil.formatInt(opResult.getCode()), opResult.getMessage()));
            }
        });
    }

    private void updatePwdState(boolean isShowPwd) {
        if (isShowPwd) {
            binding.etInputPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            binding.etInputPwd.setSelection(binding.etInputPwd.getText().length());
            binding.btnPwdState.setImageResource(R.drawable.ic_show_pwd_gray);
            return;
        }
        binding.etInputPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        binding.etInputPwd.setSelection(binding.etInputPwd.getText().length());
        binding.btnPwdState.setImageResource(R.drawable.ic_hide_pwd_gray);
    }

    private void updateLoginBtnUI() {
        String password = binding.etInputPwd.getText().toString().trim();
        boolean isValidPwd = ConfigureKit.isValidPassword(password);
        binding.btnLogin.setClickable(isValidPwd);
        binding.btnLogin.setBackgroundResource(isValidPwd ? R.drawable.bg_btn_c28_purple_gray_selector : R.drawable.bg_btn_c28_purple_shape);
    }

    private void tryToLogin(String password) {
        if (!ConfigureKit.isValidPassword(password)) {
            showTips(getString(R.string.login_pwd_len_err_tips));
            return;
        }
        viewModel.login(password);
    }

    private void tryToScanQRCode() {
        if (!PermissionUtils.hasSelfPermissions(requireContext(), Manifest.permission.CAMERA)) {
            showPermissionTipsDialog(getString(R.string.camera_permission_desc));
        }
        LoginDialogPermissionsDispatcher.onCameraPermissionGrantWithPermissionCheck(this);
    }

    public static class Builder extends CommonDialog.Builder {

        private final BluetoothDevice device;
        private OnResultCallback<Boolean> callback;

        public Builder(BluetoothDevice device) {
            this.device = device;
            setWidthRate(0.8f);
        }

        public BluetoothDevice getDevice() {
            return device;
        }

        public OnResultCallback<Boolean> getCallback() {
            return callback;
        }

        public Builder setCallback(OnResultCallback<Boolean> callback) {
            this.callback = callback;
            return this;
        }

        @Override
        public LoginDialog build() {
            return new LoginDialog(this);
        }
    }
}
