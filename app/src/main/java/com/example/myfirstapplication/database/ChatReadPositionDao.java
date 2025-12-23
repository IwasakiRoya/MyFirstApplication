// 补充ChatReadPosition的DAO
package com.example.myfirstapplication.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myfirstapplication.model.ChatReadPosition;

@Dao
public interface ChatReadPositionDao {
    // 插入/更新阅读位置（冲突时替换）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ChatReadPosition position);

    // 查询指定好友的阅读位置
    @Query("SELECT * FROM chat_read_position WHERE friendId = :friendId")
    ChatReadPosition getReadPosition(String friendId);

    // 删除指定好友的阅读位置
    @Query("DELETE FROM chat_read_position WHERE friendId = :friendId")
    void delete(String friendId);
}