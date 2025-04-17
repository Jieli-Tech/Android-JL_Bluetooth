package com.jieli.btsmart.tool.upgrade;

import com.jieli.bluetooth.bean.base.BaseError;

/**
 * 升级流程的回调
 *
 * @author zqjasonZhong
 * @date 2018/12/24
 */
public interface IUpgradeCallback {


    /**
     * OTA开始
     */
    void onStartOTA();

    /**
     * 进度回调
     *
     * @param type 类型 <p>参数 : 0 - 数据校验, 1 - 数据传输</p>
     * @param progress 进度
     */
    void onProgress(int type, float progress);

    /**
     *  OTA结束
     */
    void onStopOTA();

    /**
     * OTA取消
     */
    void onCancelOTA();

    /**
     * OTA失败
     *
     * @param error 错误信息
     */
    void onError(BaseError error);
}
