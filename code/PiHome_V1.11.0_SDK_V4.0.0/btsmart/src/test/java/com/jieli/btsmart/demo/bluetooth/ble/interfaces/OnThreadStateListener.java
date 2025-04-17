package com.jieli.btsmart.demo.bluetooth.ble.interfaces;

/**
 * 线程生命周期监听器
 *
 * @author zqjasonZhong
 * @date 2019/9/20
 */
public interface OnThreadStateListener {

    void onStart(long id, String name);

    void onEnd(long id, String name);
}
