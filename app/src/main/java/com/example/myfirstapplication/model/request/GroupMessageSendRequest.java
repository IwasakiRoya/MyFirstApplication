package com.example.myfirstapplication.model.request;

import java.io.Serializable;

/**
 * 发送群消息请求（前端提交给后端）
 */
public class GroupMessageSendRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String groupId; // 群组ID（必填）
    private String messageContent; // 消息内容（必填）
    private Integer messageType = 0; // 消息类型（默认文本）

    // 空构造函数
    public GroupMessageSendRequest() {}

    // Getter & Setter 方法（全量生成）
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getMessageContent() { return messageContent; }
    public void setMessageContent(String messageContent) { this.messageContent = messageContent; }
    public Integer getMessageType() { return messageType; }
    public void setMessageType(Integer messageType) { this.messageType = messageType; }
}