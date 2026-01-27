package com.jieli.btsmart.data.model.settings;

import androidx.annotation.NonNull;

/**
 * 功能数据
 *
 * @author zqjasonZhong
 * @since 2020/5/16
 */
@SuppressWarnings("UnusedReturnValue")
public class FunctionBean {
    private int funcId;
    private String function;
    private boolean isSelected;
    private boolean isHideLine;

    public FunctionBean() {

    }

    public FunctionBean(int funcId, String function, boolean isSelected) {
        this(funcId, function, isSelected, false);
    }

    public FunctionBean(int funcId, String function, boolean isSelected, boolean isHideLine) {
        setFuncId(funcId);
        setFunction(function);
        setSelected(isSelected);
        setHideLine(isHideLine);
    }

    public int getFuncId() {
        return funcId;
    }

    public FunctionBean setFuncId(int funcId) {
        this.funcId = funcId;
        return this;
    }

    public String getFunction() {
        return function;
    }

    public FunctionBean setFunction(String function) {
        this.function = function;
        return this;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public FunctionBean setSelected(boolean selected) {
        isSelected = selected;
        return this;
    }

    public boolean isHideLine() {
        return isHideLine;
    }

    public FunctionBean setHideLine(boolean hideLine) {
        isHideLine = hideLine;
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return "FunctionBean{" +
                "funcId=" + funcId +
                ", function='" + function + '\'' +
                ", isSelected=" + isSelected +
                ", isHideLine=" + isHideLine +
                '}';
    }
}
