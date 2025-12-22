package com.example.myfirstapplication.POJO;

public class UserResponse {
    public int code;
    public String message;
    public Data data;

    public class Data {
        public String token;
        public String userId;
    }
}
