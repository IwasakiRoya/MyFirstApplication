package com.example.myfirstapplication.model.response;
import java.util.List;

import lombok.Data;

/**
 * 硅基流动API模型列表响应
 */
@Data
public class ApiAiModelResponse {
    private int code;          // 状态码（200=成功）
    private String msg;        // 提示信息
    private List<String> data; // 模型列表（如 ["deepseek-chat", "gpt-4o", "qwen-turbo"]）
}
