package com.example.myfirstapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

/**
 * 群消息实体（关联 t_group_message 表，添加 Room 注解）
 */
@Entity(tableName = "t_group_message") // 指定映射的数据库表名
public class GroupMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 消息唯一ID（主键，UUID）
     */
    @PrimaryKey // 核心：标记为 Room 主键
    @NonNull // 非空约束
    private String messageId; // 消息唯一ID

    /**
     * 群组ID（核心字段，区分不同群组）
     */
    @NonNull // 非空约束
    private String groupId; // 群组ID

    /**
     * 发送者用户ID
     */
    @NonNull // 非空约束
    private String senderUserId; // 发送者用户ID

    /**
     * 消息内容
     */
    private String messageContent; // 消息内容

    /**
     * 消息类型（0-文本，1-图片，2-文件）
     */
    private Integer messageType; // 消息类型

    /**
     * 发送时间
     */
    private Date sendTime; // 发送时间

    /**
     * 是否删除（0-未删除，1-已删除）
     */
    private Integer isDelete; // 是否删除

    // ========== 扩展字段（前端本地使用，添加 @Ignore 注解，不持久化到数据库） ==========
    @Ignore
    private String senderNickname; // 发送者昵称（本地使用，不入库）

    @Ignore
    private String senderAvatar; // 发送者头像（本地使用，不入库）

    // ========== Room 必需：无参构造函数 ==========
    public GroupMessage() {}

    // ========== 业务构造方法（添加 @Ignore 注解，避免 Room 识别冲突） ==========
    @Ignore
    public GroupMessage(@NonNull String messageId, @NonNull String groupId, @NonNull String senderUserId) {
        this.messageId = messageId;
        this.groupId = groupId;
        this.senderUserId = senderUserId;
        this.messageType = 0; // 默认文本消息
        this.isDelete = 0; // 默认未删除
        this.sendTime = new Date();
    }

    // ========== Getter & Setter ==========
    @NonNull
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(@NonNull String messageId) {
        this.messageId = messageId;
    }

    @NonNull
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(@NonNull String groupId) {
        this.groupId = groupId;
    }

    @NonNull
    public String getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(@NonNull String senderUserId) {
        this.senderUserId = senderUserId;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

    public Integer getIsDelete() {
        return isDelete;
    }

    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }

    // ========== 扩展字段 Getter & Setter（本地使用，不入库） ==========
    public String getSenderNickname() {
        return senderNickname;
    }

    public void setSenderNickname(String senderNickname) {
        this.senderNickname = senderNickname;
    }

    public String getSenderAvatar() {
        return senderAvatar;
    }

    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }
}