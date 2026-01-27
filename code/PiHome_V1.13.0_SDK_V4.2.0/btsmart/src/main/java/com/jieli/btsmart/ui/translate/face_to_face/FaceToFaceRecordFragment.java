package com.jieli.btsmart.ui.translate.face_to_face;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.data.model.translation.TranslationSessionRecord;
import com.jieli.btsmart.databinding.FragmentFaceToFaceRecordBinding;
import com.jieli.btsmart.tool.translate.player.AudioPlayer;
import com.jieli.btsmart.ui.translate.TranslationRecordAdapter;
import com.jieli.btsmart.ui.translate.basic.BasicRecordFragment;
import com.jieli.btsmart.util.UIHelper;

/**
 * FaceToFaceRecordFragment
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 面对面翻译记录
 * @since 2025/8/26
 */
public class FaceToFaceRecordFragment extends BasicRecordFragment {

    private FragmentFaceToFaceRecordBinding mBinding;
    private TranslationRecordAdapter mAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentFaceToFaceRecordBinding.inflate(inflater, container, false);
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
        mViewModel.play(getPlayType());
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

        mAdapter = new TranslationRecordAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            final TranslationRecord record = mAdapter.getItem(position);
            if (null == record || mAdapter.isSelectedItem(record)) return;
            mViewModel.playByPosition(getPlayType(), position);
        });
        mBinding.rvTranslationRecord.setAdapter(mAdapter);
        mAdapter.setSelectedItemColor(R.color.orange_F89514, R.color.orange_F89514);
    }
}