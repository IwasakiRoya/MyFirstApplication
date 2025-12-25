package com.example.myfirstapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 聊天消息实体（完全对齐后端ChatMessage）
 */

@Entity(tableName = "messages") // 匹配后端数据库表名
public class ChatMessage {
    // 主键（自动生成，匹配后端Integer类型）
    @PrimaryKey(autoGenerate = true)
    private Integer id;          // 消息ID（后端：Integer → 前端用Integer）

    private String friendId;     // 好友ID（后端：friend_id）
    private String content;      // 消息内容（后端：content）
    private Integer type;        // 消息类型：0=接收，1=发送（后端：type）
    private Long timestamp;      // 时间戳（后端：timestamp）
    private Integer status;      // 消息状态：0=成功，1=思考中，2=失败（后端：status）
    private String userId;       // 发送者ID（后端新增字段：user_id）

    // 消息类型常量（完全匹配后端）
    public static final int TYPE_SENT = 1;       // 我方发送
    public static final int TYPE_RECEIVED = 0;   // 对方接收

    // 消息状态常量（完全匹配后端）
    public static final int STATUS_SUCCESS = 0;   // 成功
    public static final int STATUS_THINKING = 1;  // AI思考中
    public static final int STATUS_FAILED = 2;    // 失败

    // Room必需的无参构造
    public ChatMessage() {}

    // 业务构造方法（添加@Ignore避免Room报错）
    @Ignore
    public ChatMessage(String friendId, String content, Integer type, Integer status, String userId) {
        this.friendId = friendId;
        this.content = content;
        this.type = type;
        this.status = status;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
    }

    // ========== 全字段Getter/Setter（必须和后端字段名一致） ==========
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}