package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.music.MusicNameInfo;
import com.jieli.bluetooth.bean.device.music.MusicStatusInfo;
import com.jieli.bluetooth.bean.device.music.PlayModeInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 音乐控制测试
 * @since 2021/12/2
 */
public class MusicDemo {

    void getMusicInfo() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {

            @Override
            public void onMusicNameChange(BluetoothDevice device, MusicNameInfo nameInfo) {
                //此处将回调音乐名信息
            }

            @Override
            public void onMusicStatusChange(BluetoothDevice device, MusicStatusInfo statusInfo) {
                //此处将回调音乐播放状态
            }

            @Override
            public void onPlayModeChange(BluetoothDevice device, PlayModeInfo playModeInfo) {
                //此处将回调播放模式信息
            }
        });
        //执行获取设备音乐信息功能并等待结果回调
        controller.getDeviceMusicInfo(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onMusicNameChange、
                // BTRcspEventCallback#onMusicStatusChange、BTRcspEventCallback#onPlayModeChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void getMusicStatus() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {

            @Override
            public void onMusicStatusChange(BluetoothDevice device, MusicStatusInfo statusInfo) {
                //此处将回调音乐播放状态
            }
        });
        //执行获取设备音乐播放状态功能并等待结果回调
        controller.getDeviceMusicStatusInfo(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onMusicStatusChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void getPlayFileFormat() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {

            @Override
            public void onFileFormatChange(BluetoothDevice device, String fileFormat) {
                //此处将回调支持播放文件格式
            }
        });
        //执行获取支持播放格式功能并等待结果回调
        controller.getPlayFileFormat(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onFileFormatChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void musicPlayOrPause() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行音乐播放或暂停功能并等待结果回调
        controller.musicPlayOrPause(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
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


    void musicPlayPrev() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行播放上一曲功能并等待结果回调
        controller.musicPlayPrev(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
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


    void musicPlayNext() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行播放下一曲功能并等待结果回调
        controller.musicPlayNext(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
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


    void musicNextPlayMode() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行切换下一个播放模式功能并等待结果回调
        controller.musicSwitchNextPlayMode(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
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


    void musicSeekToProgress(int orientation, short time) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //orientation ： Constants.SEEK_ORIENTATION_FORWARD - 往前，Constants.SEEK_ORIENTATION_BACK - 往后
        //time ： 时间戳
        //执行跳转到指定时间戳功能并等待结果回调
        controller.musicSeekToProgress(controller.getUsingDevice(), orientation, time, new OnRcspActionCallback<Boolean>() {
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
