package com.example.myfirstapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

import java.io.Serializable;
import java.util.Date;

/**
 * 群成员关联实体（关联 t_group_user 表，添加 Room 注解）
 */
@Entity(
        tableName = "t_group_user", // 指定映射的数据库表名
        primaryKeys = {"groupId", "userId"}, // 复合主键：群组ID+用户ID（唯一标识一条群成员关系）
        foreignKeys = { // 可选：外键约束（关联 t_group 和 用户表，增强数据一致性）
                @ForeignKey(
                        entity = Group.class,
                        parentColumns = "groupId",
                        childColumns = "groupId",
                        onDelete = ForeignKey.CASCADE // 群组删除时，级联删除群成员关系
                )
        }
)
public class GroupUser implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 自增主键（可选，因已设置复合主键，此处仅作为辅助标识）
     */
    // 注意：复合主键已生效，该字段若无需自增可删除，或标记为 @NonNull 并手动赋值
    private Long id; // 自增主键

    /**
     * 群组ID（复合主键之一）
     */
    @NonNull // 非空约束
    private String groupId; // 群组ID

    /**
     * 用户ID（复合主键之一）
     */
    @NonNull // 非空约束
    private String userId; // 用户ID

    /**
     * 加入时间
     */
    private Date joinTime; // 加入时间

    /**
     * 群成员角色（0-普通成员，1-管理员，2-创建者）
     */
    private Integer role; // 群成员角色

    /**
     * 是否退出（0-未退出，1-已退出）
     */
    private Integer isQuit; // 是否退出

    // ========== Room 必需：无参构造函数 ==========
    public GroupUser() {}

    // ========== 业务构造方法（添加 @Ignore 注解，避免 Room 识别冲突） ==========
    @Ignore
    public GroupUser(@NonNull String groupId, @NonNull String userId) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = 0; // 默认普通成员
        this.isQuit = 0; // 默认未退出
        this.joinTime = new Date();
    }

    // ========== Getter & Setter ==========
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Date getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(Date joinTime) {
        this.joinTime = joinTime;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public Integer getIsQuit() {
        return isQuit;
    }

    public void setIsQuit(Integer isQuit) {
        this.isQuit = isQuit;
    }
}