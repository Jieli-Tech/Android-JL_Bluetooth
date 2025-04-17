package com.jieli.btsmart.tool.upgrade;

/**
 * 升级管理模块接口
 *
 * @author zqjasonZhong
 * @date 2018/12/24
 */
public interface IUpgradeManager {

    @SuppressWarnings("EmptyMethod")
    void configure();

    void startOTA(IUpgradeCallback callback);

    void cancelOTA();

    void release();
}
