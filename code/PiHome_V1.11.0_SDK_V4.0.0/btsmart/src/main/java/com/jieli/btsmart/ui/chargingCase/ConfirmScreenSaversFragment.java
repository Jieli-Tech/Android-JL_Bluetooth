package com.jieli.btsmart.ui.chargingCase;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.chargingcase.ChargingCaseInfo;
import com.jieli.btsmart.databinding.FragmentConfirmScreenSaversBinding;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.widget.LoadingDialog;
import com.jieli.btsmart.util.AppUtil;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 上传屏幕保护界面
 * @since 2023/12/7
 */
public class ConfirmScreenSaversFragment extends DeviceControlFragment {

    private FragmentConfirmScreenSaversBinding mBinding;
    private ConfirmScreenSaversViewModel mViewModel;

    /**
     * 文件路径
     */
    private String filePath;
    /**
     * 设备屏幕宽度
     */
    private int screenWidth;
    /**
     * 设备屏幕高度
     */
    private int screenHeight;
    private LoadingDialog loadingDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentConfirmScreenSaversBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() == null) {
            requireActivity().finish();
            return;
        }
        filePath = getArguments().getString(SConstant.KEY_FILE_PATH, "");
        if (TextUtils.isEmpty(filePath)) {
            requireActivity().finish();
            return;
        }
        screenWidth = getArguments().getInt(SConstant.KEY_DEVICE_SCREEN_WIDTH, ChargingCaseInfo.SCREEN_WIDTH);
        screenHeight = getArguments().getInt(SConstant.KEY_DEVICE_SCREEN_HEIGHT, ChargingCaseInfo.SCREEN_HEIGHT);
        mViewModel = new ViewModelProvider(this).get(ConfirmScreenSaversViewModel.class);
        JL_Log.i(TAG, "onViewCreated", "filePath : " + filePath);
        initUI();
        addObserver();
    }

    private void initUI() {
        mBinding.sbtnShowIndicator.setOnCheckedChangeListener((buttonView, isChecked) -> updateDisplayUI(isChecked));
        mBinding.btnUpload.setOnClickListener(v -> mViewModel.convertBinFile(requireContext(), filePath, screenWidth, screenHeight, mBinding.sbtnShowIndicator.isChecked()));
        mBinding.sbtnShowIndicator.setCheckedImmediately(true);
    }

    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnectionData -> {
            if (deviceConnectionData.getStatus() != StateCode.CONNECTION_OK) {
                requireActivity().setResult(Activity.RESULT_OK);
                requireActivity().finish();
            }
        });
        mViewModel.convertStateMLD.observe(getViewLifecycleOwner(), state -> {
            if (state.getState() == StateResult.STATE_WORKING) {
                showLoadingDialog();
                return;
            }
            dismissLoadingDialog();
            if (state.getState() == StateResult.STATE_FINISH) {
                if (state.isSuccess()) {
                    ConfirmScreenSaversViewModel.ConvertResult result = state.getData();
                    if (AppUtil.isGif(result.getInputFilePath())) {
                        result.setInputFilePath(findGifPath(result.getInputFilePath(), mBinding.sbtnShowIndicator.isChecked()));
                    }
                    toUploadScreenFragment(result);
                    return;
                }
                JL_Log.w(TAG, AppUtil.formatString("Transcoding failed. code = %d, %s", state.getCode(), state.getMessage()));
                showTips(getString(R.string.upload_failed));
            }
        });
    }

    private String findGifPath(String filePath, boolean isDisplayLock) {
        String flag = AppUtil.formatString("/%s/", isDisplayLock ? SConstant.DIR_LOCK : SConstant.DIR_UNLOCK);
        boolean ret = filePath.contains(flag);
        if (!ret) {
            String replace = AppUtil.formatString("/%s/", isDisplayLock ? SConstant.DIR_UNLOCK : SConstant.DIR_LOCK);
            filePath = filePath.replace(replace, flag);
        }
        return filePath;
    }

    private void updateDisplayUI(boolean isShow) {
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(14));
        if (AppUtil.isGif(filePath)) {
            filePath = findGifPath(filePath, isShow);
            Glide.with(MainApplication.getApplication()).asGif().load(filePath).apply(options)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE).into(mBinding.ivScreenSavers);
            return;
        }
        final CustomTarget<Bitmap> customTarget = new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                mBinding.ivScreenSavers.setBackground(new BitmapDrawable(getResources(), resource));
                mBinding.ivScreenSavers.setImageResource(isShow ? R.drawable.bg_screen_unlock_white : 0);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }
        };
        Glide.with(MainApplication.getApplication()).asBitmap().load(filePath).apply(options)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE).into(customTarget);
    }


    private void showLoadingDialog() {
        if (null == loadingDialog) {
            loadingDialog = new LoadingDialog(getString(R.string.loading));
        }
        if (!loadingDialog.isShow() && isAdded() && !isDetached()) {
            loadingDialog.show(getChildFragmentManager(), "Loading Dialog");
        }
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null) {
            if (loadingDialog.isShow() && isAdded() && !isDetached()) {
                loadingDialog.dismiss();
            }
            loadingDialog = null;
        }
    }

    private void toUploadScreenFragment(ConfirmScreenSaversViewModel.ConvertResult result) {
        JL_Log.d(TAG, "toUploadScreenFragment", "" + result);
        Bundle bundle = new Bundle();
        bundle.putParcelable(SConstant.KEY_CONVERT_RESULT, result);
        ContentActivity.startActivity(requireContext(), UploadScreenSaverFragment.class.getCanonicalName(), getString(R.string.charging_case_update), bundle);
        requireActivity().finish();
    }
}