package com.example.myfirstapplication.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myfirstapplication.Adapter.ChatAdapter;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.database.AppDatabase;
import com.example.myfirstapplication.model.ChatMessage;
import com.example.myfirstapplication.model.ChatReadPosition;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {
    // 控件
    private Toolbar toolbar;
    private RecyclerView rvChat;
    private EditText etInput;
    private Button btnSend;
    private Switch switchAi;

    // 数据
    private String friendId;
    private String friendName;
    private String userId; // 当前登录用户ID
    private String token;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private ApiService apiService;
    private AppDatabase db;
    private ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // ========== 新增：修复未定义的头像变量 ==========
    private String currentUserAvatar; // 当前用户头像URL
    private String friendAvatar;       // 好友头像URL

    // 阅读位置相关
    private ChatReadPosition readPosition;
    private LinearLayoutManager layoutManager;

    // 广播接收器（接收新消息通知）
    private BroadcastReceiver chatRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String refreshFriendId = intent.getStringExtra("friendId");
            if (refreshFriendId != null && refreshFriendId.equals(friendId)) {
                // 刷新当前会话的消息
                loadChatHistory(false);
            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 获取传参
        friendId = getIntent().getStringExtra("friendId");
        friendName = getIntent().getStringExtra("friendName");
        // 可选：从Intent获取好友头像（如果MessageFragment传递了）
        friendAvatar = getIntent().getStringExtra("friendAvatar");

        // 新增：如果网络头像为空，使用本地默认头像（兜底）
        if (friendAvatar == null || friendAvatar.isEmpty()) {
            friendAvatar = ""; // 空字符串让Glide使用默认头像
        }

        // 初始化
        initView();
        initData();
        initListener();

        // 注册广播（修复标记）
        IntentFilter filter = new IntentFilter("com.example.REFRESH_CHAT");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(chatRefreshReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(chatRefreshReceiver, filter);
        }

        // 加载聊天记录并定位到最后阅读位置
        loadChatHistory(true);
    }

    // ChatActivity.java - initView方法
    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        rvChat = findViewById(R.id.rv_chat);
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);
        switchAi = findViewById(R.id.switch_ai);

        // 设置标题
        toolbar.setTitle(friendName);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // 初始化RecyclerView
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // 从底部开始显示
        rvChat.setLayoutManager(layoutManager);

        // ========== 修复：初始化Adapter（兼容无头像场景） ==========
        // 方式1：使用带头像的构造（推荐，后续可从本地/后端获取头像）
        chatAdapter = new ChatAdapter(messageList, currentUserAvatar, friendAvatar);
        // 方式2：如果暂时不需要头像，使用无参构造（兼容原有代码）
        // chatAdapter = new ChatAdapter(messageList);

        rvChat.setAdapter(chatAdapter);

        // 设置重发监听
        chatAdapter.setOnMessageResendListener(this::resendMessage);

        // AI开关隐藏
        switchAi.setVisibility(View.GONE);
    }

    // 新增：重发消息方法
    private void resendMessage(ChatMessage msg) {
        // 更新状态为发送中
        msg.setStatus(ChatMessage.STATUS_THINKING);
        // 修复：防止indexOf返回-1导致崩溃
        int msgIndex = messageList.indexOf(msg);
        if (msgIndex != -1) {
            chatAdapter.notifyItemChanged(msgIndex);
        }

        // 重新发送
        sendMessage(msg);
    }

    // 初始化数据
    private void initData() {
        // 获取当前用户信息
        SharedPreferences sp = getSharedPreferences("USER_INFO", MODE_PRIVATE);
        userId = sp.getString("userId", "");
        token = sp.getString("token", "");
        // ========== 新增：从本地获取当前用户头像 ==========
        currentUserAvatar = sp.getString("avatarUrl", ""); // 假设登录时已保存头像URL

        // 初始化数据库和网络服务
        db = AppDatabase.getInstance(this);
        apiService = NetworkUtils.getApiService();

        // 获取阅读位置
        dbExecutor.execute(() -> {
            readPosition = db.chatReadPositionDao().getReadPosition(friendId);
            if (readPosition == null) {
                readPosition = new ChatReadPosition();
                readPosition.setFriendId(friendId);
                readPosition.setLastReadMsgId(0);
                readPosition.setLastReadTime(System.currentTimeMillis());
            }
        });
    }

    // 初始化监听
    private void initListener() {
        // 返回按钮
        toolbar.setNavigationOnClickListener(v -> finish());

        // 发送按钮
        btnSend.setOnClickListener(v -> {
            String content = etInput.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "消息内容不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 构建发送消息
            ChatMessage sendMsg = new ChatMessage();
            sendMsg.setFriendId(friendId);
            sendMsg.setContent(content);
            sendMsg.setType(ChatMessage.TYPE_SENT);
            sendMsg.setStatus(ChatMessage.STATUS_SUCCESS);
            sendMsg.setTimestamp(System.currentTimeMillis());

            // 本地插入并发送到后端
            sendMessage(sendMsg);
            etInput.setText("");
        });

        // AI开关（暂时隐藏，后续实现）
        switchAi.setVisibility(View.GONE);
    }

    // 加载聊天记录
    private void loadChatHistory(boolean isFirstLoad) {
        dbExecutor.execute(() -> {
            // 1. 获取本地最后一条消息的时间戳（增量拉取）
            List<ChatMessage> localMessages = db.chatDao().getChatMessages(friendId);
            long lastTimestamp = 0;
            if (!localMessages.isEmpty()) {
                lastTimestamp = localMessages.get(localMessages.size() - 1).getTimestamp();
            }

            // 2. 从后端拉取增量消息
            apiService.getChatHistory("Bearer " + token, friendId, lastTimestamp).enqueue(new Callback<BaseResponse<List<ChatMessage>>>() {
                @Override
                public void onResponse(Call<BaseResponse<List<ChatMessage>>> call, Response<BaseResponse<List<ChatMessage>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        BaseResponse<List<ChatMessage>> res = response.body();
                        if (res.getCode() == 200 && res.getData() != null && !res.getData().isEmpty()) {
                            // 3. 插入到本地数据库
                            dbExecutor.execute(() -> {
                                for (ChatMessage msg : res.getData()) {
                                    db.chatDao().insert(msg);
                                }
                                // 4. 刷新UI
                                refreshChatUI(isFirstLoad);
                            });
                        } else {
                            // 无增量数据，直接刷新本地数据
                            refreshChatUI(isFirstLoad);
                        }
                    } else {
                        // 接口失败，加载本地数据
                        refreshChatUI(isFirstLoad);
                    }
                }

                @Override
                public void onFailure(Call<BaseResponse<List<ChatMessage>>> call, Throwable t) {
                    Toast.makeText(ChatActivity.this, "加载聊天记录失败，显示本地数据", Toast.LENGTH_SHORT).show();
                    refreshChatUI(isFirstLoad);
                }
            });
        });
    }

    // 刷新聊天UI
    private void refreshChatUI(boolean isFirstLoad) {
        dbExecutor.execute(() -> {
            // 获取最新本地消息
            List<ChatMessage> latestMessages = db.chatDao().getChatMessages(friendId);
            mainHandler.post(() -> {
                messageList.clear();
                messageList.addAll(latestMessages);
                chatAdapter.notifyDataSetChanged();

                // 定位到最后阅读位置（首次加载）
                if (isFirstLoad && readPosition != null && readPosition.getLastReadMsgId() > 0) {
                    // 找到对应消息的位置
                    for (int i = 0; i < messageList.size(); i++) {
                        if (messageList.get(i).getId() == readPosition.getLastReadMsgId()) {
                            layoutManager.scrollToPosition(i);
                            break;
                        }
                    }
                } else {
                    // 滚动到最底部
                    rvChat.scrollToPosition(messageList.size() - 1);
                }
            });
        });
    }

    // 发送消息
    private void sendMessage(ChatMessage message) {
        // 1. 本地插入消息
        dbExecutor.execute(() -> {
            long msgId = db.chatDao().insert(message);
            message.setId((int) msgId);

            // 2. 刷新UI
            mainHandler.post(() -> {
                messageList.add(message);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                rvChat.scrollToPosition(messageList.size() - 1);
            });

            // 3. 发送到后端
            apiService.sendMessage("Bearer " + token, message).enqueue(new Callback<BaseResponse<ChatMessage>>() {
                @Override
                public void onResponse(Call<BaseResponse<ChatMessage>> call, Response<BaseResponse<ChatMessage>> response) {
                    if (!response.isSuccessful()) {
                        mainHandler.post(() -> {
                            Toast.makeText(ChatActivity.this, "消息发送失败", Toast.LENGTH_SHORT).show();
                            // 更新本地消息状态为失败
                            dbExecutor.execute(() -> {
                                db.chatDao().updateMessage(message.getId(), message.getContent(), ChatMessage.STATUS_FAILED);
                            });
                        });
                    }
                }

                @Override
                public void onFailure(Call<BaseResponse<ChatMessage>> call, Throwable t) {
                    mainHandler.post(() -> {
                        Toast.makeText(ChatActivity.this, "网络异常，消息发送失败", Toast.LENGTH_SHORT).show();
                        // 更新本地消息状态为失败
                        dbExecutor.execute(() -> {
                            db.chatDao().updateMessage(message.getId(), message.getContent(), ChatMessage.STATUS_FAILED);
                        });
                    });
                }
            });

            // 4. 更新阅读位置（发送消息后自动定位到最新）
            updateReadPosition((int) msgId);
        });
    }

    // 更新阅读位置
    private void updateReadPosition(int msgId) {
        dbExecutor.execute(() -> {
            readPosition.setLastReadMsgId(msgId);
            readPosition.setLastReadTime(System.currentTimeMillis());
            db.chatReadPositionDao().insertOrUpdate(readPosition);

            // 通知后端已读
            apiService.updateReadPosition("Bearer " + token, friendId, msgId).enqueue(new Callback<BaseResponse>() {
                @Override
                public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                    // 无需处理，仅告知后端
                }

                @Override
                public void onFailure(Call<BaseResponse> call, Throwable t) {
                    // 忽略失败，本地已记录
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(chatRefreshReceiver);
        dbExecutor.shutdown();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 离开页面时更新阅读位置为最后一条消息
        if (!messageList.isEmpty()) {
            int lastMsgId = messageList.get(messageList.size() - 1).getId();
            updateReadPosition(lastMsgId);
        }
    }
}