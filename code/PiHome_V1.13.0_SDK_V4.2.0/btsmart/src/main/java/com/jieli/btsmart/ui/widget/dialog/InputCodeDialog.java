package com.jieli.btsmart.ui.widget.dialog;

import android.Manifest;
import android.app.Activity;
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
import androidx.core.content.ContextCompat;

import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.auracast.AuracastQRCode;
import com.jieli.btsmart.databinding.DialogInputCodeBinding;
import com.jieli.btsmart.ui.qrcode.QRCodeScanActivity;
import com.jieli.btsmart.util.UIHelper;
import com.king.camera.scan.CameraScan;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.PermissionUtils;
import permissions.dispatcher.RuntimePermissions;

/**
 * InputCodeDialog
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/20
 * note: 输入密码对话框
 */
@RuntimePermissions
public class InputCodeDialog extends CommonDialog {

    /**
     * 是否有效的广播密钥
     *
     * @param code String 广播密钥
     * @return boolean 结果
     */
    public static boolean isValidBroadcastCode(String code) {
        if (null == code) return false;
        int size = code.getBytes().length;
        return size <= 16;
    }

    private DialogInputCodeBinding binding;
    private boolean isShowPwd = false;

    private final ActivityResultLauncher<Intent> qrCodeLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK) return;
                Intent intent = result.getData();
                if (null == intent) return;
                String content = intent.getStringExtra(CameraScan.SCAN_RESULT);
                AuracastQRCode qrCode = AuracastQRCode.parseContent(content);
                if (null == qrCode) {
                    showTips(getString(R.string.qr_code_error_tips));
                    return;
                }
                if (!(mBuilder instanceof Builder)) return;
                final Builder builder = (Builder) mBuilder;
                if (TextUtils.equals(builder.getBroadcastName(), qrCode.getName())) {
                    binding.etInputCode.setText(qrCode.getCode());
                    binding.etInputCode.setSelection(binding.etInputCode.getText().length());
                    if (builder.getCallback() != null) {
                        builder.getCallback().onResult(qrCode.getCode());
                    }
                    dismiss();
                    return;
                }
                showTips(getString(R.string.qr_code_error_tips));
            });

    protected InputCodeDialog(@NonNull Builder builder) {
        super(builder);
    }

    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogInputCodeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI();
        updatePwdState(isShowPwd);
        updateConnectBtnUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disPermissionTipsDialog();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        InputCodeDialogPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
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
        UIHelper.showAppSettingDialog(InputCodeDialog.this, getString(R.string.camera_permission_denied_tips));
    }

    private void initUI() {
        if (!(mBuilder instanceof Builder)) return;
        final Builder builder = (Builder) mBuilder;
        binding.btnPwdState.setOnClickListener(v -> {
            isShowPwd = !isShowPwd;
            updatePwdState(isShowPwd);
        });
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSure.setOnClickListener(v -> {
            String code = binding.etInputCode.getText().toString().trim();
            if (!isValidBroadcastCode(code)) {
                showTips(getString(R.string.pwd_len_err_tips));
                return;
            }
            if (null != builder.callback) {
                builder.callback.onResult(code);
            }
            dismiss();
        });
        binding.tvScanQrCode.setOnClickListener(v -> tryToScanQRCode());
        binding.etInputCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateConnectBtnUI();
            }
        });
        binding.etInputCode.setHint(getString(R.string.hint_input_password, builder.getBroadcastName()));
    }

    private void updatePwdState(boolean isShowPwd) {
        if (isShowPwd) {
            binding.etInputCode.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            binding.etInputCode.setSelection(binding.etInputCode.getText().length());
            binding.btnPwdState.setImageResource(R.drawable.ic_show_pwd_gray);
            return;
        }
        binding.etInputCode.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        binding.etInputCode.setSelection(binding.etInputCode.getText().length());
        binding.btnPwdState.setImageResource(R.drawable.ic_hide_pwd_gray);
    }

    private void updateConnectBtnUI() {
        String code = binding.etInputCode.getText().toString().trim();
        boolean isValidCode = isValidBroadcastCode(code);
        binding.btnSure.setClickable(isValidCode);
        binding.btnSure.setTextColor(ContextCompat.getColor(requireContext(), isValidCode ? R.color.blue_448eff : R.color.black_66000000));
    }

    private void tryToScanQRCode() {
        if (!PermissionUtils.hasSelfPermissions(requireContext(), Manifest.permission.CAMERA)) {
            showPermissionTipsDialog(getString(R.string.camera_permission_desc));
        }
        InputCodeDialogPermissionsDispatcher.onCameraPermissionGrantWithPermissionCheck(this);
    }

    public static class Builder extends CommonDialog.Builder {

        private String broadcastName;
        private OnResultCallback<String> callback;

        public Builder() {
            broadcastName = "";
        }

        public String getBroadcastName() {
            return broadcastName;
        }

        public Builder setBroadcastName(String broadcastName) {
            this.broadcastName = broadcastName;
            return this;
        }

        public OnResultCallback<String> getCallback() {
            return callback;
        }

        public Builder setCallback(OnResultCallback<String> callback) {
            this.callback = callback;
            return this;
        }

        @Override
        public InputCodeDialog build() {
            return new InputCodeDialog(this);
        }
    }
}
