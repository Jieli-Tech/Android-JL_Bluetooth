package com.jieli.btsmart.ui.base;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.network.NetworkDetectionHelper;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.home.HomeActivity;
import com.jieli.component.ActivityManager;
import com.jieli.component.utils.ToastUtil;

/**
 * NetworkActivity
 * @author zqjasonZhong
 * @since 2025/5/7
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 检查网络的界面
 */
public class NetworkActivity extends BaseActivity {
    protected final NetworkDetectionHelper mNetworkDetectionHelper = NetworkDetectionHelper.getInstance();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNetworkDetectionHelper.addOnNetworkDetectionListener(mDetectionListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNetworkDetectionHelper.removeOnNetworkDetectionListener(mDetectionListener);
    }

    private final NetworkDetectionHelper.OnNetworkDetectionListener mDetectionListener = (type, available) -> {
        if (available && type == ConnectivityManager.TYPE_MOBILE) {
            Activity currentActivity = ActivityManager.getInstance().getCurrentActivity();
            if (currentActivity instanceof CommonActivity || currentActivity instanceof HomeActivity) {
                ToastUtil.showToastShort(getString(R.string.mobile_network_tip));
            }
        }
    };
}
