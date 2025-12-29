package com.example.myfirstapplication.Fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myfirstapplication.Adapter.MessageAdapter;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.activity.ChatActivity;
import com.example.myfirstapplication.database.AppDatabase;
import com.example.myfirstapplication.model.ChatSummary;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.utils.NetworkUtils;
import com.example.myfirstapplication.utils.DbExecutor;
import com.example.myfirstapplication.utils.FriendAddHelper;
import com.example.myfirstapplication.utils.TimeFormatUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import android.os.Handler;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageFragment extends Fragment {
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<ChatSummary> dataList = new ArrayList<>();
    private boolean isMock = false; // 改为加载真实数据
    private ApiService apiService; // 补全缺失的 apiService 变量定义
    private String myUserId; // 补全缺失的 myUserId 变量定义（当前登录用户ID）
    private FloatingActionButton fabAddFriend; // 正确绑定FloatingActionButton

    private BroadcastReceiver chatRefreshReceiver; // 聊天刷新广播接收器
    private Handler listRefreshHandler; // 列表刷新处理器
    private String token; // 缓存当前用户Token，避免重复获取

    // 新增：列表轮询相关变量（解决对方发消息不即时刷新问题）
    private Handler listPollHandler;
    private Runnable listPollRunnable;
    private static final long LIST_POLL_INTERVAL = 1000; // 列表轮询间隔（1秒，平衡实时性和性能）
    private boolean isListPolling = false; // 轮询开关

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);

        apiService = NetworkUtils.getApiService();

        // 初始化当前用户ID和Token（与ContactsFragment逻辑一致）
        if (getContext() != null) {
            myUserId = NetworkUtils.getUserIdFromSharedPref(getContext());
            token = NetworkUtils.getTokenFromSharedPref(getContext());
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

        // 加载数据（优先真实数据）
        loadChatData();

        // 核心修复：初始化广播接收器和列表刷新处理器
        initChatRefreshReceiver();
        listRefreshHandler = new Handler(Looper.getMainLooper());

        // 新增：初始化列表轮询任务（解决实时刷新问题）
        initListPollingTask();

        // 核心修复：正确绑定FAB并设置点击事件
        fabAddFriend = view.findViewById(R.id.fab_add_friend);
        fabAddFriend.setOnClickListener(v -> {
            // 调用公共工具类，调起添加好友对话框（复用ContactsFragment的正确逻辑）
            if (getContext() != null) {
                FriendAddHelper.showAddFriendDialog(getContext(), myUserId);
            }
        });

        return view;
    }

    /**
     * 新增：初始化列表轮询任务（实时拉取聊天摘要，更新红点和最新消息）
     */
    private void initListPollingTask() {
        listPollHandler = new Handler(Looper.getMainLooper());
        listPollRunnable = new Runnable() {
            @Override
            public void run() {
                // 校验：轮询开启、页面未销毁、已登录
                if (isListPolling && !isDetached() && !isRemoving() && token != null && !token.isEmpty()) {
                    // 轮询拉取最新聊天列表
                    refreshChatList();
                    // 继续下一次轮询
                    listPollHandler.postDelayed(this, LIST_POLL_INTERVAL);
                }
            }
        };
    }

    /**
     * 新增：轻量刷新聊天列表（仅拉取数据，不弹错误提示）
     */
    private void refreshChatList() {
        if (apiService == null || token.isEmpty()) {
            return;
        }

        apiService.getChatList(token).enqueue(new Callback<BaseResponse<List<ChatSummary>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<ChatSummary>>> call, Response<BaseResponse<List<ChatSummary>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    List<ChatSummary> result = response.body().getData();
                    if (result != null && !result.isEmpty()) {
                        calculateUnreadCountForChatList(result);
                    }
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<List<ChatSummary>>> call, Throwable t) {
                // 轮询失败不弹提示，避免干扰用户
            }
        });
    }

    /**
     * 初始化聊天刷新广播接收器（统一上下文，避免注销失败）
     */
    private void initChatRefreshReceiver() {
        // 先注销已存在的广播，避免重复注册异常
        unregisterChatRefreshReceiver();

        chatRefreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // 双重判空，提升稳定性
                if (intent != null && "com.example.REFRESH_CHAT".equals(intent.getAction())) {
                    if (getContext() != null && !isDetached() && !isRemoving()) {
                        refreshChatListWithUnreadBadge();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.example.REFRESH_CHAT");
        // 统一使用 Activity 上下文，与注销逻辑保持一致
        if (getActivity() != null) {
            try {
                // 使用 ContextCompat 自动处理标志位兼容性，支持 Android 12+
                ContextCompat.registerReceiver(
                        getActivity(),
                        chatRefreshReceiver,
                        filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED
                );
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "广播注册失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 核心修复：刷新聊天列表并更新未读小红点（重新拉取最新数据，而非本地缓存）
     */
    private void refreshChatListWithUnreadBadge() {
        if (getContext() == null || apiService == null || token == null || token.isEmpty()) {
            Toast.makeText(getContext(), "无法刷新：Token为空或未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 核心：重新从后端拉取最新聊天列表（不是本地缓存）
        apiService.getChatList(token).enqueue(new Callback<BaseResponse<List<ChatSummary>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<ChatSummary>>> call, Response<BaseResponse<List<ChatSummary>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    List<ChatSummary> result = response.body().getData();
                    if (result != null && !result.isEmpty()) {
                        // 本地补全数据（保留后端未读数，本地查询仅兜底）
                        calculateUnreadCountForChatList(result);
                    } else {
                        // 无新数据，清空列表并刷新UI
                        if (listRefreshHandler != null) {
                            listRefreshHandler.post(() -> {
                                dataList.clear();
                                adapter.updateData(new ArrayList<>(dataList));
                            });
                        }
                    }
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : "服务器响应异常";
                    Toast.makeText(getContext(), "刷新聊天列表失败：" + errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<List<ChatSummary>>> call, Throwable t) {
                // 网络失败时，尝试从本地数据库拉取数据
                Toast.makeText(getContext(), "网络异常，尝试加载本地数据：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                loadLocalChatData();
            }
        });
    }

    /**
     * 核心修复：计算聊天列表中每个项的未读消息数并更新UI（保留后端数据，本地查询仅兜底）
     */
    private void calculateUnreadCountForChatList(List<ChatSummary> chatList) {
        if (chatList == null || chatList.isEmpty() || myUserId == null || myUserId.isEmpty() || getContext() == null) {
            return;
        }

        DbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            for (ChatSummary summary : chatList) {
                String friendId = summary.getFriendId();
                if (friendId != null && !friendId.isEmpty()) {
                    // 核心修复：保留后端返回的未读数，本地查询仅作为兜底（当后端未读数为0/null时使用）
                    int localUnreadCount = db.chatDao().getUnreadMsgCount(myUserId, friendId);
                    if (summary.getUnreadCount() <= 0) {
                        summary.setUnreadCount(localUnreadCount);
                    }

                    // 时间格式化兜底：将后端返回的时间戳转换为可读格式
                    if (summary.getTime() == null || summary.getTime().isEmpty() || summary.getTime().matches("\\d+")) {
                        long timestamp = 0;
                        try {
                            timestamp = summary.getTime() != null ? Long.parseLong(summary.getTime()) : System.currentTimeMillis();
                        } catch (Exception e) {
                            timestamp = System.currentTimeMillis();
                        }
                        summary.setTime(TimeFormatUtils.formatTimestampToHHmm(timestamp));
                    }

                    // 兜底：头像URL为空时设置空字符串，避免Glide加载异常
                    if (summary.getAvatarUrl() == null || summary.getAvatarUrl().isEmpty()) {
                        summary.setAvatarUrl("");
                    }
                }
            }

            // 主线程更新UI（简化逻辑，仅调用Adapter的updateData方法，避免重复刷新）
            if (listRefreshHandler != null) {
                // 主线程更新UI（简化逻辑，避免重复刷新）
                listRefreshHandler.post(() -> {
                    dataList.clear();
                    dataList.addAll(chatList);
                    // 仅调用 updateData 方法，完成数据替换和刷新
                    adapter.updateData(new ArrayList<>(dataList)); // 传入新列表，避免引用传递导致数据混乱
                });
            }
        });
    }

    /**
     * 新增：从本地数据库加载聊天数据（网络失败时兜底）
     */
    private void loadLocalChatData() {
        if (getContext() == null || myUserId == null || myUserId.isEmpty()) {
            return;
        }

        DbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            // 暂时用模拟数据兜底，避免列表为空（可扩展为本地数据库查询）
            List<ChatSummary> localList = new ArrayList<>();
            ChatSummary summary = new ChatSummary("本地缓存", "暂无最新消息", TimeFormatUtils.formatTimestampToHHmm(System.currentTimeMillis()), R.mipmap.ic_launcher_round);
            summary.setFriendId("local_001");
            summary.setUnreadCount(0);
            localList.add(summary);

            // 主线程更新UI
            if (listRefreshHandler != null) {
                listRefreshHandler.post(() -> {
                    dataList.clear();
                    dataList.addAll(localList);
                    adapter.updateData(new ArrayList<>(dataList));
                });
            }
        });
    }

    /**
     * 配套：注销聊天刷新广播接收器（与注册上下文一致，避免内存泄漏）
     */
    private void unregisterChatRefreshReceiver() {
        // 1. 确保上下文和广播接收器不为空
        if (getActivity() != null && chatRefreshReceiver != null) {
            try {
                // 2. 直接调用 Context 的原生 unregisterReceiver() 方法（无需 ContextCompat）
                getActivity().unregisterReceiver(chatRefreshReceiver);
            } catch (Exception ignored) {
                // 捕获「广播未注册却被注销」的异常，避免崩溃
                ignored.printStackTrace();
            } finally {
                // 3. 注销后置空，避免重复操作和内存泄漏
                chatRefreshReceiver = null;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // 核心修复：注销广播接收器，释放资源（直接复用封装方法，无需 ContextCompat）
        unregisterChatRefreshReceiver();

        // 新增：停止列表轮询并清理回调
        if (isListPolling && listPollHandler != null) {
            isListPolling = false;
            listPollHandler.removeCallbacksAndMessages(null);
        }

        // 移除Handler所有回调，避免内存泄漏
        if (listRefreshHandler != null) {
            listRefreshHandler.removeCallbacksAndMessages(null);
            listRefreshHandler = null;
        }
    }

    // 以下原有方法保持优化（修复数据加载逻辑）
    private void loadChatData() {
        dataList.clear();
        if (isMock) {
            loadMockData();
        } else {
            loadRecentChats();
        }
    }

    private void loadMockData() {
        if (getContext() == null) {
            return;
        }

        ChatSummary summary1 = new ChatSummary("张三", "晚上打球吗？", "18:05", R.drawable.ic_avatar_1);
        summary1.setFriendId("1001");
        summary1.setAvatarUrl("");
        summary1.setUnreadCount(2); // 模拟未读消息

        ChatSummary summary2 = new ChatSummary("李四", "项目文档发我一下", "15:30", R.drawable.ic_avatar_2);
        summary2.setFriendId("1002");
        summary2.setAvatarUrl("");
        summary2.setUnreadCount(5); // 模拟未读消息

        ChatSummary summary3 = new ChatSummary("DeepSeek AI", "你好！我是你的智能助手", "10:00", R.drawable.ic_ai_logo);
        summary3.setFriendId("ai_001");
        summary3.setAvatarUrl("");
        summary3.setUnreadCount(0); // 无未读消息

        dataList.add(summary1);
        dataList.add(summary2);
        dataList.add(summary3);
        adapter.updateData(new ArrayList<>(dataList));
        Toast.makeText(getContext(), "加载了" + dataList.size() + "条模拟数据", Toast.LENGTH_SHORT).show();
    }

    private void loadRecentChats() {
        if (getContext() == null || token == null || token.isEmpty()) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            loadMockData(); // 未登录时加载模拟数据
            return;
        }

        // 适配BaseResponse<List<ChatSummary>>泛型
        apiService.getChatList(token).enqueue(new Callback<BaseResponse<List<ChatSummary>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<ChatSummary>>> call, Response<BaseResponse<List<ChatSummary>>> response) {
                BaseResponse<List<ChatSummary>> res = response.body();
                if (response.isSuccessful() && res != null && res.getCode() == 200) {
                    List<ChatSummary> result = res.getData();
                    if (result != null && !result.isEmpty()) {
                        dataList.clear();
                        dataList.addAll(result);
                        // 核心修复：计算未读计数并刷新UI
                        calculateUnreadCountForChatList(dataList);
                    } else {
                        Toast.makeText(getContext(), "暂无最近聊天记录", Toast.LENGTH_SHORT).show();
                        loadMockData(); // 无数据时加载模拟数据
                    }
                } else {
                    String errorMsg = res != null ? res.getMessage() : "服务器响应异常";
                    Toast.makeText(getContext(), "加载失败：" + errorMsg, Toast.LENGTH_SHORT).show();
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

    // 新增：页面可见时开启轮询并刷新列表（确保返回消息列表时数据最新）
    @Override
    public void onResume() {
        super.onResume();
        // 开启列表轮询
        if (!isListPolling && token != null && !token.isEmpty()) {
            isListPolling = true;
            if (listPollHandler != null && listPollRunnable != null) {
                listPollHandler.post(listPollRunnable);
            }
        }

        // 刷新最新数据
        if (getContext() != null && token != null && !token.isEmpty()) {
            refreshChatListWithUnreadBadge();
        }
    }

    // 新增：页面不可见时停止轮询（节省资源，避免后台消耗）
    @Override
    public void onPause() {
        super.onPause();
        // 停止列表轮询
        if (isListPolling) {
            isListPolling = false;
            if (listPollHandler != null && listPollRunnable != null) {
                listPollHandler.removeCallbacks(listPollRunnable);
            }
        }
    }
}