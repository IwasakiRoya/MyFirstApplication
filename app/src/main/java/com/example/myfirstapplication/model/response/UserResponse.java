package com.example.myfirstapplication.model.response;

import com.example.myfirstapplication.model.User;

/**
 * 用户登录/注册响应（完全对齐后端UserResponse）
 */
public class UserResponse {
    private int code;
    private String message;
    private User data; // 后端直接返回User实体，无需嵌套内部类

    // ========== Getter/Setter ==========
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getData() {
        return data;
    }

    public void setData(User data) {
        this.data = data;
    }
}