package com.jieli.btsmart.ui.chargingCase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import com.jieli.btsmart.data.model.chargingcase.ResourceMsg;
import com.jieli.btsmart.databinding.FragmentConfirmScreenSaversBinding;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.ChargingBinUtil;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 编辑屏幕保护界面
 * @since 2023/12/7
 */
public class ConfirmScreenSaversFragment extends DeviceControlFragment {

    public static void goToFragment(@NonNull Context context, @NonNull ResourceMsg resourceMsg) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(SConstant.KEY_RESOURCE_MESSAGE, resourceMsg);
        bundle.setClassLoader(resourceMsg.getClass().getClassLoader());
        ContentActivity.startActivity(context, ConfirmScreenSaversFragment.class.getCanonicalName(),
                context.getString(R.string.screen_savers), bundle);
    }

    /**
     * 编辑屏保逻辑实现
     */
    private FragmentConfirmScreenSaversBinding mBinding;
    /**
     * 编辑屏保UI处理
     */
    private ConfirmScreenSaversViewModel mViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentConfirmScreenSaversBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Bundle bundle = getArguments();
        if (null == bundle) {
            finish();
            return;
        }
        ResourceMsg resourceMSg = bundle.getParcelable(SConstant.KEY_RESOURCE_MESSAGE);
        if (null == resourceMSg) {
            finish();
            return;
        }
        JL_Log.i(TAG, "onViewCreated", "" + resourceMSg);
        mViewModel = new ViewModelProvider(requireActivity(), new ConfirmScreenSaversViewModel.Factory(resourceMSg))
                .get(ConfirmScreenSaversViewModel.class);
        initUI();
        addObserver();
    }

    private void initUI() {
        mBinding.sbtnShowIndicator.setOnCheckedChangeListener((buttonView, isChecked) -> updateDisplayUI(isChecked));
        mBinding.btnUpload.setOnClickListener(v -> mViewModel.convertUploadFile(mBinding.sbtnShowIndicator.isChecked()));
        mBinding.sbtnShowIndicator.setCheckedImmediately(true);

        updateDisplayUI(mBinding.sbtnShowIndicator.isChecked());
    }

    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnectionData -> {
            if (deviceConnectionData.getStatus() != StateCode.CONNECTION_OK) {
                finish();
            }
        });
        mViewModel.convertStateMLD.observe(getViewLifecycleOwner(), state -> {
            if (state.getState() == StateResult.STATE_WORKING) {
                showLoadingDialog(getString(R.string.loading));
                return;
            }
            dismissLoadingDialog();
            if (state.getState() == StateResult.STATE_FINISH) {
                if (state.isSuccess()) {
                    toUploadScreenFragment();
                    return;
                }
                JL_Log.w(TAG, AppUtil.formatString("Transcoding failed. code = %d, %s", state.getCode(), state.getMessage()));
                showTips(getString(R.string.upload_failed));
            }
        });
    }

    private void updateDisplayUI(boolean isShow) {
        final Context context = MainApplication.getApplication();
        final String filePath = mViewModel.getResourceMsg().getSrcFilePath();
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(14));
        if (ChargingBinUtil.isGif(filePath)) {
            String gifPath = ChargingBinUtil.findGifPath(filePath, isShow);
            Glide.with(context).asGif().load(gifPath).apply(options)
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
        Glide.with(context).asBitmap().load(filePath).apply(options)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE).into(customTarget);
    }

    private void toUploadScreenFragment() {
        final ResourceMsg resourceMsg = mViewModel.getResourceMsg();
        UploadResourceFragment.goToFragment(requireContext(), resourceMsg);
        finish();
    }
}