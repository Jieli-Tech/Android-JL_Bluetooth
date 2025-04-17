package com.jieli.btsmart.data.model.music;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/6/24 17:14
 * @desc :网络电台数据
 */
@Entity()
public class NetRadioInfoData {
    @PrimaryKey
    public int id;
    public String uri;
    public String name;
    public String province;
    public String city;
}
