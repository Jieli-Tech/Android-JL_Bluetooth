package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.RcspAuth;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.BluetoothCallbackImpl;

import org.junit.Test;

import java.util.UUID;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 设备认证示例
 * @since 2022/12/29
 */
public class AuthDemo {
    private final RCSPController controller = RCSPController.getInstance();
    private RcspAuth mRcspAuth;
    private final boolean isDeviceAuth = true; //是否需要设备认证
    private boolean isAuthing;                //是否进行设备认证

    /**
     * 初始化设备认证
     */
    @Test
    public void initAuth() {
        mRcspAuth = new RcspAuth(new RcspAuth.IRcspAuthOp() {
            @Override
            public boolean sendAuthDataToDevice(BluetoothDevice device, byte[] data) {
                return controller.sendDataToDevice(device, data);
            }
        }, new RcspAuth.OnRcspAuthListener() {
            @Override
            public void onInitResult(boolean result) {
                //回调初始化结果
            }

            @Override
            public void onAuthSuccess(BluetoothDevice device) {
                //设备认证成功
                isAuthing = false;
            }

            @Override
            public void onAuthFailed(BluetoothDevice device, int code, String message) {
                //设备认证失败
                isAuthing = false;
            }
        });
    }

    /**
     * 处理认证数据
     */
    @Test
    public void handleDeviceAuth() {
        if (mRcspAuth == null) return;
        controller.getBtOperation().registerBluetoothCallback(new BluetoothCallbackImpl() {
            @Override
            public void onConnection(BluetoothDevice device, int status) {
                if (status == StateCode.CONNECTION_OK) { //设备连接成功
                    if (isDeviceAuth) { //判断设备是否需要走设备认证
                        isAuthing = mRcspAuth.startAuth(device);
                        if (!isAuthing) {
                            //开启设备认证失败
                            controller.disconnectDevice(device);
                        } else {
                            //开启设备认证成功
                        }
                    }
                    controller.getBtOperation().unregisterBluetoothCallback(this);
                }
            }

            @Override
            public void onBleDataNotification(BluetoothDevice device, UUID serviceUuid, UUID characteristicsUuid, byte[] data) {
                if (isAuthing) {
                    //处理BLE的认证数据
                    mRcspAuth.handleAuthData(device, data);
                }
            }

            @Override
            public void onSppDataNotification(BluetoothDevice device, byte[] data) {
                if (isAuthing) {
                    //处理SPP的认证数据
                    mRcspAuth.handleAuthData(device, data);
                }
            }
        });
    }

    /**
     * 释放认证对象
     */
    @Test
    public void releaseAuth() {
        if (mRcspAuth != null) {
            mRcspAuth.destroy();
            mRcspAuth = null;
        }
    }

}
