package com.jieli.btsmart.data.model.settings;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 功能项
 * @since 2023/12/7
 */
public class FunctionItem<T> {
    private int function;
    private T data;

    public int getFunction() {
        return function;
    }

    public FunctionItem<T> setFunction(int function) {
        this.function = function;
        return this;
    }

    public T getData() {
        return data;
    }

    public FunctionItem<T> setData(T data) {
        this.data = data;
        return this;
    }
}
