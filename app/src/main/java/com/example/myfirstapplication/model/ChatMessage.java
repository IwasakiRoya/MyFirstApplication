package com.example.myfirstapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

/**
 * 聊天消息实体（完全对齐后端原始数据，添加联合唯一约束避免重复，补充 equals/hashCode 确保实例可追踪）
 */
@Entity(
        tableName = "messages",
        // 核心：联合唯一约束，基于「内容+时间戳+发送者ID+好友ID」避免重复消息
        indices = {
                @androidx.room.Index(
                        value = {"content", "timestamp", "userId", "friendId"},
                        unique = true
                )
        }
)
public class ChatMessage {
    // 主键（自动生成，匹配后端Integer类型）
    @PrimaryKey(autoGenerate = true)
    public Integer id;          // 消息ID（后端原始字段）

    public String friendId;     // 好友ID（后端原始字段：friend_id）
    public String content;      // 消息内容（后端原始字段：content）
    public Integer type;        // 消息类型（后端原始字段：type，仅作为备份，前端优先基于userId判断）
    public Long timestamp;      // 时间戳（后端原始字段：timestamp）
    public Integer status;      // 消息状态（后端原始字段：status）
    public String userId;       // 发送者ID（后端原始字段：user_id，核心判断依据）

    public Integer getMsgType() {
        return msgType;
    }

    public void setMsgType(Integer msgType) {
        this.msgType = msgType;
    }

    /**
     * 新增：消息内容类型
     * 1 = 文本
     * 2 = 图片
     */
    public Integer msgType;

    // 内容类型常量
    public static final int MSG_TYPE_TEXT = 1;
    public static final int MSG_TYPE_IMAGE = 2;

    // 消息类型常量（与后端对齐，仅用于展示层判断）
    public static final int TYPE_SENT = 1;       // 我方发送
    public static final int TYPE_RECEIVED = 0;   // 对方接收

    // 消息状态常量（与后端对齐）
    public static final int STATUS_SUCCESS = 0;   // 成功
    public static final int STATUS_THINKING = 1;  // 发送中
    public static final int STATUS_FAILED = 2;    // 失败

    // Room必需的无参构造
    public ChatMessage() {}

    // 业务构造方法 (更新)
    @Ignore
    public ChatMessage(String friendId, String content, Integer type, Integer status, String userId, Integer msgType) {
        this.friendId = friendId;
        this.content = content; // 如果是图片，这里存URL
        this.type = type;
        this.status = status;
        this.userId = userId;
        this.msgType = msgType;
        this.timestamp = System.currentTimeMillis();
    }

    // ========== 全字段Getter/Setter（保持与后端原始字段一致，不做修改） ==========
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

    // ========== 核心补充：重写 equals/hashCode，以 id 为唯一标识，确保 indexOf() 可追踪 ==========
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}