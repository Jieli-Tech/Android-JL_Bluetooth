package com.jieli.btsmart.ui.widget.visualizer.record.data;

import java.util.Arrays;

/**
 * WorkingState
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 工作状态
 * @since 2025/7/9
 */
public class WorkingState extends State {
    private final byte[] data;

    public WorkingState(byte[] data) {
        super(STATE_WORKING);
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "WorkingState{" +
                "data=" + Arrays.toString(data) +
                '}';
    }
}
