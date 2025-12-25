package com.example.myfirstapplication.model.request;

/**
 * 修改密码请求（完全对齐后端ChangePwdRequest）
 */
public class ChangePwdRequest {
    private String userId;
    private String oldPwd;
    private String newPwd;

    // 无参构造（Retrofit必需）
    public ChangePwdRequest() {}

    // 业务构造方法
    public ChangePwdRequest(String userId, String oldPwd, String newPwd) {
        this.userId = userId;
        this.oldPwd = oldPwd;
        this.newPwd = newPwd;
    }

    public ChangePwdRequest(String oldPwd, String newPwd) {
        this.oldPwd = oldPwd;
        this.newPwd = newPwd;
    }

    public

    // ========== Getter/Setter ==========
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOldPwd() {
        return oldPwd;
    }

    public void setOldPwd(String oldPwd) {
        this.oldPwd = oldPwd;
    }

    public String getNewPwd() {
        return newPwd;
    }

    public void setNewPwd(String newPwd) {
        this.newPwd = newPwd;
    }
}