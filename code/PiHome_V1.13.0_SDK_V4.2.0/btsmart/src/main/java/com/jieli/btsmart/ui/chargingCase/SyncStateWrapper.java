package com.jieli.btsmart.ui.chargingCase;

/**
 * SyncStateWrapper
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 同步设备资源状态包装器
 * @since 2025/2/14
 */
class SyncStateWrapper {
    /**
     * 是否正在同步
     */
    private boolean isSyncing = false;
    /**
     * 是否跳过屏幕亮度
     */
    private boolean isSkipBrightness = false;
    /**
     * 是否跳过当前屏幕保护程序
     */
    private boolean isSkipCurrentScreenSaver = false;
    /**
     * 是否跳过当前开机动画
     */
    private boolean isSkipCurrentBootAnim = false;
    /**
     * 是否跳过墙纸
     */
    private boolean isSkipWallpaper = false;

    public boolean isSyncing() {
        return isSyncing;
    }

    public SyncStateWrapper setSyncing(boolean syncing) {
        isSyncing = syncing;
        return this;
    }

    public boolean isSkipBrightness() {
        return isSkipBrightness;
    }

    public SyncStateWrapper setSkipBrightness(boolean skipBrightness) {
        isSkipBrightness = skipBrightness;
        return this;
    }

    public boolean isSkipCurrentScreenSaver() {
        return isSkipCurrentScreenSaver;
    }

    public SyncStateWrapper setSkipCurrentScreenSaver(boolean skipCurrentScreenSaver) {
        isSkipCurrentScreenSaver = skipCurrentScreenSaver;
        return this;
    }

    public boolean isSkipCurrentBootAnim() {
        return isSkipCurrentBootAnim;
    }

    public SyncStateWrapper setSkipCurrentBootAnim(boolean skipCurrentBootAnim) {
        isSkipCurrentBootAnim = skipCurrentBootAnim;
        return this;
    }

    public boolean isSkipWallpaper() {
        return isSkipWallpaper;
    }

    public SyncStateWrapper setSkipWallpaper(boolean skipWallpaper) {
        isSkipWallpaper = skipWallpaper;
        return this;
    }

    public void reset() {
        setSyncing(false)
                .setSkipBrightness(false)
                .setSkipCurrentScreenSaver(false)
                .setSkipBrightness(false)
                .setSkipWallpaper(false);
    }

    @Override
    public String toString() {
        return "SyncStateWrapper{" +
                "isSyncing=" + isSyncing +
                ", isSkipBrightness=" + isSkipBrightness +
                ", isSkipCurrentScreenSaver=" + isSkipCurrentScreenSaver +
                ", isSkipCurrentBootAnim=" + isSkipCurrentBootAnim +
                ", isSkipWallpaper=" + isSkipWallpaper +
                '}';
    }
}
