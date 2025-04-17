package com.jieli.btsmart.data.model.basic;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 功能结果
 * @since 2023/12/6
 */
public class FunctionResult<T> extends OpResult<T> {

    private int function;

    public int getFunction() {
        return function;
    }

    public FunctionResult<T> setFunction(int function) {
        this.function = function;
        return this;
    }
}


