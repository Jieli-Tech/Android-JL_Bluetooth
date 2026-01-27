package com.jieli.btsmart.ui.chargingCase;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.jieli.bluetooth.bean.charging_case.ChargingCaseInfo;
import com.jieli.bluetooth.bean.settings.v0.ResourceInfo;
import com.jieli.bluetooth.bean.settings.v0.SDKMessage;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.chargingcase.ResourceFile;
import com.jieli.btsmart.data.model.chargingcase.ResourceMsg;
import com.jieli.btsmart.ui.CropPhotoActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.widget.dialog.SelectPhotoDialog;
import com.jieli.btsmart.util.ChargingBinUtil;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.btsmart.util.UIHelper;
import com.yalantis.ucrop.UCrop;

import java.io.File;

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
    /**
     * 资源信息
     */
    private ResourceMsg resourceMsg;

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
        goToCropPhoto(result);
    });

    private final ActivityResultLauncher<Intent> cropPhotoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && null != result.getData()) {
            Uri uri = UCrop.getOutput(result.getData());
            if (null == uri) return;
            String cropPhotoPath = uri.getPath();
            JL_Log.d(TAG, "cropPhotoLauncher", "cropPhotoPath : " + cropPhotoPath);
            if (null == cropPhotoPath || null == resourceMsg) return;
            resourceMsg.setSrcFilePath(cropPhotoPath);
            ChargingBinUtil.jumpResourceFragment(requireContext(), resourceMsg);
            resourceMsg = null;
        }
    });

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        SelectPhotoFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission({Manifest.permission.CAMERA})
    public void requestCameraPermission(ResourceMsg msg) {
        takePhoto(msg);
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
    public void requestStoragePermission(ResourceMsg msg) {
        selectPhotoFromAlbum(msg);
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
    public void requestStorageStoragePermissionBy33(ResourceMsg msg) {
        selectPhotoFromAlbum(msg);
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

    public void showSelectPhotoDialog(@NonNull ChargingCaseInfo info, @ChargingCaseInfo.ResourceType int resourceType) {
        ResourceMsg resourceMsg = new ResourceMsg(info.getAddress())
                .setScreenWidth(info.getScreenWidth())
                .setScreenHeight(info.getScreenHeight())
                .setResourceType(resourceType);
        final SDKMessage sdkMessage = info.getSdkMessage();
        resourceMsg.setChip(sdkMessage == null ? SDKMessage.CHIP_701N : sdkMessage.getChip());
        showSelectPhotoDialog(resourceMsg);
    }

    public void showSelectPhotoDialog(@NonNull ResourceMsg resourceMsg) {
        if (isInvalid()) return;
        final String tag = "select_photo";
        SelectPhotoDialog dialog = (SelectPhotoDialog) getChildFragmentManager().findFragmentByTag(tag);
        if (null == dialog) {
            dialog = new SelectPhotoDialog.Builder()
                    .listener(new SelectPhotoDialog.OnSelectPhotoListener() {
                        @Override
                        public void onTakePhoto(SelectPhotoDialog dialog) {
                            tryToTakePhoto(resourceMsg);
                        }

                        @Override
                        public void onSelectFromAlbum(SelectPhotoDialog dialog) {
                            tryToSelectPhotoFromAlbum(resourceMsg);
                        }

                        @Override
                        public void onCancel(SelectPhotoDialog dialog) {

                        }
                    }).build();
        }
        if (!dialog.isShow()) {
            dialog.show(getChildFragmentManager(), tag);
        }
    }

    protected void handleResourceFile(@NonNull ChargingCaseSettingViewModel viewModel, @NonNull ResourceFile resourceFile) {
        ChargingCaseInfo info = viewModel.getChargingCaseInfo();
        JL_Log.d(TAG, "handleResourceFile", "" + resourceFile);
        switch (resourceFile.getDevState()) {
            case ResourceFile.STATE_NOT_EXIST:
                ResourceMsg resourceMsg = new ResourceMsg(info.getAddress())
                        .setScreenWidth(info.getScreenWidth())
                        .setScreenHeight(info.getScreenHeight())
                        .setResourceType(resourceFile.getType())
                        .setSrcFilePath(resourceFile.getPath());
                final SDKMessage sdkMessage = info.getSdkMessage();
                resourceMsg.setChip(sdkMessage == null ? SDKMessage.CHIP_701N : sdkMessage.getChip());
                ChargingBinUtil.jumpResourceFragment(requireContext(), resourceMsg);
                break;
            case ResourceFile.STATE_ALREADY_EXIST:
                final ResourceInfo devFile = resourceFile.getDevFile();
                if (null != devFile) {
                    final int type = resourceFile.getType();
                    JL_Log.d(TAG, "handleResourceFile", "type : " + ChargingCaseInfo.printResourceType(type));
                    if (type == ChargingCaseInfo.TYPE_WALLPAPER) {
                        viewModel.setCurrentWallpaper(devFile.getDevHandle(), devFile.getFilePath());
                    } else if (type == ChargingCaseInfo.TYPE_SCREEN_SAVER) {
                        viewModel.setCurrentScreenSaver(devFile.getDevHandle(), devFile.getFilePath());
                    } else {
                        showTips(getString(R.string.not_support_function));
                    }
                    return;
                }
                showTips(getString(R.string.missing_resource_info));
                break;
        }
    }

    protected void syncResource(@NonNull ChargingCaseSettingViewModel viewModel, @ChargingCaseInfo.ResourceType int resource) {
        switch (resource) {
            case ChargingCaseInfo.TYPE_SCREEN_SAVER:
                final ChargingCaseInfo caseInfo = viewModel.getChargingCaseInfo();
                if (caseInfo.isJL701N()) {
                    viewModel.getCurrentScreenSaver();
                } else {
                    viewModel.browseScreenSavers();
                }
                break;
            case ChargingCaseInfo.TYPE_BOOT_ANIM:
                viewModel.getCurrentBootAnim();
                break;
            case ChargingCaseInfo.TYPE_WALLPAPER:
                viewModel.browseWallpapers();
                break;
        }
    }

    private void tryToTakePhoto(ResourceMsg resourceMsg) {
        if (!PermissionUtils.hasSelfPermissions(requireContext(), Manifest.permission.CAMERA)) {
            SelectPhotoFragmentPermissionsDispatcher.requestCameraPermissionWithPermissionCheck(this, resourceMsg);
            return;
        }
        takePhoto(resourceMsg);
    }

    private void tryToSelectPhotoFromAlbum(ResourceMsg resourceMsg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionUtils.hasSelfPermissions(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)) {
                SelectPhotoFragmentPermissionsDispatcher.requestStorageStoragePermissionBy33WithPermissionCheck(this, resourceMsg);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!PermissionUtil.isHasStoragePermission(requireContext())) {
                SelectPhotoFragmentPermissionsDispatcher.requestStoragePermissionWithPermissionCheck(this, resourceMsg);
                return;
            }
        }
        selectPhotoFromAlbum(resourceMsg);
    }

    private void takePhoto(ResourceMsg msg) {
        if (null == msg) return;
        resourceMsg = msg;
        photoFile = new File(ChargingBinUtil.getCropFilePath(requireActivity(), msg.getMac(), msg.getResourceType()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            photoUri = FileProvider.getUriForFile(requireActivity(), requireContext().getPackageName() + ".provider", photoFile);
        } else {
            photoUri = Uri.fromFile(photoFile);
        }
        takePhotoLauncher.launch(photoUri);
    }

    private void selectPhotoFromAlbum(ResourceMsg msg) {
        if (null == msg) return;
        resourceMsg = msg;
        photoFile = new File(ChargingBinUtil.getCropFilePath(requireActivity(), msg.getMac(), msg.getResourceType()));
        selectPhotoLauncher.launch("image/*");
    }

    private void goToCropPhoto(Uri uri) {
        if (null == uri || null == photoFile || null == resourceMsg) return;
        JL_Log.d(TAG, "goToCropPhoto", "path : " + uri.getPath());
        Intent intent = new Intent(requireContext(), CropPhotoActivity.class);
        intent.putExtra(CropPhotoActivity.KEY_CROP_TYPE, CropPhotoActivity.CROP_TYPE_SCREEN_SAVERS);
        intent.putExtra(CropPhotoActivity.KEY_RESOURCE_URI, uri);
        intent.putExtra(CropPhotoActivity.KEY_OUTPUT_PATH, photoFile.getPath());
        intent.putExtra(CropPhotoActivity.KEY_CROP_SIZE, new int[]{resourceMsg.getScreenWidth(), resourceMsg.getScreenHeight()});
        cropPhotoLauncher.launch(intent);
    }
}