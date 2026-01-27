package com.jieli.btsmart.data.model.basic;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 状态结果
 * @since 2023/8/23
 */
public class StateResult<T> extends OpResult<T> {
    public static final int STATE_IDLE = 0;
    public static final int STATE_WORKING = 1;
    public static final int STATE_FINISH = 2;
    private int state;
    private int progress;

    public StateResult() {
        this(0);
    }

    public StateResult(int op) {
        super(op);
    }

    public int getState() {
        return state;
    }

    public StateResult<T> setState(int state) {
        this.state = state;
        return this;
    }

    public int getProgress() {
        return progress;
    }

    public StateResult<T> setProgress(int progress) {
        this.progress = progress;
        return this;
    }

    @Override
    public StateResult<T> setData(T data) {
        return (StateResult<T>) super.setData(data);
    }

    @Override
    public StateResult<T> setCode(int code) {
        return (StateResult<T>) super.setCode(code);
    }

    @Override
    public StateResult<T> setMessage(String message) {
        return (StateResult<T>) super.setMessage(message);
    }

    @Override
    public String toString() {
        return "StateResult{" +
                "state=" + state +
                ", progress=" + progress +
                ", data=" + getData() +
                ", code=" + getCode() +
                ", message='" + getMessage() + '\'' +
                "} ";
    }
}
