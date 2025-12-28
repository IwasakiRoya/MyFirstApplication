package com.example.myfirstapplication.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myfirstapplication.R;
import com.example.myfirstapplication.database.AppDatabase;
import com.example.myfirstapplication.model.FriendRequestEntity;
import com.example.myfirstapplication.model.User;
import com.example.myfirstapplication.model.request.FriendRequest;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 公共添加好友工具类：复用添加好友逻辑，供所有页面调用
 */
public class FriendAddHelper {
    // 单例ApiService（与原有逻辑保持一致）
    private static ApiService apiService = NetworkUtils.getApiService();

    /**
     * 显示添加好友对话框（核心：与ContactsFragment中的逻辑完全一致）
     * @param context 上下文（需为Activity或已附加的Fragment上下文）
     * @param myUserId 当前登录用户ID
     */
    public static void showAddFriendDialog(Context context, String myUserId) {
        if (context == null || myUserId == null || myUserId.isEmpty()) {
            Toast.makeText(context, "参数异常，无法添加好友", Toast.LENGTH_SHORT).show();
            return;
        }

        // 加载添加好友对话框布局
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_friend, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog = builder.setView(dialogView).create();

        EditText etKeyword = dialogView.findViewById(R.id.et_friend_id);
        EditText etRequestMsg = dialogView.findViewById(R.id.et_request_msg);

        // 取消按钮点击事件
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        // 发送按钮点击事件
        dialogView.findViewById(R.id.btn_send).setOnClickListener(v -> {
            String keyword = etKeyword.getText().toString().trim();
            if (keyword.isEmpty()) {
                Toast.makeText(context, "请输入好友ID/昵称/手机号", Toast.LENGTH_SHORT).show();
                return;
            }

            // 获取token（与原有逻辑一致）
            String token = NetworkUtils.getTokenFromSharedPref(context);
            if (token.isEmpty()) {
                Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            // 搜索用户（复用NetworkUtils的搜索逻辑）
            NetworkUtils.searchUser(context, token, keyword, new NetworkUtils.OnUserSearchListener() {
                @Override
                public void onResult(User user) {
                    if (user != null) {
                        List<User> userList = new ArrayList<>();
                        userList.add(user);
                        showSearchResultDialog(context, userList, etRequestMsg.getText().toString().trim(), myUserId, token);
                        dialog.dismiss();
                    } else {
                        Toast.makeText(context, "未找到该用户", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String errorMsg) {
                    Toast.makeText(context, "搜索失败：" + errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    /**
     * 显示搜索结果对话框（复用原有逻辑）
     */
    private static void showSearchResultDialog(Context context, List<User> userList, String requestMsg, String myUserId, String token) {
        if (context == null || userList.isEmpty()) return;

        String[] names = new String[userList.size()];
        String[] userIds = new String[userList.size()];
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            names[i] = user.getNickname() + " (" + user.getUserId() + ")";
            userIds[i] = user.getUserId();
        }

        new AlertDialog.Builder(context)
                .setTitle("选择要添加的好友")
                .setItems(names, (dialog, which) -> {
                    String targetUserId = userIds[which];
                    sendFriendRequest(context, myUserId, targetUserId, requestMsg, token);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 发送好友请求（复用原有逻辑，包含本地数据库同步）
     */
    private static void sendFriendRequest(Context context, String myUserId, String targetUserId, String requestMsg, String token) {
        FriendRequest request = new FriendRequest(myUserId, targetUserId, requestMsg);

        apiService.sendFriendRequest(token, request).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Void> res = response.body();
                    Toast.makeText(context, res.getMessage(), Toast.LENGTH_SHORT).show();
                    if (res.isSuccess()) {
                        // 同步到本地数据库（子线程执行，避免阻塞主线程）
                        new Thread(() -> {
                            FriendRequestEntity entity = new FriendRequestEntity(myUserId, targetUserId, requestMsg);
                            entity.setRequestId(String.valueOf(System.currentTimeMillis()));
                            AppDatabase.getInstance(context).friendRequestDao().insert(entity);
                        }).start();
                    }
                } else {
                    Toast.makeText(context, "发送请求失败：服务器响应异常", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                Toast.makeText(context, "发送请求失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}