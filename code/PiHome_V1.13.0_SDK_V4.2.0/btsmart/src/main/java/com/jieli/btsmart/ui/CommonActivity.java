package com.jieli.btsmart.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.databinding.ActivityCommonBinding;
import com.jieli.btsmart.ui.base.BaseActivity;
import com.jieli.btsmart.util.UIHelper;

import java.util.HashMap;
import java.util.Map;

public class CommonActivity extends BaseActivity {

    private static final String TAG = CommonActivity.class.getSimpleName();
    private static final long MIN_START_SPACE_TIME = 1000;//两次内容页面打开的最小时间间隔
    private static final Map<String, Long> fastClickLimit = new HashMap<>();

    public static void startCommonActivity(Activity activity, String fragmentTag) {
        startCommonActivity(activity, fragmentTag, null);
    }

    public static void startCommonActivity(Activity activity, String fragmentTag, Bundle bundle) {
        startCommonActivity(activity, 0, fragmentTag, bundle);
    }

    public static void startCommonActivity(Activity activity, int requestCode, String fragmentTag, Bundle bundle) {
        if (activity == null || fastStart(fragmentTag)) return;
        Intent intent = new Intent(activity, CommonActivity.class);
        intent.putExtra(SConstant.KEY_FRAGMENT_TAG, fragmentTag);
        intent.putExtra(SConstant.KEY_FRAGMENT_BUNDLE, bundle);
        if (requestCode > 0) {
            activity.startActivityForResult(intent, requestCode);
        } else {
            activity.startActivity(intent);
        }
    }

    public static void startCommonActivity(Fragment fragment, int requestCode, String fragmentTag, Bundle bundle) {
        if (fragment == null || fragment.isDetached() || fastStart(fragmentTag)) return;
        Intent intent = new Intent(fragment.getContext(), CommonActivity.class);
        intent.putExtra(SConstant.KEY_FRAGMENT_TAG, fragmentTag);
        intent.putExtra(SConstant.KEY_FRAGMENT_BUNDLE, bundle);
        if (requestCode > 0) {
            fragment.startActivityForResult(intent, requestCode);
        } else {
            fragment.startActivity(intent);
        }
    }

    public static void startActivityForRequest(Fragment fragment, String fragmentTag, Bundle bundle, @NonNull ActivityResultLauncher<Intent> launcher) {
        startActivityForRequest(fragment, fragmentTag, bundle, 0, launcher);
    }

    public static void startActivityForRequest(Fragment fragment, String fragmentTag, Bundle bundle, int requestCode, ActivityResultLauncher<Intent> launcher) {
        if (null == fragment || null == fragmentTag || fastStart(fragmentTag)) return;
        Intent intent = new Intent(fragment.requireContext(), CommonActivity.class);
        intent.putExtra(SConstant.KEY_FRAGMENT_TAG, fragmentTag);
        intent.putExtra(SConstant.KEY_FRAGMENT_BUNDLE, bundle);
        if (null != launcher) {
            launcher.launch(intent);
            return;
        }
        fragment.startActivityForResult(intent, requestCode);
    }

    private static boolean fastStart(String fragmentName) {
        if (null == fragmentName) return true;  //错误参数，直接不予处理
        Long startTime = fastClickLimit.get(fragmentName);
        long currentStartTime = System.currentTimeMillis();
        if (startTime != null && currentStartTime - startTime < MIN_START_SPACE_TIME) {  //判断属于快速点击，不予处理
            JL_Log.d(TAG, "fastStart", "fast start. " + fragmentName);
            return true;
        }
        fastClickLimit.put(fragmentName, currentStartTime);
        return false;
    }

    private ActivityCommonBinding mBinding;
    private Fragment mLastFragment;
    private String fragmentClazz;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mBinding = ActivityCommonBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setWindowStatus();

        Intent intent = getIntent();
        if (null == intent) return;
        String tag = intent.getStringExtra(SConstant.KEY_FRAGMENT_TAG);
        if (TextUtils.isEmpty(tag)) {
            finish();
            return;
        }
        Bundle bundle = intent.getBundleExtra(SConstant.KEY_FRAGMENT_BUNDLE);
        switchSubFragment(tag, bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        sendActivityResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fragmentClazz != null) {
            fastClickLimit.remove(fragmentClazz);
            fragmentClazz = null;
        }
    }

    public void updateTopBar(String title, int leftRes, View.OnClickListener leftListener, int rightRes, View.OnClickListener rightListener) {
        if (title != null) {
            mBinding.rlCommonTopBar.tvCommonTopBarTitle.setText(title);
        }
        if (leftRes != 0) {
            UIHelper.show(mBinding.rlCommonTopBar.ivCommonTopBarLeftImg);
            mBinding.rlCommonTopBar.ivCommonTopBarLeftImg.setImageResource(leftRes);
        } else {
            UIHelper.gone(mBinding.rlCommonTopBar.ivCommonTopBarLeftImg);
        }
        if (leftListener != null) {
            mBinding.rlCommonTopBar.ivCommonTopBarLeftImg.setOnClickListener(leftListener);
        }
        if (rightRes != 0) {
            UIHelper.show(mBinding.rlCommonTopBar.ivCommonTopBarRightImg);
            mBinding.rlCommonTopBar.ivCommonTopBarRightImg.setImageResource(rightRes);
        } else {
            UIHelper.gone(mBinding.rlCommonTopBar.ivCommonTopBarRightImg);
        }
        if (rightListener != null) {
            mBinding.rlCommonTopBar.ivCommonTopBarRightImg.setOnClickListener(rightListener);
        }
    }

    public void updateTopBar(String title, View leftView, View rightView) {
        if (title != null) {
            mBinding.rlCommonTopBar.tvCommonTopBarTitle.setText(title);
        }
        mBinding.rlCommonTopBar.flCommonTopBarLeft.removeAllViews();
        mBinding.rlCommonTopBar.flCommonTopBarRight.removeAllViews();
        if (leftView != null) {
            UIHelper.show(mBinding.rlCommonTopBar.flCommonTopBarLeft);
            mBinding.rlCommonTopBar.flCommonTopBarLeft.addView(leftView);
        } else {
            UIHelper.gone(mBinding.rlCommonTopBar.flCommonTopBarLeft);
        }

        if (rightView != null) {
            UIHelper.show(mBinding.rlCommonTopBar.flCommonTopBarRight);
            mBinding.rlCommonTopBar.flCommonTopBarRight.addView(rightView);
        } else {
            UIHelper.gone(mBinding.rlCommonTopBar.flCommonTopBarRight);
        }
    }

    public Fragment getCurrentFragment() {
        return mLastFragment;
    }

    private void switchSubFragment(String tag, Bundle bundle) {
        if (TextUtils.isEmpty(tag)) return;
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fl_common_container);
        if (fragment != null && fragment.getClass().getSimpleName().equals(tag)) {
            if (bundle != null) {
                fragment.setArguments(bundle);
            }
            fragmentClazz = tag;
            fragment.onAttach(getApplicationContext());
            return;
        }
        fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment == null) {
            try {
                fragment = (Fragment) Class.forName(tag).newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (null == fragment) return;
        if (bundle != null) {
            fragment.setArguments(bundle);
        }
        fragmentClazz = tag;
        changeFragment(R.id.fl_common_container, mLastFragment, fragment, tag);
        mLastFragment = fragment;
    }

    private void sendActivityResume() {
        sendBroadcast(new Intent(SConstant.ACTION_ACTIVITY_RESUME));
    }
}
