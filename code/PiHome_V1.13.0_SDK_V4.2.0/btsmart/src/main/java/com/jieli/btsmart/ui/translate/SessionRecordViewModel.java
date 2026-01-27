package com.jieli.btsmart.ui.translate;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.btsmart.data.model.basic.OpResult;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.data.model.translation.TranslationSessionRecord;
import com.jieli.btsmart.tool.translate.player.AudioPlayer;
import com.jieli.btsmart.tool.translate.player.OnPlayerStateCallback;
import com.jieli.btsmart.tool.translate.player.TranslationSessionPlayer;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

/**
 * SessionRecordViewModel
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 会议记录逻辑实现
 * @since 2025/8/26
 */
public class SessionRecordViewModel extends BtBasicVM {

    private final BluetoothDevice device;
    private final int sessionId;
    private final TranslationSessionPlayer mPlayer;

    public final MutableLiveData<TranslationSessionRecord> sessionRecordMLD = new MutableLiveData<>();
    public final MutableLiveData<Integer> playerStateMLD = new MutableLiveData<>();
    public final MutableLiveData<StateResult<Integer>> stateMLD = new MutableLiveData<>();
    public final MutableLiveData<OpResult<TranslationRecord>> recordStateMLD = new MutableLiveData<>();


    public SessionRecordViewModel(BluetoothDevice device, int sessionId) {
        this.device = device;
        this.sessionId = sessionId;
        mPlayer = TranslationSessionPlayer.getInstance();
        mPlayer.setOnPlayerStateCallback(new OnPlayerStateCallback() {
            @Override
            public void onSessionRecordChange(@NonNull TranslationSessionRecord record) {
                sessionRecordMLD.postValue(record);
            }

            @Override
            public void onStart() {
                stateMLD.postValue(new StateResult<Integer>()
                        .setState(StateResult.STATE_WORKING).setProgress(-1)
                        .setCode(OpResult.RES_SUCCESS));
            }

            @Override
            public void onStateChange(int state) {
                if (state != AudioPlayer.STATE_IDLE) {
                    postPlayerState(state);
                }
            }

            @Override
            public void onProgress(int progress) {
                stateMLD.setValue(new StateResult<Integer>()
                        .setState(StateResult.STATE_WORKING).setProgress(progress)
                        .setCode(OpResult.RES_SUCCESS));
            }

            @Override
            public void onTranslationRecord(int index, @NonNull TranslationRecord record) {
                recordStateMLD.postValue(new OpResult<TranslationRecord>(index)
                        .setCode(OpResult.RES_SUCCESS)
                        .setData(record));
            }

            @Override
            public void onError(int index, int type, int code, String message) {
                recordStateMLD.postValue(new OpResult<TranslationRecord>(index)
                        .setCode(code)
                        .setMessage(message));
            }

            @Override
            public void onStop(int result) {
                postPlayerState(AudioPlayer.STATE_IDLE);
                stateMLD.setValue(new StateResult<Integer>()
                        .setState(StateResult.STATE_FINISH)
                        .setCode(result)
                        .setMessage(ErrorCode.code2Msg(result)));
            }
        });
        mPlayer.updateTranslationSessionRecord(sessionId);
    }

    @Override
    protected void release() {
        TranslationSessionPlayer.getInstance().release();
        super.release();
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    public boolean isPaused() {
        return mPlayer.isPaused();
    }

    public int getCurrentPosition() {
        return mPlayer.getCurrentPosition();
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public TranslationSessionRecord getSessionRecord() {
        return sessionRecordMLD.getValue();
    }

    public TranslationRecord getRecordByPosition(int position) {
        final TranslationSessionRecord sessionRecord = getSessionRecord();
        if (null == sessionRecord) return null;
        return sessionRecord.getItem(position);
    }

    public boolean play(int type) {
        return mPlayer.autoPlay(type);
    }

    public boolean playByPosition(int type, int position) {
        return mPlayer.play(type, position);
    }

    public boolean playByProgress(int type, int progress) {
        return mPlayer.playByProgress(type, progress);
    }

    public boolean pause() {
        return mPlayer.pause();
    }

    public boolean resume() {
        return mPlayer.resume();
    }

    private void postPlayerState(int state){
        Integer cacheState = playerStateMLD.getValue();
        if (cacheState == null || cacheState != state) {
            playerStateMLD.postValue(state);
        }
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final BluetoothDevice device;
        private final int sessionId;

        public Factory(BluetoothDevice device, int sessionId) {
            this.device = device;
            this.sessionId = sessionId;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new SessionRecordViewModel(device, sessionId);
        }
    }

}
