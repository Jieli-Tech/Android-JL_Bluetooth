package com.jieli.btsmart.data.model.basic;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 操作结果
 * @since 2023/8/23
 */
public class OpResult<T> {

    public static final int RES_SUCCESS = 0;
    public static final int RES_FAILURE = 1;

    private final int op;
    private T data;
    private int code = -1;
    private String message;

    public OpResult() {
        this(0);
    }

    public OpResult(int op) {
        this.op = op;
    }

    public int getOp() {
        return op;
    }

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
        return code == RES_SUCCESS;
    }

    @Override
    public String toString() {
        return "OpResult{" +
                "op=" + op +
                ", data=" + data +
                ", code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
