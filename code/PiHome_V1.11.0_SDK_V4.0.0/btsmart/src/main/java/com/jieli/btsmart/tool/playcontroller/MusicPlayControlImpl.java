package com.jieli.btsmart.tool.playcontroller;

import android.bluetooth.BluetoothDevice;
import android.widget.Toast;

import com.jieli.audio.media_player.JL_PlayMode;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.command.file_op.StopFileBrowseCmd;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.music.MusicNameInfo;
import com.jieli.bluetooth.bean.device.music.MusicStatusInfo;
import com.jieli.bluetooth.bean.device.music.PlayModeInfo;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.bluetooth.rcsp.BTRcspHelper;
import com.jieli.component.utils.HandlerManager;
import com.jieli.component.utils.ToastUtil;
import com.jieli.filebrowse.FileBrowseManager;
import com.jieli.filebrowse.FileBrowseUtil;
import com.jieli.filebrowse.bean.FileStruct;
import com.jieli.filebrowse.bean.SDCardBean;
import com.jieli.filebrowse.interfaces.FileObserver;

import java.util.List;


/**
 * Created by chensenhua on 2018/1/17.
 * 设备模式下的播放控制类
 */

class MusicPlayControlImpl implements PlayControl {
    private static final int MIN_STEP = 3000;
    private final String tag = getClass().getSimpleName();
    private PlayControlCallback mPlayControlCallback;
    private final RCSPController mRCSPController = RCSPController.getInstance();

    private boolean onFrontdesk = false;
    private static boolean isSetVolume = false;
    private int duration;
    private boolean isPlay;

    private String name;

    private int mode;
    private int startTime;
    private int dev;
    private long beginCountTime = 0;//记录设置startTime的时间
    private final SeekStatus mSeekStatus = new SeekStatus();


    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            //   refresh();
            if (!onFrontdesk) {
                HandlerManager.getInstance().getMainHandler().removeCallbacks(mRunnable);
                return;
            }
            HandlerManager.getInstance().getMainHandler().removeCallbacks(mRunnable);
            HandlerManager.getInstance().getMainHandler().postDelayed(mRunnable, 1000);

            long delay = System.currentTimeMillis() - beginCountTime;
            startTime += delay;
            if (mPlayControlCallback != null) {
                mPlayControlCallback.onTimeChange(startTime, duration);
            }
            setStartTime(startTime);
        }
    };

    private void setStartTime(int startTime) {
        this.startTime = startTime;
        beginCountTime = System.currentTimeMillis();
    }

    public MusicPlayControlImpl() {
        mRCSPController.addBTRcspEventCallback(callback);
        JL_Log.e(tag, "music play bluetoothEventCallback=" + callback);
        FileBrowseManager.getInstance().addFileObserver(fileObserver);
        if (mRCSPController.isDeviceConnected()) {
            mRCSPController.getDeviceMusicInfo(mRCSPController.getUsingDevice(), null);
        }

    }

    @Override
    public void playNext() {
        mRCSPController.musicPlayNext(mRCSPController.getUsingDevice(), null);
    }

    @Override
    public void playOrPause() {
        play();
    }

    @Override
    public void playPre() {
        mRCSPController.musicPlayPrev(mRCSPController.getUsingDevice(), null);
    }

    @Override
    public void play() {
        mRCSPController.musicPlayOrPause(mRCSPController.getUsingDevice(), null);
    }

    @Override
    public void pause() {
        mRCSPController.musicPlayOrPause(mRCSPController.getUsingDevice(), null);
    }


    @Override
    public void setVolume(int value) {
        if (!isSetVolume) {
            BTRcspHelper.adjustVolume(mRCSPController, MainApplication.getApplication(), value, null);
        }
    }

    private void volumeUp(int step) {
        if (!isSetVolume) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
            if (deviceInfo == null) return;
            int volume = deviceInfo.getVolume();
            int max = deviceInfo.getMaxVol();
            if (volume == max) {
                ToastUtil.showToastTop(MainApplication.getApplication().getString(R.string.current_volume) + volume, Toast.LENGTH_SHORT);
                return;
            }

            int currentVol = volume + step;
            if (currentVol > max) {
                currentVol = max;
            }
            setVolume(currentVol);

        }
    }


    @Override
    public void volumeUp() {
        volumeUp(SConstant.DEVICE_VOLUME_STEP);
    }

    @Override
    public void volumeDown() {
        volumeDown(SConstant.DEVICE_VOLUME_STEP);
    }

    private void volumeDown(int step) {
        if (!isSetVolume) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
            if (deviceInfo == null) return;
            int volume = deviceInfo.getVolume();
            if (volume == 0) {
                ToastUtil.showToastTop(MainApplication.getApplication().getString(R.string.current_volume) + volume, Toast.LENGTH_SHORT);
                return;
            }
            int currentVol = volume - step;
            if (currentVol < 0) {
                currentVol = 0;
            }
            setVolume(currentVol);
        }
    }

    @Override
    public void volumeDownByHand() {
        volumeDown(1);
    }

    @Override
    public void volumeUpByHand() {
        volumeUp(1);
    }

    @Override
    public void setPlaymode(JL_PlayMode playmode) {
        setNextPlaymode();
    }

    @Override
    public void setNextPlaymode() {
        if (mRCSPController.isDeviceConnected()) {
            mRCSPController.musicSwitchNextPlayMode(mRCSPController.getUsingDevice(), null);
        }
    }

    @Override
    public void setPlayControlCallback(PlayControlCallback callback) {
        setStartTime(0);
        mPlayControlCallback = callback;
    }

    @Override
    public void refresh() {
        if (mRCSPController.isDeviceConnected() && mPlayControlCallback != null) {
            mPlayControlCallback.onDownloadStateChange(false);
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
            if (deviceInfo == null) return;
            mPlayControlCallback.onValumeChange(deviceInfo.getVolume(), deviceInfo.getMaxVol());
            if (deviceInfo.getCurFunction() == PlayControlImpl.getInstance().getMode()) {
                mRCSPController.getDeviceMusicInfo(mRCSPController.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
                    @Override
                    public void onSuccess(BluetoothDevice device, Boolean message) {
                        mRCSPController.getDeviceMusicStatusInfo(device, null);
                    }

                    @Override
                    public void onError(BluetoothDevice device, BaseError error) {

                    }
                });
            } else {
                mPlayControlCallback.onPlayStateChange(false);
                mPlayControlCallback.onTimeChange(startTime, duration);
                mPlayControlCallback.onTitleChange(name);
                mPlayControlCallback.onPlayModeChange(getPlayMode(mode));
                mPlayControlCallback.onArtistChange(FileBrowseUtil.getDevName(dev));
            }
            mPlayControlCallback.onCoverChange(null);
            isSetVolume = false;
        }
    }


    @Override
    public void release() {
        JL_Log.e(tag, "music play --------=  release");
        mRCSPController.removeBTRcspEventCallback(callback);
        HandlerManager.getInstance().getMainHandler().removeCallbacks(mRunnable);
        FileBrowseManager.getInstance().removeFileObserver(fileObserver);
    }

    @Override
    public void seekTo(int position) {
        JL_Log.i(tag, "seekTo pos=" + position);
        int temp = position - startTime;
        if (Math.abs(temp) < MIN_STEP) {
            return;
        }
        HandlerManager.getInstance().getMainHandler().removeCallbacks(mRunnable);
        temp = temp / 1000;
        if (temp == 0) {
            return;
        } else if (temp < 0) {
            mSeekStatus.type = SeekStatus.TYPE_BACK;
        } else {
            mSeekStatus.type = SeekStatus.TYPE_FORWARD;
        }
        short time = (short) Math.abs(temp);
        mSeekStatus.pos = position;
        mSeekStatus.retryCount = 0;
        setStartTime(position);
        mRCSPController.musicSeekToProgress(mRCSPController.getUsingDevice(), mSeekStatus.type, time, null);

    }

    @Override
    public boolean isPlay() {
        return isPlay;
    }

    @Override
    public void onStart() {
        if (mRCSPController.isDeviceConnected() && mRCSPController.getDeviceInfo().getCurFunction() == PlayControlImpl.getInstance().getMode()) {
            if (!onFrontdesk) {
                onFrontdesk = true;
//                if (isPlay()) {
//                    HandlerManager.getInstance().getMainHandler().post(mRunnable);
//                }
                mRCSPController.getDeviceMusicStatusInfo(mRCSPController.getUsingDevice(), null);
            }
        }
    }

    @Override
    public void onPause() {
        onFrontdesk = false;
        HandlerManager.getInstance().getMainHandler().removeCallbacks(mRunnable);
    }

    private JL_PlayMode getPlayMode(int playmode) {
        JL_PlayMode jl_playMode;
        switch (playmode) {
            case 0x01:
                jl_playMode = JL_PlayMode.ALL_LOOP;
                break;
            case 0x02:
                jl_playMode = JL_PlayMode.DEVICE_LOOP;
                break;
            case 0x03:
                jl_playMode = JL_PlayMode.ONE_LOOP;
                break;
            case 0x04:
                jl_playMode = JL_PlayMode.ALL_RANDOM;
                break;
            case 0x05:
                jl_playMode = JL_PlayMode.FOLDER_LOOP;
                break;
            default:
                jl_playMode = JL_PlayMode.NONE;
        }

        return jl_playMode;
    }


    private final BTRcspEventCallback callback = new BTRcspEventCallback() {

        @Override
        public void onMusicNameChange(BluetoothDevice device, MusicNameInfo nameInfo) {
            if (mRCSPController.getUsingDevice() != null && !mRCSPController.isUsingDevice(device)) return;
            JL_Log.i(tag, "nameInfo music play status=" + nameInfo.toString());
            name = nameInfo.getName();
            if (mPlayControlCallback != null) {
                mPlayControlCallback.onTitleChange(nameInfo.getName());
                mPlayControlCallback.onCoverChange(null);
                mRCSPController.getDeviceMusicStatusInfo(device, null);
            }
            mSeekStatus.reset();
        }

        @Override
        public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
            if (mRCSPController.getUsingDevice() != null && !mRCSPController.isUsingDevice(device)) return;
            if (cmd.getId() == Command.CMD_STOP_FILE_BROWSE) {
                StopFileBrowseCmd stopFileBrowseCmd = (StopFileBrowseCmd) cmd;
                if (stopFileBrowseCmd.getParam().getReason() == 2) {
                    HandlerManager.getInstance().getMainHandler().removeCallbacks(mRunnable);
                    setStartTime(0);
                    if (mPlayControlCallback != null) {
                        mPlayControlCallback.onTimeChange(startTime, duration);
                    }
                }
            }
        }

        @Override
        public void onMusicStatusChange(BluetoothDevice device, MusicStatusInfo statusInfo) {
            if (mRCSPController.getUsingDevice() != null && !mRCSPController.isUsingDevice(device)) return;
            setStartTime(statusInfo.getCurrentTime());
            duration = statusInfo.getTotalTime();
            isPlay = statusInfo.isPlay();
            dev = statusInfo.getCurrentDev();
            JL_Log.d(tag, "onMusicStatusChange-->" + mSeekStatus.type + "\tstartTime=" + startTime + "\tmSeekStatus.pos=" + mSeekStatus.pos
                    + ",\n dev = " + dev + ", retryCount = " + mSeekStatus.retryCount);
            if (mSeekStatus.type == SeekStatus.TYPE_FORWARD) {
                if (startTime < mSeekStatus.pos - MIN_STEP && mSeekStatus.retryCount < SeekStatus.RETRY_LIMIT) {
                    HandlerManager.getInstance().getMainHandler().postDelayed(() -> {
                        mSeekStatus.retryCount++;
                        mRCSPController.getDeviceMusicStatusInfo(device, null);
                    }, 200);
                    return;
                }
            } else if (mSeekStatus.type == SeekStatus.TYPE_BACK) {
                if (startTime >= mSeekStatus.pos + MIN_STEP && mSeekStatus.retryCount < SeekStatus.RETRY_LIMIT) {
                    HandlerManager.getInstance().getMainHandler().postDelayed(() -> {
                        mSeekStatus.retryCount++;
                        mRCSPController.getDeviceMusicStatusInfo(device, null);
                    }, 200);
                    return;
                }
            }

            mSeekStatus.reset();
            if (mPlayControlCallback != null) {
                mPlayControlCallback.onArtistChange(FileBrowseUtil.getDevName(statusInfo.getCurrentDev()));
                mPlayControlCallback.onTimeChange(startTime, duration);
                mPlayControlCallback.onPlayStateChange(statusInfo.isPlay());
            }
            if (statusInfo.isPlay()) {
                onFrontdesk = true;
                HandlerManager.getInstance().getMainHandler().post(mRunnable);
            } else {
                HandlerManager.getInstance().getMainHandler().removeCallbacks(mRunnable);
            }
        }

        @Override
        public void onDeviceModeChange(BluetoothDevice device, int mode) {
            if (mRCSPController.getUsingDevice() != null && !mRCSPController.isUsingDevice(device)) return;
            this.onDeviceFunChange(device, CHexConver.intToByte(mode));
        }

        private void onDeviceFunChange(BluetoothDevice device, byte fun) {
            JL_Log.i("sen", "music play onDeviceFunChange=" + fun);
            if (fun != PlayControlImpl.MODE_MUSIC && mPlayControlCallback != null) {
                mPlayControlCallback.onPlayStateChange(false);
                onPause();
                if (mRCSPController.getDeviceInfo(device) != null && mRCSPController.getDeviceInfo(device).getSdkType() == JLChipFlag.JL_CHIP_FLAG_692X_ST_SOUNDBOX) {
                    setStartTime(0);
                    mPlayControlCallback.onTimeChange(startTime, duration);
                }
            } else if (mPlayControlCallback != null) {
                refresh();
            }
        }

        @Override
        public void onPlayModeChange(BluetoothDevice device, PlayModeInfo playModeInfo) {
            mode = playModeInfo.getPlayMode();
            if (mPlayControlCallback != null) {
                mPlayControlCallback.onPlayModeChange(getPlayMode(playModeInfo.getPlayMode()));
            }
        }
    };


    private final FileObserver fileObserver = new FileObserver() {
        @Override
        public void onFileReceiver(List<FileStruct> fileStructs) {

        }

        @Override
        public void onFileReadStop(boolean isEnd) {

        }

        @Override
        public void onFileReadStart() {

        }

        @Override
        public void onFileReadFailed(int reason) {

        }

        @Override
        public void onSdCardStatusChange(List<SDCardBean> onLineCards) {

        }

        @Override
        public void OnFlayCallback(boolean success) {
            if (success) {
                isPlay = success;
                if (mPlayControlCallback != null) {
                    mPlayControlCallback.onPlayStateChange(true);
                }
            }

        }
    };


    private static class SeekStatus {
        final static int TYPE_FORWARD = 0;
        final static int TYPE_BACK = 1;

        final static int RETRY_LIMIT = 5;

        int type = -1;
        int pos = 0;
        int retryCount = 0;

        void reset() {
            type = -1;
            pos = -1;
            retryCount = 0;
        }
    }
}
