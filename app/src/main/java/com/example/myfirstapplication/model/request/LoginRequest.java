package com.example.myfirstapplication.model.request;

/**
 * 登录请求（完全对齐后端LoginRequest）
 */
public class LoginRequest {
    private String username;
    private String password;

    // 无参构造
    public LoginRequest() {}

    // 业务构造方法
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // ========== Getter/Setter ==========
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}