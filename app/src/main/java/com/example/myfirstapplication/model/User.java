package com.example.myfirstapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Data;

@Data
@Entity(tableName = "users")
public class User {
    @PrimaryKey
    @NonNull
    private String userId;       // 唯一标识（通常是后端生成的UUID或手机号）
    private String username;     // 登录账号
    private String password;     // 密码（本地通常不存，或只存加密后的，用于自动登录）

    private long phoneNumber;

    // 基本资料
    private String nickname;     // 昵称
    private String avatarUrl;    // 头像地址（网络URL或本地路径）
    private String signature;    // 个性签名

    // AI 托管核心配置
    private String aiPrompt;     // 设定的人设（例如：你是一个毒舌但心软的朋友）
    private String apiKey;       // API Key（建议与用户绑定，因为每个人的额度或模型不同）
    private String aiModel;      // 使用的模型（如 deepseek-chat, gpt-4o）

    // 状态保持
    private String token;        // 后端返回的鉴权Token（用于自动登录）
    private long lastLoginTime;  // 上次登录时间

    // 构造方法
    public User(@NonNull String userId, String username) {
        this.userId = userId;
        this.username = username;
    }
}
