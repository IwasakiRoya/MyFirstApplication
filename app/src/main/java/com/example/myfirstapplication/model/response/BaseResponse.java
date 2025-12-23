package com.example.myfirstapplication.model.response;

import lombok.Data;

@Data
public class BaseResponse<T> {
    private int code;         // 200=成功
    private String msg;       // 提示信息
    private T data;      // 数据体
}

