package com.example.myfirstapplication.model.response;

/**
 * 通用响应体（完全对齐后端BaseResponse）
 */
public class BaseResponse<T> {
    private int code;
    private String message;
    private T data;

    public boolean isSuccess() {
        return code == 200;
    }

    // 静态成功方法
    public static <T> BaseResponse<T> success() {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(200);
        response.setMessage("成功");
        return response;
    }

    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(200);
        response.setMessage("成功");
        response.setData(data);
        return response;
    }

    public static <T> BaseResponse<T> error(String message) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }

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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}