package com.example.myfirstapplication.model.response;

import java.io.Serializable;

/**
 * 群组信息返回响应（后端返回给前端）
 */
public class GroupResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String groupId; // 群组唯一ID
    private String groupName; // 群组名称
    private String creatorUserId; // 创建者用户ID
    private String creatorNickname; // 创建者昵称
    private String groupAvatar; // 群组头像
    private String groupDesc; // 群组描述
    private Integer memberCount; // 群成员数量
    private String createTime; // 格式化后的创建时间

    // 空构造函数
    public GroupResponse() {}

    // Getter & Setter 方法（全量生成）
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getCreatorUserId() { return creatorUserId; }
    public void setCreatorUserId(String creatorUserId) { this.creatorUserId = creatorUserId; }
    public String getCreatorNickname() { return creatorNickname; }
    public void setCreatorNickname(String creatorNickname) { this.creatorNickname = creatorNickname; }
    public String getGroupAvatar() { return groupAvatar; }
    public void setGroupAvatar(String groupAvatar) { this.groupAvatar = groupAvatar; }
    public String getGroupDesc() { return groupDesc; }
    public void setGroupDesc(String groupDesc) { this.groupDesc = groupDesc; }
    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}