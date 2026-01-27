package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.file.op.CreateFileParam;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.impl.rcsp.file.FileOpImpl;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnTaskStateListener;
import com.jieli.filebrowse.bean.SDCardBean;

import java.io.File;
import java.util.List;

/**
 * FileOpDemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 文件操作示范代码
 * @since 2024/6/21
 */
class FileOpDemo {


    public void createBigFile(File file) {
        //Step1. 创建文件操作实现类
        FileOpImpl fileOp = FileOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用创建大文件的接口
        //file --- 文件
        final List<SDCardBean> onlineStorages = fileOp.getOnlineStorage();
        SDCardBean storage = null;
        if (null != onlineStorages) {
            for (SDCardBean sdCardBean : onlineStorages) {
                if (sdCardBean.isFlash() && sdCardBean.isOnline()) {
                    storage = sdCardBean;
                    break;
                }
            }
        }
        if (null == file || null == storage) return;
        CreateFileParam param = new CreateFileParam(RCSPController.getInstance().getUsingDevice(), file, storage);
        fileOp.createBigFile(param, new OnTaskStateListener() {
            @Override
            public void onStart() {
                //回调大文件传输开始
            }

            @Override
            public void onProgress(int progress) {
                //回调大文件传输进度
            }

            @Override
            public void onStop() {
                //回调大文件传输结束
            }

            @Override
            public void onCancel(int reason) {
                //回调大文件传输被中止
            }

            @Override
            public void onError(int code, String message) {
                //回调大文件传输异常
                //code --- 错误码
                //message --- 错误描述
            }
        });
    }


    public void cancelBigFileTransfer(){
        //Step1. 创建文件操作实现类
        FileOpImpl fileOp = FileOpImpl.instance(RCSPController.getInstance().getRcspOp());
        //Step2. 调用取消大文件传输的接口
        //只有在大文件传输中才会成功，可以用 fileOp.isFileOperating(); 来判断是否在文件操作
        fileOp.cancelBigFileTransfer(RCSPController.getInstance().getUsingDevice(), 0, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功 和 结果
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败 和 错误信息
            }
        });
    }
}
