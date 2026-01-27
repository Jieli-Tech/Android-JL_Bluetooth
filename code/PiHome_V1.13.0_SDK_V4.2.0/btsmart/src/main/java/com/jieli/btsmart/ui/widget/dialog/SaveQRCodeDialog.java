package com.jieli.btsmart.ui.widget.dialog;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.auracast.AuracastLoginInfo;
import com.jieli.btsmart.databinding.DialogSaveQrCodeBinding;
import com.jieli.btsmart.util.UIHelper;
import com.king.zxing.util.CodeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.PermissionUtils;
import permissions.dispatcher.RuntimePermissions;

/**
 * SaveQRCodeDialog
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/25
 * note: 保存二维码弹窗
 */
@RuntimePermissions
public class SaveQRCodeDialog extends CommonDialog {

    private DialogSaveQrCodeBinding binding;

    private final ExecutorService workThread = Executors.newSingleThreadExecutor();
    private Bitmap bitmap;

    protected SaveQRCodeDialog(@NonNull Builder builder) {
        super(builder);
    }

    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogSaveQrCodeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
        workThread.shutdownNow();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        SaveQRCodeDialogPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission(value = {Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public void checkWritePermission(Bitmap bitmap, String fileName) {
        disPermissionTipsDialog();
        tryToSaveImage(bitmap, fileName);
    }

    @OnShowRationale(value = {Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public void showRationaleByWritePermission(PermissionRequest request) {
        if (null != request) request.proceed();
    }

    @OnPermissionDenied(value = {Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public void onDeniedByWritePermission() {
        disPermissionTipsDialog();
        UIHelper.showAppSettingDialog(SaveQRCodeDialog.this, getString(R.string.write_permission_tips));
    }

    private void asyncWork(Runnable runnable) {
        if (workThread.isShutdown() || null == runnable) return;
        workThread.submit(runnable);
    }

    private void initUI() {
        if (!(mBuilder instanceof Builder)) return;
        final Builder builder = (Builder) mBuilder;
        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnSaveImage.setOnClickListener(v -> {
            if (null == bitmap) {
                showTips(getString(R.string.failed_to_load_qr_code));
                return;
            }
            tryToSaveImage(bitmap, "qrcode_" + builder.name + "_" + builder.getMac().replaceAll(":", ""));
        });
        binding.tvNameValue.setText(builder.getName());
        String pwdText = builder.getPassword();
        if (TextUtils.isEmpty(pwdText)) {
            pwdText = getString(R.string.unencrypted);
        }
        binding.tvPasswordValue.setText(pwdText);

        String content = builder.getQrContent();
        if (TextUtils.isEmpty(content)) {
            AuracastLoginInfo loginInfo = new AuracastLoginInfo()
                    .setDeviceName(builder.getName())
                    .setLoginPassword(builder.password);
            content = loginInfo.toString();
        }
        loadQrCodeBitmap(content);
    }

    private void loadQrCodeBitmap(String qrContent) {
        if (null == qrContent) return;
        asyncWork(() -> {
            bitmap = CodeUtils.createQRCode(qrContent, 600);
            requireActivity().runOnUiThread(() -> {
                if (null == bitmap) {
                    showTips(getString(R.string.failed_to_load_qr_code));
                    dismiss();
                    return;
                }
                binding.ivQrCode.setImageBitmap(bitmap);
            });
        });
    }

    private void tryToSaveImage(Bitmap bitmap, String fileName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !PermissionUtils.hasSelfPermissions(requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showPermissionTipsDialog(getString(R.string.write_permission_tips));
            SaveQRCodeDialogPermissionsDispatcher.checkWritePermissionWithPermissionCheck(this, bitmap, fileName);
            return;
        }
        asyncWork(() -> {
            final boolean ret = saveImage(bitmap, fileName);
            requireActivity().runOnUiThread(() -> {
                if (!(mBuilder instanceof Builder)) return;
                final Builder builder = (Builder) mBuilder;
                if (ret) {
                    showTips(getString(R.string.save_image_successfully));
                } else {
                    showTips(getString(R.string.failed_to_save_image));
                }
                final OnResultCallback<Boolean> callback = builder.getCallback();
                if (null != callback) {
                    callback.onResult(ret);
                }
                dismiss();
            });
        });
    }

    private boolean saveImage(Bitmap bitmap, String fileName) {
        String imageName = fileName + ".jpg";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, imageName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            try {
                ContentResolver resolver = requireContext().getContentResolver();
                if (null == resolver) return false;
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (null == uri) return false;
                OutputStream outputStream = resolver.openOutputStream(uri);
                if (null == outputStream) return false;
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.flush();
                outputStream.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (null == downloadDir || !downloadDir.exists()) {
            return false;
        }
        File image = new File(downloadDir, imageName);
        try {
            FileOutputStream outputStream = new FileOutputStream(image);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            // Notify the system that a new file has been added to the downloads folder
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(image));
            requireActivity().sendBroadcast(mediaScanIntent);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class Builder extends CommonDialog.Builder {
        /**
         * 设备地址
         */
        @NonNull
        private final String mac;
        /**
         * 名称
         */
        @NonNull
        private final String name;
        /**
         * 密码
         */
        @Nullable
        private final String password;
        /**
         * 二维码信息
         */
        @NonNull
        private final String qrContent;
        /**
         * 提示内容
         */
        private String tips;
        /**
         * 结果回调
         */
        private OnResultCallback<Boolean> callback;

        public Builder(@NonNull String mac, @NonNull String name, @Nullable String password, @NonNull String qrContent) {
            this.mac = mac;
            this.name = name;
            this.password = password;
            this.qrContent = qrContent;
            setCancelable(false).setWidthRate(0.8f);
        }

        @NonNull
        public String getMac() {
            return mac;
        }

        @NonNull
        public String getName() {
            return name;
        }

        @Nullable
        public String getPassword() {
            return password;
        }

        @NonNull
        public String getQrContent() {
            return qrContent;
        }

        public String getTips() {
            return tips;
        }

        public Builder setTips(String tips) {
            this.tips = tips;
            return this;
        }

        public OnResultCallback<Boolean> getCallback() {
            return callback;
        }

        public Builder setCallback(OnResultCallback<Boolean> callback) {
            this.callback = callback;
            return this;
        }

        @Override
        public SaveQRCodeDialog build() {
            return new SaveQRCodeDialog(this);
        }
    }
}
