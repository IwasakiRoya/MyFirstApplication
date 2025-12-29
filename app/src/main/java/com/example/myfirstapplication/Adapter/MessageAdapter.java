package com.example.myfirstapplication.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.model.ChatSummary;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private List<ChatSummary> list;
    private OnItemClickListener listener;
    // 1. 新增防抖变量定义（关键修复）
    private long lastClickTime = 0;
    // 定义防抖间隔（500ms，可根据需求调整）
    private static final long CLICK_INTERVAL = 500;

    public interface OnItemClickListener {
        void onItemClick(ChatSummary chat);
    }

    // 构造方法
    public MessageAdapter(List<ChatSummary> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_summary, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSummary item = list.get(position);
        holder.tvName.setText(item.getName());
        holder.tvLastMsg.setText(item.getLastMessage());
        holder.tvTime.setText(item.getTime());

        // ========== 核心修复：适配前后端未读数字段类型（Long → int） ==========
        long unreadCountLong = Math.max(item.getUnreadCount(), 0);
        int unreadCount = (int) Math.min(unreadCountLong, 99); // 限制最大为99，避免显示异常

        if (unreadCount > 0) {
            holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            // 未读数超过99显示"99+"
            holder.tvUnreadBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);
        }

        // 头像加载逻辑优化
        RequestOptions options = new RequestOptions()
                .circleCrop()
                .error(R.mipmap.ic_launcher_round);

        if (item.getAvatarUrl() != null && !item.getAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getAvatarUrl())
                    .apply(options)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(item.getAvatarResId() == 0 ? R.mipmap.ic_launcher_round : item.getAvatarResId());
        }

        // 2. 完善的点击事件（防抖+位置校验）
        holder.itemView.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            // 防抖校验：间隔小于500ms则忽略
            if (currentTime - lastClickTime > CLICK_INTERVAL) {
                lastClickTime = currentTime; // 更新最后点击时间
                // 位置有效性校验：防止列表刷新导致position失效
                int realPosition = holder.getAdapterPosition();
                if (listener != null && realPosition != RecyclerView.NO_POSITION
                        && realPosition < list.size()) {
                    listener.onItemClick(list.get(realPosition));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMsg, tvTime, tvUnreadBadge; // 新增 tvUnreadBadge
        ImageView ivAvatar;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvLastMsg = itemView.findViewById(R.id.tv_last_msg);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            // 核心新增：绑定小红点控件
            tvUnreadBadge = itemView.findViewById(R.id.tv_unread_badge);
        }
    }

    // 其他辅助方法保留
    public void removeFriendRequestItem() {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isFriendRequest()) {
                list.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    // 核心修复：新增updateData方法，确保列表数据替换后正确刷新
    public void updateData(List<ChatSummary> newList) {
        this.list = newList != null ? newList : new ArrayList<>(); // 兜底空列表
        notifyDataSetChanged(); // 单次刷新即可，无需额外调用
    }
}