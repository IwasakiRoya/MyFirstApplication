package com.example.myfirstapplication.Fragment;

import android.content.Intent;
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
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageFragment extends Fragment {
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<ChatSummary> dataList = new ArrayList<>();
    private boolean isMock = true;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);

        apiService = NetworkUtils.getApiService();

        // 初始化RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // 关键：关闭嵌套滑动优化（防止点击无响应）
        recyclerView.setNestedScrollingEnabled(false);

        // ========== 修复：简化Adapter初始化，加固跳转逻辑 ==========
        adapter = new MessageAdapter(dataList, new MessageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ChatSummary chat) {
                // 1. 校验上下文和chat对象（防止空指针）
                if (getActivity() == null || chat == null) {
                    Toast.makeText(getContext(), "跳转失败：上下文为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 2. 校验friendId（必须有值才能跳转）
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
                // 加固：添加FLAG_ACTIVITY_NEW_TASK（防止上下文异常）
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        // 加载数据
        loadChatData();

        return view;
    }

    private void loadChatData() {
        dataList.clear();
        if (isMock) {
            loadMockData();
        } else {
            loadRecentChats();
        }
    }

    // ========== 修复：确保mock数据的friendId有值 ==========
    private void loadMockData() {
        // 方式1：使用带avatarUrl的构造（如果ChatSummary有这个构造）
        ChatSummary summary1 = new ChatSummary("张三", "晚上打球吗？", "18:05", R.drawable.ic_avatar_1);
        summary1.setFriendId("1001"); // 必须设置非空的friendId
        summary1.setAvatarUrl(""); // 空URL不影响，兜底显示本地资源

        ChatSummary summary2 = new ChatSummary("李四", "项目文档发我一下", "15:30", R.drawable.ic_avatar_2);
        summary2.setFriendId("1002");
        summary2.setAvatarUrl("");

        ChatSummary summary3 = new ChatSummary("DeepSeek AI", "你好！我是你的智能助手", "10:00", R.drawable.ic_ai_logo);
        summary3.setFriendId("ai_001");
        summary3.setAvatarUrl("");

        dataList.add(summary1);
        dataList.add(summary2);
        dataList.add(summary3);
        // 关键：使用notifyDataSetChanged刷新（确保数据生效）
        adapter.notifyDataSetChanged();
        Toast.makeText(getContext(), "加载了" + dataList.size() + "条模拟数据", Toast.LENGTH_SHORT).show();
    }

    // 真实接口方法保留（无需修改）
    private void loadRecentChats() {
        String token = getContext().getSharedPreferences("USER_INFO", 0).getString("token", "");
        if (token.isEmpty()) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getChatList("Bearer " + token).enqueue(new Callback<List<ChatSummary>>() {
            @Override
            public void onResponse(Call<List<ChatSummary>> call, Response<List<ChatSummary>> response) {
                if (response.isSuccessful()) {
                    List<ChatSummary> result = response.body();
                    if (result != null && !result.isEmpty()) {
                        dataList.addAll(result);
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "暂无最近聊天记录", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "加载失败：" + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ChatSummary>> call, Throwable t) {
                Toast.makeText(getContext(), "加载失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}