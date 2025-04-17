package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.music.ID3MusicInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;

import org.junit.Test;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc  ID3信息控制测试
 * @since 2021/12/2
 */
public class ID3CtrlDemo {

    @Test
    void controlID3Stream(boolean enable){
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onID3MusicInfo(BluetoothDevice device, ID3MusicInfo id3MusicInfo) {
                //此处将会回调ID3音乐信息
            }
        });
        //enable - true : 开启, false : 关闭
        //执行控制ID3信息流推送功能并等待结果回调
        controller.controlAdvBroadcast(controller.getUsingDevice(), enable, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //enable = true, 开启ID3信息流推送，ID3信息将会在BTRcspEventCallback#onID3MusicInfo回调
                //enable = false, 关闭ID3信息流推送
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    @Test
    void getID3Info(){
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onID3MusicInfo(BluetoothDevice device, ID3MusicInfo id3MusicInfo) {
                //此处将会回调ID3音乐信息
            }
        });
        //执行获取ID3所有信息功能并等待结果回调
        controller.getID3MusicInfo(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onID3MusicInfo回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    @Test
    void id3PlayOrPause(){
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行id3音乐播放或暂停功能并等待结果回调
        controller.iD3MusicPlayOrPause(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    @Test
    void id3PlayPrev(){
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行id3音乐播放上一曲功能并等待结果回调
        controller.iD3MusicPlayPrev(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    @Test
    void id3PlayNext(){
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行id3音乐播放下一曲功能并等待结果回调
        controller.iD3MusicPlayNext(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }
}
