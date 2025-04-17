package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.fm.ChannelInfo;
import com.jieli.bluetooth.bean.device.fm.FmStatusInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;

import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc FM收音测试
 * @since 2021/12/2
 */
public class FmDemo {


    void getFMStatus() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {

            @Override
            public void onFmStatusChange(BluetoothDevice device, FmStatusInfo fmStatusInfo) {
                //此处回调FM状态信息
            }
        });
        //执行获取FM状态信息功能并等待结果回调
        controller.getFmInfo(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onFmStatusChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void fmScanForward() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onFmChannelsChange(BluetoothDevice device, List<ChannelInfo> channels) {
                //此处回调已发现的FM频道列表
            }
        });
        //执行向前搜索FM频道功能并等待结果回调
        controller.fmForwardSearchChannels(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onFmChannelsChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void fmScanBackward() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onFmChannelsChange(BluetoothDevice device, List<ChannelInfo> channels) {
                //此处回调已发现的FM频道列表
            }
        });
        //执行向后搜索FM频道功能并等待结果回调
        controller.fmBackwardSearchChannels(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onFmChannelsChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void fmScanAllChannel() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onFmChannelsChange(BluetoothDevice device, List<ChannelInfo> channels) {
                //此处回调已发现的FM频道列表
            }
        });
        //执行搜索FM频道功能并等待结果回调
        controller.fmSearchAllChannels(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onFmChannelsChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void fmScanStop() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行停止搜索FM频道功能并等待结果回调
        controller.fmStopSearch(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
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


    void fmPlayOrPause() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onFmStatusChange(BluetoothDevice device, FmStatusInfo fmStatusInfo) {
                //此处回调FM状态信息
            }
        });
        //执行FM播放或暂停功能并等待结果回调
        controller.fmPlayOrPause(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onFmStatusChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void fmPlayPrevChannel() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onFmStatusChange(BluetoothDevice device, FmStatusInfo fmStatusInfo) {
                //此处回调FM状态信息
            }
        });
        //执行播放上一个频道功能并等待结果回调
        controller.fmPlayPrevChannel(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onFmStatusChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void fmPlayNextChannel() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onFmStatusChange(BluetoothDevice device, FmStatusInfo fmStatusInfo) {
                //此处回调FM状态信息
            }
        });
        //执行播放下一个频道功能并等待结果回调
        controller.fmPlayNextChannel(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onFmStatusChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void fmPlayPrevFrequency() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onFmStatusChange(BluetoothDevice device, FmStatusInfo fmStatusInfo) {
                //此处回调FM状态信息
            }
        });
        //执行播放上一个频点功能并等待结果回调
        controller.fmPlayPrevFrequency(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onFmStatusChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void fmPlayNextFrequency() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onFmStatusChange(BluetoothDevice device, FmStatusInfo fmStatusInfo) {
                //此处回调FM状态信息
            }
        });
        //执行播放下一个频点功能并等待结果回调
        controller.fmPlayNextFrequency(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onFmStatusChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void fmPlaySelectFreq(float freq) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onFmStatusChange(BluetoothDevice device, FmStatusInfo fmStatusInfo) {
                //此处回调FM状态信息
            }
        });
        //执行播放指定的频点功能并等待结果回调
        controller.fmPlaySelectedFrequency(controller.getUsingDevice(), freq, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onFmStatusChange回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }


    void getFmFrequency() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onFrequencyTx(BluetoothDevice device, float frequency) {
                //此处回调发射频点
            }
        });
        //执行获取发射频点功能并等待结果回调
        controller.getFmFrequency(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onFrequencyTx回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }



    void setFmFrequency(float frequency) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //添加蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onFrequencyTx(BluetoothDevice device, float frequency) {
                //此处回调发射频点
            }
        });
        //执行设置指定发送频点并等待结果回调
        controller.setFmFrequency(controller.getUsingDevice(), frequency, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
                //结果将会在BTRcspEventCallback#onFrequencyTx回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

}
