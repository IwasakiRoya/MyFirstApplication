package com.example.myfirstapplication.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myfirstapplication.R;
import com.example.myfirstapplication.model.ChatSummary;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private List<ChatSummary> list;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ChatSummary chat);
    }

    // 构造方法（核心：仅通过构造传入监听）
    public MessageAdapter(List<ChatSummary> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener; // 直接赋值，不再重复绑定
    }

    // 移除：setOnItemClickListener 方法（无需额外设置）

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_summary, parent, false);
        // 完全删除这里的点击绑定逻辑！！！
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSummary item = list.get(position);
        holder.tvName.setText(item.getName());
        holder.tvLastMsg.setText(item.getLastMessage());
        holder.tvTime.setText(item.getTime());

        // 头像显示逻辑保留
        if (item.getAvatarUrl() != null && !item.getAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getAvatarUrl())
                    .circleCrop()
                    .error(item.getAvatarResId())
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(item.getAvatarResId());
        }

        // ========== 核心修复：在 onBindViewHolder 中绑定点击事件 ==========
        holder.itemView.setOnClickListener(v -> {
            // 双重校验：防止position无效/监听为空
            if (listener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                int realPosition = holder.getAdapterPosition();
                listener.onItemClick(list.get(realPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMsg, tvTime;
        ImageView ivAvatar;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvLastMsg = itemView.findViewById(R.id.tv_last_msg);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
        }
    }

    // 其他方法保留（removeFriendRequestItem、updateData）
    public void removeFriendRequestItem() {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isFriendRequest()) {
                list.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public void updateData(List<ChatSummary> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }
}