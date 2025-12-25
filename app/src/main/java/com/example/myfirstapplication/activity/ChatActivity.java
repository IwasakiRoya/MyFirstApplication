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
import java.util.concurrent.TimeUnit;

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
    // 核心修复1：线程池改为懒加载 + 标记是否已关闭
    private ExecutorService dbExecutor;
    private boolean isExecutorShutdown = false;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // 头像变量
    private String currentUserAvatar;
    private String friendAvatar;

    // 阅读位置相关
    private ChatReadPosition readPosition;
    private LinearLayoutManager layoutManager;

    // 广播接收器
    private BroadcastReceiver chatRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String refreshFriendId = intent.getStringExtra("friendId");
            if (refreshFriendId != null && refreshFriendId.equals(friendId)) {
                // 修复：校验页面是否已销毁
                if (!isFinishing() && !isDestroyed()) {
                    loadChatHistory(false);
                }
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
        friendAvatar = getIntent().getStringExtra("friendAvatar");

        // 兜底头像
        if (friendAvatar == null || friendAvatar.isEmpty()) {
            friendAvatar = "";
        }

        // 核心修复2：初始化线程池（懒加载，避免提前创建）
        dbExecutor = Executors.newSingleThreadExecutor();
        isExecutorShutdown = false;

        // 初始化
        initView();
        initData();
        initListener();

        // 注册广播
        IntentFilter filter = new IntentFilter("com.example.REFRESH_CHAT");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(chatRefreshReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(chatRefreshReceiver, filter);
        }

        // 修复3：校验friendId非空后再加载聊天记录
        if (friendId == null || friendId.isEmpty()) {
            Toast.makeText(this, "好友ID不能为空", Toast.LENGTH_SHORT).show();
            finish(); // 关闭页面，避免后续崩溃
            return;
        }
        loadChatHistory(true);
    }

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
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);

        // 初始化Adapter
        chatAdapter = new ChatAdapter(messageList, currentUserAvatar, friendAvatar);
        rvChat.setAdapter(chatAdapter);

        // 设置重发监听
        chatAdapter.setOnMessageResendListener(this::resendMessage);

        // AI开关隐藏
        switchAi.setVisibility(View.GONE);
    }

    private void resendMessage(ChatMessage msg) {
        if (isExecutorShutdown || dbExecutor == null) return;

        msg.setStatus(ChatMessage.STATUS_THINKING);
        int msgIndex = messageList.indexOf(msg);
        if (msgIndex != -1) {
            chatAdapter.notifyItemChanged(msgIndex);
        }
        sendMessage(msg);
    }

    private void initData() {
        // 获取当前用户信息
        SharedPreferences sp = getSharedPreferences("USER_INFO", MODE_PRIVATE);
        userId = sp.getString("userId", "");
        token = sp.getString("token", "");
        currentUserAvatar = sp.getString("avatarUrl", "");

        // 初始化数据库（后端未实现接口，暂时不初始化ApiService）
        db = AppDatabase.getInstance(this);
        // 核心修复4：后端接口未实现，跳过ApiService初始化
        // apiService = NetworkUtils.getApiService();

        // 获取阅读位置（核心修复：补充userId参数）
        executeInDbExecutor(() -> {
            // 修复：调用getReadPosition时传入两个参数（friendId + userId）
            readPosition = db.chatReadPositionDao().getReadPosition(friendId, userId);
            if (readPosition == null) {
                readPosition = new ChatReadPosition();
                readPosition.setFriendId(friendId);
                readPosition.setUserId(userId); // 补充：给阅读位置实体设置userId
                readPosition.setLastReadMsgId(0);
                readPosition.setLastReadTime(System.currentTimeMillis());

                // 可选：插入新的阅读位置到数据库（保证数据完整性）
                db.chatReadPositionDao().insertOrUpdate(readPosition);
            }
        });
    }

    private void initListener() {
        // 返回按钮
        toolbar.setNavigationOnClickListener(v -> finish());

        // 发送按钮（后端未实现接口，暂时禁用）
        btnSend.setOnClickListener(v -> {
            Toast.makeText(this, "后端接口未实现，暂时无法发送消息", Toast.LENGTH_SHORT).show();
            /*
            String content = etInput.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "消息内容不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            ChatMessage sendMsg = new ChatMessage();
            sendMsg.setFriendId(friendId);
            sendMsg.setContent(content);
            sendMsg.setType(ChatMessage.TYPE_SENT);
            sendMsg.setStatus(ChatMessage.STATUS_SUCCESS);
            sendMsg.setTimestamp(System.currentTimeMillis());

            sendMessage(sendMsg);
            etInput.setText("");
            */
        });

        switchAi.setVisibility(View.GONE);
    }

    // 加载聊天记录（核心修复5：后端未实现接口，仅加载本地数据）
    private void loadChatHistory(boolean isFirstLoad) {
        executeInDbExecutor(() -> {
            // 仅加载本地数据，跳过网络请求
            List<ChatMessage> localMessages = db.chatDao().getChatMessages(friendId);
            mainHandler.post(() -> {
                messageList.clear();
                messageList.addAll(localMessages);
                chatAdapter.notifyDataSetChanged();

                // 定位到最后阅读位置
                if (isFirstLoad && readPosition != null && readPosition.getLastReadMsgId() > 0) {
                    for (int i = 0; i < messageList.size(); i++) {
                        if (messageList.get(i).getId() == readPosition.getLastReadMsgId()) {
                            layoutManager.scrollToPosition(i);
                            break;
                        }
                    }
                } else {
                    rvChat.scrollToPosition(messageList.size() - 1);
                }
            });
        });

        // 注释掉网络请求逻辑（后端未实现）
        /*
        dbExecutor.execute(() -> {
            List<ChatMessage> localMessages = db.chatDao().getChatMessages(friendId);
            long lastTimestamp = 0;
            if (!localMessages.isEmpty()) {
                lastTimestamp = localMessages.get(localMessages.size() - 1).getTimestamp();
            }

            apiService.getChatHistory("Bearer " + token, friendId, lastTimestamp).enqueue(new Callback<BaseResponse<List<ChatMessage>>>() {
                @Override
                public void onResponse(Call<BaseResponse<List<ChatMessage>>> call, Response<BaseResponse<List<ChatMessage>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        BaseResponse<List<ChatMessage>> res = response.body();
                        if (res.getCode() == 200 && res.getData() != null && !res.getData().isEmpty()) {
                            executeInDbExecutor(() -> {
                                for (ChatMessage msg : res.getData()) {
                                    db.chatDao().insert(msg);
                                }
                                refreshChatUI(isFirstLoad);
                            });
                        } else {
                            refreshChatUI(isFirstLoad);
                        }
                    } else {
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
        */
    }

    // 核心修复6：封装线程池执行方法，添加状态校验
    private void executeInDbExecutor(Runnable task) {
        // 校验线程池状态，避免向已关闭的线程池提交任务
        if (isExecutorShutdown || dbExecutor == null || dbExecutor.isShutdown() || dbExecutor.isTerminated()) {
            return;
        }
        try {
            dbExecutor.execute(task);
        } catch (Exception e) {
            e.printStackTrace();
            // 捕获异常，避免崩溃
            Toast.makeText(this, "数据库操作失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 刷新聊天UI（使用封装的执行方法）
    private void refreshChatUI(boolean isFirstLoad) {
        executeInDbExecutor(() -> {
            List<ChatMessage> latestMessages = db.chatDao().getChatMessages(friendId);
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return; // 校验页面状态
                messageList.clear();
                messageList.addAll(latestMessages);
                chatAdapter.notifyDataSetChanged();

                if (isFirstLoad && readPosition != null && readPosition.getLastReadMsgId() > 0) {
                    for (int i = 0; i < messageList.size(); i++) {
                        if (messageList.get(i).getId() == readPosition.getLastReadMsgId()) {
                            layoutManager.scrollToPosition(i);
                            break;
                        }
                    }
                } else {
                    rvChat.scrollToPosition(messageList.size() - 1);
                }
            });
        });
    }

    // 发送消息（后端未实现，暂时注释）
    private void sendMessage(ChatMessage message) {
        /*
        executeInDbExecutor(() -> {
            long msgId = db.chatDao().insert(message);
            message.setId((int) msgId);

            mainHandler.post(() -> {
                messageList.add(message);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                rvChat.scrollToPosition(messageList.size() - 1);
            });

            apiService.sendMessage("Bearer " + token, message).enqueue(new Callback<BaseResponse<ChatMessage>>() {
                @Override
                public void onResponse(Call<BaseResponse<ChatMessage>> call, Response<BaseResponse<ChatMessage>> response) {
                    if (!response.isSuccessful()) {
                        mainHandler.post(() -> {
                            Toast.makeText(ChatActivity.this, "消息发送失败", Toast.LENGTH_SHORT).show();
                            executeInDbExecutor(() -> {
                                db.chatDao().updateMessage(message.getId(), message.getContent(), ChatMessage.STATUS_FAILED);
                            });
                        });
                    }
                }

                @Override
                public void onFailure(Call<BaseResponse<ChatMessage>> call, Throwable t) {
                    mainHandler.post(() -> {
                        Toast.makeText(ChatActivity.this, "网络异常，消息发送失败", Toast.LENGTH_SHORT).show();
                        executeInDbExecutor(() -> {
                            db.chatDao().updateMessage(message.getId(), message.getContent(), ChatMessage.STATUS_FAILED);
                        });
                    });
                }
            });

            updateReadPosition((int) msgId);
        });
        */
    }

    // 更新阅读位置
    private void updateReadPosition(int msgId) {
        executeInDbExecutor(() -> {
            if (readPosition == null) return;
            readPosition.setLastReadMsgId(msgId);
            readPosition.setLastReadTime(System.currentTimeMillis());
            db.chatReadPositionDao().insertOrUpdate(readPosition);

            // 注释掉后端接口调用
            /*
            apiService.updateReadPosition("Bearer " + token, friendId, msgId).enqueue(new Callback<BaseResponse>() {
                @Override
                public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {}

                @Override
                public void onFailure(Call<BaseResponse> call, Throwable t) {}
            });
            */
        });
    }

    // 核心修复7：优化线程池销毁逻辑
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 校验广播接收器是否注册
        try {
            unregisterReceiver(chatRefreshReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 标记线程池已关闭
        isExecutorShutdown = true;
        if (dbExecutor != null && !dbExecutor.isShutdown()) {
            dbExecutor.shutdown(); // 优雅关闭
            try {
                // 等待1秒，让未完成的任务执行完毕
                if (!dbExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    dbExecutor.shutdownNow(); // 强制关闭
                }
            } catch (InterruptedException e) {
                dbExecutor.shutdownNow();
            }
        }
        dbExecutor = null; // 清空引用
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 校验消息列表非空 + 线程池可用
        if (!messageList.isEmpty() && !isExecutorShutdown) {
            int lastMsgId = messageList.get(messageList.size() - 1).getId();
            updateReadPosition(lastMsgId);
        }
    }
}