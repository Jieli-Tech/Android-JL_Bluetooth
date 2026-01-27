package com.jieli.btsmart.ui.widget.product_dialog;

import android.bluetooth.BluetoothAdapter;

import com.jieli.btsmart.data.model.bluetooth.BlackFlagInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 黑名单管理
 *
 * @author zqjasonZhong
 * @date 2019/8/8
 */
public class BlacklistHandler {

    private volatile static BlacklistHandler instance;

   /* private final int TIMEOUT = 180 * 1000;
    private final static int MSG_CHECK_BLACKlIST = 0x364;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg == null) return false;
            if (msg.what == MSG_CHECK_BLACKlIST) {
                if (mBlacklist.size() > 0) {
                    mBlacklist.remove(0);
                    startTimeTask();
                }
            }
            return false;
        }
    });*/

    /**
     * 黑名单
     * 作用: 拦截已拒绝的设备显示</p>
     */
    private final List<BlackFlagInfo> mBlacklist = new ArrayList<>();

    private BlacklistHandler() {

    }

    public static BlacklistHandler getInstance() {
        if (instance == null) {
            synchronized (BlacklistHandler.class) {
                if (instance == null) {
                    instance = new BlacklistHandler();
                }
            }
        }
        return instance;
    }

    public void release() {
       /* if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }*/
        mBlacklist.clear();
        instance = null;
    }

    public void addData(String deviceAddr, int seq) {
//        addData(getBlackFlag(deviceAddr, seq));
        BlackFlagInfo info = new BlackFlagInfo(deviceAddr, seq);
        if (!mBlacklist.contains(info)) {
            mBlacklist.add(info);
        }

        if (mBlacklist.size() >= 200) { //上限是200个
            mBlacklist.remove(0);
        }
    }

    public BlackFlagInfo getInfo(String deviceAddr, int seq) {
        BlackFlagInfo info = new BlackFlagInfo(deviceAddr, seq);
        if (mBlacklist.indexOf(info) != -1) {
            return mBlacklist.get(mBlacklist.indexOf(info));
        }
        return null;
    }


    public void resetRepeatTime(String deviceAddr) {
        for (BlackFlagInfo info : mBlacklist) {
          if(info.getAddress().equals(deviceAddr)){
              info.setRepeatTime(0);
          }
        }
    }

//    public void addData(String item) {
//        if (item != null && !mBlacklist.contains(item)) {
//            mBlacklist.add(item);
////            startTimeTask();
//            if (mBlacklist.size() >= 200) { //上限是200个
//                mBlacklist.remove(0);
//            }
//        }
//    }


    public boolean isContains(String deviceAddr, int seq) {
        return mBlacklist.contains(new BlackFlagInfo(deviceAddr, seq));
    }

    public boolean isContains(String value) {
//        boolean ret = false;
//        if (value != null && mBlacklist.size() > 0) {
//            ret = mBlacklist.contains(value);
//        }
//
        if(value == null) return false;
        String[] data = value.split("_");
        return isContains(data[0], Integer.parseInt(data[1]));
    }

    public static String getBlackFlag(String deviceAddr, int seq) {
        if (BluetoothAdapter.checkBluetoothAddress(deviceAddr) && seq >= 0 && seq <= 255) {
            return deviceAddr + "_" + seq;
        } else {
            return null;
        }
    }

    /*private void startTimeTask() {
        if (mBlacklist.size() > 0) {
            mHandler.removeMessages(MSG_CHECK_BLACKlIST);
            mHandler.sendEmptyMessageDelayed(MSG_CHECK_BLACKlIST, TIMEOUT);
        }
    }*/
}
