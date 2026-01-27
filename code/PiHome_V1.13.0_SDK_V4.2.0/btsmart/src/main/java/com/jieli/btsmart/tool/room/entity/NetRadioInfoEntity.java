package com.jieli.btsmart.tool.room.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/9 16:30
 * @desc :网络电台信息表
 */
@Entity(primaryKeys = {"radioInfoId"})
public class NetRadioInfoEntity {
    @NonNull
    public String radioInfoId;
    public String uuid;
    public String placeId;
    public String name;
    public String icon;
    public String stream;
    public String description;
    public String updateTime;
    public String explain;
    public boolean collectState = false;

//    public String getId() {
//        return radioInfoId;
//    }

    public void setId(String id) {
        this.radioInfoId = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getExplain() {
        return explain;
    }

    public void setExplain(String explain) {
        this.explain = explain;
    }

    public boolean isCollectState() {
        return collectState;
    }

    public void setCollectState(boolean collectState) {
        this.collectState = collectState;
    }

}
