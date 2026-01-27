package com.jieli.btsmart.ui.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.SystemUtil;
import com.jieli.component.utils.ToastUtil;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/8/11 7:44 PM
 * @desc :
 */
public abstract class BaseActivity extends AppCompatActivity {
    protected String TAG = getClass().getSimpleName();
    private CustomBackPress mCustomBackPress;


    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if (mCustomBackPress != null && mCustomBackPress.onBack()) {
            return;
        }
        setResult(Activity.RESULT_OK);
        finish();
    }

    public boolean isInvalid() {
        return isFinishing() || isDestroyed();
    }

    public CustomBackPress getCustomBackPress() {
        return mCustomBackPress;
    }

    public void setCustomBackPress(CustomBackPress customBackPress) {
        mCustomBackPress = customBackPress;
    }

    /**
     * 切换fragment(不带tag)
     *
     * @param containerId layout id
     * @param fragment    target fragment
     */
    public void changeFragment(int containerId, Fragment fragment) {
        changeFragment(containerId, fragment, null);
    }

    /**
     * 切换fragment
     *
     * @param containerId layout id
     * @param fragment    fragment
     * @param fragmentTag fragment tag
     */

    public void changeFragment(int containerId, Fragment fragment, String fragmentTag) {
        if (fragment != null && !isFinishing()) {
            Fragment origin = getSupportFragmentManager().findFragmentById(containerId);
            changeFragment(containerId, origin, fragment, fragmentTag);
        }
    }

    /**
     * 切换fragment
     *
     * @param containerId layout id
     * @param origin      fragment
     * @param target      fragment
     * @param fragmentTag fragment tag
     */
    public void changeFragment(int containerId, Fragment origin, Fragment target, String fragmentTag) {
        if (target != null && !isFinishing()) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if (!target.isAdded()) {
                if (!TextUtils.isEmpty(fragmentTag)) {
                    fragmentTransaction.add(containerId, target, fragmentTag);
                } else {
                    fragmentTransaction.add(containerId, target);
                }
            }
            if (origin != null) {
                fragmentTransaction.hide(origin);
            }
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.show(target);
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    public void setWindowStatus() {
        setWindowStatus(R.id.main);
    }

    public void setWindowStatus(int id) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            SystemUtil.setImmersiveStateBar(getWindow(), true);
        } else {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(id), new OnApplyWindowInsetsListener() {
                @Override
                public @NonNull WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                    Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                    v.setPadding(statusBarInsets.left, statusBarInsets.top, statusBarInsets.right, statusBarInsets.bottom);
                    return insets;
                }
            });
        }
    }

    protected void showTips(String tips) {
        ToastUtil.showToastLong(tips);
        JL_Log.d(TAG, tips);
    }

    protected void showTips(String format, Object... args) {
        showTips(AppUtil.formatString(format, args));
    }

    public interface CustomBackPress {

        boolean onBack();
    }
}
