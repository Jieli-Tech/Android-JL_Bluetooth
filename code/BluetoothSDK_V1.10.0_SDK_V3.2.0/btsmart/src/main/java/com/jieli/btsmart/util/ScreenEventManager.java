package com.jieli.btsmart.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;

import com.jieli.bluetooth.interfaces.IScreenEventListener;
import com.jieli.bluetooth.utils.CommonUtil;

import java.util.HashSet;

/**
 * 屏幕事件管理
 *
 * Created by zqjasonzhong on 2018/9/10.
 */
public class ScreenEventManager {

    private static ScreenEventManager instance;
    private ScreenBroadcastReceiver mScreenBroadcastReceiver;
    private HashSet<IScreenEventListener> mScreenEventListeners;

    private static final int TYPE_SCREEN_ON = 0;
    private static final int TYPE_SCREEN_OFF = 1;
    private static final int TYPE_USER_PRESENT = 2;

    private ScreenEventManager(){
        registerReceiver();
    }

    public static ScreenEventManager getInstance(){
        if(instance == null){
            synchronized (ScreenEventManager.class){
                if(instance == null){
                    instance = new ScreenEventManager();
                }
            }
        }
        return instance;
    }

    @Override
    protected void finalize() throws Throwable {
        unregisterReceiver();
        super.finalize();
    }

    public boolean registerScreenEventListener(IScreenEventListener listener){
        boolean ret = false;
        if(listener != null){
            if(mScreenEventListeners == null){
                mScreenEventListeners = new HashSet<>();
            }
            ret = mScreenEventListeners.add(listener);
        }
        return ret;
    }

    public boolean unregisterScreenEventListener(IScreenEventListener listener){
        boolean ret = false;
        if(listener != null && mScreenEventListeners != null){
            ret = mScreenEventListeners.remove(listener);
        }
        return ret;
    }

    public void getScreenState(){
        if(CommonUtil.getMainContext() != null){
            PowerManager manager = (PowerManager) CommonUtil.getMainContext().getSystemService(Context.POWER_SERVICE);
            if(manager != null){
                boolean state;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH){
                    state = manager.isInteractive();
                }else{
                    state = manager.isScreenOn();
                }
                if(state){
                    notifyScreenEvent(TYPE_SCREEN_ON);
                }else{
                    notifyScreenEvent(TYPE_SCREEN_OFF);
                }
            }
        }
    }

    private void registerReceiver(){
        if(mScreenBroadcastReceiver == null && CommonUtil.getMainContext() != null){
            mScreenBroadcastReceiver = new ScreenBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            intentFilter.addAction(Intent.ACTION_USER_PRESENT);
            CommonUtil.getMainContext().registerReceiver(mScreenBroadcastReceiver, intentFilter);
        }
    }

    private void unregisterReceiver(){
        if(mScreenBroadcastReceiver != null && CommonUtil.getMainContext() != null){
            CommonUtil.getMainContext().unregisterReceiver(mScreenBroadcastReceiver);
            mScreenBroadcastReceiver = null;
        }
    }

    private class ScreenBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null){
                String action = intent.getAction();
                if(!TextUtils.isEmpty(action)){
                    switch (action){
                        case Intent.ACTION_SCREEN_ON:{
                            notifyScreenEvent(TYPE_SCREEN_ON);
                            break;
                        }
                        case Intent.ACTION_SCREEN_OFF:{
                            notifyScreenEvent(TYPE_SCREEN_OFF);
                            break;
                        }
                        case Intent.ACTION_USER_PRESENT:{
                            notifyScreenEvent(TYPE_USER_PRESENT);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void notifyScreenEvent(final int type){
        if(CommonUtil.getMainHandler() != null){
            CommonUtil.getMainHandler().post(() -> {
                if(mScreenEventListeners != null){
                    for (IScreenEventListener listener : new HashSet<>(mScreenEventListeners)){
                        switch (type){
                            case TYPE_SCREEN_ON: { //screen on
                                listener.onScreenOn();
                                break;
                            }
                            case TYPE_SCREEN_OFF: { //screen off
                                listener.onScreenOff();
                                break;
                            }
                            case TYPE_USER_PRESENT: { //user present
                                listener.onUserPresent();
                                break;
                            }
                        }
                    }
                }
            });
        }
    }
}
