package com.example.myfirstapplication.model.request;

import java.io.Serializable;

/**
 * 群组创建/加入请求（前端提交给后端）
 */
public class GroupAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String groupName; // 群组名称（创建必填）
    private String groupDesc; // 群组描述（可选）
    private String groupAvatar; // 群组头像（可选）
    private String targetGroupId; // 目标群组ID（加入必填）
    private Integer operateType; // 操作类型（0-创建，1-加入）

    // 空构造函数
    public GroupAddRequest() {}

    // Getter & Setter 方法（全量生成）
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getGroupDesc() { return groupDesc; }
    public void setGroupDesc(String groupDesc) { this.groupDesc = groupDesc; }
    public String getGroupAvatar() { return groupAvatar; }
    public void setGroupAvatar(String groupAvatar) { this.groupAvatar = groupAvatar; }
    public String getTargetGroupId() { return targetGroupId; }
    public void setTargetGroupId(String targetGroupId) { this.targetGroupId = targetGroupId; }
    public Integer getOperateType() { return operateType; }
    public void setOperateType(Integer operateType) { this.operateType = operateType; }
}