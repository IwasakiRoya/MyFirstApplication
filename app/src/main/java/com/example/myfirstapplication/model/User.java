package com.example.myfirstapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 用户实体（完全对齐后端User）
 */
@Entity(tableName = "users") // 匹配后端表名
public class User {
    @PrimaryKey
    @NonNull
    private String userId;       // 用户ID（后端：user_id）
    private String username;     // 登录账号（后端：username）
    private String password;     // 加密密码（后端：password → 前端本地不存储）
    private Long phoneNumber;    // 手机号（后端：phone_number → Long类型）
    private String nickname;     // 昵称（后端：nickname）
    private String avatarUrl;    // 头像地址（后端：avatar_url）
    private String signature;    // 个性签名（后端：signature）
    private String aiPrompt;     // AI人设（后端：ai_prompt）
    private String apiKey;       // AI接口Key（后端：api_key）
    private String aiModel;      // AI模型（后端：ai_model）
    private String token;        // 登录Token（后端：token）
    private Long lastLoginTime;  // 上次登录时间戳（后端：last_login_time）

    // Room必需的无参构造
    public User() {
        this.userId = "";
    }

    // 业务构造方法
    @Ignore
    public User(@NonNull String userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    // ========== Getter/Setter ==========
    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(Long phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAiPrompt() {
        return aiPrompt;
    }

    public void setAiPrompt(String aiPrompt) {
        this.aiPrompt = aiPrompt;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(Long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
}