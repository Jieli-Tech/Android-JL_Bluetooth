package com.jieli.btsmart.ui.translate.face_to_face;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.OpResult;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.databinding.FragmentFaceToFaceTranslationBinding;
import com.jieli.btsmart.tool.ai.doubao.translate.model.language.Language;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.base.BaseActivity;
import com.jieli.btsmart.ui.translate.TranslationRecordAdapter;
import com.jieli.btsmart.ui.translate.basic.BasicTranslationFragment;
import com.jieli.btsmart.ui.translate.language.SelectLanguageFragment;
import com.jieli.btsmart.ui.widget.visualizer.LongPressRecordView;
import com.jieli.btsmart.util.TranslateUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.jl_dialog.Jl_Dialog;

/**
 * FaceToFaceTranslationFragment
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 面对面翻译界面
 * @since 2025/6/9
 */
public class FaceToFaceTranslationFragment extends BasicTranslationFragment {

    private FragmentFaceToFaceTranslationBinding mBinding;
    private LongPressRecordView mRecordView;
    private TranslationRecordAdapter mAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentFaceToFaceTranslationBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public int getTranslationMode() {
        return TranslationMode.MODE_FACE_TO_FACE_TRANSLATION;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecordView.setListener(null);
    }

    @Override
    protected void initUI() {
        super.initUI();
        if (requireActivity() instanceof BaseActivity) {
            ((BaseActivity) requireActivity()).setCustomBackPress(() -> {
                exitFragment();
                return true;
            });
        }
        mRecordView = new LongPressRecordView(requireContext(), mViewModel.getEdrAddress(), mBinding.viewRecord);
        mRecordView.setListener(new LongPressRecordView.OnRecordEventListener() {
            @Override
            public void onStateChange(int state) {
                if (state == LongPressRecordView.STATE_IDLE) {
                    UIHelper.show(mBinding.btnSwitchLanguage);
                    UIHelper.show(mBinding.btnDeviceRecordCtrl);
                } else {
                    UIHelper.gone(mBinding.btnSwitchLanguage);
                    UIHelper.gone(mBinding.btnDeviceRecordCtrl);
                }
            }

            @Override
            public void onRecordFile(String filePath) {
                mViewModel.writePcmFile(filePath);
            }
        });
        mBinding.viewToolBar.ivTranslationDirection.setImageResource(R.drawable.ic_two_way_gray);
        mBinding.viewToolBar.btnBack.setOnClickListener(v -> exitFragment());
        mBinding.viewToolBar.tvReceiverLanguage.setText(SelectLanguageFragment.getLanguage(requireContext(), srcLanguage));
        mBinding.viewToolBar.tvPlayerLanguage.setText(SelectLanguageFragment.getLanguage(requireContext(), destLanguage));
        UIHelper.gone(mBinding.viewToolBar.btnVolumeCtrl);
        mBinding.btnSwitchLanguage.setOnClickListener(v -> {

        });
        mBinding.btnDeviceRecordCtrl.setOnClickListener(v -> {
            if (!mViewModel.isTranslating()) return;
            if (mViewModel.isDeviceRecoding()) {
                mViewModel.stopDeviceRecording();
            } else {
                mViewModel.startDeviceRecording();
            }
        });

        mAdapter = new TranslationRecordAdapter();
        mBinding.rvTranslationRecord.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.rvTranslationRecord.setAdapter(mAdapter);
        View emptyView = LayoutInflater.from(requireContext()).inflate(R.layout.view_face_to_face_empty, null, false);
        ((TextView) emptyView.findViewById(R.id.tv_src_content)).setText(Language.getFaceToFaceTips(srcLanguage));
        ((TextView) emptyView.findViewById(R.id.tv_dest_content)).setText(Language.getFaceToFaceTips(destLanguage));
        mAdapter.setEmptyView(emptyView);
    }

    @Override
    protected void addObserver() {
        super.addObserver();
        mViewModel.translationRecordMLD.observeForever(translationRecordObserver);
        mViewModel.workTimeMLD.observe(getViewLifecycleOwner(), time -> {
            if (isInvalid() || null == time) return;
            mBinding.viewToolBar.tvTitle.setText(TranslateUtil.formatDuration(time));
        });
        mViewModel.deviceRecordStateMLD.observe(getViewLifecycleOwner(), state -> {
            if (isInvalid() || null == state) return;
            if (state.getState() == StateResult.STATE_WORKING) {
                updateDeviceRecordState(true);
            } else if (state.getState() == StateResult.STATE_FINISH) {
                updateDeviceRecordState(false);
                if (state.getCode() != ErrorCode.ERR_NONE) {
                    showTips("%s!\n%s : %s, %s",
                            getString(R.string.recording_failed),
                            getString(R.string.error_code), CommonUtil.formatInt(state.getCode()), state.getMessage());
                }
            }
        });
        mViewModel.saveSessionRecordMLD.observe(getViewLifecycleOwner(), result -> {
            if (isInvalid() || null == result) return;
            if (result.isSuccess()) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(SConstant.KEY_BLUETOOTH_DEVICE, mViewModel.getDevice());
                bundle.putInt(SConstant.KEY_SESSION_ID, result.getData());
                ContentActivity.startActivity(requireContext(), FaceToFaceRecordFragment.class.getCanonicalName(), bundle);
            }
        });
    }

    @Override
    protected void removeObserver() {
        if (null == mViewModel) return;
        mViewModel.translationRecordMLD.removeObserver(translationRecordObserver);
        super.removeObserver();
    }

    private void exitFragment() {
        if (mViewModel.isTranslating()) {
            new Jl_Dialog.Builder()
                    .content(getString(R.string.exit_face_to_face_translation))
                    .contentColor(ContextCompat.getColor(requireContext(), R.color.black_242424))
                    .left(getString(R.string.cancel))
                    .leftColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                    .leftClickListener((v, dialogFragment) -> dialogFragment.dismiss())
                    .right(getString(R.string.confirm))
                    .rightColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                    .rightClickListener((v, dialogFragment) -> {
                        dialogFragment.dismiss();
                        JL_Log.i(TAG, "exitFragment", "User actively exits translation mode.");
                        mViewModel.exitMode();
                    })
                    .build().show(getChildFragmentManager(), Jl_Dialog.class.getSimpleName());
        } else {
            finish();
        }
    }

    private void updateDeviceRecordState(boolean isRecording) {
        if(isRecording){
            Glide.with(MainApplication.getApplication())
                    .asGif()
                    .placeholder(R.drawable.ic_stop_record_black)
                    .load(R.drawable.anim_device_record)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(mBinding.ivDeviceRecord);

        }else{
            mBinding.ivDeviceRecord.setImageResource(R.drawable.ic_headset_record_black);
        }
        mBinding.tvDeviceRecord.setText(isRecording ? getString(R.string.stop_recording) : getString(R.string.control_headset));
    }

    private final Observer<OpResult<TranslationRecord>> translationRecordObserver = result -> {
        if (null == result || isInvalid()) return;
        if (!result.isSuccess()) {
            showTips(CommonUtil.formatString("%s\n%s : %s, %s",
                    getString(R.string.translation_failed),
                    getString(R.string.error_code), CommonUtil.formatInt(result.getCode()), result.getMessage()));
            return;
        }
        TranslationRecord record = result.getData();
        if (null == record) return;
        int position = mAdapter.getItemPosition(record);
        if (position == -1) {
            mAdapter.addData(record);
            position = mAdapter.getItemPosition(record);
        } else {
            TranslationRecord cache = mAdapter.getItem(position);
            cache.setSrcText(record.getSrcText());
            cache.setSrcLanguage(record.getSrcLanguage());
            cache.setSrcFilePath(record.getSrcFilePath());
            cache.setSrcFileDuration(record.getSrcFileDuration());
            cache.setDestText(record.getDestText());
            cache.setDestLanguage(record.getDestLanguage());
            cache.setDestFilePath(record.getDestFilePath());
            cache.setDestFileDuration(record.getDestFileDuration());
            mAdapter.notifyItemChanged(position);
        }
        boolean isItemVisible = UIHelper.isItemVisible(mBinding.rvTranslationRecord, position, true);
        JL_Log.d(TAG, "translationRecord", "data size : " + mAdapter.getData().size() + ", position : " + position
                + ", isItemVisible : " + isItemVisible);
        if (!isItemVisible) {
            mBinding.rvTranslationRecord.scrollToPosition(position);
        }
    };
}