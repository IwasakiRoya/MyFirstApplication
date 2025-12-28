package com.example.myfirstapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

/**
 * 好友实体（完全对齐后端Friend）
 */
@Entity(tableName = "friends", primaryKeys = {"myId", "friendId"}) // 复合主键匹配后端
public class Friend {
    @NonNull
    public String myId;                // 当前用户ID（后端：my_id）
    @NonNull
    public String friendId;            // 好友ID（后端：friend_id）
    public String friendNickname;      // 好友昵称（后端：friend_nickname）
    public String friendAvatar;        // 好友头像（后端：friend_avatar）
    public Boolean isAutoReply;        // 是否自动回复（后端：is_auto_reply → Boolean类型）

    // Room必需的无参构造
    public Friend() {}

    // 业务构造方法
    @Ignore
    public Friend(@NonNull String myId, @NonNull String friendId) {
        this.myId = myId;
        this.friendId = friendId;
        this.isAutoReply = false; // 默认关闭
    }

    // ========== Getter/Setter ==========
    @NonNull
    public String getMyId() {
        return myId;
    }

    public void setMyId(@NonNull String myId) {
        this.myId = myId;
    }

    @NonNull
    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(@NonNull String friendId) {
        this.friendId = friendId;
    }

    public String getFriendNickname() {
        return friendNickname;
    }

    public void setFriendNickname(String friendNickname) {
        this.friendNickname = friendNickname;
    }

    public String getFriendAvatar() {
        return friendAvatar;
    }

    public void setFriendAvatar(String friendAvatar) {
        this.friendAvatar = friendAvatar;
    }

    public Boolean getAutoReply() {
        return isAutoReply;
    }

    public void setAutoReply(Boolean autoReply) {
        isAutoReply = autoReply;
    }
}