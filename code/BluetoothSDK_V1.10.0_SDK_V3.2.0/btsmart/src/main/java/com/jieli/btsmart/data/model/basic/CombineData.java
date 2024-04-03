package com.jieli.btsmart.data.model.basic;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 组合数据
 * @since 2023/8/31
 */
public class CombineData<T, R> {
    private final T t;
    private final R r;

    public CombineData(T t, R r) {
        this.t = t;
        this.r = r;
    }

    public T getT() {
        return t;
    }

    public R getR() {
        return r;
    }

    @Override
    public String toString() {
        return "CombineData{" +
                "t=" + t +
                ", r=" + r +
                '}';
    }
}
