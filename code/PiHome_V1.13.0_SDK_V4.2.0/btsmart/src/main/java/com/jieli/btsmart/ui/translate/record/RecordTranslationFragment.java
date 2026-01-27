package com.jieli.btsmart.ui.translate.record;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.jieli.bluetooth.bean.translation.AudioData;
import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.OpResult;
import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.databinding.FragmentRecordTranslationBinding;
import com.jieli.btsmart.tool.translate.TranslateState;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.base.BaseActivity;
import com.jieli.btsmart.ui.translate.basic.BasicTranslationFragment;
import com.jieli.btsmart.ui.translate.language.SelectLanguageFragment;
import com.jieli.btsmart.ui.widget.visualizer.record.data.IdleState;
import com.jieli.btsmart.ui.widget.visualizer.record.data.State;
import com.jieli.btsmart.ui.widget.visualizer.record.data.WorkingState;
import com.jieli.btsmart.util.TranslateUtil;
import com.jieli.jl_dialog.Jl_Dialog;

/**
 * RecordTranslationFragment
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 录音翻译界面
 * @since 2025/6/9
 */
public class RecordTranslationFragment extends BasicTranslationFragment {

    private FragmentRecordTranslationBinding mBinding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentRecordTranslationBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public int getTranslationMode() {
        return TranslationMode.MODE_RECORDING_TRANSLATION;
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
        mBinding.viewToolBar.ivTranslationDirection.setImageResource(R.drawable.ic_left_arrow_gray);
        mBinding.viewToolBar.btnBack.setOnClickListener(v -> exitFragment());
        mBinding.viewToolBar.btnVolumeCtrl.setOnClickListener(v -> {
            if (!mViewModel.isTranslating()) return;
            mViewModel.setMuteState(!mViewModel.isMute());
        });

        mBinding.btnSwitchLanguage.setOnClickListener(v -> {

        });
        mBinding.btnPp.setOnClickListener(v -> {
            if (!mViewModel.isTranslating()) return;
            if (mViewModel.isPaused()) {
                mViewModel.resumeTranslate();
            } else {
                mViewModel.pauseTranslate();
            }
        });
        mBinding.btnStopTranslation.setOnClickListener(v -> {
            JL_Log.i(TAG, "exitFragment", "User actively exits translation mode.");
            mViewModel.exitMode();
        });

        mBinding.viewToolBar.tvReceiverLanguage.setText(SelectLanguageFragment.getLanguage(requireContext(), srcLanguage));
        mBinding.viewToolBar.tvPlayerLanguage.setText(SelectLanguageFragment.getLanguage(requireContext(), destLanguage));
    }

    @Override
    protected void addObserver() {
        super.addObserver();
        mViewModel.translationRecordMLD.observeForever(translationRecordObserver);
        mViewModel.translateStateMLD.observeForever(translateStateObserver);
        mViewModel.workTimeMLD.observe(getViewLifecycleOwner(), time -> {
            if (isInvalid()) return;
            mBinding.viewToolBar.tvTitle.setText(TranslateUtil.formatDuration(time));
        });
        mViewModel.muteStateMLD.observe(getViewLifecycleOwner(), isMute -> {
            if (isInvalid()) return;
            mBinding.viewToolBar.btnVolumeCtrl.setImageResource(isMute ? R.drawable.ic_voice_black : R.drawable.ic_mute_black);
        });
        mViewModel.saveSessionRecordMLD.observe(getViewLifecycleOwner(), result -> {
            if (isInvalid() || null == result) return;
            if (result.isSuccess()) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(SConstant.KEY_BLUETOOTH_DEVICE, mViewModel.getDevice());
                bundle.putInt(SConstant.KEY_SESSION_ID, result.getData());
                ContentActivity.startActivity(requireContext(), SessionRecordFragment.class.getCanonicalName(), bundle);
            }
        });
    }

    @Override
    protected void removeObserver() {
        if (null == mViewModel) return;
        mViewModel.translateStateMLD.removeObserver(translateStateObserver);
        mViewModel.translationRecordMLD.removeObserver(translationRecordObserver);
        super.removeObserver();
    }

    private final Observer<TranslateState> translateStateObserver = state -> {
        if (null == state || isInvalid()) return;
        switch (state) {
            case STATE_IDLE:
                mBinding.btnPp.setImageResource(R.drawable.ic_play_purple);
                mBinding.btnPp.setClickable(false);
                break;
            case STATE_WORKING:
                mBinding.btnPp.setClickable(true);
                mBinding.btnPp.setImageResource(R.drawable.ic_pause_purple);
                mBinding.viewPlayWave.startRecord(recordState -> {
                    switch (recordState.getState()) {
                        case State.STATE_START: {
                            break;
                        }
                        case State.STATE_WORKING: {
                            WorkingState workingState = (WorkingState) recordState;
                            mViewModel.writeAudioData(new AudioData(AudioData.SOURCE_PHONE_MIC, Constants.AUDIO_TYPE_PCM, workingState.getData()));
                            break;
                        }
                        case State.STATE_IDLE: {
                            //录音结束
                            IdleState idleState = (IdleState) recordState;
                            if (idleState.getCode() != ErrorCode.ERR_NONE) {
                                showTips("%s\n%s : %s, %s",
                                        getString(R.string.recording_failed),
                                        getString(R.string.error_code), CommonUtil.formatInt(idleState.getCode()), idleState.getMessage());
                            }
                            break;
                        }
                    }
                });
                break;
            case STATE_PAUSE:
                mBinding.btnPp.setClickable(true);
                mBinding.btnPp.setImageResource(R.drawable.ic_play_purple);
                mBinding.viewPlayWave.release();
                break;
        }
    };

    private final Observer<OpResult<TranslationRecord>> translationRecordObserver = result -> {
        if (null == result || isInvalid()) return;
        if (result.isSuccess()) {
            updateTranslateUI(result.getData());
            return;
        }
        showTips(CommonUtil.formatString("%s\n%s : %s, %s",
                getString(R.string.translation_failed),
                getString(R.string.error_code),
                CommonUtil.formatInt(result.getCode()), result.getMessage()));
    };

    private void updateTranslateUI(TranslationRecord record) {
        if (isInvalid() || null == record) return;
        mBinding.tvSrcContent.append(record.getSrcText());
        mBinding.tvDestContent.append(record.getDestText());
    }

    private void exitFragment() {
        if (mViewModel.isTranslating()) {
            mViewModel.pauseTranslate();
            new Jl_Dialog.Builder()
                    .content(getString(R.string.exit_simultaneous_interpreting))
                    .contentColor(ContextCompat.getColor(requireContext(), R.color.black_242424))
                    .left(getString(R.string.cancel))
                    .leftColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                    .leftClickListener((v, dialogFragment) -> {
                        dialogFragment.dismiss();
                        mViewModel.resumeTranslate();
                    })
                    .right(getString(R.string.confirm))
                    .rightColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                    .rightClickListener((v, dialogFragment) -> {
                        dialogFragment.dismiss();
                        mViewModel.exitMode(true);
                    })
                    .build().show(getChildFragmentManager(), Jl_Dialog.class.getSimpleName());
        } else {
            finish();
        }
    }
}