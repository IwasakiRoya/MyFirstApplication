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

import androidx.annotation.NonNull;
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

    // ========== 核心新增：轮询相关变量（优化轮询逻辑） ==========
    private Handler pollHandler; // 轮询处理器
    private Runnable pollRunnable; // 轮询任务
    private static final long POLL_INTERVAL = 500; // 轮询间隔缩短为500ms，提升实时性
    private boolean isPolling = false; // 轮询开关（防止页面销毁后继续轮询）
    private long lastPullTimestamp = 0; // 记录最后一次拉取消息的时间戳，避免重复拉取

    // ========== 核心新增：已读/未读核心变量（解决渲染消息标记已读问题） ==========
    private long lastRenderedMessageTimestamp = 0; // 最新已渲染（可见）消息的时间戳（临界点）
    private boolean isChatPageVisible = true; // 聊天页面是否可见（避免后台操作标记已读）

    // ========== 新增核心变量 ==========
    private boolean isUserScrollingToHistory = false; // 是否用户主动向上滚动查看历史消息
    private int lastScrollY = 0; // 记录上一次滚动的Y轴偏移量，用于判断滚动方向

    // ========= 广播 =========
    private final BroadcastReceiver chatRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String refreshFriendId = intent.getStringExtra("friendId");
            if (friendId != null && friendId.equals(refreshFriendId)) {
                // 收到广播，立即拉取新消息（非首次加载）
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

        // 注册广播接收器（接收外部刷新通知）
        IntentFilter filter = new IntentFilter("com.example.REFRESH_CHAT");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(chatRefreshReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(chatRefreshReceiver, filter);
        }

        // 首次加载历史聊天记录
        loadChatHistory(true);
        // ========== 核心修复：初始化轮询任务（优化实时性） ==========
        initPollingTask();
    }

    /**
     * 核心修复：初始化消息轮询任务（实时刷新新消息，修复轮询逻辑漏洞）
     */
    private void initPollingTask() {
        pollHandler = new Handler(Looper.getMainLooper());
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                // 校验：轮询开启、页面未销毁、未退出
                if (isPolling && !isFinishing() && !isDestroyed() && !token.isEmpty()) {
                    // 轮询拉取新消息（非首次加载，仅拉取最新消息）
                    loadChatHistory(false);

                    // 继续下一次轮询（确保轮询不中断）
                    pollHandler.postDelayed(this, POLL_INTERVAL);
                }
            }
        };
    }

    // ChatActivity.java - onResume 方法修改
    @Override
    protected void onResume() {
        super.onResume();
        isChatPageVisible = true; // 标记页面可见
        // 重置滚动标记（回到聊天页面，默认不处于查看历史消息状态）
        isUserScrollingToHistory = false;
        lastScrollY = 0;
        // 开启轮询（核心修复：立即执行第一次轮询，无延迟）
        if (!isPolling) {
            isPolling = true;
            if (pollHandler != null && pollRunnable != null) {
                // 立即执行一次轮询，然后再按间隔轮询（提升实时性）
                pollHandler.post(pollRunnable);
            }
        }

        // 进入聊天页面，先刷新UI，再标记已读（确保渲染完成后获取最新可见消息）
        refreshUI(false);
        // 延迟标记已读，确保RecyclerView已渲染完成
        mainHandler.postDelayed(() -> markCurrentRenderedMessagesAsRead(), 300);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isChatPageVisible = false; // 标记页面不可见
        // 停止轮询
        if (isPolling) {
            isPolling = false;
            if (pollHandler != null && pollRunnable != null) {
                pollHandler.removeCallbacks(pollRunnable);
            }
        }

        // 核心新增：页面退出/暂停时，标记当前已渲染消息为已读（解决退出后仍显示未读）
        markCurrentRenderedMessagesAsRead();
    }

    // ========== 核心新增：标记当前已渲染（可见）消息为已读 ==========
    /**
     * 标记当前RecyclerView中可见的最新消息为已读（核心逻辑）
     */
    private void markCurrentRenderedMessagesAsRead() {
        if (messageList.isEmpty() || friendId.isEmpty() || userId.isEmpty() || token.isEmpty()) {
            return;
        }

        // 步骤1：更新最新已渲染消息的时间戳（获取可见的最新消息）
        updateLastRenderedMessageTimestamp();

        // 步骤2：本地标记已读 + 同步后端阅读位置
        DbExecutor.execute(() -> {
            long currentRenderedTs = lastRenderedMessageTimestamp;
            if (currentRenderedTs <= 0) {
                currentRenderedTs = System.currentTimeMillis();
            }

            // 1. 更新本地阅读时间
            db.chatDao().markChatAsRead(userId, friendId, currentRenderedTs);

            // 2. 获取最新消息ID（用于后端更新阅读位置）
            List<ChatMessage> latestMsgList = db.chatDao().getChatMessagesByTwoUsers(userId, friendId);
            int latestMsgId = 0;
            if (!latestMsgList.isEmpty()) {
                ChatMessage latestMsg = latestMsgList.get(latestMsgList.size() - 1);
                latestMsgId = latestMsg.getId() != null ? latestMsg.getId() : 0;
            }

            // 3. 同步更新后端阅读位置（传递最新已渲染时间戳，精准标记已读）
            updateBackendReadPosition(latestMsgId, currentRenderedTs);

            // 4. 发送广播，通知MessageFragment刷新列表和小红点
            sendChatRefreshBroadcast(friendId);
        });
    }

    /**
     * 更新最新已渲染（可见）消息的时间戳（临界点：该时间戳之前的消息视为已读）
     */
    private void updateLastRenderedMessageTimestamp() {
        if (rvChat == null || layoutManager == null || messageList.isEmpty()) {
            return;
        }

        // 步骤1：获取RecyclerView中最后一个完全可见的消息位置（用户真正看到的最新消息）
        int lastVisiblePos = layoutManager.findLastCompletelyVisibleItemPosition();
        if (lastVisiblePos == -1 || lastVisiblePos >= messageList.size()) {
            // 无完全可见消息，获取最后一个可见项兜底
            lastVisiblePos = layoutManager.findLastVisibleItemPosition();
            if (lastVisiblePos == -1 || lastVisiblePos >= messageList.size()) {
                return;
            }
        }

        // 步骤2：获取该位置的消息，更新最新已渲染时间戳
        ChatMessage lastRenderedMessage = messageList.get(lastVisiblePos);
        if (lastRenderedMessage != null && lastRenderedMessage.getTimestamp() != null) {
            // 仅保留最新的时间戳（避免回滚到历史消息的旧时间戳）
            if (lastRenderedMessage.getTimestamp() > lastRenderedMessageTimestamp) {
                lastRenderedMessageTimestamp = lastRenderedMessage.getTimestamp();
            }
        }
    }

// ChatActivity.java - 新增后端阅读位置更新方法
    /**
     * 调用后端接口，更新后端 ChatReadPosition 表的阅读位置（传递最新已渲染时间戳）
     */
    private void updateBackendReadPosition(int latestMsgId, long currentTime) {
        if (token.isEmpty() || friendId.isEmpty() || userId.isEmpty()) {
            return;
        }

        // 调用后端 /api/chat/read 接口（对应 ChatController 的 updateReadPosition 方法）
        String authToken = token;
        apiService.updateReadPosition(authToken, friendId, latestMsgId)
                .enqueue(new Callback<BaseResponse<Void>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                        if (!response.isSuccessful() || response.body() == null || response.body().getCode() != 200) {
                            mainHandler.post(() -> {
                                Toast.makeText(ChatActivity.this, "同步已读状态到服务器失败", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                        mainHandler.post(() -> {
                            Toast.makeText(ChatActivity.this, "网络异常，同步已读状态失败", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    /**
     * 发送聊天刷新广播（通知MessageFragment更新对应聊天项的摘要和小红点）
     */
    private void sendChatRefreshBroadcast(String refreshFriendId) {
        Intent intent = new Intent("com.example.REFRESH_CHAT");
        intent.putExtra("friendId", refreshFriendId);

        // 核心：限定广播仅在当前应用内传递，提升安全性
        intent.setPackage(getPackageName());
        // 发送全局广播（仅本应用内可接收）
        sendBroadcast(intent);
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

        // 初始化聊天适配器（传递头像+当前用户ID参数）
        chatAdapter = new ChatAdapter(messageList, currentUserAvatar, friendAvatar, userId);
        rvChat.setAdapter(chatAdapter);

        // ========== 核心修改：完善RecyclerView滚动监听器，判断用户是否查看历史消息 ==========
        rvChat.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // 1. 滚动停止时，更新滚动状态标记
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // 若用户滚动到列表顶部（历史消息最前方），或未回到底部，标记为「正在查看历史消息」
                    int firstVisiblePos = layoutManager.findFirstVisibleItemPosition();
                    int lastVisiblePos = layoutManager.findLastVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();

                    // 判定条件：① 滚动到顶部 ② 未滚动到最底部 ③ 不是刚加载完成的默认状态
                    isUserScrollingToHistory = (firstVisiblePos == 0)
                            || (lastVisiblePos != totalItemCount - 1 && totalItemCount > 0);

                    // 滚动停止后，更新已渲染消息时间戳（避免滚动过程中频繁触发）
                    if (isChatPageVisible) {
                        updateLastRenderedMessageTimestamp();
                        // 延迟标记已读，确保滚动稳定
                        mainHandler.postDelayed(() -> markCurrentRenderedMessagesAsRead(), 200);
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    // 2. 用户主动拖动时，记录滚动方向
                    int currentScrollY = recyclerView.computeVerticalScrollOffset();
                    // 向上滚动（查看历史消息）：当前滚动偏移量 < 上一次滚动偏移量
                    isUserScrollingToHistory = (currentScrollY < lastScrollY) && (lastScrollY > 0);
                    lastScrollY = currentScrollY;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // dy < 0 ：向上滚动（查看历史消息）；dy > 0 ：向下滚动（回到最新消息）
                if (dy < 0) {
                    isUserScrollingToHistory = true;
                } else if (dy > 0 && layoutManager.findLastCompletelyVisibleItemPosition() == messageList.size() - 1) {
                    // 向下滚动且回到最底部，取消「查看历史消息」标记
                    isUserScrollingToHistory = false;
                    updateLastRenderedMessageTimestamp();
                }
                // 更新滚动偏移量
                lastScrollY = recyclerView.computeVerticalScrollOffset();
            }
        });

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

        // 初始化滚动标记变量
        isUserScrollingToHistory = false;
        lastScrollY = 0;
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
     * 核心修复：加载聊天历史记录（解决对方消息不显示+重复消息+实时刷新问题）
     * @param firstLoad 是否为首次加载（首次加载全量数据，非首次加载拉取最新消息）
     */
    private void loadChatHistory(boolean firstLoad) {
        // 核心优化1：添加Token校验，避免无权限获取消息
        if (token.isEmpty()) {
            Toast.makeText(this, "Token为空，无法获取聊天历史", Toast.LENGTH_SHORT).show();
            refreshUI(firstLoad);
            return;
        }

        // 核心优化2：修复lastTs逻辑（首次加载传0，非首次传最后一次拉取的时间戳，避免重复拉取）
        long lastTs = 0;
        if (!firstLoad) {
            // 非首次加载：使用最后一次拉取的时间戳，仅拉取新消息
            lastTs = lastPullTimestamp;
            // 兜底：如果消息列表不为空，使用最新消息的时间戳
            if (!messageList.isEmpty() && lastTs == 0) {
                ChatMessage latestMsg = messageList.get(messageList.size() - 1);
                lastTs = (latestMsg.getTimestamp() == null) ? 0 : latestMsg.getTimestamp();
            }
        }

        // 调用后端接口获取聊天记录（后端无Bearer前缀，直接传token）
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

                                        // 步骤2：唯一性校验，已存在的消息直接跳过，避免重复
                                        ChatMessage existingMsg = db.chatDao().getMessageByUniqueKey(
                                                m.getContent(), m.getTimestamp(), m.getUserId(), m.getFriendId()
                                        );
                                        if (existingMsg != null) {
                                            continue;
                                        }

                                        // 步骤3：修复消息类型判断，确保对方消息正确渲染
                                        boolean isCurrentUserMsg = userId.equals(m.getUserId());
                                        if (isCurrentUserMsg) {
                                            m.setType(ChatMessage.TYPE_SENT); // 我方消息：1
                                            m.setStatus(ChatMessage.STATUS_SUCCESS); // 我方消息默认成功，避免转圈
                                        } else {
                                            m.setType(ChatMessage.TYPE_RECEIVED); // 对方消息：0
                                            m.setStatus(ChatMessage.STATUS_SUCCESS); // 对方消息直接成功，隐藏转圈
                                        }

                                        // 步骤4：插入数据库（IGNORE去重，避免重复消息）
                                        long insertResult = db.chatDao().insert(m);
                                    }

                                    // 核心修复：更新最后一次拉取的时间戳（避免下次重复拉取）
                                    if (!data.isEmpty()) {
                                        ChatMessage latestNewMsg = data.get(data.size() - 1);
                                        lastPullTimestamp = latestNewMsg.getTimestamp() != null ? latestNewMsg.getTimestamp() : System.currentTimeMillis();
                                    }

                                    // 刷新UI（确保拉取的消息更新到界面）
                                    refreshUI(firstLoad);

                                    // 核心新增：刷新UI后，更新已渲染消息并标记已读（实时接收新消息后立即标记）
                                    if (isChatPageVisible) {
                                        mainHandler.postDelayed(() -> {
                                            updateLastRenderedMessageTimestamp();
                                            markCurrentRenderedMessagesAsRead();
                                            // 新消息加载完成后，若用户未查看历史消息，才滚动到最底部
                                            if (!isUserScrollingToHistory && !messageList.isEmpty()) {
                                                rvChat.scrollToPosition(messageList.size() - 1);
                                            }
                                        }, 200);
                                    }
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
     * 核心修复：刷新聊天界面UI（确保对方消息渲染+消息排序+实时显示最新数据）
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

            // 主线程更新UI（避免跨线程操作视图，确保实时性）
            mainHandler.post(() -> {
                // 核心修复：先对比消息列表是否有变化，再更新（避免不必要的刷新）
                if (!messageList.equals(finalLatest)) {
                    messageList.clear();
                    messageList.addAll(finalLatest);
                    chatAdapter.notifyDataSetChanged();
                }

                // 核心修改：仅当「用户未主动查看历史消息」时，才滚动到最底部
                if ((finalFirstLoad || !finalLatest.isEmpty()) && rvChat.getAdapter() != null && !isUserScrollingToHistory) {
                    rvChat.scrollToPosition(messageList.size() - 1);
                }

                // 滚动完成后，更新最新已渲染消息时间戳
                updateLastRenderedMessageTimestamp();
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

                // 核心修改：发送消息时，强制滚动到最底部（忽略历史消息查看状态）
                rvChat.scrollToPosition(newMsgPosition);
                // 重置滚动标记，恢复默认状态（后续新消息仍可正常自动滚动）
                isUserScrollingToHistory = false;

                // 发送后更新已渲染消息时间戳（改为标记所有消息为已读）
                updateLastRenderedMessageToLatest(msg.getTimestamp());
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

                                    // 核心修改：仅当「用户未主动查看历史消息」时，才滚动到最底部
                                    if (!isUserScrollingToHistory) {
                                        rvChat.scrollToPosition(messageList.size() - 1);
                                    }

                                    // 核心修复：更新最后一次拉取时间戳，确保轮询能拉取后续消息
                                    lastPullTimestamp = System.currentTimeMillis();

                                    // 核心新增：发送成功后，标记已渲染消息为已读
                                    markCurrentRenderedMessagesAsRead();

                                    // 发送广播，通知消息列表刷新
                                    sendChatRefreshBroadcast(friendId);
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
     * 发送消息时，标记所有消息为已读（直接更新为最新消息时间戳，覆盖中间所有消息）
     * @param latestTimestamp 最新消息（发送消息）的时间戳
     */
    private void updateLastRenderedMessageToLatest(Long latestTimestamp) {
        if (latestTimestamp == null) {
            latestTimestamp = System.currentTimeMillis();
        }
        // 直接将临界点设为最新消息时间戳，所有早于该时间的消息都视为已读
        lastRenderedMessageTimestamp = latestTimestamp;
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
        if (pollHandler != null) {
            pollHandler.removeCallbacksAndMessages(null);
        }
    }
}