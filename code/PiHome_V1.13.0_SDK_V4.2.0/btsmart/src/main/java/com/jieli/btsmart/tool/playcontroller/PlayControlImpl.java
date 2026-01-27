package com.jieli.btsmart.tool.playcontroller;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;

import com.jieli.audio.media_player.JL_MediaPlayerCallback;
import com.jieli.audio.media_player.JL_PlayMode;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.device.music.MusicStatusInfo;
import com.jieli.bluetooth.bean.device.voice.VolumeInfo;
import com.jieli.bluetooth.bean.response.TargetInfoResponse;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.tool.bluetooth.rcsp.DeviceOpHandler;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.JL_MediaPlayerServiceManager;
import com.jieli.component.utils.HandlerManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chensenhua on 2018/1/30.
 */

public class PlayControlImpl implements PlayControl {
    public static final byte MODE_MUSIC = 1; //播放USB/TF Card模式，说明正在操作设备
    public static final byte MODE_BT = 0; //蓝牙模式，说明蓝牙设备已连接
    public static final byte MODE_AUX = 3; //扩音器模式
    public static final byte MODE_NET_RADIO = 4; //网络电台
    private static volatile PlayControlImpl mInstance;
    private MusicPlayControlImpl mBluetoothPlayControl;
    private final LocalPlayControlImpl mLocalPlayControl;
    private LineInPlayControlImpl auxPlayControl;
    private NetRadioPlayControlImpl mNetRadioPlayControlImpl;
    private PlayControl mPlayControl;
    private final RCSPController mRCSPController = RCSPController.getInstance();

    private final List<PlayControlCallback> callbackList = new ArrayList<>();

    private byte mode = (byte) -1;
    private final String tag = getClass().getSimpleName();

    private boolean onFrontDesk = false;


    public byte getMode() {
        return mode;
    }

    private PlayControlImpl() {
        mLocalPlayControl = new LocalPlayControlImpl(JL_MediaPlayerServiceManager.getInstance().getJl_mediaPlayer(), AppUtil.getContext());
        JL_MediaPlayerServiceManager.getInstance().getJl_mediaPlayer().registerMusicPlayerCallback(jl_mediaPlayerCallback);
        mRCSPController.addBTRcspEventCallback(bluetoothEventCallback);
        updateMode(MODE_BT);
        if (mRCSPController.isDeviceConnected()) {
            onDeviceFunChange(mRCSPController.getDeviceInfo().getCurFunction());
        }
        JL_MediaPlayerServiceManager.getInstance().getJl_mediaPlayer().setSessionCallback(new MediaButtonSessionCallbackImpl(JL_MediaPlayerServiceManager.getInstance().getJl_mediaPlayer(), this));
    }

    public PlayControlCallback getControlCallback() {
        return controlCallback;
    }

    public static PlayControlImpl getInstance() {
        if (mInstance == null) {
            synchronized (PlayControlImpl.class) {
                if (mInstance == null) {
                    mInstance = new PlayControlImpl();
                }
            }
        }
        return mInstance;
    }


    public void updateMode(byte mode) {
        JL_Log.i(tag, "updateMode=" + mode + ", last mode : " + this.mode);
        if (this.mode == mode) {
            return;
        }
        this.mode = mode;
        if (mPlayControl != null) {
            mPlayControl.setPlayControlCallback(null);
            mPlayControl.onPause();
        }

        if (mode == MODE_MUSIC) {
            if (mBluetoothPlayControl == null) {
                mBluetoothPlayControl = new MusicPlayControlImpl();
            }
            mPlayControl = mBluetoothPlayControl;
        } else if (mode == MODE_AUX) {
            if (auxPlayControl == null) {
                auxPlayControl = new LineInPlayControlImpl();
            }
            mPlayControl = auxPlayControl;
        } else if (mode == MODE_NET_RADIO) {
            if (mNetRadioPlayControlImpl == null) {
                mNetRadioPlayControlImpl = NetRadioPlayControlImpl.getInstance(AppUtil.getContext());
            }
            mPlayControl = mNetRadioPlayControlImpl;
        } else {
            mPlayControl = mLocalPlayControl;
        }

        mPlayControl.setPlayControlCallback(controlCallback);
        controlCallback.onModeChange(mode);

        boolean isPlay = mPlayControl.isPlay();
        if (mode != PlayControlImpl.MODE_BT && mLocalPlayControl.isPlay()) {
            JL_Log.e("sen", "pause player");
            mLocalPlayControl.pause();
        } else if (mode == MODE_BT) {
            mPlayControl.refresh();
        }
        if (mode != PlayControlImpl.MODE_NET_RADIO && (mNetRadioPlayControlImpl != null && mNetRadioPlayControlImpl.isPlay())) {
            mNetRadioPlayControlImpl.pause();
        } else if (mode == MODE_NET_RADIO) {
            mPlayControl.refresh();
        }
        if (onFrontDesk) {
            mPlayControl.onStart();
        }

    }

    @Override
    public void playNext() {
        mPlayControl.playNext();
    }


    @Override
    public void playPre() {
        mPlayControl.playPre();
    }

    @Override
    public void play() {
        mPlayControl.play();
    }

    @Override
    public void pause() {
        mPlayControl.pause();
    }

    @Override
    public void playOrPause() {
        mPlayControl.playOrPause();
    }


    @Override
    public void setVolume(int value) {
        mPlayControl.setVolume(value);
    }

    @Override
    public void volumeUp() {
        mPlayControl.volumeUp();
    }

    @Override
    public void volumeDown() {
        mPlayControl.volumeDown();
    }

    @Override
    public void volumeDownByHand() {
        mPlayControl.volumeDownByHand();
    }

    @Override
    public void volumeUpByHand() {
        mPlayControl.volumeUpByHand();
    }

    @Override
    public void setPlaymode(JL_PlayMode playmode) {
        mPlayControl.setPlaymode(playmode);
    }

    @Override
    public void setNextPlaymode() {
        mPlayControl.setNextPlaymode();
    }

    /**
     * 该函数在此无效
     *
     * @param callback 回调
     */
    @Override
    public void setPlayControlCallback(PlayControlCallback callback) {

    }

    @Override
    public void refresh() {
        mPlayControl.refresh();
    }

    @Override
    public void release() {
        if (mBluetoothPlayControl != null) {
            mBluetoothPlayControl.release();
        }

        if (mLocalPlayControl != null) {
            mLocalPlayControl.release();
        }
        HandlerManager.getInstance().getMainHandler().removeCallbacks(mSwitchTask);
        JL_MediaPlayerServiceManager.getInstance().getJl_mediaPlayer().unregisterMusicPlayerCallback(jl_mediaPlayerCallback);
        mRCSPController.removeBTRcspEventCallback(bluetoothEventCallback);
    }

    @Override
    public void seekTo(int position) {
        mPlayControl.seekTo(position);
    }

    @Override
    public boolean isPlay() {
        return mPlayControl.isPlay();
    }

    @Override
    public void onStart() {
        onFrontDesk = true;
        mPlayControl.onStart();
    }

    @Override
    public void onPause() {
        onFrontDesk = false;
        mPlayControl.onPause();
    }


    private boolean isDeviceSpeak;

    public void setDeviceSpeak(boolean deviceSpeak) {
        isDeviceSpeak = deviceSpeak;
    }


    public void registerPlayControlListener(PlayControlCallback controlCallback) {
        callbackList.add(controlCallback);
    }


    public void unregisterPlayControlListener(PlayControlCallback controlCallback) {
        callbackList.remove(controlCallback);
    }


    private final BTRcspEventCallback bluetoothEventCallback = new BTRcspEventCallback() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (DeviceOpHandler.getInstance().isReconnecting()) return;
            if (mRCSPController.getUsingDevice() != null && !mRCSPController.isUsingDevice(device)) return;
            updateMode(MODE_BT);
        }

        @Override
        public void onVolumeChange(BluetoothDevice device, VolumeInfo volume) {
            if (mRCSPController.getUsingDevice() != null && !mRCSPController.isUsingDevice(device)) return;
            controlCallback.onValumeChange(volume.getVolume(), volume.getMaxVol());
        }

        @Override
        public void onAuxStatusChange(BluetoothDevice device, boolean isPlay) {
            if (mRCSPController.getUsingDevice() != null && !mRCSPController.isUsingDevice(device)) return;
            if (isPlay) {
                updateMode(MODE_AUX);
            }
        }

        @Override
        public void onMusicStatusChange(BluetoothDevice device, MusicStatusInfo statusInfo) {
            if (mRCSPController.getUsingDevice() != null && !mRCSPController.isUsingDevice(device)) return;
            if (statusInfo.isPlay() && mRCSPController.getDeviceInfo(device).getCurFunction() == AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC) {
                updateMode(PlayControlImpl.MODE_MUSIC);
            }
        }

        @Override
        public void onDeviceModeChange(BluetoothDevice device, int mode) {
            if (mRCSPController.getUsingDevice() != null && !mRCSPController.isUsingDevice(device)) return;
            onDeviceFunChange((byte) mode);
        }

        @Override
        public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
            if (cmd == null) return;
            if (mRCSPController.getUsingDevice() != null && !mRCSPController.isUsingDevice(device)) return;
            if (cmd.getId() == Command.CMD_RECEIVE_SPEECH_START) {
                HandlerManager.getInstance().getMainHandler().removeCallbacks(mSwitchTask);
            }
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            mode = -1;
            TargetInfoResponse targetInfo = mRCSPController.getDeviceInfo(device);
            if (targetInfo != null) {
                onDeviceFunChange(targetInfo.getCurFunction());
            }
        }
    };

    public void onDeviceFunChange(byte fun) {
        JL_Log.d(tag, "onDeviceFunChange", "fun --> " + fun);
        HandlerManager.getInstance().getMainHandler().removeCallbacks(mSwitchTask);
        if (fun == PlayControlImpl.MODE_MUSIC && mode != fun) {
            updateMode(PlayControlImpl.MODE_MUSIC);
        } else if (fun == PlayControlImpl.MODE_AUX && mode != fun) {
            updateMode(PlayControlImpl.MODE_AUX);
        } else if (fun == PlayControlImpl.MODE_BT && mode != fun) {
            if (mode != PlayControlImpl.MODE_NET_RADIO) {
                HandlerManager.getInstance().getMainHandler().postDelayed(mSwitchTask, 50);
            }
        }

    }

    private final Runnable mSwitchTask = () -> updateMode(PlayControlImpl.MODE_BT);


//    private Runnable mGetPlayStatus = new Runnable() {
//        @Override
//        public void run() {
//            byte fun = mode;
//            if (fun == PlayControlImpl.MODE_MUSIC ) {
//                BluetoothClient.getInstance().getDeviceMucicInfo(null);
//            } else if (fun == PlayControlImpl.MODE_AUX) {
//                BluetoothClient.getInstance().getAuxStatusInfo(null);
//            }
//
//        }
//    };

    private final JL_MediaPlayerCallback jl_mediaPlayerCallback = new JL_MediaPlayerCallback() {
        @Override
        public void onMusicPlay() {
            super.onMusicPlay();
            JL_Log.d(tag, "onMusicPlay");
            if (mode == MODE_NET_RADIO) {
                updateMode(MODE_NET_RADIO);
            } else {
                updateMode(MODE_BT);
            }
//            updateMode(MODE_BT);
            controlCallback.onPlayStateChange(true);
        }
    };


    private final PlayControlCallback controlCallback = new PlayControlCallback() {

        @Override
        public void onTitleChange(String title) {
            for (PlayControlCallback controlCallback : callbackList) {
                controlCallback.onTitleChange(title);
            }

        }

        @Override
        public void onArtistChange(String name) {
            for (PlayControlCallback controlCallback : callbackList) {
                controlCallback.onArtistChange(name);
            }
        }

        @Override
        public void onPlayStateChange(boolean isPlay) {
            for (PlayControlCallback controlCallback : callbackList) {
                controlCallback.onPlayStateChange(isPlay);
            }
        }

        @Override
        public void onPlayModeChange(JL_PlayMode mode) {
            for (PlayControlCallback controlCallback : callbackList) {
                controlCallback.onPlayModeChange(mode);
            }
        }

        @Override
        public void onTimeChange(int current, int total) {
            for (PlayControlCallback controlCallback : callbackList) {
                controlCallback.onTimeChange(current, total);
            }
        }


        @Override
        public void onValumeChange(int current, int max) {
            for (PlayControlCallback controlCallback : callbackList) {
                controlCallback.onValumeChange(current, max);
            }
        }

        @Override
        public void onCoverChange(Bitmap cover) {
            for (PlayControlCallback controlCallback : callbackList) {
                controlCallback.onCoverChange(cover);
            }
        }

        @Override
        public void onFailed(String msg) {
            for (PlayControlCallback controlCallback : callbackList) {
                controlCallback.onFailed(msg);
            }
        }

        @Override
        public void onDownloadStateChange(boolean state) {
            for (PlayControlCallback controlCallback : callbackList) {
                controlCallback.onDownloadStateChange(state);
            }
        }

        @Override
        public void onModeChange(int mode) {
            for (PlayControlCallback controlCallback : callbackList) {
                controlCallback.onModeChange(mode);
            }
        }
    };


    @SuppressLint("DefaultLocale")
    public static String formatTime(int time) {
        if (time <= 0) {
            return "00:00";
        }
        StringBuilder sb = new StringBuilder();
        time /= 1000;
        //小时
        if (time / 3600 > 0) {
            sb.append(AppUtil.formatString("%02d", time / 3600));
            sb.append(":");
        }
        //分钟
        if (time / 60 > 0) {
            sb.append(AppUtil.formatString("%02d", time % 3600 / 60));
            sb.append(":");
        } else {
            sb.append("00:");
        }
        //秒
        sb.append(AppUtil.formatString("%02d", time % 60));
        return sb.toString();
    }


}
