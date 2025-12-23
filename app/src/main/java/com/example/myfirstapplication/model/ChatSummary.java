package com.example.myfirstapplication.model;

import lombok.Data;

@Data
public class ChatSummary {
    private String name;
    private String lastMessage;
    private String time;
    private int avatarResId;
    private String avatarUrl;
    private String friendId; // 新增：好友ID
    private boolean isFriendRequest; // 新增：是否是好友请求项

    // 构造方法
    public ChatSummary(String name, String lastMessage, String time, int avatarResId) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.avatarResId = avatarResId;
        this.isFriendRequest = false;
    }

    public ChatSummary(String name, String lastMessage, String time, String avatarUrl) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.avatarUrl = avatarUrl;
        this.isFriendRequest = false;
    }
}