package com.jieli.btsmart.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.chargingcase.ChargingCaseInfo;
import com.jieli.btsmart.databinding.ActivityCropPhotoBinding;
import com.jieli.btsmart.ui.base.BaseActivity;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.component.utils.SystemUtil;
import com.jieli.component.utils.ValueUtil;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropFragment;
import com.yalantis.ucrop.UCropFragmentCallback;
import com.yalantis.ucrop.view.GestureCropImageView;
import com.yalantis.ucrop.view.UCropView;

import java.io.File;

/**
 * 裁剪图片界面
 */
public class CropPhotoActivity extends BaseActivity implements UCropFragmentCallback, DevicePopDialogFilter.IgnoreFilter {

    private ActivityCropPhotoBinding mCropPhotoBinding;
    private UCropFragment mCropFragment;
    private UCropView mUCropView;
    private GestureCropImageView mGestureCropImageView;

    private int cropType;
    private Uri photoUri;
    private String outputPath;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public final static String KEY_CROP_TYPE = "crop_type";
    public final static String KEY_RESOURCE_URI = "resource_uri";
    public final static String KEY_OUTPUT_PATH = "output_path";
    public final static String KEY_CROP_SIZE = "crop_size";

    public final static int CROP_TYPE_AVATAR = 1;
    /**
     * 裁剪屏幕保护
     */
    public final static int CROP_TYPE_SCREEN_SAVERS = 2;

    /**
     * 是否裁剪成功
     */
    private boolean isCropSuccess;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemUtil.setImmersiveStateBar(getWindow(), true);
        mCropPhotoBinding = ActivityCropPhotoBinding.inflate(getLayoutInflater());
        setContentView(mCropPhotoBinding.getRoot());
        if (null == getIntent()) {
            finish();
            return;
        }
        cropType = getIntent().getIntExtra(KEY_CROP_TYPE, 0);
        photoUri = getIntent().getParcelableExtra(KEY_RESOURCE_URI);
        outputPath = getIntent().getStringExtra(KEY_OUTPUT_PATH);
        JL_Log.i(TAG, "-onCreate- cropType = " + cropType + ", photoUri = " + photoUri + ", outputPath = " + outputPath);
        if (cropType <= 0 || null == photoUri || null == outputPath) {
            finish();
            return;
        }
        DevicePopDialogFilter.getInstance().addIgnoreFilter(this);
        initUI();
        initUCrop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isCropSuccess) {
            File outputFile = new File(outputPath);
            if (outputFile.exists()) {
                outputFile.delete();
            }
        }
        DevicePopDialogFilter.getInstance().removeIgnoreFilter(this);
        mCropPhotoBinding = null;
    }

    @Override
    public void loadingProgress(boolean showLoader) {

    }

    @Override
    public void onCropFinish(@NonNull UCropFragment.UCropResult result) {
        JL_Log.i(TAG, "-onCropFinish- result : " + result.mResultCode + ", intent = " + result.mResultData);
        isCropSuccess = result.mResultCode == Activity.RESULT_OK;
        setResult(result.mResultCode, result.mResultData);
        finish();
    }

    private void initUI() {
        mCropPhotoBinding.tvCropPhotoCancel.setOnClickListener(v -> {
            isCropSuccess = false;
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
        mCropPhotoBinding.tvCropPhotoSelect.setOnClickListener(v -> {
            if (mCropFragment != null && mCropFragment.isAdded()) {
                JL_Log.i(TAG, "[cropAndSaveImage] >>>");
                mCropFragment.cropAndSaveImage();
            }
        });
        mCropPhotoBinding.ivCropPhotoRotate.setOnClickListener(v -> {
            if (mCropFragment != null && mCropFragment.isAdded() && mGestureCropImageView != null) {
                mGestureCropImageView.postRotate(90);
                mGestureCropImageView.setImageToWrapCropBounds();
            }
        });
        mCropPhotoBinding.btnBack.setOnClickListener(v -> {
            isCropSuccess = false;
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    private void initUCrop() {
        UCrop uCrop = UCrop.of(photoUri, Uri.fromFile(new File(outputPath)));
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(100);
        //是否隐藏底部控制栏
        options.setHideBottomControls(true);
        //是否可以自由裁剪
        options.setFreeStyleCropEnabled(false);
        options.setShowCropGrid(false);
        //裁剪区域绘制
        options.setShowCropFrame(true);
        options.setCropFrameStrokeWidth(ValueUtil.dp2px(this, 2));
        if (cropType == CROP_TYPE_SCREEN_SAVERS) {
            int[] arg = getIntent() == null ? null : getIntent().getIntArrayExtra(KEY_CROP_SIZE);
            if (null == arg) {
                arg = new int[]{ChargingCaseInfo.SCREEN_WIDTH, ChargingCaseInfo.SCREEN_HEIGHT};
            }
            int width = arg[0];
            int height = arg[1];
            JL_Log.d(TAG, "initUCrop", "width = " + width + ", height = " + height);
            uCrop.withAspectRatio(width, height).withMaxResultSize(width, height);
            options.setCircleDimmedLayer(false);
            options.withAspectRatio(width, height);
            options.setCropFrameRadius(ValueUtil.dp2px(this, 14));
            //设置图像最大体积
            options.setMaxBitmapSize(1024 * 1024);
        } else {
            uCrop.withAspectRatio(1.0f, 1.0f).withMaxResultSize(180, 180);
            options.setCircleDimmedLayer(true);
            options.withAspectRatio(1.0f, 1.0f);
            //设置图像最大体积
            options.setMaxBitmapSize(1024 * 1024);
        }
        uCrop.withOptions(options);
        setupFragment(uCrop);
    }

    private void setupFragment(@NonNull UCrop uCrop) {
        mCropFragment = uCrop.getFragment(uCrop.getIntent(CropPhotoActivity.this).getExtras());
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fl_crop_photo_container, mCropFragment, UCropFragment.TAG)
                .commitAllowingStateLoss();

        mHandler.postDelayed(() -> {
            if (mCropFragment != null && mCropFragment.isAdded()) {
                mUCropView = mCropFragment.requireView().findViewById(com.yalantis.ucrop.R.id.ucrop);
                mGestureCropImageView = mUCropView.getCropImageView();
//                int padding = ValueUtil.dp2px(getApplicationContext(), 32);
//                mUCropView.getOverlayView().setPadding(padding, 0, padding, 0);
            }
        }, 200);
    }

    @Override
    public boolean shouldIgnore(BleScanMessage bleScanMessage) {
        return true;
    }
}