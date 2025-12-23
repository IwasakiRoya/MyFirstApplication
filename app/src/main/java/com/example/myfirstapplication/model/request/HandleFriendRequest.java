package com.example.myfirstapplication.model.request;

import lombok.Data;

@Data
public class HandleFriendRequest {
    private long requestId;       // 后端请求ID
    private int status;           // 1-通过 2-拒绝

    public HandleFriendRequest(long requestId, int status) {
        this.requestId = requestId;
        this.status = status;
    }
}