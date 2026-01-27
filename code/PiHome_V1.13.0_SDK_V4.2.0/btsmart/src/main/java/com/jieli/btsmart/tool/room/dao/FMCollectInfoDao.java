package com.jieli.btsmart.tool.room.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.jieli.btsmart.tool.room.entity.FMCollectInfoEntity;

import java.util.List;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/1 15:46
 * @desc :
 */
@Dao
public interface FMCollectInfoDao {
    @Query("SELECT * FROM FMCollectInfoEntity WHERE btDeviceAddress = :address ORDER BY freq ASC")
    LiveData<List<FMCollectInfoEntity>> getAll(String address);

    @Query("SELECT * FROM FMCollectInfoEntity WHERE btDeviceAddress LIKE :address AND mode LIKE :mode AND freq LIKE :freq")
    List<FMCollectInfoEntity> getByAddressAndModeAnd(String address, int mode, int freq);

    @Insert
    void insert(FMCollectInfoEntity entity);

    @Delete
    void delete(FMCollectInfoEntity entity);
}
