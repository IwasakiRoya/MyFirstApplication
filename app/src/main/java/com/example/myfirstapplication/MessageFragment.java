package com.example.myfirstapplication;

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
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.model.ChatSummary;

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
    private boolean isMock = true; // 开发环境开关

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 点击列表项跳转到聊天详情页
        adapter = new MessageAdapter(dataList, chat -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("friendName", chat.getName());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        if (isMock) {
            loadMockData();
        } else {
            loadRealData();
        }

        return view;
    }

    private void loadMockData() {
        dataList.add(new ChatSummary("张三", "晚上打球吗？", "18:05", R.drawable.ic_avatar_1));
        dataList.add(new ChatSummary("李四", "项目文档发我一下", "15:30", R.drawable.ic_avatar_2));
        dataList.add(new ChatSummary("DeepSeek AI", "你好！我是你的智能助手", "10:00", R.drawable.ic_ai_logo));
        adapter.notifyDataSetChanged();
    }

    private void loadRealData() {
        // 这里的 Retrofit 构建可以封装成单例
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        // 这里的 Token 实际开发中从 SharedPreferences 获取
        apiService.getChatList("Bearer your_token_here").enqueue(new Callback<List<ChatSummary>>() {
            @Override
            public void onResponse(Call<List<ChatSummary>> call, Response<List<ChatSummary>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    dataList.clear();
                    dataList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<ChatSummary>> call, Throwable t) {
                Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
            }
        });
    }
}