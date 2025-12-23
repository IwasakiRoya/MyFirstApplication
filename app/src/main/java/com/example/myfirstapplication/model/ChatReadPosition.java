// 阅读位置实体（Room存储）
package com.example.myfirstapplication.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Data;

@Data
@Entity(tableName = "chat_read_position")
public class ChatReadPosition {
    @PrimaryKey
    public String friendId;       // 好友ID（唯一）
    public int lastReadMsgId;     // 最后阅读的消息ID
    public long lastReadTime;     // 最后阅读时间戳
}
