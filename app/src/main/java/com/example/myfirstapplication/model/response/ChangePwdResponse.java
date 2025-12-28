package com.example.myfirstapplication.model.response;

/**
 * 修改密码响应（完全对齐后端ChangePwdResponse）
 */
public class ChangePwdResponse {
    private int code;
    private String message;

    // ========== Getter/Setter ==========
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}