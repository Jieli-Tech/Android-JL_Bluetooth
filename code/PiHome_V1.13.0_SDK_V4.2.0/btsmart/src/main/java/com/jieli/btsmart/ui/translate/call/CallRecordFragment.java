package com.jieli.btsmart.ui.translate.call;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.data.model.translation.TranslationSessionRecord;
import com.jieli.btsmart.databinding.FragmentCallRecordBinding;
import com.jieli.btsmart.tool.translate.player.AudioPlayer;
import com.jieli.btsmart.ui.translate.TranslationRecordAdapter;
import com.jieli.btsmart.ui.translate.basic.BasicRecordFragment;
import com.jieli.btsmart.util.UIHelper;

/**
 * CallRecordFragment
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 通话翻译记录
 * @since 2025/8/26
 */
public class CallRecordFragment extends BasicRecordFragment {

    private FragmentCallRecordBinding mBinding;
    private TranslationRecordAdapter mAdapter;

    private int playType = TranslationSessionRecord.TYPE_SRC_TEXT;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentCallRecordBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public int getPlayType() {
        return playType;
    }

    @Override
    public void updatePlayerState(int state) {
        if (isInvalid()) return;
        if (state == AudioPlayer.STATE_PLAYING) {
            UIHelper.gone(mBinding.viewPlayType);
            UIHelper.show(mBinding.viewPlayer.getRoot());
            int currentPlayType = getPlayType();
            if (currentPlayType == TranslationSessionRecord.TYPE_DEST_TEXT) {
                mAdapter.setSelectedItemColor(0, R.color.blue_448eff);
                mBinding.viewPlayer.sbProgress.setProgressDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_seekbar_progress_blue_layer));
                mBinding.viewPlayer.sbProgress.setThumb(ContextCompat.getDrawable(requireContext(), R.drawable.bg_thumb_blue_selector));
                mBinding.viewPlayer.btnPp.setText(getString(R.string.playing_dest_text));
                mBinding.viewPlayer.btnPp.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff));
                mBinding.viewPlayer.btnPp.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_pause_blue, 0, 0, 0);
                mBinding.viewPlayer.btnPp.setBackgroundResource(R.drawable.bg_btn_c18_blue_gray_selector);
            } else {
                mAdapter.setSelectedItemColor(R.color.orange_F89514, 0);
                mBinding.viewPlayer.sbProgress.setProgressDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_seekbar_progress_orange_layer));
                mBinding.viewPlayer.sbProgress.setThumb(ContextCompat.getDrawable(requireContext(), R.drawable.bg_thumb_orange_selector));
                mBinding.viewPlayer.btnPp.setText(getString(R.string.playing_src_text));
                mBinding.viewPlayer.btnPp.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange_F89514));
                mBinding.viewPlayer.btnPp.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_pause_orange, 0, 0, 0);
                mBinding.viewPlayer.btnPp.setBackgroundResource(R.drawable.bg_btn_c18_orange_gray_selector);
            }
        } else {
            UIHelper.gone(mBinding.viewPlayer.getRoot());
            UIHelper.show(mBinding.viewPlayType);
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
            mAdapter.resetSelectedItem();
        }
    }

    @Override
    public void updateSessionRecord(TranslationSessionRecord sessionRecord) {
        if (isInvalid() || null == sessionRecord) return;
        mBinding.viewToolBar.tvTitle.setText(sessionRecord.getSession().getTitle());
        mAdapter.resetSelectedItem();
        mAdapter.setList(sessionRecord.getRecords());
        int max = sessionRecord.getDuration(getPlayType());
        updateTime(mBinding.viewPlayer.tvEndTime, max);
        mBinding.viewPlayer.sbProgress.setMax(max);
        updateProgress(mBinding.viewPlayer, 0);
        updatePlayerState(AudioPlayer.STATE_IDLE);
    }

    @Override
    public void updateTranslationRecord(TranslationRecord record) {
        if (isInvalid() || null == record) return;
        mAdapter.updateSelectedItem(record);
        int position = mAdapter.getItemPosition(record);
        boolean isItemVisible = UIHelper.isItemVisible(mBinding.rvTranslationRecord, position, true);
        if (!isItemVisible) {
            mBinding.rvTranslationRecord.scrollToPosition(position);
        }
    }

    @Override
    protected void initUI() {
        super.initUI();
        mBinding.viewToolBar.tvLeft.setOnClickListener(v -> finish());
        initViewPlayer(mBinding.viewPlayer);
        mBinding.btnPlaySrc.setOnClickListener(v -> playByType(TranslationSessionRecord.TYPE_SRC_TEXT));
        mBinding.btnPlayDest.setOnClickListener(v -> playByType(TranslationSessionRecord.TYPE_DEST_TEXT));

        mAdapter = new TranslationRecordAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            final TranslationRecord record = mAdapter.getItem(position);
            if (null == record || mAdapter.isSelectedItem(record)) return;
            mViewModel.playByPosition(getPlayType(), position);
        });
        mBinding.rvTranslationRecord.setAdapter(mAdapter);
        mAdapter.setSelectedItemColor(R.color.orange_F89514, 0);
    }


    private void playByType(int type) {
        int position = mViewModel.getCurrentPosition();
        int currentPlayType = getPlayType();
        if (currentPlayType == type) { //与上一次播放类型相同，就恢复播放或者开始播放
            if (mViewModel.isPaused()) {
                mViewModel.resume();
            } else if (!mViewModel.isPlaying()) {
                mViewModel.play(currentPlayType);
            }
        } else { //与上一次播放类型不同，就切换播放类型
            playType = type;
            currentPlayType = getPlayType();
            if (position == 0) {
                mAdapter.resetSelectedItem();
                mViewModel.play(currentPlayType);
            } else {
                mViewModel.playByPosition(currentPlayType, position);
            }
        }
    }
}