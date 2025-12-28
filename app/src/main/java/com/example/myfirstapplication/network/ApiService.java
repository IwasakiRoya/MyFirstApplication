package com.example.myfirstapplication.network;

import com.example.myfirstapplication.model.ChatMessage;
import com.example.myfirstapplication.model.ChatSummary;
import com.example.myfirstapplication.model.Friend;
import com.example.myfirstapplication.model.FriendRequestEntity;
import com.example.myfirstapplication.model.User;
import com.example.myfirstapplication.model.request.ChangePwdRequest;
import com.example.myfirstapplication.model.request.FriendRequest;
import com.example.myfirstapplication.model.request.HandleFriendRequest;
import com.example.myfirstapplication.model.request.LoginRequest;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.model.response.ChangePwdResponse;
import com.example.myfirstapplication.model.response.UserResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

/**
 * API接口（完全对齐后端Controller路径和参数）
 */
public interface ApiService {
    // ========== 用户相关 ==========
    @POST("api/user/login")
    Call<UserResponse> login(@Body LoginRequest request);

    @POST("api/user/register")
    Call<UserResponse> register(@Body LoginRequest request);

    // 问题代码：注解格式虽看似正确，但可能存在 Retrofit 版本兼容问题，或参数解析异常
    @GET("api/user/info")
    Call<BaseResponse<User>> getUserInfo(
            @Header("Authorization") String token,
            @Query("userId") String userId // Retrofit 无法识别该注解参数
    );

    @PUT("api/user/info")
    Call<BaseResponse<Void>> updateUserInfo(
            @Header("Authorization") String token,
            @Body User user
    );

    @POST("api/user/changePwd")
    Call<ChangePwdResponse> changePassword(
            @Header("Authorization") String token,
            @Body ChangePwdRequest request
    );

    @GET("api/user/search")
    Call<BaseResponse<User>> searchUser(
            @Header("Authorization") String token,
            @Query("keyword") String keyword
    );

    @POST("api/user/logout")
    Call<BaseResponse<Void>> logout(@Header("Authorization") String token);

    // ========== 聊天相关 ==========
    @POST("api/chat/send")
    Call<BaseResponse<ChatMessage>> sendMessage(
            @Header("Authorization") String token,
            @Body ChatMessage message
    );

    @GET("api/chat/history")
    Call<BaseResponse<List<ChatMessage>>> getChatHistory(
            @Header("Authorization") String token,
            @Query("friendId") String friendId,
            @Query("lastTimestamp") long lastTimestamp
    );

    @POST("api/chat/read")
    Call<BaseResponse<Void>> updateReadPosition(
            @Header("Authorization") String token,
            @Query("friendId") String friendId,
            @Query("readMsgId") int readMsgId
    );

    @GET("api/chat/messages/unread") // 对齐后端路径：/api/chat/messages/unread
    Call<BaseResponse<List<ChatMessage>>> getUnreadMessages(
            @Header("Authorization") String token,
            @Query("lastTimestamp") long lastTimestamp
    );

    @GET("api/chat/list")
    Call<BaseResponse<List<ChatSummary>>> getChatList(@Header("Authorization") String token);

    // ========== 好友相关 ==========
    @POST("api/friend/request")
    Call<BaseResponse<Void>> sendFriendRequest(
            @Header("Authorization") String token,
            @Body FriendRequest request
    );

    @GET("api/friend/requests")
    Call<BaseResponse<List<FriendRequestEntity>>> getFriendRequests(
            @Header("Authorization") String token
    );

    @POST("api/friend/handle")
    Call<BaseResponse<Void>> handleFriendRequest(
            @Header("Authorization") String token,
            @Body HandleFriendRequest request
    );

    @GET("api/friend/list")
    Call<BaseResponse<List<Friend>>> getFriendList(
            @Header("Authorization") String token
    );

    // ========== AI相关（前端保留，后端暂未实现） ==========
    @POST("v1/chat/completions")
    Call<BaseResponse<String>> getAiResponse(
            @Header("Authorization") String auth,
            @Body String request
    );

    @GET("api/ai/models")
    Call<BaseResponse<List<String>>> getAiModels(@Query("apiKey") String apiKey);

    @PUT("api/user/aiConfig")
    Call<BaseResponse<Void>> saveAiConfig(
            @Header("Authorization") String token,
            @Body User user
    );
}