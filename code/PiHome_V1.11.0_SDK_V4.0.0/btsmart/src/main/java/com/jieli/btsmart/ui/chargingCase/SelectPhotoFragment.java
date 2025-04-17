package com.jieli.btsmart.ui.chargingCase;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.settings.v0.ResourceInfo;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.CropPhotoActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.FileUtil;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.PermissionUtils;
import permissions.dispatcher.RuntimePermissions;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 请求摄像头权限
 * @since 2023/12/11
 */
@RuntimePermissions
public abstract class SelectPhotoFragment extends DeviceControlFragment {

    /**
     * 图片文件
     */
    protected File photoFile;
    /**
     * 图片链接
     */
    protected Uri photoUri;

    private final ActivityResultLauncher<Uri> takePhotoLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
        if (result) {
            if (null == photoUri) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && null != photoFile) {
                photoUri = Uri.fromFile(photoFile);
            }
            goToCropPhoto(photoUri);
        }
    });

    private final ActivityResultLauncher<String> selectPhotoLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
        if (null == result) return;
        JL_Log.d(TAG, "selectPhotoLauncher", "path = " + result.getPath());
        goToCropPhoto(result);
    });

    private final ActivityResultLauncher<Intent> cropPhotoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && null != result.getData()) {
            Uri uri = UCrop.getOutput(result.getData());
            if (null == uri) return;
            String cropPhotoPath = uri.getPath();
            JL_Log.d(TAG, "cropPhotoLauncher", "cropPhotoPath = " + cropPhotoPath);
            Bundle bundle = new Bundle();
            bundle.putString(SConstant.KEY_FILE_PATH, cropPhotoPath);
            bundle.putInt(SConstant.KEY_DEVICE_SCREEN_WIDTH, getDeviceScreenWidth());
            bundle.putInt(SConstant.KEY_DEVICE_SCREEN_HEIGHT, getDeviceScreenHeight());
            ContentActivity.startActivity(requireContext(), ConfirmScreenSaversFragment.class.getCanonicalName(), getString(R.string.screen_savers), bundle);
        }
    });

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        SelectPhotoFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission({Manifest.permission.CAMERA})
    public void requestCameraPermission(DeviceInfo deviceInfo) {
        takePhoto(deviceInfo);
    }

    @OnShowRationale({Manifest.permission.CAMERA})
    public void onShowRationaleCameraPermission(@NonNull PermissionRequest request) {
        request.proceed();
    }

    @OnPermissionDenied({Manifest.permission.CAMERA})
    public void onDeniedCameraPermission() {
        UIHelper.showAppSettingDialog(SelectPhotoFragment.this, getString(R.string.permissions_tips_02) + getString(R.string.permission_camera));
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE})
    public void requestStoragePermission(DeviceInfo deviceInfo) {
        selectPhotoFromAlbum(deviceInfo);
    }

    @OnShowRationale({Manifest.permission.READ_EXTERNAL_STORAGE})
    public void onShowRationaleStoragePermission(@NonNull PermissionRequest request) {
        request.proceed();
    }

    @OnPermissionDenied({Manifest.permission.READ_EXTERNAL_STORAGE})
    public void onDeniedStoragePermission() {
        UIHelper.showAppSettingDialog(SelectPhotoFragment.this, getString(R.string.permissions_tips_02) + getString(R.string.permission_storage));
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @NeedsPermission({Manifest.permission.READ_MEDIA_IMAGES})
    public void requestStorageStoragePermissionBy33(DeviceInfo deviceInfo) {
        selectPhotoFromAlbum(deviceInfo);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnShowRationale({Manifest.permission.READ_MEDIA_IMAGES})
    public void onShowRationaleStoragePermissionBy33(@NonNull PermissionRequest request) {
        request.proceed();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnPermissionDenied({Manifest.permission.READ_MEDIA_IMAGES})
    public void onDeniedStoragePermissionBy33() {
        onDeniedStoragePermission();
    }

    public void tryToTakePhoto(DeviceInfo deviceInfo) {
        if (!PermissionUtils.hasSelfPermissions(requireContext(), Manifest.permission.CAMERA)) {
            SelectPhotoFragmentPermissionsDispatcher.requestCameraPermissionWithPermissionCheck(this, deviceInfo);
            return;
        }
        takePhoto(deviceInfo);
    }

    public void tryToSelectPhotoFromAlbum(DeviceInfo deviceInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionUtils.hasSelfPermissions(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)) {
                SelectPhotoFragmentPermissionsDispatcher.requestStorageStoragePermissionBy33WithPermissionCheck(this, deviceInfo);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!PermissionUtil.isHasStoragePermission(requireContext())) {
                SelectPhotoFragmentPermissionsDispatcher.requestStoragePermissionWithPermissionCheck(this, deviceInfo);
                return;
            }
        }
        selectPhotoFromAlbum(deviceInfo);
    }

    public abstract int getDeviceScreenWidth();

    public abstract int getDeviceScreenHeight();

    public String getCropFilePath(String deviceMac) {
        return FileUtil.createFilePath(requireContext(), requireContext().getPackageName(), SConstant.DIR_RESOURCE,
                deviceMac, SConstant.DIR_CUSTOM) + File.separator + SConstant.CROP_FILE_NAME;
    }

    public File[] getCustomFiles(String deviceMac) {
        File customDir = new File(FileUtil.createFilePath(requireContext(), requireContext().getPackageName(),
                SConstant.DIR_RESOURCE, deviceMac, SConstant.DIR_CUSTOM));
        File[] customFiles = customDir.listFiles();
        if (null == customFiles || customFiles.length == 0) return new File[0];
        List<File> fileList = new ArrayList<>(Arrays.asList(customFiles));
        List<File> list = new ArrayList<>();
        for (File file : fileList) {
            if (file.isDirectory()) continue;
            if (file.exists() && file.isFile() && file.getName().startsWith(ResourceInfo.CUSTOM_SCREEN_NAME)
                    && !file.getName().equals(SConstant.CROP_FILE_NAME)) {
                list.add(file);
            }
        }
        customFiles = list.toArray(new File[0]);
        return customFiles;
    }

    private void takePhoto(DeviceInfo deviceInfo) {
        if (null == deviceInfo) return;
        photoFile = new File(getCropFilePath(deviceInfo.getEdrAddr()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            photoUri = FileProvider.getUriForFile(requireActivity(), requireContext().getPackageName() + ".provider", photoFile);
        } else {
            photoUri = Uri.fromFile(photoFile);
        }
        takePhotoLauncher.launch(photoUri);
    }

    private void selectPhotoFromAlbum(DeviceInfo deviceInfo) {
        if (null == deviceInfo) return;
        photoFile = new File(getCropFilePath(deviceInfo.getEdrAddr()));
        selectPhotoLauncher.launch("image/*");
    }

    private void goToCropPhoto(Uri uri) {
        if (null == uri || null == photoFile) return;

        Intent intent = new Intent(requireContext(), CropPhotoActivity.class);
        intent.putExtra(CropPhotoActivity.KEY_CROP_TYPE, CropPhotoActivity.CROP_TYPE_SCREEN_SAVERS);
        intent.putExtra(CropPhotoActivity.KEY_RESOURCE_URI, uri);
        intent.putExtra(CropPhotoActivity.KEY_OUTPUT_PATH, photoFile.getPath());
        intent.putExtra(CropPhotoActivity.KEY_CROP_SIZE, new int[]{getDeviceScreenWidth(), getDeviceScreenHeight()});
        cropPhotoLauncher.launch(intent);
    }
}