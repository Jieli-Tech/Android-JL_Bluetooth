package com.jieli.btsmart.ui.multimedia.control.id3;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.text.TextUtils;

import com.jieli.audio.media_player.AudioFocusManager;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.music.ID3MusicInfo;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.ui.base.bluetooth.BluetoothBasePresenter;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.SystemUtil;

/**
 * ID3控制逻辑实现
 *
 * @author zqjasonZhong
 * @since 2020/6/5
 */
public class ID3ControlPresenterImpl extends BluetoothBasePresenter implements ID3ControlContract.ID3ControlPresenter {
    private final ID3ControlContract.ID3ControlView mView;

    private PlayControlImpl mPlayControl;
    private AudioFocusManager mAudioFocusManager;

    private int showPlayerFlag = PLAYER_FLAG_NONE;
    private boolean tryToPlayState;
    private int MSG_PLAY_RESPONSE_OVER_TIME = 120;
    private boolean playCmdIsResponse = true;
    public final static int PLAYER_FLAG_NONE = 0;
    public final static int PLAYER_FLAG_LOCAL = 1;
    public final static int PLAYER_FLAG_OTHER = 2;
    public final static int PLAYER_FLAG_NET_RADIO = 3;
    public final static int PLAYER_FLAG_OTHER_EMPTY = 4;
    private final Handler handler = new android.os.Handler(message -> {
        if (message.what == MSG_PLAY_RESPONSE_OVER_TIME) {
            playCmdIsResponse = true;
        }
        return false;
    });

    public ID3ControlPresenterImpl(ID3ControlContract.ID3ControlView view) {
        super(view);
        mView = SystemUtil.checkNotNull(view);

        getRCSPController().addBTRcspEventCallback(mEventCallback);
    }

    @Override
    public int showPlayerFlag() {
        return showPlayerFlag;
    }

    @Override
    public void updatePlayerFlag(int flag) {
        showPlayerFlag = flag;
    }

    @Override
    public ID3MusicInfo getMusicInfo() {
        if (getDeviceInfo() == null) return null;
        return getDeviceInfo().getiD3MusicInfo();
    }

    @Override
    public void getID3MusicInfo() {
        JL_Log.e("zzc_id3", "getID3MusicInfo >>>>>>>  ");
        mRCSPController.getDeviceSettingsInfo(mRCSPController.getUsingDevice(), 0xffffffff, new OnRcspActionCallback<ADVInfoResponse>() {
            @Override
            public void onSuccess(BluetoothDevice device, ADVInfoResponse message) {
                mView.onID3CmdSuccess(message);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                mView.onID3CmdFailed(error);
            }
        });
    }

    @Override
    public void playID3Prev() {
        if (!playCmdIsResponse) return;
        mRCSPController.iD3MusicPlayPrev(mRCSPController.getUsingDevice(), null);
    }

    @Override
    public void playID3Next() {
        if (!playCmdIsResponse) return;
        mRCSPController.iD3MusicPlayNext(mRCSPController.getUsingDevice(), null);
    }

    /**
     * 播放暂停ID3音乐
     *
     * @param srcPlayState 当前的ID3播放状态
     */
    @Override
    public void playOrPauseID3(boolean srcPlayState) {
        JL_Log.d("zzc_id3", " isID3Play(): " + isID3Play() + " tryToPlayState: " + tryToPlayState + " srcPlayState: " + srcPlayState);
        if (!playCmdIsResponse) return;
        playCmdIsResponse = false;
        mRCSPController.iD3MusicPlayOrPause(mRCSPController.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                tryToPlayState = !srcPlayState;
                handler.removeMessages(MSG_PLAY_RESPONSE_OVER_TIME);
                handler.sendEmptyMessageDelayed(MSG_PLAY_RESPONSE_OVER_TIME, 3000L);
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
    public void destroy() {
        destroyRCSPController(mEventCallback);
    }

    @Override
    public void start() {
    }

    private PlayControlImpl getPlayControl() {
        if (mPlayControl == null) {
            mPlayControl = PlayControlImpl.getInstance();
        }
        return mPlayControl;
    }

    private AudioFocusManager getAudioFocusManager() {
        if (mAudioFocusManager == null) {
            mAudioFocusManager = AudioFocusManager.getInstance();
            mAudioFocusManager.init(AppUtil.getContext());
        }
        return mAudioFocusManager;
    }

    private boolean isID3Play() {
        return !getPlayControl().isPlay() && !getAudioFocusManager().isHasAudioFocus() && getAudioFocusManager().isMusicPlay();
    }

    private final BTRcspEventCallback mEventCallback = new BTRcspEventCallback() {

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (status == StateCode.CONNECTION_DISCONNECT || status == StateCode.CONNECTION_FAILED) {
                updatePlayerFlag(PLAYER_FLAG_NONE);
            } else if (status == StateCode.CONNECTION_OK) {
                updatePlayerFlag(PLAYER_FLAG_NONE);
            }
        }

        @Override
        public void onID3MusicInfo(BluetoothDevice device, ID3MusicInfo id3MusicInfo) {
            JL_Log.d("zzc_id3", "onID3MusicInfo : " + id3MusicInfo + ", showPlayerFlag : " + showPlayerFlag + ", isID3Play : " + isID3Play());
            if (showPlayerFlag != PLAYER_FLAG_OTHER) {
                return;
            }
            if (TextUtils.isEmpty(id3MusicInfo.getArtist())) {
                return;
            }
            if (id3MusicInfo.getCurrentTime() == 0 || id3MusicInfo.getCurrentTime() <= id3MusicInfo.getTotalTime() + 1) {
                if (id3MusicInfo.isPlayStatus() != isID3Play()) {
                    id3MusicInfo.setPlayStatus(isID3Play());
                }
                JL_Log.d("zzc_id3", " isID3Play(): " + isID3Play() + " tryToPlayState: " + tryToPlayState + " id3MusicInfo: " + id3MusicInfo);
                if (isID3Play() == tryToPlayState) {
                    playCmdIsResponse = true;
                }
                mView.onID3MusicInfo(id3MusicInfo);
            }
        }
    };
}
