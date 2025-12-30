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
import com.example.myfirstapplication.model.GroupResponse;
import com.example.myfirstapplication.model.SearchResult;
import com.example.myfirstapplication.model.User;
import com.example.myfirstapplication.model.request.FriendRequest;
import com.example.myfirstapplication.model.request.GroupAddRequest;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 公共添加好友/群组工具类：复用输入框，支持用户/群组搜索
 */
public class FriendAddHelper {
    // 单例 ApiService
    private static ApiService apiService = NetworkUtils.getApiService();

    /**
     * 显示添加好友/群组对话框（核心：复用原有输入框，扩展群组逻辑）
     * @param context 上下文
     * @param myUserId 当前登录用户ID
     */
    public static void showAddFriendDialog(Context context, String myUserId) {
        if (context == null || myUserId == null || myUserId.isEmpty()) {
            Toast.makeText(context, "参数异常，无法添加", Toast.LENGTH_SHORT).show();
            return;
        }

        // 加载原有添加对话框布局（复用输入框，不改动布局文件）
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_friend, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog = builder.setView(dialogView).create();

        EditText etKeyword = dialogView.findViewById(R.id.et_friend_id);
        EditText etRequestMsg = dialogView.findViewById(R.id.et_request_msg);

        // 修改输入框提示语（支持用户/群组ID）
        etKeyword.setHint("请输入用户ID或群组ID");

        // 取消按钮点击事件
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        // 发送按钮点击事件（扩展为统一搜索）
        dialogView.findViewById(R.id.btn_send).setOnClickListener(v -> {
            String keyword = etKeyword.getText().toString().trim();
            if (keyword.isEmpty()) {
                Toast.makeText(context, "请输入用户ID或群组ID", Toast.LENGTH_SHORT).show();
                return;
            }

            // 获取 token
            String token = NetworkUtils.getTokenFromSharedPref(context);
            if (token.isEmpty()) {
                Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            // 统一搜索（用户+群组）
            NetworkUtils.searchUserOrGroup(context, token, keyword, new NetworkUtils.OnSearchResultListener() {
                @Override
                public void onResult(List<SearchResult> resultList) {
                    if (resultList == null || resultList.isEmpty()) {
                        Toast.makeText(context, "未找到该用户或群组", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 显示统一搜索结果对话框
                    showSearchResultDialog(context, resultList, etRequestMsg.getText().toString().trim(), myUserId, token);
                    dialog.dismiss();
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
     * 显示统一搜索结果对话框（支持用户/群组选择）
     */
    private static void showSearchResultDialog(Context context, List<SearchResult> resultList,
                                               String requestMsg, String myUserId, String token) {
        if (context == null || resultList.isEmpty()) return;

        // 构建结果展示数组
        String[] resultNames = new String[resultList.size()];
        for (int i = 0; i < resultList.size(); i++) {
            SearchResult result = resultList.get(i);
            if (result.getResultType() == SearchResult.TYPE_USER) {
                User user = result.getUser();
                resultNames[i] = "用户：" + user.getNickname() + " (" + user.getUserId() + ")";
            } else if (result.getResultType() == SearchResult.TYPE_GROUP) {
                GroupResponse group = result.getGroup();
                resultNames[i] = "群组：" + group.getGroupName() + " (" + group.getGroupId() + ")";
            }
        }

        new AlertDialog.Builder(context)
                .setTitle("选择要添加的好友或群组")
                .setItems(resultNames, (dialog, which) -> {
                    SearchResult result = resultList.get(which);
                    if (result.getResultType() == SearchResult.TYPE_USER) {
                        // 原有逻辑：发送好友请求
                        User user = result.getUser();
                        sendFriendRequest(context, myUserId, user.getUserId(), requestMsg, token);
                    } else if (result.getResultType() == SearchResult.TYPE_GROUP) {
                        // 新增逻辑：发送加入群组请求
                        GroupResponse group = result.getGroup();
                        sendGroupJoinRequest(context, myUserId, group.getGroupId(), token);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 原有逻辑：发送好友请求（保持不变）
     */
    private static void sendFriendRequest(Context context, String myUserId, String targetUserId,
                                          String requestMsg, String token) {
        FriendRequest request = new FriendRequest(myUserId, targetUserId, requestMsg);

        apiService.sendFriendRequest(token, request).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Void> res = response.body();
                    Toast.makeText(context, res.getMessage(), Toast.LENGTH_SHORT).show();
                    if (res.isSuccess()) {
                        // 同步到本地数据库
                        new Thread(() -> {
                            FriendRequestEntity entity = new FriendRequestEntity(myUserId, targetUserId, requestMsg);
                            entity.setRequestId(String.valueOf(System.currentTimeMillis()));
                            AppDatabase.getInstance(context).friendRequestDao().insert(entity);
                        }).start();
                    }
                } else {
                    Toast.makeText(context, "发送好友请求失败：服务器响应异常", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                Toast.makeText(context, "发送好友请求失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 新增逻辑：发送加入群组请求
     */
    private static void sendGroupJoinRequest(Context context, String myUserId, String targetGroupId, String token) {
        // 构建加入群组请求
        GroupAddRequest groupAddRequest = new GroupAddRequest();
        groupAddRequest.setTargetGroupId(targetGroupId);
        groupAddRequest.setOperateType(1); // 1=加入群组（0=创建群组）

        // 调用后端加入群组接口
        apiService.joinGroup(token, groupAddRequest).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Void> res = response.body();
                    Toast.makeText(context, res.getMessage(), Toast.LENGTH_SHORT).show();
                    if (res.isSuccess()) {
                        // 同步到本地数据库（群成员关系）
                        new Thread(() -> {
                            com.example.myfirstapplication.model.GroupUser groupUser =
                                    new com.example.myfirstapplication.model.GroupUser();
                            groupUser.setGroupId(targetGroupId);
                            groupUser.setUserId(myUserId);
                            groupUser.setRole(0); // 0=普通成员
                            groupUser.setIsQuit(0); // 0=未退出
                            AppDatabase.getInstance(context).groupUserDao().insert(groupUser);
                        }).start();
                    }
                } else {
                    Toast.makeText(context, "加入群组失败：服务器响应异常", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                Toast.makeText(context, "加入群组失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}