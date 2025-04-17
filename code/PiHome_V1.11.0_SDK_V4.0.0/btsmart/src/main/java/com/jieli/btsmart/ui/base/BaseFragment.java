package com.jieli.btsmart.ui.base;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.callbacks.OnFragmentLifeCircle;
import com.jieli.component.utils.ToastUtil;


/**
 * Fragment的基类
 *
 * @author zqjasonZhong
 * date : 2017/11/10
 */
public class BaseFragment extends Jl_BaseFragment {
    protected String TAG = getClass().getSimpleName();
    private Bundle bundle;

    private OnFragmentLifeCircle mOnFragmentLifeCircle;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mOnFragmentLifeCircle != null) {
            mOnFragmentLifeCircle.onActivityCreated(this, getActivity());
        }
    }

    @Override
    public void onDestroyView() {
        if (mOnFragmentLifeCircle != null) {
            mOnFragmentLifeCircle.onDestroyView(this, getActivity());
        }
        super.onDestroyView();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (mOnFragmentLifeCircle != null) {
            if (hidden) {
                mOnFragmentLifeCircle.onHidden(this, getActivity());
            } else {
                mOnFragmentLifeCircle.onShow(this, getActivity());
            }
        }
    }


    public void setOnFragmentLifeCircle(OnFragmentLifeCircle lifeCircle) {
        this.mOnFragmentLifeCircle = lifeCircle;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    /**
     * 切换fragment
     *
     * @param containerId 控件id
     * @param fragment    切换fragment
     * @param fragmentTag fragment tag
     */
    public void changeFragment(int containerId, Fragment fragment, String fragmentTag) {
        if (fragment != null && isAdded() && !isDetached()) {
            FragmentManager fragmentManager = getChildFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if (!TextUtils.isEmpty(fragmentTag)) {
                fragmentTransaction.replace(containerId, fragment, fragmentTag);
            } else {
                fragmentTransaction.replace(containerId, fragment);
            }
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    public void changeFragment(int containerId, Fragment fragment) {
        changeFragment(containerId, fragment, null);
    }

    protected void showTips(String tips) {
        ToastUtil.showToastLong(tips);
        JL_Log.d(TAG, tips);
    }

    protected void showTips(String format, Object... args) {
        showTips(AppUtil.formatString(format, args));
    }
}
