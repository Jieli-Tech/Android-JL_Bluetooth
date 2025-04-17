package com.jieli.btsmart.tool.room.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.jieli.btsmart.tool.room.entity.UserEntity;

import java.util.List;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/9 17:52
 * @desc :
 */
@Dao
public interface UserDao {
    @Query("SELECT * FROM UserEntity")
    LiveData<List<UserEntity>> getAll();

    @Query("SELECT * FROM UserEntity WHERE userName = :userName")
    List<UserEntity> findUserByUserName(String userName);

    @Insert
    void insert(UserEntity entity);

    @Delete
    void delete(UserEntity entity);

}
