package com.example.myfirstapplication.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.model.FriendRequestEntity;
import com.example.myfirstapplication.model.User;
import com.example.myfirstapplication.model.response.BaseResponse;
import com.example.myfirstapplication.network.ApiService;
import com.example.myfirstapplication.utils.NetworkUtils;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {
    private List<FriendRequestEntity> requestList;
    private OnRequestListener listener;
    private ApiService apiService; // 接口服务
    private String token; // 用户token
    private long lastClickTime = 0; // 防抖

    // 回调接口（适配实体类）
    public interface OnRequestListener {
        void onAccept(FriendRequestEntity request);
        void onReject(FriendRequestEntity request);
    }

    // 统一构造方法：包含所有必要参数
    public FriendRequestAdapter(List<FriendRequestEntity> list, OnRequestListener listener, String token) {
        this.requestList = list == null ? java.util.Collections.emptyList() : list;
        this.listener = listener;
        this.token = token;
        this.apiService = NetworkUtils.getApiService(); // 统一获取ApiService单例
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRequestEntity request = requestList.get(position);
        if (request == null) return;

        // 1. 设置发起者昵称（先用人ID兜底，再异步查真实昵称）
        String fromUserId = request.getFromUserId();
        // 初始显示：用户ID兜底
        holder.tvName.setText("用户" + fromUserId);
        // 异步查询真实昵称并更新
        loadUserName(fromUserId, holder.tvName);

        // 2. 设置验证消息（空值兜底）
        String requestMsg = request.getRequestMsg() == null ? "请求添加你为好友" : request.getRequestMsg();
        holder.tvMsg.setText(requestMsg);

        // 3. 加载发起者头像（先显示默认头像，再异步查真实头像）
        RequestOptions options = new RequestOptions()
                .transform(new CircleCrop())
                .error(R.mipmap.ic_launcher_round);
        // 初始显示默认头像
        holder.ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
        // 异步查询真实头像并更新
        loadUserAvatar(fromUserId, holder.ivAvatar, options);

        // 4. 通过按钮（防抖）
        holder.btnAccept.setOnClickListener(v -> {
            if (isClickValid()) {
                if (listener != null) {
                    listener.onAccept(request);
                }
            }
        });

        // 5. 拒绝按钮（防抖）
        holder.btnReject.setOnClickListener(v -> {
            if (isClickValid()) {
                if (listener != null) {
                    listener.onReject(request);
                }
            }
        });
    }

    /**
     * 异步加载用户昵称（调用searchUser接口）
     */
    private void loadUserName(String userId, TextView tvName) {
        // 空值校验
        if (apiService == null || token == null || token.isEmpty() || userId == null) {
            return;
        }
        // 调用searchUser接口，通过userId查用户信息
        apiService.searchUser(token, userId)
                .enqueue(new Callback<BaseResponse<User>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<User>> call, Response<BaseResponse<User>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                            User user = response.body().getData();
                            if (user != null && user.getNickname() != null && !user.getNickname().isEmpty()) {
                                // 更新UI必须在主线程
                                tvName.setText(user.getNickname());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<User>> call, Throwable t) {
                        // 查不到昵称，保持“用户+ID”的兜底显示即可
                    }
                });
    }

    /**
     * 异步加载用户头像（调用searchUser接口）
     */
    private void loadUserAvatar(String userId, ImageView ivAvatar, RequestOptions options) {
        // 空值校验
        if (apiService == null || token == null || token.isEmpty() || userId == null) {
            return;
        }
        apiService.searchUser(token, userId)
                .enqueue(new Callback<BaseResponse<User>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<User>> call, Response<BaseResponse<User>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getCode() == 200) {
                            User user = response.body().getData();
                            if (user != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                                // 主线程加载头像
                                Glide.with(ivAvatar.getContext())
                                        .load(user.getAvatarUrl())
                                        .apply(options)
                                        .into(ivAvatar);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<User>> call, Throwable t) {
                        // 查不到头像，保持默认头像即可
                    }
                });
    }

    /**
     * 点击防抖校验
     */
    private boolean isClickValid() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime > 500) {
            lastClickTime = currentTime;
            return true;
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    /**
     * 更新请求列表
     */
    public void updateData(List<FriendRequestEntity> newList) {
        this.requestList = newList == null ? java.util.Collections.emptyList() : newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvMsg;
        Button btnAccept, btnReject;

        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvMsg = itemView.findViewById(R.id.tv_msg);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}