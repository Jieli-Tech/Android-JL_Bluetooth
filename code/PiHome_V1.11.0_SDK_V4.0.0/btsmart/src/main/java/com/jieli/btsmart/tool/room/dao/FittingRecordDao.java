package com.jieli.btsmart.tool.room.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.jieli.btsmart.tool.room.entity.HearingAidFittingRecordEntity;

import java.util.List;

/**
 * @ClassName: FittingRecordDao
 * @Description: 验配记录-Dao
 * @Author: ZhangHuanMing
 * @CreateDate: 2022/6/27 16:40
 */
@Dao
public interface FittingRecordDao {
    /**
     * 查询全部验配记录
     */
    @Transaction
    @Query("SELECT * FROM tb_fitting_record")
    List<HearingAidFittingRecordEntity> getAllFittingRecord();

    /**
     * 查询验配记录
     */
    @Transaction
    @Query("SELECT * FROM tb_fitting_record WHERE recordKey = :keyStr ORDER BY time DESC")
    LiveData<List<HearingAidFittingRecordEntity>> getFittingRecords(String keyStr);

    /**
     * 查找验配记录 by id
     */
    @Transaction
    @Query("SELECT * FROM tb_fitting_record WHERE id = :id ORDER BY time DESC")
    HearingAidFittingRecordEntity getFittingRecord(int id);

    /**
     * 增加验配记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertFittingRecord(HearingAidFittingRecordEntity... entities);

    /**
     * 修改验配记录
     */
    @Update(onConflict = OnConflictStrategy.REPLACE)
    public void updateFittingRecord(HearingAidFittingRecordEntity... entities);

    /**
     * 删除验配记录
     */
    @Delete()
    public void deleteFittingRecord(HearingAidFittingRecordEntity... entities);
}
