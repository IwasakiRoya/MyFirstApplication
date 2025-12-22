package com.example.myfirstapplication.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages") // Room数据库表名
public class ChatMessage {
    // 主键（自动生成）
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String friendId;  // 所属会话的好友ID
    public String content;   // 消息内容
    public int type;         // 消息类型：发送/接收
    public long timestamp;   // 时间戳（消息发送/接收时间）
    public int status;       // 消息状态（用int类型，适配Room和UI）

    // 消息类型常量（供Adapter和Service使用）
    public final static int TYPE_SENT = 1;       // 我方发送的消息
    public final static int TYPE_RECEIVED = 0;   // 对方接收的消息

    // 消息状态常量（替代原来的ChatStatus内部类，更适配Room）
    public final static int STATUS_SUCCESS = 0;   // 成功
    public final static int STATUS_THINKING = 1;  // AI思考中
    public final static int STATUS_FAILED = 2;    // 失败

    // 无参构造方法（Room必须）
    public ChatMessage() {}

    // 有参构造方法（适配Service中的saveToDb调用）
    public ChatMessage(String friendId, String content, int type, int status) {
        this.friendId = friendId;
        this.content = content;
        this.type = type;
        this.status = status;
        this.timestamp = System.currentTimeMillis(); // 自动生成时间戳
    }

    // ========== Getter/Setter（Room必须，适配数据读写） ==========
    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}