package com.jieli.btsmart.ui.translate.record;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.data.model.translation.TranslationSessionRecord;
import com.jieli.btsmart.databinding.FragmentSessionRecordBinding;
import com.jieli.btsmart.tool.translate.player.AudioPlayer;
import com.jieli.btsmart.ui.translate.basic.BasicRecordFragment;

/**
 * SessionRecordFragment
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 同声传译记录
 * @since 2025/8/26
 */
public class SessionRecordFragment extends BasicRecordFragment {

    private FragmentSessionRecordBinding mBinding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentSessionRecordBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public int getPlayType() {
        return TranslationSessionRecord.TYPE_SRC_TEXT;
    }

    @Override
    public void updatePlayerState(int state) {
        if (isInvalid()) return;
        if (state == AudioPlayer.STATE_PLAYING) {
            mBinding.viewPlayer.btnPp.setText(getString(R.string.pause));
            mBinding.viewPlayer.btnPp.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_pause_orange, 0, 0, 0);
        } else {
            mBinding.viewPlayer.btnPp.setText(getString(R.string.playback));
            mBinding.viewPlayer.btnPp.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_play_orange, 0, 0, 0);
        }
    }

    @Override
    public void updateState(StateResult<Integer> stateResult) {
        if (isInvalid() || null == stateResult) return;
        if (stateResult.getState() == StateResult.STATE_WORKING) {
            if (stateResult.getProgress() >= 0) {
                updateProgress(mBinding.viewPlayer, stateResult.getProgress());
            }
        } else if (stateResult.getState() == StateResult.STATE_FINISH) {
            if(stateResult.isSuccess()){
                updateProgress(mBinding.viewPlayer, mBinding.viewPlayer.sbProgress.getMax());
            }
            mBinding.tvSrcContent.setTextColor(ContextCompat.getColor(requireContext(), R.color.black_E6000000));
            mBinding.tvSrcContent.setText(mBinding.tvSrcContent.getText().toString());
            mBinding.tvDestContent.setTextColor(ContextCompat.getColor(requireContext(), R.color.black_80000000));
            mBinding.tvDestContent.setText(mBinding.tvDestContent.getText().toString());
        }
    }

    @Override
    public void updateSessionRecord(TranslationSessionRecord sessionRecord) {
        if (isInvalid() || null == sessionRecord) return;
        mBinding.viewToolBar.tvTitle.setText(sessionRecord.getSession().getTitle());
        mBinding.tvSrcContent.setText(sessionRecord.getText(TranslationSessionRecord.TYPE_SRC_TEXT));
        mBinding.tvDestContent.setText(sessionRecord.getText(TranslationSessionRecord.TYPE_DEST_TEXT));
        int max = sessionRecord.getDuration(getPlayType());
        JL_Log.d(TAG, "updateSessionRecord", "max : " + max);
        updateTime(mBinding.viewPlayer.tvEndTime, max);
        mBinding.viewPlayer.sbProgress.setMax(max);
        updateProgress(mBinding.viewPlayer, 0);
        updatePlayerState(AudioPlayer.STATE_IDLE);
        mViewModel.play(getPlayType());
    }

    @Override
    public void updateTranslationRecord(TranslationRecord record) {
        if (isInvalid() || null == record) return;
        String src = record.getSrcText();
        String allSrc = mBinding.tvSrcContent.getText().toString();
        int start = allSrc.indexOf(src);
        int end;
        int color = ContextCompat.getColor(requireContext(), R.color.orange_F89514);
        if (start > -1) {
            end = start + src.length();
            SpannableString span = new SpannableString(allSrc);
            span.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mBinding.tvSrcContent.setText(span);
        }

        src = record.getDestText();
        allSrc = mBinding.tvDestContent.getText().toString();
        start = allSrc.indexOf(src);
        if (start > -1) {
            end = start + src.length();
            SpannableString span = new SpannableString(allSrc);
            span.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mBinding.tvDestContent.setText(span);
        }
    }

    @Override
    protected void initUI() {
        super.initUI();
        mBinding.viewToolBar.tvLeft.setOnClickListener(v -> finish());
        initViewPlayer(mBinding.viewPlayer);
    }
}