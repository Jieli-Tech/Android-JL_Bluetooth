package com.jieli.btsmart.tool.room.entity;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.jieli.btsmart.BR;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/1 13:53
 * @desc :
 */
@Entity
public class FMCollectInfoEntity extends BaseObservable {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String btDeviceAddress;
    public int mode;//FM&AM
    public int freq;
    @Ignore
    @Bindable
    public boolean isPlay;

    public int getId() {
        return id;
    }

    public String getBtDeviceSSid() {
        return btDeviceAddress;
    }

    public void setBtDeviceSSid(String btDeviceSSid) {
        this.btDeviceAddress = btDeviceSSid;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getFreq() {
        return freq;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }

    public boolean isPlay() {
        return isPlay;
    }

    public void setPlay(boolean play) {
        isPlay = play;
        notifyPropertyChanged(BR.isPlay);
    }
}
