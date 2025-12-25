package com.example.myfirstapplication.Fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myfirstapplication.Adapter.FriendAdapter;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.activity.ChatActivity;
import com.example.myfirstapplication.activity.FriendRequestActivity;
import com.example.myfirstapplication.database.AppDatabase;
import com.example.myfirstapplication.model.Friend;
import com.example.myfirstapplication.model.request.FriendRequest;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.utils.NetworkUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        // 初始化当前用户ID
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
        // 核心修复：加固好友点击跳转逻辑
        friendAdapter = new FriendAdapter(friend -> {
            // 1. 校验上下文和好友对象
            if (getActivity() == null || getActivity().isFinishing() || friend == null) {
                Toast.makeText(getContext(), "跳转失败：页面状态异常", Toast.LENGTH_SHORT).show();
                return;
            }
            // 2. 校验friendId和friendNickname非空
            if (friend.getFriendId() == null || friend.getFriendId().isEmpty()) {
                Toast.makeText(getContext(), "好友ID不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (friend.getFriendNickname() == null || friend.getFriendNickname().isEmpty()) {
                friend.setFriendNickname("未知好友"); // 兜底
            }
            // 3. 执行跳转
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("friendName", friend.getFriendNickname());
            intent.putExtra("friendId", friend.getFriendId());
            intent.putExtra("friendAvatar", friend.getFriendAvatar()); // 传递头像
            startActivity(intent);
        });
        rvFriends.setAdapter(friendAdapter);

        // 加载数据
        listenUnreadFriendRequests();
        loadMyFriends();
        // syncFriendRequestsFromServer(); // 后端未实现，暂时注释

        // 点击事件
        llFriendRequest.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), FriendRequestActivity.class));
        });

        llAddFriend.setOnClickListener(v -> {
            Toast.makeText(getContext(), "后端接口未实现，暂时无法添加好友", Toast.LENGTH_SHORT).show();
            // showAddFriendDialog(); // 后端未实现，暂时注释
        });

        fabAddFriend.setOnClickListener(v -> {
            Toast.makeText(getContext(), "后端接口未实现，暂时无法添加好友", Toast.LENGTH_SHORT).show();
            // showAddFriendDialog();
        });

        return view;
    }

    // 监听未读好友请求
    private void listenUnreadFriendRequests() {
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
        // 修复：调用正确的DAO方法（getMyFriends）
        AppDatabase.getInstance(getContext()).friendDao()
                .getMyFriends(myUserId)
                .observe(getViewLifecycleOwner(), friends -> {
                    if (friends == null) {
                        friendAdapter.updateData(null);
                        return;
                    }
                    friendAdapter.updateData(friends);
                });
    }

    // 以下方法暂时注释（后端未实现）
    /*
    private void syncFriendRequestsFromServer() {
        NetworkUtils.getApiService().getFriendRequests("Bearer " + getToken())
                .enqueue(new Callback<BaseResponse<List<FriendRequestEntity>>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<List<FriendRequestEntity>>> call, Response<BaseResponse<List<FriendRequestEntity>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<List<FriendRequestEntity>> res = response.body();
                            if (res.getCode() == 200) {
                                List<FriendRequestEntity> serverRequests = res.getData();
                                if (serverRequests == null || serverRequests.isEmpty()) return;

                                new Thread(() -> {
                                    AppDatabase db = AppDatabase.getInstance(getContext());
                                    for (FriendRequestEntity req : serverRequests) {
                                        FriendRequestEntity existing = db.friendRequestDao().getRequestById(req.getRequestId());
                                        if (existing == null) {
                                            db.friendRequestDao().insert(req);
                                        }
                                    }
                                }).start();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<List<FriendRequestEntity>>> call, Throwable t) {}
                });
    }

    private void showAddFriendDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_friend, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("添加好友")
                .setView(dialogView)
                .setNegativeButton("取消", null);

        EditText etKeyword = dialogView.findViewById(R.id.et_friend_id);
        EditText etRequestMsg = dialogView.findViewById(R.id.et_request_msg);
        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("搜索");
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String keyword = etKeyword.getText().toString().trim();
                if (keyword.isEmpty()) {
                    Toast.makeText(getContext(), "请输入好友ID/昵称/手机号", Toast.LENGTH_SHORT).show();
                    return;
                }

                NetworkUtils.searchUser(getContext(), getToken(), keyword, new NetworkUtils.OnSearchResultListener() {
                    @Override
                    public void onResult(BaseResponse response) {
                        if (response.getCode() == 200) {
                            List<User> userList = (List<User>) response.getData();
                            if (userList.isEmpty()) {
                                Toast.makeText(getContext(), "未找到该用户", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            showSearchResultDialog(userList, etRequestMsg.getText().toString().trim());
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), response.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String errorMsg) {
                        Toast.makeText(getContext(), "搜索失败：" + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();
    }

    private void showSearchResultDialog(List<User> userList, String requestMsg) {
        String[] names = new String[userList.size()];
        String[] userIds = new String[userList.size()];
        for (int i = 0; i < userList.size(); i++) {
            names[i] = userList.get(i).getNickname() + " (" + userList.get(i).getUserId() + ")";
            userIds[i] = userList.get(i).getUserId();
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

    private void sendFriendRequest(String targetUserId, String requestMsg) {
        FriendRequest request = new FriendRequest(myUserId, targetUserId, requestMsg);

        NetworkUtils.getApiService().sendFriendRequest("Bearer " + getToken(), request)
                .enqueue(new Callback<BaseResponse<Void>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<Void> res = response.body();
                            Toast.makeText(getContext(), res.getMessage(), Toast.LENGTH_SHORT).show();
                            if (res.getCode() == 200) {
                                new Thread(() -> {
                                    FriendRequestEntity entity = new FriendRequestEntity(myUserId, targetUserId, requestMsg);
                                    entity.setRequestId(String.valueOf(System.currentTimeMillis())); // 临时生成ID
                                    AppDatabase.getInstance(getContext()).friendRequestDao().insert(entity);
                                }).start();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                        Toast.makeText(getContext(), "发送请求失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    */

    private String getToken() {
        return getContext().getSharedPreferences("USER_INFO", 0).getString("token", "");
    }
}