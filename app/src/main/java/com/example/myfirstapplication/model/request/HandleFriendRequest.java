package com.example.myfirstapplication.model.request;

/**
 * 处理好友请求（完全对齐后端HandleFriendRequest）
 */
public class HandleFriendRequest {
    private String requestId;
    private int status; // 1=同意，2=拒绝

    // 无参构造
    public HandleFriendRequest() {}

    // 业务构造方法
    public HandleFriendRequest(String requestId, int status) {
        this.requestId = requestId;
        this.status = status;
    }

    // ========== Getter/Setter ==========
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}