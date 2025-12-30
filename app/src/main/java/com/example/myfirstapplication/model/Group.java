package com.example.myfirstapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

/**
 * 群组基础实体（关联 t_group 表，添加 Room 注解）
 */
@Entity(tableName = "t_group") // 核心：指定映射的数据库表名
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 群组唯一ID（主键，UUID）
     */
    @PrimaryKey // 核心：标记为 Room 主键
    @NonNull // 非空约束
    private String groupId; // 群组唯一ID

    /**
     * 群组名称
     */
    private String groupName; // 群组名称

    /**
     * 创建者用户ID
     */
    private String creatorUserId; // 创建者用户ID

    /**
     * 群组头像URL
     */
    private String groupAvatar; // 群组头像URL

    /**
     * 群组描述
     */
    private String groupDesc; // 群组描述

    /**
     * 创建时间
     */
    private Date createTime; // 创建时间

    /**
     * 更新时间
     */
    private Date updateTime; // 更新时间

    /**
     * 是否删除（0-未删除，1-已删除）
     */
    private Integer isDelete; // 是否删除

    // ========== Room 必需：无参构造函数（用于数据库实例化） ==========
    public Group() {}

    // ========== 业务构造方法（添加 @Ignore 注解，避免 Room 识别冲突） ==========
    @Ignore
    public Group(@NonNull String groupId, String groupName, String creatorUserId) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.creatorUserId = creatorUserId;
        this.isDelete = 0; // 默认未删除
        this.createTime = new Date();
        this.updateTime = new Date();
    }

    // ========== Getter & Setter（保持原有逻辑，补充非空注解） ==========
    @NonNull
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(@NonNull String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public void setCreatorUserId(String creatorUserId) {
        this.creatorUserId = creatorUserId;
    }

    public String getGroupAvatar() {
        return groupAvatar;
    }

    public void setGroupAvatar(String groupAvatar) {
        this.groupAvatar = groupAvatar;
    }

    public String getGroupDesc() {
        return groupDesc;
    }

    public void setGroupDesc(String groupDesc) {
        this.groupDesc = groupDesc;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getIsDelete() {
        return isDelete;
    }

    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }
}