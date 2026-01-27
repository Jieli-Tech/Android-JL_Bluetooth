package com.jieli.btsmart.tool.room.entity;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/9 16:53
 * @desc :
 */
public class UserWithRadioList {
    @Embedded
    public UserEntity userEntity;
    @Relation(
            parentColumn = "userId",
            entityColumn = "radioInfoId",
            associateBy = @Junction(NetRadioCollectAndUserEntity.class)
    )
    public List<NetRadioInfoEntity> radioInfoEntities;
}
