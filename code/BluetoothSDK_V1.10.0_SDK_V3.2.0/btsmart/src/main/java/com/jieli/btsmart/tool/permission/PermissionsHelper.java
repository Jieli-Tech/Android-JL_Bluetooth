package com.jieli.btsmart.tool.permission;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;


import com.jieli.component.utils.SystemUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.component.permission.PermissionManager;
import com.jieli.jl_dialog.Jl_Dialog;

/**
 * 权限辅助类
 *
 * @author zqjasonZhong
 * @date 2020/3/25
 */
@Deprecated
public class PermissionsHelper {
    private final static String TAG = "PermissionsHelper";
    private AppCompatActivity mActivity;
    private Jl_Dialog notifyGpsDialog;
    private Jl_Dialog notifyDialog;

    private OnPermissionListener mListener;
    private final Handler mHandler;

    private PermissionManager mPermissionManager;

    /**
     * 应用请求的权限列表
     */
    public final static String[] sPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,

            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,

            Manifest.permission.READ_PHONE_STATE,

//            Manifest.permission.RECORD_AUDIO,

           /* Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,*/

//            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };

    private String[] mPermissions;

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        destroy();
    }

    public PermissionsHelper(AppCompatActivity activity) {
        mActivity = SystemUtil.checkNotNull(activity);
        mHandler = new Handler(Looper.getMainLooper());
        mPermissions = sPermissions;
    }

    public static boolean checkAppPermissionsIsAllow(Context context) {
        if (context == null) return false;
        return PermissionUtil.isHasStoragePermission(context)
                && PermissionUtil.isHasLocationPermission(context);
    }

    public void setOnPermissionListener(OnPermissionListener listener) {
        mListener = listener;
    }

    public void destroy() {
        if (mPermissionManager != null) {
            mPermissionManager.release();
            mPermissionManager = null;
        }
        mListener = null;
        mActivity = null;
        dismissNotifyGPSDialog();
        dismissNotifyPermissionDialog();
        mHandler.removeCallbacksAndMessages(null);
    }

    public void checkAppRequestPermissions(final String[] permissions) {
        if (mActivity == null || permissions == null || permissions.length == 0) return;
        mPermissions = permissions;
        getPermissionManager().permissions(permissions).callback(new PermissionManager.OnPermissionStateCallback() {
            @Override
            public void onSuccess() {
                if (PermissionUtil.checkGpsProviderEnable(mActivity)) {
                    callbackPermissionSuccess(permissions);
                } else {
                    JL_Log.i(TAG, "checkAppRequestPermissions :: onGPSError ");
                    showNotifyGPSDialog();
                }
            }

            @Override
            public void onFailed(boolean isShouldShowDialog, String permission, Intent intent) {
                if (isShouldShowDialog) {
                    showToPermissionSettingDialog(permission, intent);
                } else {
                    callbackPermissionFailed(permission);
                }
            }

            @Override
            public void onError(int code, String message) {

            }
        }).request();
    }

    public String getPermissionName(String permission) {
        String name = permission;
        switch (permission) {
            case Manifest.permission.READ_CONTACTS:
            case Manifest.permission.WRITE_CONTACTS:
                name = getString(R.string.permission_contacts);
                break;
            case Manifest.permission.RECORD_AUDIO:
                name = getString(R.string.permission_mic);
                break;
            case Manifest.permission.READ_PHONE_STATE:
                name = getString(R.string.permission_read_phone_state);
                break;
            case Manifest.permission.ACCESS_COARSE_LOCATION:
            case Manifest.permission.ACCESS_FINE_LOCATION:
                name = getString(R.string.permission_location);
                break;
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                name = getString(R.string.permission_storage);
                break;
            case Manifest.permission.WRITE_SETTINGS:
                name = getString(R.string.permission_phone_settings);
                break;
        }
        return name;
    }

    private PermissionManager getPermissionManager() {
        if (mPermissionManager == null) {
            mPermissionManager = PermissionManager.with(mActivity);
        }
        return mPermissionManager;
    }

    private void showToPermissionSettingDialog(final String permission, final Intent intent) {
        if (mActivity == null || mActivity.isDestroyed() || mActivity.isFinishing()) return;
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.permissions_tips_01));
        sb.append(getPermissionName(permission));
        sb.append(getString(R.string.permission));
        if (notifyDialog == null) {
            notifyDialog = new Jl_Dialog.Builder()
                    .title(getString(R.string.tips))
                    .content(sb.toString())
                    .cancel(false)
                    .left(getString(R.string.cancel))
                    .leftColor(mActivity.getResources().getColor(R.color.gray_text_444444))
                    .right(getString(R.string.to_setting))
                    .rightColor(mActivity.getResources().getColor(R.color.red_FF688C))
                    .leftClickListener((v, dialogFragment) -> {
                        dismissNotifyPermissionDialog();
                        callbackPermissionFailed(permission);
                    })
                    .rightClickListener((v, dialogFragment) -> {
                        dismissNotifyPermissionDialog();
                        if (Manifest.permission.WRITE_SETTINGS.equals(permission)) {
                            mActivity.startActivityForResult(intent, SConstant.REQUEST_CODE_PERMISSIONS);
                        } else {
                            checkAppRequestPermissions(mPermissions);
                        }
                    })
                    .build();
        } else {
            JL_Log.d(TAG, sb.toString());
            if (notifyDialog.getBuilder() != null) {
                notifyDialog.getBuilder().content(sb.toString());
            }
        }
        if (!notifyDialog.isShow()) {
            JL_Log.d(TAG, sb.toString());
            notifyDialog.show(mActivity.getSupportFragmentManager(), "request_permission");
        }
    }

    private boolean isNotifyPermissionDialog() {
        return notifyDialog != null && notifyDialog.isShow();
    }

    private void dismissNotifyPermissionDialog() {
        if (notifyDialog != null) {
            if (notifyDialog.isShow() && !mActivity.isDestroyed()) {
                notifyDialog.dismiss();
            }
            notifyDialog = null;
        }
    }

    /**
     * 显示打开定位服务(gps)提示框
     */
    private void showNotifyGPSDialog() {
        if (mActivity == null || mActivity.isDestroyed() || mActivity.isFinishing()) return;
        if (notifyGpsDialog == null) {
            notifyGpsDialog = new Jl_Dialog.Builder()
                    .title(getString(R.string.tips))
                    .content(getString(R.string.open_gpg_tip))
                    .cancel(false)
                    .left(getString(R.string.cancel))
                    .leftColor(mActivity.getResources().getColor(R.color.gray_text_444444))
                    .right(getString(R.string.to_setting))
                    .rightColor(mActivity.getResources().getColor(R.color.red_FF688C))
                    .leftClickListener((v, dialogFragment) -> {
                        dismissNotifyGPSDialog();
                        callbackPermissionFailed(Manifest.permission.ACCESS_FINE_LOCATION);
                    })
                    .rightClickListener((v, dialogFragment) -> {
                        dismissNotifyGPSDialog();
                        mActivity.startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), SConstant.REQUEST_CODE_CHECK_GPS);
                    })
                    .build();
        }
        if (!notifyGpsDialog.isShow()) {
            notifyGpsDialog.show(mActivity.getSupportFragmentManager(), "notify_gps_dialog");
        }
    }

    private void dismissNotifyGPSDialog() {
        if (notifyGpsDialog != null) {
            if (notifyGpsDialog.isShow() && !mActivity.isDestroyed()) {
                notifyGpsDialog.dismiss();
            }
            notifyGpsDialog = null;
        }
    }

    private String getString(int res) {
        if (mActivity == null) return null;
        return mActivity.getString(res);
    }

    private void callbackPermissionSuccess(final String[] permissions) {
        if (permissions != null && mListener != null) {
            mHandler.post(() -> {
                if (mListener != null) mListener.onPermissionsSuccess(permissions);
            });
        }
    }

    private void callbackPermissionFailed(final String permission) {
        if (permission != null && mListener != null) {
            mHandler.post(() -> {
                if (mListener != null) mListener.onPermissionFailed(permission);
            });
        }
    }

    public interface OnPermissionListener {

        void onPermissionsSuccess(String[] permissions);

        void onPermissionFailed(String permission);
    }
}
