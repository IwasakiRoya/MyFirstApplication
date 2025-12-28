package com.example.myfirstapplication.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

/**
 * 好友请求实体（完全对齐后端FriendRequestEntity）
 */
@Entity(tableName = "friend_requests") // 匹配后端表名
public class FriendRequestEntity {
    @PrimaryKey
    @NonNull
    public String requestId;     // 请求ID（后端：request_id → String/UUID）
    @NonNull
    public String fromUserId;    // 发起者ID（后端：from_user_id）
    @NonNull
    public String toUserId;      // 接收者ID（后端：to_user_id）
    public String requestMsg;    // 请求备注（后端：request_msg）
    public Integer status;       // 状态：0=未处理，1=同意，2=拒绝（后端：status）
    public Date createTime;      // 发起时间（后端：create_time → Date类型）
    public Date handleTime;      // 处理时间（后端：handle_time → Date类型）

    // 状态常量（完全匹配后端）
    public static final int STATUS_UNHANDLED = 0;
    public static final int STATUS_AGREE = 1;
    public static final int STATUS_REJECT = 2;

    // Room必需的无参构造
    public FriendRequestEntity() {}

    // 业务构造方法
    @Ignore
    public FriendRequestEntity(@NonNull String fromUserId, @NonNull String toUserId, String requestMsg) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.requestMsg = requestMsg;
        this.status = STATUS_UNHANDLED;
        this.createTime = new Date();
    }

    // ========== Getter/Setter ==========
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @NonNull
    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(@NonNull String fromUserId) {
        this.fromUserId = fromUserId;
    }

    @NonNull
    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(@NonNull String toUserId) {
        this.toUserId = toUserId;
    }

    public String getRequestMsg() {
        return requestMsg;
    }

    public void setRequestMsg(String requestMsg) {
        this.requestMsg = requestMsg;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(Date handleTime) {
        this.handleTime = handleTime;
    }
}