package com.jieli.btsmart.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.ui.base.BaseActivity;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.SystemUtil;

import java.util.HashMap;
import java.util.Map;

public class CommonActivity extends BaseActivity {

    private ImageView ivCommonTopBarLeftImg;
    private ImageView ivCommonTopBarRightImg;
    private TextView tvCommonTopBarTitle;
    private FrameLayout frameLayoutLeft;
    private FrameLayout frameLayoutRight;

    private Fragment mLastFragment;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemUtil.setImmersiveStateBar(getWindow(), true);
        setContentView(R.layout.activity_common);
        ivCommonTopBarLeftImg = findViewById(R.id.iv_common_top_bar_left_img);
        ivCommonTopBarRightImg = findViewById(R.id.iv_common_top_bar_right_img);
        tvCommonTopBarTitle = findViewById(R.id.tv_common_top_bar_title);
        frameLayoutLeft = findViewById(R.id.fl_common_top_bar_left);
        frameLayoutRight = findViewById(R.id.fl_common_top_bar_right);

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
        fastClickLimit.clear();
    }

    public void updateTopBar(String title, int leftRes, View.OnClickListener leftListener, int rightRes, View.OnClickListener rightListener) {
        if (tvCommonTopBarTitle != null && title != null) {
            tvCommonTopBarTitle.setText(title);
        }
        if (ivCommonTopBarLeftImg != null) {
            if (leftRes != 0) {
                ivCommonTopBarLeftImg.setVisibility(View.VISIBLE);
                ivCommonTopBarLeftImg.setImageResource(leftRes);
            } else {
                ivCommonTopBarLeftImg.setVisibility(View.GONE);
            }
            if (leftListener != null) {
                ivCommonTopBarLeftImg.setOnClickListener(leftListener);
            }
        }
        if (ivCommonTopBarRightImg != null) {
            if (rightRes != 0) {
                ivCommonTopBarRightImg.setVisibility(View.VISIBLE);
                ivCommonTopBarRightImg.setImageResource(rightRes);
            } else {
                ivCommonTopBarRightImg.setVisibility(View.GONE);
            }
            if (rightListener != null) {
                ivCommonTopBarRightImg.setOnClickListener(rightListener);
            }
        }
    }

    public void updateTopBar(String title, View leftView, View rightView) {
        if (tvCommonTopBarTitle != null && title != null) {
            tvCommonTopBarTitle.setText(title);
        }
        frameLayoutLeft.removeAllViews();
        frameLayoutRight.removeAllViews();
        if (leftView != null) {
            frameLayoutLeft.setVisibility(View.VISIBLE);
            frameLayoutLeft.addView(leftView);
        } else {
            frameLayoutLeft.setVisibility(View.GONE);
        }

        if (rightView != null) {
            frameLayoutRight.setVisibility(View.VISIBLE);
            frameLayoutRight.addView(rightView);
        } else {
            frameLayoutRight.setVisibility(View.GONE);
        }
    }

    public Fragment getCurrentFragment() {
        return mLastFragment;
    }


    private static boolean fastStart(String fragmentName) {
        if (null == fragmentName) return true;  //错误参数，直接不予处理
        Long startTime = fastClickLimit.get(fragmentName);
        long currentStartTime = System.currentTimeMillis();
        if (startTime != null && currentStartTime - startTime < MIN_START_SPACE_TIME) {  //判断属于快速点击，不予处理
            JL_Log.d("zzc", "fastStart: fast start. " + fragmentName);
            return true;
        }
        fastClickLimit.put(fragmentName, currentStartTime);
        return false;
    }

    private void switchSubFragment(String tag, Bundle bundle) {
        if (TextUtils.isEmpty(tag)) return;
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fl_common_container);
        if (fragment != null && fragment.getClass().getSimpleName().equals(tag)) {
            if (bundle != null) {
                fragment.setArguments(bundle);
            }
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
            if (fragment instanceof Jl_BaseFragment) {
                ((Jl_BaseFragment) fragment).setBundle(bundle);
            }
        }
        changeFragment(R.id.fl_common_container, mLastFragment, fragment, tag);
        mLastFragment = fragment;
    }

    private void sendActivityResume() {
        sendBroadcast(new Intent(SConstant.ACTION_ACTIVITY_RESUME));
    }
}
