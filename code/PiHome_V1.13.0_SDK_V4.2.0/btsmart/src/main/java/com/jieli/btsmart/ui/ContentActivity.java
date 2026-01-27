package com.jieli.btsmart.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.databinding.ActivityContentBinding;
import com.jieli.btsmart.ui.base.BaseActivity;

import java.util.HashMap;
import java.util.Map;

public class ContentActivity extends BaseActivity {

    private final static String KEY_FRAGMENT_TITLE = "KEY_FRAGMMENT_TITLE";
    private final static String KEY_FRAGMENT_BUNDLE = "KEY_FRAGMMENT_BUNDLE";

    private static final long MIN_START_SPACE_TIME = 1000;//两次内容页面打开的最小时间间隔
    private static final Map<String, Long> fastClickLimit = new HashMap<>();

    public static void startActivity(Context context, String fragmentName, @StringRes int title) {
        if (null == context) return;
        startActivity(context, fragmentName, context.getString(title));
    }

    public static void startActivity(Context context, String fragmentName, String title) {
        startActivity(context, fragmentName, title, new Bundle());
    }

    public static void startActivity(Context context, String fragmentName, Bundle bundle) {
        startActivity(context, fragmentName, "", bundle);
    }

    public static void startActivity(Context context, String fragmentName, String title, Bundle bundle) {
        if (null == context || null == fragmentName || fastStart(fragmentName)) return;
        Intent intent = new Intent(context, ContentActivity.class);
        intent.putExtra(SConstant.KEY_FRAGMENT_TAG, fragmentName);
        intent.putExtra(KEY_FRAGMENT_TITLE, title);
        intent.putExtra(KEY_FRAGMENT_BUNDLE, bundle);
        context.startActivity(intent);
    }

    public static void startActivityForRequest(Activity activity, String fragmentName, String title, Bundle bundle, int requestCode) {
        if (null == activity || null == fragmentName || fastStart(fragmentName)) return;
        Intent intent = new Intent(activity, ContentActivity.class);
        intent.putExtra(SConstant.KEY_FRAGMENT_TAG, fragmentName);
        intent.putExtra(KEY_FRAGMENT_TITLE, title);
        intent.putExtra(KEY_FRAGMENT_BUNDLE, bundle);
        activity.startActivityForResult(intent, requestCode);
    }


    public static void startActivityForRequest(Fragment fragment, int requestCode, String fragmentName, String title, Bundle bundle) {
        startActivityForRequest(fragment, fragmentName, title, bundle, requestCode, null);
    }

    public static void startActivityForRequest(Fragment fragment, String fragmentName, String title, Bundle bundle, ActivityResultLauncher<Intent> launcher) {
        startActivityForRequest(fragment, fragmentName, title, bundle, 0, launcher);
    }

    private static void startActivityForRequest(Fragment fragment, String fragmentName, String title, Bundle bundle, int requestCode, ActivityResultLauncher<Intent> launcher) {
        if (null == fragment || null == fragmentName || fastStart(fragmentName)) return;
        Intent intent = new Intent(fragment.requireActivity(), ContentActivity.class);
        intent.putExtra(SConstant.KEY_FRAGMENT_TAG, fragmentName);
        intent.putExtra(KEY_FRAGMENT_TITLE, title);
        intent.putExtra(KEY_FRAGMENT_BUNDLE, bundle);
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
            JL_Log.d("ContentActivity", "fastStart", "fast start. " + fragmentName);
            return true;
        }
        fastClickLimit.put(fragmentName, currentStartTime);
        return false;
    }

    /**
     * 当前Fragment名称
     */
    private String curFragmentName = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        ActivityContentBinding binding = ActivityContentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setWindowStatus(R.id.rl_content_main);
        final Intent intent = getIntent();
        if (null == intent) {
            finish();
            return;
        }
        String fragmentName = intent.getStringExtra(SConstant.KEY_FRAGMENT_TAG);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentName);
        if (fragment == null && fragmentName != null) {
            try {
                fragment = (Fragment) Class.forName(fragmentName).newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (null == fragment) {
            finish();
            return;
        }
        curFragmentName = fragmentName;
        binding.viewTopBar.tvLeft.setOnClickListener(v -> {
            if (getCustomBackPress() != null && getCustomBackPress().onBack()) return;
            finish();
        });
        binding.viewTopBar.tvTitle.setText(intent.getStringExtra(KEY_FRAGMENT_TITLE));
        fragment.setArguments(intent.getBundleExtra(KEY_FRAGMENT_BUNDLE));
        changeFragment(R.id.fl_content_container, fragment, fragmentName);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!TextUtils.isEmpty(curFragmentName)) {
            fastClickLimit.remove(curFragmentName);
            curFragmentName = "";
        }
    }
}
