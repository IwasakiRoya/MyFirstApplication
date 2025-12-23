package com.example.myfirstapplication.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myfirstapplication.model.User;

@Dao
public interface UserDao {
    // 插入/更新用户信息（冲突时替换）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(User user);

    // 根据用户ID查询
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    User getUserById(String userId);

    // 清空用户表（退出登录）
    @Query("DELETE FROM users")
    void clearUser();
}