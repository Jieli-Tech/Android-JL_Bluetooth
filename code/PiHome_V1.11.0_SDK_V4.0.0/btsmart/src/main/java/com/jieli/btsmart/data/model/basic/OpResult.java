package com.jieli.btsmart.data.model.basic;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 操作结果
 * @since 2023/8/23
 */
public class OpResult<T> {
    private T data;
    private int code = -1;
    private String message;

    public T getData() {
        return data;
    }

    public OpResult<T> setData(T data) {
        this.data = data;
        return this;
    }

    public int getCode() {
        return code;
    }

    public OpResult<T> setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public OpResult<T> setMessage(String message) {
        this.message = message;
        return this;
    }

    public boolean isSuccess() {
        return code == 0;
    }

    @Override
    public String toString() {
        return "OpResult{" +
                "data=" + data +
                ", code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
