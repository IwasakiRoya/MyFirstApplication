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
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MessageFragment extends Fragment {
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<ChatSummary> dataList = new ArrayList<>();
    private boolean isMock = true; // 上线前改为false，关闭模拟数据
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);

        // 优化：复用NetworkUtils中的ApiService（避免重复创建Retrofit）
        apiService = NetworkUtils.getApiService();

        // 初始化UI
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 仅展示最近聊天会话
        // MessageFragment中打开ChatActivity的代码
        adapter = new MessageAdapter(dataList, chat -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("friendName", chat.getName());
            intent.putExtra("friendId", chat.getFriendId());
            // 新增：传递好友头像
            intent.putExtra("friendAvatar", chat.getAvatarUrl()); // 假设ChatSummary有avatarUrl字段
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // 核心修复：加载数据前先清空列表 + 仅首次加载模拟数据
        loadChatData();

        return view;
    }

    // 新增：统一管理数据加载逻辑（区分mock/真实接口）
    private void loadChatData() {
        // 每次加载前先清空列表（彻底避免重复）
        dataList.clear();

        if (isMock) {
            // Mock模式：仅当列表为空时添加（防止重复）
            if (dataList.isEmpty()) {
                loadMockData();
            }
        } else {
            // 真实接口模式：加载后端数据
            loadRecentChats();
        }
    }

    // 加载模拟的最近聊天数据（仅添加一次）
    private void loadMockData() {
        ChatSummary summary1 = new ChatSummary("张三", "晚上打球吗？", "18:05", R.drawable.ic_avatar_1);
        summary1.setFriendId("1001");
        ChatSummary summary2 = new ChatSummary("李四", "项目文档发我一下", "15:30", R.drawable.ic_avatar_2);
        summary2.setFriendId("1002");
        ChatSummary summary3 = new ChatSummary("DeepSeek AI", "你好！我是你的智能助手", "10:00", R.drawable.ic_ai_logo);
        summary3.setFriendId("ai_001");

        dataList.add(summary1);
        dataList.add(summary2);
        dataList.add(summary3);
        adapter.notifyDataSetChanged();
    }

    // 从后端加载最近聊天会话（优化：添加空值/异常处理）
    private void loadRecentChats() {
        String token = getContext().getSharedPreferences("USER_INFO", 0).getString("token", "");
        // 空Token判断（避免无效请求）
        if (token.isEmpty()) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getChatList("Bearer " + token).enqueue(new Callback<List<ChatSummary>>() {
            @Override
            public void onResponse(Call<List<ChatSummary>> call, Response<List<ChatSummary>> response) {
                if (response.isSuccessful()) {
                    List<ChatSummary> result = response.body();
                    // 空数据判断（避免空指针）
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