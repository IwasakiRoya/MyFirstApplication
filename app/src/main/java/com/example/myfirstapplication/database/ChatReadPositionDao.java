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

    // 修复：补充userId参数，匹配查询逻辑
    @Query("SELECT * FROM chat_read_position WHERE friendId = :friendId AND userId = :userId")
    ChatReadPosition getReadPosition(String friendId, String userId);

    // 删除阅读位置
    @Query("DELETE FROM chat_read_position WHERE friendId = :friendId AND userId = :userId")
    void delete(String friendId, String userId);
}