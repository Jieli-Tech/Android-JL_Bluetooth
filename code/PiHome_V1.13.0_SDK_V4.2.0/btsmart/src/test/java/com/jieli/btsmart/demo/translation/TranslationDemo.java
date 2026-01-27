package com.jieli.btsmart.demo.translation;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.translation.AudioData;
import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.impl.rcsp.translation.TranslationImpl;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.interfaces.rcsp.translation.TranslationCallback;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TranslationDemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译功能示例代码
 * @since 2025/1/5
 */
public class TranslationDemo {

    private TranslationImpl translationImpl;

    private void init() {
        translationImpl = new TranslationImpl(RCSPController.getInstance().getRcspOp(),
                new TranslationInitDemo.CustomAITranslation());
        boolean isSupportTranslation = translationImpl.isSupportTranslation();
        if(!isSupportTranslation){ //不支持翻译功能
            translationImpl.destroy();
            translationImpl = null;
            return;
        }
        final TranslationCallback callback = new TranslationCallback() {
            @Override
            public void onModeChange(@NonNull BluetoothDevice device, @NonNull TranslationMode mode) {
                //回调翻译模式改变
            }

            @Override
            public void onReceiveAudioData(@NonNull BluetoothDevice device, @NonNull AudioData audioData) {
                //回调接收到的音频数据
            }

            @Override
            public void onError(BluetoothDevice device, int code, String message) {
                //回调错误事件
            }
        };
        translationImpl.addTranslationCallback(callback);
    }

    private void destroy() {
        if (null != translationImpl) {
            translationImpl.destroy();
            translationImpl = null;
        }
    }

    private void enterMode() {
        if (null == translationImpl || !translationImpl.isSupportTranslation()) {
            System.out.println("TranslationImpl is not initialized or the device does not support translation functionality.");
            return;
        }
        //构建翻译模式信息
        TranslationMode mode = new TranslationMode(TranslationMode.MODE_RECORDING_TRANSLATION,
                Constants.AUDIO_TYPE_OPUS);
        //执行进入模式操作
        translationImpl.enterMode(mode, new TranslationCallback() {
            @Override
            public void onModeChange(@NonNull BluetoothDevice device, @NonNull TranslationMode mode) {
                //回调翻译模式改变
            }

            @Override
            public void onReceiveAudioData(@NonNull BluetoothDevice device, @NonNull AudioData audioData) {
                //回调接收到的音频数据
            }

            @Override
            public void onError(BluetoothDevice device, int code, String message) {
                //回调错误事件
                if (code == ErrorCode.SUB_ERR_RESPONSE_BAD_RESULT) { //错误结果
                    if(message == null) return;
                    Pattern pattern = Pattern.compile("result : (\\d+),");
                    Matcher matcher = pattern.matcher(message);
                    if(matcher.find()){
                        String value = matcher.group(1);
                        if(TextUtils.isDigitsOnly(value)){
                            try {
                               int result =  Integer.parseInt(value);
                               switch (result){
                                   case StateCode.RESULT_IN_PROGRESS:
                                       break;
                                   case StateCode.RESULT_INVALID_PARAM:
                                       break;
                                   case StateCode.RESULT_DEVICE_IN_CALL:
                                       break;
                                   case StateCode.RESULT_DEVICE_IN_AUDIO_PLAYING:
                                       break;
                                   case StateCode.RESULT_DEVICE_IS_BUSY:
                                       break;
                                   case StateCode.RESULT_FAILED:
                                       break;
                               }
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
    }

    private void exitMode() {
        if (null == translationImpl || !translationImpl.isSupportTranslation()) {
            System.out.println("TranslationImpl is not initialized or the device does not support translation functionality.");
            return;
        }
        //不处于翻译模式
        if (!translationImpl.isWorking()) {
            System.out.println("Not in translation mode.");
            return;
        }
        //执行退出模式操作
        translationImpl.exitMode(new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                //回调操作成功
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
            }
        });
    }
}
