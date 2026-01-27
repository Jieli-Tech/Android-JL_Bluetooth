package com.jieli.btsmart.ui.multimedia.control.linein;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.btsmart.databinding.FragmentLineinControlBinding;
import com.jieli.btsmart.ui.base.BaseFragment;


/**
 * LineInControlFragment
 * @author zqjasonZhong
 * @since 2025/5/7
 * @email zhongzhuocheng@zh-jieli.com
 * @desc LineIn控制界面
 */
public class LineInControlFragment extends BaseFragment {

    private FragmentLineinControlBinding mBinding;
    private LineInControlVM mViewModel;

    public static LineInControlFragment newInstance() {
        return new LineInControlFragment();
    }

    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(LineInControlFragment.class.getSimpleName()) != null) {
            return fragmentManager.findFragmentByTag(LineInControlFragment.class.getSimpleName());
        }
        return new LineInControlFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentLineinControlBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(LineInControlVM.class);
        initUI();
        addObserver();
        mViewModel.refresh();
    }

    private void initUI() {
        mBinding.ibPlayOrPause.setOnClickListener(v -> mViewModel.playOrPause());
    }

    private void addObserver() {
        mViewModel.playStatusMLD.observe(getViewLifecycleOwner(), isPlay -> mBinding.ibPlayOrPause.setSelected(isPlay));
    }
}
