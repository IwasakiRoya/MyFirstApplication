package com.example.myfirstapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.myfirstapplication.model.ChatMessage;

import java.util.List;

@Dao
public interface ChatDao {
    // 插入消息（返回自增ID）
    @Insert
    long insert(ChatMessage message);

    // 修复：参数类型改为Integer（匹配实体类的id类型）
    @Query("UPDATE messages SET content = :newContent, status = :newStatus WHERE id = :msgId")
    void updateMessage(Integer msgId, String newContent, Integer newStatus);

    // 查询好友聊天记录（LiveData）
    @Query("SELECT * FROM messages WHERE friendId = :fId ORDER BY timestamp ASC")
    LiveData<List<ChatMessage>> getMessagesByFriend(String fId);

    // 查询好友聊天记录（同步）
    @Query("SELECT * FROM messages WHERE friendId = :friendId ORDER BY timestamp ASC")
    List<ChatMessage> getChatMessages(String friendId);
}