package com.example.myfirstapplication.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myfirstapplication.Adapter.FriendRequestAdapter;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.database.AppDatabase;
import com.example.myfirstapplication.model.Friend;
import com.example.myfirstapplication.model.FriendRequestEntity;
import com.example.myfirstapplication.model.User;
import com.example.myfirstapplication.model.request.HandleFriendRequest;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.utils.NetworkUtils;

import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendRequestActivity extends AppCompatActivity {
    private RecyclerView rvRequests;
    private FriendRequestAdapter adapter;
    private String myUserId;
    private String token;
    private ApiService apiService;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_request);

        // 1. 初始化基础数据
        SharedPreferences sp = getSharedPreferences("USER_INFO", MODE_PRIVATE);
        myUserId = sp.getString("userId", "1000");
        token = sp.getString("token", "");

        // 2. 校验登录状态
        if (token.isEmpty() || myUserId.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 3. 初始化控件
        rvRequests = findViewById(R.id.rv_requests);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));

        // 4. 初始化数据库和ApiService（复用单例）
        db = AppDatabase.getInstance(this);
        apiService = NetworkUtils.getApiService();

        // 5. 加载好友请求列表
        loadFriendRequests();
    }

    /**
     * 加载本地待处理的好友请求
     */
    private void loadFriendRequests() {
        db.friendRequestDao()
                .getPendingRequests(myUserId)
                .observe(this, new Observer<List<FriendRequestEntity>>() {
                    @Override
                    public void onChanged(List<FriendRequestEntity> requests) {
                        if (requests == null || requests.isEmpty()) {
                            Toast.makeText(FriendRequestActivity.this, "暂无好友请求", Toast.LENGTH_SHORT).show();
                            rvRequests.setAdapter(null); // 清空适配器
                            return;
                        }
                        // 初始化适配器
                        adapter = new FriendRequestAdapter(requests, new FriendRequestAdapter.OnRequestListener() {
                            @Override
                            public void onAccept(FriendRequestEntity request) {
                                handleRequest(request, FriendRequestEntity.STATUS_AGREE); // 对齐后端常量
                            }

                            @Override
                            public void onReject(FriendRequestEntity request) {
                                handleRequest(request, FriendRequestEntity.STATUS_REJECT); // 对齐后端常量
                            }
                        });
                        rvRequests.setAdapter(adapter);
                    }
                });
    }

    private void handleRequest(FriendRequestEntity request, int status) {
        // 1. 构造后端请求体（后端仅需requestId和status）
        HandleFriendRequest handleRequest = new HandleFriendRequest();
        handleRequest.setRequestId(request.getRequestId());
        handleRequest.setStatus(status);

        // 2. 调用后端接口处理好友请求
        apiService.handleFriendRequest("Bearer " + token, handleRequest)
                .enqueue(new Callback<BaseResponse<Void>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<Void> res = response.body();
                            if (res.getCode() == 200) {
                                // 3. 数据库操作统一放子线程
                                new Thread(() -> {
                                    // 3.1 更新好友请求状态
                                    request.setStatus(status);
                                    request.setHandleTime(new Date());
                                    db.friendRequestDao().update(request);

                                    // 3.2 仅同意请求时，添加好友并查询用户信息
                                    if (status == FriendRequestEntity.STATUS_AGREE) {
                                        String fromUserId = request.getFromUserId();
                                        Friend friend = new Friend(myUserId, fromUserId);

                                        // 核心逻辑：先兜底，再异步拉取真实信息
                                        friend.setFriendNickname("好友" + fromUserId); // 兜底昵称
                                        friend.setFriendAvatar(""); // 兜底头像
                                        friend.setAutoReply(false);

                                        // 先插入兜底数据，保证好友列表能显示
                                        db.friendDao().insert(friend);

                                        // 异步调用searchUser接口，拉取真实的昵称/头像并更新
                                        getRemoteUserInfo(fromUserId, friend);
                                    }
                                }).start();

                                // 4. 发送广播刷新好友列表
                                Intent refreshIntent = new Intent("com.example.REFRESH_FRIEND_REQUEST");
                                sendBroadcast(refreshIntent);

                                // 5. 提示用户
                                Toast.makeText(FriendRequestActivity.this,
                                        status == FriendRequestEntity.STATUS_AGREE ? "已通过好友请求" : "已拒绝好友请求",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(FriendRequestActivity.this, "操作失败：" + res.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(FriendRequestActivity.this, "服务器响应异常", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                        Toast.makeText(FriendRequestActivity.this, "操作失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 补充：从后端拉取用户信息（可选，保证数据最新）
     */
    /**
     * 补充：通过searchUser接口（keyword传userId）拉取用户信息
     */
    private void getRemoteUserInfo(String userId, Friend friend) {
        // 核心适配：把userId作为keyword传给searchUser接口
        apiService.searchUser("Bearer " + token, userId)
                .enqueue(new Callback<BaseResponse<User>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<User>> call, Response<BaseResponse<User>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<User> res = response.body();
                            // 校验接口返回码
                            if (res.getCode() == 200) {
                                User user = res.getData();
                                if (user != null) {
                                    // 子线程更新数据库（保证线程安全）
                                    new Thread(() -> {
                                        // 更新Friend表的昵称/头像
                                        friend.setFriendNickname(user.getNickname() != null ? user.getNickname() : "好友" + userId);
                                        friend.setFriendAvatar(user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
                                        db.friendDao().update(friend);

                                        // 同步到User表（可选，如果有User表的话）
                                        // db.userDao().insert(user);
                                    }).start();
                                }
                            } else {
                                // 接口返回失败，仅日志记录，不影响核心流程
                                Log.d("FriendRequest", "查询用户信息失败：" + res.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<User>> call, Throwable t) {
                        // 网络失败不影响核心流程，仅打印日志
                        Log.e("FriendRequest", "查询用户信息网络失败：" + t.getMessage());
                        t.printStackTrace();
                    }
                });
    }
}