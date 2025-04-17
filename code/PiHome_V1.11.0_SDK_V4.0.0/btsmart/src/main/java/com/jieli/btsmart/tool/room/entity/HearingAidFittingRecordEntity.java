package com.jieli.btsmart.tool.room.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.jieli.btsmart.tool.room.converter.IntArrayConverter;

/**
 * @ClassName: HearingAidFittingRecordEntity
 * @Description: 辅听验配记录
 * @Author: ZhangHuanMing
 * @CreateDate: 2022/6/27 16:28
 */
@Entity(tableName = "tb_fitting_record")
@TypeConverters({IntArrayConverter.class})
public class HearingAidFittingRecordEntity implements Parcelable {
    public HearingAidFittingRecordEntity() {
    }

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String recordKey;//channelsNum+channelsFreqs  根据channelsNum 和 channelsFreqs 过滤
    //记录名
    public String recordName;
    //记录时间
    public long time;
    //版本
    public int version;
    //通道数
    public int channelsNum;
    //增益类型
    public int gainsType;
    //通道频率数组
    public int[] channelsFreqs;
    //左耳通道值数组
    public float[] leftChannelsValues;
    //右耳通道值数组
    public float[] rightChannelsValues;


    protected HearingAidFittingRecordEntity(Parcel in) {
        id = in.readInt();
        recordKey = in.readString();
        recordName = in.readString();
        time = in.readLong();
        channelsNum = in.readInt();
        gainsType = in.readInt();
        channelsFreqs = in.createIntArray();
        leftChannelsValues = in.createFloatArray();
        rightChannelsValues = in.createFloatArray();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(recordKey);
        dest.writeString(recordName);
        dest.writeLong(time);
        dest.writeInt(channelsNum);
        dest.writeInt(gainsType);
        dest.writeIntArray(channelsFreqs);
        dest.writeFloatArray(leftChannelsValues);
        dest.writeFloatArray(rightChannelsValues);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<HearingAidFittingRecordEntity> CREATOR = new Creator<HearingAidFittingRecordEntity>() {
        @Override
        public HearingAidFittingRecordEntity createFromParcel(Parcel in) {
            return new HearingAidFittingRecordEntity(in);
        }

        @Override
        public HearingAidFittingRecordEntity[] newArray(int size) {
            return new HearingAidFittingRecordEntity[size];
        }
    };

    public static String createKey(String macString, int channelsNum) {
        return "mac:" + macString + "channelsNum:" + channelsNum;
    }
}
