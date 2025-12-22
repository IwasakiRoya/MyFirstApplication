package com.example.myfirstapplication.network;

import com.example.myfirstapplication.POJO.LoginRequest;
import com.example.myfirstapplication.POJO.UserResponse;
import com.example.myfirstapplication.model.AiRequest;
import com.example.myfirstapplication.model.AiResponse;
import com.example.myfirstapplication.model.ChatSummary;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {
    // 这里的路径对应你后端的 @PostMapping("/api/user/login")
    @POST("api/user/login")
    Call<UserResponse> login(@Body LoginRequest request);

    @POST("api/user/register")
    Call<UserResponse> register(@Body LoginRequest request);

    // 获取聊天列表
    @GET("api/chat/list")
    Call<List<ChatSummary>> getChatList(@Header("Authorization") String token);
    /**
     * {
     *   "code": 200,
     *   "data": [
     *     {"id": "1", "name": "张三", "lastMessage": "下班一起干饭？", "time": "18:30", "avatarUrl": "..." },
     *     {"id": "2", "name": "DeepSeek-AI", "lastMessage": "已为您生成回复建议", "time": "14:20", "avatarUrl": "..." }
     *   ]
     * }
     */


    @POST("v1/chat/completions") // 注意路径，DeepSeek/OpenAI 通常是这个
    Call<AiResponse> getAiResponse(
            @Header("Authorization") String auth, // 动态传入 API Key
            @Body AiRequest request
    );

}
