package com.example.myfirstapplication.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.model.Friend;

import java.util.ArrayList;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {
    private List<Friend> friendList = new ArrayList<>();
    private OnFriendClickListener listener;
    private long lastClickTime = 0; // 防抖：最后点击时间

    // 点击防抖间隔（500ms）
    private static final long CLICK_INTERVAL = 500;

    public interface OnFriendClickListener {
        void onFriendClick(Friend friend);
    }

    public FriendAdapter(OnFriendClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend friend = friendList.get(position);
        if (friend == null) return;

        // 1. 设置昵称（空值兜底）
        String nickname = friend.getFriendNickname() == null ? "未知好友" : friend.getFriendNickname();
        holder.tvName.setText(nickname);

        // 2. 加载头像（Glide圆形裁剪+错误占位）
        RequestOptions options = new RequestOptions()
                .transform(new CircleCrop())
                .error(R.mipmap.ic_launcher_round);

        String avatarUrl = friend.getFriendAvatar();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(avatarUrl)
                    .apply(options)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
        }

        // 3. 点击事件（防抖）
        holder.itemView.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime > CLICK_INTERVAL) {
                lastClickTime = currentTime;
                if (listener != null) {
                    listener.onFriendClick(friend);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    /**
     * 更新好友列表（全量更新）
     */
    public void updateData(List<Friend> newList) {
        if (newList == null) {
            friendList.clear();
        } else {
            friendList = new ArrayList<>(newList);
        }
        notifyDataSetChanged();
    }

    /**
     * 新增好友（增量更新）
     */
    public void addFriend(Friend friend) {
        if (friend == null) return;
        friendList.add(friend);
        notifyItemInserted(friendList.size() - 1);
    }

    /**
     * 获取好友列表
     */
    public List<Friend> getFriendList() {
        return new ArrayList<>(friendList);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName;

        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
        }
    }
}