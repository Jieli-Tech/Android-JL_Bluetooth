package com.jieli.btsmart.tool.bluetooth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.component.utils.SystemUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 铃声处理类
 *
 * @author zqjasonZhong
 * @since 2020/7/24
 */
public class RingHandler {
    private final static String TAG = RingHandler.class.getSimpleName();
    @SuppressLint("StaticFieldLeak")
    private volatile static RingHandler instance;
    private Context mContext;

    private final RingtoneManager ringtoneManager;
    private Ringtone mAlarmRing;
    private int cacheMusicVolume = -1;
    private int playStatus = -1;

    private final List<OnRingStatusListener> mOnRingStatusListenerList = new ArrayList<>();
    private final static int CHECK_PLAY_STATUS_INTERVAL = 3 * 1000;
    private final static int MSG_ALARM_RING_TIMEOUT = 0x6985;
    private final static int MSG_CHECK_PLAY_STATUS = 0x6986;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), this::handleMessage);

    private RingHandler(Context context) {
        mContext = SystemUtil.checkNotNull(context);

        ringtoneManager = new RingtoneManager(MainApplication.getApplication());
    }

    public static RingHandler getInstance() {
        if (null == instance) {
            synchronized (RingHandler.class) {
                if (null == instance) {
                    instance = new RingHandler(MainApplication.getApplication());
                }
            }
        }
        return instance;
    }

    public void destroy() {
        stopAlarmRing();
        mHandler.removeCallbacksAndMessages(null);
        mOnRingStatusListenerList.clear();
        mAlarmRing = null;
        mContext = null;
        instance = null;
    }

    public void registerOnRingStatusListener(OnRingStatusListener onRingStatusListener) {
        if (!mOnRingStatusListenerList.contains(onRingStatusListener)) {
            mOnRingStatusListenerList.add(onRingStatusListener);
        }
    }

    public void unregisterOnRingStatusListener(OnRingStatusListener onRingStatusListener) {
        mOnRingStatusListenerList.remove(onRingStatusListener);
    }

    /**
     * 从系统中获取当前闹钟铃声
     *
     * @return 当前闹钟铃声
     */
    private Ringtone getCurrentAlarmRing() {
//        Uri currentAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        Uri currentAlarm = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALARM); //系统当前  闹钟铃声
        Ringtone ringtone = RingtoneManager.getRingtone(mContext, currentAlarm);
        JL_Log.d(TAG, "getCurrentAlarmRing : " + currentAlarm + ", ringtone : " + ringtone);
        if (ringtone == null) {
            if (ringtoneManager.getCursor() != null && ringtoneManager.getCursor().getCount() > 0) {
                ringtone = ringtoneManager.getRingtone(0);
            }
        }
        return ringtone;
    }

    /**
     * 获取当前闹钟铃声
     *
     * @return 当前闹钟铃声
     */
    public Ringtone getAlarmRing() {
        if (mAlarmRing == null) {
            mAlarmRing = getCurrentAlarmRing();
        }
        return mAlarmRing;
    }

    /**
     * 当前铃声是否播放
     *
     * @return 结果
     */
    public boolean isPlayAlarmRing() {
        return mAlarmRing != null && mAlarmRing.isPlaying();
    }

    /**
     * 播放闹钟铃声
     */
    public void playAlarmRing(int type, long timeout) {
        if (mAlarmRing == null) {
            mAlarmRing = getCurrentAlarmRing();
        }
        if (mAlarmRing != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mAlarmRing.setLooping(true);
                mAlarmRing.setVolume(1.0f);
            }
            AudioAttributes audioAttributes;
            AudioAttributes.Builder build;
            if (type == Constants.SEARCH_TYPE_DEVICE) { //找设备
                build = new AudioAttributes.Builder()
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);
            } else { //找手机
                build = new AudioAttributes.Builder()
                        .setLegacyStreamType(AudioManager.STREAM_RING);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                build.setHapticChannelsMuted(true);
            }
            audioAttributes = build.build();
            mAlarmRing.setAudioAttributes(audioAttributes);
            if (mAlarmRing.isPlaying()) {
                mAlarmRing.stop();
            }
            try {
                mAlarmRing.play();
                changeMaxVolume();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mHandler.removeMessages(MSG_ALARM_RING_TIMEOUT);
            if (timeout > 0) {
                mHandler.sendEmptyMessageDelayed(MSG_ALARM_RING_TIMEOUT, timeout);
            }
            mHandler.sendEmptyMessage(MSG_CHECK_PLAY_STATUS);
        }
    }

    /**
     * 关闭闹钟铃声
     */
    public void stopAlarmRing() {
        if (mAlarmRing != null && mAlarmRing.isPlaying()) {
            mAlarmRing.stop();
        }
        resetVolume();
        if (mAlarmRing != null) {
            onRingStatus(mAlarmRing.isPlaying());
        }
        mHandler.removeMessages(MSG_CHECK_PLAY_STATUS);
        mHandler.removeMessages(MSG_ALARM_RING_TIMEOUT);
    }

    private void changeMaxVolume() {
        if (mContext == null) return;
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (null != audioManager) {
            cacheMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        }
    }

    private void resetVolume() {
        if (mContext == null) return;
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (null != audioManager && cacheMusicVolume != -1) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, cacheMusicVolume, 0);
            cacheMusicVolume = -1;
        }
    }

    private void onRingStatus(final boolean isPlay) {
        int status = isPlay ? 1 : 0;
        if (playStatus == status) return;
        playStatus = status;
        if (!mOnRingStatusListenerList.isEmpty()) {
            mHandler.post(() -> {
                for (OnRingStatusListener listener : mOnRingStatusListenerList) {
                    listener.onRingStatusChange(isPlay);
                }
            });
        }
    }

    private boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_ALARM_RING_TIMEOUT:
                stopAlarmRing();
                break;
            case MSG_CHECK_PLAY_STATUS:
                if (mAlarmRing != null) {
                    onRingStatus(mAlarmRing.isPlaying());
                }
                mHandler.sendEmptyMessageDelayed(MSG_CHECK_PLAY_STATUS, CHECK_PLAY_STATUS_INTERVAL);
                break;
        }
        return false;
    }

    public interface OnRingStatusListener {

        void onRingStatusChange(boolean isPlay);
    }
}
