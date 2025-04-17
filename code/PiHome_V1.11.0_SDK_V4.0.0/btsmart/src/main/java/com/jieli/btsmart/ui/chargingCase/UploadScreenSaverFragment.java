package com.jieli.btsmart.ui.chargingCase;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.databinding.FragmentUploadScreenSaverBinding;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.base.Jl_BaseActivity;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 上传屏幕保护图片页面
 * @since 2023/12/8
 */
public class UploadScreenSaverFragment extends DeviceControlFragment {

    private FragmentUploadScreenSaverBinding mBinding;
    private UploadScreenSaverViewModel mViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentUploadScreenSaverBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Bundle bundle = getArguments();
        if (null == bundle) {
            requireActivity().finish();
            return;
        }
        ConfirmScreenSaversViewModel.ConvertResult result = bundle.getParcelable(SConstant.KEY_CONVERT_RESULT);
        if (null == result) {
            requireActivity().finish();
            return;
        }
        mViewModel = new ViewModelProvider(this).get(UploadScreenSaverViewModel.class);
        mViewModel.convertResult = result;
        if (requireActivity() instanceof Jl_BaseActivity) {
            ((Jl_BaseActivity)requireActivity()).setCustomBackPress(() -> {
                if(mViewModel.isTransferring()){
                    showTips(getString(R.string.transferring_file_tips));
                    return true;
                }
                return false;
            });
        }
        initUI();
        addObserver();
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mViewModel.transferScreenSaver();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel.cancelTransfer();
    }

    private void initUI() {
        mBinding.btnOperation.setOnClickListener(v -> {
            final StateResult<Integer> stateResult = mViewModel.transferStateMLD.getValue();
            if (null == stateResult) return;
            final int state = stateResult.getState();
            JL_Log.d(TAG, "btnOperation", "state = " + state);
            if (state == UploadScreenSaverViewModel.STATE_WORKING) {
                mViewModel.cancelTransfer();
            } else if (state == UploadScreenSaverViewModel.STATE_STOP) {
                if (stateResult.isSuccess()) {
                    requireActivity().setResult(Activity.RESULT_OK);
                    requireActivity().finish();
                    return;
                }
                requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                mViewModel.transferScreenSaver();
            }
        });
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(14));
        final String filePath = mViewModel.convertResult.getInputFilePath();
        if (AppUtil.isGif(filePath)) {
            Glide.with(MainApplication.getApplication()).asGif().load(filePath).apply(options)
                    .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(mBinding.ivScreenSaversEffect);
        } else {
            Glide.with(MainApplication.getApplication()).asBitmap().load(filePath).apply(options)
                    .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(mBinding.ivScreenSaversEffect);
        }
    }

    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnectionData -> {
            if (deviceConnectionData.getStatus() != StateCode.CONNECTION_OK) {
                requireActivity().setResult(Activity.RESULT_OK);
                requireActivity().finish();
            }
        });
        mViewModel.transferStateMLD.observe(getViewLifecycleOwner(), this::updateStateUI);
    }

    private void updateStateUI(StateResult<Integer> state) {
        if (null == state) return;
        JL_Log.d(TAG, "updateStateUI", "" + state);
        switch (state.getState()) {
            case UploadScreenSaverViewModel.STATE_IDLE: {
                mBinding.groupUploading.setVisibility(View.INVISIBLE);
                mBinding.tvState.setVisibility(View.INVISIBLE);
                mBinding.btnOperation.setVisibility(View.INVISIBLE);
                mBinding.pbTransferProgress.setProgress(0);
                mBinding.tvProgress.setText("0%");
                break;
            }
            case UploadScreenSaverViewModel.STATE_WORKING: {
                mBinding.groupUploading.setVisibility(View.VISIBLE);
                mBinding.tvState.setVisibility(View.INVISIBLE);
                mBinding.btnOperation.setVisibility(View.VISIBLE);
                mBinding.btnOperation.setText(getString(R.string.cancel));
                mBinding.btnOperation.setBackgroundResource(R.drawable.bg_btn_gray_24_shape);
                if (state.getData() != null && state.getData() >= 0) {
                    mBinding.pbTransferProgress.setProgress(state.getData());
                    mBinding.tvProgress.setText(AppUtil.formatString("%d%%", state.getData()));
                }
                break;
            }
            case UploadScreenSaverViewModel.STATE_CANCEL: {
                mBinding.groupUploading.setVisibility(View.INVISIBLE);
                mBinding.btnOperation.setVisibility(View.INVISIBLE);
                mBinding.tvState.setVisibility(View.VISIBLE);
                mBinding.tvState.setText(getString(R.string.cancel_upload_image));
                break;
            }
            case UploadScreenSaverViewModel.STATE_STOP: {
                mBinding.groupUploading.setVisibility(View.INVISIBLE);
                mBinding.tvState.setVisibility(View.VISIBLE);
                mBinding.btnOperation.setVisibility(View.VISIBLE);
                mBinding.btnOperation.setBackgroundResource(R.drawable.bg_btn_purple_gray_24_selector);
                mBinding.btnOperation.setTextColor(ContextCompat.getColor(requireContext(), R.color.white_ffffff));
                requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (state.isSuccess()) {
                    Intent intent = new Intent(SConstant.ACTION_SCREEN_SAVER_CHANGE);
                    intent.putExtra(SConstant.KEY_FILE_PATH, state.getMessage());
                    requireActivity().sendBroadcast(intent);
                    mBinding.tvState.setText(getString(R.string.upload_success));
                    mBinding.btnOperation.setText(getString(R.string.finish));
                    return;
                }
                mBinding.tvState.setText(getString(R.string.upload_failed));
                mBinding.btnOperation.setText(getString(R.string.retry));
                JL_Log.d(TAG, "updateStateUI", "upload failed. code = " + state.getCode() + ", " + state.getMessage());
            }
        }
    }
}