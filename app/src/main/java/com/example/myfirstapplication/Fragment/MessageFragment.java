package com.example.myfirstapplication.Fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myfirstapplication.Adapter.MessageAdapter;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.activity.ChatActivity;
import com.example.myfirstapplication.model.ChatSummary;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.utils.FriendAddHelper;
import com.example.myfirstapplication.utils.NetworkUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageFragment extends Fragment {
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<ChatSummary> dataList = new ArrayList<>();
    private boolean isMock = false; // 改为加载真实数据
    private ApiService apiService;
    private String myUserId; // 当前登录用户ID（用于添加好友）
    private FloatingActionButton fabAddFriend; // 正确绑定FloatingActionButton

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);

        apiService = NetworkUtils.getApiService();

        // 初始化当前用户ID（与ContactsFragment逻辑一致）
        if (getContext() != null) {
            myUserId = NetworkUtils.getUserIdFromSharedPref(getContext());
        }

        // 初始化RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setNestedScrollingEnabled(false);

        // 初始化Adapter
        adapter = new MessageAdapter(dataList, new MessageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ChatSummary chat) {
                // 1. 校验上下文和chat对象
                if (getActivity() == null || chat == null) {
                    Toast.makeText(getContext(), "跳转失败：上下文为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 2. 校验friendId
                String friendId = chat.getFriendId();
                if (friendId == null || friendId.isEmpty()) {
                    Toast.makeText(getContext(), "好友ID为空，无法跳转", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 3. 执行跳转
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("friendName", chat.getName());
                intent.putExtra("friendId", friendId);
                intent.putExtra("friendAvatar", chat.getAvatarUrl());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        // 加载数据
        loadChatData();

        // ========== 核心修复：正确绑定FAB并设置点击事件 ==========
        fabAddFriend = view.findViewById(R.id.fab_add_friend);
        fabAddFriend.setOnClickListener(v -> {
            // 调用公共工具类，调起添加好友对话框（复用ContactsFragment的正确逻辑）
            if (getContext() != null) {
                FriendAddHelper.showAddFriendDialog(getContext(), myUserId);
            }
        });

        return view;
    }

    // 以下原有方法保持不变（无需修改）
    private void loadChatData() {
        dataList.clear();
        if (isMock) {
            loadMockData();
        } else {
            loadRecentChats();
        }
    }

    private void loadMockData() {
        if (getContext() == null) return;

        ChatSummary summary1 = new ChatSummary("张三", "晚上打球吗？", "18:05", R.drawable.ic_avatar_1);
        summary1.setFriendId("1001");
        summary1.setAvatarUrl("");

        ChatSummary summary2 = new ChatSummary("李四", "项目文档发我一下", "15:30", R.drawable.ic_avatar_2);
        summary2.setFriendId("1002");
        summary2.setAvatarUrl("");

        ChatSummary summary3 = new ChatSummary("DeepSeek AI", "你好！我是你的智能助手", "10:00", R.drawable.ic_ai_logo);
        summary3.setFriendId("ai_001");
        summary3.setAvatarUrl("");

        dataList.add(summary1);
        dataList.add(summary2);
        dataList.add(summary3);
        adapter.notifyDataSetChanged();
        Toast.makeText(getContext(), "加载了" + dataList.size() + "条模拟数据", Toast.LENGTH_SHORT).show();
    }

    private void loadRecentChats() {
        if (getContext() == null) return;

        String token = NetworkUtils.getTokenFromSharedPref(getContext());
        if (token.isEmpty()) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            loadMockData(); // 未登录时加载模拟数据
            return;
        }

        // 适配BaseResponse<List<ChatSummary>>泛型
        apiService.getChatList(token).enqueue(new Callback<BaseResponse<List<ChatSummary>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<ChatSummary>>> call, Response<BaseResponse<List<ChatSummary>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<ChatSummary>> res = response.body();
                    if (res.getCode() == 200) {
                        List<ChatSummary> result = res.getData();
                        if (result != null && !result.isEmpty()) {
                            dataList.addAll(result);
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(getContext(), "暂无最近聊天记录", Toast.LENGTH_SHORT).show();
                            loadMockData(); // 无数据时加载模拟数据
                        }
                    } else {
                        Toast.makeText(getContext(), "加载失败：" + res.getMessage(), Toast.LENGTH_SHORT).show();
                        loadMockData();
                    }
                } else {
                    Toast.makeText(getContext(), "加载失败：" + response.code(), Toast.LENGTH_SHORT).show();
                    loadMockData();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<List<ChatSummary>>> call, Throwable t) {
                Toast.makeText(getContext(), "加载失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                loadMockData(); // 网络失败时加载模拟数据
            }
        });
    }
}