package com.jieli.btsmart.ui.widget.visualizer.record;

import androidx.annotation.NonNull;

import com.jieli.btsmart.ui.widget.visualizer.record.data.State;


/**
 * OnRecordStateCallback
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 录音状态回调
 * @since 2025/7/9
 */
public interface OnRecordStateCallback {

    /**
     * 状态改变回调
     *
     * @param state RecordState 录音状态
     */
    void onChange(@NonNull State state);
}
