package com.jieli.btsmart.tool.room.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RoomWarnings;
import androidx.room.Transaction;
import androidx.room.Update;

import com.jieli.btsmart.tool.room.NetRadioUpdateSelectData;
import com.jieli.btsmart.tool.room.entity.NetRadioCollectAndUserEntity;
import com.jieli.btsmart.tool.room.entity.NetRadioInfoEntity;
import com.jieli.btsmart.tool.room.entity.NetRadioPlayInfoEntity;
import com.jieli.btsmart.tool.room.entity.UserWithRadioList;

import java.util.List;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/9 16:57
 * @desc :
 */
@Dao
public interface NetRadioInfoDao {
    /**
     * 查询全部用户
     */
    @Transaction
    @Query("SELECT * FROM UserEntity")
    List<UserWithRadioList> getUserWithRadioList();

    /**
     * 查询单个用户（返回结果LiveData）
     */
    @Transaction
    @Query("SELECT * FROM UserEntity WHERE userId =:userId")
    LiveData<List<UserWithRadioList>> getUserWithRadioListByUserId(int userId);

    /**
     * 查询单个用户（返回结果List）
     */
    @Transaction
    @Query("SELECT * FROM UserEntity WHERE userId =:userId")
    List<UserWithRadioList> getUserWithRadiosByUserId(int userId);

    /**
     * 查询电台信息By  radioId
     */
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM NetRadioInfoEntity WHERE radioInfoId =:radioInfoId")
//此处使用SELECT radioInfoId FROM NetRadioInfoEntity WHERE radioInfoId =:radioInfoId，接收值是List<String> getNetRadioInfoByRadioInfoId(String radioInfoId);
    List<NetRadioInfoEntity> getNetRadioInfoByRadioInfoId(String radioInfoId);


    /**
     * 获取网络电台收藏By userId AND radioId
     */
    @Query("SELECT * FROM NetRadioCollectAndUserEntity WHERE userId =:userId AND radioInfoId =:radioInfoId")
    List<NetRadioCollectAndUserEntity> getNetRadioCollectAndUserEntityByUserIdAndRadioInfoId(int userId, String radioInfoId);

    /**
     * 获取user的收藏列表
     */
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT *  FROM (SELECT NetRadioInfoEntity.* , UserEntity.*  FROM NetRadioCollectAndUserEntity LEFT JOIN NetRadioInfoEntity ON NetRadioCollectAndUserEntity.radioInfoId = NetRadioInfoEntity.radioInfoId LEFT JOIN UserEntity ON NetRadioCollectAndUserEntity.userId = UserEntity.userId )a WHERE a.userId=:userId")
    LiveData<List<NetRadioInfoEntity>> getNetRadioInfoByUserId(int userId);

    /**
     * 插入收藏的关联信息
     */
    @Insert()
    void insertCollectAndUser(NetRadioCollectAndUserEntity entity);

    /**
     * 删除收藏的关联信息
     */
    @Delete()
    void deleteCollectByUserAndRadio(NetRadioCollectAndUserEntity entity);

    /**
     * 插入电台收藏信息
     */
    @Insert()
    void insertNetRadioInfo(NetRadioInfoEntity entity);

    /**
     * 查询网络电台播放列表
     */
    @Query("SELECT * FROM NetRadioPlayInfoEntity")
    List<NetRadioPlayInfoEntity> getAllNetRadioPlayInfo();

    /**
     * 查询网络电台当前播放
     */
    @Query("SELECT * FROM NetRadioPlayInfoEntity WHERE selected =:play LIMIT 1")
    NetRadioPlayInfoEntity getCurrentNetRadioPlayInfo(boolean play);

    /**
     * 删除网络电台播放列表
     */
    @Delete(entity = NetRadioPlayInfoEntity.class)
    void deleteNetRadioPlayInfo(List<NetRadioPlayInfoEntity> entitys);

    @Query("DELETE FROM NetRadioPlayInfoEntity")
    void deleteAllNetRadioPlayInfo();

    /**
     * 添加网络电台播放列表
     */
    @Insert(entity = NetRadioPlayInfoEntity.class, onConflict = OnConflictStrategy.REPLACE)
    void insertNetRadioPlayInfos(List<NetRadioPlayInfoEntity> entitys);

    /**
     * 更新网络电台播放列表
     */
    @Update(entity = NetRadioPlayInfoEntity.class)
    void updateNetRadioPlayInfo(List<NetRadioUpdateSelectData> entitys);

    @Query("UPDATE NetRadioPlayInfoEntity set selected = 0")
    void resetNetRadioPlayInfosSelected();

}
