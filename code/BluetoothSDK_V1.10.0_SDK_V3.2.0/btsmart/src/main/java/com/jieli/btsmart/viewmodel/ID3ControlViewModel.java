package com.jieli.btsmart.viewmodel;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.jieli.audio.media_player.AudioFocusManager;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.music.ID3MusicInfo;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.tool.playcontroller.PlayControlCallback;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

public class ID3ControlViewModel extends BtBasicVM {
    private PlayControlImpl mPlayControl;
    private AudioFocusManager mAudioFocusManager;

    private boolean isOpeningID3Stream;
    private int showPlayerFlag = PLAYER_FLAG_NONE;
    private boolean tryToPlayState;
    private int testCount;
    private boolean playCmdIsResponse = true;
    public final MutableLiveData<BaseError> mID3CmdError = new MutableLiveData<>();
    public final MutableLiveData<ID3MusicInfo> mID3MusicInfo = new MutableLiveData<>();
    public final MutableLiveData<Integer> mChangePlayerFlag = new MutableLiveData<>();

    public final static int PLAYER_FLAG_NONE = 0;
    public final static int PLAYER_FLAG_LOCAL = 1;
    public final static int PLAYER_FLAG_OTHER = 2;

    public ID3ControlViewModel() {
        super();
        mRCSPController.addBTRcspEventCallback(mEventCallback);
        getPlayControl().registerPlayControlListener(mControlCallback);
        getAudioFocusManager().registerOnAudioFocusChangeCallback(mOnAudioFocusChangeCallback);
    }

    public boolean isOpeningID3InfoStream() {
        return isOpeningID3Stream;
    }

    public void openID3InfoStream() {
        mRCSPController.openID3MusicNotification(getConnectedDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                setOpeningID3Stream(true);
                testCount = 1;
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                mID3CmdError.setValue(error);
                testCount = 1;
            }
        });
    }

    public void closeID3InfoStream() {
        JL_Log.i("zzc_id3", "closeID3InfoStream >>>>>>> ");
        mRCSPController.closeID3MusicNotification(getConnectedDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                isOpeningID3Stream = false;
                testCount = 0;
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                mID3CmdError.setValue(error);
            }
        });
    }

    public int showPlayerFlag() {
        return showPlayerFlag;
    }

    public ID3MusicInfo getMusicInfo() {
        if (getDeviceInfo() == null) return null;
        return getDeviceInfo().getiD3MusicInfo();
    }

    public void getID3MusicInfo() {
        JL_Log.e("zzc_id3", "getID3MusicInfo >>>>>>>  ");
        mRCSPController.getID3MusicInfo(getConnectedDevice(), null);
    }

    public void playID3Prev() {
        mRCSPController.iD3MusicPlayPrev(getConnectedDevice(), null);
    }

    public void playID3Next() {
        mRCSPController.iD3MusicPlayNext(getConnectedDevice(), null);
    }

    public void playOrPauseID3(boolean srcPlayState) {
        Log.e("zzc_id3", " isID3Play(): " + isID3Play() + " tryToPlayState: " + tryToPlayState + " srcPlayState: " + srcPlayState);
        if (!playCmdIsResponse) return;
        playCmdIsResponse = false;
        mRCSPController.iD3MusicPlayOrPause(getConnectedDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                tryToPlayState = !srcPlayState;
                playCmdIsResponse = true;
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                tryToPlayState = srcPlayState;
                playCmdIsResponse = true;
            }
        });
    }

    @Override
    protected void release() {
        super.release();
        mRCSPController.removeBTRcspEventCallback(mEventCallback);
        getPlayControl().unregisterPlayControlListener(mControlCallback);
        getAudioFocusManager().unregisterOnAudioFocusChangeCallback(mOnAudioFocusChangeCallback);
    }

    protected PlayControlImpl getPlayControl() {
        if (mPlayControl == null) {
            mPlayControl = PlayControlImpl.getInstance();
        }
        return mPlayControl;
    }

    protected AudioFocusManager getAudioFocusManager() {
        if (mAudioFocusManager == null) {
            mAudioFocusManager = AudioFocusManager.getInstance();
            mAudioFocusManager.init(AppUtil.getContext());
        }
        return mAudioFocusManager;
    }

    private boolean isID3Play() {
        /*boolean isOtherPlayerPlay = false;
        if (getAudioFocusManager().isHasAudioFocus()) { //本地播放器有焦点
            if (getPlayControl().isPlay()) {
                return false;
            } else if (getAudioFocusManager().isMusicPlay()) { //本地播放器暂停，但是还有其他音乐在播放
                isOtherPlayerPlay = true;
            }
        } else {//本地播放器没有焦点
            isOtherPlayerPlay = !getPlayControl().isPlay() && getAudioFocusManager().isMusicPlay();
        }
        return isOtherPlayerPlay;*/
        return !getPlayControl().isPlay() && !getAudioFocusManager().isHasAudioFocus() && getAudioFocusManager().isMusicPlay();
    }

    private void setPlayerFlag(int flag) {
        showPlayerFlag = flag;
    }

    private void onChangePlayerFlag(int flag) {
        setPlayerFlag(flag);
        JL_Log.e("zzc_id3", "onChangePlayerFlag : " + flag);
        mChangePlayerFlag.setValue(flag);
//        mView.onChangePlayerFlag(flag);
        if (flag == PLAYER_FLAG_OTHER && !isOpeningID3Stream) {
            openID3InfoStream();
        }
    }

    private boolean isLocalMusicGetFocus() {
        return showPlayerFlag == PLAYER_FLAG_LOCAL;
    }

    private void setOpeningID3Stream(boolean enable) {
        isOpeningID3Stream = enable;
    }

    private void getID3AllInfo() {
        if (testCount == 0) {
            getID3MusicInfo();
            testCount++;
        } else {
            testCount++;
            if (testCount == 11) {
                closeID3InfoStream();
            }
        }
    }

    private final BTRcspEventCallback mEventCallback = new BTRcspEventCallback() {
        private boolean isNeedCallback;

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (status == StateCode.CONNECTION_DISCONNECT || status == StateCode.CONNECTION_FAILED) {
                setOpeningID3Stream(false);
                testCount = 0;
                setPlayerFlag(PLAYER_FLAG_NONE);
            } else if (status == StateCode.CONNECTION_OK) {
                setPlayerFlag(PLAYER_FLAG_NONE);
                if (!isOpeningID3Stream) {
                    openID3InfoStream();
                }
            }
        }

        @Override
        public void onID3MusicInfo(BluetoothDevice device, ID3MusicInfo id3MusicInfo) {
            JL_Log.d("zzc_id3", "onID3MusicInfo : " + id3MusicInfo + ", showPlayerFlag : " + showPlayerFlag + ", isID3Play : " + isID3Play()
                    + ",\n testCount : " + testCount + ", isOpeningID3Stream : " + isOpeningID3Stream);
            if (showPlayerFlag != PLAYER_FLAG_OTHER) {
                if (isID3Play()) {
                    setPlayerFlag(PLAYER_FLAG_OTHER);
                    isNeedCallback = true;
                } else if (showPlayerFlag == PLAYER_FLAG_NONE) {
                    if (!isOpeningID3Stream) setOpeningID3Stream(true);
                    onChangePlayerFlag(PLAYER_FLAG_LOCAL);
                } else if (showPlayerFlag == PLAYER_FLAG_LOCAL && mAudioFocusManager.isHasAudioFocus() && mPlayControl.isPlay()) {
                    if (isOpeningID3Stream) {
                        setOpeningID3Stream(false);
                        closeID3InfoStream();
                    }
                }
                return;
            }
            if (TextUtils.isEmpty(id3MusicInfo.getTitle())) {//不合法ID3信息
                getID3AllInfo();
                return;
            }
            if (id3MusicInfo.getTotalTime() == 0) {//不合法ID3信息
                getID3AllInfo();
                return;
            }
            if (!isOpeningID3Stream && isID3Play()) {
                setOpeningID3Stream(true);
                openID3InfoStream();
            } else if (mAudioFocusManager.isHasAudioFocus() && mPlayControl.isPlay()) {
                setOpeningID3Stream(false);
                closeID3InfoStream();
            }
            if (id3MusicInfo.getCurrentTime() == 0 || id3MusicInfo.getCurrentTime() <= id3MusicInfo.getTotalTime()) {
                if (testCount != 0) testCount = 0;
                if (id3MusicInfo.isPlayStatus() != isID3Play()) {
                    id3MusicInfo.setPlayStatus(isID3Play());
                }
                Log.e("zzc_id3", " isID3Play(): " + isID3Play() + " tryToPlayState: " + tryToPlayState + " id3MusicInfo: " + id3MusicInfo);
                if (isID3Play() == tryToPlayState) {
                    playCmdIsResponse = true;
                }
                mID3MusicInfo.setValue(id3MusicInfo);
            } else {
                getID3AllInfo();
            }
            if (isNeedCallback) {
                isNeedCallback = false;
                onChangePlayerFlag(showPlayerFlag);
            }
        }

    };
    private final PlayControlCallback mControlCallback = new PlayControlCallback() {
        @Override
        public void onTimeChange(int current, int total) {
            super.onTimeChange(current, total);
        }

        @Override
        public void onPlayStateChange(boolean isPlay) {
            super.onPlayStateChange(isPlay);
            JL_Log.e("zzc_id3", "onPlayStateChange : " + isPlay);
            if (!isLocalMusicGetFocus() && isPlay) {
                onChangePlayerFlag(PLAYER_FLAG_LOCAL);
            }
            if (isPlay && isOpeningID3Stream) {
                closeID3InfoStream();
            }
        }
    };
    private final AudioFocusManager.OnAudioFocusChangeCallback mOnAudioFocusChangeCallback = new AudioFocusManager.OnAudioFocusChangeCallback() {
        @Override
        public void onAudioFocusLossTransient() {
            JL_Log.e("zzc_id3", "onAudioFocusLossTransient");
            onChangePlayerFlag(PLAYER_FLAG_OTHER);
        }

        @Override
        public void onAudioFocusGain() {
            JL_Log.e("zzc_id3", "onAudioFocusLossTransient");
            onChangePlayerFlag(PLAYER_FLAG_LOCAL);
        }

        @Override
        public void onAudioFocusLossTransientCanDuck() {
        }

        @Override
        public void onAudioLoss() {
            JL_Log.e("zzc_id3", "onAudioLoss");
            onChangePlayerFlag(PLAYER_FLAG_OTHER);
        }
    };
}
