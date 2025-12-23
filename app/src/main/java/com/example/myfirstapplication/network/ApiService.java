package com.example.myfirstapplication.network;

import com.example.myfirstapplication.model.ChatMessage;
import com.example.myfirstapplication.model.User;
import com.example.myfirstapplication.model.request.ChangePwdRequest;
import com.example.myfirstapplication.model.request.FriendRequest;
import com.example.myfirstapplication.model.response.ApiAiModelResponse;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.model.request.HandleFriendRequest;
import com.example.myfirstapplication.model.request.LoginRequest;
import com.example.myfirstapplication.model.response.ChangePwdResponse;
import com.example.myfirstapplication.model.response.UserResponse;
import com.example.myfirstapplication.model.request.AiRequest;
import com.example.myfirstapplication.model.response.AiResponse;
import com.example.myfirstapplication.model.ChatSummary;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;
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
    // ========== 新增：拉取未读消息接口 ==========
    /**
     * 从服务器拉取指定时间后的未读消息
     * @param authorization 鉴权Token（Bearer + token）
     * @param lastTimestamp 最后一次同步时间戳（增量拉取）
     * @return 包含未读消息列表的BaseResponse
     */
    @GET("messages/unread") // 替换为你后端实际的接口路径
    Call<BaseResponse<List<ChatMessage>>> getUnreadMessages(
            @Header("Authorization") String authorization,
            @Query("lastTimestamp") long lastTimestamp
    );


    @POST("v1/chat/completions") // 注意路径，DeepSeek/OpenAI 通常是这个
    Call<AiResponse> getAiResponse(
            @Header("Authorization") String auth, // 动态传入 API Key
            @Body AiRequest request
    );

    // ========== 新增好友请求相关接口 ==========
    // 1. 发送好友请求
    @POST("api/friend/request")
    Call<BaseResponse> sendFriendRequest(@Header("Authorization") String token, @Body FriendRequest request);

    // 2. 获取当前用户的好友请求列表
    @GET("api/friend/requests")
    Call<BaseResponse> getFriendRequests(@Header("Authorization") String token);

    // 3. 处理好友请求（通过/拒绝）
    @POST("api/friend/handle")
    Call<BaseResponse> handleFriendRequest(@Header("Authorization") String token, @Body HandleFriendRequest request);

    // 4. 搜索用户（通过手机号/昵称）
    @GET("api/user/search")
    Call<BaseResponse> searchUser(
            @Header("Authorization") String token,  // 请求头：鉴权Token
            @Query("keyword") String keyword        // 查询参数：搜索关键词（注解放在参数前）
    );

    // ========== 新增个人信息相关 ==========
    // 1. 获取用户信息（用于初始化“我”的页面）
    @GET("api/user/info")
    Call<BaseResponse<User>> getUserInfo(@Header("Authorization") String token);

    // 2. 修改个人信息（排除userId）
    @PUT("api/user/info")
    Call<BaseResponse> updateUserInfo(@Header("Authorization") String token, @Body User user);

    // 3. 修改密码
    @POST("api/user/changePwd")
    Call<ChangePwdResponse> changePassword(@Header("Authorization") String token, @Body ChangePwdRequest request);

    // ========== AI托管相关 ==========
    // 4. 根据API Key查询可使用的模型列表（硅基流动）
    @GET("api/ai/models")
    Call<ApiAiModelResponse> getAiModels(@Query("apiKey") String apiKey);

    // 5. 保存AI配置（关联用户）
    @PUT("api/user/aiConfig")
    Call<BaseResponse> saveAiConfig(@Header("Authorization") String token, @Body User user);

    // ========== 账户操作 ==========
    // 6. 退出登录（清理后端Token）
    @POST("api/user/logout")
    Call<BaseResponse> logout(@Header("Authorization") String token);

    // ========== 新增聊天核心接口 ==========
    /**
     * 发送消息到后端
     * @param token 鉴权Token
     * @param message 消息体
     * @return 发送结果
     */
    @POST("api/chat/send")
    Call<BaseResponse<ChatMessage>> sendMessage(
            @Header("Authorization") String token,
            @Body ChatMessage message
    );

    /**
     * 拉取指定好友的聊天记录（增量）
     * @param token 鉴权Token
     * @param friendId 好友ID
     * @param lastTimestamp 本地最后一条消息的时间戳（增量拉取）
     * @return 聊天记录列表
     */
    @GET("api/chat/history")
    Call<BaseResponse<List<ChatMessage>>> getChatHistory(
            @Header("Authorization") String token,
            @Query("friendId") String friendId,
            @Query("lastTimestamp") long lastTimestamp
    );

    /**
     * 更新阅读位置（告知后端已读）
     * @param token 鉴权Token
     * @param friendId 好友ID
     * @param readMsgId 最后阅读的消息ID
     * @return 更新结果
     */
    @POST("api/chat/read")
    Call<BaseResponse> updateReadPosition(
            @Header("Authorization") String token,
            @Query("friendId") String friendId,
            @Query("readMsgId") int readMsgId
    );

}
