package com.example.myfirstapplication.model;
import com.example.myfirstapplication.utils.TimeFormatUtils;
import com.google.gson.annotations.SerializedName; // 导入Gson注解

/**
 * 聊天列表摘要（前端本地使用，需对齐后端返参字段）
 */
public class ChatSummary {
    private String name;
    private String lastMessage;

    // 核心修复：映射后端的 lastMessageTime 字段，接收时间戳Long
    @SerializedName("lastMessageTime")
    private Long lastMessageTime; // 后端返回的时间戳（Long），替代原有的 String time

    private String time; // 格式化后的时间字符串（前端展示用）
    private int avatarResId;
    private String avatarUrl;

    // 核心修复：映射后端的 friendId 字段（确保Gson正确解析）
    @SerializedName("friendId")
    private String friendId;

    private boolean isFriendRequest;

    // 核心修复：映射后端的 unreadCount 字段（Long类型）
    @SerializedName("unreadCount")
    private Long unreadCount; // 先接收后端的Long类型，再转换为int

    // 保留原有构造方法（不变）
    public ChatSummary(String name, String lastMessage, String time, int avatarResId) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.avatarResId = avatarResId;
        this.isFriendRequest = false;
        this.unreadCount = 0L;
    }

    public ChatSummary(String name, String lastMessage, String time, String avatarUrl) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.avatarUrl = avatarUrl;
        this.isFriendRequest = false;
        this.unreadCount = 0L;
    }

    public ChatSummary(String friendId, String name, String lastMessage, Long lastMessageTime, Long unreadCount) {
        this.friendId = friendId;
        this.name = name;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.unreadCount = unreadCount;
        this.isFriendRequest = false;
        // 初始化时直接格式化时间
        this.time = formatLastMessageTime();
    }

    // 新增：格式化 lastMessageTime 为前端展示的 HH:mm 格式
    private String formatLastMessageTime() {
        if (this.lastMessageTime == null || this.lastMessageTime <= 0) {
            return "未知时间";
        }
        return TimeFormatUtils.formatTimestampToHHmm(this.lastMessageTime);
    }

    // ========== Getter/Setter 核心修复（兼容后端Long类型，保留前端int使用） ==========
    public int getUnreadCount() {
        // 后端Long转为前端int，兜底为0（避免null）
        return this.unreadCount != null ? this.unreadCount.intValue() : 0;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = (long) unreadCount;
    }

    public void setUnreadCount(Long unreadCount) {
        this.unreadCount = unreadCount != null ? unreadCount : 0L;
    }

    // 新增：lastMessageTime 的 Getter/Setter
    public Long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
        // 同步更新格式化后的时间
        this.time = formatLastMessageTime();
    }

    // 保留其他原有 Getter/Setter（不变）
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getAvatarResId() {
        return avatarResId;
    }

    public void setAvatarResId(int avatarResId) {
        this.avatarResId = avatarResId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }

    public boolean isFriendRequest() {
        return isFriendRequest;
    }

    public void setFriendRequest(boolean friendRequest) {
        isFriendRequest = friendRequest;
    }
}