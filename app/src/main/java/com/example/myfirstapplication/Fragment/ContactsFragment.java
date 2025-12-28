package com.example.myfirstapplication.Fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myfirstapplication.Adapter.FriendAdapter;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.activity.ChatActivity;
import com.example.myfirstapplication.activity.FriendRequestActivity;
import com.example.myfirstapplication.database.AppDatabase;
import com.example.myfirstapplication.model.Friend;
import com.example.myfirstapplication.model.FriendRequestEntity;
import com.example.myfirstapplication.model.User;
import com.example.myfirstapplication.model.request.FriendRequest;
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

public class ContactsFragment extends Fragment {
    private LinearLayout llFriendRequest;
    private LinearLayout llAddFriend;
    private RecyclerView rvFriends;
    private TextView tvRequestBadge;
    private FloatingActionButton fabAddFriend;
    private FriendAdapter friendAdapter;
    private String myUserId;
    private ApiService apiService;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        // 初始化当前用户ID
        if (getContext() == null) return view;
        SharedPreferences sp = getContext().getSharedPreferences("USER_INFO", 0);
        myUserId = sp.getString("userId", "1000");

        // 初始化控件
        llFriendRequest = view.findViewById(R.id.ll_friend_request);
        llAddFriend = view.findViewById(R.id.ll_add_friend);
        rvFriends = view.findViewById(R.id.rv_friends);
        tvRequestBadge = view.findViewById(R.id.tv_request_badge);
        fabAddFriend = view.findViewById(R.id.fab_add_friend);

        // 初始化ApiService
        apiService = NetworkUtils.getApiService();

        // 初始化RecyclerView
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        friendAdapter = new FriendAdapter(friend -> {
            // 校验上下文和好友对象
            if (getActivity() == null || getActivity().isFinishing() || friend == null) {
                Toast.makeText(getContext(), "跳转失败：页面状态异常", Toast.LENGTH_SHORT).show();
                return;
            }
            // 校验friendId和friendNickname非空
            String friendId = friend.getFriendId();
            if (friendId == null || friendId.isEmpty()) {
                Toast.makeText(getContext(), "好友ID不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            // 强制兜底：确保昵称不为null
            String friendName = friend.getFriendNickname();
            friendName = (friendName == null || friendName.isEmpty()) ? "未知好友" : friendName;

            // 执行跳转
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("friendName", friendName);
            intent.putExtra("friendId", friendId);
            intent.putExtra("friendAvatar", friend.getFriendAvatar() == null ? "" : friend.getFriendAvatar());
            startActivity(intent);
        });
        rvFriends.setAdapter(friendAdapter);

        // 加载数据
        listenUnreadFriendRequests();
        loadMyFriends();
        syncFriendRequestsFromServer(); // 启用真实接口

        // 点击事件（简化：直接调用公共工具类）
        llFriendRequest.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), FriendRequestActivity.class));
        });

        llAddFriend.setOnClickListener(v -> {
            // 复用公共工具类，调起添加好友对话框
            if (getContext() != null) {
                FriendAddHelper.showAddFriendDialog(getContext(), myUserId);
            }
        });

        fabAddFriend.setOnClickListener(v -> {
            // 复用公共工具类，调起添加好友对话框
            if (getContext() != null) {
                FriendAddHelper.showAddFriendDialog(getContext(), myUserId);
            }
        });

        return view;
    }


    // 监听未读好友请求
    private void listenUnreadFriendRequests() {
        if (getContext() == null) return;
        AppDatabase.getInstance(getContext()).friendRequestDao()
                .getPendingRequests(myUserId)
                .observe(getViewLifecycleOwner(), requests -> {
                    if (requests == null || requests.isEmpty()) {
                        tvRequestBadge.setVisibility(View.GONE);
                    } else {
                        tvRequestBadge.setVisibility(View.VISIBLE);
                        tvRequestBadge.setText(String.valueOf(requests.size()));
                    }
                });
    }

    // 加载我的好友列表
    private void loadMyFriends() {
        if (getContext() == null) return;
        // 先加载本地数据
        AppDatabase.getInstance(getContext()).friendDao()
                .getMyFriends(myUserId)
                .observe(getViewLifecycleOwner(), friends -> {
                    friendAdapter.updateData(friends);
                });

        // 调用后端接口同步好友列表
        String token = getToken();
        if (token.isEmpty()) return;

        apiService.getFriendList(token).enqueue(new Callback<BaseResponse<List<Friend>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<Friend>>> call, Response<BaseResponse<List<Friend>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Friend> serverFriends = response.body().getData();
                    if (serverFriends == null || serverFriends.isEmpty()) return;

                    // 同步到本地数据库（优化：批量插入/更新，补充 myId）
                    new Thread(() -> {
                        // 非空校验：避免 getContext() 为空导致崩溃
                        if (getContext() == null) return;

                        AppDatabase db = AppDatabase.getInstance(getContext());
                        // 核心修复：为每个好友补充 myId（当前登录用户ID），确保后续能查询到
                        for (Friend friend : serverFriends) {
                            friend.setMyId(myUserId); // 补充复合主键的myId，匹配本地数据库
                        }
                        // 方式1：批量插入（推荐）
                        db.friendDao().insertOrUpdateBatch(serverFriends);

                        // 为每个好友获取详细信息（精准查询，无冗余）
                        for (Friend friend : serverFriends) {
                            fetchFriendUserInfo(friend.getFriendId());
                        }
                    }).start();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<List<Friend>>> call, Throwable t) {
                Toast.makeText(getContext(), "同步好友列表失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 修复：获取好友的详细信息（改用 getUserInfo 精准查询）
    private void fetchFriendUserInfo(String friendId) {
        String token = getToken();
        if (token.isEmpty() || getContext() == null || friendId == null || friendId.isEmpty()) {
            System.out.println("获取好友信息失败：参数为空");
            return;
        }

        // 调用 NetworkUtils 中新增的 getFriendUserInfo 方法
        NetworkUtils.getFriendUserInfo(getContext(), token, friendId, new NetworkUtils.OnGetFriendUserInfoListener() {
            @Override
            public void onSuccess(User user) {
                if (user != null && getContext() != null) {
                    // 更新本地好友信息（映射 User 到 Friend）
                    updateFriendInfo(user);
                }
            }

            @Override
            public void onError(String errorMsg) {
                // 静默失败，不提示用户（仅记录日志）
                System.out.println("获取好友信息失败：" + errorMsg);
            }
        });
    }

    // 重构：更新好友信息到本地数据库（无冗余存储，仅更新昵称/头像）
    private void updateFriendInfo(User user) {
        if (user == null || user.getUserId() == null || user.getUserId().isEmpty()) {
            return;
        }

        new Thread(() -> {
            if (getContext() == null) return;

            AppDatabase db = AppDatabase.getInstance(getContext());
            // 精准查询本地好友（根据 myUserId 和 好友ID（user.getUserId()））
            Friend existingFriend = db.friendDao().getFriendById(myUserId, user.getUserId());

            if (existingFriend != null) {
                // 1. 核心：映射 User 信息到 Friend 实体（不存储冗余信息，仅更新必要字段）
                // 空值兜底，确保昵称/头像不为null
                String newNickname = user.getNickname() == null ? "未知好友" : user.getNickname();
                String newAvatar = user.getAvatarUrl() == null ? "" : user.getAvatarUrl();

                // 2. 更新好友的昵称和头像（仅更新这两个字段，符合不冗余存储的设计）
                existingFriend.setFriendNickname(newNickname);
                existingFriend.setFriendAvatar(newAvatar);

                // 3. 更新到数据库（使用 insertOrUpdate，冲突时替换，确保数据最新）
                db.friendDao().insertOrUpdate(existingFriend);

                // 4. 主线程刷新UI（优化：单次刷新，不重复观察 LiveData）
                mainHandler.post(() -> {
                    // 校验 Fragment 生命周期，避免内存泄漏和刷新失效
                    if (isAdded() && getViewLifecycleOwner().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                        // 重新获取完整好友列表，全量更新（简单可靠，无冗余）
                        db.friendDao().getMyFriends(myUserId).observe(getViewLifecycleOwner(), friends -> {
                            friendAdapter.updateData(friends);
                            // 移除该次观察，避免重复回调（优化性能）
                            db.friendDao().getMyFriends(myUserId).removeObservers(getViewLifecycleOwner());
                        });
                    }
                });
            }
        }).start();
    }

    // 从服务器同步好友请求
    private void syncFriendRequestsFromServer() {
        if (getContext() == null) return;
        String token = getToken();
        if (token.isEmpty()) return;

        // 改用修复后的NetworkUtils方法，适配新的回调接口
        NetworkUtils.syncFriendRequests(getContext(), token, new NetworkUtils.OnFriendRequestSyncListener() {
            @Override
            public void onSuccess(List<FriendRequestEntity> requestList) {
                if (requestList.isEmpty()) return;

                new Thread(() -> {
                    AppDatabase db = AppDatabase.getInstance(getContext());
                    for (FriendRequestEntity req : requestList) {
                        FriendRequestEntity existing = db.friendRequestDao().getRequestById(req.getRequestId());
                        if (existing == null) {
                            db.friendRequestDao().insert(req);
                        }
                    }
                }).start();
            }

            @Override
            public void onError(String errorMsg) {
                Toast.makeText(getContext(), "同步好友请求失败：" + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 显示添加好友对话框
    private void showAddFriendDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_friend, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        AlertDialog dialog = builder.setView(dialogView).create();

        EditText etKeyword = dialogView.findViewById(R.id.et_friend_id);
        EditText etRequestMsg = dialogView.findViewById(R.id.et_request_msg);

        // 为自定义布局中的按钮设置点击事件
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btn_send).setOnClickListener(v -> {
            String keyword = etKeyword.getText().toString().trim();
            if (keyword.isEmpty()) {
                Toast.makeText(getContext(), "请输入好友ID/昵称/手机号", Toast.LENGTH_SHORT).show();
                return;
            }

            String token = getToken();
            NetworkUtils.searchUser(getContext(), token, keyword, new NetworkUtils.OnUserSearchListener() {
                @Override
                public void onResult(User user) {
                    if (user != null) {
                        // 包装成列表，适配原有对话框逻辑
                        List<User> userList = new ArrayList<>();
                        userList.add(user);
                        showSearchResultDialog(userList, etRequestMsg.getText().toString().trim());
                        dialog.dismiss();
                    } else {
                        Toast.makeText(getContext(), "未找到该用户", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String errorMsg) {
                    Toast.makeText(getContext(), "搜索失败：" + errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    // 显示搜索结果对话框
    private void showSearchResultDialog(List<User> userList, String requestMsg) {
        if (getContext() == null || userList.isEmpty()) return;
        String[] names = new String[userList.size()];
        String[] userIds = new String[userList.size()];
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            names[i] = user.getNickname() + " (" + user.getUserId() + ")";
            userIds[i] = user.getUserId();
        }

        new AlertDialog.Builder(getContext())
                .setTitle("选择要添加的好友")
                .setItems(names, (dialog, which) -> {
                    String targetUserId = userIds[which];
                    sendFriendRequest(targetUserId, requestMsg);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 发送好友请求
    private void sendFriendRequest(String targetUserId, String requestMsg) {
        if (getContext() == null) return;
        FriendRequest request = new FriendRequest(myUserId, targetUserId, requestMsg);
        String token = getToken();

        apiService.sendFriendRequest(token, request).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Void> res = response.body();
                    Toast.makeText(getContext(), res.getMessage(), Toast.LENGTH_SHORT).show();
                    if (res.isSuccess()) {
                        new Thread(() -> {
                            FriendRequestEntity entity = new FriendRequestEntity(myUserId, targetUserId, requestMsg);
                            entity.setRequestId(String.valueOf(System.currentTimeMillis()));
                            AppDatabase.getInstance(getContext()).friendRequestDao().insert(entity);
                        }).start();
                    }
                } else {
                    Toast.makeText(getContext(), "发送请求失败：服务器响应异常", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                Toast.makeText(getContext(), "发送请求失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 抽取token获取逻辑，避免重复代码
    private String getToken() {
        if (getContext() == null) return "";
        return getContext().getSharedPreferences("USER_INFO", 0).getString("token", "");
    }

    // 生命周期优化：避免内存泄漏
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清空适配器引用
        if (friendAdapter != null) {
            friendAdapter.updateData(new ArrayList<>());
        }
        friendAdapter = null;
        apiService = null;
    }
}