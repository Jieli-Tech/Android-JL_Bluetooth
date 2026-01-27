package com.jieli.btsmart.tool.playcontroller;

import android.bluetooth.BluetoothDevice;
import android.widget.Toast;

import com.jieli.audio.media_player.JL_PlayMode;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.bluetooth.rcsp.BTRcspHelper;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.ToastUtil;


/**
 * Created by chensenhua on 2018/1/17.
 * 设备模式下的播放控制类
 */

class LineInPlayControlImpl implements PlayControl {
    private final String tag = getClass().getSimpleName();
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private PlayControlCallback mPlayControlCallback;
    private static boolean isSetVolume = false;
    private boolean isPlay = true;


    public LineInPlayControlImpl() {
        mRCSPController.addBTRcspEventCallback(callback);
        mRCSPController.getAuxStatusInfo(mRCSPController.getUsingDevice(), null);
    }

    @Override
    public void playNext() {


    }

    @Override
    public void playOrPause() {
        boolean isPlay = this.isPlay;
        if (isPlay) {
            pause();
        } else {
            play();
        }

    }

    @Override
    public void playPre() {

    }

    @Override
    public void play() {
        mRCSPController.auxPlayOrPause(mRCSPController.getUsingDevice(), null);
    }

    @Override
    public void pause() {
        play();
    }


    @Override
    public void setVolume(int value) {
        if (!isSetVolume) {
            BTRcspHelper.adjustVolume(mRCSPController, MainApplication.getApplication(), value, null);
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

    @Override
    public void volumeDownByHand() {
        volumeDown(1);
    }

    @Override
    public void volumeUpByHand() {
        volumeUp(1);
    }

    @Override
    public void setPlaymode(JL_PlayMode playMode) {

    }

    @Override
    public void setNextPlaymode() {

    }

    @Override
    public void setPlayControlCallback(PlayControlCallback callback) {
        mPlayControlCallback = callback;
    }

    @Override
    public void refresh() {
        if (mRCSPController.isDeviceConnected()) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
            if (deviceInfo == null) return;
            mPlayControlCallback.onDownloadStateChange(false);
            mPlayControlCallback.onValumeChange(deviceInfo.getVolume(), deviceInfo.getMaxVol());
            if (deviceInfo.getCurFunction() == AttrAndFunCode.SYS_INFO_FUNCTION_AUX) {
                mRCSPController.getAuxStatusInfo(mRCSPController.getUsingDevice(), null);
            } else {
                mPlayControlCallback.onPlayStateChange(false);
            }
            mPlayControlCallback.onCoverChange(null);
            mPlayControlCallback.onPlayStateChange(isPlay);
            isSetVolume = false;

        }
    }


    @Override
    public void release() {
        mRCSPController.removeBTRcspEventCallback(callback);
    }

    @Override
    public void seekTo(int position) {

    }

    @Override
    public boolean isPlay() {
        return isPlay;

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onPause() {

    }

    private void volumeUp(int step) {
        if (!isSetVolume) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
            if (deviceInfo == null) return;
            int volume = deviceInfo.getVolume();
            int max = deviceInfo.getMaxVol();
            if (volume == max) {
                ToastUtil.showToastTop(AppUtil.getContext().getString(R.string.current_volume) + volume, Toast.LENGTH_SHORT);
                return;
            }

            int currentVol = volume + step;
            if (currentVol > max) {
                currentVol = max;
            }
            setVolume(currentVol);

        }
    }

    private void volumeDown(int step) {
        if (!isSetVolume) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
            if (deviceInfo == null) return;
            int volume = deviceInfo.getVolume();
            if (volume == 0) {
                ToastUtil.showToastTop(AppUtil.getContext().getString(R.string.current_volume) + volume, Toast.LENGTH_SHORT);
                return;
            }
            int currentVol = volume - step;
            if (currentVol < 0) {
                currentVol = 0;
            }
            setVolume(currentVol);
        }
    }

    private final BTRcspEventCallback callback = new BTRcspEventCallback() {

        @Override
        public void onAuxStatusChange(BluetoothDevice device, boolean isPlay) {
            LineInPlayControlImpl.this.isPlay = isPlay;
            if (mPlayControlCallback != null) {
                mPlayControlCallback.onPlayStateChange(isPlay);
            }
        }
    };


}
