package com.jieli.btsmart.tool.room.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/9 16:41
 * @desc :网络电台收藏表（多对多）
 */
@Entity(primaryKeys = {"userId", "radioInfoId"})
public class NetRadioCollectAndUserEntity {
    public int userId;
    @NonNull
    @ColumnInfo(index = true)
    public String radioInfoId = "";
}
