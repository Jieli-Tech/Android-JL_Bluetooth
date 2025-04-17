package com.jieli.btsmart.ui.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import com.jieli.component.base.Jl_BaseFragment;

public abstract class BaseViewModelFragment<VB extends ViewDataBinding> extends Jl_BaseFragment {
    protected VB mBinding = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setRetainInstance(true);
        if (mBinding == null) {
            mBinding = DataBindingUtil.inflate(inflater, getLayoutId(), container, false);
            actionsOnViewInflate();
        }

        if (mBinding != null) {
            View rootView = mBinding.getRoot();
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent !=null){
                parent.removeView(rootView);
            }
            return rootView;
        } else {
            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }

    public void actionsOnViewInflate() {

    }

    public abstract int getLayoutId();
}
