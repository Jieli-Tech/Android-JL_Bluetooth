package com.jieli.btsmart.data.model.bluetooth;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/9/2 9:57 AM
 * @desc :
 */
public class DefaultAlarmBell {
    private String name;
    private int index;
    private boolean selected;


    public DefaultAlarmBell() {
    }

    public DefaultAlarmBell(int index, String name, boolean selected) {
        this.name = name;
        this.index = index;
        this.selected = selected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return "DefaultAlarmBell{" +
                "name='" + name + '\'' +
                ", index=" + index +
                ", selected=" + selected +
                '}';
    }
}
