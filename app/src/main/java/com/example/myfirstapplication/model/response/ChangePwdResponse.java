package com.example.myfirstapplication.model.response;

import lombok.Data;

@Data
public class ChangePwdResponse {
    private int code;
    private String message;

    // Getter & Setter
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}