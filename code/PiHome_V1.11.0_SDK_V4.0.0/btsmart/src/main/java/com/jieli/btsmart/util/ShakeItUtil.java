package com.jieli.btsmart.util;

import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.jieli.btsmart.R;

/**
 * @ClassName: ShakeItUtil
 * @Description: 摇一摇功能
 * @Author: ZhangHuanMing
 * @CreateDate: 2021/1/29 9:50
 */
public class ShakeItUtil {
    private final Context mContext;
    private final SensorManager sensorManager;
    private final ShakeSensorListener shakeListener;
    private final Handler mUIHandler = new Handler(Looper.getMainLooper());

    private ShakeItListener mShakeItListener;
    boolean isStart = false;

    public ShakeItUtil(Context context) {
        this.mContext = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        shakeListener = new ShakeSensorListener();
        //注册监听加速度传感器
    }

    public void startShakeIt() {
        if (isStart) return;
        isStart = true;
        sensorManager.registerListener(shakeListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
    }

    public void stopShakeIt() {
        //取消注册
        if (!isStart) return;
        isStart = false;
        sensorManager.unregisterListener(shakeListener);
    }

    public void setShakeItListener(ShakeItListener shakeItListener) {
        this.mShakeItListener = shakeItListener;
    }

    public void release() {
        stopShakeIt();
        mUIHandler.removeCallbacksAndMessages(null);
    }

    private class ShakeSensorListener implements SensorEventListener {
        boolean isShake = false;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (isShake) return;
            int type = event.sensor.getType();
            if (type == Sensor.TYPE_ACCELEROMETER) {
                float[] values = event.values;
                /*
                 * x : x轴方向的重力加速度，向右为正,屏幕的width方向是x轴
                 * y : y轴方向的重力加速度，向前为正，屏幕的height方向是y轴
                 * z : z轴方向的重力加速度，向上为正，垂直于屏幕方向是z轴
                 */
                float x = Math.abs(values[0]);
                float y = Math.abs(values[1]);
                float z = Math.abs(values[2]);
                //加速度超过19，摇一摇成功
                if (x > 35 || y > 30 || z > 45) {
                    Log.d("TAG", "onSensorChanged: x : " + x + " y : " + y + " z : " + z);
                    isShake = true;
                    if (mShakeItListener != null) {
                        mShakeItListener.onShakeItStart();
                    }
                    //播放声音
//                    playSound(mContext);//FM状态下有声音输入，小机自动切换模式
                    //震动，注意权限
                    vibrate(500);
                    //仿网络延迟操作，这里可以去请求服务器...
                    mUIHandler.postDelayed(() -> {
                        if (mShakeItListener != null) {
                            mShakeItListener.onShakeItEnd();
                        }
                        isShake = false;
                    }, 1000);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

    private void playSound(Context context) {
        MediaPlayer player = MediaPlayer.create(context, R.raw.shake_sound);
        player.start();
    }

    private void vibrate(long milliseconds) {
        if (null == mContext || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        Vibrator vibrator = (Vibrator) mContext.getSystemService(Service.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {// 判断手机硬件是否有振动器
            VibrationEffect vibrationEffect = VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE);
            vibrator.vibrate(vibrationEffect);
        }
    }

    public interface ShakeItListener {
        void onShakeItStart();

        void onShakeItEnd();
    }
}
