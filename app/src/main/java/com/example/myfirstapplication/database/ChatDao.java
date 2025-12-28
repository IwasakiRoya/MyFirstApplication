package com.example.myfirstapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myfirstapplication.model.ChatMessage;

import java.util.List;

@Dao
public interface ChatDao {
    // 核心优化：OnConflictStrategy.IGNORE，已存在唯一约束的消息直接忽略，避免重复插入
    @Insert(onConflict = OnConflictStrategy.IGNORE)
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

    // 新增：按当前用户+好友ID查询（优化跨用户消息查询，可选）
    @Query("SELECT * FROM messages WHERE (userId = :currentUserId AND friendId = :friendId) OR (userId = :friendId AND friendId = :currentUserId) ORDER BY timestamp ASC")
    List<ChatMessage> getChatMessagesByTwoUsers(String currentUserId, String friendId);

    // 新增：根据唯一约束查询消息是否已存在（避免重复）
    @Query("SELECT * FROM messages WHERE content = :content AND timestamp = :timestamp AND userId = :userId AND friendId = :friendId LIMIT 1")
    ChatMessage getMessageByUniqueKey(String content, Long timestamp, String userId, String friendId);

    // 新增：删除好友所有消息（用于重置，可选）
    @Query("DELETE FROM messages WHERE friendId = :friendId")
    void deleteAllMessagesByFriend(String friendId);
}