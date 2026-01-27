package com.jieli.btsmart.ui.chargingCase;

import android.app.Activity;
import android.content.Context;
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
import com.jieli.bluetooth.bean.charging_case.ChargingCaseInfo;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.chargingcase.ResourceMsg;
import com.jieli.btsmart.databinding.FragmentUploadResourceBinding;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.base.BaseActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.ChargingBinUtil;
import com.jieli.btsmart.util.UIHelper;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 上传资源页面
 * @since 2023/12/8
 */
public class UploadResourceFragment extends DeviceControlFragment {

    public static void goToFragment(@NonNull Context context, @NonNull ResourceMsg resourceMsg) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(SConstant.KEY_RESOURCE_MESSAGE, resourceMsg);
        bundle.setClassLoader(resourceMsg.getClass().getClassLoader());
        String title = (resourceMsg.getResourceType() == ChargingCaseInfo.TYPE_WALLPAPER) ?
                context.getString(R.string.wallpaper_update) : context.getString(R.string.charging_case_update);
        ContentActivity.startActivity(context, UploadResourceFragment.class.getCanonicalName(), title, bundle);
    }

    /**
     * 上传资源页面UI处理
     */
    private FragmentUploadResourceBinding mBinding;
    /**
     * 上传资源逻辑实现
     */
    private UploadResourceViewModel mViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentUploadResourceBinding.inflate(inflater, container, false);
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
        final ResourceMsg resourceMsg = bundle.getParcelable(SConstant.KEY_RESOURCE_MESSAGE);
        if (null == resourceMsg) {
            finish();
            return;
        }
        mViewModel = new ViewModelProvider(this, new UploadResourceViewModel.Factory(resourceMsg)).get(UploadResourceViewModel.class);
        initUI();
        addObserver();
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mViewModel.uploadResource();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mViewModel.cancelUpload();
    }

    private void initUI() {
        if (requireActivity() instanceof BaseActivity) {
            ((BaseActivity) requireActivity()).setCustomBackPress(() -> {
                if (mViewModel.isWorking()) {
                    showTips(getString(R.string.transferring_file_tips));
                    return true;
                }
                return false;
            });
        }
        mBinding.btnOperation.setOnClickListener(v -> {
            final StateResult<Integer> stateResult = mViewModel.uploadStateMLD.getValue();
            if (null == stateResult) return;
            final int state = stateResult.getState();
            JL_Log.d(TAG, "btnOperation", "state = " + state);
            if (state == UploadResourceViewModel.STATE_WORKING || state == UploadResourceViewModel.STATE_CONVERTING) {
                mViewModel.cancelUpload();
            } else if (state == UploadResourceViewModel.STATE_STOP) {
                if (stateResult.isSuccess()) { //上传成功
                    finish(300, () -> {
                        final ResourceMsg resourceMsg = mViewModel.getResourceMsg();
                        Intent intent = new Intent();
                        intent.putExtra(SConstant.KEY_RESOURCE_MESSAGE, resourceMsg);
                        requireActivity().setResult(Activity.RESULT_OK, intent);
                    });
                    return;
                }
                //重新上传
                finish();
//                requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//                mViewModel.uploadResource();
            }
        });
        loadResource();
    }

    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnectionData -> {
            if (deviceConnectionData.getStatus() != StateCode.CONNECTION_OK) {
                finish();
            }
        });
        mViewModel.uploadStateMLD.observe(getViewLifecycleOwner(), this::updateStateUI);
    }

    private void updateProgress(int progress) {
        mBinding.pbTransferProgress.setProgress(progress);
        mBinding.tvProgress.setText(AppUtil.formatString("%d%%", progress));
    }

    private void loadResource() {
        final String filePath = mViewModel.getResourceMsg().getSrcFilePath();
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(14));
        if (ChargingBinUtil.isGif(filePath)) {
            Glide.with(MainApplication.getApplication()).asGif().load(filePath).apply(options)
                    .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(mBinding.ivScreenSaversEffect);
        } else {
            Glide.with(MainApplication.getApplication()).asBitmap().load(filePath).apply(options)
                    .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(mBinding.ivScreenSaversEffect);
        }
    }

    private void updateStateUI(StateResult<Integer> state) {
        if (null == state) return;
        JL_Log.d(TAG, "updateStateUI", "" + state);
        switch (state.getState()) {
            case UploadResourceViewModel.STATE_IDLE: {
                UIHelper.hide(mBinding.groupUploading);
                UIHelper.hide(mBinding.tvState);
                UIHelper.hide(mBinding.btnOperation);
                updateProgress(0);
                break;
            }
            case UploadResourceViewModel.STATE_CONVERTING:
            case UploadResourceViewModel.STATE_WORKING: {
                UIHelper.hide(mBinding.tvState);
                UIHelper.show(mBinding.groupUploading);
                UIHelper.show(mBinding.btnOperation);
                mBinding.btnOperation.setText(getString(R.string.cancel));
                mBinding.btnOperation.setBackgroundResource(R.drawable.bg_btn_gray_24_shape);
                if (state.getData() != null && state.getData() >= 0) {
                    updateProgress(state.getData());
                }
                break;
            }
            case UploadResourceViewModel.STATE_CANCEL: {
                requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                UIHelper.hide(mBinding.groupUploading);
                UIHelper.hide(mBinding.btnOperation);
                UIHelper.show(mBinding.tvState);
                updateProgress(0);
                mBinding.tvState.setText(getString(R.string.cancel_upload_image));
                break;
            }
            case UploadResourceViewModel.STATE_STOP: {
                requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                UIHelper.hide(mBinding.groupUploading);
                UIHelper.show(mBinding.tvState);
                UIHelper.show(mBinding.btnOperation);
                mBinding.btnOperation.setBackgroundResource(R.drawable.bg_btn_purple_gray_24_selector);
                mBinding.btnOperation.setTextColor(ContextCompat.getColor(requireContext(), R.color.white_ffffff));
                updateProgress(0);
                if (state.isSuccess()) {
                    final ResourceMsg resourceMsg = mViewModel.getResourceMsg();
                    Intent intent = new Intent(SConstant.ACTION_USING_RESOURCE_CHANGE);
                    intent.putExtra(SConstant.KEY_RESOURCE_TYPE, resourceMsg.getResourceType());
                    requireActivity().sendBroadcast(intent);
                    mBinding.tvState.setText(getString(R.string.upload_success));
                    mBinding.btnOperation.setText(getString(R.string.finish));
                    return;
                }
                String text = (state.getCode() == ErrorCode.SUB_ERR_INSUFFICIENT_SPACE) ? getString(R.string.insufficient_space) :
                        getString(R.string.upload_failed);
                mBinding.tvState.setText(text);
                mBinding.btnOperation.setText(getString(R.string.confirm));
                JL_Log.d(TAG, "updateStateUI", "upload failed. code : " + state.getCode() + ", " + state.getMessage());
            }
        }
    }
}