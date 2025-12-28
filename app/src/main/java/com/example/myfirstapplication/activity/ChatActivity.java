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
import com.example.myfirstapplication.utils.DbExecutor;
import com.example.myfirstapplication.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView rvChat;
    private EditText etInput;
    private Button btnSend;
    private Switch switchAi;

    private String friendId;
    private String friendName;
    private String userId;
    private String token;

    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private ApiService apiService;
    private AppDatabase db;

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private String currentUserAvatar;
    private String friendAvatar;

    private ChatReadPosition readPosition;
    private LinearLayoutManager layoutManager;

    // ========= 广播 =========
    private final BroadcastReceiver chatRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String refreshFriendId = intent.getStringExtra("friendId");
            if (friendId != null && friendId.equals(refreshFriendId)) {
                loadChatHistory(false);
            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 获取传递的好友参数
        friendId = getIntent().getStringExtra("friendId");
        friendName = getIntent().getStringExtra("friendName");
        friendAvatar = getIntent().getStringExtra("friendAvatar");

        // 校验好友ID有效性
        if (friendId == null || friendId.isEmpty()) {
            Toast.makeText(this, "好友ID不能为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 核心调整：先初始化数据（获取 userId），再初始化视图（初始化适配器）
        initData();
        initView();
        initListener();

        // 后续逻辑不变
        IntentFilter filter = new IntentFilter("com.example.REFRESH_CHAT");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(chatRefreshReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(chatRefreshReceiver, filter);
        }

        // 首次加载历史聊天记录
        loadChatHistory(true);
    }
    /**
     * 初始化视图控件
     */
    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        rvChat = findViewById(R.id.rv_chat);
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);
        switchAi = findViewById(R.id.switch_ai);

        // 设置工具栏标题和返回按钮
        toolbar.setTitle(friendName == null || friendName.isEmpty() ? "聊天窗口" : friendName);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 初始化RecyclerView布局管理器（消息从底部开始排列）
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);

        // 初始化聊天适配器（传递头像+当前用户ID参数，核心修复：解决消息类型判断错误）
        chatAdapter = new ChatAdapter(messageList, currentUserAvatar, friendAvatar, userId);
        rvChat.setAdapter(chatAdapter);

        // 隐藏AI开关（暂未实现）
        switchAi.setVisibility(View.GONE);
    }

    /**
     * 初始化用户数据、数据库、网络接口
     */
    private void initData() {
        // 从SharedPreferences获取当前用户信息
        SharedPreferences sp = getSharedPreferences("USER_INFO", MODE_PRIVATE);
        userId = sp.getString("userId", "");
        token = sp.getString("token", "");
        currentUserAvatar = sp.getString("avatarUrl", "");

        // 核心修复：移除硬编码兜底，强制跳转登录，避免用户归属错误
        if (userId.isEmpty()) {
            mainHandler.post(() -> {
                Toast.makeText(this, "当前用户未登录，请先登录后再进入聊天", Toast.LENGTH_LONG).show();
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivity(loginIntent);
                finish();
            });
            return;
        }

        // 初始化数据库和网络接口
        db = AppDatabase.getInstance(this);
        apiService = NetworkUtils.getApiService();

        // 初始化已读位置（数据库异步操作）
        DbExecutor.execute(() -> {
            readPosition = db.chatReadPositionDao().getReadPosition(friendId, userId);
            if (readPosition == null) {
                readPosition = new ChatReadPosition();
                readPosition.setFriendId(friendId);
                readPosition.setUserId(userId);
                readPosition.setLastReadMsgId(0);
                readPosition.setLastReadTime(System.currentTimeMillis());
                db.chatReadPositionDao().insertOrUpdate(readPosition);
            }
        });
    }

    /**
     * 初始化视图监听器
     */
    private void initListener() {
        // 工具栏返回按钮点击事件（关闭当前页面）
        toolbar.setNavigationOnClickListener(v -> finish());

        // 发送按钮点击事件（发送消息）
        btnSend.setOnClickListener(v -> {
            String content = etInput.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "消息不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 构建待发送的消息对象（正确设置type和status）
            ChatMessage msg = new ChatMessage();
            msg.setFriendId(friendId);
            msg.setUserId(userId); // 确保写入当前登录用户真实ID
            msg.setContent(content);
            msg.setType(ChatMessage.TYPE_SENT); // 我方发送：1
            msg.setStatus(ChatMessage.STATUS_THINKING); // 发送中：1
            msg.setTimestamp(System.currentTimeMillis());

            // 发送消息
            sendMessage(msg);
            // 清空输入框
            etInput.setText("");
        });
    }

    /**
     * 加载聊天历史记录（核心优化：解决对方消息不显示+重复消息+跨用户无数据问题）
     * @param firstLoad 是否为首次加载（首次加载全量数据，非首次加载拉取新消息）
     */
    private void loadChatHistory(boolean firstLoad) {
        // 核心优化1：添加Token校验，避免无权限获取消息
        if (token.isEmpty()) {
            Toast.makeText(this, "Token为空，无法获取聊天历史", Toast.LENGTH_SHORT).show();
            refreshUI(firstLoad);
            return;
        }

        // 核心优化2：修复lastTs逻辑，首次加载强制传0（确保后端返回全量消息），非首次取最新消息的timestamp
        long lastTs = 0;
        if (!firstLoad && !messageList.isEmpty()) {
            ChatMessage latestMsg = messageList.get(messageList.size() - 1);
            lastTs = (latestMsg.getTimestamp() == null) ? 0 : latestMsg.getTimestamp();
        }

        // 调用后端接口获取聊天记录（后端无Bearer前缀，直接传token，无需拼接）
        String authToken = token;
        apiService.getChatHistory(authToken, friendId, lastTs)
                .enqueue(new Callback<BaseResponse<List<ChatMessage>>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<List<ChatMessage>>> call,
                                           Response<BaseResponse<List<ChatMessage>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getCode() == 200) {

                            List<ChatMessage> data = response.body().getData();
                            if (data != null && !data.isEmpty()) {
                                DbExecutor.execute(() -> {
                                    // 核心优化3：遍历消息，完善对方消息判断+唯一性校验，解决不显示+重复问题
                                    for (ChatMessage m : data) {
                                        // 步骤1：非空校验，避免空指针（补充timestamp非空兜底，后端可能返回null）
                                        if (m.getContent() == null || m.getUserId() == null) {
                                            continue;
                                        }
                                        if (m.getTimestamp() == null) {
                                            m.setTimestamp(System.currentTimeMillis());
                                        }
                                        if (m.getFriendId() == null || m.getFriendId().isEmpty()) {
                                            m.setFriendId(friendId); // 兜底设置friendId，确保查询有效
                                        }

                                        // 步骤2：唯一性校验，已存在的消息直接跳过，避免重复（依赖ChatDao的getMessageByUniqueKey）
                                        ChatMessage existingMsg = db.chatDao().getMessageByUniqueKey(
                                                m.getContent(), m.getTimestamp(), m.getUserId(), m.getFriendId()
                                        );
                                        if (existingMsg != null) {
                                            continue;
                                        }

                                        // 步骤3：核心修复：强制按userId区分消息类型，忽略后端无效type字段
                                        boolean isCurrentUserMsg = userId.equals(m.getUserId());
                                        // 添加日志，验证匹配结果
                                        android.util.Log.d("ChatActivityDebug", "当前登录userId: " + userId + ", 消息userId: " + m.getUserId() + ", 匹配结果：" + isCurrentUserMsg);

                                        if (isCurrentUserMsg) {
                                            m.setType(ChatMessage.TYPE_SENT); // 我方消息：1
                                            m.setStatus(ChatMessage.STATUS_SUCCESS); // 我方消息默认成功，避免转圈
                                        } else {
                                            m.setType(ChatMessage.TYPE_RECEIVED); // 对方消息（用户2）：0
                                            m.setStatus(ChatMessage.STATUS_SUCCESS); // 对方消息直接成功，隐藏转圈
                                        }

                                        // 步骤4：使用IGNORE去重，已存在唯一约束的消息直接忽略
                                        long insertResult = db.chatDao().insert(m);
                                    }
                                    // 刷新UI（确保拉取的消息更新到界面）
                                    refreshUI(firstLoad);
                                });
                            } else {
                                // 无新数据，直接刷新UI
                                refreshUI(firstLoad);
                            }
                        } else {
                            // 接口响应失败，刷新UI显示本地数据
                            String errorMsg = response.body() != null ? response.body().getMessage() : "服务器响应异常";
                            mainHandler.post(() -> Toast.makeText(ChatActivity.this, "加载聊天记录失败：" + errorMsg, Toast.LENGTH_SHORT).show());
                            refreshUI(firstLoad);
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<List<ChatMessage>>> call, Throwable t) {
                        // 网络请求失败，刷新UI显示本地数据
                        mainHandler.post(() -> Toast.makeText(ChatActivity.this, "网络异常，无法加载聊天记录：" + t.getMessage(), Toast.LENGTH_SHORT).show());
                        refreshUI(firstLoad);
                    }
                });
    }

    /**
     * 刷新聊天界面UI（核心优化：确保对方消息渲染+消息排序+聊天页显示最新数据）
     * @param firstLoad 是否为首次加载（首次加载滚动到最底部）
     */
    private void refreshUI(boolean firstLoad) {
        DbExecutor.execute(() -> {
            // 核心：使用双向匹配查询方法，获取双方消息
            List<ChatMessage> latest = db.chatDao().getChatMessagesByTwoUsers(userId, friendId);

            // 核心优化1：对消息按时间戳升序排序（确保消息从早到晚排列，修复null值排序漏洞）
            List<ChatMessage> sortedMessages = new ArrayList<>();
            if (latest != null && !latest.isEmpty()) {
                Collections.sort(latest, (msg1, msg2) -> {
                    long ts1 = (msg1.getTimestamp() == null) ? 0 : msg1.getTimestamp();
                    long ts2 = (msg2.getTimestamp() == null) ? 0 : msg2.getTimestamp();
                    return Long.compare(ts1, ts2);
                });
                sortedMessages.addAll(latest);
            }

            // 核心优化2：再次校验消息type和status，兜底处理，确保对方消息渲染
            for (ChatMessage msg : sortedMessages) {
                // 非空校验
                if (msg.getUserId() == null) {
                    msg.setUserId("unknown_user");
                }
                // 核心：强制按userId兜底消息类型，忽略后端无效type
                boolean isCurrentUserMsg = userId.equals(msg.getUserId());
                msg.setType(isCurrentUserMsg ? ChatMessage.TYPE_SENT : ChatMessage.TYPE_RECEIVED);

                // 兜底设置消息状态，避免对方消息转圈，同时确保我方消息状态正确
                if (msg.getStatus() == null) {
                    msg.setStatus(ChatMessage.STATUS_SUCCESS);
                } else if (msg.getStatus() == ChatMessage.STATUS_THINKING && msg.getType() == ChatMessage.TYPE_RECEIVED) {
                    msg.setStatus(ChatMessage.STATUS_SUCCESS); // 对方消息不转圈
                }
            }

            // 修复：创建final临时变量，解决lambda表达式引用问题
            final List<ChatMessage> finalLatest = sortedMessages;
            final boolean finalFirstLoad = firstLoad;

            // 主线程更新UI（避免跨线程操作视图）
            mainHandler.post(() -> {
                messageList.clear();
                messageList.addAll(finalLatest); // 引用final临时变量
                chatAdapter.notifyDataSetChanged();

                // 首次加载时滚动到最底部（修复：正确滚动到最后一条消息）
                if (finalFirstLoad && !messageList.isEmpty()) {
                    rvChat.scrollToPosition(messageList.size() - 1);
                }
            });
        });
    }

    /**
     * 发送聊天消息（核心优化：解决消息一直转圈+重复发送+状态更新不生效）
     * @param msg 待发送的消息对象
     */
    private void sendMessage(ChatMessage msg) {
        DbExecutor.execute(() -> {
            // 步骤1：唯一性校验，避免重复发送
            ChatMessage existingMsg = db.chatDao().getMessageByUniqueKey(
                    msg.getContent(), msg.getTimestamp(), msg.getUserId(), msg.getFriendId()
            );
            if (existingMsg != null) {
                mainHandler.post(() -> Toast.makeText(ChatActivity.this, "该消息已发送，无需重复发送", Toast.LENGTH_SHORT).show());
                return;
            }

            // 步骤2：插入本地数据库，获取自动生成的ID（依赖IGNORE去重）
            long insertResult = db.chatDao().insert(msg);
            if (insertResult == -1) {
                mainHandler.post(() -> Toast.makeText(ChatActivity.this, "消息插入失败，已存在重复消息", Toast.LENGTH_SHORT).show());
                return;
            }
            final int msgId = (int) insertResult;
            msg.setId(msgId);

            // 步骤3：主线程更新UI，立即显示发送中的消息
            mainHandler.post(() -> {
                int newMsgPosition = messageList.size();
                messageList.add(msg);
                chatAdapter.notifyItemInserted(newMsgPosition);
                rvChat.scrollToPosition(newMsgPosition);
            });

            // 步骤4：调用后端接口发送消息到服务器（后端无Bearer前缀，直接传token）
            if (token.isEmpty()) {
                mainHandler.post(() -> {
                    msg.setStatus(ChatMessage.STATUS_FAILED);
                    // 同时更新数据库中的状态
                    DbExecutor.execute(() -> db.chatDao().updateMessage(msgId, msg.getContent(), ChatMessage.STATUS_FAILED));
                    chatAdapter.notifyDataSetChanged();
                    Toast.makeText(ChatActivity.this, "Token为空，无法发送消息到服务器", Toast.LENGTH_SHORT).show();
                });
                return;
            }
            String authToken = token;
            apiService.sendMessage(authToken, msg)
                    .enqueue(new Callback<BaseResponse<ChatMessage>>() {
                        @Override
                        public void onResponse(Call<BaseResponse<ChatMessage>> call,
                                               Response<BaseResponse<ChatMessage>> response) {
                            // 核心修复：先更新数据库，再同步更新内存列表和适配器
                            DbExecutor.execute(() -> {
                                db.chatDao().updateMessage(msgId, msg.getContent(), ChatMessage.STATUS_SUCCESS);
                                mainHandler.post(() -> {
                                    // 找到消息位置，精准刷新
                                    int msgPosition = messageList.indexOf(msg);
                                    if (msgPosition != -1) {
                                        messageList.get(msgPosition).setStatus(ChatMessage.STATUS_SUCCESS);
                                        chatAdapter.notifyItemChanged(msgPosition);
                                    } else {
                                        chatAdapter.notifyDataSetChanged();
                                    }
                                    rvChat.scrollToPosition(messageList.size() - 1);
                                });
                            });
                        }

                        @Override
                        public void onFailure(Call<BaseResponse<ChatMessage>> call, Throwable t) {
                            // 核心修复：先更新数据库，再同步更新内存列表和适配器
                            DbExecutor.execute(() -> {
                                db.chatDao().updateMessage(msgId, msg.getContent(), ChatMessage.STATUS_FAILED);
                                mainHandler.post(() -> {
                                    int msgPosition = messageList.indexOf(msg);
                                    if (msgPosition != -1) {
                                        messageList.get(msgPosition).setStatus(ChatMessage.STATUS_FAILED);
                                        chatAdapter.notifyItemChanged(msgPosition);
                                    } else {
                                        chatAdapter.notifyDataSetChanged();
                                    }
                                    Toast.makeText(ChatActivity.this, "消息发送失败，请检查网络", Toast.LENGTH_SHORT).show();
                                });
                            });
                        }
                    });
        });
    }

    /**
     * 页面销毁时释放资源
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销广播接收器
        try {
            unregisterReceiver(chatRefreshReceiver);
        } catch (Exception ignored) {}

        // 移除所有未执行的Handler回调，避免内存泄漏
        mainHandler.removeCallbacksAndMessages(null);
    }
}