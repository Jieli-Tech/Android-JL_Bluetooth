package com.jieli.btsmart.ui.translate.basic;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.device.DeviceConnection;
import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.data.model.translation.TranslationSessionRecord;
import com.jieli.btsmart.databinding.ViewRecordPlayerBinding;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.translate.SessionRecordViewModel;
import com.jieli.btsmart.util.TranslateUtil;

/**
 * BasicRecordFragment
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译记录基类
 * @since 2025/8/27
 */
public abstract class BasicRecordFragment extends DeviceControlFragment {

    protected SessionRecordViewModel mViewModel;

    public abstract int getPlayType();

    public abstract void updateSessionRecord(TranslationSessionRecord sessionRecord);

    public abstract void updatePlayerState(int state);

    public abstract void updateState(StateResult<Integer> stateResult);

    public abstract void updateTranslationRecord(TranslationRecord record);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Bundle bundle = getArguments();
        final BluetoothDevice device = null == bundle ? null : bundle.getParcelable(SConstant.KEY_BLUETOOTH_DEVICE);
        final int sessionId = null == bundle ? -1 : bundle.getInt(SConstant.KEY_SESSION_ID, -1);
        if (null == device || sessionId == -1) {
            finish();
            return;
        }
        mViewModel = new ViewModelProvider(this, new SessionRecordViewModel.Factory(device, sessionId))
                .get(SessionRecordViewModel.class);
        initUI();
        addObserver();
    }

    @Override
    public void onDestroyView() {
        clearObserver();
        super.onDestroyView();
    }

    protected void initUI() {
        hideTopBar();
    }

    protected void addObserver() {
        mViewModel.deviceConnectionMLD.observeForever(connectionObserver);
        mViewModel.sessionRecordMLD.observe(getViewLifecycleOwner(), this::updateSessionRecord);
        mViewModel.playerStateMLD.observe(getViewLifecycleOwner(), this::updatePlayerState);
        mViewModel.stateMLD.observe(getViewLifecycleOwner(), this::updateState);
        mViewModel.recordStateMLD.observe(getViewLifecycleOwner(), result -> {
            if (isInvalid()) return;
            if (result.isSuccess()) { //播放成功
                updateTranslationRecord(result.getData());
                return;
            }
            //播放音频失败
            TranslationRecord record = mViewModel.getRecordByPosition(result.getOp());
            if (null == record) return;
            String content = getPlayType() == TranslationSessionRecord.TYPE_DEST_TEXT ? record.getDestText()
                    : record.getSrcText();
            showTips(CommonUtil.formatString("%s\n%s : %s, %s",
                    getString(R.string.play_failed, content), getString(R.string.error_code),
                    CommonUtil.formatInt(result.getCode()), result.getMessage()));
        });
    }

    protected void clearObserver() {
        mViewModel.deviceConnectionMLD.removeObserver(connectionObserver);
    }

    protected void initViewPlayer(@NonNull ViewRecordPlayerBinding binding){
        binding.sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            boolean isFromUser;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                isFromUser = fromUser;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!isFromUser) return;
                int progress = seekBar.getProgress();
                if (!mViewModel.playByProgress(getPlayType(), progress)) {
                    showTips(getString(R.string.playback_failed));
                }
            }
        });
        binding.btnPp.setOnClickListener(v -> {
            if (mViewModel.isPlaying()) {
                mViewModel.pause();
            } else if (mViewModel.isPaused()) {
                mViewModel.resume();
            } else {
                mViewModel.play(getPlayType());
            }
        });
    }

    protected void updateProgress(ViewRecordPlayerBinding binding, int progress) {
        if (isInvalid()) return;
        binding.sbProgress.setProgress(progress);
        updateTime(binding.tvStartTime, progress);
    }

    protected void updateTime(@NonNull TextView textView, int time) {
        textView.setText(TranslateUtil.formatDurationToHm(time));
    }

    private final Observer<DeviceConnection> connectionObserver = connection -> {
        if (null == connection) return;
        if (BluetoothUtil.deviceEquals(connection.getDevice(), mViewModel.getDevice())
                && connection.getStatus() != StateCode.CONNECTION_OK) {
            finish();
        }
    };
}
