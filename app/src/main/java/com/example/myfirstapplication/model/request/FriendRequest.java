package com.example.myfirstapplication.model.request;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Data;

// 好友请求表（本地存储待处理/已处理的请求）
@Entity(tableName = "friend_requests")
public class FriendRequest {
    @PrimaryKey(autoGenerate = true)
    public long requestId;          // 本地请求ID
    @NonNull
    public String fromUserId;       // 请求发起者ID
    @NonNull
    public String toUserId;         // 请求接收者ID
    public String fromUserName;     // 发起者昵称（缓存）
    public String fromUserAvatar;   // 发起者头像（缓存）
    public String requestMsg;       // 验证消息（如“我是XXX”）
    public int status;              // 请求状态：0-待处理 1-已通过 2-已拒绝
    public long createTime;         // 请求创建时间
    public long handleTime;         // 处理时间（通过/拒绝）

    // 构造方法（发起请求用）
    public FriendRequest(@NonNull String fromUserId, @NonNull String toUserId, String requestMsg) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.requestMsg = requestMsg;
        this.status = 0;
        this.createTime = System.currentTimeMillis();
    }

    // 状态常量
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_ACCEPTED = 1;
    public static final int STATUS_REJECTED = 2;
}