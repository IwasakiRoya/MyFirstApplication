package com.example.myfirstapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

/**
 * 群消息-用户已读关联实体（关联 t_group_message_read 表，添加 Room 注解）
 */
@Entity(tableName = "t_group_message_read") // 指定映射的数据库表名
public class GroupMessageRead implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 自增主键
     */
    @PrimaryKey(autoGenerate = true) // 核心：自增主键（适配数据库 AUTO_INCREMENT）
    private Long id;

    /**
     * 群消息ID（关联 t_group_message 表）
     */
    @NonNull // 非空约束
    private String groupMessageId;

    /**
     * 群组ID（冗余字段，提升查询效率）
     */
    @NonNull // 非空约束
    private String groupId;

    /**
     * 用户ID（独立的用户标识）
     */
    @NonNull // 非空约束
    private String userId;

    /**
     * 是否已读（0-未读，1-已读，每个用户独立）
     */
    private Integer isRead;

    /**
     * 已读时间（未读时为null）
     */
    private Date readTime;

    // ========== Room 必需：无参构造函数 ==========
    public GroupMessageRead() {}

    // ========== 业务构造方法（添加 @Ignore 注解，避免 Room 识别冲突） ==========
    @Ignore
    public GroupMessageRead(@NonNull String groupMessageId, @NonNull String groupId, @NonNull String userId) {
        this.groupMessageId = groupMessageId;
        this.groupId = groupId;
        this.userId = userId;
        this.isRead = 0; // 默认未读
        this.readTime = null;
    }

    // ========== Getter & Setter ==========
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    public String getGroupMessageId() {
        return groupMessageId;
    }

    public void setGroupMessageId(@NonNull String groupMessageId) {
        this.groupMessageId = groupMessageId;
    }

    @NonNull
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(@NonNull String groupId) {
        this.groupId = groupId;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public Integer getIsRead() {
        return isRead;
    }

    public void setIsRead(Integer isRead) {
        this.isRead = isRead;
    }

    public Date getReadTime() {
        return readTime;
    }

    public void setReadTime(Date readTime) {
        this.readTime = readTime;
    }
}