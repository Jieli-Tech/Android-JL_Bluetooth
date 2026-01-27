package com.jieli.btsmart.data.model.light;

import com.google.gson.GsonBuilder;

import java.util.List;

public class ColorCollectList {
    private List<ColorCollect> list;

    public List<ColorCollect> getList() {
        return list;
    }

    public void setList(List<ColorCollect> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }
}
