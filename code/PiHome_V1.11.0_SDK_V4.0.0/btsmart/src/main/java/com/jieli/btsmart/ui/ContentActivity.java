package com.jieli.btsmart.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.ui.base.BaseActivity;
import com.jieli.component.utils.SystemUtil;

import java.util.HashMap;
import java.util.Map;

public class ContentActivity extends BaseActivity {

    private final static String KEY_FRAGMENT_TITLE = "KEY_FRAGMMENT_TITLE";
    private final static String KEY_FRAGMENT_BUNDLE = "KEY_FRAGMMENT_BUNDLE";

    private static final long MIN_START_SPACE_TIME = 1000;//两次内容页面打开的最小时间间隔
    private static final Map<String, Long> fastClickLimit = new HashMap<>();

    private TextView tvContentTitle;
    private ImageButton ibBackButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemUtil.setImmersiveStateBar(getWindow(), true);
        setContentView(R.layout.activity_content);
        tvContentTitle = findViewById(R.id.tv_content_title);
        ibBackButton = findViewById(R.id.ib_content_back);
        ibBackButton.setOnClickListener(mOnClickListener);
        String fragmentName = getIntent().getStringExtra(SConstant.KEY_FRAGMENT_TAG);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentName);
        if (fragment == null && fragmentName != null) {
            try {
                fragment = (Fragment) Class.forName(fragmentName).newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (fragment != null) {
            fragment.setArguments(getIntent().getExtras());
            changeFragment(R.id.fl_content_container, fragment, fragmentName);
            tvContentTitle.setText(getIntent().getStringExtra(KEY_FRAGMENT_TITLE));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fastClickLimit.clear();
    }

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
        bundle.putString(SConstant.KEY_FRAGMENT_TAG, fragmentName);
        bundle.putString(KEY_FRAGMENT_TITLE, title);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    public static void startActivityForRequest(Activity activity, String fragmentName, String title, Bundle bundle, int requestCode) {
        if (null == activity || null == fragmentName || fastStart(fragmentName)) return;
        Intent intent = new Intent(activity, ContentActivity.class);
        bundle.putString(SConstant.KEY_FRAGMENT_TAG, fragmentName);
        bundle.putString(KEY_FRAGMENT_TITLE, title);
        intent.putExtras(bundle);
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
        bundle.putString(SConstant.KEY_FRAGMENT_TAG, fragmentName);
        bundle.putString(KEY_FRAGMENT_TITLE, title);
        intent.putExtras(bundle);
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
            JL_Log.d("zzc", "fastStart: fast start. " + fragmentName);
            return true;
        }
        fastClickLimit.put(fragmentName, currentStartTime);
        return false;
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == ibBackButton) {
                if(getCustomBackPress() != null && getCustomBackPress().onBack()) return;
                finish();
            }
        }
    };

}
