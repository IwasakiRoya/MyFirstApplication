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
import com.example.myfirstapplication.database.ChatDao;
import com.example.myfirstapplication.model.ChatSummary;
import com.example.myfirstapplication.model.User;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.utils.NetworkUtils;
import com.example.myfirstapplication.utils.DbExecutor;
import com.example.myfirstapplication.utils.FriendAddHelper;
import com.example.myfirstapplication.utils.TimeFormatUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // 新增：头像缓存Map，避免重复拉取头像（解决头像闪烁）
    private Map<String, String> avatarCacheMap = new HashMap<>();

    // 列表轮询相关变量（优化：降低轮询间隔，关闭不必要的轮询，仅保留核心刷新）
    private Handler listPollHandler;
    private Runnable listPollRunnable;
    private static final long LIST_POLL_INTERVAL = 3000; // 优化：改为3秒，减少刷新频率
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

                // 核心修复：跳转聊天页时，标记该好友聊天为已读（同步本地+后端）
                markChatAsRead(friendId);

                // 3. 执行跳转
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("friendName", chat.getName());
                intent.putExtra("friendId", friendId);
                // 优先使用缓存头像，避免重复加载
                String avatarUrl = avatarCacheMap.getOrDefault(friendId, chat.getAvatarUrl());
                intent.putExtra("friendAvatar", avatarUrl);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        // 加载数据（优先真实数据）
        loadChatData();

        // 初始化广播接收器和列表刷新处理器
        initChatRefreshReceiver();
        listRefreshHandler = new Handler(Looper.getMainLooper());

        // 初始化列表轮询任务（优化：降低频率，移除头像拉取）
        initListPollingTask();

        // 正确绑定FAB并设置点击事件
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
     * 初始化列表轮询任务（优化：仅拉取列表数据，不处理头像，减少刷新频率）
     */
    private void initListPollingTask() {
        listPollHandler = new Handler(Looper.getMainLooper());
        listPollRunnable = new Runnable() {
            @Override
            public void run() {
                // 校验：轮询开启、页面未销毁、已登录
                if (isListPolling && !isDetached() && !isRemoving() && token != null && !token.isEmpty()) {
                    // 轮询拉取最新聊天列表（仅刷新数据，不处理头像）
                    refreshChatList();
                    // 继续下一次轮询
                    listPollHandler.postDelayed(this, LIST_POLL_INTERVAL);
                }
            }
        };
    }

    /**
     * 轻量刷新聊天列表（仅拉取数据，不弹错误提示，不处理头像）
     */
    private void refreshChatList() {
        if (apiService == null || token == null || token.isEmpty()) {
            return;
        }

        apiService.getChatList(token).enqueue(new Callback<BaseResponse<List<ChatSummary>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<ChatSummary>>> call, Response<BaseResponse<List<ChatSummary>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                    List<ChatSummary> result = response.body().getData();
                    if (result != null && !result.isEmpty()) {
                        // 调用优化后的方法，仅处理未读计数，不重复拉取头像
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
     * 刷新聊天列表并更新未读小红点（重新拉取最新数据，而非本地缓存）
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
     * 核心修复：
     * 1. 优先信任后端未读计数，本地仅兜底（解决全红BUG）
     * 2. 头像优先使用缓存，不再轮询拉取（解决头像闪烁）
     * 3. 优化列表刷新逻辑，避免重复刷新
     */
    private void calculateUnreadCountForChatList(List<ChatSummary> chatList) {
        if (chatList == null || chatList.isEmpty() || myUserId == null || myUserId.isEmpty() || getContext() == null) {
            return;
        }

        DbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            ChatDao chatDao = db.chatDao(); // 获取ChatDao实例

            for (ChatSummary summary : chatList) {
                String friendId = summary.getFriendId();
                if (friendId != null && !friendId.isEmpty()) {
                    // 核心修复1：优先使用后端未读计数，本地仅当后端为0/null时兜底
                    int backendUnreadCount = summary.getUnreadCount() == 0 ? 0 : summary.getUnreadCount();
                    int localUnreadCount = chatDao.getUnreadMsgCount(myUserId, friendId);
                    // 仅当后端未读数为0时，才使用本地计数（避免本地与后端冲突）
                    summary.setUnreadCount(backendUnreadCount > 0 ? backendUnreadCount : localUnreadCount);

                    // 核心修复2：时间戳转换兜底，避免空指针
                    if (summary.getTime() == null || summary.getTime().isEmpty()) {
                        long timestamp = 0;
                        try {
                            timestamp = summary.getLastMessageTime() != null ? summary.getLastMessageTime() : System.currentTimeMillis();
                        } catch (Exception e) {
                            timestamp = System.currentTimeMillis();
                        }
                        summary.setTime(TimeFormatUtils.formatTimestampToHHmm(timestamp));
                    }

                    // 核心修复3：头像处理（优先缓存，仅首次拉取，解决闪烁）
                    String avatarUrl = summary.getAvatarUrl();
                    // 先从缓存获取
                    if (avatarCacheMap.containsKey(friendId)) {
                        summary.setAvatarUrl(avatarCacheMap.get(friendId));
                    } else {
                        // 缓存中无数据，且后端头像为空时，才拉取一次（非轮询）
                        if ((avatarUrl == null || avatarUrl.isEmpty()) && token != null && !token.isEmpty()) {
                            NetworkUtils.getFriendUserInfo(getContext(), token, friendId, new NetworkUtils.OnGetFriendUserInfoListener() {
                                @Override
                                public void onSuccess(User user) {
                                    if (user != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                                        // 存入缓存，后续不再拉取
                                        avatarCacheMap.put(friendId, user.getAvatarUrl());
                                        summary.setAvatarUrl(user.getAvatarUrl());
                                        // 主线程刷新列表（仅一次，避免重复）
                                        if (listRefreshHandler != null) {
                                            listRefreshHandler.post(() -> {
                                                adapter.updateData(new ArrayList<>(dataList));
                                            });
                                        }
                                    }
                                }

                                @Override
                                public void onError(String errorMsg) {
                                    // 静默失败，存入空缓存，避免重复请求
                                    avatarCacheMap.put(friendId, "");
                                    summary.setAvatarUrl("");
                                }
                            });
                        } else {
                            // 后端有头像数据，存入缓存
                            String finalAvatarUrl = avatarUrl == null ? "" : avatarUrl;
                            avatarCacheMap.put(friendId, finalAvatarUrl);
                            summary.setAvatarUrl(finalAvatarUrl);
                        }
                    }
                }
            }

            // 主线程更新UI（简化逻辑，仅一次刷新，避免重复）
            if (listRefreshHandler != null) {
                listRefreshHandler.post(() -> {
                    dataList.clear();
                    dataList.addAll(chatList);
                    adapter.updateData(new ArrayList<>(dataList)); // 传入新列表，避免引用传递导致数据混乱
                });
            }
        });
    }

    /**
     * 核心新增：标记聊天为已读（同步本地数据库+后端，解决未读全红）
     */
    private void markChatAsRead(String friendId) {
        if (getContext() == null || myUserId == null || myUserId.isEmpty() || friendId == null || friendId.isEmpty()) {
            return;
        }

        // 1. 本地标记已读（更新阅读时间戳）
        long currentTime = System.currentTimeMillis();
        DbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            db.chatDao().markChatAsRead(myUserId, friendId, currentTime);
        });

        // 2. 后端标记已读（可选，根据接口调整，确保后端未读计数同步清零）
        if (apiService != null && token != null && !token.isEmpty()) {
            // 注：需根据实际后端接口补充markChatAsRead接口调用，此处为占位
//            apiService.markChatAsRead(token, myUserId, friendId, currentTime).enqueue(new Callback<BaseResponse<Void>>() {
//                @Override
//                public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
//                    // 后端标记成功，刷新列表
//                    if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
//                        refreshChatList();
//                    }
//                }
//
//                @Override
//                public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
//                    // 静默失败，本地已标记，不影响用户体验
//                }
//            });
        }
    }

    /**
     * 从本地数据库加载聊天数据（网络失败时兜底）
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
            summary.setUnreadCount(0); // 本地数据未读计数强制为0，避免全红
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
                // 2. 直接调用 Context 的原生 unregisterReceiver() 方法
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

        // 注销广播接收器，释放资源
        unregisterChatRefreshReceiver();

        // 停止列表轮询并清理回调
        if (isListPolling && listPollHandler != null) {
            isListPolling = false;
            listPollHandler.removeCallbacksAndMessages(null);
        }

        // 移除Handler所有回调，避免内存泄漏
        if (listRefreshHandler != null) {
            listRefreshHandler.removeCallbacksAndMessages(null);
            listRefreshHandler = null;
        }

        // 清空头像缓存，释放内存
        avatarCacheMap.clear();
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

    /**
     * 页面可见时开启轮询并刷新列表（优化：降低轮询频率，减少资源消耗）
     */
    @Override
    public void onResume() {
        super.onResume();
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

    /**
     * 页面不可见时停止轮询（节省资源，避免后台消耗）
     */
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