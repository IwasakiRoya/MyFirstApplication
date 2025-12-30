package com.example.myfirstapplication.model.response;

import java.io.Serializable;

/**
 * 群消息返回响应（后端返回给前端）
 */
public class GroupMessageResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String messageId; // 消息唯一ID
    private String groupId; // 群组ID
    private String senderUserId; // 发送者用户ID
    private String senderNickname; // 发送者昵称
    private String senderAvatar; // 发送者头像
    private String messageContent; // 消息内容
    private Integer messageType; // 消息类型
    private String sendTime; // 格式化后的发送时间
    private Integer isRead; // 是否已读

    // 空构造函数
    public GroupMessageResponse() {}

    // Getter & Setter 方法（全量生成）
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getSenderUserId() { return senderUserId; }
    public void setSenderUserId(String senderUserId) { this.senderUserId = senderUserId; }
    public String getSenderNickname() { return senderNickname; }
    public void setSenderNickname(String senderNickname) { this.senderNickname = senderNickname; }
    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
    public String getMessageContent() { return messageContent; }
    public void setMessageContent(String messageContent) { this.messageContent = messageContent; }
    public Integer getMessageType() { return messageType; }
    public void setMessageType(Integer messageType) { this.messageType = messageType; }
    public String getSendTime() { return sendTime; }
    public void setSendTime(String sendTime) { this.sendTime = sendTime; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
}