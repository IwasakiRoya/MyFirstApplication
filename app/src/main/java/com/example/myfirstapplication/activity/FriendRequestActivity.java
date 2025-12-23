package com.example.myfirstapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myfirstapplication.Adapter.FriendRequestAdapter;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.database.AppDatabase;
import com.example.myfirstapplication.model.Friend;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.model.request.FriendRequest;
import com.example.myfirstapplication.model.request.HandleFriendRequest;
import com.example.myfirstapplication.network.ApiService;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FriendRequestActivity extends AppCompatActivity {
    private RecyclerView rvRequests;
    private FriendRequestAdapter adapter;
    private String myUserId;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_request);

        // 初始化
        myUserId = getSharedPreferences("USER_INFO", 0).getString("userId", "1000");
        rvRequests = findViewById(R.id.rv_requests);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));

        // 初始化Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // 加载好友请求列表
        loadFriendRequests();
    }

    private void loadFriendRequests() {
        AppDatabase.getInstance(this).friendRequestDao()
                .getPendingRequests(myUserId)
                .observe(this, new Observer<List<FriendRequest>>() {
                    @Override
                    public void onChanged(List<FriendRequest> requests) {
                        adapter = new FriendRequestAdapter(requests, new FriendRequestAdapter.OnRequestListener() {
                            @Override
                            public void onAccept(FriendRequest request) {
                                handleRequest(request, 1); // 1-通过
                            }

                            @Override
                            public void onReject(FriendRequest request) {
                                handleRequest(request, 2); // 2-拒绝
                            }
                        });
                        rvRequests.setAdapter(adapter);
                    }
                });
    }

    // 处理好友请求
    private void handleRequest(FriendRequest request, int status) {
        // 调用后端接口
        HandleFriendRequest handleRequest = new HandleFriendRequest(request.requestId, status);
        apiService.handleFriendRequest("Bearer " + getToken(), handleRequest)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse res = response.body();
                            if (res.getCode() == 200) {
                                // 更新本地请求状态
                                request.status = status;
                                request.handleTime = System.currentTimeMillis();
                                AppDatabase.getInstance(FriendRequestActivity.this).friendRequestDao().update(request);

                                // 如果是通过，添加到好友列表
                                if (status == 1) {
                                    // 双向添加好友关系
                                    Friend friend1 = new Friend(myUserId, request.fromUserId);
                                    friend1.friendNickname = request.fromUserName;
                                    Friend friend2 = new Friend(request.fromUserId, myUserId);

                                    AppDatabase.getInstance(FriendRequestActivity.this).friendDao().insert(friend1);
                                    // 后端会处理对方的好友关系，本地仅存自己的
                                }

                                Toast.makeText(FriendRequestActivity.this, status == 1 ? "已通过" : "已拒绝", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(FriendRequestActivity.this, res.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        Toast.makeText(FriendRequestActivity.this, "操作失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // 在 FriendRequestActivity 的 handleRequest 方法中，处理成功后添加：
        // 发送广播通知 MessageFragment 刷新列表
        Intent refreshIntent = new Intent("com.example.REFRESH_FRIEND_REQUEST");
        sendBroadcast(refreshIntent);
    }

    private String getToken() {
        return getSharedPreferences("USER_INFO", 0).getString("token", "");
    }
}