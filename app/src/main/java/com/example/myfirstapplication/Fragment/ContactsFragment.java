package com.example.myfirstapplication.Fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myfirstapplication.Adapter.FriendAdapter;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.activity.ChatActivity;
import com.example.myfirstapplication.activity.FriendRequestActivity;
import com.example.myfirstapplication.database.AppDatabase;
import com.example.myfirstapplication.model.Friend;
import com.example.myfirstapplication.model.request.FriendRequest;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.utils.NetworkUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ContactsFragment extends Fragment {
    private LinearLayout llFriendRequest; // 好友请求入口
    private LinearLayout llAddFriend;     // 添加好友入口
    private RecyclerView rvFriends;       // 好友列表
    private TextView tvRequestBadge;      // 未读请求红点
    private FloatingActionButton fabAddFriend; // 新增FAB控件
    private FriendAdapter friendAdapter;
    private String myUserId;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        // 初始化当前用户ID
        SharedPreferences sp = getContext().getSharedPreferences("USER_INFO", 0);
        myUserId = sp.getString("userId", "1000");

        // 初始化控件（新增FAB绑定）
        llFriendRequest = view.findViewById(R.id.ll_friend_request);
        llAddFriend = view.findViewById(R.id.ll_add_friend);
        rvFriends = view.findViewById(R.id.rv_friends);
        tvRequestBadge = view.findViewById(R.id.tv_request_badge);
        fabAddFriend = view.findViewById(R.id.fab_add_friend); // 绑定FAB

        // 初始化RecyclerView
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        friendAdapter = new FriendAdapter(friend -> {
            // 点击好友跳转到聊天页
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("friendName", friend.friendNickname);
            intent.putExtra("friendId", friend.friendId);
            startActivity(intent);
        });
        rvFriends.setAdapter(friendAdapter);

        // ========== 核心逻辑 ==========
        // 1. 监听未读好友请求（更新红点）
        listenUnreadFriendRequests();
        // 2. 加载我的好友列表
        loadMyFriends();
        // 3. 同步后端好友请求（保证数据最新）
        syncFriendRequestsFromServer();

        // ========== 点击事件 ==========
        // 好友请求入口 → 跳转到请求处理页
        llFriendRequest.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), FriendRequestActivity.class));
        });

        // 添加好友入口（线性布局）→ 弹出添加好友弹窗
        llAddFriend.setOnClickListener(v -> {
            showAddFriendDialog();
        });

        // 新增：FAB添加好友按钮点击事件（核心！解决点击无反应）
        fabAddFriend.setOnClickListener(v -> {
            showAddFriendDialog(); // 和线性布局入口复用同一套弹窗逻辑
        });

        return view;
    }

    // 监听未读好友请求（更新红点）
    private void listenUnreadFriendRequests() {
        AppDatabase.getInstance(getContext()).friendRequestDao()
                .getPendingRequests(myUserId)
                .observe(getViewLifecycleOwner(), new Observer<List<FriendRequest>>() {
                    @Override
                    public void onChanged(List<FriendRequest> requests) {
                        if (requests.isEmpty()) {
                            tvRequestBadge.setVisibility(View.GONE);
                        } else {
                            tvRequestBadge.setVisibility(View.VISIBLE);
                            tvRequestBadge.setText(String.valueOf(requests.size()));
                        }
                    }
                });
    }

    // 加载我的好友列表
    private void loadMyFriends() {
        AppDatabase.getInstance(getContext()).friendDao()
                .getMyFriends(myUserId)
                .observe(getViewLifecycleOwner(), friends -> {
                    friendAdapter.updateData(friends);
                });
    }

    // 同步后端好友请求到本地（优化：避免重复插入）
    private void syncFriendRequestsFromServer() {
        NetworkUtils.getApiService().getFriendRequests("Bearer " + getToken())
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse res = response.body();
                            if (res.getCode() == 200) {
                                // 解析后端返回的请求列表并同步到本地
                                List<FriendRequest> serverRequests = (List<FriendRequest>) res.getData();
                                if (serverRequests == null || serverRequests.isEmpty()) return;

                                new Thread(() -> {
                                    AppDatabase db = AppDatabase.getInstance(getContext());
                                    for (FriendRequest req : serverRequests) {
                                        // 优化：先查询是否存在，避免重复插入
                                        FriendRequest existing = db.friendRequestDao().getRequestById(req.requestId);
                                        if (existing == null) {
                                            db.friendRequestDao().insert(req);
                                        }
                                    }
                                }).start();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        // 网络失败时使用本地缓存，可选提示用户
                        // Toast.makeText(getContext(), "同步好友请求失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 补全：显示添加好友弹窗（完整实现，包含searchUser接口调用）
    private void showAddFriendDialog() {
        // 1. 加载弹窗布局（dialog_add_friend.xml）
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_friend, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("添加好友")
                .setView(dialogView)
                .setNegativeButton("取消", null);

        // 2. 绑定弹窗控件
        EditText etKeyword = dialogView.findViewById(R.id.et_friend_id); // 搜索关键词输入框
        EditText etRequestMsg = dialogView.findViewById(R.id.et_request_msg); // 验证消息
        AlertDialog dialog = builder.create();

        // 3. 替换弹窗的“发送”按钮为“搜索”（或直接在布局中改）
        dialog.setOnShowListener(dialogInterface -> {
            // 弹窗显示后，修改确认按钮文字为“搜索”
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("搜索");
            // 搜索按钮点击事件
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String keyword = etKeyword.getText().toString().trim();
                if (keyword.isEmpty()) {
                    Toast.makeText(getContext(), "请输入好友ID/昵称/手机号", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 4. 调用searchUser接口搜索用户
                NetworkUtils.searchUser(getContext(), getToken(), keyword, new NetworkUtils.OnSearchResultListener() {
                    @Override
                    public void onResult(BaseResponse response) {
                        if (response.getCode() == 200) {
                            List<Friend> userList = (List<Friend>) response.getData();
                            if (userList.isEmpty()) {
                                Toast.makeText(getContext(), "未找到该用户", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // 5. 显示搜索结果弹窗，让用户选择要添加的好友
                            showSearchResultDialog(userList, etRequestMsg.getText().toString().trim());
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), response.getMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String errorMsg) {
                        Toast.makeText(getContext(), "搜索失败：" + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        // 显示弹窗
        dialog.show();
    }

    // 新增：显示搜索结果弹窗，选择要添加的好友
    private void showSearchResultDialog(List<Friend> userList, String requestMsg) {
        // 提取用户名列表
        String[] names = new String[userList.size()];
        String[] userIds = new String[userList.size()];
        for (int i = 0; i < userList.size(); i++) {
            names[i] = userList.get(i).friendNickname + " (" + userList.get(i).friendId + ")";
            userIds[i] = userList.get(i).friendId;
        }

        // 显示选择弹窗
        new AlertDialog.Builder(getContext())
                .setTitle("选择要添加的好友")
                .setItems(names, (dialog, which) -> {
                    String targetUserId = userIds[which];
                    // 6. 调用发送好友请求接口（需在ApiService中定义）
                    sendFriendRequest(targetUserId, requestMsg);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 新增：发送好友请求
    private void sendFriendRequest(String targetUserId, String requestMsg) {
        // 构建好友请求体
        FriendRequest request = new FriendRequest(myUserId, targetUserId, requestMsg);
        request.setStatus(0); // 0=未处理

        // 调用发送请求接口
        NetworkUtils.getApiService().sendFriendRequest("Bearer " + getToken(), request)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse res = response.body();
                            Toast.makeText(getContext(), res.getMsg(), Toast.LENGTH_SHORT).show();
                            if (res.getCode() == 200) {
                                // 发送成功，同步本地请求列表
                                new Thread(() -> {
                                    AppDatabase.getInstance(getContext()).friendRequestDao().insert(request);
                                }).start();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        Toast.makeText(getContext(), "发送请求失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 获取Token
    private String getToken() {
        return getContext().getSharedPreferences("USER_INFO", 0).getString("token", "");
    }
}