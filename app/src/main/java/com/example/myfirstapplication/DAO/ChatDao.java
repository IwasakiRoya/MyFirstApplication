package com.example.myfirstapplication.DAO;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.myfirstapplication.model.ChatMessage;

import java.util.List;

@Dao
public interface ChatDao {
    // 插入消息并返回生成的主键 ID (long)
    @Insert
    long insert(ChatMessage message);

    // 更新指定 ID 消息的内容和状态
    @Query("UPDATE messages SET content = :newContent, status = :newStatus WHERE id = :msgId")
    void updateMessage(long msgId, String newContent, int newStatus);

    // 查询某个好友的聊天记录 (用于列表显示)
    @Query("SELECT * FROM messages WHERE friendId = :fId ORDER BY timestamp ASC")
    LiveData<List<ChatMessage>> getMessagesByFriend(String fId);

    // 根据friendId查询聊天记录（供UI展示）
    @Query("SELECT * FROM messages WHERE friendId = :friendId ORDER BY timestamp ASC")
    List<ChatMessage> getChatMessages(String friendId);
}
