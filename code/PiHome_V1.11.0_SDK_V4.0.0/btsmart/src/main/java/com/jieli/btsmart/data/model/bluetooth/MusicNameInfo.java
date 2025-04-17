package com.jieli.btsmart.data.model.bluetooth;

import androidx.annotation.NonNull;

/**
 * 音乐名字信息
 */
public class MusicNameInfo {
    private int cluster;  //簇号 -- 用于定位文件路径
    private String name;  //音乐名

    public MusicNameInfo() {
    }

    public MusicNameInfo(int cluster, String name) {
        setCluster(cluster);
        setName(name);
    }

    public int getCluster() {
        return cluster;
    }

    public void setCluster(int cluster) {
        this.cluster = cluster;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    @NonNull
    @Override
    public String toString() {
        return "MusicNameInfo{" +
                "cluster=" + cluster +
                ", name='" + name + '\'' +
                '}';
    }
}
