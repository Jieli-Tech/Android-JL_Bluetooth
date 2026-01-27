package com.jieli.btsmart.ui.qrcode;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.provider.MediaStore;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.ActivityQrcodeScanBinding;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.ui.widget.dialog.PermissionTipsDialog;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.ToastUtil;
import com.king.camera.scan.AnalyzeResult;
import com.king.camera.scan.CameraScan;
import com.king.camera.scan.analyze.Analyzer;
import com.king.camera.scan.util.PointUtils;
import com.king.wechat.qrcode.WeChatQRCodeDetector;
import com.king.wechat.qrcode.scanning.WeChatCameraScanActivity;
import com.king.wechat.qrcode.scanning.analyze.WeChatScanningAnalyzer;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.PermissionUtils;
import permissions.dispatcher.RuntimePermissions;

/**
 * QRCodeScanActivity
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 扫码界面
 * @since 2025/9/8
 */
@RuntimePermissions
public class QRCodeScanActivity extends WeChatCameraScanActivity implements DevicePopDialogFilter.IgnoreFilter {

    private final String tag = getClass().getSimpleName();

    private ActivityQrcodeScanBinding binding;

    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();

    private PermissionTipsDialog permissionTipsDialog;

    private final ActivityResultLauncher<Intent> selectImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result != null && result.getResultCode() == Activity.RESULT_OK) {
                    parsePhoto(result.getData());
                }
            });

    @Override
    public void initUI() {
        EdgeToEdge.enable(this);
        binding = ActivityQrcodeScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });
        super.initUI();
        DevicePopDialogFilter.getInstance().addIgnoreFilter(this);
        initTopBar();
        binding.btnFlashlight.setOnClickListener(v -> {
            toggleTorchState();
            updateLightUI(isLightOn());
        });
        binding.btnSelectFromAlbum.setOnClickListener(v -> tryToSelectPhoto());
        updateLightUI(isLightOn());
    }

    @Override
    public void initCameraScan(@NonNull CameraScan<List<String>> cameraScan) {
        super.initCameraScan(cameraScan);
        cameraScan.setPlayBeep(true);
    }

    @Nullable
    @Override
    public Analyzer<List<String>> createAnalyzer() {
        // 如果需要返回结果二维码位置信息，则初始化分析器时，isOutputVertices参数传 true 即可
        return new WeChatScanningAnalyzer(true);
    }

    @Override
    public boolean isContentView() {
        return false;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_qrcode_scan;
    }

    @Override
    public int getFlashlightId() {
        return View.NO_ID;
    }

    @Override
    public void onScanResultCallback(@NonNull AnalyzeResult<List<String>> result) {
        //停止分析
        getCameraScan().setAnalyzeImage(false);

        // 显示结果点
        displayResultPoint(result);

        handleResult(result.getResult().get(0));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disPermissionTipsDialog();
        DevicePopDialogFilter.getInstance().removeIgnoreFilter(this);
    }

    @Override
    public boolean shouldIgnore(BleScanMessage bleScanMessage) {
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        QRCodeScanActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE})
    public void requestStoragePermission() {
        disPermissionTipsDialog();
        selectImageLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    @OnShowRationale({Manifest.permission.READ_EXTERNAL_STORAGE,})
    public void showRelationForStoragePermission(PermissionRequest request) {
        if (null != request) request.proceed();
    }

    @OnPermissionDenied({Manifest.permission.READ_EXTERNAL_STORAGE,})
    public void onStoragePermissionDenied() {
        disPermissionTipsDialog();
        UIHelper.showAppSettingDialog(this, getString(R.string.select_photo_storage_permission_tips));
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @NeedsPermission({Manifest.permission.READ_MEDIA_IMAGES})
    public void requestStoragePermissionBy33() {
        disPermissionTipsDialog();
        selectImageLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnShowRationale({Manifest.permission.READ_MEDIA_IMAGES,})
    public void showRelationForStoragePermissionBy33(PermissionRequest request) {
        if (null != request) request.proceed();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnPermissionDenied({Manifest.permission.READ_MEDIA_IMAGES,})
    public void onStoragePermissionDeniedBy33() {
        disPermissionTipsDialog();
        UIHelper.showAppSettingDialog(this, getString(R.string.select_photo_storage_permission_tips));
    }

    private boolean isLightOn() {
        return getCameraScan().isTorchEnabled();
    }

    protected void showTips(int resId) {
        showTips(getString(resId));
    }

    protected void showTips(String content) {
        JL_Log.d(tag, "showTips", content);
        ToastUtil.showToastShort(content);
    }

    private void initTopBar() {
        binding.viewTopBar.tvLeft.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_back_white, 0, 0, 0);
        binding.viewTopBar.tvLeft.setOnClickListener(v -> finish());
        binding.viewTopBar.tvTitle.setText(getString(R.string.scan_qr_code));
        binding.viewTopBar.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.white_ffffff));
    }

    private void updateLightUI(boolean isOn) {
        binding.btnFlashlight.setImageResource(isOn ? R.drawable.ic_flashlight_black : R.drawable.ic_flashlight_white);
    }

    private void asyncThread(Runnable runnable) {
        if (null == runnable || threadPool.isShutdown()) return;
        threadPool.execute(runnable);
    }

    private void displayResultPoint(AnalyzeResult<List<String>> result) {
        List<Point> list = new ArrayList<>();
        if (result instanceof WeChatScanningAnalyzer.QRCodeAnalyzeResult) {
            WeChatScanningAnalyzer.QRCodeAnalyzeResult<?> analyzeResult = (WeChatScanningAnalyzer.QRCodeAnalyzeResult<?>) result;
            int width = result.getImageWidth();
            int height = result.getImageHeight();

            List<Mat> mats = analyzeResult.getPoints();
            if (mats != null) {
                for (Mat mat : mats) {
                    Point point0 = new Point((int) mat.get(0, 0)[0], (int) mat.get(0, 1)[0]);
                    Point point1 = new Point((int) mat.get(1, 0)[0], (int) mat.get(1, 1)[0]);
                    Point point2 = new Point((int) mat.get(2, 0)[0], (int) mat.get(2, 1)[0]);
                    Point point3 = new Point((int) mat.get(3, 0)[0], (int) mat.get(3, 1)[0]);

                    int centerX = (point0.x + point1.x + point2.x + point3.x) / 4;
                    int centerY = (point0.y + point1.y + point2.y + point3.y) / 4;

                    //将实际的结果中心点坐标转换成界面预览的坐标
                    Point point = PointUtils.transform(centerX, centerY, width, height,
                            viewfinderView.getWidth(), viewfinderView.getHeight());
                    list.add(point);
                }
            }
        }
        //显示结果点信息
        viewfinderView.showResultPoints(list);
    }

    private void tryToSelectPhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionUtils.hasSelfPermissions(this, Manifest.permission.READ_MEDIA_IMAGES)) {
                showPermissionTipsDialog(getString(R.string.select_photo_storage_permission));
            }
            QRCodeScanActivityPermissionsDispatcher.requestStoragePermissionBy33WithPermissionCheck(this);
            return;
        }
        if (!PermissionUtils.hasSelfPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showPermissionTipsDialog(getString(R.string.select_photo_storage_permission));
        }
        QRCodeScanActivityPermissionsDispatcher.requestStoragePermissionWithPermissionCheck(this);
    }

    private void parsePhoto(Intent data) {
        if (null == data) return;
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
            if (null != bitmap) {
                asyncThread(() -> {
                    final List<String> result = WeChatQRCodeDetector.detectAndDecode(bitmap);
                    runOnUiThread(() -> {
                        String text = result.isEmpty() ? "" : result.get(0);
                        handleResult(text);
                    });
                });
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        showTips(getString(R.string.qr_code_error_tips));
    }

    private void handleResult(String text) {
        JL_Log.d(tag, "handleResult", text);
        Intent intent = new Intent();
        intent.putExtra(CameraScan.SCAN_RESULT, text);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void showPermissionTipsDialog(String tips) {
        if (isDestroyed() || isFinishing()) return;
        if (null == permissionTipsDialog) {
            permissionTipsDialog = new PermissionTipsDialog.Builder()
                    .tips(tips)
                    .build();
        }
        if (!permissionTipsDialog.isShow()) {
            permissionTipsDialog.show(getSupportFragmentManager(), PermissionTipsDialog.class.getSimpleName());
        }
    }

    public void disPermissionTipsDialog() {
        if (isDestroyed() || isFinishing() || null == permissionTipsDialog) return;
        if (permissionTipsDialog.isShow()) {
            permissionTipsDialog.dismiss();
        }
        permissionTipsDialog = null;
    }
}