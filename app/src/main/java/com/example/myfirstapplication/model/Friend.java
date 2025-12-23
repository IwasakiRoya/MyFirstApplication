package com.example.myfirstapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import lombok.Data;

@Data
@Entity(tableName = "friends",
        primaryKeys = {"myId", "friendId"}) // 复合主键
public class Friend {
    @NonNull
    public String myId;         // 当前登录用户ID
    @NonNull
    public String friendId;     // 好友的用户ID

    public String friendNickname; // 给好友起的备注
    public String friendAvatar;   // 好友头像缓存
    public boolean isAutoReply;   // 关键：是否针对该好友开启了“托管”模式

    public Friend(String myId, String friendId) {
        this.myId = myId;
        this.friendId = friendId;
    }
}
