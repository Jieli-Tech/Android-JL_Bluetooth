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
    private int state;

    public int getState() {
        return state;
    }

    public StateResult<T> setState(int state) {
        this.state = state;
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
                ", data=" + getData() +
                ", code=" + getCode() +
                ", message='" + getMessage() + '\'' +
                "} ";
    }
}
