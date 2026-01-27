package com.jieli.btsmart.ui.widget.visualizer.record.data;

/**
 * State
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 录音状态
 * @since 2025/7/9
 */
public class State {
    /**
     * 空闲状态
     */
    public static final int STATE_IDLE = 0;
    /**
     * 开始状态
     */
    public static final int STATE_START = 1;
    /**
     * 工作状态
     */
    public static final int STATE_WORKING = 2;


    /**
     * 状态
     */
    private final int state;

    public State(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    @Override
    public String toString() {
        return "RecordState{" +
                "state=" + state +
                '}';
    }
}
