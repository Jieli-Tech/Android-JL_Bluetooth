package com.jieli.btsmart.tool.translate;

/**
 * TranslateState
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译状态
 * @since 2025/8/8
 */
public enum TranslateState {
    STATE_IDLE(0),
    STATE_WORKING(1),
    STATE_PAUSE(2);

    private final int value;

    TranslateState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
