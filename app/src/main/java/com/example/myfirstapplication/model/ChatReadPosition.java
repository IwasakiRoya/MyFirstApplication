package com.example.myfirstapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 聊天阅读位置（完全对齐后端ChatReadPosition）
 */
@Entity(tableName = "chat_read_position") // 匹配后端表名
public class ChatReadPosition {
    @PrimaryKey
    @NonNull
    private String friendId;       // 好友ID（后端：friend_id）

    private Integer lastReadMsgId; // 最后阅读消息ID（后端：last_read_msg_id）
    private Long lastReadTime;     // 最后阅读时间戳（后端：last_read_time）
    private String userId;         // 所属用户ID（后端新增字段：user_id）

    // Room必需的无参构造
    public ChatReadPosition() {
        this.friendId = "";
        this.lastReadMsgId = 0;
        this.lastReadTime = System.currentTimeMillis();
        this.userId = "";
    }

    // 业务构造方法
    @Ignore
    public ChatReadPosition(@NonNull String friendId, String userId, Integer lastReadMsgId, Long lastReadTime) {
        this.friendId = friendId;
        this.userId = userId;
        this.lastReadMsgId = lastReadMsgId;
        this.lastReadTime = lastReadTime;
    }

    // ========== Getter/Setter ==========
    @NonNull
    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(@NonNull String friendId) {
        this.friendId = friendId;
    }

    public Integer getLastReadMsgId() {
        return lastReadMsgId;
    }

    public void setLastReadMsgId(Integer lastReadMsgId) {
        this.lastReadMsgId = lastReadMsgId;
    }

    public Long getLastReadTime() {
        return lastReadTime;
    }

    public void setLastReadTime(Long lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}