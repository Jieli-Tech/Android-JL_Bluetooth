package com.jieli.btsmart.ui.launcher;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.amap.api.maps.MapsInitializer;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.BuildConfig;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.databinding.ActivityLauncherBinding;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.home.HomeActivity;
import com.jieli.btsmart.ui.settings.app.WebBrowserFragment;
import com.jieli.btsmart.ui.widget.UserServiceDialog;
import com.jieli.btsmart.util.JL_MediaPlayerServiceManager;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.component.ActivityManager;
import com.jieli.component.base.Jl_BaseActivity;
import com.jieli.component.network.WifiHelper;
import com.jieli.component.utils.SystemUtil;
import com.jieli.jl_dialog.Jl_Dialog;

/**
 * 启动页界面
 */
public class LauncherActivity extends Jl_BaseActivity {
    private LauncherVM mViewModel;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> openGpsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> toHomeActivity());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemUtil.setImmersiveStateBar(getWindow(), true);
        ActivityLauncherBinding binding = ActivityLauncherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mViewModel = new ViewModelProvider(this).get(LauncherVM.class);

        JL_Log.i(TAG, "APP version : " + SystemUtil.getVersionName(getApplicationContext()) + ", code = " + SystemUtil.getVersion(getApplicationContext()));
        if (BuildConfig.OPEN_LAUNCHER_ANIM) {
            Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale);
            scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    updateView();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            binding.rlLauncherLayout.startAnimation(scaleAnimation);
            return;
        }
        updateView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

    private void updateView() {
        if (!mViewModel.isAgreeUserAgreement()) {
            MapsInitializer.updatePrivacyShow(getApplicationContext(), true, true);
            showUserServiceTipsDialog();
            return;
        }

        MainApplication.privacyPolicyAgreed();
        MapsInitializer.updatePrivacyShow(getApplicationContext(), true, true);
        MapsInitializer.updatePrivacyAgree(getApplicationContext(), true);
        if (PermissionUtil.isHasStoragePermission(this)) {
            JL_MediaPlayerServiceManager.getInstance().bindService();
        }
        MainApplication.getApplication().uploadAppInfo();

        if (mViewModel.isAllowRequestFloatingPermission(getApplicationContext())) {
            showRequestFloatingDialog(getString(R.string.request_floating_window_permission_tips));
        } else {
            toHomeActivity();
        }
    }

    private void showUserServiceTipsDialog() {
        final String flag = UserServiceDialog.class.getName();
        UserServiceDialog dialog = (UserServiceDialog) getSupportFragmentManager().findFragmentByTag(flag);
        if (null == dialog) {
            dialog = new UserServiceDialog();
            dialog.setOnUserServiceListener(new UserServiceDialog.OnUserServiceListener() {
                @Override
                public void onUserService() {
                    toWebFragment(0);
                }

                @Override
                public void onPrivacyPolicy() {
                    toWebFragment(1);
                }

                @Override
                public void onExit(DialogFragment dialogFragment) {
                    mViewModel.setAgreeUserAgreement(false);
                    MapsInitializer.updatePrivacyAgree(getApplicationContext(), false);
                    dialogFragment.dismiss();
                    ActivityManager.getInstance().popAllActivity();
                    finish();
//            System.exit(0);
                }

                @Override
                public void onAgree(DialogFragment dialogFragment) {
                    JL_Log.i(TAG, " ==================== onAgree ==================");
                    dialogFragment.dismiss();
                    mViewModel.setAgreeUserAgreement(true);
                    updateView();
                }
            });
        }
        if (!dialog.isShow()) dialog.show(getSupportFragmentManager(), flag);
    }

    private void showRequestFloatingDialog(String text) {
        if (isFinishing() || isDestroyed()) return;
        final String tag = "request_floating_permission";
        Jl_Dialog dialog = (Jl_Dialog) getSupportFragmentManager().findFragmentByTag(tag);
        if (dialog == null) {
            dialog = Jl_Dialog.builder()
                    .title(getString(R.string.tips))
                    .content(text)
                    .showProgressBar(false)
                    .width(0.8f)
                    .left(getString(R.string.allow))
                    .leftClickListener((v, dialogFragment) -> {
                        mViewModel.setBanRequestFloatingWindowPermission(getApplicationContext(), false);
                        dialogFragment.dismiss();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            openGpsLauncher.launch(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                        } else {
                            toHomeActivity();
                        }
                    })
                    .right(getString(R.string.cancel))
                    .rightClickListener((v, dialogFragment) -> {
                        mViewModel.setBanRequestFloatingWindowPermission(getApplicationContext(), true);
                        dialogFragment.dismiss();
                        toHomeActivity();
                    })
                    .cancel(false)
                    .build();
        }
        if (!dialog.isShow()) {
            dialog.show(getSupportFragmentManager(), tag);
        }
    }

    private void toWebFragment(int flag) {
        Bundle bundle = new Bundle();
        bundle.putInt(SConstant.KEY_WEB_FLAG, flag);
        CommonActivity.startCommonActivity(LauncherActivity.this,
                WebBrowserFragment.class.getCanonicalName(), bundle);
    }

    private void toHomeActivity() {
        WifiHelper.getInstance(this).registerBroadCastReceiver(this);
        mHandler.postDelayed(() -> {
            startActivity(new Intent(this, HomeActivity.class));
            mHandler.postDelayed(LauncherActivity.this::finish, 500);
        }, 1500);
    }
}
