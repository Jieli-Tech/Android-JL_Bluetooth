package com.jieli.btsmart.tool.room.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/9 16:42
 * @desc :用户信息表
 */
@Entity
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public int userId;
    public String userName;
    public String password;

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
