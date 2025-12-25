package com.example.myfirstapplication.model.request;

/**
 * 发送好友请求（完全对齐后端FriendRequest）
 */
public class FriendRequest {
    private String fromUserId;
    private String toUserId;
    private String requestMsg;

    // 无参构造
    public FriendRequest() {}

    // 业务构造方法
    public FriendRequest(String fromUserId, String toUserId, String requestMsg) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.requestMsg = requestMsg;
    }

    // ========== Getter/Setter ==========
    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public String getRequestMsg() {
        return requestMsg;
    }

    public void setRequestMsg(String requestMsg) {
        this.requestMsg = requestMsg;
    }
}