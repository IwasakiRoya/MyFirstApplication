package com.example.myfirstapplication.model;

import androidx.annotation.NonNull; // 必须导入这个包
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import lombok.Data;

@Data
@Entity(tableName = "chat_read_position")
public class ChatReadPosition {
    // 核心修复：添加 @NonNull 注解，标记主键非空
    @PrimaryKey
    @NonNull
    public String friendId;       // 好友ID（唯一）
    public int lastReadMsgId;     // 最后阅读的消息ID
    public long lastReadTime;     // 最后阅读时间戳

    // 可选：添加无参构造（Room 推荐，避免lombok生成的构造有问题）
    public ChatReadPosition() {
        this.friendId = ""; // 初始化空字符串，避免null
        this.lastReadMsgId = 0;
        this.lastReadTime = System.currentTimeMillis();
    }

    // 有参构造（方便业务使用）
    @Ignore
    public ChatReadPosition(@NonNull String friendId, int lastReadMsgId, long lastReadTime) {
        this.friendId = friendId;
        this.lastReadMsgId = lastReadMsgId;
        this.lastReadTime = lastReadTime;
    }
}