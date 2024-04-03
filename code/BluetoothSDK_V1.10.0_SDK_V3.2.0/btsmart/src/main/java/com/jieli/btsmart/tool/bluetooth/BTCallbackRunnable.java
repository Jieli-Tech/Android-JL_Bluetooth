package com.jieli.btsmart.tool.bluetooth;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理蓝牙事件回调
 *
 * @author zqjasonZhong
 * @since 2020/5/13
 */
public class BTCallbackRunnable implements Runnable {
    private final List<BTEventCallback> mCallbackSet;
    private final BtCallback mBtCallback;

    BTCallbackRunnable(List<BTEventCallback> set, BtCallback callback) {
        this.mCallbackSet = set;
        this.mBtCallback = callback;
    }

    @Override
    public void run() {
        if (mBtCallback != null && mCallbackSet != null) {
            for (BTEventCallback callback : new ArrayList<>(mCallbackSet)) {
                if (callback != null) {
                    mBtCallback.onCallback(callback);
                }
            }
        }
    }
}
