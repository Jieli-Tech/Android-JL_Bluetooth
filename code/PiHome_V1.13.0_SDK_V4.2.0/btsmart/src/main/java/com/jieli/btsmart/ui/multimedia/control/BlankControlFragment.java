package com.jieli.btsmart.ui.multimedia.control;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.FragmentBlankControlBinding;
import com.jieli.btsmart.ui.base.BaseFragment;


/**
 * BlankControlFragment
 * @author zqjasonZhong
 * @since 2025/5/7
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 空白控制界面
 */
public class BlankControlFragment extends BaseFragment {
    public final static String KEY_CONTENT_TEXT = "content_text";

    private FragmentBlankControlBinding mBinding;

    public static Fragment newInstance() {
        return new BlankControlFragment();
    }

    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(BlankControlFragment.class.getSimpleName()) != null) {
            return fragmentManager.findFragmentByTag(BlankControlFragment.class.getSimpleName());
        }
        return new BlankControlFragment();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentBlankControlBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        String content = getString(R.string.unconnected_device_tips);
        final Bundle bundle = getArguments();
        if (bundle != null) {
            String text = bundle.getString(KEY_CONTENT_TEXT);
            if (text != null && !text.equals(content)) {
                mBinding.tvUnconnectedDeviceTips.setText(text);
            }
        }
    }
}
